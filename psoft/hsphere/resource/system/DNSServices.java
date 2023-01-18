package psoft.hsphere.resource.system;

import gnu.regexp.RE;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedList;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.dns.DNSRecord;

/* loaded from: hsphere.zip:psoft/hsphere/resource/system/DNSServices.class */
public class DNSServices {
    public static void createZone(HostEntry host, String zone, String origin, String mailaddr, String master, String slave1, String s1ip, String slave2, String s2ip) throws Exception {
        createZone(host, zone, origin, mailaddr, master, slave1, s1ip, slave2, s2ip, null, null, null, null);
    }

    public static void createZone(HostEntry host, String zone, String origin, String mailaddr, String master, String slave1, String s1ip, String slave2, String s2ip, String refresh, String retry, String expire, String minimum) throws Exception {
        if (C0004CP.isMyDNSEnabled()) {
            if (HostEntry.getEmulationMode()) {
                Session.getLog().warn("EMULATION MODE, create DNS zone: " + zone);
                return;
            }
            Connection con = getMyDNSConnection();
            try {
                PreparedStatement pstm = con.prepareStatement("INSERT INTO soa (origin, ns, mbox, serial, refresh, retry, expire, minimum) VALUES(?, ?, ?, UNIX_TIMESTAMP(NOW()), ?, ?, ?, ?)");
                pstm.setString(1, zone + ".");
                pstm.setString(2, origin + ".");
                pstm.setString(3, mailaddr + ".");
                pstm.setInt(4, Integer.parseInt(refresh));
                pstm.setInt(5, Integer.parseInt(retry));
                pstm.setInt(6, Integer.parseInt(expire));
                pstm.setInt(7, Integer.parseInt(minimum));
                pstm.executeUpdate();
                Session.closeStatement(pstm);
                PreparedStatement pstm2 = con.prepareStatement("SELECT id FROM soa WHERE origin = ?");
                pstm2.setString(1, zone + ".");
                ResultSet rs = pstm2.executeQuery();
                if (rs.next()) {
                    int zone_id = rs.getInt(1);
                    Session.closeStatement(pstm2);
                    PreparedStatement pstm3 = con.prepareStatement("INSERT INTO rr (zone, name, type, data, aux, ttl) VALUES(?, '', 'NS', ?, 0, ?)");
                    pstm3.setInt(1, zone_id);
                    pstm3.setString(2, master + ".");
                    pstm3.setInt(3, Integer.parseInt(minimum));
                    pstm3.executeUpdate();
                    Session.closeStatement(pstm3);
                    if (slave1 != null) {
                        pstm3 = con.prepareStatement("INSERT INTO rr (zone, name, type, data, aux, ttl) VALUES(?, '', 'NS', ?, 0, ?)");
                        pstm3.setInt(1, zone_id);
                        pstm3.setString(2, slave1 + ".");
                        pstm3.setInt(3, Integer.parseInt(minimum));
                        pstm3.executeUpdate();
                        Session.closeStatement(pstm3);
                    }
                    if (slave2 != null) {
                        pstm3 = con.prepareStatement("INSERT INTO rr (zone, name, type, data, aux, ttl) VALUES(?, '', 'NS', ?, 0, ?)");
                        pstm3.setInt(1, zone_id);
                        pstm3.setString(2, slave2 + ".");
                        pstm3.setInt(3, Integer.parseInt(minimum));
                        pstm3.executeUpdate();
                        Session.closeStatement(pstm3);
                    }
                    if (pstm3 != null) {
                        Session.closeStatement(pstm3);
                    }
                    if (con != null) {
                        con.close();
                        return;
                    }
                    return;
                }
                throw new Exception("MyDNS database error: can't find record in the soa table for the field origin='" + zone + ".'");
            } catch (Throwable th) {
                if (0 != 0) {
                    Session.closeStatement(null);
                }
                if (con != null) {
                    con.close();
                }
                throw th;
            }
        }
        LinkedList ll = new LinkedList();
        ll.add("--zone=" + zone);
        ll.add("--origin=" + origin);
        ll.add("--email=" + mailaddr);
        ll.add("--master=" + master);
        if (null != slave1) {
            ll.add("--slave=" + slave1);
            ll.add("--slaveip=" + s1ip);
        }
        if (null != slave2) {
            ll.add("--slave=" + slave2);
            ll.add("--slaveip=" + s2ip);
        }
        if (null != refresh) {
            ll.add("--refresh=" + refresh);
        }
        if (null != retry) {
            ll.add("--retry=" + retry);
        }
        if (null != expire) {
            ll.add("--expire=" + expire);
        }
        if (null != minimum) {
            ll.add("--minimum=" + minimum);
        }
        host.exec("dns-zone-add", ll);
    }

