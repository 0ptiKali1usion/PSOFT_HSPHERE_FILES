package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/converter/MailRelayAdd.class */
public class MailRelayAdd extends C0004CP {
    private static Connection con;

    public MailRelayAdd() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            MailRelayAdd test = new MailRelayAdd();
            test.m28go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Operations finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m28go() throws Exception {
        con = Session.getTransConnection();
        PreparedStatement plansPs = null;
        try {
            try {
                PreparedStatement plansPs2 = con.prepareStatement("SELECT count(*) FROM type_name WHERE id=1010 OR id=1011");
                ResultSet plansRs = plansPs2.executeQuery();
                plansRs.next();
                if (plansRs.getInt(1) == 0) {
                    plansPs2.close();
                    plansPs2 = con.prepareStatement("INSERT INTO type_name(id, name, description, required, priority, ttl )VALUES(?, ?, ?, 0, 0, NULL)");
                    plansPs2.setInt(1, 1010);
                    plansPs2.setString(2, "mail_relay");
                    plansPs2.setString(3, "Mail Relay");
                    plansPs2.executeUpdate();
                    plansPs2.setInt(1, 1011);
                    plansPs2.setString(2, "mx_list");
                    plansPs2.setString(3, "Mail exchange Records List");
                    plansPs2.executeUpdate();
                }
                plansPs2.close();
                plansPs = con.prepareStatement("SELECT id FROM plans WHERE deleted<>1 AND id NOT IN (SELECT plan_id FROM plan_resource WHERE type_id=1010) AND id IN (SELECT plan_id FROM plan_resource WHERE type_id=1000)");
                ResultSet plansRs2 = plansPs.executeQuery();
                while (plansRs2.next()) {
                    System.out.println("Processing plan #" + plansRs2.getInt(1));
                    fixPlan(plansRs2.getInt(1));
                }
                Session.closeStatement(plansPs);
                con.close();
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(plansPs);
            con.close();
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public void fixPlan(int planId) throws Exception {
        PreparedStatement iModsPs = null;
        try {
            PreparedStatement iModsPs2 = con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, ?, ?, 0, 1)");
            iModsPs2.setInt(1, planId);
            iModsPs2.setInt(2, 1010);
            iModsPs2.setString(3, "psoft.hsphere.resource.dns.MailRelay");
            iModsPs2.executeUpdate();
            iModsPs2.setInt(1, planId);
            iModsPs2.setInt(2, 1011);
            iModsPs2.setString(3, "psoft.hsphere.resource.dns.MXList");
            iModsPs2.executeUpdate();
            iModsPs2.close();
            PreparedStatement iModsPs3 = con.prepareStatement("INSERT INTO plan_iresource(plan_id, type_id, mod_id, new_type_id, order_id, disabled) VALUES(?, 1010, ?, 1011, 0, 0)");
            iModsPs3.setInt(1, planId);
            iModsPs3.setNull(2, 12);
            iModsPs3.executeUpdate();
            iModsPs3.setInt(1, planId);
            iModsPs3.setString(2, "signup");
            iModsPs3.executeUpdate();
            iModsPs3.close();
            PreparedStatement iModsPs4 = con.prepareStatement("UPDATE plan_iresource SET new_type_id=1010 WHERE plan_id = ? AND type_id=1000 AND new_type_id=1007");
            iModsPs4.setInt(1, planId);
            iModsPs4.executeUpdate();
            iModsPs4.close();
            iModsPs = con.prepareStatement("INSERT INTO plan_imod(plan_id, type_id, mod_id, disabled) VALUES(?, 1010, ?, 0)");
            iModsPs.setInt(1, planId);
            iModsPs.setNull(2, 12);
            iModsPs.executeUpdate();
            iModsPs.setInt(1, planId);
            iModsPs.setString(2, "signup");
            iModsPs.executeUpdate();
            iModsPs.close();
            Session.closeStatement(iModsPs);
        } catch (Throwable th) {
            Session.closeStatement(iModsPs);
            throw th;
        }
    }
}
