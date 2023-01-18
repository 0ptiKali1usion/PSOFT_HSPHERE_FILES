package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.Date;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/RevenueCron.class */
public class RevenueCron extends BackgroundJob {
    public RevenueCron(C0004CP cp) throws Exception {
        super(cp, "REVENUE");
    }

    public RevenueCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    /* JADX WARN: Multi-variable type inference failed */
    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        Date lastDate;
        checkSuspended();
        getLog().info("Starting Revenue Cron");
        Date startDate = TimeUtils.getDate();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                setProgress(Reseller.getResellerList().size(), 1, 0);
                for (Reseller reseller : Reseller.getResellerList()) {
                    long resId = reseller.getId();
                    try {
                        Date lastDate2 = null;
                        ps = con.prepareStatement("SELECT MAX(cdate) FROM revenue r, plans p WHERE r.plan_id = p.id AND reseller_id = ?");
                        ps.setLong(1, resId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            lastDate2 = rs.getDate(1);
                        }
                        if (lastDate2 == null) {
                            ps.close();
                            ps = con.prepareStatement("SELECT MIN(created) FROM accounts WHERE reseller_id = ?");
                            ps.setLong(1, resId);
                            ResultSet rs2 = ps.executeQuery();
                            if (rs2.next()) {
                                lastDate = rs2.getTimestamp(1);
                            } else {
                                lastDate = TimeUtils.getDate();
                            }
                            Calendar cal = TimeUtils.getCalendar(TimeUtils.dropMinutes(lastDate));
                            cal.add(5, -1);
                            lastDate2 = cal.getTime();
                        }
                        Calendar begin = TimeUtils.getCalendar(lastDate2);
                        begin.add(5, 1);
                        Date end = TimeUtils.dropMinutes(TimeUtils.getDate());
                        getLog().debug("Revenue TODO: from " + begin.getTime() + " to " + end.toString() + ", Reseller #" + resId);
                        Session.setResellerId(resId);
                        while (begin.getTime().before(end)) {
                            getLog().debug("Revenue Cron: Processing " + begin.getTime());
                            Connection transCon = Session.getTransConnection();
                            try {
                                try {
                                    for (Plan p : Plan.getPlanList()) {
                                        if (p.getBilling() != 0) {
                                            processPlan(p.getId(), begin.getTime(), transCon);
                                        }
                                    }
                                    begin.add(5, 1);
                                    Session.commitTransConnection(transCon);
                                } catch (Exception ex) {
                                    transCon.rollback();
                                    throw ex;
                                    break;
                                }
                            } catch (Throwable th) {
                                Session.commitTransConnection(transCon);
                                throw th;
                                break;
                            }
                        }
                    } catch (Throwable ex2) {
                        getLog().debug("Problem with the reseller: " + Long.toString(resId), ex2);
                    }
                    addProgress(1, "Finished processing reseller #" + resId);
                    saveLastUser(resId);
                }
                long timeDiff = TimeUtils.currentTimeMillis() - startDate.getTime();
                getLog().info("Revenue Cron FINISHED. Process took: " + (timeDiff / 60000) + " min " + ((timeDiff - ((timeDiff / 60000) * 60000)) / 1000) + " sec");
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                getLog().error("Revenue Cron error: ", e);
                Session.closeStatement(null);
                con.close();
            }
        } catch (Throwable th2) {
            Session.closeStatement(null);
            con.close();
            throw th2;
        }
    }

    protected void processPlan(int planId, Date workDate, Connection con) throws Exception {
        try {
            double setup = 0.0d;
            double refund = 0.0d;
            double recurrent = 0.0d;
            double usage = 0.0d;
            double special = 0.0d;
            double moneyback = 0.0d;
            Calendar toDate = TimeUtils.getCalendar(workDate);
            toDate.add(5, 1);
            PreparedStatement ps = con.prepareStatement("SELECT SUM(be.amount) FROM bill_entry be WHERE be.created >= ? AND be.created < ? AND be.canceled IS NULL AND be.plan_id = ? AND type = ? AND EXISTS (SELECT * FROM bill b , accounts a WHERE b.id = bill_entry.bill_id AND a.id = b.account_id AND (a.failed = 0 OR a.failed IS NULL))");
            ps.setDate(1, new java.sql.Date(workDate.getTime()));
            ps.setDate(2, new java.sql.Date(toDate.getTime().getTime()));
            ps.setInt(3, planId);
            ps.setInt(4, 1);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                setup = rs.getDouble(1);
            }
            ps.setInt(4, 2);
            ResultSet rs2 = ps.executeQuery();
            if (rs2.next()) {
                recurrent = rs2.getDouble(1);
            }
            ps.setInt(4, 3);
            ResultSet rs3 = ps.executeQuery();
            if (rs3.next()) {
                usage = rs3.getDouble(1);
            }
            ps.setInt(4, 4);
            ResultSet rs4 = ps.executeQuery();
            if (rs4.next()) {
                refund = rs4.getDouble(1);
            }
            ps.setInt(4, Resource.B_REFUND_ALL);
            ResultSet rs5 = ps.executeQuery();
            if (rs5.next()) {
                refund += rs5.getDouble(1);
            }
            ps.setInt(4, 10);
            ResultSet rs6 = ps.executeQuery();
            if (rs6.next()) {
                moneyback = rs6.getDouble(1);
            }
            if (setup == 0.0d && recurrent == 0.0d && usage == 0.0d && refund == 0.0d && moneyback == 0.0d) {
                PreparedStatement ps3 = con.prepareStatement("DELETE from revenue WHERE plan_id = ? and setup = ? and special=? and recurrent = ? and usage = ? and refund = ? and moneyback = ?");
                ps3.setInt(1, planId);
                ps3.setDouble(2, 0.0d);
                ps3.setDouble(3, 0.0d);
                ps3.setDouble(4, 0.0d);
                ps3.setDouble(5, 0.0d);
                ps3.setDouble(6, 0.0d);
                ps3.setDouble(7, 0.0d);
                ps3.executeUpdate();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO revenue(cdate, plan_id, setup, special, recurrent, usage, refund, moneyback) VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
                ps2.setDate(1, new java.sql.Date(workDate.getTime()));
                ps2.setInt(2, planId);
                ps2.setDouble(3, 0.0d);
                ps2.setDouble(4, 0.0d);
                ps2.setDouble(5, 0.0d);
                ps2.setDouble(6, 0.0d);
                ps2.setDouble(7, 0.0d);
                ps2.setDouble(8, 0.0d);
                ps2.executeUpdate();
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                return;
            }
            ps.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT SUM(amount) FROM bill_entry WHERE created >= ? AND created < ? AND canceled IS NULL AND plan_id = ? AND type = ? AND rtype = ? AND EXISTS (SELECT * FROM bill b , accounts a WHERE b.id = bill_entry.bill_id AND a.id = b.account_id AND (a.failed = 0 OR a.failed IS NULL))");
            ps4.setDate(1, new java.sql.Date(workDate.getTime()));
            ps4.setDate(2, new java.sql.Date(toDate.getTime().getTime()));
            ps4.setInt(3, planId);
            ps4.setInt(4, 1);
            ps4.setInt(5, 14);
            ResultSet rs7 = ps4.executeQuery();
            if (rs7.next()) {
                special = rs7.getDouble(1);
            }
            PreparedStatement ps22 = con.prepareStatement("INSERT INTO revenue(cdate, plan_id, setup, special, recurrent, usage, refund, moneyback) VALUES(?, ?, ?, ?, ?, ?, ?, ?)");
            ps22.setDate(1, new java.sql.Date(workDate.getTime()));
            ps22.setInt(2, planId);
            ps22.setDouble(3, setup - special);
            ps22.setDouble(4, special);
            ps22.setDouble(5, recurrent);
            ps22.setDouble(6, usage);
            ps22.setDouble(7, refund < 0.0d ? -refund : 0.0d);
            ps22.setDouble(8, moneyback < 0.0d ? -moneyback : 0.0d);
            ps22.executeUpdate();
            Session.closeStatement(ps4);
            Session.closeStatement(ps22);
        } catch (Throwable th) {
            Session.closeStatement(null);
            Session.closeStatement(null);
            throw th;
        }
    }
}
