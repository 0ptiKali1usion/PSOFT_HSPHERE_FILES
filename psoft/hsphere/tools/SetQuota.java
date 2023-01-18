package psoft.hsphere.tools;

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
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.WebUser;

/* loaded from: hsphere.zip:psoft/hsphere/tools/SetQuota.class */
public class SetQuota extends C0004CP {
    public SetQuota() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    public static void main(String[] argv) throws Exception {
        long lserverId = 0;
        if (argv.length > 0) {
            if ("-lid".equals(argv[0]) || "--lserverid".equals(argv[0])) {
                try {
                    lserverId = Long.parseLong(argv[1]);
                } catch (NumberFormatException e) {
                    System.out.println("Wrong logical server id " + argv[1]);
                    System.exit(-1);
                }
            } else if ("-h".equals(argv[0]) || "--help".equals(argv[0])) {
                System.out.println("-lid n or --lserverid n work only on accounts on logical server with passed is");
                System.exit(0);
            }
        }
        if (lserverId != 0) {
            System.out.println("Working on logical server with id=" + lserverId);
        } else {
            System.out.println("Working on all logical servers");
        }
        new SetQuota();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT username FROM users" + (lserverId != 0 ? " ,user_account, parent_child, unix_user WHERE users.id=user_account.user_id  and user_account.account_id = parent_child.account_id and parent_child.child_type = ? and unix_user.id = parent_child.child_id and unix_user.hostid = ?" : ""));
                if (lserverId != 0) {
                    ps.setLong(1, 7L);
                    ps.setLong(2, lserverId);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    User u = User.getUser(rs.getString(1));
                    Session.setUser(u);
                    System.out.println("User :" + Session.getUser().getLogin());
                    Iterator aci = u.getAccountIds().iterator();
                    while (aci.hasNext()) {
                        Account newAccount = u.getAccount((ResourceId) aci.next());
                        if (newAccount == null || newAccount.getPlan() == null) {
                            System.out.println("No account or plan exists, skip");
                        } else {
                            Session.setAccount(newAccount);
                            System.out.println("Account :" + newAccount.getId() + " plan:" + newAccount.getPlan().getDescription());
                            try {
                                proceedAccount(newAccount);
                            } catch (Exception ex) {
                                System.err.println("Error:" + ex.toString() + ", skipping ...");
                                Session.getLog().error("Error: ", ex);
                            }
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
            System.out.println("Finished");
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected static void proceedAccount(Account a) throws Exception {
        ResourceId uId = a.getId().findChild("unixuser");
        if (uId == null) {
            System.out.println("Unixuser not found");
            return;
        }
        String login = "";
        Resource user = uId.get();
        if (user instanceof WebUser) {
            login = ((WebUser) user).getLogin();
        }
        ResourceId qId = uId.findChild("quota");
        if (qId == null) {
            System.out.println("Quota not found ???");
            return;
        }
        Resource quota = qId.get();
        if (quota instanceof HostDependentResource) {
            ((HostDependentResource) quota).physicalCreate(((HostDependentResource) quota).getHostId());
            System.out.println("Quota for user " + login + " was set to " + quota.getAmount());
            return;
        }
        System.out.println("Quota for user " + login + " is not HostDependentResource, skipped ...");
    }
}
