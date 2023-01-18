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

/* loaded from: hsphere.zip:psoft/hsphere/tools/WindowsPlansFix.class */
public class WindowsPlansFix extends C0004CP {
    public WindowsPlansFix() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) throws Exception {
        System.out.println("Start WindowsPlansFix");
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config/hsphere");
        Database db = Toolbox.getDB(config);
        Connection con = db.getTransConnection();
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        try {
            try {
                ps = con.prepareStatement("SELECT plan_id FROM plan_value where name='_CREATED_BY_' AND value IN ('windows','2') ORDER BY plan_id");
                ps2 = con.prepareStatement("SELECT order_id FROM plan_iresource where plan_id=? AND (mod_id IS NULL OR mod_id='') AND new_type_id=61 AND type_id=27 ");
                ps3 = con.prepareStatement("UPDATE plan_iresource SET order_id = 0 WHERE plan_id =?AND (mod_id IS NULL OR mod_id='') AND new_type_id=61 AND type_id=27 AND order_id>0");
                ps4 = con.prepareStatement("UPDATE plan_iresource SET order_id = order_id-1 WHERE plan_id =?AND (mod_id IS NULL OR mod_id='') AND type_id=9 AND order_id>?");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int plan_id = rs.getInt("plan_id");
                    System.out.println("Plan=" + plan_id);
                    ps2.setInt(1, plan_id);
                    ResultSet rs2 = ps2.executeQuery();
                    while (rs2.next()) {
                        int order_id = rs2.getInt("order_id");
                        if (order_id > 0) {
                            ps3.setInt(1, plan_id);
                            ps3.executeUpdate();
                            ps4.setInt(1, plan_id);
                            ps4.setInt(2, order_id);
                            ps4.executeUpdate();
                            con.commit();
                            System.out.println("  wrong id=" + order_id + " FIXED");
                        }
                    }
                }
                if (ps != null) {
                    ps.close();
                }
                if (ps2 != null) {
                    ps2.close();
                }
                if (ps3 != null) {
                    ps3.close();
                }
                if (ps4 != null) {
                    ps4.close();
                }
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                Session.getLog().error("Unable to fix plan", ex);
                System.out.println("   [   FAIL   ]");
                if (ps != null) {
                    ps.close();
                }
                if (ps2 != null) {
                    ps2.close();
                }
                if (ps3 != null) {
                    ps3.close();
                }
                if (ps4 != null) {
                    ps4.close();
                }
                Session.commitTransConnection(con);
            }
            System.out.println("Finished");
            System.exit(0);
        } catch (Throwable th) {
            if (ps != null) {
                ps.close();
            }
            if (ps2 != null) {
                ps2.close();
            }
            if (ps3 != null) {
                ps3.close();
            }
            if (ps4 != null) {
                ps4.close();
            }
            Session.commitTransConnection(con);
            throw th;
        }
    }
}
