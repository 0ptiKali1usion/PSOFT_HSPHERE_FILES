package psoft.hsphere.resource.p003ds;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.p001ds.DSNetInterface;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.ds.BandwidthResource */
/* loaded from: hsphere.zip:psoft/hsphere/resource/ds/BandwidthResource.class */
public class BandwidthResource extends Resource {
    public static final int WARN_PROCENT = 80;
    public static final int SUSPEND_PERCENTAGE = 150;
    public static final long TIME_TO_LIVE = 900000;
    public static final long MSEC_IN_MONTH = 2592000;
    protected double bandwidth;
    protected double bandwidthInout;
    protected double bandwidthOut;
    protected double size;
    protected BandwidthType type;
    protected long lastTimeLoaded;
    static final double BANDWIDTHLIKE_COEF = 9.5367431640625E-7d;
    static final double TRAFFICLIKE_COEF = 9.313225746154785E-10d;
    protected static String[] trafficlikeLabels = {"MB", "GB", "TB"};
    protected static String[] bandwidthlikeLabels = {"kbps", "mbps", "gbps"};

    public BandwidthResource(int resType, Collection initValues) throws Exception {
        super(resType, initValues);
        this.type = null;
        this.lastTimeLoaded = 0L;
        Session.getLog().debug("Bandwidth Init");
        Iterator i = initValues.iterator();
        this.size = USFormat.parseDouble((String) i.next());
        this.type = getPlanBandwidthType();
    }

    public BandwidthResource(ResourceId id) throws Exception {
        super(id);
        this.type = null;
        this.lastTimeLoaded = 0L;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT size, type FROM ds_bandwidth WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.size = rs.getDouble(1);
                this.type = BandwidthType.getType(rs.getInt(2));
                if (this.type == BandwidthType.UNKNOWN) {
                    throw new Exception("Unable to determine a bandwidth type, specified as value '" + rs.getInt(2) + "' in the ds_bandwidth table.");
                }
            } else {
                notFound();
            }
            reload();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public final BandwidthType getPlanBandwidthType() throws Exception {
        return getPlanBandwidthType(getAccount().getPlan());
    }

    public BandwidthType getPlanBandwidthType(Plan otherPlan) throws Exception {
        String planValue = otherPlan.getValue(getId().getType(), "_BANDWIDTH_TYPE_");
        BandwidthType bt = planValue != null ? BandwidthType.getType(planValue) : BandwidthType.UNKNOWN;
        if (bt == BandwidthType.UNKNOWN) {
            throw new Exception("Unable to determine a bandwidth type, specified as value '" + planValue + "' in the plan #" + getAccount().getPlan().getId());
        }
        return bt;
    }

    public static BandwidthType getPlanBandwidthType(Plan newPlan, ResourceType resType) throws Exception {
        String planValue = newPlan.getValue(resType.getId(), "_BANDWIDTH_TYPE_");
        BandwidthType bt = planValue != null ? BandwidthType.getType(planValue) : BandwidthType.UNKNOWN;
        if (bt == BandwidthType.UNKNOWN) {
            throw new Exception("Unable to determine a bandwidth type, specified as value '" + planValue + "' in the plan #" + newPlan.getId());
        }
        return bt;
    }

    public BandwidthType getBandwidthType() throws Exception {
        return this.type;
    }

