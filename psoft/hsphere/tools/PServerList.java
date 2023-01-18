package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PServerList.class */
public class PServerList {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    Database f230db = Toolbox.getDB(this.config);

    public Database getDb() {
        return this.f230db;
    }

    public static void main(String[] args) throws Exception {
        PServerList pSList = new PServerList();
        String cond = "";
        String params = "";
        try {
            params = args[0];
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
            if (params.indexOf(118) >= 0) {
                if (!"".equals(cond)) {
                    cond = cond + ",";
                }
                cond = cond + "12";
            }
            if (params.indexOf(113) >= 0) {
                cond = "ALL";
            }
        } else {
            cond = "1,2,3,4,6,10,12,18,25";
        }
        if ("".equals(cond)) {
            System.err.println("Incorrect command. Please use one of the following flags : [w][n][m][c][p][y][q][v] ");
            System.out.println("Usage : java psoft.hsphere.tools.PServerList [w][n][m][c][p][y][q][v]");
            System.out.println("Example : java psoft.hsphere.tools.PServerList q");
            System.exit(-1);
        }
        Connection con = pSList.getDb().getConnection();
        PreparedStatement ps = null;
        try {
            int currId = 0;
            if (!"ALL".equals(cond)) {
                ps = con.prepareStatement("SELECT DISTINCT ip1 FROM p_server WHERE p_server.id IN (SELECT ps_id FROM p_server_group_map WHERE group_id IN (SELECT id FROM l_server_groups WHERE type_id IN (" + cond + ")))");
            } else {
                ps = con.prepareStatement("SELECT p.name, p.ip1, p.mask1, l.id, l.name, g.type_id, i.ip, i.mask, i.flag FROM p_server p, l_server l, l_server_groups g, l_server_ips i WHERE l.p_server_id = p.id AND  l.group_id = g.id AND i.l_server_id = l.id AND (i.flag = 4 OR i.flag = 2 OR (i.flag BETWEEN 10 AND 1000) ) ORDER BY l.id, i.flag");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (!"ALL".equals(cond)) {
                    System.out.println(rs.getString(1));
                } else {
                    int id = rs.getInt("id");
                    if (id != currId) {
                        System.out.println(rs.getString(1) + "\t" + rs.getString(2) + "\t" + rs.getString(3) + "\t" + rs.getString(4) + "\t" + rs.getString(5) + "\t" + rs.getString(6) + "\t" + rs.getString(7) + "\t" + rs.getString(8));
                        currId = id;
                    }
                }
            }
            System.exit(0);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }
}
