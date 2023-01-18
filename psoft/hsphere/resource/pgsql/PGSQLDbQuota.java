package psoft.hsphere.resource.pgsql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.Quota;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/pgsql/PGSQLDbQuota.class */
public class PGSQLDbQuota extends Quota {
    protected String usedMb;
    protected long lastTimeLoaded;

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getUsageMultiplier() {
        Session.getLog().debug("In Quota own getUsageMultiplier");
        double actSize = getActualSize();
        double defaultQuota = getFreeNumber();
        int paidQuota = (int) Math.max(this.size, defaultQuota);
        Session.getLog().debug("PGSQLDbQuota usage multiplier ActualSize=" + actSize + " PaidQuota=" + paidQuota + " defaultQuota=" + defaultQuota + " size=" + this.size);
        if (actSize <= paidQuota) {
            return 0.0d;
        }
        return actSize - paidQuota;
    }

    public PGSQLDbQuota(ResourceId id) throws Exception {
        super(id);
        this.lastTimeLoaded = 0L;
    }

    public PGSQLDbQuota(int type, Collection values) throws Exception {
        super(type, values);
        this.lastTimeLoaded = 0L;
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

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "limitMb".equals(key) ? new TemplateString(this.size) : "usedMb".equals(key) ? new TemplateString(getActualSize()) : "info".equals(key) ? new TemplateString(info()) : super.get(key);
    }

    protected double getActualSize() {
        try {
            return USFormat.parseDouble(quotaReport(0));
        } catch (Exception e) {
            return 0.0d;
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        int warn;
        double value = getActualSize();
        try {
            warn = Integer.parseInt(Settings.get().getValue("quota_warn"));
        } catch (Exception e) {
            warn = 80;
        }
        return (value * 100.0d) / ((double) this.size) > ((double) warn);
    }

    @Override // psoft.hsphere.resource.Quota
    public String info() {
        try {
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"PGSQL Quota", "DB", recursiveGet("db_name").toString(), Double.toString(getActualSize()), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    protected String _getName() {
        try {
            return recursiveGet("db_name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.pgsqldb_quota.refund", new Object[]{new Double(this.size), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.pgsqldb_quota.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.pgsqldb_quota.refundall", new Object[]{new Double(this.size - getFreeNumber()), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.pgsqldb_quota.usage", new Object[]{new Double(this.size), new Double(getActualSize()), new Double(getActualSize() - this.size), _getName(), f42df.format(begin), f42df.format(end)});
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
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.pgsqldb_quota.recurrent_change", new Object[]{getMonthPeriodInWords(), new Double(delta), _getName(), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.pgsqldb_quota.refund_change", new Object[]{new Double(-delta), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota
    protected String[] getActualQuotaReport() {
        String[] rep = new String[2];
        ArrayList args = new ArrayList();
        rep[1] = new Double(this.size).toString();
        try {
            HostEntry host = recursiveGet("host");
            if (!HostEntry.getEmulationMode()) {
                args.add(((PGSQLResource) getParent().get().getParent().get()).getRootLogin());
                args.add(recursiveGet("db_name").toString());
                LinkedList result = (LinkedList) host.exec("pgsql-db-size", args);
                if (result.size() != 1) {
                    rep[0] = "0.0";
                }
                this.usedMb = (String) result.get(0);
                rep[0] = new Double(this.usedMb).toString();
                return rep;
            }
            rep[0] = new Double(this.size / 2).toString();
            return rep;
        } catch (Exception ex) {
            Session.getLog().error("Unable to get PGSQL database size", ex);
            rep[0] = "0.0";
            return rep;
        }
    }
}
