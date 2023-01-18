package psoft.hsphere.tools;

import gnu.regexp.RE;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ServiceZoneRenamer.class */
public class ServiceZoneRenamer extends C0004CP {
    String oldName;
    String newName;
    List zones;

    /* renamed from: re */
    RE f236re;
    protected final int SLAVE_ABSENT = 0;

    public ServiceZoneRenamer(String oldName, String newName) throws Exception {
        super("psoft_config.hsphere");
        this.f236re = null;
        this.SLAVE_ABSENT = 0;
        this.oldName = oldName;
        this.newName = newName;
        this.f236re = new RE("(.*\\.)" + oldName + "$");
    }

    /* renamed from: go */
    public void m4go() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        this.zones = new ArrayList();
        User u = User.getUser(FMACLManager.ADMIN);
        Session.setUser(u);
        AdmDNSZone oldZone = AdmDNSZone.getByName(this.oldName);
        if (oldZone == null) {
            System.out.println("Service zone " + this.oldName + " does not exists");
            System.exit(0);
        }
        oldZone.getCustomRecords();
        oldZone.getServiceRecords();
        Connection con = Session.getTransConnection();
        try {
            try {
                PreparedStatement ps3 = con.prepareStatement("UPDATE dns_zones SET name = ? WHERE id=?");
                ps1 = con.prepareStatement("UPDATE dns_records SET name = ? WHERE id = ?");
                ps2 = con.prepareStatement("UPDATE l_server SET name = ? WHERE id = ?");
                PreparedStatement ps32 = con.prepareStatement("SELECT id, name, type, data, ttl, pref FROM dns_records WHERE id IN (SELECT id FROM e_dns_records WHERE zone_id=?)");
                PreparedStatement ps4 = con.prepareStatement("SELECT id, name, email, refresh, retry, expire, master, slave1, slave2 FROM dns_zones WHERE name like ? OR name = ?");
                con.prepareStatement("UPDATE domains SET name = ? where name like ?");
                PreparedStatement ps6 = con.prepareStatement("SELECT a.id, a.name, a.type, a.data, a.ttl, a.pref  FROM dns_records a, parent_child b, parent_child c, parent_child d, parent_child e, dns_zones f WHERE a.id = b.child_id AND b.parent_id=c.child_id AND c.parent_id = d.child_id AND e.parent_id=d.child_id AND e.child_id = f.id and f.id=?");
                PreparedStatement ps7 = con.prepareStatement("UPDATE domains SET name= ? WHERE name=?");
                ps4.setString(1, "%." + this.oldName);
                ps4.setString(2, this.oldName);
                ResultSet rs = ps4.executeQuery();
                while (rs.next()) {
                    ps3.setString(1, getNewName(rs.getString(2)));
                    ps3.setLong(2, rs.getLong(1));
                    ps3.executeUpdate();
                    Hashtable z = new Hashtable();
                    z.put("id", rs.getString("id"));
                    z.put("name", rs.getString("name"));
                    z.put("email", rs.getString("email"));
                    z.put("refresh", rs.getString("refresh"));
                    z.put("retry", rs.getString("retry"));
                    z.put("expire", rs.getString("expire"));
                    z.put("master", rs.getString("master"));
                    z.put("slave1", rs.getString("slave1"));
                    z.put("slave2", rs.getString("slave2"));
                    System.out.println("WORKING ON " + rs.getString("name"));
                    ps32.setLong(1, oldZone.getId());
                    ResultSet rs1 = ps32.executeQuery();
                    ArrayList arrayList = new ArrayList();
                    fillRecords(ps1, arrayList, rs1);
                    ps6.setLong(1, rs.getLong(1));
                    ResultSet rs12 = ps6.executeQuery();
                    fillRecords(ps1, arrayList, rs12);
                    z.put("records", arrayList);
                    this.zones.add(z);
                    ps7.setString(1, getNewName(rs.getString("name")));
                    ps7.setString(2, rs.getString("name"));
                    ps7.executeUpdate();
                }
                ps = con.prepareStatement("SELECT id, name FROM l_server ORDER BY id");
                ResultSet rs2 = ps.executeQuery();
                while (rs2.next()) {
                    if (this.f236re.isMatch(rs2.getString(2))) {
                        ps2.setString(1, getNewName(rs2.getString(2)));
                        ps2.setLong(2, rs2.getLong(1));
                        ps2.executeUpdate();
                    }
                }
                rebuildDNS();
                con.commit();
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                System.out.println("Something wrong happend...");
                ex.printStackTrace();
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.commitTransConnection(con);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        String oldZone = "";
        String newZone = "";
        int i = 0;
        while (i < argv.length) {
            if ("-oz".equals(argv[i]) || "--old_zone".equals(argv[i])) {
                oldZone = argv[i + 1];
                i++;
            }
            if ("-nz".equals(argv[i]) || "--new_zone".equals(argv[i])) {
                newZone = argv[i + 1];
                i++;
            }
            if ("-h".equals(argv[i]) || "--help".equals(argv[i])) {
                printHelpMessage();
                System.exit(0);
            }
            i++;
        }
        if (oldZone.length() > 0 && newZone.length() > 0) {
            System.out.print("Initializing ...");
            ServiceZoneRenamer sa = new ServiceZoneRenamer(oldZone, newZone);
            System.out.println(" Done");
            sa.m4go();
        } else {
            System.out.println("Missconfiguration ...");
            printHelpMessage();
        }
        System.exit(0);
    }

    private void rebuildDNS() throws Exception {
        String name;
        for (int i = 0; i < this.zones.size(); i++) {
            Hashtable zone = (Hashtable) this.zones.get(i);
            System.out.println("DNS zone " + this.oldName + " deletion");
            long master = Long.parseLong((String) zone.get("master"));
            long slave1 = Long.parseLong((String) zone.get("slave1"));
            long slave2 = Long.parseLong((String) zone.get("slave2"));
            try {
                DNSServices.deleteZone(HostManager.getHost(master), (String) zone.get("name"));
                if (slave1 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(slave1), (String) zone.get("name"));
                }
                if (slave2 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(slave2), (String) zone.get("name"));
                }
            } catch (Exception e) {
                System.out.println("FAILED to delete zone " + ((String) zone.get("name")) + " Nothing critical");
            }
            HostEntry m = HostManager.getHost(master);
            HostEntry s1 = null;
            String ip1 = null;
            HostEntry s2 = null;
            String ip2 = null;
            if (slave1 != 0) {
                s1 = HostManager.getHost(slave1);
                ip1 = s1.getIPs().iterator().next().toString();
            }
            if (slave2 != 0) {
                s2 = HostManager.getHost(slave2);
                ip2 = s2.getIPs().iterator().next().toString();
            }
            if (this.oldName.equals(zone.get("name"))) {
                name = this.newName;
            } else {
                name = getNewName((String) zone.get("name"));
            }
            System.out.println("Creating DNS zone " + name + " MASTER " + m.getName() + " SLAVE1 " + (s1 != null ? s1.getName() : "UNASSIGNED") + " SLAVE2 " + (s2 != null ? s2.getName() : "UNASSIGNED"));
            DNSServices.createZone(m, name, m.getName(), (String) zone.get("email"), m.getName(), s1 != null ? s1.getName() : null, ip1, s2 != null ? s2.getName() : null, ip2, (String) zone.get("refresh"), (String) zone.get("retry"), (String) zone.get("expire"), (String) zone.get("minimum"));
            String ipm = m.getPServer().getPFirstIP();
            if (slave1 != 0) {
                DNSServices.createSlaveZone(s1, name, ipm);
            }
            if (slave2 != 0) {
                DNSServices.createSlaveZone(s2, name, ipm);
            }
            createDNSRecords(master, (List) zone.get("records"), name);
        }
    }

    private void createDNSRecords(long masterId, List records, String zName) throws Exception {
        Connection con = Session.getDb();
        HostEntry host = HostManager.getHost(masterId);
        con.prepareStatement("SELECT name, type, data, ttl, pref  FROM dns_records WHERE id=?");
        for (int i = 0; i < records.size(); i++) {
            Hashtable t = (Hashtable) records.get(i);
            try {
                System.out.print("New DNS record:" + zName + " name:" + t.get("name") + " type:" + t.get("type") + " ttl:" + t.get("ttl") + " data:" + t.get("data") + " pref:" + t.get("pref"));
                DNSServices.addToZone(host, zName, (String) t.get("name"), (String) t.get("type"), (String) t.get("ttl"), (String) t.get("data"), (String) t.get("pref"), false);
                System.out.println("[   OK   ]");
            } catch (Exception ex) {
                System.out.println("[   FAIL   ]");
                ex.printStackTrace();
            }
        }
    }

    private String getNewName(String tString) {
        if (this.oldName.equals(tString)) {
            return this.newName;
        }
        return this.f236re.substitute(tString, "$1" + this.newName);
    }

    private static void printHelpMessage() {
        System.out.println("Utility for changing service zone name. Changes zone name, LServers names, rebuilds DNS.");
        System.out.println("WARNING: USE ONLY ON EMPTY INSTALATION OF H-SPHERE.");
        System.out.println("Usage java.psoft.hsphere.tools.ServiceZoneRenamer -oz zone_name -nz zone_name");
        System.out.println("    -oz|--old_zone Name of the currently present service zone");
        System.out.println("    -nz|--new_zone Name which should be set to service zone");
    }

    private void fillRecords(PreparedStatement ps1, List l, ResultSet rs) throws Exception {
        while (rs.next()) {
            if (this.f236re.isMatch(rs.getString(2)) || this.newName.equals(rs.getString(2))) {
                System.out.println("\tUpdating DNSRecord " + rs.getString(2));
                ps1.setString(1, getNewName(rs.getString(2)));
                ps1.setLong(2, rs.getLong(1));
                ps1.executeUpdate();
                Hashtable t = new Hashtable();
                t.put("name", getNewName(rs.getString(2)));
                t.put("type", rs.getString(3));
                t.put("data", rs.getString(4));
                t.put("ttl", rs.getString(5));
                t.put("pref", rs.getString(6) == null ? "" : rs.getString(6));
                l.add(t);
            }
        }
    }
}
