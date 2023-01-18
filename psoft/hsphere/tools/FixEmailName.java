package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/FixEmailName.class */
public class FixEmailName extends C0004CP {
    public FixEmailName() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) throws Exception {
        System.out.println("Start FixEmailName");
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config/hsphere");
        Database db = Toolbox.getDB(config);
        Connection con = db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, email FROM mailboxes WHERE email LIKE '%@%'");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String s = rs.getString("email");
                int i = s.indexOf(64);
                try {
                    String query = "UPDATE mailboxes SET email='" + s.substring(0, i) + "' WHERE id=" + rs.getString("id");
                    System.out.println(query);
                    ps = con.prepareStatement(query);
                    ps.executeUpdate();
                } catch (Exception ex) {
                    Session.getLog().error("Unable to change email", ex);
                    System.out.println("   [   FAIL   ]");
                }
            }
            System.out.println("Finished");
            System.exit(0);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }
}
