package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/converter/ConvTraffic.class */
public class ConvTraffic extends C0004CP {
    public ConvTraffic() throws Exception {
        initLog();
        setConfig();
    }

    public static void main(String[] argv) throws Exception {
        new ConvTraffic();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT username FROM users ");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    User u = User.getUser(rs.getString(1));
                    Session.setUser(u);
                    Iterator aci = u.getAccountIds().iterator();
                    while (aci.hasNext()) {
                        Account newAccount = u.getAccount((ResourceId) aci.next());
                        Session.setAccount(newAccount);
                        Session.getLog().debug("Accoutn :" + newAccount);
                        try {
                            Session.getLog().debug("Account traffic available:" + newAccount.getPlan().isResourceAvailable("traffic"));
                            Session.getLog().debug("Account traffic is:" + newAccount.getId().findChild("traffic"));
                            if (newAccount.getPlan().isResourceAvailable("traffic") != null && newAccount.getId().findChild("traffic") == null) {
                                Session.getLog().info("Add traffic to Account ID " + newAccount.getId().getId());
                                newAccount.addChild("traffic", "", new ArrayList());
                            }
                        } catch (NullPointerException ex) {
                            Session.getLog().error("Error in Account:" + newAccount, ex);
                        }
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
            System.out.println("Convertion finished.");
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
