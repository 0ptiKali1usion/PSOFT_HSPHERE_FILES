package psoft.hsphere.tools;

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
import psoft.hsphere.resource.apache.VirtualHostingResource;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PostApacheConfigs.class */
public class PostApacheConfigs extends C0004CP {
    boolean initContent;

    public PostApacheConfigs(boolean ic) throws Exception {
        super("psoft_config.hsphere");
        this.initContent = false;
        this.initContent = ic;
    }

    public static void main(String[] argv) throws Exception {
        long lserverId = 0;
        boolean ic = false;
        if (argv.length == 0) {
            printHelpMessage();
            System.exit(0);
        }
        int i = 0;
        while (i < argv.length) {
            if ("-lid".equals(argv[i]) || "--lserverid".equals(argv[i])) {
                try {
                    lserverId = Long.parseLong(argv[i + 1]);
                    i++;
                } catch (NumberFormatException e) {
                    System.out.println("Wrong logical server id " + argv[i + 1]);
                    System.exit(-1);
                }
            } else if ("-ic".equals(argv[i]) || "--initcontent".equals(argv[i])) {
                ic = true;
            } else if ("-h".equals(argv[i]) || "--help".equals(argv[i])) {
                printHelpMessage();
                System.exit(0);
            }
            i++;
        }
        if (lserverId != 0) {
            System.out.println("Working on logical server with id=" + lserverId);
        } else {
            System.out.println("Working on all logical servers");
        }
        PostApacheConfigs test = new PostApacheConfigs(ic);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("UPDATE apache_vhost SET entry = NULL " + (lserverId != 0 ? " WHERE host_id = ?" : ""));
                if (lserverId != 0) {
                    ps2.setLong(1, lserverId);
                }
                ps2.executeUpdate();
                ps2.close();
                ps = con.prepareStatement("SELECT username FROM users " + (lserverId != 0 ? " ,user_account, parent_child, unix_user WHERE users.id=user_account.user_id  and user_account.account_id = parent_child.account_id and parent_child.child_type = ? and unix_user.id = parent_child.child_id and unix_user.hostid = ?" : ""));
                if (lserverId != 0) {
                    ps.setLong(1, 7L);
                    ps.setLong(2, lserverId);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    User u = User.getUser(rs.getString(1));
                    Session.setUser(u);
                    Iterator aci = u.getAccountIds().iterator();
                    while (aci.hasNext()) {
                        try {
                            Account newAccount = u.getAccount((ResourceId) aci.next());
                            System.out.println("Account: " + newAccount.getId());
                            Session.setAccount(newAccount);
                            test.processAccount(newAccount);
                        } catch (Exception e2) {
                            Session.getLog().error("ERROR", e2);
                        }
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e3) {
                e3.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps);
                con.close();
            }
            System.out.println("Process finished.");
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void processAccount(Account a) throws Exception {
        Collection<ResourceId> col = a.getId().findAllChildren("hosting");
        for (ResourceId hostingId : col) {
            Resource hosting = hostingId.get();
            if (hosting instanceof VirtualHostingResource) {
                if (this.initContent) {
                    try {
                        System.out.print("Re-initializing content for " + ((VirtualHostingResource) hosting).recursiveGet("name").toString());
                        ((VirtualHostingResource) hosting).initContent();
                        System.out.println("    [    OK    ]");
                    } catch (Exception ex) {
                        System.out.println("    [    FAIL  ]");
                        Session.getLog().error("Failed to re-initialize content", ex);
                    }
                }
                try {
                    System.out.print("Config republishing for " + ((VirtualHostingResource) hosting).recursiveGet("name").toString());
                    ((VirtualHostingResource) hosting).restart();
                    System.out.println("    [    OK    ]");
                } catch (Exception ex2) {
                    System.out.println("    [    FAIL  ]");
                    Session.getLog().error("Failed to republish config", ex2);
                }
            }
        }
    }

    public static void printHelpMessage() {
        System.out.println("Usage: ");
        System.out.println("java psoft.hsphere.tools.PostApacheConfigs [-lid n ] [ -ic ]");
        System.out.println("\n\t-lid|--lserverid n work only on accounts on logical server with passed number");
        System.out.println("\t-ic|--initcontent initialise content");
        System.out.println("\t-h|--help print this message");
    }
}
