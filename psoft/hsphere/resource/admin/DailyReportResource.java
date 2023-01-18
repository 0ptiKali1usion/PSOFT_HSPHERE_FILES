package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.calc.Calc;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/DailyReportResource.class */
public class DailyReportResource extends Resource {
    protected final String shortDateFormat = "MM/dd/yyyy";
    protected final int NEW_ACCOUNT = 1;
    protected final int CANCELED_ACCOUNT = 2;
    protected final int FAILED_ACCOUNT = 2;
    protected Date date1;
    protected Date date2;

    public DailyReportResource(int type, Collection initValue) throws Exception {
        super(type, initValue);
        this.shortDateFormat = "MM/dd/yyyy";
        this.NEW_ACCOUNT = 1;
        this.CANCELED_ACCOUNT = 2;
        this.FAILED_ACCOUNT = 2;
    }

    public DailyReportResource(ResourceId rid) throws Exception {
        super(rid);
        this.shortDateFormat = "MM/dd/yyyy";
        this.NEW_ACCOUNT = 1;
        this.CANCELED_ACCOUNT = 2;
        this.FAILED_ACCOUNT = 2;
    }

    public TemplateModel FM_defaultDate() throws Exception {
        Calendar calend = TimeUtils.getCalendar();
        calend.add(5, -1);
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        return new TemplateString(dateFormat.format(calend.getTime()));
    }

