package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/converter/SetResourceAmount.class */
public class SetResourceAmount extends C0004CP {
    public SetResourceAmount() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            SetResourceAmount test = new SetResourceAmount();
            test.m27go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m27go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Session.getLog().info("Starting :");
            ps = con.prepareStatement("SELECT username FROM users");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = User.getUser(rs.getString("username"));
                Session.getLog().debug("SetAmount: The user is " + u.getLogin());
                System.out.println("Current user: " + u.getLogin());
                try {
                    Session.setUser(u);
                    Iterator i = u.getAccountIds().iterator();
                    while (i.hasNext()) {
                        try {
                            Session.setAccount(u.getAccount((ResourceId) i.next()));
                            Session.getLog().info("SetAmount: Using Account " + Session.getAccount().getId().getId());
                            proceedResources(Session.getAccount());
                        } catch (Exception e) {
                            System.err.println("Some problem during update:" + Session.getAccount().getId());
                            Session.getLog().info("Some problem during update:" + Session.getAccount().getId());
                        }
                    }
                } catch (UnknownResellerException e2) {
                    Session.getLog().warn("Live client of removed reseller", e2);
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

    protected void proceedResources(Account a) throws Exception {
        for (ResourceId rId : a.getChildManager().getAllResources()) {
            try {
                Resource r = rId.get();
                r.saveAmount(r.getAmount());
            } catch (Exception e) {
                Session.getLog().info("Some problem during get:" + a.getId(), e);
                System.err.println("Cant get amount from " + rId.getNamedType() + " rId = " + rId);
            }
        }
    }
}
