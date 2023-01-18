package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/converter/FixServiceCNAME.class */
public class FixServiceCNAME {
    private static Connection con;

    public FixServiceCNAME() throws Exception {
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        Database db = Toolbox.getDB(config);
        con = db.getConnection();
    }

    public static void main(String[] argv) {
        try {
            FixServiceCNAME test = new FixServiceCNAME();
            test.m31go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Operations finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m31go() throws Exception {
        PreparedStatement plansPs = null;
        try {
            plansPs = con.prepareStatement("SELECT DISTINCT (plan_id) FROM plan_resource WHERE type_id = 34");
            ResultSet plansRs = plansPs.executeQuery();
            while (plansRs.next()) {
                System.out.println("Processing plan #" + plansRs.getInt(1));
                try {
                    fixPlan(plansRs.getInt(1));
                } catch (Exception ex) {
                    System.err.println("Error during updating plan:" + plansRs.getInt(1));
                    ex.printStackTrace();
                }
            }
            PreparedStatement ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = data");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("Duplicate record found:" + rs.getString(1));
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM dns_records WHERE id IN (SELECT child_id FROM parent_child WHERE child_type = 3006 AND parent_id IN (SELECT child_id FROM parent_child WHERE child_type = 1000 AND parent_type = 34))");
            ps2.executeUpdate();
            ps2.close();
            con.prepareStatement("DELETE FROM parent_child WHERE child_type = 3006 AND parent_id IN (SELECT child_id FROM parent_child WHERE child_type = 1000 AND parent_type = 34)").executeUpdate();
            Session.closeStatement(plansPs);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(plansPs);
            con.close();
            throw th;
        }
    }

    public void fixPlan(int planId) throws Exception {
        PreparedStatement iModsPs = null;
        try {
            iModsPs = con.prepareStatement("SELECT plan_id FROM plan_imod WHERE type_id = 1000 AND plan_id = ? AND mod_id='service'");
            iModsPs.setInt(1, planId);
            ResultSet iMods = iModsPs.executeQuery();
            if (!iMods.next()) {
                fixIMod(planId);
                System.out.println("Plan " + planId + " :PROCESSED");
            } else {
                System.out.println("Plan " + planId + " already modified :SKIPPED");
            }
        } finally {
            Session.closeStatement(iModsPs);
        }
    }

    public void fixIMod(int planId) throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO plan_imod(plan_id, type_id, mod_id, disabled) VALUES(?, 1000, 'service', 0)");
            ps3.setInt(1, planId);
            ps3.executeUpdate();
            ps3.close();
            ps = con.prepareStatement("UPDATE plan_iresource SET new_mod_id = 'service' WHERE  type_id = 34 AND new_type_id = 1000 AND plan_id = ?");
            ps.setInt(1, planId);
            ps.executeUpdate();
            PreparedStatement ps12 = con.prepareStatement("INSERT INTO plan_iresource(plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES(?, 1000, 'service', 1001, 'signup', 0, 0)");
            ps12.setInt(1, planId);
            ps12.executeUpdate();
            ps12.close();
            ps1 = con.prepareStatement("INSERT INTO plan_iresource(plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES(?, 1000, 'service', 1007, '', 1, 0)");
            ps1.setInt(1, planId);
            ps1.executeUpdate();
            ps2 = con.prepareStatement("INSERT INTO plan_ivalue(plan_id, type_id, type, mod_id, value, label, order_id, disabled) VALUES(?, 1000, 6, 'service', 3, 'Mail Server', 0, 0)");
            ps2.setInt(1, planId);
            ps2.executeUpdate();
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            throw th;
        }
    }
}