    public TemplateModel FM_initParams(String sdate1, String sdate2) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        Calendar calend = TimeUtils.getCalendar();
        if ("".equals(sdate1)) {
            calend.add(5, -1);
            this.date1 = calend.getTime();
        } else {
            try {
                this.date1 = dateFormat.parse(sdate1);
            } catch (ParseException e) {
                calend.add(5, -1);
                this.date1 = calend.getTime();
                calend.add(5, 1);
                this.date2 = calend.getTime();
                throw new HSUserException("dailyreportresource.from");
            }
        }
        if ("".equals(sdate2)) {
            calend.setTime(this.date1);
            calend.add(5, 1);
            this.date2 = calend.getTime();
        } else {
            try {
                this.date2 = dateFormat.parse(sdate2);
            } catch (ParseException e2) {
                calend.setTime(this.date1);
                calend.add(5, 1);
                this.date2 = calend.getTime();
                throw new HSUserException("dailyreportresource.to");
            }
        }
        return this;
    }

    public TemplateList FM_newAccounts() throws Exception {
        return getAccountList(1);
    }

    public TemplateList FM_canceledAccounts() throws Exception {
        return getAccountList(2);
    }

    public TemplateList FM_failedAccounts() throws Exception {
        return getAccountList(2);
    }

    public TemplateList getAccountList(int type) throws Exception {
        String query;
        Iterator i;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if (type == 1) {
            query = "SELECT id, plan_id, period_id FROM accounts WHERE created >= ? AND created < ? AND bi_id <> 0 AND ((failed = 0) OR (failed is null)) AND reseller_id = ? ORDER BY plan_id, period_id";
        } else if (type == 2) {
            query = "SELECT id, plan_id, period_id FROM accounts WHERE deleted >= ? AND deleted < ? AND failed = 1  AND reseller_id = ?  AND bi_id <> 0 ORDER BY plan_id, period_id";
        } else {
            query = "SELECT id, plan_id, period_id FROM accounts WHERE deleted >= ? AND deleted < ? AND failed = 0  AND reseller_id = ?  AND bi_id <> 0 ORDER BY plan_id, period_id";
        }
        try {
            ps = con.prepareStatement(query);
            ps.setDate(1, new java.sql.Date(this.date1.getTime()));
            ps.setDate(2, new java.sql.Date(this.date2.getTime()));
            ps.setLong(3, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            double sumRealAmount = 0.0d;
            double sumAssumedAmount = 0.0d;
            String oldPlanId = "";
            double oldPeriod = 0.0d;
            int count = 0;
            NumberFormat nFormatter = NumberFormat.getNumberInstance();
            while (rs.next()) {
                int id = rs.getInt(1);
                String planId = rs.getString(2);
                if (type == 1) {
                    try {
                        i = getNewAccountParams(id);
                    } catch (Exception e) {
                        Session.getLog().debug("Error getting account calculation,skip account " + id, e);
                    }
                } else if (type == 2) {
                    i = getFailedAccountParams(id);
                } else {
                    i = getCanceledAccountParams(id);
                }
                double period_month = ((Double) i.next()).doubleValue();
                double signupAmount = ((Double) i.next()).doubleValue();
                double assumedAmount = ((Double) i.next()).doubleValue();
                if ("".equals(oldPlanId)) {
                    oldPlanId = planId;
                    oldPeriod = period_month;
                }
                if (!oldPlanId.equals(planId) || oldPeriod != period_month) {
                    TemplateMap map = new TemplateMap();
                    map.put("plan", Plan.getPlan(oldPlanId));
                    map.put("period_month", nFormatter.format(oldPeriod));
                    map.put("count", Integer.toString(count));
                    map.put("real_amount", USFormat.format(sumRealAmount));
                    map.put("assumed_amount", USFormat.format(sumAssumedAmount));
                    list.add((TemplateModel) map);
                    oldPlanId = planId;
                    oldPeriod = period_month;
                    sumRealAmount = 0.0d;
                    sumAssumedAmount = 0.0d;
                    count = 0;
                }
                sumRealAmount += signupAmount;
                sumAssumedAmount += assumedAmount;
                count++;
            }
            if (count > 0) {
                TemplateMap map2 = new TemplateMap();
                map2.put("plan", Plan.getPlan(oldPlanId));
                map2.put("period_month", nFormatter.format(oldPeriod));
                map2.put("count", Integer.toString(count));
                map2.put("real_amount", USFormat.format(sumRealAmount));
                map2.put("assumed_amount", USFormat.format(sumAssumedAmount));
                list.add((TemplateModel) map2);
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private Iterator getNewAccountParams(int id) throws Exception {
        double period_month;
        double signupAmount;
        double recurrent;
        ArrayList res = new ArrayList();
        Account oldAccount = Session.getAccount();
        Account newAccount = (Account) Account.get(new ResourceId(id, 0));
        try {
            Session.setAccount(newAccount);
            if (newAccount.getBillingInfo().getBillingType() == 0) {
                period_month = 0.0d;
                signupAmount = 0.0d;
                recurrent = 0.0d;
            } else {
                period_month = Calc.getMultiplier();
                Bill bill = (Bill) newAccount.getBills().next();
                signupAmount = bill.getTotal();
                recurrent = 0.0d;
                Iterator i = bill.getTypeEntries(2);
                while (i.hasNext()) {
                    BillEntry entry = (BillEntry) i.next();
                    recurrent += entry.getAmount();
                }
            }
            double assumedAmount = (((recurrent / period_month) * 12.0d) - recurrent) + signupAmount;
            res.add(new Double(period_month));
            res.add(new Double(signupAmount));
            res.add(new Double(assumedAmount));
            return res.iterator();
        } finally {
            Session.setAccount(oldAccount);
        }
    }

    private Iterator getFailedAccountParams(int id) throws Exception {
        ArrayList res = new ArrayList();
        res.add(new Double(0.0d));
        res.add(new Double(0.0d));
        res.add(new Double(0.0d));
        return res.iterator();
    }

    private Iterator getCanceledAccountParams(int id) throws Exception {
        double recurrent = 0.0d;
        double refund = 0.0d;
        double payed = 0.0d;
        ArrayList res = new ArrayList();
        Account oldAccount = Session.getAccount();
        Account newAccount = (Account) Account.get(new ResourceId(id, 0));
        try {
            Session.setAccount(newAccount);
            double period_month = Calc.getMultiplier();
            Bill bill = (Bill) newAccount.getBills().next();
            double signupAmount = bill.getTotal();
            Iterator i = bill.getTypeEntries(2);
            while (i.hasNext()) {
                recurrent += ((BillEntry) i.next()).getAmount();
            }
            Object[] entryArray = ((Bill) newAccount.getActualBills().next()).getEntries().toArray();
            for (int i2 = entryArray.length - 1; i2 >= 0; i2--) {
                BillEntry entry = (BillEntry) entryArray[i2];
                if (entry.getType() == 4) {
                    refund += entry.getAmount();
                }
                if (entry.getType() == 2 || entry.getType() == 1) {
                    break;
                }
            }
            Iterator i3 = newAccount.getBills();
            while (i3.hasNext()) {
                payed += ((Bill) i3.next()).getTotal();
            }
            double assumedAmount = ((((recurrent / period_month) * 12.0d) - recurrent) + signupAmount) - payed;
            res.add(new Double(period_month));
            res.add(new Double(-refund));
            res.add(new Double(assumedAmount));
            return res.iterator();
        } finally {
            Session.setAccount(oldAccount);
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("date1") ? new TemplateString(new SimpleDateFormat("MM/dd/yyyy").format(this.date1)) : key.equals("date2") ? new TemplateString(new SimpleDateFormat("MM/dd/yyyy").format(this.date2)) : super.get(key);
    }
}
