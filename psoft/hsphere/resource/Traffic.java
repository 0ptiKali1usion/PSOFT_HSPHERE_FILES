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
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.admin.Settings;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/Traffic.class */
public class Traffic extends AbstractChangeableResource {
    public static final int WARN_PROCENT = 80;
    public static final int SUSPEND_PERCENTAGE = 150;
    public static final long TIME_TO_LIVE = 900000;
    public static final long MSEC_IN_MONTH = 2592000;
    protected double size;
    protected int tt_type;
    protected double traffic;
    protected double inTraffic;
    protected double outTraffic;
    protected long lastTimeLoaded;
    protected static String[] labels = {"KB", "MB", "GB", "TB"};

    public static double getAmount(InitToken token) {
        Iterator i = token.getValues().iterator();
        try {
            return USFormat.parseDouble((String) i.next());
        } catch (Exception e) {
            Session.getLog().warn("Problem parsing double", e);
            return 0.0d;
        }
    }

    public static double getSetupMultiplier(InitToken token) {
        return 1.0d;
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getAmount() {
        return this.size;
    }

    public double getTraffic() {
        return this.traffic;
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getTotalAmount() {
        return getAmount();
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getSetupMultiplier() {
        Session.getLog().debug("In Traffic own getSetupMultiplier");
        return 1.0d;
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
        String descr = "Traffic is within the limit:" + this.size + " GB";
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
            Session.getLog().debug("In Traffic own getUsageMultiplier");
            double defaultTraffic = getFreeNumber();
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
            TrafficRecord tr = getExtTraffic(new java.sql.Date(beginDate), new java.sql.Date(endDate));
            double usedTraffic = tr.xfer / 1048576.0d;
            Session.getLog().debug("Used traffic: " + usedTraffic);
            Session.getLog().debug("Used traffic(to month): " + usedTraffic);
            float paidTraffic = (float) Math.max(this.size, defaultTraffic);
            if (usedTraffic < paidTraffic) {
                return 0.0d;
            }
            return usedTraffic - paidTraffic;
        } catch (Exception ex) {
            Session.getLog().debug("Error getUsageMultiplier()", ex);
            return 0.0d;
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void saveSize(double newSize) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE traffics SET tt_size = ? WHERE id = ?");
            ps.setDouble(1, newSize);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.size = newSize;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static double getRecurrentMultiplier(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        double size = USFormat.parseDouble((String) i.next());
        double defaultTraffic = token.getFreeUnits();
        if (defaultTraffic >= size) {
            return 0.0d;
        }
        return size - defaultTraffic;
    }

    public void reload() {
        long now = TimeUtils.currentTimeMillis();
        if (now - this.lastTimeLoaded < 900000) {
            return;
        }
        this.lastTimeLoaded = now;
        try {
            Calendar cal = TimeUtils.getCalendar();
            Date endDate = cal.getTime();
            Date beginDate = getStartDate();
            Session.getLog().debug("reload Traffic");
            TrafficRecord tr = getExtTraffic(new java.sql.Date(beginDate.getTime()), new java.sql.Date(endDate.getTime()));
            this.traffic = tr.xfer / 1024.0d;
            this.inTraffic = tr.f162in / 1024.0d;
            this.outTraffic = tr.out / 1024.0d;
            Session.getLog().debug("Current traffic =" + this.traffic);
        } catch (Exception e) {
            Session.getLog().debug("Error reload traffic", e);
        }
    }

    public Date getStartDate() {
        Date result;
        Account a = getAccount();
        if (this.tt_type == 0 || this.tt_type == -1) {
            result = getPeriodBegin();
        } else {
            try {
                result = a.getId().findChild("traffic").get().getPeriodBegin();
            } catch (Exception e) {
                Session.getLog().warn("Summary traffic not found", e);
                result = getPeriodBegin();
            }
        }
        if (result.after(a.getCreated())) {
            Calendar cal = TimeUtils.getCalendar();
            cal.setTime(result);
            cal.add(5, -1);
            result = cal.getTime();
        }
        return result;
    }

    protected double getTraffic(java.sql.Date dBegin, java.sql.Date dEnd) throws SQLException, Exception {
        TrafficRecord tr = getExtTraffic(dBegin, dEnd);
        return tr.xfer;
    }

    protected TrafficRecord getExtTraffic(java.sql.Date dBegin, java.sql.Date dEnd) throws SQLException, Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            switch (this.tt_type) {
                case -1:
                    StringBuffer query2 = new StringBuffer("SELECT SUM(xfer) AS xfer, SUM(in_traffic) AS in_traffic, SUM(out_traffic) AS out_traffic FROM trans_log t, accounts a WHERE t.account_id = a.id AND a.reseller_id = ? AND t.cdate BETWEEN ? AND ?");
                    if (Session.useAccelerator() && Session.hasFlagField()) {
                        query2.append(" AND (flag IS NULL OR flag != 1)");
                    }
                    ps = con.prepareStatement(query2.toString());
                    ps.setLong(1, Session.getUser().getId());
                    ps.setDate(2, dBegin);
                    ps.setDate(3, dEnd);
                    break;
                case 0:
                    StringBuffer query1 = new StringBuffer("SELECT SUM(xfer) AS xfer, SUM(in_traffic) AS in_traffic, SUM(out_traffic) AS out_traffic FROM trans_log WHERE account_id = ? AND cdate BETWEEN ? AND ?");
                    if (Session.useAccelerator() && Session.hasFlagField()) {
                        query1.append(" AND (flag IS NULL OR flag != 1)");
                    }
                    ps = con.prepareStatement(query1.toString());
                    ps.setLong(1, getAccount().getId().getId());
                    ps.setDate(2, dBegin);
                    ps.setDate(3, dEnd);
                    break;
                default:
                    StringBuffer query3 = new StringBuffer("SELECT SUM(xfer) AS xfer, SUM(in_traffic) AS in_traffic, SUM(out_traffic) AS out_traffic FROM trans_log WHERE resource_id = ? AND tt_type = ? AND cdate BETWEEN ? AND ?");
                    if (Session.useAccelerator() && Session.hasFlagField()) {
                        query3.append(" AND (flag IS NULL OR flag != 1)");
                    }
                    ps = con.prepareStatement(query3.toString());
                    ps.setLong(1, getParent().getId());
                    ps.setInt(2, this.tt_type);
                    ps.setDate(3, dBegin);
                    ps.setDate(4, dEnd);
                    break;
            }
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            TrafficRecord tr = null;
            if (rs.next()) {
                tr = new TrafficRecord(rs.getDouble("xfer"), rs.getDouble("in_traffic"), rs.getDouble("out_traffic"));
            }
            Session.getLog().debug("Traffic from " + dBegin + " to " + dEnd + " Domain id:" + getParent().getId() + " tt_type:" + this.tt_type + ". Incoming traffic: " + tr.f162in + "; Outcoming traffic: " + tr.out + "; Summary traffic: " + tr.xfer);
            TrafficRecord trafficRecord = tr;
            Session.closeStatement(ps);
            con.close();
            return trafficRecord;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Traffic(ResourceId id) throws Exception {
        super(id);
        this.lastTimeLoaded = 0L;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT tt_size, tt_type FROM traffics WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.size = rs.getDouble(1);
                this.tt_type = rs.getInt(2);
            } else {
                notFound();
            }
            reload();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Traffic(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.lastTimeLoaded = 0L;
        Session.getLog().debug("Traffic Init");
        Iterator i = initValues.iterator();
        this.size = USFormat.parseDouble((String) i.next());
        this.tt_type = i.hasNext() ? Integer.parseInt((String) i.next()) : 0;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM traffics WHERE id = ?");
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

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void changeResourcePhysical(double oldSize) throws Exception {
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO traffics(id, tt_size, tt_type) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setDouble(2, this.size);
            ps.setInt(3, this.tt_type);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static String smartLabel(double kBytes) {
        Session.getLog().debug("kbytes:" + kBytes);
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
        double numberUnits = kBytes * 1024.0d;
        for (int i = 0; i < labels.length; i++) {
            if (numberUnits < 1024.0d) {
                return numberFormat.format(numberUnits) + " " + labels[i];
            }
            numberUnits /= 1024.0d;
        }
        return numberFormat.format(kBytes);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("traffic".equals(key)) {
            return new TemplateString(this.traffic);
        }
        if ("reload".equals(key)) {
            reload();
            return null;
        } else if ("size".equals(key)) {
            return new TemplateString(this.size);
        } else {
            if ("tt_type".equals(key)) {
                return new TemplateString(this.tt_type);
            }
            if ("text_traffic".equals(key)) {
                return new TemplateString(smartLabel(this.traffic));
            }
            if ("text_in_traffic".equals(key)) {
                return new TemplateString(smartLabel(this.inTraffic));
            }
            if ("text_out_traffic".equals(key)) {
                return new TemplateString(smartLabel(this.outTraffic));
            }
            if ("info".equals(key)) {
                return new TemplateString(info());
            }
            if ("start_date".equals(key)) {
                return new TemplateString(DateFormat.getDateInstance(2).format(getStartDate()));
            }
            return super.get(key);
        }
    }

    public boolean suspendLimit() {
        if (this.tt_type != 0) {
            return false;
        }
        int susp = 150;
        reload();
        try {
            susp = Integer.parseInt(Settings.get().getValue("traffic_susp"));
        } catch (Exception e) {
        }
        return (this.traffic * 100.0d) / (this.size * 1024.0d) > ((double) susp);
    }

    public boolean warnLimit() {
        if (this.tt_type != 0) {
            return false;
        }
        reload();
        int warn = 80;
        try {
            warn = Integer.parseInt(Settings.get().getValue("traffic_warn"));
        } catch (Exception e) {
        }
        return (this.traffic * 100.0d) / (this.size * 1024.0d) > ((double) warn);
    }

    public String info() {
        try {
            reload();
            return Localizer.translateMessage("traffic.overlimitinfo", new String[]{smartLabel(this.traffic), smartLabel(this.size * 1024.0d)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.traffic.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.traffic.recurrent", new Object[]{getMonthPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date start, Date end) throws Exception {
        return Localizer.translateMessage("bill.traffic.recurrent", new Object[]{token.getMonthPeriodInWords(), new Double(token.getFreeUnits()), new Double(getAmount(token)), new Double(getAmount(token) - token.getFreeUnits()), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.traffic.refundall", new Object[]{new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        Calendar cal = TimeUtils.getCalendar(begin);
        cal.add(5, -1);
        Date beginDate = cal.getTime();
        cal.setTime(end);
        cal.add(5, -1);
        Date endDate = cal.getTime();
        TrafficRecord tr = getExtTraffic(new java.sql.Date(beginDate.getTime()), new java.sql.Date(endDate.getTime()));
        double usedTraffic = tr.xfer / 1024.0d;
        double defaultTraffic = getFreeNumber();
        double paidTraffic = Math.max(this.size, defaultTraffic) * 1024.0d * 1.0d;
        Session.getLog().debug("Paid traffic:" + paidTraffic + " koef:1.0 traffic:" + usedTraffic);
        return Localizer.translateMessage("bill.traffic.usage", new Object[]{smartLabel(usedTraffic - paidTraffic), smartLabel(this.size * 1.0d * 1024.0d), f42df.format(beginDate), f42df.format(endDate)});
    }

    @Override // psoft.hsphere.Resource
    public void monthlyAction(Date nextDate) throws Exception {
        if (Session.useAccelerator() && Session.hasFlagField()) {
            Calendar cal = TimeUtils.getCalendar(getPeriodBegin());
            cal.add(5, -1);
            Date beginDate = cal.getTime();
            cal.setTime(nextDate);
            cal.add(5, -1);
            Date endDate = cal.getTime();
            getLog().info("Calc monthly action for " + getId() + "(" + getPeriodBegin() + ":" + nextDate + ")");
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE trans_log SET flag = 1 WHERE account_id = ? AND cdate BETWEEN ? AND ?");
                ps.setLong(1, getAccount().getId().getId());
                ps.setDate(2, new java.sql.Date(beginDate.getTime()));
                ps.setDate(3, new java.sql.Date(endDate.getTime()));
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.traffic.recurrent_change", new Object[]{getMonthPeriodInWords(), new Double(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.traffic.refund_change", new Object[]{new Double(-delta), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public double inUse() {
        reload();
        return getTraffic();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/Traffic$TrafficRecord.class */
    public class TrafficRecord {

        /* renamed from: in */
        double f162in;
        double out;
        double xfer;

        TrafficRecord(double xfer, double in, double out) {
            Traffic.this = r7;
            this.f162in = in;
            this.out = out;
            if (xfer == 0.0d && (in > 0.0d || out > 0.0d)) {
                this.xfer = in + out;
            } else {
                this.xfer = xfer;
            }
        }
    }

    public static String getDescription(InitToken token) throws Exception {
        return Resource.getDescription(token) + " " + smartLabel(getAmount(token) * 1024.0d);
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return TypeRegistry.getDescription(getId().getType()) + " " + smartLabel(this.size * 1024.0d);
    }
}
