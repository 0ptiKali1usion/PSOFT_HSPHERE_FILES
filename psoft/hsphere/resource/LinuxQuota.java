package psoft.hsphere.resource;

import com.psoft.hsphere.quotamanagers.QuotaManager;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.admin.Settings;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/LinuxQuota.class */
public class LinuxQuota extends Quota {
    protected int storageType;

    public LinuxQuota(ResourceId id) throws Exception {
        super(id);
        this.storageType = 1;
        readProperty();
    }

    public LinuxQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.storageType = 1;
        readProperty();
    }

    private String getUID() throws Exception {
        return recursiveGet("uid").toString();
    }

    private String getDir() throws Exception {
        return recursiveGet("dir").toString();
    }

    private String getUsername() throws Exception {
        return recursiveGet("login").toString();
    }

    @Override // psoft.hsphere.resource.Quota
    public String[] getActualQuotaReport() {
        try {
            return getQuotaManager().getQuotaUsage(getUsername(), getUID(), getDir());
        } catch (Throwable e) {
            getLog().warn("can not get quota for " + getId(), e);
            return quotaNA;
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public String quotaReport(int index) {
        Long timeToLive = (Long) this.report.get("time");
        Object o = this.report.get("report");
        if (timeToLive != null && o != null && System.currentTimeMillis() - timeToLive.longValue() < 300000) {
            return ((String[]) o)[index];
        }
        try {
            String[] rep = getQuotaManager().getQuotaUsage(getUsername(), getUID(), getDir());
            if (rep == null) {
                return "not available";
            }
            this.report.put("report", rep);
            this.report.put("time", new Long(System.currentTimeMillis()));
            return rep[index];
        } catch (Throwable e) {
            Session.getLog().warn("can not get quota for " + getId(), e);
            return "not available";
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
            physicalCreate(getHostId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
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
        if ("limitMb".equals(key)) {
            if (HostEntry.getEmulationMode()) {
                return new TemplateString(this.size);
            }
            return new TemplateString(quotaReport(1));
        } else if ("limitFiles".equals(key)) {
            return new TemplateString(quotaReport(3));
        } else {
            if ("usedMb".equals(key)) {
                if (HostEntry.getEmulationMode()) {
                    return new TemplateString(this.size / 2);
                }
                return new TemplateString(quotaReport(0));
            } else if ("usedFiles".equals(key)) {
                return new TemplateString(quotaReport(2));
            } else {
                if ("reload".equals(key)) {
                    this.report.put("time", null);
                    return new TemplateOKResult();
                } else if ("info".equals(key)) {
                    return new TemplateString(info());
                } else {
                    if ("start_date".equals(key)) {
                        return new TemplateString(DateFormat.getDateInstance(2).format(getPeriodBegin()));
                    }
                    return super.get(key);
                }
            }
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        int warn;
        try {
            double value = USFormat.parseDouble(quotaReport(0));
            try {
                warn = Integer.parseInt(Settings.get().getValue("quota_warn"));
            } catch (Exception e) {
                warn = 80;
            }
            return (value * 100.0d) / ((double) this.size) > ((double) warn);
        } catch (ParseException e2) {
            return false;
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public String info() {
        try {
            Resource unixuser = getParent().get();
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"Linux Quota", FMACLManager.USER, unixuser.get("login").toString(), quotaReport(0), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.quota.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.quota.recurrent", new Object[]{token.getPeriodInWords(), new Double(token.getFreeUnits()), new Double(getAmount(token)), new Double(getAmount(token) - token.getFreeUnits()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.quota.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.quota.refundall", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public TemplateModel FM_physicalCreate() throws Exception {
        physicalCreate(getHostId());
        return this;
    }

    @Override // psoft.hsphere.resource.Quota
    public void setQuota() throws Exception {
        getQuotaManager().setQuota(getUsername(), getUID(), getDir(), this.size);
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        getQuotaManager(targetHostId).createUser(getUsername(), getUID(), getDir(), this.size);
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        getQuotaManager(targetHostId).deleteUser(getUsername(), getUID(), getDir());
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    private QuotaManager getQuotaManager(long hostId) throws Exception {
        return QuotaManager.getQuotaManager(HostManager.getHost(hostId), this.storageType);
    }

    private QuotaManager getQuotaManager() throws Exception {
        return QuotaManager.getQuotaManager(recursiveGet("host"), this.storageType);
    }

    protected void readProperty() {
        String quotaManager = Session.getPropertyString("QUOTA_MANAGER");
        if (quotaManager.length() > 0) {
            this.storageType = QuotaManager.getManagerType(quotaManager);
        } else if ("TRUE".equals(Session.getPropertyString("SUPPORT_NET_APP"))) {
            this.storageType = 2;
        } else if ("TRUE".equals(Session.getPropertyString("SUPPORT_BLUE_ARC"))) {
            this.storageType = 3;
        } else {
            this.storageType = 1;
        }
    }
}
