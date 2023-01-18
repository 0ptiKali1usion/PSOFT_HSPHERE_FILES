package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/tools/BillingEraser.class */
public class BillingEraser extends C0004CP {
    Hashtable accntIds;
    boolean isReseller;

    public BillingEraser(String accnts, boolean isReseller) throws Exception {
        super("psoft_config.hsphere");
        this.accntIds = new Hashtable();
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT user_id FROM user_account WHERE account_id = ?");
            StringTokenizer st = new StringTokenizer(accnts, ",");
            while (st.hasMoreTokens()) {
                long acc = Long.parseLong(st.nextToken());
                ps.setLong(1, acc);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.accntIds.put(new Long(acc), new Long(rs.getLong(1)));
                } else {
                    System.out.println("Account " + acc + " does not has user. Skipping...");
                }
            }
            Session.closeStatement(ps);
            con.close();
            this.isReseller = isReseller;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* renamed from: go */
    public void m20go() throws Exception {
        Account currAcc = Session.getAccount();
        User currUser = Session.getUser();
        for (Long accId : this.accntIds.keySet()) {
            Long userId = (Long) this.accntIds.get(accId);
            try {
                System.out.print("Loading account " + accId);
                Session.setUser(User.getUser(userId.longValue()));
                Account a = Session.getUser().getAccount(new ResourceId(accId.longValue(), 0));
                System.out.println("     [    OK    ]");
                if (this.isReseller) {
                    try {
                        try {
                            System.out.println("Clearing billing for reseller account " + a.getId().getId() + "and all his accounts ");
                            processResellerAccount(a);
                            System.out.println("Clearing billing for reseller account " + a.getId().getId() + " and all his accounts     [    OK    ]");
                            Session.setUser(currUser);
                            Session.setAccount(currAcc);
                        } catch (Throwable th) {
                            Session.setUser(currUser);
                            Session.setAccount(currAcc);
                            throw th;
                        }
                    } catch (Exception ex) {
                        System.out.println("     [   FAIL   ]");
                        Session.getLog().error("Error while billing erasue for account " + a.getId(), ex);
                        Session.setUser(currUser);
                        Session.setAccount(currAcc);
                    }
                } else {
                    try {
                        try {
                            System.out.print("Clearing billing");
                            processAccount(a, true, true);
                            System.out.println("     [    OK    ]");
                            Session.setUser(currUser);
                            Session.setAccount(currAcc);
                        } catch (Exception ex2) {
                            System.out.println("     [   FAIL   ]");
                            Session.getLog().error("Error while billing erasue for account " + a.getId(), ex2);
                            Session.setUser(currUser);
                            Session.setAccount(currAcc);
                        }
                    } catch (Throwable th2) {
                        Session.setUser(currUser);
                        Session.setAccount(currAcc);
                        throw th2;
                    }
                }
            } catch (Exception ex3) {
                System.out.println("     [   FAIL   ]");
                Session.getLog().error("Error while billing erasue for account " + accId, ex3);
                Session.setUser(currUser);
                Session.setAccount(currAcc);
            }
        }
    }

    public void processAccount(Account a, boolean issueCorrection, boolean doCommit) throws Exception {
        PreparedStatement psDelBill = null;
        PreparedStatement psDelBillEntry = null;
        PreparedStatement psDelBillEntryRes = null;
        double delta = 0.0d;
        if (Reseller.getReseller(a.getUser().getId()) != null && doCommit) {
            System.out.println("Passed account " + a.getId().getId() + " is a reseller account");
            System.out.println("Please use --reselles option");
            throw new Exception("Wrong parameters usage");
        }
        boolean wasTrans = Session.isTransConnection();
        Connection con = wasTrans ? Session.getDb() : Session.getTransConnection();
        try {
            try {
                psDelBillEntryRes = con.prepareStatement("DELETE FROM bill_entry_res WHERE id = ?");
                psDelBillEntry = con.prepareStatement("DELETE FROM bill_entry WHERE id = ?");
                psDelBill = con.prepareStatement("DELETE FROM bill WHERE id = ?");
                PreparedStatement psBalanceCredit = con.prepareStatement("UPDATE balance_credit SET balance = ? WHERE id = ?");
                Iterator billsIt = a.getBills();
                while (billsIt.hasNext()) {
                    Bill b = (Bill) billsIt.next();
                    for (BillEntry be : b.getEntries()) {
                        if (be.getResellerEntry() != null) {
                            delta += be.getResellerEntry().getAmount();
                        }
                        psDelBillEntry.setLong(1, be.getId());
                        psDelBillEntryRes.setLong(1, be.getId());
                        psDelBillEntry.executeUpdate();
                        psDelBillEntryRes.executeUpdate();
                    }
                    psDelBill.setLong(1, b.getId());
                    psDelBill.executeUpdate();
                    Session.getBillingLog().info("Erasing bill " + b.getId() + " for account " + a.getId().getId());
                    psBalanceCredit.setDouble(1, 0.0d);
                    psBalanceCredit.setDouble(2, a.getId().getId());
                    psBalanceCredit.executeUpdate();
                }
                if (doCommit) {
                    Session.commitTransConnection(con);
                }
                Session.closeStatement(psDelBill);
                Session.closeStatement(psDelBillEntry);
                Session.closeStatement(psDelBillEntryRes);
                Session.commitTransConnection(con);
                if (issueCorrection && a.getResellerId() != 1 && delta != 0.0d) {
                    User cu = Session.getUser();
                    Account ca = Session.getAccount();
                    try {
                        Account resA = Session.getReseller().getAccount();
                        Session.setUser(resA.getUser());
                        Session.setAccount(resA);
                        Date now = new Date();
                        resA.getBill().addEntry(6, now, -1L, -1, "Adjustment for" + a.getId() + " account's billing erasue", now, (Date) null, (String) null, delta);
                        Session.setUser(cu);
                        Session.setAccount(ca);
                    } catch (Throwable th) {
                        Session.setUser(cu);
                        Session.setAccount(ca);
                        throw th;
                    }
                }
            } catch (Exception ex) {
                Session.getLog().error("Error resetting billing for account " + a.getId(), ex);
                if (doCommit) {
                    con.rollback();
                }
                throw ex;
            }
        } catch (Throwable th2) {
            Session.closeStatement(psDelBill);
            Session.closeStatement(psDelBillEntry);
            Session.closeStatement(psDelBillEntryRes);
            Session.commitTransConnection(con);
            throw th2;
        }
    }

