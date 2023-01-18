package psoft.hsphere.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import javax.xml.transform.TransformerFactoryConfigurationError;
import org.apache.regexp.RE;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;
import psoft.hsinst.Hsinst;
import psoft.hsinst.boxes.ClusterPreparer;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HostList;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.ResourceTypeList;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.exception.UnableAddAdmInstantAliasException;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.PhysicalServer;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.AdmInstantAlias;
import psoft.hsphere.resource.admin.AdmResellerSSL;
import psoft.hsphere.resource.admin.AdmServiceDNSRecord;
import psoft.hsphere.resource.admin.EnterpriseManager;
import psoft.hsphere.resource.admin.LServerIP;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.hsphere.resource.admin.ResellerCpAlias;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/IPMigratorFast.class */
public class IPMigratorFast extends C0004CP {
    private Hashtable<String, C0015IP> ips;
    private Hashtable<String, String> ipsReverced;

    /* renamed from: hl */
    private HostList f228hl;
    private ToolLogger logger;
    public static ResourceTypeList hostIPDepTypes;
    protected Hashtable<Long, RepostThread> threadList;

    public IPMigratorFast() throws Exception {
        super("psoft_config.hsphere");
        this.ips = null;
        this.ipsReverced = null;
        this.f228hl = null;
        this.threadList = new Hashtable<>();
    }

    protected void initValues(Hashtable<String, C0015IP> ips, Hashtable<String, String> ipsReversed, ToolLogger logger, String serverIds) throws Exception {
        this.ips = ips;
        this.ipsReverced = ipsReversed;
        this.logger = logger;
        if (serverIds != null && serverIds.length() > 0) {
            this.f228hl = new HostList(serverIds);
        }
        hostIPDepTypes = HsphereToolbox.getTypeListByInterface(IPDependentResource.class);
    }

