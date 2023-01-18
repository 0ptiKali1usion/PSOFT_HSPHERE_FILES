package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.DNSServices;

/* loaded from: hsphere.zip:psoft/hsphere/converter/DNSCreator.class */
public class DNSCreator extends C0004CP {
    protected final int SLAVE_ABSENT = 0;

    public DNSCreator() throws Exception {
        super("psoft_config.hsphere");
        this.SLAVE_ABSENT = 0;
    }

    public static void main(String[] argv) {
        try {
            DNSCreator test = new DNSCreator();
            test.m34go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m34go() throws Exception {
        Connection con = Session.getDb();
        try {
            Session.getLog().info("Starting :");
            PreparedStatement ps = con.prepareStatement("SELECT id,name,email FROM dns_zones");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    createDNSZone(rs.getLong("id"), rs.getString("name"), rs.getString("email"));
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        } finally {
            con.close();
        }
    }

    private void createDNSZone(long zoneId, String name, String email) throws Exception {
        Iterator i = HostManager.getRandomHostsList(2).iterator();
        long master = ((HostEntry) i.next()).getId();
        long slave1 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
        long slave2 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
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
        try {
            System.out.print("Creating DNS zone " + name);
            DNSServices.createZone(m, name, m.getName(), email, m.getName(), s1 != null ? s1.getName() : null, ip1, s2 != null ? s2.getName() : null, ip2, "10800", C0004CP.INETADDRTTL, "604800", "86400");
            if (slave1 != 0) {
                DNSServices.createSlaveZone(s1, name, m.getIPs().iterator().next().toString());
            }
            if (slave2 != 0) {
                DNSServices.createSlaveZone(s2, name, m.getIPs().iterator().next().toString());
            }
            System.out.println("[   OK   ]");
            updateDNSZone(zoneId, master, slave1, slave2);
            createDNSRecords(zoneId, name, master);
        } catch (Exception ex) {
            System.out.println("[   FAIL   ]");
            ex.printStackTrace();
        }
    }

    private void createDNSRecords(long zoneId, String zoneName, long masterId) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps1 = con.prepareStatement("SELECT a.name, a.type, a.data, a.ttl, a.pref FROM dns_records a WHERE a.name LIKE ?");
            ps1.setString(1, "%" + zoneName);
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                try {
                    HostEntry host = HostManager.getHost(masterId);
                    System.out.print("New DNS record:" + zoneName + " name:" + rs1.getString("name") + " type:" + rs1.getString("type") + " ttl:" + rs1.getString("ttl") + " data:" + rs1.getString("data") + " pref:" + rs1.getString("pref"));
                    DNSServices.addToZone(host, zoneName, rs1.getString("name"), rs1.getString("type"), rs1.getString("ttl"), rs1.getString("data"), rs1.getString("pref"));
                    System.out.println("[   OK   ]");
                } catch (Exception ex) {
                    System.out.println("[   FAIL   ]");
                    ex.printStackTrace();
                }
            }
        } finally {
            con.close();
        }
    }

    private void updateDNSZone(long zoneId, long master, long slave1, long slave2) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE dns_zones set master=?,slave1=?,slave2=? WHERE id=?");
            ps.setLong(1, master);
            ps.setLong(2, slave1);
            ps.setLong(3, slave2);
            ps.setLong(4, zoneId);
            ps.executeUpdate();
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }
}