    public void processResellerAccount(Account a) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getTransConnection();
        try {
            try {
                if (Reseller.getReseller(a.getUser().getId()) == null) {
                    System.out.println("Passed account " + a.getId().getId() + " is not a reseller account");
                    throw new Exception("Wrong parameters usage");
                }
                ps = con.prepareStatement("SELECT id FROM accounts WHERE reseller_id = ?");
                ps.setLong(1, a.getUser().getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    try {
                        Session.save();
                        ResourceId rid = new ResourceId(rs.getLong(1), 0);
                        Account ra = (Account) Account.get(rid);
                        Session.setUser(ra.getUser());
                        Session.setAccount(ra);
                        try {
                            System.out.print("CLearing billing for " + ra.getId().getId());
                            processAccount(ra, false, false);
                            System.out.println("     [    OK    ]");
                        } catch (Exception ex) {
                            System.out.println("     [   FAIL   ]");
                            throw ex;
                        }
                    } finally {
                    }
                }
                try {
                    Session.save();
                    Session.setUser(a.getUser());
                    Session.setAccount(a);
                    processAccount(a, false, false);
                } finally {
                }
            } catch (Exception ex2) {
                con.rollback();
                throw ex2;
            }
        } finally {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
        }
    }

    public static void printHelpMessage() {
        System.out.println("\n");
        System.out.println("Usage:");
        System.out.println("java psoft.hsphere.tools.BillingEraser --acounts accounts --resellers resellers");
        System.out.println("\t where");
        System.out.println("\t accounts accounts' IDs separated by comma");
        System.out.println("\t accounts resellers' IDs separated by comma");
        System.out.println("Using --accounts and --resellers parameters simultaniously is disabled.");
        System.out.println("\n");
    }

    public static void printWarnMessage() {
        System.out.println("\n");
        System.out.println("WARNING: THIS UTILITY IS USED TO ERASE BILLING HISTORY OF ACCOUNTS.");
        System.out.println("BILLING ERASED WITH THIS UTILITY IS NOT RECOVERABLE");
        System.out.println("DO NOT RUN THE UTILITY WHILE H-SPHERE CONTROL PANEL IS RUNNING");
        System.out.println("YOU ARE STRONGLY ADVISED TO BACK UP H-SPHERE DATABASE BEFORE USING THIS UTILITY");
        System.out.println("Please remember that erasing reseller's billing history will cause erasing of billing history of the reseller's accounts.");
    }

    public static void main(String[] argv) throws Exception {
        boolean configured = false;
        String accnts = "";
        boolean isReseller = false;
        if (argv.length <= 2) {
            for (int i = 0; i < argv.length; i++) {
                if ("--accounts".equals(argv[i])) {
                    accnts = argv[i + 1];
                    configured = true;
                }
                if ("--resellers".equals(argv[i])) {
                    accnts = argv[i + 1];
                    isReseller = true;
                    configured = true;
                }
            }
        }
        if (configured) {
            System.out.print("Initialization ....");
            BillingEraser eraser = new BillingEraser(accnts, isReseller);
            System.out.println("     [    OK    ]");
            eraser.m20go();
            System.out.println("Process finished");
        } else {
            printWarnMessage();
            printHelpMessage();
        }
        System.exit(0);
    }
}
