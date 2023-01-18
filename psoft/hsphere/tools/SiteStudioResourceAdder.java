package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/tools/SiteStudioResourceAdder.class */
public class SiteStudioResourceAdder extends C0004CP {
    public SiteStudioResourceAdder() throws Exception {
        super("psoft_config.hsphere");
    }

    /* renamed from: go */
    public void m3go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            System.out.print("Looking for domains without SiteStudio ...");
            ps = con.prepareStatement("SELECT u.id, p1.account_id, p1.child_id FROM parent_child p1, users u, user_account ua WHERE p1.parent_type IN (2, 31, 32, 34, 35) AND p1.child_type=9 AND p1.account_id=ua.account_id AND ua.user_id=u.id AND NOT EXISTS  (SELECT p2.child_id FROM parent_child p2  WHERE p2.parent_id=p1.child_id AND p2.child_type=59)");
            ResultSet rs = ps.executeQuery();
            System.out.println("Done");
            while (rs.next()) {
                User u = User.getUser(rs.getLong(1));
                System.out.print("Working on user " + u.getLogin() + " acc# " + rs.getString(2));
                Session.setUser(u);
                Account acc = u.getAccount(new ResourceId(rs.getString(2) + "_0"));
                Session.setAccount(acc);
                Resource hosting = new ResourceId(rs.getString(3) + "_9").get();
                double credit = acc.getBill().getCustomCredit();
                try {
                    acc.getBill().setCredit(100000.0d);
                    hosting.addChild("sitestudio", "", null);
                    System.out.println(" Done");
                    acc.getBill().setCredit(credit);
                } catch (Exception e) {
                    Session.getLog().error("Errors during SS adding", e);
                    System.out.println(" Errors. See log for details");
                    acc.getBill().setCredit(credit);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        System.out.print("Initializing ...");
        SiteStudioResourceAdder sa = new SiteStudioResourceAdder();
        System.out.println(" Done");
        sa.m3go();
        System.exit(0);
    }
}
