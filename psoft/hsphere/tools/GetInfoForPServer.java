package psoft.hsphere.tools;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/GetInfoForPServer.class */
public class GetInfoForPServer {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    protected Database f225db = Toolbox.getDB(this.config);

    public Database getDb() {
        return this.f225db;
    }

    public static void main(String[] args) throws Exception {
        GetInfoForPServer pSList = new GetInfoForPServer();
        String ip = "";
        try {
            ip = args[0];
        } catch (Exception e) {
            System.err.println("IP not specified");
            System.exit(0);
        }
        long hostIMP = 0;
        try {
            hostIMP = Long.parseLong(pSList.config.getString("IMP_HOST"));
        } catch (Exception e2) {
        }
        long hostMyAdmin = 0;
        try {
            hostMyAdmin = Long.parseLong(pSList.config.getString("PHP_MY_ADMIN_HOST"));
        } catch (Exception e3) {
        }
        long hostPgAdmin = 0;
        try {
            hostPgAdmin = Long.parseLong(pSList.config.getString("PHP_PG_ADMIN"));
        } catch (Exception e4) {
        }
        String cpIP = "unknown";
        try {
            cpIP = InetAddress.getByName(pSList.config.getString("CP_HOST")).getHostAddress();
        } catch (Exception e5) {
        }
        Connection con = pSList.getDb().getConnection();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT ip FROM l_server_ips WHERE l_server_id  = ? AND flag IN (2, 4)");
            ps2.setLong(1, hostIMP);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                System.out.println(rs.getString(1));
            } else {
                System.out.println("unknown");
            }
            ps2.setLong(1, hostMyAdmin);
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                System.out.println(rs2.getString(1));
            } else {
                System.out.println("unknown");
            }
            ps2.setLong(1, hostPgAdmin);
            ResultSet rs3 = ps2.executeQuery();
            if (rs3.next()) {
                System.out.println(rs3.getString(1));
            } else {
                System.out.println("unknown");
            }
            ps2.close();
            System.out.println("http://" + cpIP + ":" + pSList.config.getString("DEFAULT_CP_PORT") + pSList.config.getString("CP_URI"));
            ps2.close();
            ps = con.prepareStatement("SELECT id FROM l_server WHERE p_server_id IN (SELECT id FROM p_server WHERE ip1 = ?) AND group_id IN (SELECT id FROM l_server_groups WHERE type_id = ?)");
            ps.setString(1, ip);
            ps.setInt(2, 3);
            if (ps.executeQuery().next()) {
                System.out.println("MAIL YES");
            } else {
                System.out.println("MAIL NO");
            }
            ps.setString(1, ip);
            ps.setInt(2, 1);
            if (ps.executeQuery().next()) {
                System.out.println("WEB YES");
            } else {
                System.out.println("WEB NO");
            }
            System.exit(0);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }
}