    public static void deleteZone(HostEntry host, String zone) throws Exception {
        if (C0004CP.isMyDNSEnabled()) {
            if (HostEntry.getEmulationMode()) {
                Session.getLog().warn("EMULATION MODE, delete DNS zone: " + zone);
                return;
            }
            PreparedStatement pstm = null;
            Connection con = getMyDNSConnection();
            try {
                PreparedStatement pstm2 = con.prepareStatement("SELECT id FROM soa WHERE origin=?");
                pstm2.setString(1, zone + ".");
                ResultSet rs = pstm2.executeQuery();
                if (rs.next()) {
                    int zone_id = rs.getInt(1);
                    Session.closeStatement(pstm2);
                    PreparedStatement pstm3 = con.prepareStatement("DELETE FROM rr WHERE zone=?");
                    pstm3.setInt(1, zone_id);
                    pstm3.executeUpdate();
                    Session.closeStatement(pstm3);
                    pstm = con.prepareStatement("DELETE FROM soa WHERE origin=?");
                    pstm.setString(1, zone + ".");
                    pstm.executeUpdate();
                    Session.closeStatement(pstm);
                    if (pstm != null) {
                        return;
                    }
                    return;
                }
                throw new Exception("MyDNS database error: can't find record in the soa table for the field origin='" + zone + ".'");
            } finally {
                if (con != null) {
                    con.close();
                }
                if (pstm != null) {
                    Session.closeStatement(pstm);
                }
            }
        }
        LinkedList ll = new LinkedList();
        ll.add(zone);
        host.exec("dns-zone-del", ll);
    }

    public static void createSlaveZone(HostEntry host, String zone, String master) throws Exception {
        if (!C0004CP.isMyDNSEnabled()) {
            LinkedList ll = new LinkedList();
            ll.add("--zone=" + zone);
            ll.add("--master=" + master);
            host.exec("dns-slavezone-add", ll);
        }
    }

    public static void updateSlaveZone(HostEntry host, String zone, String master, boolean force) throws Exception {
        if (!C0004CP.isMyDNSEnabled()) {
            LinkedList ll = new LinkedList();
            ll.add("--zone=" + zone);
            ll.add("--master=" + master);
            if (force) {
                ll.add("force");
            }
            host.exec("dns-slavezone-update", ll);
        }
    }

    public static void addToZone(HostEntry host, String zone, String name, String type, String ttl, String data, String pref, boolean testRecord) throws Exception {
        String data2;
        if (!name.equals(data) || type.equals("MX")) {
            if (testRecord && DNSRecord.testDuplicateRecords(name, type, data)) {
                throw new HSUserException("duplicate.dns", new Object[]{name});
            }
            if (C0004CP.isMyDNSEnabled()) {
                if (HostEntry.getEmulationMode()) {
                    Session.getLog().warn("EMULATION MODE, add DNS record " + name + " to zone: " + zone);
                    return;
                }
                if (!type.equals("MX")) {
                    pref = "0";
                }
                RE reData = new RE("\\d+.\\d+.\\d+.\\d+");
                if (reData.isMatch(data)) {
                    data2 = Session.int2ext(data);
                } else {
                    data2 = data + ".";
                }
                Connection con = getMyDNSConnection();
                PreparedStatement pstm = null;
                try {
                    PreparedStatement pstm2 = con.prepareStatement("SELECT id FROM soa WHERE origin = ?");
                    pstm2.setString(1, zone + ".");
                    ResultSet rs = pstm2.executeQuery();
                    if (rs.next()) {
                        int zone_id = rs.getInt(1);
                        String name_without_zone = "";
                        if (!name.equals(zone)) {
                            name_without_zone = name.substring(0, name.lastIndexOf(zone) - 1);
                        }
                        Session.closeStatement(pstm2);
                        PreparedStatement pstm3 = con.prepareStatement("INSERT INTO rr (zone, name, type, data, aux, ttl) VALUES(?, ?, ?, ?, ?, ?)");
                        pstm3.setInt(1, zone_id);
                        pstm3.setString(2, name_without_zone);
                        pstm3.setString(3, type);
                        pstm3.setString(4, Session.int2ext(data2));
                        pstm3.setInt(5, Integer.parseInt(pref));
                        pstm3.setInt(6, ttl != null ? Integer.parseInt(ttl) : 0);
                        pstm3.executeUpdate();
                        Session.closeStatement(pstm3);
                        pstm = con.prepareStatement("UPDATE soa SET serial = UNIX_TIMESTAMP(NOW()) WHERE id = ?");
                        pstm.setInt(1, zone_id);
                        pstm.executeUpdate();
                        Session.closeStatement(pstm);
                        if (con != null) {
                            return;
                        }
                        return;
                    }
                    throw new Exception("MyDNS database error: can't find record in the soa table for field origin='" + zone + ".'");
                } finally {
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                    if (con != null) {
                        con.close();
                    }
                }
            }
            LinkedList ll = new LinkedList();
            ll.add("--action=add");
            ll.add("--zone=" + zone);
            ll.add("--type=" + type);
            ll.add("--name=" + MailServices.shellQuote(name));
            if (null != ttl && !"".equals(ttl)) {
                ll.add("--ttl=" + ttl);
            }
            if (null != pref && !"".equals(pref)) {
                ll.add("--pref=" + pref);
            }
            if (type.equalsIgnoreCase("TXT")) {
                ll.add("--data=\"" + MailServices.shellQuote(data) + '\"');
            } else {
                ll.add("--data=" + Session.int2ext(data));
            }
            host.exec("dns-zone-update", ll);
        }
    }

