package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.TimeUtils;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ResetBalance.class */
public class ResetBalance extends C0004CP {
    long accId;
    boolean processAll;
    boolean processBalance;
    double processBalanceAmount;
    double newBalanceAmount;
    String billingDescription;
    boolean force;

    public ResetBalance(long accId, double newBalance, String description) throws Exception {
        super("psoft_config.hsphere");
        this.processAll = false;
        this.processBalance = false;
        this.processBalanceAmount = 0.0d;
        this.newBalanceAmount = 0.0d;
        this.billingDescription = "";
        this.force = false;
        this.accId = accId;
        this.newBalanceAmount = newBalance;
        this.billingDescription = description;
        System.out.println("Reset balance for account ID:" + accId + " to " + newBalance);
    }

    public ResetBalance(double newBalance, String description) throws Exception {
        super("psoft_config.hsphere");
        this.processAll = false;
        this.processBalance = false;
        this.processBalanceAmount = 0.0d;
        this.newBalanceAmount = 0.0d;
        this.billingDescription = "";
        this.force = false;
        this.processAll = true;
        this.newBalanceAmount = newBalance;
        this.billingDescription = description;
        System.out.println("Reset balance for all accounts to " + newBalance);
    }

    public ResetBalance(double oldBalance, double newBalance, String description) throws Exception {
        super("psoft_config.hsphere");
        this.processAll = false;
        this.processBalance = false;
        this.processBalanceAmount = 0.0d;
        this.newBalanceAmount = 0.0d;
        this.billingDescription = "";
        this.force = false;
        this.processBalance = true;
        this.newBalanceAmount = newBalance;
        this.processBalanceAmount = oldBalance;
        this.billingDescription = description;
        System.out.println("Reset balance for all accounts which have balance equals:" + oldBalance + " to " + newBalance);
    }

    public void setForce(boolean force) {
        this.force = force;
    }

    /* JADX WARN: Removed duplicated region for block: B:170:0x0239  */
    /* JADX WARN: Removed duplicated region for block: B:184:0x0197 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static void main(java.lang.String[] r8) {
        /*
            Method dump skipped, instructions count: 581
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.tools.ResetBalance.main(java.lang.String[]):void");
    }

    /* renamed from: go */
    public void m6go() throws Exception {
        PreparedStatement ps = null;
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        Connection con = Session.getDb();
        try {
            if (this.processAll) {
                ps = con.prepareStatement("SELECT a.id FROM accounts a, user_account u, balance_credit b WHERE  a.deleted IS NULL AND u.account_id = a.id AND b.id = a.id AND b.balance <> ?");
                ps.setDouble(1, this.newBalanceAmount);
            }
            if (this.processBalance) {
                ps = con.prepareStatement("SELECT a.id FROM accounts a,balance_credit b, user_account u WHERE b.id = a.id AND b.balance = ?AND a.deleted IS NULL AND u.account_id = a.id");
                ps.setDouble(1, this.processBalanceAmount);
            }
            if (this.accId > 0) {
                ps = con.prepareStatement("SELECT a.id FROM accounts a, user_account u, balance_credit b WHERE  a.deleted IS NULL AND u.account_id = a.id AND a.id = ? AND b.id = a.id AND b.balance <> ?");
                ps.setLong(1, this.accId);
                ps.setDouble(2, this.newBalanceAmount);
            }
            Session.getLog().info("Starting :");
            Session.setBillingNote(this.billingDescription);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Account acc = (Account) Account.get(new ResourceId(rs.getString(1) + "_0"));
                    if (acc != null) {
                        Session.setUser(acc.getUser());
                        Session.setAccount(acc);
                        processAccount(acc);
                    } else {
                        System.out.println("Can not get account #" + rs.getString(1) + " for processing. Please check the account id");
                    }
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                } catch (Exception ex) {
                    System.out.println("An error occured while processin account id" + rs.getLong(1) + " see log for more details");
                    Session.getLog().error("An error occured while processin account id" + rs.getLong(1), ex);
                    ex.printStackTrace();
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                }
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    private void processAccount(Account account) {
        try {
            try {
                double balance = account.getBill().getBalance();
                double changes = -(balance - this.newBalanceAmount);
                if (this.force) {
                    Account.addCredit(account.getId().getId(), changes, "Changing balance from " + USFormat.format(balance) + " to " + USFormat.format(this.newBalanceAmount), TimeUtils.getDate());
                }
                System.out.print("Account ID:" + account.getId().getId() + "  Changing balance from " + USFormat.format(balance) + " to " + USFormat.format(this.newBalanceAmount) + " sub " + USFormat.format(changes) + " credit");
                if (this.force) {
                    System.out.println("\t\t[     OK     ]");
                } else {
                    System.out.println("\t\t[     CHECK     ]");
                }
            } catch (Exception ex) {
                System.out.println("\t\t[    FAIL    ]");
                ex.printStackTrace();
            }
        } catch (Exception ex1) {
            System.out.println("\t\t[    FAIL    ]");
            ex1.printStackTrace();
        }
    }

    public static void printHelp() {
        System.out.println("NAME:\n\tpsoft.hsphere.tools.ResetBalance \n\tTo reset billing balance using different criteria");
        System.out.println("\tBy default the tool run only in information mode");
        System.out.println("\tto fix balances run utility with --process option.");
        System.out.println("SYNOPSIS:\n\tpsoft.hsphere.tools.ResetBalance options");
        System.out.println("OPTIONS:");
        System.out.println("\t-h|--help \t\t\t- shows this screen");
        System.out.println("\t-acc|--acountId number\t\t- process only account with given number");
        System.out.println("\t-all|--all \t\t\t- process all account");
        System.out.println("\t-b|--balance <old balance>\t- process accounts which balance is equals <balance for process>");
        System.out.println("\t-n|--newbalance <new balance>\t- set balance to <balance for process>");
        System.out.println("\t-d|--description <description>\t- <credit description> - notyes which will be added to credit operation");
        System.out.println("\t--process\t\t\t- to force process otherway only show affected accounts");
        System.exit(0);
    }
}
