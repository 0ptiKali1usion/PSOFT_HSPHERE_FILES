package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MigratorToMyDNS.class */
public class MigratorToMyDNS extends C0004CP {
    public MigratorToMyDNS() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] args) throws Exception {
        try {
            StringBuffer keys = new StringBuffer("");
            for (String str : args) {
                keys.append(str);
            }
            if (keys.toString().indexOf("--help") != -1) {
                System.out.println("NAME:\n\t psoft.hsphere.tools.MigratorToMyDNS - H-Sphere DNS migration to MyDNS utility");
                System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.MigratorToMyDNS [options]");
                System.out.println("OPTIONS:");
                System.out.println("\t--help \t- shows this screen");
                System.out.println("\t-dz|--delete_zones when this option utility will try to delete each DNS zone on the new set of DNS logical servers before recreate it.");
                System.exit(0);
            }
            System.out.print("DNS migration to MyDNS utility \n itializing ...");
            MigratorToMyDNS migrator = new MigratorToMyDNS();
            try {
                migrator.checkConnectionToMyDNS();
            } catch (Exception e) {
                System.out.println("Can't connect to myDNS database. Check settings in the hsphere.properties file");
                System.exit(1);
            }
            migrator.createMyDNSLServerGroup();
            migrator.initTTLs();
            migrator.processLServers();
            String[] dnsarg = new String[3];
            dnsarg[0] = "-m";
            dnsarg[1] = "db";
            if (keys.toString().indexOf("-dz") != -1 || keys.toString().indexOf("--delete_zones") != -1) {
                dnsarg[2] = "--delete_zones";
            }
            DNSCreator.main(dnsarg);
        } catch (Exception e2) {
            e2.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    private void processLServers() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE l_server SET group_id = ? WHERE group_id = ?");
            ps.setInt(1, 21);
            ps.setInt(2, 2);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void initTTLs() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE dns_records SET ttl = ? WHERE ttl IS NULL");
            ps.setInt(1, 21600);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void checkConnectionToMyDNS() throws Exception {
        Class.forName("org.gjt.mm.mysql.Driver");
        String db_url = "jdbc:mysql://" + Session.getPropertyString("MYDNS_DB_HOST") + "/mydns";
        Connection con = DriverManager.getConnection(db_url, Session.getPropertyString("MYDNS_USER"), Session.getPropertyString("MYDNS_PASS"));
        con.close();
    }

    private void createMyDNSLServerGroup() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM l_server_groups WHERE id = ?");
            ps.setInt(1, 21);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                ps.close();
                ps = con.prepareStatement("INSERT INTO l_server_groups VALUES( ?, ?, ?)");
                ps.setInt(1, 21);
                ps.setInt(2, 2);
                ps.setString(3, "MyDNS name server");
                ps.executeUpdate();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }
}
