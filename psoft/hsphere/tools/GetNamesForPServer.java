package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/GetNamesForPServer.class */
public class GetNamesForPServer {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    protected Database f226db = Toolbox.getDB(this.config);

    public Database getDb() {
        return this.f226db;
    }

    public static void main(String[] args) throws Exception {
        GetNamesForPServer pSList = new GetNamesForPServer();
        String ip = "";
        try {
            ip = args[0];
        } catch (Exception e) {
            System.err.println("IP not specified");
            System.exit(0);
        }
        Connection con = pSList.getDb().getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM l_server WHERE p_server_id IN (SELECT id FROM p_server WHERE ip1 = ?)");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1));
            }
            Session.closeStatement(ps);
            con.close();
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
