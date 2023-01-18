package psoft.hsphere.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.util.TimeUtils;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/resource/Quota.class */
public abstract class Quota extends AbstractChangeableResource implements HostDependentResource {
    public static final long TIME_TO_LIVE = 300000;
    public static final int WARN_PROCENT = 80;
    protected static String[] quotaNA = {"not available", "not available", "not available", "not available", "not available", "not available"};
    protected Map report;

    public abstract boolean warnLimit();

    public abstract String info();

    public abstract boolean canBeMovedTo(long j) throws Exception;

    public abstract void physicalCreate(long j) throws Exception;

    public abstract void physicalDelete(long j) throws Exception;

    public abstract void setHostId(long j) throws Exception;

    public abstract long getHostId() throws Exception;

    protected abstract String[] getActualQuotaReport();

    public Quota(ResourceId id) throws Exception {
        super(id);
        this.report = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT size_mb FROM quotas WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.size = rs.getInt(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void changeResourcePhysical(double oldSize) throws Exception {
        setQuota();
        refresh();
    }

    public Quota(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.report = new HashMap();
        Iterator i = initValues.iterator();
        Double tmpSize = new Double(USFormat.parseDouble((String) i.next()));
        this.size = tmpSize.intValue();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
    }

    public boolean suspendLimit() {
        return false;
    }

    public void setQuota() throws Exception {
        physicalCreate(getHostId());
    }

    protected void refresh() {
        this.report = new HashMap();
    }

    public String quotaReport(int index) {
        Long timeToLive = (Long) this.report.get("time");
        Object o = this.report.get("report");
        if (timeToLive != null && o != null && TimeUtils.currentTimeMillis() - timeToLive.longValue() < 300000) {
            Session.getLog().debug("Inside quota report: will get cashed data");
            return ((String[]) o)[index];
        }
        try {
            Session.getLog().debug("Inside quota report: will get data physically");
            String[] temp = getActualQuotaReport();
            this.report.put("report", temp);
            this.report.put("time", new Long(TimeUtils.currentTimeMillis()));
            return temp[index];
        } catch (Throwable e) {
            getLog().warn("can not get quota for " + getId(), e);
            return "not available";
        }
    }

    public double spaceUsed() {
        try {
            return USFormat.parseDouble(quotaReport(0));
        } catch (Exception ex) {
            Session.getLog().error("Error parsing double " + quotaReport(0), ex);
            return 0.0d;
        }
    }

    @Override // psoft.hsphere.Resource
    public double inUse() {
        return spaceUsed();
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void saveSize(double newSize) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE quotas SET size_mb = ? WHERE id = ?");
            ps.setInt(1, (int) newSize);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public String getResellerRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.quota.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.quota.recurrent_change", new Object[]{getPeriodInWords(), new Double(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.quota.refund_change", new Object[]{new Double(-delta), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getResellerRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{getPeriodInWords(), getDescription(), f42df.format(begin), f42df.format(end)});
    }

    public static String getResellerRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        double newSize = token.getAmount();
        double oldSize = newSize;
        if (token.getRes() != null) {
            oldSize = token.getRes().getAmount();
        }
        if (newSize - oldSize != 0.0d) {
            return Localizer.translateMessage("bill.reseller.quota.recurrent", new Object[]{token.getPeriodInWords(), new Double(newSize - oldSize), f42df.format(begin), f42df.format(end)});
        }
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{token.getPeriodInWords(), token.getDescription(), f42df.format(begin), f42df.format(end)});
    }

    public static String getResellerRecurrentRefundDescription(InitToken token, Date begin, Date end) throws Exception {
        double newSize = token.getAmount();
        double oldSize = token.getRes().getAmount();
        return Localizer.translateMessage("bill.reseller.quota.refund", new Object[]{TypeRegistry.getDescription(token.getResourceType().getId()), new Double(oldSize - newSize), f42df.format(begin), f42df.format(end)});
    }

    public static String getDescription(InitToken token) throws Exception {
        return Resource.getDescription(token) + "MB " + getAmount(token);
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("quota.desc", new Object[]{new Integer(this.size).toString()});
    }
}
