package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/ResellerCron.class */
public class ResellerCron extends BackgroundJob {

    /* renamed from: df */
    protected static DateFormat f85df = DateFormat.getDateInstance(3);

    public ResellerCron(C0004CP cp) throws Exception {
        super(cp, "RESELLER");
    }

    public ResellerCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        getLog().info("Starting Reseller Cron");
        Date startDate = TimeUtils.getDate();
        setProgress(Reseller.getResellerList().size(), 1, 0);
        Iterator r = Reseller.getResellerList().iterator();
        while (true) {
            if (!r.hasNext()) {
                break;
            } else if (isInterrupted()) {
                getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
                break;
            } else {
                checkSuspended();
                Reseller resel = r.next();
                if (resel == null || !resel.isSuspended() || resel.getId() == 1) {
                    try {
                        try {
                            Account a = resel.getAccount();
                            Session.setAccount(a);
                            Session.setUser(a.getUser());
                            synchronized (a) {
                                calculateReseler(resel, a, resel.getResourceId());
                            }
                            Session.setUser(null);
                            Session.setAccount(null);
                        } catch (Exception ex) {
                            getLog().error("Error calc Reseller#:" + resel.getId(), ex);
                            Session.setUser(null);
                            Session.setAccount(null);
                        } catch (Throwable ex2) {
                            getLog().warn("Billing error Reseller#:" + resel.getId(), ex2);
                            Session.setUser(null);
                            Session.setAccount(null);
                        }
                        addProgress(1);
                    } catch (Throwable th) {
                        Session.setUser(null);
                        Session.setAccount(null);
                        throw th;
                    }
                }
            }
        }
        long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
        getLog().info("Resellers Cron FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
    }

    /* JADX WARN: Multi-variable type inference failed */
    protected void calculateReseler(Reseller resel, Account a, ResourceId resRid) throws Exception {
        Session.setAccount(a);
        Date now = TimeUtils.getDate();
        Date beginPeriod = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement psD = con.prepareStatement("SELECT MAX(created) FROM bill_entry WHERE bill_id IN (SELECT id FROM bill WHERE account_id = ?) AND  type IN (12,13,14,15)");
            psD.setLong(1, a.getId().getId());
            ResultSet rsD = psD.executeQuery();
            if (rsD.next()) {
                beginPeriod = rsD.getTimestamp(1);
                getLog().debug("Begin date for reseller Id:" + resel.getId() + " found = " + beginPeriod + " accountId:" + a.getId().getId() + " acount bill created date :" + a.getCreated());
            }
            Date date = beginPeriod;
            Date beginPeriod2 = beginPeriod;
            if (date == null) {
                Date beginPeriod3 = new Date(a.getCreated().getTime());
                beginPeriod2 = beginPeriod3;
                if (beginPeriod3 == null) {
                    throw new Exception("Can`t find last updated date!!!");
                }
            }
            Date beginPeriod4 = TimeUtils.dropMinutes(beginPeriod2);
            Date endPeriod = TimeUtils.dropMinutes(TimeUtils.getDate());
            getLog().debug("Reseller Id:" + resel.getId() + " Begin date:" + beginPeriod4 + " endDate:" + endPeriod);
            PreparedStatement ps = con.prepareStatement("SELECT b.type, sum(r.amount) FROM bill_entry b, bill_entry_res r WHERE b.id = r.id  AND r.reseller_id = ? AND b.created >= ? AND b.created < ? AND b.canceled IS NULL GROUP BY b.type");
            ps.setLong(1, resel.getId());
            ps.setTimestamp(2, new Timestamp(beginPeriod4.getTime()));
            ps.setTimestamp(3, new Timestamp(endPeriod.getTime()));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getDouble(2) != 0.0d) {
                    getLog().debug("Reseller Id:" + resRid.getId() + " Type:" + rs.getInt(1) + " amount:" + rs.getDouble(2));
                    switch (rs.getInt(1)) {
                        case 1:
                            a.getBill().addEntry(12, endPeriod, resRid, Localizer.translateMessage("bill.b_reseller_setup", new Object[]{f85df.format(beginPeriod4), f85df.format(endPeriod)}), beginPeriod4, endPeriod, null, rs.getDouble(2));
                            break;
                        case 2:
                            a.getBill().addEntry(13, endPeriod, resRid, Localizer.translateMessage("bill.b_reseller_recurrent", new Object[]{f85df.format(beginPeriod4), f85df.format(endPeriod)}), beginPeriod4, endPeriod, null, rs.getDouble(2));
                            break;
                        case 3:
                            a.getBill().addEntry(14, endPeriod, resRid, Localizer.translateMessage("bill.b_reseller_usage", new Object[]{f85df.format(beginPeriod4), f85df.format(endPeriod)}), beginPeriod4, endPeriod, null, rs.getDouble(2));
                            break;
                        case 4:
                            a.getBill().addEntry(15, endPeriod, resRid, Localizer.translateMessage("bill.b_reseller_refund", new Object[]{f85df.format(beginPeriod4), f85df.format(endPeriod)}), beginPeriod4, endPeriod, null, rs.getDouble(2));
                            break;
                        case 10:
                            a.getBill().addEntry(15, endPeriod, resRid, Localizer.translateMessage("bill.b_reseller_money", new Object[]{f85df.format(beginPeriod4), f85df.format(endPeriod)}), beginPeriod4, endPeriod, null, rs.getDouble(2));
                            break;
                        case Resource.B_REFUND_ALL /* 104 */:
                            a.getBill().addEntry(15, endPeriod, resRid, Localizer.translateMessage("bill.b_reseller_refund_all", new Object[]{f85df.format(beginPeriod4), f85df.format(endPeriod)}), beginPeriod4, endPeriod, null, rs.getDouble(2));
                            break;
                    }
                }
            }
            a.getBill().charge(a.getBillingInfo());
            a.sendInvoice(now);
            con.close();
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }
}
