package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.ftp.FTPVHostResource;
import psoft.hsphere.resource.ftp.FTPVHostUserResource;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PostFTPConfigs.class */
public class PostFTPConfigs extends C0004CP {
    long accId;
    long lServerId;
    boolean processAll;
    protected final int SLAVE_ABSENT = 0;

    public PostFTPConfigs(long accId, long lServerId, boolean processAll) throws Exception {
        super("psoft_config.hsphere");
        this.SLAVE_ABSENT = 0;
        this.accId = accId;
        this.lServerId = lServerId;
        this.processAll = processAll;
        System.out.println("Working on " + (processAll ? " all vitual FTPs" : lServerId > 0 ? "logical server #" + lServerId : "account id#" + accId));
        System.out.println("Started at " + TimeUtils.getDate());
    }

    public static void main(String[] argv) {
        long accId = -1;
        long lServerId = -1;
        boolean processAll = false;
        boolean configured = false;
        int i = 0;
        while (i < argv.length) {
            if ("-acc".equals(argv[i]) || "--accountId".equals(argv[i])) {
                try {
                    accId = Long.parseLong(argv[i + 1]);
                    i++;
                    configured = true;
                } catch (NumberFormatException e) {
                    System.out.println("Unrecognized account id " + argv[i + 1]);
                }
            }
            if ("-lid".equals(argv[i]) || "--lserverId".equals(argv[i])) {
                try {
                    lServerId = Long.parseLong(argv[i + 1]);
                    i++;
                    configured = true;
                } catch (NumberFormatException e2) {
                    System.out.println("Unrecognized logical server id " + argv[i + 1]);
                }
            }
            if ("-all".equals(argv[i]) || "--all".equals(argv[i])) {
                processAll = true;
            }
            if ("-h".equals(argv[i]) || "--help".equals(argv[i])) {
                printHelp();
            }
            i++;
        }
        if ((configured || processAll) && (lServerId <= 0 || accId <= 0)) {
            try {
                try {
                    PostFTPConfigs test = new PostFTPConfigs(accId, lServerId, processAll);
                    test.m9go();
                    System.out.println("Finished at " + TimeUtils.getDate());
                } catch (Throwable th) {
                    System.out.println("Finished at " + TimeUtils.getDate());
                    throw th;
                }
            } catch (Exception e3) {
                e3.printStackTrace();
                System.exit(-1);
                System.out.println("Finished at " + TimeUtils.getDate());
            }
            System.out.println("Posting FTP configs finished");
            System.exit(0);
            return;
        }
        System.out.println("Missconfiguration ");
        printHelp();
    }

    /* renamed from: go */
    public void m9go() throws Exception {
        PreparedStatement ps = null;
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        Connection con = Session.getDb();
        try {
            if (this.processAll) {
                ps = con.prepareStatement("SELECT DISTINCT a.account_id, a.user_id, c.id FROM user_account a, parent_child b, ftp_vhost c WHERE c.id = b.child_id AND b.account_id = a.account_id");
            }
            if (this.lServerId > 0) {
                ps = con.prepareStatement("SELECT DISTINCT a.account_id, a.user_id, c.id FROM user_account a, parent_child b, ftp_vhost c, parent_child d, unix_user e WHERE c.id = b.child_id AND b.account_id = a.account_id AND d.account_id = a.account_id AND d.child_type=? AND e.id = d.child_id AND e.hostid = ?");
                ps.setLong(1, 7L);
                ps.setLong(2, this.lServerId);
            }
            if (this.accId > 0) {
                ps = con.prepareStatement("SELECT DISTINCT a.account_id, a.user_id, c.id FROM user_account a, parent_child b, ftp_vhost c WHERE c.id = b.child_id AND b.account_id = a.account_id AND a.account_id = ?");
                ps.setLong(1, this.accId);
            }
            Session.getLog().info("Starting :");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Account a = null;
                    User u = User.getUser(rs.getLong("user_id"));
                    Session.setUser(u);
                    Iterator i = u.getAccountIds().iterator();
                    while (true) {
                        if (!i.hasNext()) {
                            break;
                        }
                        ResourceId rId = (ResourceId) i.next();
                        if (rId.getId() == rs.getLong("account_id")) {
                            a = u.getAccount(rId);
                            break;
                        }
                    }
                    if (a != null) {
                        System.out.println("Got account " + a.getId().toString() + " for processing");
                        Session.setAccount(a);
                        processAccount(new ResourceId(rs.getString("id") + "_2001"));
                    } else {
                        System.out.println("Can not get account #" + this.accId + " for processing. Please check the account id");
                    }
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                } catch (Exception ex) {
                    System.out.println("An error occured while processin account id" + rs.getLong("account_id") + " see log for more details");
                    Session.getLog().error("An error occured while processin account id" + rs.getLong("account_id"), ex);
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                }
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    private void processAccount(ResourceId ftpId) {
        try {
            System.out.print("\tGetting " + ftpId.toString());
            FTPVHostResource ftp = (FTPVHostResource) ftpId.get();
            System.out.println("\t\t\t[     OK     ]");
            try {
                System.out.print("\tDeleting config " + ftpId.toString());
                ftp.physicalDelete(ftp.getHostId());
                System.out.println("\t\t[     OK     ]");
            } catch (Exception e) {
                System.out.println("\t\t[    FAIL    ]");
            }
            try {
                System.out.print("\tCreating config " + ftpId.toString());
                ftp.physicalCreate(ftp.getHostId());
                System.out.println("\t\t[     OK     ]");
            } catch (Exception e2) {
                System.out.println("\t\t[    FAIL    ]");
            }
            Collection<ResourceId> c = ftp.getChildHolder().getChildrenByName("ftp_vhost_user");
            for (ResourceId ftpVUserId : c) {
                try {
                    System.out.print("\tGetting " + ftpVUserId.toString());
                    FTPVHostUserResource ftpVUser = (FTPVHostUserResource) ftpVUserId.get();
                    System.out.println("\t\t\t[     OK     ]");
                    try {
                        System.out.print("\tDeleting ...");
                        ftpVUser.physicalDelete(ftp.getHostId());
                        System.out.println("\t\t\t[     OK     ]");
                    } catch (Exception e3) {
                        System.out.println("\t\t[    FAIL    ]");
                    }
                    try {
                        System.out.print("\tCreating ...");
                        ftpVUser.physicalCreate(ftp.getHostId());
                        System.out.println("\t\t\t[     OK     ]");
                    } catch (Exception e4) {
                        System.out.println("\t\t[    FAIL    ]");
                    }
                } catch (Exception e5) {
                    System.out.println("\t\t[    FAIL    ]");
                }
            }
        } catch (Exception e6) {
            System.out.println("\t\t[    FAIL    ]");
        }
    }

    public static void printHelp() {
        System.out.println("NAME:\n\t psoft.hsphere.tools.PostFTPConfigs - H-Sphere virtual FTP hosts gererator utility");
        System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.IPMigrator options");
        System.out.println("OPTIONS:");
        System.out.println("\t-h|--help \t\t- shows this screen");
        System.out.println("\t-acc|--acountId number\t- process only account with given number");
        System.out.println("\t-lid|--lserverId \t- process only accounts on logical server with given number");
        System.out.println("\t-all|--all \t\t- process all virtula FTPs");
        System.exit(0);
    }
}
