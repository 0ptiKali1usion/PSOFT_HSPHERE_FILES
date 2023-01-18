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
import psoft.hsphere.User;
import psoft.hsphere.resource.MixedIPResource;

/* loaded from: hsphere.zip:psoft/hsphere/converter/DeleteIP.class */
public class DeleteIP extends C0004CP {
    public DeleteIP() throws Exception {
        initLog();
        setConfig();
    }

    public static void main(String[] argv) throws Exception {
        DeleteIP test = new DeleteIP();
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
                        Session.getLog().debug("Account :" + newAccount);
                        try {
                            test.processAccount(newAccount);
                        } catch (Exception e) {
                            Session.getLog().error("Error in Account:" + newAccount.getId(), e);
                        }
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e2) {
                e2.printStackTrace();
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

    protected void processAccount(Account a) throws Exception {
        Session.getLog().debug("Account traffic available:" + a.getPlan().isResourceAvailable("ip"));
        if (a.getPlan().isResourceAvailable("ip") != null) {
            for (ResourceId resourceId : a.getId().findAllChildren("ip")) {
                Resource ip = resourceId.get();
                if ("1".equals(ip.get("shared").toString())) {
                    Session.getLog().debug("Ip for domain " + ip.recursiveGet("real_name") + " deleted");
                    ((MixedIPResource) ip).FM_ipdelete();
                    ip.delete(false);
                }
            }
        }
    }
}
