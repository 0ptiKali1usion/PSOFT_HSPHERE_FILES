package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.IIS.FTPVHostAnonymousResource;
import psoft.hsphere.resource.MixedIPResource;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.AdmInstantAlias;
import psoft.hsphere.resource.admin.AdmResellerSSL;
import psoft.hsphere.resource.admin.AdmServiceDNSRecord;
import psoft.hsphere.resource.admin.LogicalServer;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.ftp.FTPVHostResource;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/IPMigrator.class */
public class IPMigrator extends C0004CP {
    public static int IP_TYPE_ID = 8;
    public static int DOMAIN_ALIAS_A_RECORD = 6401;
    public static int FTP_VHOST = 2001;
    public static int FTP_VHOST_ANONYMOUS = 2002;
    public static int DNS_ZONE = 3001;
    public static int _3L_DNS_ZONE = 3004;
    public static int SERVICE_DNS_ZONE = 3005;
    private Hashtable ips;
    private List newIPs;

    public IPMigrator(Hashtable ips, List newIPs) throws Exception {
        super("psoft_config.hsphere");
        this.ips = null;
        this.newIPs = null;
        this.ips = ips;
        this.newIPs = newIPs;
    }

    public static void main(String[] args) throws Exception {
        try {
            StringBuffer keys = new StringBuffer("");
            for (String str : args) {
                keys.append(str);
            }
            if (keys.toString().indexOf("--help") != -1) {
                System.out.println("NAME:\n\t psoft.hsphere.tools.IPMigrator - H-Sphere IP migration utility");
                System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.IPMigrator [options] < ipmigration.xml");
                System.out.println("OPTIONS:");
                System.out.println("\t--help \t- shows this screen");
                System.out.println("\t--ftp-on \t- enable FTP");
                System.out.println("\t--ftp-off \t- disable FTP");
                System.out.println("\t--ftp-reset \t- reconfigure FTP");
                System.out.println("\t--ip-change \t-  process changing IP");
                System.out.println("\t--ip-depended \t-  process IP depended resources");
                System.out.println("\t--service-zone \t-  change Service zone server IP");
                System.out.println("\t--custom-rec \t-  process Custom DNS records");
                System.exit(0);
            }
            System.out.print("IP migration utility \n itializing ...");
            Hashtable ips = new Hashtable();
            List newIPs = new ArrayList();
            DOMParser parser = new DOMParser();
            parser.parse(new InputSource(System.in));
            Document doc = parser.getDocument();
            Element root = doc.getDocumentElement();
            NodeList list = root.getElementsByTagName("ip");
            for (int i = 0; i < list.getLength(); i++) {
                Element ipEl = (Element) list.item(i);
                String name = ipEl.getAttributes().getNamedItem("name").getNodeValue();
                String newip = ipEl.getAttributes().getNamedItem("new_ip").getNodeValue();
                String newmask = ipEl.getAttributes().getNamedItem("new_mask").getNodeValue();
                ips.put(name, C0015IP.createIP(newip, newmask));
                newIPs.add(name.trim());
                newIPs.add(newip.trim());
            }
            IPMigrator migrator = new IPMigrator(ips, newIPs);
            System.out.println(keys);
            migrator.processMigration(keys.toString());
            User admin = User.getUser(FMACLManager.ADMIN);
            Account adm = admin.getAccount(new ResourceId(1L, 0));
            Session.setUser(admin);
            Session.setAccount(adm);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    public void processMigration(String keys) throws Exception {
        if (keys.indexOf("--ftp-off") != -1 || keys.indexOf("--ftp-reset") != -1 || "".equals(keys)) {
            System.out.print("\tDisable FTP\t...");
            reconfigFTPconfig(true);
            System.out.println("\t[OK]");
        }
        if (keys.indexOf("--ip-change") != -1 || "".equals(keys)) {
            System.out.println("\tIP CORRESPONDENCE TABLE");
            Enumeration e = this.ips.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                System.out.print("\t" + key + " -> " + this.ips.get(key) + "\t...");
                try {
                    processIP(key, (C0015IP) this.ips.get(key));
                } catch (Exception ex) {
                    Session.getLog().error("Error process ip:" + key, ex);
                }
                System.out.println("\t[OK]");
            }
        }
        if (keys.indexOf("--ip-depend") != -1 || "".equals(keys)) {
            System.out.print("\tProcess IP depended\t...");
            reconfigIPDepended();
            System.out.println("\t[OK]");
        }
        if (keys.indexOf("--service-zone") != -1 || "".equals(keys)) {
            System.out.print("\tChange Service zone server IP\t...");
            reconfigServiceZones();
            System.out.println("\t[OK]");
        }
        if (keys.indexOf("--ftp-on") != -1 || keys.indexOf("--ftp-reset") != -1 || "".equals(keys)) {
            System.out.print("\tEnable FTP\t...");
            reconfigFTPconfig(false);
            System.out.println("\t[OK]");
        }
        if (keys.indexOf("--custom-rec") != -1 || "".equals(keys)) {
            System.out.print("Process Custom DNS Recordsi\t...");
            processCustomDNS();
            System.out.println("\t[OK]");
        }
    }

    protected void reconfigIPDepended() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT username FROM users");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        User u = User.getUser(rs.getString(1));
                        Session.setUser(u);
                        Iterator aci = u.getAccountIds().iterator();
                        while (aci.hasNext()) {
                            ResourceId aRid = (ResourceId) aci.next();
                            try {
                                Account newAccount = u.getAccount(aRid);
                                Session.setAccount(newAccount);
                                double oldCredit = newAccount.getBill().getCredit();
                                newAccount.getBill().setCredit(1000000.0d);
                                Session.getLog().debug("Account :" + newAccount);
                                try {
                                    try {
                                        processIPsForAccount(newAccount);
                                        newAccount.getBill().setCredit(oldCredit);
                                    } catch (Exception ex) {
                                        Session.getLog().error("Error in Account:" + newAccount, ex);
                                        newAccount.getBill().setCredit(oldCredit);
                                    }
                                } catch (Throwable th) {
                                    newAccount.getBill().setCredit(oldCredit);
                                    throw th;
                                }
                            } catch (Exception e) {
                                System.err.println(" -- Unable to get account:" + aRid.getId() + "\n");
                            }
                        }
                    } catch (Exception e2) {
                        System.err.println(" - Unable to get user:" + rs.getString(1) + "\n");
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e3) {
                e3.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    protected void processIPsForAccount(Account a) throws Exception {
        Collection<ResourceId> resIds = a.getChildManager().getAllResources();
        for (ResourceId carResId : resIds) {
            if (carResId.getType() == DOMAIN_ALIAS_A_RECORD) {
                Resource carRes = carResId.get();
                if (this.newIPs.contains(String.valueOf(carRes.get("data")))) {
                    carRes.delete(false);
                }
            }
            if (carResId.getType() == _3L_DNS_ZONE || carResId.getType() == DNS_ZONE || carResId.getType() == SERVICE_DNS_ZONE) {
                try {
                    Resource zone = carResId.get();
                    if (zone instanceof DNSZone) {
                        ((DNSZone) zone).postIPChanges(false);
                        Session.getLog().debug("UPDATE ZONE :" + String.valueOf(zone.get("name")));
                    }
                } catch (Exception ex) {
                    Session.getLog().error("DNS Zone update failed:", ex);
                }
            }
        }
        for (ResourceId ipResourceId : resIds) {
            if (ipResourceId.getType() == IP_TYPE_ID) {
                Resource ipRes = ipResourceId.get();
                Session.getLog().debug("Getting IP resource:" + ipRes.get("ip"));
                if (!this.newIPs.contains(String.valueOf(ipRes.get("ip")).trim())) {
                    Session.getLog().debug(ipRes.get("ip") + " skipped IPS:" + this.newIPs);
                } else {
                    Session.getLog().debug("IP " + ipRes.get("ip") + " is " + ipRes.get("shared"));
                    if ("1".equals(ipRes.get("shared").toString())) {
                        Resource ipResource = ipResourceId.get();
                        Resource parent = ipResource.getParent().get();
                        if (ipResource.FM_getChild("a_record") != null) {
                            ipResource.delete(false);
                            ArrayList args = new ArrayList();
                            args.add("0");
                            parent.addChild("ip", "shared", args);
                        } else {
                            ipResource.delete(false);
                            ArrayList args2 = new ArrayList();
                            args2.add("0");
                            parent.addChild("ip", "shard_no_a", args2);
                        }
                    }
                    ((MixedIPResource) ipRes).FM_reconfig();
                }
            }
        }
    }

    protected void reconfigFTPconfig(boolean clean) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT username FROM users");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    User u = User.getUser(rs.getString(1));
                    try {
                        Session.setUser(u);
                        Iterator aci = u.getAccountIds().iterator();
                        while (aci.hasNext()) {
                            try {
                                Account newAccount = u.getAccount((ResourceId) aci.next());
                                Session.setAccount(newAccount);
                                Session.getLog().debug("Account :" + newAccount);
                                try {
                                    processFTPForAccount(newAccount, clean);
                                } catch (Exception ex) {
                                    Session.getLog().error("Error in Account: " + newAccount.getId(), ex);
                                }
                            } catch (Exception e) {
                            }
                        }
                    } catch (Exception e2) {
                        System.err.println(" - Unable to get user:" + rs.getString(1) + "\n");
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e3) {
                e3.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void processFTPForAccount(Account a, boolean clean) throws Exception {
        Collection<ResourceId> resIds = a.getChildManager().getAllResources();
        for (ResourceId resId : resIds) {
            if (resId.getType() == FTP_VHOST) {
                if (clean) {
                    ((FTPVHostResource) resId.get()).clearConfig();
                } else {
                    ((FTPVHostResource) resId.get()).recoverConfig();
                }
            } else if (resId.getType() == FTP_VHOST_ANONYMOUS) {
                if (clean) {
                    ((FTPVHostAnonymousResource) resId.get()).clearConfig();
                } else {
                    ((FTPVHostAnonymousResource) resId.get()).recoverConfig();
                }
            }
        }
    }

    public void processIP(String oldIP, C0015IP newIP) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l_server_id, mask, flag, r_id, r_type FROM l_server_ips WHERE ip = ?");
            ps.setString(1, oldIP);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                switch (rs.getInt(3)) {
                    case 0:
                    case 1:
                        processDedicated(rs.getInt(1), oldIP, rs.getInt(4), rs.getInt(5), newIP);
                        break;
                    case 2:
                    case 3:
                    case 4:
                    case 5:
                    case 7:
                    default:
                        processIP(rs.getInt(1), oldIP, rs.getInt(3), newIP);
                        break;
                    case 6:
                        processBusyDNSResellerIP(rs.getInt(1), oldIP, rs.getInt(3), newIP);
                        break;
                    case 8:
                        processBusyCPResellerIP(rs.getInt(1), oldIP, rs.getInt(3), newIP);
                        break;
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

    protected void processDedicated(int lserverId, String ip, int rId, int rType, C0015IP newIP) throws Exception {
        System.out.print("DEDICATED");
        LogicalServer ls = LogicalServer.get(lserverId);
        if (rId != 0) {
            ResourceId ipResourceId = new ResourceId(rId, rType);
            if (ipResourceId != null) {
                Account a = ipResourceId.getAccount();
                Session.setUser(a.getUser());
                Session.setAccount(a);
                double oldCredit = a.getBill().getCredit();
                a.getBill().setCredit(1000000.0d);
                try {
                    Resource ipResource = ipResourceId.get();
                    Resource parent = ipResource.getParent().get();
                    if (ipResource.FM_getChild("a_record") != null) {
                        ipResource.delete(false);
                        ls.FM_delIPRange(ip, ip, 0);
                        ls.FM_addIPRange(newIP.toString(), newIP.toString(), newIP.getMask(), 0);
                        ArrayList args = new ArrayList();
                        args.add("1");
                        args.add(newIP.toString());
                        parent.addChild("ip", "dedicated", args);
                    } else {
                        ipResource.delete(false);
                        ls.FM_delIPRange(ip, ip, 0);
                        ls.FM_addIPRange(newIP.toString(), newIP.toString(), newIP.getMask(), 0);
                        ArrayList args2 = new ArrayList();
                        args2.add("1");
                        args2.add(newIP.toString());
                        parent.addChild("ip", "dedic_no_a", args2);
                    }
                    ((MixedIPResource) ipResource).FM_reconfig();
                    a.getBill().setCredit(oldCredit);
                    return;
                } catch (Throwable th) {
                    a.getBill().setCredit(oldCredit);
                    throw th;
                }
            }
            return;
        }
        ls.FM_delIPRange(ip, ip, 0);
        ls.FM_addIPRange(newIP.toString(), newIP.toString(), newIP.getMask(), 0);
    }

    protected void processIP(int lserverId, String ip, int flag, C0015IP newIP) throws Exception {
        System.out.println("flag = " + flag);
        LogicalServer ls = LogicalServer.get(lserverId);
        ls.FM_delIPRange(ip, ip, flag);
        ls.FM_addIPRange(newIP.toString(), newIP.toString(), newIP.getMask(), flag);
    }

    protected void processBusyDNSResellerIP(int lserverId, String ip, int flag, C0015IP newIP) throws Exception {
        System.out.println("flag = " + flag);
        LogicalServer ls = LogicalServer.get(lserverId);
        ls.FM_delIPRange(ip, ip, flag);
        ls.FM_addIPRange(newIP.toString(), newIP.toString(), newIP.getMask(), 5);
    }

    protected void processBusyCPResellerIP(int lserverId, String ip, int flag, C0015IP newIP) throws Exception {
        System.out.println("flag = " + flag);
        LogicalServer ls = LogicalServer.get(lserverId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        long sslId = 0;
        try {
            ps = con.prepareStatement("SELECT id FROM reseller_ssl WHERE ip = ?");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sslId = rs.getLong(1);
            }
            Session.closeStatement(ps);
            con.close();
            AdmResellerSSL resSSL = AdmResellerSSL.get(sslId);
            resSSL.repostConfig(newIP.toString());
            resSSL.changeDBIP(newIP.toString());
            ls.FM_delIPRange(ip, ip, flag);
            ls.FM_addIPRange(newIP.toString(), newIP.toString(), newIP.getMask(), 7);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
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
                ipsSelect = ipsSelect + "'" + ((String) e.nextElement()) + "'";
                if (e.hasMoreElements()) {
                    ipsSelect = ipsSelect + ",";
                }
            }
            String ipsSelect2 = ipsSelect + ")";
            ps = con.prepareStatement("SELECT e.id, d.data, d.name, e.zone_id, d.type, d.ttl, d.pref, e.type_rec  FROM e_dns_records e, dns_records d  WHERE e.id = d.id AND alias_id IS NULL  AND d.data IN " + ipsSelect2);
            System.out.println("IP in " + ipsSelect2);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                AdmDNSZone ezone = null;
                try {
                    ezone = AdmDNSZone.get(rs.getLong(4));
                    newIP = this.ips.get(rs.getString(2)).toString();
                    name = rs.getString(3);
                } catch (Exception ex) {
                    System.err.println("Error fix dns record DNSZONE:" + ezone.getName() + " IP:" + rs.getString(2));
                    Session.getLog().error("Error fix dns record DNSZONE:" + ezone.getName() + " IP:" + rs.getString(2), ex);
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
                System.out.println("The DNS Record has been fixed DNSZONE:" + ezone.getName() + " IP:" + rs.getString(2) + " new " + newIP);
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
                    for (AdmDNSZone zone : AdmDNSZone.getZones()) {
                        for (AdmInstantAlias al : zone.getAliases()) {
                            Enumeration e = this.ips.keys();
                            while (e.hasMoreElements()) {
                                String ip = (String) e.nextElement();
                                if (e.hasMoreElements()) {
                                    al.delRecord(ip);
                                }
                            }
                            al.createAllDNSRecords();
                        }
                        try {
                            zone.postIPChanges(false);
                        } catch (Exception ex) {
                            System.err.println("Error posting changed data to zone:" + zone.getName());
                            Session.getLog().error("DNS Zone update failed", ex);
                        }
                    }
                    Session.setResellerId(1L);
                } catch (Throwable th) {
                    Session.setResellerId(1L);
                    throw th;
                }
            } catch (Exception ex2) {
                Session.getLog().error("DNS Zone update failed:", ex2);
                Session.setResellerId(1L);
            }
        }
    }
}
