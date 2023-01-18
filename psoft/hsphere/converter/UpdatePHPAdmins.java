package psoft.hsphere.converter;

import java.net.InetAddress;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/converter/UpdatePHPAdmins.class */
public class UpdatePHPAdmins {
    private static Connection con;
    ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    Database f80db = Toolbox.getDB(this.config);

    public int getLIDByName(String name) throws Exception {
        URL varURL = null;
        int hostId = 0;
        try {
            String hostName = this.config.getString(name);
            try {
                hostId = Integer.parseInt(hostName);
            } catch (NumberFormatException e) {
                varURL = new URL(hostName);
            }
            InetAddress hostIP = null;
            if (hostId == 0) {
                try {
                    hostIP = InetAddress.getByName(varURL.getHost());
                } catch (Exception e2) {
                    System.err.println("Can`t resolve host:" + varURL.getHost());
                    return 0;
                }
            }
            Connection con2 = this.f80db.getConnection();
            PreparedStatement ps = null;
            try {
                try {
                    if (hostId == 0) {
                        ps = con2.prepareStatement("SELECT l_server_id, p.ip1 FROM l_server_ips i, l_server l, p_server p WHERE i.ip = ? AND i.l_server_id = l.id AND l.p_server_id = p.id AND flag IN (4, 2)");
                        ps.setString(1, hostIP.getHostAddress());
                    } else {
                        ps = con2.prepareStatement("SELECT l.id, p.ip1 FROM l_server l, p_server p WHERE l.id  = ? AND l.p_server_id = p.id ");
                        ps.setInt(1, hostId);
                    }
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        System.out.println(name + " " + rs.getInt(1) + " " + rs.getString(2));
                    }
                    return 0;
                } catch (SQLException e3) {
                    throw e3;
                }
            } finally {
                Session.closeStatement(ps);
                con2.close();
            }
        } catch (Exception e4) {
            return 0;
        }
    }

    public static void main(String[] argv) {
        try {
            UpdatePHPAdmins test = new UpdatePHPAdmins();
            for (String str : argv) {
                int id = test.getLIDByName(str);
                if (id != 0) {
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
}
