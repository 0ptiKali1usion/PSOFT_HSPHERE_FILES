package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/IPInfo.class */
public class IPInfo {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    Database f227db = Toolbox.getDB(this.config);

    public Database getDb() {
        return this.f227db;
    }

    public static void main(String[] args) throws Exception {
        IPInfo pSList = new IPInfo();
        String cond = "";
        String params = "";
        try {
            params = args[0];
        } catch (ArrayIndexOutOfBoundsException e) {
        }
        if (params != null && params.indexOf(97) < 0) {
            if (params.indexOf(105) >= 0) {
                cond = "IPS";
            }
            if (params.indexOf(115) >= 0) {
                cond = "LOGICAL";
            }
            if (params.indexOf(108) >= 0) {
                cond = "LIST";
            }
            if (params.indexOf(113) >= 0) {
                cond = "ALL";
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
            if ("LIST".equals(cond)) {
                ps = con.prepareStatement("SELECT DISTINCT id, name, ip1 FROM p_server WHERE p_server.id IN (SELECT ps_id FROM p_server_group_map WHERE group_id IN (SELECT id FROM l_server_groups WHERE type_id IN (1,2,3,4,6,10,18)))");
            } else if ("IPS".equals(cond)) {
                ps = con.prepareStatement("SELECT DISTINCT ip, mask FROM l_server_ips");
            } else if ("LOGICAL".equals(cond)) {
                ps = con.prepareStatement("SELECT DISTINCT s.id, s.name, s.p_server_id, g.name FROM l_server s, l_server_groups g WHERE s.type_id=1 AND s.group_id=g.id AND g.type_id=1 ORDER BY s.id;");
            } else {
                ps = con.prepareStatement("SELECT p.name, p.ip1, p.mask1, l.id, l.name, g.type_id, i.ip, i.mask FROM p_server p, l_server l, l_server_groups g, l_server_ips i WHERE l.p_server_id = p.id AND l.group_id = g.id AND i.l_server_id = l.id AND (i.flag = 4 OR i.flag = 2) ORDER BY l.id");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if ("LIST".equals(cond)) {
                    PreparedStatement ps1 = con.prepareStatement("SELECT type_id FROM l_server_groups WHERE id IN (SELECT group_id FROM p_server_group_map WHERE ps_id = " + rs.getString(1) + ")");
                    ResultSet rs1 = ps1.executeQuery();
                    String groups = "";
                    while (rs1.next()) {
                        if ("1".equals(rs1.getString(1))) {
                            groups = groups + "w";
                        } else if ("2".equals(rs1.getString(1))) {
                            groups = groups + "n";
                        } else if ("3".equals(rs1.getString(1))) {
                            groups = groups + "m";
                        } else if ("4".equals(rs1.getString(1))) {
                            groups = groups + "y";
                        } else if ("10".equals(rs1.getString(1))) {
                            groups = groups + "c";
                        } else if ("18".equals(rs1.getString(1))) {
                            groups = groups + "p";
                        }
                    }
                    System.out.println(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3) + "\t" + groups + "\tNo");
                } else if ("IPS".equals(cond)) {
                    System.out.println(rs.getString(1) + "\t" + rs.getString(2));
                } else if ("LOGICAL".equals(cond)) {
                    System.out.println(rs.getString(1) + "|" + rs.getString(2) + "|" + rs.getString(3) + "|No|" + rs.getString(4));
                } else if (!"ALL".equals(cond)) {
                    System.out.println(rs.getString(1));
                } else {
                    System.out.println(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4) + "\t" + rs.getString(5) + "\t" + rs.getString(6) + "\t" + rs.getString(7) + "\t" + rs.getString(8));
                }
            }
            System.exit(0);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }
}
