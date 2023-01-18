package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Hashtable;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;

/* loaded from: hsphere.zip:psoft/hsphere/converter/RBillsRecover.class */
public class RBillsRecover extends C0004CP {
    long mt_id;

    public RBillsRecover() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) throws Exception {
        RBillsRecover t = new RBillsRecover();
        try {
            if (argv.length > 0 && "--info".equals(argv[0])) {
                t.analyzeBillEntries(false);
            } else if (argv.length > 0 && "--processFix".equals(argv[0])) {
                t.analyzeBillEntries(true);
            } else {
                printHelpMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }

    public void analyzeBillEntries(boolean force) throws Exception {
        PreparedStatement berPs = null;
        PreparedStatement berefPs = null;
        PreparedStatement bePs = null;
        PreparedStatement sumPs = null;
        PreparedStatement updtBalancePs = null;
        Connection con = Session.getTransConnection();
        try {
            try {
                Hashtable affectedEntries = new Hashtable();
                Hashtable affectedResellers = new Hashtable();
                ArrayList affectedRefund = new ArrayList();
                berPs = con.prepareStatement("SELECT r.id, r.created, r.reseller_id, e.rid, e.rtype, r.amount FROM bill_entry_res r,  bill_entry e WHERE r.description ~ 'above free [1-5] MB of Disk space Quota' AND r.id = e.id");
                berefPs = con.prepareStatement("SELECT  SUM(r.amount) AS amount FROM bill_entry_res r,  bill_entry e WHERE e.rid = ? AND r.id = e.id AND e.type IN (4,104)");
                bePs = con.prepareStatement("SELECT id, started, ended, amount FROM bill_entry WHERE type=13 AND started <= ? AND ended > ? AND EXISTS (SELECT * FROM bill, user_account WHERE bill.account_id=user_account.account_id AND user_account.user_id = ? AND bill.id=bill_entry.bill_id)");
                sumPs = con.prepareStatement("SELECT sum(are.amount) FROM bill_entry b, bill_entry_res are WHERE b.id = are.id  AND are.reseller_id = ? AND b.created >= ? AND b.created < ? AND b.canceled IS NULL and b.type=2 AND are.description !~ 'above free [1-5] MB of Disk space Quota'");
                PreparedStatement updtBePs = con.prepareStatement("UPDATE bill_entry SET amount = ? WHERE id = ?");
                updtBalancePs = con.prepareStatement("UPDATE balance_credit SET balance = ? WHERE id = ?");
                PreparedStatement updtBerPs = con.prepareStatement("UPDATE bill_entry_res SET amount = 0 WHERE description ~ 'above free [1-5] MB of Disk space Quota'");
                ResultSet berRs = berPs.executeQuery();
                double totalAmount = 0.0d;
                while (berRs.next()) {
                    bePs.setDate(1, berRs.getDate("created"));
                    bePs.setDate(2, berRs.getDate("created"));
                    bePs.setLong(3, berRs.getLong("reseller_id"));
                    totalAmount += berRs.getDouble("amount");
                    ResultSet beRs = bePs.executeQuery();
                    while (beRs.next()) {
                        if (!affectedEntries.keySet().contains(new Long(beRs.getLong("id")))) {
                            Hashtable t = new Hashtable();
                            t.put("started", beRs.getTimestamp("started"));
                            t.put("ended", beRs.getTimestamp("ended"));
                            t.put("amount", new Double(beRs.getDouble("amount")));
                            t.put("reseller_id", new Long(berRs.getLong("reseller_id")));
                            t.put("id", new Long(beRs.getLong("id")));
                            t.put("refund", new Double(0.0d));
                            affectedEntries.put(new Long(beRs.getLong("id")), t);
                        }
                        if (!affectedRefund.contains(new Long(berRs.getLong("rid")))) {
                            berefPs.setLong(1, berRs.getLong("rid"));
                            ResultSet berefRs = berefPs.executeQuery();
                            if (berefRs.next()) {
                                if (berefRs.getDouble("amount") != 0.0d) {
                                    System.out.println("REFUND :" + berefRs.getDouble("amount") + " ENTRIES:" + beRs.getDouble("amount"));
                                    Hashtable rt = (Hashtable) affectedEntries.get(new Long(beRs.getLong("id")));
                                    if (rt != null) {
                                        rt.put("refund", new Double(((Double) rt.get("refund")).doubleValue() + berefRs.getDouble("amount")));
                                        affectedRefund.add(new Long(berRs.getLong("rid")));
                                    }
                                }
                            }
                        }
                    }
                }
                double oldAmountTotal = 0.0d;
                double newAmountTotal = 0.0d;
                double refundTotal = 0.0d;
                for (Hashtable t2 : affectedEntries.values()) {
                    long resId = ((Long) t2.get("reseller_id")).longValue();
                    sumPs.setLong(1, resId);
                    sumPs.setTimestamp(2, (Timestamp) t2.get("started"));
                    sumPs.setTimestamp(3, (Timestamp) t2.get("ended"));
                    ResultSet sumRs = sumPs.executeQuery();
                    if (sumRs.next()) {
                        double oldAmount = ((Double) t2.get("amount")).doubleValue();
                        double refund = ((Double) t2.get("refund")).doubleValue();
                        double newAmount = sumRs.getDouble(1);
                        oldAmountTotal += oldAmount;
                        newAmountTotal += newAmount;
                        refundTotal += refund;
                        try {
                            String resellerName = Reseller.getReseller(resId).getUser();
                            if (resellerName != null) {
                                Hashtable r = (Hashtable) affectedResellers.get(resellerName);
                                if (r == null) {
                                    r = new Hashtable();
                                    r.put("reseller_id", new Long(resId));
                                    r.put("reseller_name", resellerName);
                                    affectedResellers.put(resellerName, r);
                                }
                                double am = 0.0d;
                                if (r.get("amount") != null) {
                                    am = ((Double) r.get("amount")).doubleValue();
                                }
                                r.put("amount", new Double(am + (oldAmount - (newAmount - refund))));
                                double rf = 0.0d;
                                if (r.get("refund") != null) {
                                    rf = ((Double) r.get("refund")).doubleValue();
                                }
                                r.put("refund", new Double(rf + refund));
                            }
                        } catch (UnknownResellerException e) {
                            System.out.println("Reseller ID:" + resId + " has skipped");
                        }
                        System.out.println("ID=" + ((Long) t2.get("id")).longValue() + "\tOLD_AMOUNT=" + oldAmount + "\tNEW_AMOUNT=" + (newAmount - refund) + " REFUND:" + refund);
                        if (force && oldAmount != newAmount - refund) {
                            updtBePs.setDouble(1, newAmount - refund);
                            updtBePs.setLong(2, ((Long) t2.get("id")).longValue());
                            updtBePs.executeUpdate();
                            System.out.println("New amount " + newAmount + "has been set");
                        }
                    }
                }
                double resellerChange = 0.0d;
                for (Hashtable r2 : affectedResellers.values()) {
                    try {
                        Account a = Reseller.getReseller(((Long) r2.get("reseller_id")).longValue()).getAccount();
                        double am2 = ((Double) r2.get("amount")).doubleValue();
                        System.out.println("Reseller:" + r2.get("reseller_name") + " balance:" + a.getBill().getBalance() + " changes:" + am2 + " new balance" + (am2 + a.getBill().getBalance()) + " refund:" + ((Double) r2.get("refund")).doubleValue());
                        resellerChange += am2;
                        if (force) {
                            updtBalancePs.setDouble(1, am2 + a.getBill().getBalance());
                            updtBalancePs.setLong(2, a.getId().getId());
                            updtBalancePs.executeUpdate();
                            System.out.println("Reseller:" + r2.get("reseller_name") + ": accountId:" + a.getId().getId() + "amount:" + (am2 + a.getBill().getBalance()) + "balance updated\t\t+");
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        System.out.println("Reseller:" + r2.get("reseller_name") + ": skipped\t\t-");
                    }
                }
                System.out.println("Resellers changes:" + resellerChange);
                System.out.println("Total old:" + oldAmountTotal + " new:" + (newAmountTotal - refundTotal) + "(refund:" + refundTotal + ") diff:" + ((oldAmountTotal - newAmountTotal) - refundTotal));
                if ((oldAmountTotal - newAmountTotal) - refundTotal == 0.0d) {
                    System.out.println("The system is fine");
                }
                if (force) {
                    updtBerPs.executeUpdate();
                    System.out.println("Amount for bill_entry_res has been reset");
                }
                Session.closeStatement(sumPs);
                Session.closeStatement(berPs);
                Session.closeStatement(berefPs);
                Session.closeStatement(bePs);
                Session.closeStatement(updtBalancePs);
                Session.commitTransConnection(con);
            } catch (Exception ex2) {
                System.out.println("Error occured");
                ex2.printStackTrace();
                Session.closeStatement(sumPs);
                Session.closeStatement(berPs);
                Session.closeStatement(berefPs);
                Session.closeStatement(bePs);
                Session.closeStatement(updtBalancePs);
                Session.commitTransConnection(con);
            }
        } catch (Throwable th) {
            Session.closeStatement(sumPs);
            Session.closeStatement(berPs);
            Session.closeStatement(berefPs);
            Session.closeStatement(bePs);
            Session.closeStatement(updtBalancePs);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public static void printHelpMessage() {
        System.out.println("NAME:\n\tpsoft.hsphere.converter.RBillsRecover\n\tH-Sphere reset disk quota bug in resellers.");
        System.out.println("SYNOPSIS:\n\tpsoft.hsphere.converter.RBillsRecover --info");
        System.out.println("OPTIONS:");
        System.out.println("\t-h|--help \t\t- shows this screen");
        System.out.println("\t--info\t- list of fixed resellers");
        System.exit(0);
    }
}
