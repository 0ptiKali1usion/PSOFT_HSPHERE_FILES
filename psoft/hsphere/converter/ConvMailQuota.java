package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/ConvMailQuota.class */
public class ConvMailQuota extends C0004CP {
    public ConvMailQuota() throws Exception {
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
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM accounts WHERE deleted IS NULL");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Account newAccount = (Account) new ResourceId(rs.getLong(1), 0).get();
                    Session.setAccount(newAccount);
                    Collection<ResourceId> col = newAccount.getId().findAllChildren("mailbox");
                    for (ResourceId boxId : col) {
                        if (boxId.findChild("mail_quota") != null) {
                            Session.getLog().info("Skip mailbox, quota exists " + rs.getLong(1));
                        } else {
                            Session.getLog().info("Add quota to Account " + rs.getLong(1));
                            boxId.get().addChild("mail_quota", "", new ArrayList());
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