    List getNetInterfaces() throws Exception {
        List res = new ArrayList();
        TemplateList dsList = getAccount().FM_findChildren("ds");
        while (dsList.hasNext()) {
            DedicatedServerResource dsr = dsList.next();
            DedicatedServer ds = dsr.getDSObject();
            res.addAll(ds.getNetInterfaces());
        }
        return res;
    }

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

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return this.size;
    }

    public double getBandwidth() {
        return this.bandwidth;
    }

    @Override // psoft.hsphere.Resource
    public double getTotalAmount() {
        return getAmount();
    }

    @Override // psoft.hsphere.Resource
    public double getSetupMultiplier() {
        Session.getLog().debug("In Bandwidth own getSetupMultiplier");
        return 1.0d;
    }

    @Override // psoft.hsphere.Resource
    public BillEntry usageCharge(Date end) throws Exception {
        BillEntry be = null;
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
        String descr = getUsageChargeDescription(now, end);
        if (amount > 0.0d) {
            be = getAccount().getBill().addEntry(3, TimeUtils.getDate(), getId(), descr, beginDate, endDate, null, amount);
        }
        Session.billingLog(amount, descr, 0.0d, null, "USAGE CHARGE");
        return be;
    }

    @Override // psoft.hsphere.Resource
    public double getUsageMultiplier() {
        try {
            Session.getLog().debug("In Bandwidth own getUsageMultiplier");
            double defaultBandwidth = getFreeNumber();
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
            BandwidthRecord br = getExtBandwidth(new java.sql.Date(beginDate), new java.sql.Date(endDate));
            double usedBandwidth = br.current;
            Session.getLog().debug("Used bandwidth: " + usedBandwidth);
            float paidBandwidth = (float) Math.max(this.size, defaultBandwidth);
            if (usedBandwidth < paidBandwidth) {
                return 0.0d;
            }
            return usedBandwidth - paidBandwidth;
        } catch (Exception ex) {
            Session.getLog().debug("Error getUsageMultiplier()", ex);
            return 0.0d;
        }
    }

    public static double getRecurrentMultiplier(InitToken token) throws Exception {
        BandwidthResource curRes = (BandwidthResource) token.getRes();
        if (curRes != null) {
            BandwidthType newBType = curRes.getPlanBandwidthType(token.getPlan());
            if (newBType != curRes.getBandwidthType()) {
                return 0.0d;
            }
        }
        Iterator i = token.getValues().iterator();
        double size = USFormat.parseDouble((String) i.next());
        double defaultBandwidth = token.getFreeUnits();
        if (defaultBandwidth >= size) {
            return 0.0d;
        }
        return size - defaultBandwidth;
    }

    @Override // psoft.hsphere.Resource
    public double getRecurrentMultiplier() {
        double defaultBandwidth = getFreeNumber();
        if (defaultBandwidth >= this.size) {
            return 0.0d;
        }
        return this.size - defaultBandwidth;
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
            Session.getLog().debug("Reloading bandwidth.");
            BandwidthRecord br = getExtBandwidth(new java.sql.Date(beginDate.getTime()), new java.sql.Date(endDate.getTime()));
            this.bandwidth = br.current;
            this.bandwidthInout = br.inout;
            this.bandwidthOut = br.out;
            Session.getLog().debug("Current bandwidth = " + this.bandwidth);
        } catch (Exception e) {
            Session.getLog().debug("Error while reloading bandwidth", e);
        }
    }

    public Date getStartDate() {
        Account a = getAccount();
        Date result = getPeriodBegin();
        if (result.after(a.getCreated())) {
            Calendar cal = TimeUtils.getCalendar();
            cal.setTime(result);
            cal.add(5, -1);
            result = cal.getTime();
        }
        return result;
    }

    protected double getBandwidth(java.sql.Date dBegin, java.sql.Date dEnd) throws SQLException, Exception {
        BandwidthRecord br = getExtBandwidth(dBegin, dEnd);
        return br.current;
    }

    public BandwidthRecord getExtBandwidth(java.sql.Date dBegin, java.sql.Date dEnd) throws SQLException, Exception {
        double inout;
        double out;
        double current;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT SUM(inout_95percentile), SUM(inout_average), SUM(out_95percentile), SUM(out_average) FROM ds_bandwidth_log WHERE account_id = ? AND cdate BETWEEN ? AND ?");
            ps.setLong(1, getAccount().getId().getId());
            ps.setDate(2, dBegin);
            ps.setDate(3, dEnd);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            BandwidthRecord br = null;
            double coef = this.type.isTrafficlike() ? (((dEnd.getTime() - dBegin.getTime()) / 1000) * TRAFFICLIKE_COEF) / 8.0d : BANDWIDTHLIKE_COEF;
            if (rs.next()) {
                switch (this.type.toInt()) {
                    case 1:
                        inout = rs.getDouble(1) * coef;
                        out = rs.getDouble(3) * coef;
                        current = inout;
                        break;
                    case 2:
                        inout = rs.getDouble(2) * coef;
                        out = rs.getDouble(4) * coef;
                        current = inout;
                        break;
                    case 3:
                        inout = rs.getDouble(1) * coef;
                        out = rs.getDouble(3) * coef;
                        current = inout;
                        break;
                    case 4:
                        inout = rs.getDouble(2) * coef;
                        out = rs.getDouble(4) * coef;
                        current = inout;
                        break;
                    case 5:
                        inout = rs.getDouble(1) * coef;
                        out = rs.getDouble(3) * coef;
                        current = out;
                        break;
                    case 6:
                        inout = rs.getDouble(2) * coef;
                        out = rs.getDouble(4) * coef;
                        current = out;
                        break;
                    case 7:
                        inout = rs.getDouble(1) * coef;
                        out = rs.getDouble(3) * coef;
                        current = out;
                        break;
                    case 8:
                        inout = rs.getDouble(2) * coef;
                        out = rs.getDouble(4) * coef;
                        current = out;
                        break;
                    default:
                        throw new Exception("Cannot process 'ds_bandwidth_log' for type '" + getBandwidthType() + "'.");
                }
                br = new BandwidthRecord(inout, out, current);
            }
            Session.getLog().debug("Bandwidth from " + dBegin + " to " + dEnd + ". Combined bandwidth (in+out): " + br.inout + "; Outcoming bandwidth: " + br.out + "; Current bandwidth: " + br.current);
            BandwidthRecord bandwidthRecord = br;
            Session.closeStatement(ps);
            con.close();
            return bandwidthRecord;
        } catch (Throwable th) {
            Session.closeStatement(null);
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
            ps = con.prepareStatement("DELETE FROM ds_bandwidth WHERE id = ?");
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

    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.Resource
    public synchronized double changeResource(Plan newPlan, int periodId, Collection values) throws Exception {
        double newSize = 0.0d;
        double oldSize = this.size;
        BandwidthType newBType = getPlanBandwidthType(newPlan);
        if (newBType == getBandwidthType()) {
            Iterator i = values.iterator();
            try {
                newSize = USFormat.parseDouble((String) i.next());
            } catch (Exception e) {
                Session.getLog().warn("Problem parsing double", e);
            }
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE ds_bandwidth SET size = ? WHERE id = ?");
                ps.setDouble(1, newSize);
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                this.size = newSize;
                Session.closeStatement(ps);
                con.close();
                return oldSize;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        ResourceType newRT = newPlan.getResourceType(getId().getType());
        try {
            newSize = USFormat.parseDouble(newRT.getValue("_FREE_UNITS_"));
        } catch (Exception e2) {
            Session.getLog().warn("Problem parsing double", e2);
        }
        Connection con2 = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            ps2 = con2.prepareStatement("UPDATE ds_bandwidth SET size = ?, type = ? WHERE id = ?");
            ps2.setDouble(1, newSize);
            ps2.setInt(2, newBType.toInt());
            ps2.setLong(3, getId().getId());
            ps2.executeUpdate();
            this.size = newSize;
            this.type = newBType;
            Session.closeStatement(ps2);
            con2.close();
            return oldSize;
        } catch (Throwable th2) {
            Session.closeStatement(ps2);
            con2.close();
            throw th2;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ds_bandwidth(id, size, type) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setDouble(2, this.size);
            ps.setInt(3, this.type.toInt());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            setNetinterfaceStartDate();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String smartLabel(double amount) {
        return smartLabel(amount, this.type);
    }

    public static String smartLabel(double amount, BandwidthType bandwidthType) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
        String[] labels = bandwidthType.isTrafficlike() ? trafficlikeLabels : bandwidthlikeLabels;
        double numberUnits = amount * 1024.0d;
        for (int i = 0; i < labels.length; i++) {
            if (Math.abs(numberUnits) < 1024.0d) {
                return numberFormat.format(numberUnits) + " " + labels[i];
            }
            numberUnits /= 1024.0d;
        }
        return numberFormat.format(amount);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("bandwidth".equals(key)) {
            return new TemplateString(this.bandwidth);
        }
        if ("reload".equals(key)) {
            reload();
            return null;
        } else if ("size".equals(key)) {
            return new TemplateString(this.size);
        } else {
            if ("billing_type".equals(key)) {
                return new TemplateString(this.type);
            }
            if ("text_bandwidth".equals(key)) {
                return new TemplateString(smartLabel(this.bandwidth));
            }
            if ("text_inout_bandwidth".equals(key)) {
                return new TemplateString(smartLabel(this.bandwidthInout));
            }
            if ("text_out_bandwidth".equals(key)) {
                return new TemplateString(smartLabel(this.bandwidthOut));
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
        int susp = 150;
        try {
            susp = Integer.parseInt(Settings.get().getValue("bandwidth_susp"));
        } catch (Exception e) {
        }
        return (this.bandwidth * 100.0d) / this.size > ((double) susp);
    }

    public boolean warnLimit() {
        int warn = 80;
        try {
            warn = Integer.parseInt(Settings.get().getValue("bandwidth_warn"));
        } catch (Exception e) {
        }
        return (this.bandwidth * 100.0d) / this.size > ((double) warn);
    }

    public String info() {
        try {
            return Localizer.translateMessage("ds_bandwidth.overlimitinfo", new String[]{smartLabel(this.bandwidth), smartLabel(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.refund", new Object[]{smartLabel(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.ds_bandwidth.recurrent", new Object[]{getMonthPeriodInWords(), smartLabel(getFreeNumber()), smartLabel(this.size), smartLabel(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        BandwidthType bt = getPlanBandwidthType(token.getPlan(), token.getResourceType());
        return Localizer.translateMessage("bill.ds_bandwidth.recurrent", new Object[]{token.getMonthPeriodInWords(), smartLabel(token.getFreeUnits(), bt), smartLabel(getAmount(token), bt), smartLabel(getAmount(token) - token.getFreeUnits(), bt), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ds_bandwidth.refundall", new Object[]{smartLabel(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        Calendar cal = TimeUtils.getCalendar(begin);
        cal.add(5, -1);
        Date beginDate = cal.getTime();
        cal.setTime(end);
        cal.add(5, -1);
        Date endDate = cal.getTime();
        BandwidthRecord br = getExtBandwidth(new java.sql.Date(beginDate.getTime()), new java.sql.Date(endDate.getTime()));
        double usedBandwidth = br.current / 1024.0d;
        double defaultBandwidth = getFreeNumber();
        double paidBandwidth = Math.max(this.size, defaultBandwidth) * 1024.0d * 1.0d;
        Session.getLog().debug("Paid bandwidth:" + paidBandwidth + " coefficient:1.0 bandwidth:" + usedBandwidth);
        return Localizer.translateMessage("bill.ds_bandwidth.usage", new Object[]{smartLabel(usedBandwidth - paidBandwidth), smartLabel(this.size * 1.0d * 1024.0d), f42df.format(beginDate), f42df.format(endDate)});
    }

    @Override // psoft.hsphere.Resource
    public void closePeriodMonthlyAction(Date nextDate) throws Exception {
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
            ps = con.prepareStatement("UPDATE ds_bandwidth_log SET is_closed = 1 WHERE account_id = ? AND cdate BETWEEN ? AND ?");
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

    @Override // psoft.hsphere.Resource
    public void openPeriodMonthlyAction(Date nextDate) throws Exception {
        setNetinterfaceStartDate();
        reload();
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.recurrent_change", new Object[]{getMonthPeriodInWords(), smartLabel(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.refund_change", new Object[]{smartLabel(-delta), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public double inUse() {
        reload();
        return getBandwidth();
    }

    /* renamed from: psoft.hsphere.resource.ds.BandwidthResource$BandwidthRecord */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/ds/BandwidthResource$BandwidthRecord.class */
    public class BandwidthRecord {
        double current;
        double inout;
        double out;

        BandwidthRecord(double current, double inout, double out) {
            BandwidthResource.this = r5;
            this.current = current;
            this.inout = inout;
            this.out = out;
        }
    }

    private void setNetinterfaceStartDate() throws Exception {
        List<DSNetInterface> netInterfaces = new ArrayList();
        Account a = getAccount();
        if (a != null) {
            LinkedList dsResourceList = new LinkedList();
            a.getId().findAllChildren("ds", dsResourceList);
            Iterator iter = dsResourceList.iterator();
            while (iter.hasNext()) {
                DedicatedServer ds = ((DedicatedServerResource) ((ResourceId) iter.next()).get()).getDSObject();
                netInterfaces.addAll(ds.getNetInterfaces());
            }
            for (DSNetInterface ni : netInterfaces) {
                List l = new ArrayList();
                l.add(ni.getMrtgTargetName());
                l.add(String.valueOf(TimeUtils.getDateTimeInSeconds(getStartDate())));
                HostEntry he = HostManager.getHost(ni.getMrtgHostId());
                he.exec("mrtg-rrd-startdate-set", l);
            }
        }
    }
}
