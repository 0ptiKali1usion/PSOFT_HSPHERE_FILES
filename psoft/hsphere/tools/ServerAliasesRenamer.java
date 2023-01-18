package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.AdmResellerSSL;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ServerAliasesRenamer.class */
public class ServerAliasesRenamer extends C0004CP {

    /* renamed from: db */
    protected Database f235db;
    protected ResourceBundle config;
    protected List l_server_ids;
    protected List data;
    protected List ips;
    protected String xmlFileName;

    public ServerAliasesRenamer() throws Exception {
        super("psoft_config.hsphere");
        this.l_server_ids = new ArrayList();
        this.data = new ArrayList();
        this.ips = new ArrayList();
        this.config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        this.f235db = Toolbox.getDB(this.config);
    }

    public static void main(String[] args) {
        try {
            StringBuffer keys = new StringBuffer("");
            for (String str : args) {
                keys.append(str);
            }
            ServerAliasesRenamer test = new ServerAliasesRenamer();
            if (keys.toString().indexOf("--help") != -1) {
                test.printDescr();
            }
            System.out.println(keys);
            test.m5go(keys.toString(), args);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Server aliases rename proccess finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m5go(String keys, String[] args) throws Exception {
        if (keys.indexOf("--xml") != -1) {
            getIPsFromArgs(args);
            getDataForLServers();
            process();
        }
        if (keys.indexOf("--lserver") != -1) {
            getLServerIDsfromArgs(args);
            getData();
            processForLServer();
        }
    }

    private void processForLServer() throws Exception {
        for (int i = 0; i < this.data.size(); i++) {
            Hashtable oneAlias = (Hashtable) this.data.get(i);
            try {
                long lServerId = ((Integer) oneAlias.get("l_server_id")).longValue();
                String oldIP = new String();
                new String();
                int ipFlag = 0;
                String type = (String) oneAlias.get("type");
                if (type.equals("CNAME")) {
                    oldIP = getOldIPforCNAME(lServerId);
                    ipFlag = 4;
                } else if (type.equals("A")) {
                    long eDnsRecId = ((Long) oneAlias.get("e_dns_rec_id")).longValue();
                    oldIP = getOldIPforA(eDnsRecId);
                    ipFlag = getIPFlagByOldIP(oldIP);
                }
                String newIP = oldIP;
                mainActions(oneAlias, lServerId, oldIP, newIP, ipFlag);
            } catch (Exception e) {
                if (oneAlias.get("reseller_id") != null && oneAlias.get("e_dns_rec_id") != null) {
                    String errMess = "Error occured while processing: \nreseller -> " + oneAlias.get("reseller_id") + "\ne_dns_rec_id -> " + oneAlias.get("e_dns_rec_id") + "\n";
                    System.out.println(errMess);
                    e.printStackTrace();
                    System.out.println("");
                    Session.getLog().error(errMess, e);
                } else {
                    e.printStackTrace();
                    Session.getLog().error(e);
                }
            }
        }
    }

    private int getIPFlagByOldIP(String oldIP) throws Exception {
        int result = 0;
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT id FROM reseller_ssl WHERE ip = ?");
            ps2.setString(1, oldIP);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                result = 8;
                rs.close();
            } else {
                ps2 = con.prepareStatement("SELECT flag FROM l_server_ips WHERE ip = ?");
                ps2.setString(1, oldIP);
                ResultSet rs2 = ps2.executeQuery();
                while (true) {
                    if (!rs2.next()) {
                        break;
                    }
                    int tmp = rs2.getInt(1);
                    if (tmp == 6) {
                        result = tmp;
                        break;
                    }
                    result = 4;
                }
            }
            ps2.close();
            con.close();
            return result;
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private String getOldIPforCNAME(long l_server_id) throws Exception {
        String result = new String();
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip FROM l_server_ips WHERE flag =? AND l_server_id = ?");
            ps.setInt(1, 4);
            ps.setLong(2, l_server_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getString(1);
            }
            ps.close();
            con.close();
            return result;
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private String getOldIPforA(long e_dns_rec_id) throws Exception {
        String result = new String();
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT data FROM dns_records WHERE id =?");
            ps.setLong(1, e_dns_rec_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result = rs.getString(1);
            }
            ps.close();
            con.close();
            return result;
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void mainActions(Hashtable oneAlias, long lServerId, String oldIP, String newIP, int ipFlag) throws Exception {
        try {
            String prefix = (String) oneAlias.get("prefix");
            long eZoneId = ((Long) oneAlias.get("e_zone_id")).longValue();
            long eDnsRecId = ((Long) oneAlias.get("e_dns_rec_id")).longValue();
            System.out.println("\tProcess alias with prefix [" + prefix + "], e_zoneId [" + eZoneId + "], e_dns_rec_id [" + eDnsRecId + "]");
            long resellerId = ((Long) oneAlias.get("reseller_id")).longValue();
            Session.setResellerId(resellerId);
            AdmDNSZone zone = AdmDNSZone.get(eZoneId);
            try {
                System.out.print("\tDelete Service DNS Record... ");
                zone.delServiceRecord(eDnsRecId);
                System.out.print("[OK]\n");
                try {
                    switch (ipFlag) {
                        case 8:
                            recreateAlias(lServerId, oldIP, newIP, prefix, zone, true);
                            break;
                        default:
                            recreateAlias(lServerId, oldIP, newIP, prefix, zone, false);
                            break;
                    }
                } catch (Exception e) {
                    Session.getLog().error("Error recreating service DNS record:", e);
                    Session.setResellerId(0L);
                }
            } catch (Exception e2) {
                Session.getLog().error("Error deleting service DNS record:", e2);
                Session.setResellerId(0L);
                return;
            }
        } catch (Exception ex) {
            Session.getLog().error("Error processing server alias", ex);
            System.out.println("\tError processing server alias!");
            ex.printStackTrace();
        }
        System.out.print("\n");
        Session.setResellerId(0L);
    }

    private void process() throws Exception {
        for (int i = 0; i < this.ips.size(); i++) {
            Hashtable oneIP = (Hashtable) this.ips.get(i);
            String oldIP = (String) oneIP.get("old_ip");
            String newIP = (String) oneIP.get("new_ip");
            long lServerId = ((Long) oneIP.get("l_server_id")).longValue();
            List aliases = (List) oneIP.get("aliases");
            int ipFlag = ((Integer) oneIP.get("ip_flag")).intValue();
            System.out.println("Process renaming for IP: [" + newIP + "] ...");
            for (int counter = 0; counter < aliases.size(); counter++) {
                Hashtable oneAlias = (Hashtable) aliases.get(counter);
                mainActions(oneAlias, lServerId, oldIP, newIP, ipFlag);
            }
        }
    }

    private void recreateAlias(long lServerId, String oldIP, String newIP, String prefix, AdmDNSZone zone, boolean isCPSSL) throws Exception {
        System.out.println("\tReseller ID = " + Session.getResellerId());
        try {
            System.out.print("\tCreate Service DNS Record... ");
            zone.addServiceRecord(newIP, prefix, lServerId, isCPSSL);
            System.out.print("[OK]\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (isCPSSL) {
            System.out.print("\tRebuild Reseller CP SSL: Config... ");
            Connection con = this.f235db.getConnection();
            PreparedStatement ps = null;
            long resId = 0;
            try {
                ps = con.prepareStatement("SELECT id FROM reseller_ssl WHERE ip = ?");
                ps.setString(1, oldIP);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    resId = rs.getLong(1);
                }
                ps.close();
                con.close();
                AdmResellerSSL resSSL = AdmResellerSSL.get(resId);
                resSSL.changeDBIP(newIP);
                resSSL.setInsecPort(Session.getProperty("RESELLER_SSL_INSEC_PORT"));
                resSSL.repostConfig(newIP);
                System.out.print("[OK]\n");
            } catch (Throwable th) {
                ps.close();
                con.close();
                throw th;
            }
        }
    }

    private void getDataForLServers() throws Exception {
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT e_dns_rec_id, prefix, e_zone_id FROM l_server_aliases WHERE l_server_id = ? AND e_dns_rec_id IN (SELECT id FROM dns_records WHERE data = ?)");
            for (int i = 0; i < this.ips.size(); i++) {
                Hashtable oneIP = (Hashtable) this.ips.get(i);
                long lServerId = ((Long) oneIP.get("l_server_id")).longValue();
                String oldIP = (String) oneIP.get("old_ip");
                ps.setLong(1, lServerId);
                ps.setString(2, oldIP);
                ResultSet rs = ps.executeQuery();
                ArrayList arrayList = new ArrayList();
                while (rs.next()) {
                    Hashtable oneAlias = new Hashtable();
                    long e_dns_rec_id = rs.getLong(1);
                    String prefix = rs.getString(2);
                    long e_zone_id = rs.getLong(3);
                    oneAlias.put("e_dns_rec_id", new Long(e_dns_rec_id));
                    oneAlias.put("e_zone_id", new Long(e_zone_id));
                    setPrefixAndResellerId(oneAlias, e_zone_id, prefix);
                    getDataForOneRecord(e_dns_rec_id, oneAlias);
                    arrayList.add(oneAlias);
                }
                oneIP.put("aliases", arrayList);
            }
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void setPrefixAndResellerId(Hashtable oneAlias, long e_zone_id, String oldPrefix) throws Exception {
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM dns_zones WHERE id = ?");
            ps.setLong(1, e_zone_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String zoneName = rs.getString(1);
                int i = oldPrefix.indexOf(zoneName);
                if (i != -1) {
                    oneAlias.put("prefix", oldPrefix.substring(0, i - 1));
                }
            }
            setResellerId(oneAlias, e_zone_id);
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void getIPsFromArgs(String[] args) throws Exception {
        int i = 0;
        while (true) {
            if (i >= args.length) {
                break;
            } else if (!"--xml".equals(args[i])) {
                i++;
            } else {
                this.xmlFileName = args[i + 1];
                break;
            }
        }
        parseXML();
        findLServerIDsByIPs();
    }

    private void getData() throws Exception {
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT e_dns_rec_id, prefix, e_zone_id FROM l_server_aliases WHERE l_server_id = ? ORDER BY e_dns_rec_id");
            for (int i = 0; i < this.l_server_ids.size(); i++) {
                Integer l_server_id = (Integer) this.l_server_ids.get(i);
                ps.setInt(1, l_server_id.intValue());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Hashtable oneAlias = new Hashtable();
                    long e_dns_rec_id = rs.getLong(1);
                    String prefix = rs.getString(2);
                    long e_zone_id = rs.getLong(3);
                    oneAlias.put("e_dns_rec_id", new Long(e_dns_rec_id));
                    oneAlias.put("e_zone_id", new Long(e_zone_id));
                    getDataForOneRecord(e_dns_rec_id, oneAlias);
                    oneAlias.put("l_server_id", l_server_id);
                    setPrefixAndResellerId(oneAlias, e_zone_id, prefix);
                    this.data.add(oneAlias);
                }
            }
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void setResellerId(Hashtable oneAlias, long e_zone_id) throws Exception {
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT reseller_id FROM e_zones WHERE id = ?");
            ps.setLong(1, e_zone_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                oneAlias.put("reseller_id", new Long(rs.getLong(1)));
            }
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void getDataForOneRecord(long e_dns_rec_id, Hashtable oneAlias) throws Exception {
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT type_rec FROM e_dns_records WHERE id = ?");
            ps2.setLong(1, e_dns_rec_id);
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                int type_rec = rs.getInt(1);
                oneAlias.put("type_rec", new Integer(type_rec));
            }
            rs.close();
            ps = con.prepareStatement("SELECT name, type, data FROM dns_records WHERE id = ?");
            ps.setLong(1, e_dns_rec_id);
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                String name = rs2.getString(1);
                String type = rs2.getString(2);
                String data = rs2.getString(3);
                oneAlias.put("name", name);
                oneAlias.put("type", type);
                oneAlias.put("data", data);
            }
            rs2.close();
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void getLServerIDsfromArgs(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            if ("--lserver".equals(args[i])) {
                for (int j = i + 1; j < args.length; j++) {
                    this.l_server_ids.add(new Integer(String.valueOf(args[j])));
                }
                return;
            }
        }
    }

    private void findLServerIDsByIPs() throws Exception {
        Connection con = this.f235db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l_server_id, flag FROM l_server_ips WHERE ip = ?");
            for (int i = 0; i < this.ips.size(); i++) {
                Hashtable oneIP = (Hashtable) this.ips.get(i);
                String new_ip = (String) oneIP.get("new_ip");
                ps.setString(1, new_ip);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    long lServerId = rs.getLong(1);
                    oneIP.put("l_server_id", new Long(lServerId));
                    int ipFlag = rs.getInt(2);
                    oneIP.put("ip_flag", new Integer(ipFlag));
                }
            }
            ps.close();
            con.close();
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void parseXML() throws Exception {
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(this.xmlFileName));
        Document doc = parser.getDocument();
        Element root = doc.getDocumentElement();
        NodeList list = root.getElementsByTagName("ip");
        for (int i = 0; i < list.getLength(); i++) {
            Element ipEl = (Element) list.item(i);
            String name = ipEl.getAttributes().getNamedItem("name").getNodeValue();
            String newip = ipEl.getAttributes().getNamedItem("new_ip").getNodeValue();
            Hashtable oneIP = new Hashtable();
            oneIP.put("old_ip", name);
            oneIP.put("new_ip", newip);
            this.ips.add(oneIP);
        }
    }

    private void printDescr() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.ServerAliasesRenamer - this H-Sphere tool \n\t\trecreates server aliases for resellers.");
        System.out.println("SYNOPSIS:\n\t java psoft.hsphere.tools.ServerAliasesRenamer [options]");
        System.out.println("OPTIONS:");
        System.out.println("\t--help - shows this screen");
        System.out.println("\t--xml <file.xml> - run the tool for determined xml file");
        System.out.println("\t--lserver <l_server_id_1>... <l_server_id_n> - run the tool for\n\t\tdetermined Logical Server IDs");
        System.out.println("NOTES:");
        System.out.println("\tUse the tool only with \"--xml\" option or with\"--lserver\" option. \n\t\tDo not mix them.");
        System.exit(0);
    }
}
