package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.resource.apache.VirtualHostingResource;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/ApacheReConfigurator.class */
public class ApacheReConfigurator extends C0004CP {
    protected static final int PLAN_ID = 7011;

    public ApacheReConfigurator() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            ApacheReConfigurator test = new ApacheReConfigurator();
            User admin = User.getUser(FMACLManager.ADMIN);
            Account adm = admin.getAccount(new ResourceId(1L, 0));
            Session.setUser(admin);
            Session.setAccount(adm);
            test.m36go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m36go() throws Exception {
        Connection con = Session.getDb();
        try {
            Session.getLog().info("Starting :");
            PreparedStatement ps1 = con.prepareStatement("UPDATE plan_value SET value='8388608' WHERE type_id=41 AND name in('RMIN','RMAX')");
            ps1.executeUpdate();
            Plan.loadAllPlans();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM accounts WHERE id in ( SELECT account_id FROM parent_child WHERE  child_type in (40,41,42))");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Account a = (Account) Account.get(new ResourceId(rs.getLong(1), 0));
                Session.setAccount(a);
                try {
                    Session.setUser(a.getUser());
                    try {
                        System.out.println("Current user: " + Session.getUser().getLogin());
                        System.out.println("Got account " + Session.getAccount().getId().toString() + " plan ID=" + Session.getAccount().getPlan().getId());
                        apacheReconfig(Session.getAccount().getId());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.err.println("Some problem during update:" + Session.getAccount().getId());
                        Session.getLog().info("Some problem during update:" + Session.getAccount().getId());
                    }
                } catch (UnknownResellerException e) {
                    Session.getLog().warn("Live client of removed reseller", e);
                }
            }
        } finally {
            con.close();
        }
    }

    protected void apacheReconfig(ResourceId accId) throws Exception {
        Collection<ResourceId> hostings = accId.findAllChildren("hosting");
        if (hostings == null) {
            return;
        }
        for (ResourceId hostingId : hostings) {
            VirtualHostingResource hosting = (VirtualHostingResource) hostingId.get();
            if (hosting.FM_findChild("rlimitcpu") != null || hosting.FM_findChild("rlimitmem") != null || hosting.FM_findChild("rlimitnproc") != null) {
                System.out.println("reconfiguring ... ");
                hosting.FM_updateConfig();
            }
        }
    }
}