    public static void addToZone(HostEntry host, String zone, String name, String type, String ttl, String data) throws Exception {
        addToZone(host, zone, name, type, ttl, data, null);
    }

    public static void addToZone(HostEntry host, String zone, String name, String type, String ttl, String data, String pref) throws Exception {
        addToZone(host, zone, name, type, ttl, data, pref, true);
    }

    public static void addToZone(HostEntry host, String zone, String name, String type, String data) throws Exception {
        addToZone(host, zone, name, type, null, data, null, true);
    }

    public static void deleteFromZone(HostEntry host, String zone, String name, String type, String pref, String data) throws Exception {
        String data2;
        if (!name.equals(data) || type.equals("MX")) {
            if (C0004CP.isMyDNSEnabled()) {
                if (HostEntry.getEmulationMode()) {
                    Session.getLog().warn("EMULATION MODE, delete DNS record " + name + " from zone: " + zone);
                    return;
                }
                RE reData = new RE("\\d+.\\d+.\\d+.\\d+");
                if (reData.isMatch(data)) {
                    data2 = Session.int2ext(data);
                } else {
                    data2 = data + ".";
                }
                Connection con = getMyDNSConnection();
                PreparedStatement pstm = null;
                try {
                    PreparedStatement pstm2 = con.prepareStatement("SELECT id FROM soa WHERE origin = ?");
                    pstm2.setString(1, zone + ".");
                    ResultSet rs = pstm2.executeQuery();
                    if (rs.next()) {
                        int zone_id = rs.getInt(1);
                        String name_without_zone = "";
                        if (!name.equals(zone)) {
                            name_without_zone = name.substring(0, name.lastIndexOf(zone) - 1);
                        }
                        Session.closeStatement(pstm2);
                        PreparedStatement pstm3 = con.prepareStatement("DELETE FROM rr WHERE zone=? AND name=? AND type=? AND data=?");
                        pstm3.setInt(1, zone_id);
                        pstm3.setString(2, name_without_zone);
                        pstm3.setString(3, type);
                        pstm3.setString(4, data2);
                        pstm3.executeUpdate();
                        Session.closeStatement(pstm3);
                        pstm = con.prepareStatement("UPDATE soa SET serial = UNIX_TIMESTAMP(NOW()) WHERE id = ?");
                        pstm.setInt(1, zone_id);
                        pstm.executeUpdate();
                        Session.closeStatement(pstm);
                        if (pstm != null) {
                            return;
                        }
                        return;
                    }
                    throw new Exception("MyDNS database error: can't find record in the soa table for field origin='" + zone + ".'");
                } finally {
                    if (con != null) {
                        con.close();
                    }
                    if (pstm != null) {
                        Session.closeStatement(pstm);
                    }
                }
            }
            LinkedList ll = new LinkedList();
            ll.add("--action=del");
            ll.add("--zone=" + zone);
            ll.add("--type=" + type);
            ll.add("--name=" + MailServices.shellQuote(name));
            if (null != pref && !"".equals(pref)) {
                ll.add("--pref=" + pref);
            }
            if (type.equalsIgnoreCase("TXT")) {
                ll.add("--data=\"" + MailServices.shellQuote(data) + '\"');
            } else {
                ll.add("--data=" + Session.int2ext(data));
            }
            host.exec("dns-zone-update", ll);
        }
    }

    public static void deleteFromZone(HostEntry host, String zone, String name, String type, String data) throws Exception {
        deleteFromZone(host, zone, name, type, null, data);
    }

    public static Connection getMyDNSConnection() throws Exception {
        Class.forName("org.gjt.mm.mysql.Driver");
        String db_url = "jdbc:mysql://" + Session.getPropertyString("MYDNS_DB_HOST") + "/mydns";
        Connection con = DriverManager.getConnection(db_url, Session.getPropertyString("MYDNS_USER"), Session.getPropertyString("MYDNS_PASS"));
        return con;
    }
}
