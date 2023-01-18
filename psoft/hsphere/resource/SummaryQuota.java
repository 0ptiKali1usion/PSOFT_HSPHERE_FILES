package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/SummaryQuota.class */
public class SummaryQuota extends Quota {
    public static final long TIME_TO_LIVE = 900000;
    public static final int SUSPEND_PERCENTAGE = 150;
    protected long usageLoadTime;
    protected long lastDayUsageLoadTime;
    protected double used;
    protected double lastDayUsed;

    public SummaryQuota(ResourceId id) throws Exception {
        super(id);
        this.usageLoadTime = 0L;
        this.lastDayUsageLoadTime = 0L;
    }

    public SummaryQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.usageLoadTime = 0L;
        this.lastDayUsageLoadTime = 0L;
    }

    @Override // psoft.hsphere.Resource
    public BillEntry usageCharge(Date end) throws Exception {
        Calendar cal = TimeUtils.getCalendar(getPeriodBegin());
        Date now = cal.getTime();
        cal.add(5, -1);
        Date beginDate = cal.getTime();
        cal.setTime(end);
        cal.add(5, -1);
        Date endDate = cal.getTime();
        getLog().info("Calc usage for " + getId() + "(" + getPeriodBegin() + ":" + end + ")");
        double amount = calc(3, beginDate, endDate);
        if (amount < 0.0d) {
            throw new Exception("Usage fee can't be negative");
        }
        String descr = "Summary Disk usage is within the limit " + this.size + " MB";
        if (amount > 0.0d) {
            descr = getUsageChargeDescription(now, end);
        }
        double resAmount = 0.0d;
        String resDescr = null;
        if (Session.getResellerId() != 1) {
            resAmount = resellerCalc(3, beginDate, endDate);
        }
        BillEntry be = null;
        if (amount > 0.0d || resAmount > 0.0d) {
            be = getAccount().getBill().addEntry(3, TimeUtils.getDate(), getId(), descr, beginDate, endDate, null, amount);
        }
        if (Session.getResellerId() != 1 && resAmount > 0.0d) {
            resDescr = getResellerUsageChargeDescription(now, end);
            be.createResellerEntry(resAmount, resDescr);
        }
        Session.billingLog(amount, descr, resAmount, resDescr, "USAGE CHARGE");
        return be;
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getUsageMultiplier() {
        try {
            Session.getLog().debug("In SummaryQuota own getUsageMultiplier");
            double defaultUsage = getFreeNumber();
            double paidUsage = Math.max(this.size, defaultUsage);
            Calendar cal = TimeUtils.getCalendar();
            cal.add(5, -1);
            Date now = cal.getTime();
            cal.setTime(getAccount().getPeriodEnd());
            cal.add(5, -1);
            Date byAccount = cal.getTime();
            cal.setTime(getPeriodBegin());
            cal.add(2, 1);
            cal.add(5, -1);
            Date nextMonth = cal.getTime();
            long endDate = Math.min(nextMonth.getTime(), Math.min(byAccount.getTime(), now.getTime()));
            cal.setTime(getPeriodBegin());
            cal.add(5, -1);
            long beginDate = cal.getTime().getTime();
            double usedDisk = 0.0d;
            try {
                usedDisk = getUsage(new java.sql.Date(beginDate), new java.sql.Date(endDate));
            } catch (Exception ex) {
                Session.getLog().error("Error getting summary quota for account :" + Session.getAccount().getId(), ex);
            }
            if (usedDisk < paidUsage) {
                return 0.0d;
            }
            Session.getLog().debug("SummaryQuota getUsageMultiplier = " + usedDisk + " = " + paidUsage);
            return usedDisk - paidUsage;
        } catch (Exception ex2) {
            Session.getLog().debug("Error getUsageMultiplier()", ex2);
            return 0.0d;
        }
    }

    public double getUsage() {
        return getUsage(true);
    }

    public double getUsage(boolean useCach) {
        if (useCach) {
            long now = TimeUtils.currentTimeMillis();
            if (now - this.usageLoadTime < 900000) {
                return this.used;
            }
            this.usageLoadTime = now;
        }
        Calendar cal = TimeUtils.getCalendar();
        long endDate = cal.getTime().getTime();
        cal.setTime(getPeriodBegin());
        cal.add(5, -1);
        long beginDate = cal.getTime().getTime();
        try {
            this.used = getUsage(new java.sql.Date(beginDate), new java.sql.Date(endDate));
        } catch (Exception ex) {
            Session.getLog().error("Error getting summary quota for account :" + Session.getAccount().getId(), ex);
        }
        return this.used;
    }

    public double getLastDayUsage(boolean useCach) {
        if (useCach) {
            long now = TimeUtils.currentTimeMillis();
            if (now - this.lastDayUsageLoadTime < 900000) {
                return this.lastDayUsed;
            }
            this.lastDayUsageLoadTime = now;
        }
        Calendar cal = TimeUtils.getCalendar();
        long endDate = cal.getTime().getTime();
        long beginDate = TimeUtils.dropMinutes(cal.getTime()).getTime();
        try {
            this.lastDayUsed = getUsage(new java.sql.Date(beginDate), new java.sql.Date(endDate));
            if (this.used == 0.0d) {
                cal.add(5, -1);
                long beginDate2 = cal.getTime().getTime();
                this.lastDayUsed = getUsage(new java.sql.Date(beginDate2), new java.sql.Date(endDate));
            }
        } catch (Exception ex) {
            Session.getLog().error("Error getting last day's usage for account :" + Session.getAccount().getId(), ex);
        }
        return this.lastDayUsed;
    }

    public double getUsage(java.sql.Date dBegin, java.sql.Date dEnd) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT SUM(used)/COUNT(DISTINCT(CDATE)) FROM usage_log WHERE account_id = ? AND cdate BETWEEN ? AND ?");
            ps.setLong(1, Session.getAccount().getId().getId());
            ps.setDate(2, dBegin);
            ps.setDate(3, dEnd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Session.getLog().debug("Summary disk usage is " + rs.getDouble(1) + " for account #" + Session.getAccount().getId().getId());
                double round = Math.round(rs.getDouble(1) * 100.0d) / 100.0d;
                Session.closeStatement(ps);
                con.close();
                return round;
            }
            Session.closeStatement(ps);
            con.close();
            return 0.0d;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO quotas VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.size);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM quotas WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String smartLabel(double mBytes) {
        NumberFormat numberFormat = USFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(mBytes);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("limitMb".equals(key)) {
            return new TemplateString(this.size);
        }
        if ("usedMb".equals(key)) {
            return new TemplateString(smartLabel(getUsage()));
        }
        if ("lastDayUsedMb".equals(key)) {
            return new TemplateString(smartLabel(getLastDayUsage(true)));
        }
        if ("info".equals(key)) {
            return new TemplateString(info());
        }
        if ("start_date".equals(key)) {
            return new TemplateString(DateFormat.getDateInstance(2).format(getStartDate()));
        }
        if ("short_start_date".equals(key)) {
            return new TemplateString(DateFormat.getDateInstance(3).format(getStartDate()));
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean suspendLimit() {
        int susp = 150;
        if (getUsage() == 0.0d && this.size == 0) {
            return false;
        }
        try {
            susp = Integer.parseInt(Settings.get().getValue("disk_usage_susp"));
        } catch (Exception e) {
        }
        return (getUsage() * 100.0d) / ((double) this.size) > ((double) susp);
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        int warn;
        double value = getUsage();
        if (getUsage() == 0.0d && this.size == 0) {
            return false;
        }
        try {
            warn = Integer.parseInt(Settings.get().getValue("disk_usage_warn"));
        } catch (Exception e) {
            warn = 80;
        }
        return (value * 100.0d) / ((double) this.size) > ((double) warn);
    }

    @Override // psoft.hsphere.resource.Quota
    public String info() {
        try {
            getParent().get();
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"Summary quota", "account", String.valueOf(Session.getAccount().getId().getId()), smartLabel(getUsage()), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return 0L;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.summary_usage.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.summary_usage.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.summary_usage.recurrent", new Object[]{token.getPeriodInWords(), new Double(token.getFreeUnits()), new Double(getAmount(token)), new Double(getAmount(token) - token.getFreeUnits()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.summary_usage.refundall", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        Calendar cal = TimeUtils.getCalendar(begin);
        cal.add(5, -1);
        Date beginDate = cal.getTime();
        cal.setTime(end);
        cal.add(5, -1);
        Date endDate = cal.getTime();
        return Localizer.translateMessage("bill.summary_usage.usage", new Object[]{new Double(this.size), new Double(getUsage()), smartLabel(getUsageMultiplier()), f42df.format(beginDate), f42df.format(endDate)});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.summary_usage.recurrent_change", new Object[]{getMonthPeriodInWords(), new Double(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.summary_usage.refund_change", new Object[]{new Double(-delta), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota
    public void setQuota() throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota
    protected String[] getActualQuotaReport() {
        String[] rep = {new Double(getUsage(false)).toString(), new Double(getAmount()).toString()};
        return rep;
    }

    public Date getStartDate() {
        Date result = getPeriodBegin();
        Account a = getAccount();
        if (result.after(a.getCreated())) {
            Calendar cal = TimeUtils.getCalendar();
            cal.setTime(result);
            cal.add(5, -1);
            result = cal.getTime();
        }
        return result;
    }
}
