package psoft.hsphere.resource.p003ds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.p003ds.BandwidthResource;
import psoft.util.TimeUtils;

/* renamed from: psoft.hsphere.resource.ds.ResellerBandwidth */
/* loaded from: hsphere.zip:psoft/hsphere/resource/ds/ResellerBandwidth.class */
public class ResellerBandwidth extends BandwidthResource {
    public ResellerBandwidth(int resType, Collection initValues) throws Exception {
        super(resType, initValues);
    }

    public ResellerBandwidth(ResourceId id) throws Exception {
        super(id);
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource
    public BandwidthType getPlanBandwidthType(Plan otherPlan) throws Exception {
        String planValue = otherPlan.getValue(getId().getType(), "_R_DS_BANDWIDTH_TYPE_");
        BandwidthType bt = planValue != null ? BandwidthType.getType(planValue) : BandwidthType.UNKNOWN;
        if (bt == BandwidthType.UNKNOWN) {
            throw new Exception("Unable to determine a bandwidth type, specified as value '" + planValue + "' in the plan #" + getAccount().getPlan().getId());
        }
        return bt;
    }

    public static BandwidthType getPlanBandwidthType(Plan newPlan, ResourceType resType) throws Exception {
        String planValue = newPlan.getValue(resType.getId(), "_R_DS_BANDWIDTH_TYPE_");
        BandwidthType bt = planValue != null ? BandwidthType.getType(planValue) : BandwidthType.UNKNOWN;
        if (bt == BandwidthType.UNKNOWN) {
            throw new Exception("Unable to determine a bandwidth type, specified as value '" + planValue + "' in the plan #" + newPlan.getId());
        }
        return bt;
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    public double getSetupMultiplier() {
        Session.getLog().debug("In Reseller Bandwidth own getSetupMultiplier");
        return 1.0d;
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource
    public boolean suspendLimit() {
        int susp = 150;
        try {
            susp = Integer.parseInt(Settings.get().getValue("bandwidth_susp"));
        } catch (Exception e) {
        }
        return (this.bandwidth * 100.0d) / this.size > ((double) susp);
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource
    public boolean warnLimit() {
        int warn = 80;
        try {
            warn = Integer.parseInt(Settings.get().getValue("bandwidth_warn"));
        } catch (Exception e) {
        }
        return (this.bandwidth * 100.0d) / this.size > ((double) warn);
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource
    public String info() {
        try {
            return Localizer.translateMessage("ds_bandwidth.overlimitinfo", new String[]{smartLabel(this.bandwidth), smartLabel(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.ds_bandwidth.recurrent", new Object[]{getMonthPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.recurrent", new Object[]{token.getMonthPeriodInWords(), new Double(token.getFreeUnits()), new Double(getAmount(token)), new Double(getAmount(token) - token.getFreeUnits()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ds_bandwidth.refundall", new Object[]{new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        Calendar cal = TimeUtils.getCalendar(begin);
        cal.add(5, -1);
        Date beginDate = cal.getTime();
        cal.setTime(end);
        cal.add(5, -1);
        Date endDate = cal.getTime();
        BandwidthResource.BandwidthRecord br = getExtBandwidth(new java.sql.Date(beginDate.getTime()), new java.sql.Date(endDate.getTime()));
        double usedBandwidth = br.current / 1024.0d;
        double defaultBandwidth = getFreeNumber();
        double paidBandwidth = Math.max(this.size, defaultBandwidth) * 1024.0d * 1.0d;
        Session.getLog().debug("Paid bandwidth:" + paidBandwidth + " coefficient:1.0 bandwidth:" + usedBandwidth);
        return Localizer.translateMessage("bill.ds_bandwidth.usage", new Object[]{smartLabel(usedBandwidth - paidBandwidth), smartLabel(this.size * 1.0d * 1024.0d), f42df.format(beginDate), f42df.format(endDate)});
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
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

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    public void openPeriodMonthlyAction(Date nextDate) throws Exception {
        reload();
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.recurrent_change", new Object[]{getMonthPeriodInWords(), new Double(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.p003ds.BandwidthResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.ds_bandwidth.refund_change", new Object[]{new Double(-delta), f42df.format(begin), f42df.format(end)});
    }
}
