package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.admin.Settings;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinQuota.class */
public class WinQuota extends Quota {
    private Map report;

    public WinQuota(ResourceId id) throws Exception {
        super(id);
        this.report = new HashMap();
    }

    public WinQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.report = new HashMap();
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
                if ("info".equals(key)) {
                    return new TemplateString(info());
                }
                if ("reload".equals(key)) {
                    this.report.put("time", null);
                    return new TemplateOKResult();
                } else if ("start_date".equals(key)) {
                    return new TemplateString(DateFormat.getDateInstance(2).format(getPeriodBegin()));
                } else {
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
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"Windows Quota", FMACLManager.USER, unixuser.get("login").toString(), quotaReport(0), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String login = recursiveGet("login").toString();
        String path = recursiveGet("dir").toString();
        he.exec("addquota.asp", (String[][]) new String[]{new String[]{"user-name", login}, new String[]{"path", path}, new String[]{"space", Integer.toString(this.size)}});
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

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.quota.refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.quota.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.quota.refundall", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.Quota
    public String[] getActualQuotaReport() {
        String[] rep = new String[4];
        try {
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            String login = recursiveGet("login").toString();
            recursiveGet("dir").toString();
            Collection retv = he.exec("getquota.asp", (String[][]) new String[]{new String[]{"user-name", login}});
            if (retv.size() != 2) {
                return quotaNA;
            }
            Iterator i = retv.iterator();
            rep[0] = (String) i.next();
            rep[1] = (String) i.next();
            rep[2] = "unlimited";
            rep[3] = "unlimited";
            return rep;
        } catch (Throwable e) {
            getLog().warn("can not get quota for " + getId(), e);
            return quotaNA;
        }
    }
}
