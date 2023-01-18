package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/ConvMailQuota2.class */
public class ConvMailQuota2 extends C0004CP {
    public ConvMailQuota2() throws Exception {
        initLog();
        setConfig();
    }

    public static void main(String[] argv) throws Exception {
        new ConvMailQuota();
        User u = User.getUser(FMACLManager.ADMIN);
        Session.setUser(u);
        Iterator aci = u.getAccountIds().iterator();
        Session.setAccount(u.getAccount((ResourceId) aci.next()));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE deleted IS NULL");
                ResultSet rs = ps.executeQuery();
                ps2 = con.prepareStatement("SELECT size_mb FROM quotas WHERE id = ?");
                while (rs.next()) {
                    Account newAccount = (Account) new ResourceId(rs.getLong(1), 0).get();
                    Session.setAccount(newAccount);
                    Collection<ResourceId> col = newAccount.getId().findAllChildren("mail_quota");
                    for (ResourceId quotaId : col) {
                        Resource quota = quotaId.get();
                        ps2.setLong(1, quotaId.getId());
                        ResultSet rs2 = ps2.executeQuery();
                        if (rs2.next()) {
                            int size = rs2.getInt(1);
                            try {
                                if (Session.getPropertyString("IRIS_USER").equals("") || ((MailDomain) quota.getParent().get().getParent().get()).getCatchAll().equals(quota.recursiveGet("email").toString())) {
                                }
                                MailServices.setMboxQuota(quota.recursiveGet("mail_server"), quota.recursiveGet("fullemail").toString(), size, false);
                                Session.getLog().info("Update quota,ID=" + quotaId.getId());
                            } catch (Exception e) {
                                Session.getLog().info("Update quota failed,ID=" + quotaId.getId(), e);
                            }
                        } else {
                            Session.getLog().info("Cant find quota size for ID=" + quotaId.getId());
                        }
                    }
                }
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                con.close();
            } catch (Exception e2) {
                e2.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                con.close();
            }
            System.out.println("Convertion finished.");
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }
}
