package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.MixedIPResource;

/* loaded from: hsphere.zip:psoft/hsphere/converter/CreateIP.class */
public class CreateIP extends C0004CP {
    public CreateIP() throws Exception {
        initLog();
        setConfig();
    }

    public static void main(String[] argv) throws Exception {
        new CreateIP();
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
                            Session.getLog().debug("Account traffic available:" + newAccount.getPlan().isResourceAvailable("ip"));
                            if (newAccount.getPlan().isResourceAvailable("ip") != null) {
                                if (newAccount.getPlan().isResourceAvailable("nodomain") != null) {
                                    createIP(newAccount, "nodomain");
                                }
                                if (newAccount.getPlan().isResourceAvailable("domain") != null) {
                                    createIP(newAccount, "domain");
                                }
                                if (newAccount.getPlan().isResourceAvailable("subdomain") != null) {
                                    createIP(newAccount, "subdomain");
                                }
                                if (newAccount.getPlan().isResourceAvailable("3ldomain") != null) {
                                    createIP(newAccount, "3ldomain");
                                }
                                if (newAccount.getPlan().isResourceAvailable("service_domain") != null) {
                                    createIP(newAccount, "service_domain");
                                }
                            }
                        } catch (Exception ex) {
                            Session.getLog().error("Error in Account:" + newAccount, ex);
                        }
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            Session.closeStatement(ps);
            con.close();
        }
        System.out.println("Convertion finished.");
        System.exit(0);
    }

    protected static void createIP(Account newAccount, String name) throws Exception {
        Resource resource;
        for (ResourceId resourceId : newAccount.getId().findAllChildren(name)) {
            Resource domain = resourceId.get();
            if (domain.getId().FM_getChild("ip") == null) {
                if ("nodomain".equals(name)) {
                    resource = domain.addChild("ip", "shard_no_a", new ArrayList()).get();
                } else {
                    resource = domain.addChild("ip", "shared", new ArrayList()).get();
                }
                MixedIPResource ip = (MixedIPResource) resource;
                ip.FM_reconfig();
            }
        }
    }
}
