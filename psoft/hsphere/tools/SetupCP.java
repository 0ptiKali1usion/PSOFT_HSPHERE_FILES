package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.hsphere.plan.wizard.PlanCreator;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.AdmCustomDNSRecord;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.AdmInstantAlias;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.hsphere.resource.admin.PhysicalServer;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.util.FakeRequest;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/tools/SetupCP.class */
public class SetupCP extends C0004CP {
    public Hashtable existPhysicals;

    public SetupCP() throws Exception {
        super("psoft_config.hsphere");
        this.existPhysicals = new Hashtable();
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Initializing ");
        try {
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(System.in));
            Document doc = parser.getDocument();
            Element root = doc.getDocumentElement();
            SetupCP setup = new SetupCP();
            try {
                Element servers = (Element) root.getElementsByTagName("servers").item(0);
                setup.processServers(servers);
            } catch (NullPointerException e) {
                System.out.println("Section servers [SKIPPED]");
            }
            Session.setResellerId(1L);
            if (HostManager.getHosts(2).size() > 0) {
                try {
                    Element zones = (Element) root.getElementsByTagName("zones").item(0);
                    setup.processZones(zones);
                } catch (NullPointerException e2) {
                    System.out.println("Section zones [SKIPPED]");
                }
            }
            if (HostManager.getHosts(1).size() > 0 && HostManager.getHosts(2).size() > 0 && HostManager.getHosts(3).size() > 0) {
                setup.processSystemUsers(root);
            } else {
                System.out.print("System user creation skipped in case the install has no web or name or mail server [SKIPPED]");
            }
        } catch (Exception e3) {
            e3.printStackTrace();
            System.exit(1);
        }
        System.out.println("Setup finished");
        System.exit(0);
    }

    public void processServers(Element servers) throws Exception {
        NodeList physycals = servers.getElementsByTagName("physical");
        if (physycals.getLength() > 0) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT DISTINCT id FROM p_server");
                ResultSet rs = ps.executeQuery();
                this.existPhysicals.clear();
                while (rs.next()) {
                    PhysicalServer ph = PhysicalServer.get(rs.getLong(1));
                    this.existPhysicals.put(ph.getName(), ph);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        for (int i = 0; i < physycals.getLength(); i++) {
            Element phEl = (Element) physycals.item(i);
            String name = phEl.getAttributes().getNamedItem("name").getNodeValue();
            processPhysical(phEl, (PhysicalServer) this.existPhysicals.get(name));
        }
    }

    public void processZones(Element zonesEl) throws Exception {
        NodeList zones = zonesEl.getElementsByTagName("zone");
        for (int i = 0; i < zones.getLength(); i++) {
            Element znEl = (Element) zones.item(i);
            String name = znEl.getAttributes().getNamedItem("name").getNodeValue();
            AdmDNSZone admZone = AdmDNSZone.getByName(name);
            processZone(znEl, admZone);
        }
    }

    public void processZone(Element znEl, AdmDNSZone admZone) throws Exception {
        String name = znEl.getAttributes().getNamedItem("name").getNodeValue();
        String email = znEl.getAttributes().getNamedItem("email").getNodeValue();
        String master = znEl.getAttributes().getNamedItem("master").getNodeValue();
        String slave1 = znEl.getAttributes().getNamedItem("slave1").getNodeValue();
        String slave2 = znEl.getAttributes().getNamedItem("slave2").getNodeValue();
        String allowHosting = znEl.getAttributes().getNamedItem("allowHosting").getNodeValue();
        System.out.print("Service DNS Zone " + name + " ");
        try {
            long masterId = 0;
            long slave1Id = 0;
            long slave2Id = 0;
            if ("".equals(master)) {
                Iterator i = HostManager.getRandomHostsList(2).iterator();
                masterId = ((HostEntry) i.next()).getId();
                slave1Id = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
                slave2Id = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
            } else {
                List<HostEntry> dnsServers = HostManager.getHosts(2);
                for (HostEntry he : dnsServers) {
                    if (master.equals(he.getName())) {
                        masterId = he.getId();
                    }
                    if (slave1.equals(he.getName())) {
                        slave1Id = he.getId();
                    }
                    if (slave2.equals(he.getName())) {
                        slave2Id = he.getId();
                    }
                    if (masterId != 0 && slave1Id != 0 && slave2Id != 0) {
                        break;
                    }
                }
                if (slave1Id == masterId) {
                    slave1Id = 0;
                    System.out.print("Slave1 " + slave1 + " has been skiped.");
                }
                if (slave2Id == masterId || slave2Id == slave1Id) {
                    slave2Id = 0;
                    System.out.print("Slave2 " + slave2 + " has been skiped.");
                }
            }
            if (admZone == null) {
                long newId = Session.getNewIdAsLong();
                admZone = AdmDNSZone.create(newId, name, email, "yes".equals(allowHosting), masterId, slave1Id, slave2Id);
                System.out.println("[CREATED]");
            } else {
                System.out.println("[SKIPPED]");
            }
        } catch (Exception ex) {
            System.out.println("[FAILED]");
            ex.printStackTrace();
        }
        NodeList aliases = znEl.getElementsByTagName("alias");
        for (int i2 = 0; i2 < aliases.getLength(); i2++) {
            List existAliases = admZone.getAliases();
            Element alEl = (Element) aliases.item(i2);
            String prefix = alEl.getAttributes().getNamedItem("prefix").getNodeValue();
            String tag = alEl.getAttributes().getNamedItem("tag").getNodeValue();
            int tagId = Integer.parseInt(tag);
            AdmInstantAlias al = null;
            Iterator j = existAliases.iterator();
            while (true) {
                if (!j.hasNext()) {
                    break;
                }
                AdmInstantAlias alTmp = (AdmInstantAlias) j.next();
                if (prefix.equals(alTmp.getPrefix())) {
                    al = alTmp;
                    break;
                }
            }
            System.out.print("\tAlias with prefix:" + prefix + " tag:" + tag);
            String updated = "[CREATED]";
            if (al != null) {
                try {
                    if (al.getTag() == tagId) {
                        System.out.println(" [SKIPPED]");
                        break;
                    }
                } catch (Exception e) {
                    System.out.println(" [FAILED]");
                }
            }
            if (al != null) {
                al.delete();
                updated = " [RECREATED]";
            }
            admZone.addAllias(prefix, tagId);
            System.out.println(updated);
        }
        NodeList cpaliases = znEl.getElementsByTagName("cpalias");
        for (int i3 = 0; i3 < cpaliases.getLength(); i3++) {
            List existCpAliases = admZone.getCpAliases();
            Element cpalEl = (Element) cpaliases.item(i3);
            String prefix2 = cpalEl.getAttributes().getNamedItem("prefix").getNodeValue();
            AdmInstantAlias al2 = null;
            Iterator j2 = existCpAliases.iterator();
            while (true) {
                if (!j2.hasNext()) {
                    break;
                }
                AdmInstantAlias alTmp2 = (AdmInstantAlias) j2.next();
                if (prefix2.equals(alTmp2.getPrefix())) {
                    al2 = alTmp2;
                    break;
                }
            }
            System.out.print("\tReseller CP Alias with prefix:" + prefix2);
            if (al2 != null) {
                System.out.println(" [SKIPPED]");
                break;
            }
            try {
                admZone.addCpAlias(prefix2);
                System.out.println("[CREATED]");
            } catch (Exception e2) {
                System.out.println(" [FAILED]");
            }
            System.out.println(" [FAILED]");
        }
        NodeList records = znEl.getElementsByTagName("record");
        for (int i4 = 0; i4 < records.getLength(); i4++) {
            List existRecords = admZone.getCustomRecords();
            Element recEl = (Element) records.item(i4);
            String recName = recEl.getAttributes().getNamedItem("name").getNodeValue();
            String data = recEl.getAttributes().getNamedItem("data").getNodeValue();
            String type = recEl.getAttributes().getNamedItem("type").getNodeValue();
            String ttl = recEl.getAttributes().getNamedItem("ttl").getNodeValue();
            String pref = recEl.getAttributes().getNamedItem("pref").getNodeValue();
            AdmCustomDNSRecord rec = null;
            Iterator j3 = existRecords.iterator();
            while (true) {
                if (!j3.hasNext()) {
                    break;
                }
                AdmCustomDNSRecord recTmp = (AdmCustomDNSRecord) j3.next();
                if (recTmp.getName().startsWith(recName)) {
                    rec = recTmp;
                    break;
                }
            }
            System.out.print("\tCustom DNSRecord with name:" + recName + " with data:" + data);
            String updated2 = "[CREATED]";
            if (rec != null) {
                try {
                } catch (Exception e3) {
                    System.out.println(" [FAILED]");
                }
                if (data.equals(rec.getData())) {
                    System.out.println(" [SKIPPED]");
                }
            }
            if (rec != null) {
                rec.delete();
                updated2 = " [RECREATED]";
            }
            admZone.addCustRecord(data, recName, type, ttl, pref);
            System.out.println(updated2);
        }
    }

    public void processSystemUsers(Element root) throws Exception {
        NodeList users = root.getElementsByTagName("sysuser");
        for (int i = 0; i < users.getLength(); i++) {
            processSystemUser((Element) users.item(i));
        }
    }

    public User processSystemUser(Element userEl) throws Exception {
        String login = userEl.getAttributes().getNamedItem("login").getNodeValue();
        String domain = userEl.getAttributes().getNamedItem("domain").getNodeValue();
        AdmDNSZone admZone = AdmDNSZone.getByName(domain);
        if (admZone == null) {
            throw new Exception("Zone " + domain + " isn`t exist");
        }
        System.out.print("System user " + login + " with domain name:" + domain);
        if (admZone.isLocked()) {
            System.err.print("Zone " + domain + " already taked.");
            System.out.println(" [SKIPPED]");
            return null;
        }
        User sysUser = null;
        try {
            sysUser = User.getUser(login);
        } catch (Exception e) {
        }
        if (sysUser != null) {
            Session.setUser(sysUser);
            System.out.println(" [EXIST]");
        } else {
            try {
                String password = userEl.getAttributes().getNamedItem("password").getNodeValue();
                User.createUser(login, password, Session.getResellerId());
                sysUser = User.getUser(login);
                Session.setUser(sysUser);
                System.out.println(" [CREATED]");
            } catch (Exception ex) {
                System.out.println(" [FAILED]");
                ex.printStackTrace();
            }
        }
        Element accountEl = (Element) userEl.getElementsByTagName("account").item(0);
        processAccount(sysUser, accountEl, domain);
        return sysUser;
    }

    public Account processAccount(User sysUser, Element accountEl, String domain) throws Exception {
        Resource unix;
        String name = accountEl.getAttributes().getNamedItem("name").getNodeValue();
        Account sysAccount = null;
        Iterator i = sysUser.getAccountIds().iterator();
        while (true) {
            if (!i.hasNext()) {
                break;
            }
            Account ac = sysUser.getAccount((ResourceId) i.next());
            if (ac.getPlan().isResourceAvailable("service_domain") != null) {
                sysAccount = ac;
                System.out.println("\tAccount [FOUND]");
                break;
            }
        }
        if (sysAccount != null && (unix = sysAccount.FM_getChild("unixuser").get()) != null) {
            System.out.print("\t\tService domain " + domain);
            List args = new ArrayList();
            args.add(domain);
            unix.addChild("service_domain", "", args);
            System.out.println(" [CREATED]");
            return sysAccount;
        }
        Plan sysPlan = null;
        Iterator i2 = Plan.getPlanList().iterator();
        while (true) {
            if (!i2.hasNext()) {
                break;
            }
            Plan pl = (Plan) i2.next();
            if (pl.isDeleted()) {
                System.out.println("\tPlan " + name + " [DELETED]");
            } else if (name.equals(pl.getDescription()) && pl.isResourceAvailable("service_domain") != null) {
                sysPlan = pl;
                System.out.println("\tPlan " + name + " [FOUND]");
                break;
            }
        }
        if (sysPlan == null) {
            AdmDNSZone admZone = AdmDNSZone.getByName(domain);
            AdmInstantAlias al = null;
            Iterator j = admZone.getAliases().iterator();
            while (true) {
                if (!j.hasNext()) {
                    break;
                }
                AdmInstantAlias alTmp = (AdmInstantAlias) j.next();
                if (alTmp.getTag() == 2) {
                    al = alTmp;
                    break;
                }
            }
            if (al == null) {
                throw new Exception("Instant alias with tag 2 isn`t exist in zone " + domain);
            }
            System.out.print("\tPlan " + name);
            try {
                sysPlan = createSystemPlan(name, Integer.toString(al.getTag()), al.getPrefix(), domain);
                System.out.println(" [CREATED]");
            } catch (Exception ex) {
                System.out.println(" [FAILED]");
                throw ex;
            }
        }
        return createSystemAccount(sysUser, accountEl, sysPlan, domain);
    }

    public Account createSystemAccount(User sysUser, Element accountEl, Plan sysPlan, String domain) throws Exception {
        Hashtable initval = new Hashtable();
        NodeList infos = accountEl.getElementsByTagName("info");
        for (int i = 0; i < infos.getLength(); i++) {
            Element info = (Element) infos.item(i);
            String prefix = info.getAttributes().getNamedItem("prefix").getNodeValue();
            NodeList items = info.getElementsByTagName("item");
            for (int j = 0; j < items.getLength(); j++) {
                Node item = items.item(j);
                String name = prefix + item.getAttributes().getNamedItem("name").getNodeValue();
                String value = "";
                if (item.getFirstChild() != null) {
                    value = item.getFirstChild().getNodeValue();
                }
                initval.put(name, new String[]{value});
            }
        }
        initval.put("login", new String[]{sysUser.getLogin()});
        initval.put("password", new String[]{sysUser.getPassword()});
        initval.put("type_domain", new String[]{"service_domain"});
        initval.put("domain_name", new String[]{domain});
        initval.put("_mod", new String[]{"service"});
        Session.setRequest(new FakeRequest(initval));
        SignupManager.saveRequest(Session.getRequest());
        System.out.print("\t\tSystem account for domain " + sysPlan.getDescription());
        try {
            BillingInfoObject bi = new BillingInfoObject(new NameModifier("_bi_"));
            ContactInfoObject ci = new ContactInfoObject(new NameModifier("_ci_"));
            Account acc = sysUser.addAccount(sysPlan.getId(), bi, ci, "Account", "service", 0);
            System.out.println(" [CREATED]");
            return acc;
        } catch (Exception ex) {
            System.out.println(" [FAILED]");
            throw ex;
        }
    }

    public static Plan createSystemPlan(String planName, String aliasTag, String aliasPrefix, String serviceZone) throws Exception {
        Hashtable initValues = new Hashtable();
        initValues.put("plan_name", new String[]{planName});
        initValues.put("homedir", new String[]{HostEntry.UnixUserDefaultHomeDir});
        initValues.put("mixedip", new String[]{"shared"});
        initValues.put("trial", new String[]{"0"});
        initValues.put("shared_ip_tag", new String[]{aliasTag});
        initValues.put("i_service_domain", new String[]{"on"});
        initValues.put("i_domain", new String[]{"on"});
        initValues.put("i_cgi", new String[]{"on"});
        initValues.put("i_cgidir", new String[]{"on"});
        initValues.put("i_frontpage", new String[]{"on"});
        initValues.put("i_ssi", new String[]{"on"});
        initValues.put("i_php3", new String[]{"on"});
        initValues.put("i_redirect_url", new String[]{"on"});
        initValues.put("i_transferlog", new String[]{"on"});
        initValues.put("i_referrerlog", new String[]{"on"});
        initValues.put("i_agentlog", new String[]{"on"});
        initValues.put("i_errorlog", new String[]{"on"});
        initValues.put("i_webalizer", new String[]{"on"});
        initValues.put("i_modlogan", new String[]{"on"});
        initValues.put("", new String[]{"on"});
        initValues.put("calias", new String[]{"." + aliasPrefix + "NNN." + serviceZone});
        initValues.put("stopgapalias", new String[]{"." + aliasPrefix + "NNN." + serviceZone});
        initValues.put("i_responder", new String[]{"on"});
        initValues.put("i_mailbox_alias", new String[]{"on"});
        initValues.put("i_mail_forward", new String[]{"on"});
        initValues.put("i_mailing_list", new String[]{"on"});
        initValues.put("f_traffic", new String[]{"5"});
        initValues.put("f_quota", new String[]{"20"});
        initValues.put("f_mail_quota", new String[]{"10"});
        initValues.put("wizard", new String[]{"unix"});
        initValues.put("unix_hosting", new String[]{"1"});
        initValues.put("mail", new String[]{"3"});
        initValues.put("mysql", new String[]{"4"});
        initValues.put("pgsql", new String[]{"18"});
        initValues.put("mssql", new String[]{"15"});
        User admUser = User.getUser(FMACLManager.ADMIN);
        User oldUser = Session.getUser();
        try {
            Session.setUser(admUser);
            Session.setRequest(new FakeRequest(initValues));
            Plan system = PlanCreator.process();
            system.FM_accessChange("3,");
            Session.setRequest(null);
            Session.setUser(oldUser);
            return system;
        } catch (Throwable th) {
            Session.setRequest(null);
            Session.setUser(oldUser);
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    public void processPhysical(Element phEl, PhysicalServer ph) throws Exception {
        String name = phEl.getAttributes().getNamedItem("name").getNodeValue();
        String ip1 = phEl.getAttributes().getNamedItem("ip1").getNodeValue();
        String mask1 = phEl.getAttributes().getNamedItem("mask1").getNodeValue();
        String ip2 = phEl.getAttributes().getNamedItem("ip2").getNodeValue();
        String mask2 = phEl.getAttributes().getNamedItem("mask2").getNodeValue();
        String login = phEl.getAttributes().getNamedItem("login").getNodeValue();
        String password = phEl.getAttributes().getNamedItem("password").getNodeValue();
        System.out.print("Physical server " + name + " with IP1 = " + ip1 + " MASK1 = " + mask1);
        try {
            if (ph == null) {
                ph = PhysicalServer.create(name, ip1, mask1, ip2, mask2, login, password, login == "" ? 1 : 2);
                System.out.println(" [CREATED]");
            } else {
                ph.FM_update(ip1, mask1, ip2, mask2, login, password);
                System.out.println(" [UPDATED]");
            }
        } catch (Exception ex) {
            System.out.println(" [FAILED]");
            ex.printStackTrace();
        }
        NodeList logicals = phEl.getElementsByTagName("logical");
        Hashtable existLogicals = new Hashtable();
        if (logicals.getLength() > 0) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT DISTINCT id FROM l_server WHERE p_server_id = ?");
                ps.setLong(1, ph.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    LogicalServer lg = LogicalServer.get(rs.getLong(1));
                    existLogicals.put(lg.getName(), lg);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        for (int i = 0; i < logicals.getLength(); i++) {
            Element lgEl = (Element) logicals.item(i);
            String lName = lgEl.getAttributes().getNamedItem("name").getNodeValue();
            processLogical(lgEl, (LogicalServer) existLogicals.get(lName), ph);
        }
    }

    /* JADX WARN: Finally extract failed */
    public void processLogical(Element lgEl, LogicalServer lg, PhysicalServer ph) throws Exception {
        String name = lgEl.getAttributes().getNamedItem("name").getNodeValue();
        String description = lgEl.getAttributes().getNamedItem("description").getNodeValue();
        String type = lgEl.getAttributes().getNamedItem("type").getNodeValue();
        int typeId = "win2k".equals(type) ? 2 : 1;
        String group = lgEl.getAttributes().getNamedItem("group").getNodeValue();
        String allowSignup = lgEl.getAttributes().getNamedItem("allowSignup").getNodeValue();
        int signup = "no".equals(allowSignup) ? 0 : 1;
        String fileServer = lgEl.getAttributes().getNamedItem("fileServer").getNodeValue();
        String path = lgEl.getAttributes().getNamedItem("path").getNodeValue();
        System.out.print("\tLogical server " + name + " with group = " + group);
        try {
            if (lg == null) {
                int groupId = groupIdByName(group);
                if (!ph.getGroups().contains(Integer.toString(groupId))) {
                    ph.FM_addGroup(groupId);
                }
                lg = LogicalServer.create(name, groupId, fileServer, description, path, typeId, ph.getId(), signup);
                HostManager.loadHosts(true);
                System.out.println(" [CREATED]");
            } else {
                System.out.println(" [SKIPPED]");
            }
        } catch (Exception ex) {
            System.out.println(" [FAILED]");
            ex.printStackTrace();
        }
        NodeList ips = lgEl.getElementsByTagName("ip");
        List existIPs = new ArrayList();
        if (ips.getLength() > 0) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT DISTINCT ip,mask,flag FROM l_server_ips WHERE l_server_id = ?");
                ps.setLong(1, lg.getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    existIPs.add(new IPm(rs.getString(1), rs.getString(2), rs.getInt(3)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        for (int i = 0; i < ips.getLength(); i++) {
            Element ipEl = (Element) ips.item(i);
            processIP(ipEl, lg, existIPs);
        }
    }

    public void processIP(Element ipEl, LogicalServer lg, List existIPs) throws Exception {
        String ip = ipEl.getAttributes().getNamedItem("ip").getNodeValue();
        String mask = ipEl.getAttributes().getNamedItem("mask").getNodeValue();
        String flag = ipEl.getAttributes().getNamedItem("type").getNodeValue();
        System.out.print("\t\tIP " + ip + " with flag = " + flag);
        try {
            int idFlag = ipFlagByName(flag);
            if (!existIPs.contains(new IPm(ip, mask, idFlag))) {
                lg.FM_addIPRange(ip, ip, mask, idFlag);
                System.out.println(" [CREATED]");
            } else {
                System.out.println(" [SKIPPED]");
            }
        } catch (Exception ex) {
            System.out.println(" [FAILED]");
            ex.printStackTrace();
        }
    }

    public static int groupIdByName(String name) throws Exception {
        if ("web".equals(name)) {
            return 1;
        }
        if ("dns".equals(name)) {
            return 2;
        }
        if ("mail".equals(name)) {
            return 3;
        }
        if ("mysql".equals(name)) {
            return 4;
        }
        if ("win".equals(name)) {
            return 5;
        }
        if ("real".equals(name)) {
            return 6;
        }
        if ("win_real".equals(name)) {
            return 7;
        }
        if ("cp".equals(name)) {
            return 10;
        }
        if ("mssql".equals(name)) {
            return 15;
        }
        if ("pgsql".equals(name)) {
            return 18;
        }
        if ("mrtg".equals(name)) {
            return 25;
        }
        throw new Exception("Incompatible type of logical server group:" + name);
    }

    public static int ipFlagByName(String name) throws Exception {
        if ("shared".equals(name)) {
            return 2;
        }
        if ("dedicated".equals(name)) {
            return 0;
        }
        if ("service".equals(name)) {
            return 4;
        }
        if ("dns".equals(name)) {
            return 5;
        }
        throw new Exception("Incompatible type of IP flag:" + name);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/tools/SetupCP$IPm.class */
    public class IPm {

        /* renamed from: ip */
        protected String f237ip;
        protected String mask;
        protected int flag;

        public IPm(String ip, String mask, int flag) {
            SetupCP.this = r4;
            this.f237ip = ip;
            this.mask = mask;
            this.flag = flag;
        }

        public boolean equals(Object obj) {
            IPm testIp = (IPm) obj;
            if (this.f237ip.equals(testIp.getIP()) && this.mask.equals(testIp.getMask()) && this.flag == testIp.getFlag()) {
                return true;
            }
            return false;
        }

        public String getIP() {
            return this.f237ip;
        }

        public String getMask() {
            return this.mask;
        }

        public int getFlag() {
            return this.flag;
        }
    }
}
