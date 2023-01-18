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
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.email.MailQuota;
import psoft.hsphere.resource.system.MailServices;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MboxRepair.class */
public class MboxRepair extends C0004CP {
    public MboxRepair() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    public static void main(String[] argv) throws Exception {
        new MboxRepair();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT username FROM users");
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
                            try {
                                proceedAccount(newAccount);
                            } catch (Exception ex) {
                                System.err.println("Error:" + ex.toString() + ", skiping");
                                Session.getLog().error("Error: ", ex);
                            }
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
            System.out.println("Finished");
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected static void proceedAccount(Account a) throws Exception {
        for (ResourceId resourceId : a.getId().findAllChildren("mailbox")) {
            try {
                Resource mailbox = resourceId.get();
                ResourceId qId = mailbox.getId().findChild("mail_quota");
                if (qId == null) {
                    System.out.println("MailQuota not found ???");
                } else {
                    Resource quota = qId.get();
                    long size = Math.round(((MailQuota) quota).getAmount());
                    HostEntry he = mailbox.recursiveGet("mail_server");
                    String email = mailbox.get("email").toString();
                    String fullemail = mailbox.get("fullemail").toString();
                    String domain = mailbox.getParent().get("name").toString();
                    String pass = mailbox.get("password").toString();
                    Iterator i2 = he.exec("mbox-check", new String[]{domain, email}).iterator();
                    if (!"1".equals((String) i2.next())) {
                        System.out.println("Recreate lost mailbox " + fullemail);
                        MailServices.createMailbox(he, fullemail, pass, (int) size);
                        System.out.println("Recreated ok");
                    }
                }
            } catch (Exception e) {
                System.out.println("Problem updating box: " + e.getMessage());
            }
        }
    }
}
