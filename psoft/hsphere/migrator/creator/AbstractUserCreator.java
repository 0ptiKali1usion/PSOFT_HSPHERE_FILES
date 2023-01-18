package psoft.hsphere.migrator.creator;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.C0004CP;
import psoft.hsphere.PrefixHolder;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.tools.ExternalCP;
import psoft.util.FakeRequest;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/migrator/creator/AbstractUserCreator.class */
public abstract class AbstractUserCreator implements UserCreator {
    private static User mainAdmin = null;
    private static Account mainAdmAccount = null;
    protected UserCreatorConfig conf;
    private ArrayList dedicatedIPs = new ArrayList();
    private String prfx = "";
    private long quotaValue = 0;
    private long totalClients = 0;
    private final String DEFAULT_PLAN = "Unix";

    /* renamed from: cp */
    C0004CP f99cp = null;
    protected MigratedUser currentUser = null;

    public C0004CP getCp() throws Exception {
        if (C0004CP.getCP() == null) {
            this.f99cp = new ExternalCP();
        } else {
            this.f99cp = C0004CP.getCP();
            this.f99cp.setConfig();
            this.f99cp.initLog();
        }
        return this.f99cp;
    }

    public AbstractUserCreator(UserCreatorConfig conf) throws Exception {
        getCp();
        this.conf = conf;
        if (conf.isMailActivated()) {
            try {
                outMessage("Initializing mail");
                Session.initMailer();
                outOK();
            } catch (Exception ex) {
                outFail("Failed to initialize mail", ex);
                throw new Exception("Unable to initialize mail");
            }
        }
        PrefixHolder.disablePrefixes();
        if (conf.isResumed()) {
            outMessage("Resuming from previous launch. Whill start from user " + conf.getResumedUser() + ".\n");
        }
        mainAdmin = User.getUser(FMACLManager.ADMIN);
        mainAdmAccount = mainAdmin.getAccount(new ResourceId(1L, 0));
        Session.setUser(mainAdmin);
        Session.setAccount(mainAdmAccount);
    }

    public void setRequest(Hashtable args) {
        Session.setRequest(new FakeRequest(args));
    }

    public void clearRequest() {
        Session.setRequest(new FakeRequest(new Hashtable()));
    }

    public void outOK() {
        System.out.println(" [  OK  ]");
        if (this.conf.getLog() != null) {
            try {
                this.conf.getLog().write(" [  OK  ]\n");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public void outFail(String errMessage) {
        System.out.println(" [  FAILED  ]");
        System.out.println(errMessage);
        Session.getLog().error(errMessage);
        if (this.conf.getLog() != null) {
            try {
                this.conf.getLog().write(" [  FAILED  ]\n");
                this.conf.getLog().write(errMessage + "\n");
            } catch (IOException e) {
            }
        }
    }

    public void outMessage(String message) {
        System.out.print(this.prfx + message);
        Session.getLog().debug(message);
        if (this.conf.getLog() != null) {
            try {
                this.conf.getLog().write(this.prfx + message);
                this.conf.getLog().flush();
            } catch (IOException e) {
            }
        }
    }

    public void outFail() {
        System.out.println(" [  FAILED  ]");
        if (this.conf.getLog() != null) {
            try {
                this.conf.getLog().write(" [  FAILED  ]\n");
            } catch (IOException e) {
            }
        }
    }

    public void outFail(String message, Exception ex) throws Exception {
        System.out.println(message);
        Session.getLog().error(ex);
        if (this.conf.getLog() != null) {
            try {
                this.conf.getLog().write(" [  FAILED  ]\n");
                this.conf.getLog().write(this.prfx + message + "\n");
                if (this.conf.isLogDetailed()) {
                    ex.printStackTrace(new PrintWriter((Writer) this.conf.getLog(), true));
                }
                this.conf.getLog().flush();
            } catch (IOException e) {
            }
        }
        User oldUser = Session.getUser();
        Account a = Session.getAccount();
        try {
            try {
                Session.getLog().debug("WILL TRY TO SET MAIN ADMIN");
                Session.setUser(mainAdmin);
                Session.setAccount(mainAdmAccount);
                Session.getLog().debug("CREATIND TT");
                Ticket tt = Ticket.create(ex, null, null);
                Session.getLog().debug("TT CREATED");
                if (this.currentUser != null) {
                    Session.getLog().debug("SUBMITING BUG");
                    this.currentUser.submitError(tt);
                }
            } catch (Exception ex1) {
                Session.getLog().error(ex.getMessage(), ex1);
                Session.setUser(oldUser);
                Session.setAccount(a);
            }
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(a);
        }
    }

    protected void printHelp() {
        UserCreatorConfig userCreatorConfig = this.conf;
        UserCreatorConfig.printHelp();
    }

    public void addCredit(Account a, String balance) throws Exception {
        Date now = TimeUtils.getDate();
        Bill bill = a.getBill();
        double amount = -(Double.parseDouble(balance) - bill.getBalance());
        synchronized (bill) {
            bill.addEntry(6, now, -1L, -1, "Initial balance adjustment", now, (Date) null, (String) null, amount);
            bill.setCredit(0.0d);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO payment (account_id, amount, id, description, short_desc, entered) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, a.getId().getId());
            ps.setDouble(2, amount);
            ps.setString(3, "");
            ps.setString(4, "Starting balance");
            ps.setString(5, "OTHER");
            ps.setDate(6, new java.sql.Date(now.getTime()));
            ps.executeUpdate();
            con.close();
        }
        a.initBill(now, true);
    }

    public void englareCredit(Account a, double credit) throws Exception {
        try {
            outMessage("Preparing balance ");
            a.getBill().setCredit(credit);
            outOK();
        } catch (Exception ex) {
            outFail("Failed to prepare balance", ex);
            if (!this.conf.isForced()) {
                throw new Exception("Unable to englare credit");
            }
        }
    }
}
