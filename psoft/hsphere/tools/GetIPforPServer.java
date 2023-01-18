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

/* loaded from: hsphere.zip:psoft/hsphere/tools/GetIPforPServer.class */
public class GetIPforPServer {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    Database f224db = Toolbox.getDB(this.config);

    public Database getDb() {
        return this.f224db;
    }

    public static void main(String[] args) throws Exception {
        GetIPforPServer pSList = new GetIPforPServer();
        String cond = "";
        String params = "";
        String ip = "";
        try {
            params = args[0];
            ip = args[1];
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        if (params != null && params.indexOf(97) < 0) {
            if (params.indexOf(119) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "1";
            }
            if (params.indexOf(110) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "2";
            }
            if (params.indexOf(109) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "3";
            }
            if (params.indexOf(99) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "10";
            }
            if (params.indexOf(112) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "18";
            }
            if (params.indexOf(121) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "4";
            }
        } else {
            cond = "1,2,3,4,6,10,18";
        }
        if ("".equals(cond)) {
            System.err.println("Unknown flag(s)");
            System.exit(-1);
        }
        Connection con = pSList.getDb().getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l.name FROM l_server l, p_server p WHERE l.p_server_id = p.id AND p.ip1 = ? AND l.group_id IN (SELECT id FROM l_server_groups WHERE type_id IN (" + cond + "))");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    String hostIP = InetAddress.getByName(rs.getString(1)).getHostAddress();
                    System.out.println(hostIP);
                    System.exit(0);
                } catch (Exception e2) {
                }
            }
            Session.closeStatement(ps);
            con.close();
            System.exit(-1);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