    public static void printHelp() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.IPMigratorFast - H-Sphere IP migration utility");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.IPMigratorFast [options] ipmigration.xml");
        System.out.println("OPTIONS:");
        System.out.println("\t--help \t- shows this screen");
        System.out.println("\t--ip-change \t-  change IP");
        System.out.println("\t--repost-configs \t-  repost IP dependent resources");
        System.out.println("\t--recreate-zone \t-  change and repost DNS records");
        System.out.println("\t--service-zone \t-  change service zone server IP");
        System.out.println("\t--custom-rec \t-  process service DNS records");
        System.out.println("\t--lServerIds=<id1>,<id2>,...,<idN> \t- to specify logical server ids");
        System.out.println("\t--repost-cp-ssl \t-  Repost SSL CP VHost configs");
        System.out.println("\t--change-mail-relays \t- Change mail relay IPs in database and on physical box");
        System.out.println("\t--clear-old-ips \t-  remove old ips from database and servers");
    }

    public static void main(String[] args) throws Exception {
        final ToolLogger logger = new ToolLogger(args);
        try {
            IPMigratorFast migrator = new IPMigratorFast();
            HashMap<String, String> keys = new HashMap<>();
            for (String arg : args) {
                if (arg.startsWith("--")) {
                    StringTokenizer commands = new StringTokenizer(arg, "=");
                    String command = "";
                    String value = "";
                    if (commands.hasMoreTokens()) {
                        command = commands.nextToken();
                    }
                    if (commands.hasMoreTokens()) {
                        value = commands.nextToken();
                    }
                    if (command != null && !"".equals(command)) {
                        keys.put(command, value);
                    }
                    logger.outMessage(command + "=" + value + '\n');
                }
            }
            if (keys.toString().indexOf("--help") != -1 || args.length <= 0) {
                printHelp();
                System.exit(0);
            }
            logger.outMessage("IP migration utility \n itializing ...\n");
            Hashtable<String, C0015IP> ips = new Hashtable<>();
            Hashtable<String, String> ipsReverced = new Hashtable<>();
            DOMParser parser = new DOMParser();
            String fileName = args[args.length - 1];
            try {
                parser.setFeature("http://xml.org/sax/features/validation", true);
                parser.setErrorHandler(new DefaultHandler() { // from class: psoft.hsphere.tools.IPMigratorFast.1
                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void error(SAXParseException e) throws SAXException {
                        logger.outFail("Unable to parse xml file " + e.getMessage() + " Line:" + e.getLineNumber() + " Column:" + e.getColumnNumber());
                        throw e;
                    }

                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void fatalError(SAXParseException e) throws SAXException {
                        logger.outFail("Unable to parse xml file " + e.getMessage() + " Line:" + e.getLineNumber() + " Column:" + e.getColumnNumber());
                        throw e;
                    }

                    @Override // org.xml.sax.helpers.DefaultHandler, org.xml.sax.ErrorHandler
                    public void warning(SAXParseException e) {
                        logger.outWarn("Warning during parse xml file " + e.getMessage() + " Line:" + e.getLineNumber() + " Column:" + e.getColumnNumber());
                    }
                });
            } catch (Exception e) {
                logger.outFail("Parser error : ", e);
                System.exit(1);
            } catch (TransformerFactoryConfigurationError transformerFactoryConfigurationError) {
                logger.outFail("Error: " + transformerFactoryConfigurationError);
                System.exit(1);
            }
            try {
                parser.parse(new InputSource(new FileInputStream(fileName)));
            } catch (FileNotFoundException filenotfoundexception) {
                logger.outFail("File not found " + fileName + " " + filenotfoundexception);
                System.exit(1);
            } catch (Exception e2) {
                System.exit(1);
            }
            Document doc = parser.getDocument();
            Element root = doc.getDocumentElement();
            NodeList list = root.getElementsByTagName("ip");
            RE ipRE = new RE("^(([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([1-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-4])$");
            RE maskRE = new RE("^255\\.(([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){2}([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])$");
            for (int i = 0; i < list.getLength(); i++) {
                Element ipEl = (Element) list.item(i);
                String name = ipEl.getAttributes().getNamedItem("name").getNodeValue();
                String newip = ipEl.getAttributes().getNamedItem("new_ip").getNodeValue();
                String newmask = ipEl.getAttributes().getNamedItem("new_mask").getNodeValue();
                if (!ipRE.match(name.trim())) {
                    logger.outFail("Old IP Address " + name + " isn't valid\n");
                    System.exit(1);
                }
                if (!ipRE.match(newip.trim())) {
                    logger.outFail("New IP Address " + newip + " isn't valid\n");
                    System.exit(1);
                }
                if (!maskRE.match(newmask.trim())) {
                    logger.outFail("Netmask " + newmask + " isn't valid\n");
                    System.exit(1);
                }
                if (ipsReverced.containsKey(newip)) {
                    logger.outFail("IP address " + newip + " is duplicated\n");
                    System.exit(1);
                }
                if (ipsReverced.containsValue(name)) {
                    logger.outFail("IP address " + name + " is duplicated\n");
                    System.exit(1);
                }
                ips.put(name, new C0015IP(newip, newmask));
                ipsReverced.put(newip, name);
            }
            migrator.initValues(ips, ipsReverced, logger, keys.get("--lServerIds"));
            System.out.println(keys);
            migrator.processMigration(keys);
            User admin = User.getUser(1L);
            Account adm = admin.getAccount(new ResourceId(1L, 0));
            Session.setUser(admin);
            Session.setAccount(adm);
        } catch (Exception e3) {
            logger.outFail("Error occured during IP Migration", e3);
            System.exit(1);
        }
        logger.outMessage("Migration finished\n");
        System.exit(0);
    }

    public void processMigration(HashMap keys) throws Exception {
        boolean processed = false;
        boolean force = keys.size() == 0;
        if (keys.keySet().contains("--lServerIds")) {
            force = keys.size() == 1;
        }
        if (keys.keySet().contains("--ip-change") || force) {
            this.logger.outMessage("\tIP CORRESPONDENCE TABLE\n");
            processed = true;
            this.logger.outMessage("PROCESSING IPCHANGE\n");
            Enumeration e = this.ips.keys();
            while (e.hasMoreElements()) {
                String key = e.nextElement();
                try {
                    processIP(key, this.ips.get(key));
                } catch (Exception ex) {
                    this.logger.outFail("An error occured while changing IPs. Aborting...", ex);
                    throw ex;
                }
            }
            try {
                changeWinFTPVHostAnonymousIPs();
            } catch (Exception ex2) {
                this.logger.outMessage("An error occured while changing Windows VFTP Anonymous IPs. Aborting...", ex2);
            }
        }
        if (keys.keySet().contains("--recreate-zone") || force) {
            this.logger.outMessage("\trecreate DNS\t...");
            processed = true;
            try {
                updateDNS();
                this.logger.outOK();
            } catch (Exception ex3) {
                this.logger.outFail("Error processing DNS", ex3);
            }
        }
        if (keys.keySet().contains("--repost-configs") || force) {
            processed = true;
            if (!keys.keySet().contains("--lServerIds") || this.f228hl != null) {
                this.logger.outMessage("\trepost configs for IP dependent resources\t...\n");
                repostConfigOnPhBox();
                repostIPDependend();
                this.logger.outOK();
            }
        }
        if (keys.keySet().contains("--service-zone") || force) {
            processed = true;
            try {
                this.logger.outMessage("\tChange Service zone server IP\t...");
                reconfigServiceZones();
                this.logger.outOK();
            } catch (Exception ex4) {
                this.logger.outFail(ex4.getMessage(), ex4);
            }
        }
        if (keys.keySet().contains("--custom-rec") || force) {
            processed = true;
            try {
                this.logger.outMessage("Process Custom DNS Records...");
                processCustomDNS();
                this.logger.outOK();
            } catch (Exception ex5) {
                this.logger.outFail(ex5.getMessage(), ex5);
            }
        }
        if (keys.keySet().contains("--repost-cp-ssl") || force) {
            processed = true;
            try {
                repostCPSSLVHostConfigs();
                this.logger.outOK();
            } catch (Exception ex6) {
                this.logger.outMessage("An error occured while reposting resellers' CP VHost configs. Aborting...", ex6);
            }
        }
        if (keys.keySet().contains("--change-mail-relays") || force) {
            processed = true;
            try {
                changeMailRelayIPs();
                this.logger.outOK();
            } catch (Exception ex7) {
                this.logger.outMessage("An error occured while changing mail relay IPs. Aborting...", ex7);
            }
        }
        if (keys.keySet().contains("--clear-old-ips")) {
            processed = true;
            try {
                this.logger.outMessage("Removing Old IPs...\n");
                clearIPs();
                this.logger.outOK();
            } catch (Exception ex8) {
                this.logger.outFail(ex8.getMessage(), ex8);
            }
        }
        if (!processed) {
            printHelp();
        }
    }

    protected void repostConfigOnPhBox() throws Exception {
        StringBuffer ipsSelect = new StringBuffer("(");
        Enumeration e = this.ipsReverced.keys();
        while (e.hasMoreElements()) {
            ipsSelect.append("'");
            ipsSelect.append(e.nextElement());
            ipsSelect.append("'");
            if (e.hasMoreElements()) {
                ipsSelect.append(" ,");
            }
        }
        ipsSelect.append(")");
        if (this.f228hl == null || this.f228hl.size() == 0) {
            Connection con1 = Session.getDb();
            PreparedStatement ps1 = null;
            try {
                try {
                    ps1 = con1.prepareStatement("SELECT distinct l.id FROM l_server l JOIN l_server_ips lsi ON lsi.l_server_id = l.id WHERE lsi.ip IN " + ipsSelect.toString());
                    ResultSet rs1 = ps1.executeQuery();
                    List<Long> lsIds = new ArrayList<>();
                    while (rs1.next()) {
                        lsIds.add(Long.valueOf(rs1.getLong(1)));
                    }
                    this.f228hl = new HostList(lsIds);
                    Session.closeStatement(ps1);
                    con1.close();
                } catch (Throwable th) {
                    Session.closeStatement(ps1);
                    con1.close();
                    throw th;
                }
            } catch (Exception e2) {
                throw new Exception("Failed to get migrated logical server ids", e2);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                HSphereConfigBuilder hsBuilder = new HSphereConfigBuilder();
                hsBuilder.build();
                Hsinst hsinst = hsBuilder.getHsinst();
                ClusterPreparer cp = new ClusterPreparer(hsinst);
                ps = con.prepareStatement("SELECT distinct p.id, p.ip1, p.name FROM p_server p JOIN l_server l ON p.id = l.p_server_id JOIN l_server_ips lsi ON lsi.l_server_id = l.id WHERE lsi.ip IN " + ipsSelect.toString());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String psIP = rs.getString(2);
                    try {
                        this.logger.outMessage("\t\tRe-posting config.xml for server with IP " + psIP);
                        cp.publishConfigToServer(psIP);
                        PhysicalServer curPs = PhysicalServer.getPServer(rs.getLong(1));
                        curPs.reconfigServices();
                        this.logger.outOK();
                    } catch (Exception e3) {
                        this.logger.outFail();
                        this.logger.outMessage("\t\tFailed to repost config.xml file on server " + rs.getString(3) + "\n" + e3.toString());
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                this.logger.outMessage("Failed to repost config.xml file", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    protected void repostIPDependend() throws Exception {
        boolean worked;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT child_id, child_type, u.account_id, user_id  FROM parent_child  p, user_account u WHERE child_type  IN (" + hostIPDepTypes.toString() + ") AND u.account_id = p.account_id  AND EXISTS (SELECT * FROM accounts WHERE p.account_id = id)  ORDER BY u.account_id");
                List<ResourceId> resourceList = new ArrayList<>();
                ResourceId accountId = null;
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    if (accountId == null) {
                        accountId = new ResourceId(rs.getLong(3), 0);
                    }
                    if (accountId != null && (!rs.getString(3).equals(String.valueOf(accountId.getId())) || rs.isLast())) {
                        if (rs.isLast()) {
                            if (!rs.getString(3).equals(String.valueOf(accountId.getId()))) {
                                accountId = new ResourceId(rs.getLong(3), 0);
                                resourceList.clear();
                            }
                            ResourceId rid = new ResourceId(rs.getLong(1), rs.getInt(2));
                            resourceList.add(rid);
                        }
                        if (resourceList.size() > 0) {
                            addAction(accountId, resourceList);
                        }
                        accountId = new ResourceId(rs.getLong(3), 0);
                        resourceList.clear();
                    }
                    ResourceId rid2 = new ResourceId(rs.getLong(1), rs.getInt(2));
                    resourceList.add(rid2);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                this.logger.outMessage("Failed to repost IP dependent config files", e);
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
            do {
                TimeUtils.sleep(1000L);
                worked = false;
                synchronized (this.threadList) {
                    Collection threads = this.threadList.values();
                    for (Object thread1 : threads) {
                        RepostThread thread = (RepostThread) thread1;
                        worked = thread.isWorked();
                    }
                }
            } while (worked);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void addAction(ResourceId accountId, List resIds) throws Exception {
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            try {
                Account newAccount = (Account) Account.get(accountId);
                Session.setAccount(newAccount);
                Session.setUser(newAccount.getUser());
                for (Object resId1 : resIds) {
                    ResourceId resId = (ResourceId) resId1;
                    if (hostIPDepTypes.contains(Integer.valueOf(resId.getType()))) {
                        Resource res = resId.get();
                        if (res != null) {
                            if ((res instanceof IPDependentResource) && (res instanceof HostDependentResource)) {
                                long hostId = ((HostDependentResource) res).getHostId();
                                if (this.f228hl == null || this.f228hl.size() <= 0 || this.f228hl.contains(hostId)) {
                                    synchronized (this.threadList) {
                                        RepostThread thrd = this.threadList.get(new Long(hostId));
                                        if (thrd == null) {
                                            thrd = new RepostThread(this, hostId);
                                            thrd.start();
                                            this.threadList.put(Long.valueOf(hostId), thrd);
                                        }
                                        thrd.addAction(new RepostAction(accountId, res));
                                    }
                                }
                            }
                        }
                    }
                }
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            } catch (Exception ex) {
                System.err.println("Unable to repost account:" + accountId + "\t[FAILED]");
                Session.getLog().error("Unable to repost account:" + accountId, ex);
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            }
        } catch (Throwable th) {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
            throw th;
        }
    }

    public void updateDNS() throws Exception {
        this.logger.outMessage("UPDATING DNS RECORDS");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            try {
                String ipsSelect = "(";
                Enumeration e = this.ips.keys();
                while (e.hasMoreElements()) {
                    ipsSelect = ipsSelect + "'" + ((Object) e.nextElement()) + "'";
                    if (e.hasMoreElements()) {
                        ipsSelect = ipsSelect + ",";
                    }
                }
                List<String> failedZones = new ArrayList<>();
                String query = "SELECT a.id, a.name, a.type, a.ttl, a.data, a.pref, d.master, d.name FROM dns_records a, dns_zones d WHERE (a.name LIKE '%.'||d.name OR a.name = d.name) AND a.data IN " + (ipsSelect + ")") + " AND NOT EXISTS (SELECT * FROM e_dns_records e WHERE a.id = e.id)";
                ps = con.prepareStatement(query);
                ps2 = con.prepareStatement("UPDATE dns_records SET data = ? WHERE id = ?");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    long masterId = rs.getLong(7);
                    HostEntry host = HostManager.getHost(masterId);
                    String newIp = this.ips.get(rs.getString(5)).toString();
                    if (newIp != null) {
                        try {
                            ps2.setString(1, newIp);
                            ps2.setLong(2, rs.getLong(1));
                            ps2.executeUpdate();
                            try {
                                DNSServices.deleteFromZone(host, rs.getString(8), rs.getString(2), rs.getString(3), rs.getString(6), rs.getString(5));
                                DNSServices.addToZone(host, rs.getString(8), rs.getString(2), rs.getString(3), rs.getString(4), newIp, rs.getString(6), false);
                            } catch (Exception e2) {
                                failedZones.add(rs.getString(8));
                            }
                        } catch (Exception ex) {
                            System.out.print("New DNS record:" + rs.getString(8) + " name:" + rs.getString(2) + " type:" + rs.getString(3) + " ttl:" + rs.getString(4) + " data:" + rs.getString(5) + " pref:" + rs.getString(6));
                            Session.getLog().error("Unable to update dns record:" + rs.getString(1), ex);
                            System.out.println("[FAILED]");
                        }
                    }
                }
                if (failedZones.size() > 0) {
                    recreateZones(failedZones);
                }
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                con.close();
                this.logger.outMessage("FINISHED UPDATING DNS RECORDS");
            } catch (Exception e3) {
                e3.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                con.close();
                this.logger.outMessage("FINISHED UPDATING DNS RECORDS");
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps2);
            con.close();
            this.logger.outMessage("FINISHED UPDATING DNS RECORDS");
            throw th;
        }
    }

    public void recreateZones(List failedZones) throws Exception {
        for (Object failedZone : failedZones) {
            String zoneName = (String) failedZone;
            if (zoneName != null && !"".equals(zoneName)) {
                try {
                    DNSCreator.m17go(1, true, zoneName);
                } catch (Exception e) {
                    System.err.println("Unable to repost DNS zone:" + zoneName + "\t[FAILED]");
                }
            }
        }
    }

    public void processIP(String oldIP, C0015IP newIP) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l_server_id, flag  FROM l_server_ips  WHERE ip = ?");
            ps.setString(1, oldIP);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                LogicalServer ls = LogicalServer.get(rs.getLong("L_server_id"));
                LServerIP lsIP = ls.getIp(oldIP, rs.getInt("flag"));
                try {
                    this.logger.outMessage("Server " + lsIP.getLserverId() + ":\t" + lsIP.getIp().toString() + "\t->\t" + newIP.toString());
                    processIPChange(lsIP, newIP);
                    this.logger.outOK();
                } catch (UnableAddAdmInstantAliasException aliasEx) {
                    this.logger.outWarn("See H-Sphere logs for details", aliasEx);
                } catch (Exception ex) {
                    this.logger.outFail("", ex);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void processIPChange(LServerIP lsIP, C0015IP newIP) throws Exception {
        LogicalServer ls = LogicalServer.get(lsIP.getLserverId());
        LServerIP newLsIP = ls.getIp(newIP.toString(), lsIP.getFlag() < 0 ? (-1) * lsIP.getFlag() : lsIP.getFlag());
        if (newLsIP == null) {
            Session.getLog().debug("Trying to add new IP " + newIP.toString());
            newLsIP = ls.addIP(newIP.toString(), newIP.getMask(), lsIP.getFlag());
        }
        if (lsIP.getRid() != null) {
            boolean oldIpValid = lsIP.isIPValid();
            if (!oldIpValid) {
                lsIP.releaseIP();
            }
        }
        Session.getLog().debug("Swapping IPs " + lsIP.getIp() + " -> " + newLsIP.getIp());
        swapIPs(lsIP, newLsIP);
    }

    private void swapIPs(LServerIP oldIP, LServerIP newIP) throws Exception {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        Connection con = Session.isTransConnection() ? Session.getDb() : Session.getTransConnection();
        Session.getLog().debug("Inside swapping IPs " + oldIP.getIp() + " -> " + newIP.getIp());
        Session.getLog().debug("OLD IP=" + oldIP.getIp() + " rid=" + oldIP.getRid() + " flag=" + oldIP.getFlag() + " lserverId=" + oldIP.getLserverId());
        Session.getLog().debug("NEW IP=" + newIP.getIp() + " rid=" + newIP.getRid() + " flag=" + newIP.getFlag() + " lserverId=" + newIP.getLserverId());
        try {
            try {
                if (oldIP.getRid() != null) {
                    ps1 = con.prepareStatement("UPDATE l_server_ips SET r_id = ?, r_type = ? WHERE l_server_id = ? AND ip = ?");
                    ps1.setLong(1, oldIP.getRid().getId());
                    ps1.setInt(2, oldIP.getRid().getType());
                    ps1.setLong(3, oldIP.getLserverId());
                    ps1.setString(4, newIP.getIp().toString());
                }
                ps2 = con.prepareStatement("UPDATE l_server_ips SET flag = ? WHERE l_server_id = ? AND ip = ?" + (oldIP.getRid() == null ? "" : " AND r_id = ?"));
                ps2.setLong(1, oldIP.getFlag() > 0 ? (-1) * oldIP.getFlag() : oldIP.getFlag());
                ps2.setLong(2, oldIP.getLserverId());
                ps2.setString(3, oldIP.getIp().toString());
                if (oldIP.getRid() != null) {
                    ps2.setLong(4, oldIP.getRid().getId());
                }
                if (oldIP.getRid() != null) {
                    ps1.executeUpdate();
                }
                ps2.executeUpdate();
                oldIP.setFlag((-1) * oldIP.getFlag());
                con.commit();
                Session.commitTransConnection(con);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
            } catch (Exception e) {
                con.rollback();
                Session.commitTransConnection(con);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
            }
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            throw th;
        }
    }

    protected void processCustomDNS() throws Exception {
        String newIP;
        String name;
        String name2;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            String ipsSelect = "(";
            Enumeration e = this.ips.keys();
            while (e.hasMoreElements()) {
                ipsSelect = ipsSelect + "'" + ((Object) e.nextElement()) + "'";
                if (e.hasMoreElements()) {
                    ipsSelect = ipsSelect + ",";
                }
            }
            String ipsSelect2 = ipsSelect + ")";
            ps = con.prepareStatement("SELECT e.id, d.data, d.name, e.zone_id, d.type, d.ttl, d.pref, e.type_rec  FROM e_dns_records e, dns_records d  WHERE e.id = d.id AND alias_id IS NULL  AND d.data IN " + ipsSelect2);
            this.logger.outMessage("IP in " + ipsSelect2 + '\n');
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AdmDNSZone ezone = null;
                try {
                    ezone = AdmDNSZone.get(rs.getLong(4));
                    newIP = this.ips.get(rs.getString(2)).toString();
                    name = rs.getString(3);
                } catch (Exception ex) {
                    String zoneInfo = ezone != null ? ezone.getName() : " Zone ID: " + Long.toString(rs.getLong(4));
                    this.logger.outMessage("Error fix dns record DNSZONE:" + zoneInfo + " IP:" + rs.getString(2) + '\n', ex);
                }
                if (name.lastIndexOf("." + ezone.getName()) > 0) {
                    name2 = name.substring(0, name.lastIndexOf("." + ezone.getName()));
                } else if (name.equals(ezone.getName())) {
                    name2 = "";
                }
                if (rs.getInt(8) == 2) {
                    ezone.delCustomRecord(rs.getLong(1));
                    ezone.addCustRecord(newIP, name2, rs.getString(5), rs.getString(6), rs.getString(7));
                } else {
                    AdmServiceDNSRecord admSerRec = AdmServiceDNSRecord.get(rs.getLong(1));
                    if (admSerRec != null) {
                        long lServerId = admSerRec.getLServerId();
                        Session.getLog().debug("Now we are going to delete Service Record");
                        boolean isResellerCPSSL = false;
                        if (rs.getInt(8) == 4) {
                            isResellerCPSSL = true;
                            ezone.delServiceRecord(rs.getLong(1), true);
                        } else {
                            ezone.delServiceRecord(rs.getLong(1));
                        }
                        ezone.addServiceRecord(newIP, name2, rs.getString(5), rs.getString(6), rs.getString(7), lServerId, isResellerCPSSL);
                    }
                }
                this.logger.outMessage("The DNS Record has been fixed DNSZONE:" + ezone.getName() + " IP:" + rs.getString(2) + " new " + newIP + '\n');
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void reconfigServiceZones() throws Exception {
        for (Reseller resel : Reseller.getResellerList()) {
            try {
                try {
                    Session.setResellerId(resel.getId());
                    for (Object o : AdmDNSZone.getZones()) {
                        AdmDNSZone zone = (AdmDNSZone) o;
                        for (Object o1 : zone.getAliases()) {
                            AdmInstantAlias al = (AdmInstantAlias) o1;
                            Enumeration e = this.ips.keys();
                            while (e.hasMoreElements()) {
                                String ip = e.nextElement();
                                al.delRecord(ip);
                            }
                            al.createAllDNSRecords();
                        }
                        for (Object o2 : zone.getCpAliases()) {
                            ResellerCpAlias al2 = (ResellerCpAlias) o2;
                            Enumeration e2 = this.ips.keys();
                            while (e2.hasMoreElements()) {
                                String ip2 = e2.nextElement();
                                al2.delRecord(ip2);
                            }
                            al2.createAllDNSRecords();
                        }
                        try {
                            zone.postIPChanges(false);
                        } catch (Exception ex) {
                            System.err.println("Error posting changed data to zone:" + zone.getName());
                            Session.getLog().error("DNS Zone update failed", ex);
                        }
                    }
                    Session.setResellerId(1L);
                } catch (Exception ex2) {
                    Session.getLog().error("DNS Zone update failed:", ex2);
                    Session.setResellerId(1L);
                }
            } catch (Throwable th) {
                Session.setResellerId(1L);
                throw th;
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v13, types: [java.lang.String[], java.lang.String[][]] */
    public void changeWinFTPVHostAnonymousIPs() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        this.logger.outMessage("CHANGING IP FOR IIS VFTP ANONYMOUS\n");
        try {
            ps = con.prepareStatement("SELECT u.hostid, lip.ip FROM parent_child p1, parent_child p2, parent_child p3, unix_user u, l_server_ips lip WHERE p1.child_type = ? AND p1.parent_id = p2.child_id AND p3.parent_id = p2.child_id AND p3.child_type = ? AND p2.parent_id = u.id AND p3.child_id = lip.r_id");
            ps.setInt(1, 2002);
            ps.setInt(2, 8);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (this.ipsReverced.keySet().contains(rs.getString("ip")) && (this.f228hl == null || this.f228hl.size() <= 0 || this.f228hl.contains(rs.getLong("hostid")))) {
                    try {
                        String newIP = rs.getString("ip");
                        String ip = this.ipsReverced.get(newIP);
                        this.logger.outMessage("Server " + rs.getLong("hostid") + ":\t" + ip + "\t->\t" + newIP);
                        WinHostEntry whe = (WinHostEntry) HostManager.getHost(rs.getLong("hostid"));
                        whe.exec("net/changeIP-ftp.aspx", (String[][]) new String[]{new String[]{"oldIP", ip}, new String[]{"newIP", newIP}});
                        this.logger.outOK();
                    } catch (Exception ex) {
                        this.logger.outFail(ex.getMessage(), ex);
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
            this.logger.outMessage("FINISHED CHANGING IP FOR IIS VFTP ANONYMOUS\n");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            this.logger.outMessage("FINISHED CHANGING IP FOR IIS VFTP ANONYMOUS\n");
            throw th;
        }
    }

    public void repostCPSSLVHostConfigs() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        Connection con = Session.getDb();
        this.logger.outMessage("REPOSTING CP SSL VHOST CONFIGS\n");
        String ipsSelect = "(";
        Enumeration e = this.ips.keys();
        while (e.hasMoreElements()) {
            ipsSelect = ipsSelect + "'" + ((Object) e.nextElement()) + "'";
            if (e.hasMoreElements()) {
                ipsSelect = ipsSelect + ",";
            }
        }
        String ipsSelect2 = ipsSelect + ")";
        Session.save();
        try {
            ps = con.prepareStatement("SELECT rssl.id, lip.l_server_id, rssl.ip, rssl.reseller_id FROM l_server l INNER JOIN l_server_groups lg ON (l.group_id = lg.id) INNER JOIN l_server_ips lip ON (lip.l_server_id = l.id) INNER JOIN reseller_ssl rssl ON (lip.ip = rssl.ip) WHERE lg.type_id = 10 AND lip.ip IN " + ipsSelect2);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String oldIP = rs.getString("ip");
                if (this.f228hl == null || this.f228hl.size() == 0 || this.f228hl.contains(rs.getLong("l_server_id"))) {
                    try {
                        C0015IP newIP = this.ips.get(oldIP);
                        String ip = newIP.toString();
                        this.logger.outMessage("Server " + rs.getLong("l_server_id") + ":\t" + oldIP + "\t->\t" + ip);
                        Session.setResellerId(rs.getLong("reseller_id"));
                        AdmResellerSSL resSSL = AdmResellerSSL.get(rs.getLong("id"));
                        resSSL.repostConfig(ip);
                        resSSL.changeDBIP(ip);
                        ps1 = con.prepareStatement("SELECT r_id FROM l_server_ips WHERE ip = ? AND flag = ? LIMIT 1");
                        ps1.setString(1, ip);
                        ps1.setInt(2, 8);
                        ResultSet rs1 = ps1.executeQuery();
                        if (rs1.next()) {
                            resSSL.changeDBId(rs1.getLong("r_id"));
                        }
                        this.logger.outOK();
                    } catch (Exception ex) {
                        this.logger.outFail(ex.getMessage(), ex);
                    }
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            Session.restore();
            this.logger.outMessage("FINISHED REPOSTING CP SSL VHOST CONFIGS\n");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            Session.restore();
            this.logger.outMessage("FINISHED REPOSTING CP SSL VHOST CONFIGS\n");
            throw th;
        }
    }

    private void changeMailRelayIPs() throws Exception {
        this.logger.outMessage("CHANGING IP FOR MAIL RELAYS\n");
        String ipsSelect = "(";
        Enumeration e = this.ips.keys();
        while (e.hasMoreElements()) {
            ipsSelect = ipsSelect + "'" + ((Object) e.nextElement()) + "'";
            if (e.hasMoreElements()) {
                ipsSelect = ipsSelect + ",";
            }
        }
        String ipsSelect2 = ipsSelect + ")";
        EnterpriseManager em = null;
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT mserver_id, action, ip, note FROM admin_mail_relay WHERE ip IN " + ipsSelect2);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.isFirst()) {
                    em = new EnterpriseManager();
                }
                long mserverId = rs.getLong("mserver_id");
                String action = rs.getString("action");
                String oldIp = rs.getString("ip");
                String newIp = this.ips.get(oldIp).getIP();
                String note = rs.getString("note");
                try {
                    this.logger.outMessage("Updating mail relay with IP " + oldIp + ", mail server Id " + mserverId + ", action " + action + "...");
                    em.FM_delMailRelay(mserverId, action, oldIp);
                    em.FM_addMailRelay(mserverId, action, newIp, note);
                    this.logger.outMessage("\tOK\n");
                } catch (Exception e2) {
                    this.logger.outMessage("\tFailed due to " + e2.getMessage() + "\n");
                }
            }
            Session.closeStatement(ps);
            con.close();
            this.logger.outMessage("FINISHED CHANGING IP FOR MAIL RELAYS\n");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            this.logger.outMessage("FINISHED CHANGING IP FOR MAIL RELAYS\n");
            throw th;
        }
    }

    public void clearIPs() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        String ipsSelect = "(";
        Enumeration e = this.ips.keys();
        while (e.hasMoreElements()) {
            ipsSelect = ipsSelect + "'" + ((Object) e.nextElement()) + "'";
            if (e.hasMoreElements()) {
                ipsSelect = ipsSelect + ",";
            }
        }
        try {
            ps = con.prepareStatement("SELECT l_server_id, ip, flag FROM l_server_ips WHERE flag < 0 AND ip IN " + (ipsSelect + ")") + (this.f228hl == null ? "" : " AND l_server_id IN (" + this.f228hl.toString() + ")"));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    LogicalServer ls = LogicalServer.get(rs.getLong("l_server_id"));
                    ls.deleteIP(rs.getString("ip"), rs.getInt("flag"));
                    this.logger.outMessage("IP " + rs.getString("ip") + " with flag " + rs.getInt("flag") + " removed\n");
                } catch (Exception e2) {
                    this.logger.outMessage("IP " + rs.getString("ip") + " wasn't removed. " + e2 + "\n");
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
