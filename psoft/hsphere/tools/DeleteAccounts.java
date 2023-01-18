package psoft.hsphere.tools;

import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/DeleteAccounts.class */
public class DeleteAccounts extends C0004CP {
    public DeleteAccounts() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] args) throws Exception {
        DeleteAccounts da = new DeleteAccounts();
        da.processDeleting();
        System.exit(0);
    }

    public void processDeleting() {
        System.out.println("Initializing ");
        try {
            Session.setUser(User.getUser(FMACLManager.ADMIN));
            Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
            AccountManager admin = (AccountManager) Session.getAccount().FM_findChild(FMACLManager.ADMIN).get();
            LineNumberReader strReader = new LineNumberReader(new InputStreamReader(System.in));
            while (true) {
                String curStr = strReader.readLine();
                if (curStr == null) {
                    break;
                }
                try {
                    sweepAccount(admin, Long.parseLong(curStr.trim()));
                } catch (NumberFormatException e) {
                    System.err.println(" ERROR parsing AccountId:" + curStr);
                }
            }
        } catch (Exception e2) {
            e2.printStackTrace();
            System.exit(1);
        }
        System.out.println("Deleting finished");
    }

    /* JADX WARN: Finally extract failed */
    protected void sweepAccount(AccountManager admin, long acctId) throws Exception {
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                System.out.print("DELETING AccountID :" + acctId);
                Account a = (Account) Account.get(new ResourceId(acctId, 0));
                if (a == null) {
                    System.out.println("\t[NONEXISTANT]");
                    Session.setAccount(oldAccount);
                    Session.setUser(oldUser);
                    return;
                }
                System.out.print(" ..... ");
                User u = a.getUser();
                if (u == null) {
                    String login = "_" + TimeUtils.currentTimeMillis();
                    User.createUser(login, "_" + TimeUtils.currentTimeMillis(), a.getResellerId());
                    u = User.getUser(login);
                    try {
                        ps = con.prepareStatement("INSERT INTO user_account (user_id, account_id, type_id) VALUES (? ,?, ?)");
                        ps.setLong(1, u.getId());
                        ps.setLong(2, a.getId().getId());
                        ps.setInt(3, a.getId().getType());
                        ps.executeUpdate();
                        Session.closeStatement(ps);
                        con.close();
                        System.out.print(" Fake user has been created:" + u.getLogin());
                    } catch (Throwable th) {
                        Session.closeStatement(ps);
                        con.close();
                        throw th;
                    }
                }
                Session.setUser(u);
                admin.FM_deleteUserAccount(u.getLogin(), acctId);
                System.out.println("\t[DELETED]");
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            } catch (HSUserException ex) {
                System.out.println("\n\t\t\tERROR:" + ex.getMessage() + "\t[FAILED]");
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            } catch (Exception e) {
                System.out.println("\t[FAILED]");
                Session.setAccount(oldAccount);
                Session.setUser(oldUser);
            }
        } catch (Throwable th2) {
            Session.setAccount(oldAccount);
            Session.setUser(oldUser);
            throw th2;
        }
    }
}
