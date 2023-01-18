package psoft.hsphere.resource.vps;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.ChangeableResource;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.VPSDependentResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/vps/VPSGenericResource.class */
public abstract class VPSGenericResource extends Quota implements VPSDependentResource, ChangeableResource {
    public boolean physicallyInitialized;
    public String cronLogger;

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public abstract void physicalCreate(long j) throws Exception;

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public abstract void physicalDelete(long j) throws Exception;

    abstract String getVPSLabel();

    abstract String getGetFilename();

    abstract String getConfigWord();

    abstract String getErrorMessage();

    abstract String getResourceName();

    public VPSGenericResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.physicallyInitialized = false;
        this.cronLogger = "";
    }

    public VPSGenericResource(ResourceId id) throws Exception {
        super(id);
        this.physicallyInitialized = false;
        this.cronLogger = "";
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
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
            PreparedStatement ps1 = con.prepareStatement("DELETE FROM vps_resources WHERE rid = ?");
            ps1.setLong(1, getId().getId());
            ps.executeUpdate();
            ps1.executeUpdate();
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
        if ("limit".equals(key) || "limitMb".equals(key)) {
            if (HostEntry.getEmulationMode()) {
                return new TemplateString(this.size);
            }
            return new TemplateString(quotaReport(0));
        } else if ("used".equals(key) || "usedMb".equals(key)) {
            if (HostEntry.getEmulationMode()) {
                return new TemplateString(this.size / 2);
            }
            try {
                VPSResource vps = recursiveGet("vps");
                if (((Integer) vps.getServerStatusReport(1)).intValue() != 2 || (this instanceof VPSQuotaResource)) {
                    return new TemplateString(quotaReport(1));
                }
                return new TemplateString("0");
            } catch (Exception e) {
                getLog().warn(getErrorMessage() + getId(), e);
                return new TemplateString("Not Available");
            }
        } else if ("reload".equals(key)) {
            this.report.put("time", null);
            return new TemplateOKResult();
        } else if ("info".equals(key)) {
            return new TemplateString(info());
        } else {
            if ("start_date".equals(key)) {
                return new TemplateString(DateFormat.getDateInstance(2).format(getStartDate()));
            }
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        return false;
    }

    @Override // psoft.hsphere.resource.Quota
    public String info() {
        try {
            return Localizer.translateMessage(getVPSLabel() + ".info", new String[]{quotaReport(0), quotaReport(1)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getVPSLabel() + ".refund", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill." + getVPSLabel() + ".recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill." + getVPSLabel() + ".refundall", new Object[]{new Double(this.size), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public TemplateModel FM_physicalCreate() throws Exception {
        physicalCreate(getHostId());
        return this;
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("hostid").toString());
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public boolean isPsInitialized() {
        return this.physicallyInitialized;
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void setPsInitialized(boolean init) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE vps_resources SET initialized = ? WHERE rid = ?");
                Timestamp now = TimeUtils.getSQLTimestamp();
                if (init) {
                    ps.setTimestamp(1, now);
                } else {
                    ps.setNull(1, 93);
                }
                ps.setLong(2, getId().getId());
                if (ps.executeUpdate() == 0) {
                    ps = con.prepareStatement("INSERT INTO vps_resources(rid, vps_id, initialized) VALUES (?, ?, ?)");
                    ps.setLong(1, getId().getId());
                    ps.setLong(2, recursiveGet("vps").getId().getId());
                    if (init) {
                        ps.setTimestamp(3, now);
                    } else {
                        ps.setNull(3, 93);
                    }
                    ps.executeUpdate();
                }
                this.physicallyInitialized = init;
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Unable to set physicallyInitialized", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void parseConfig(List config) throws Exception {
        for (int i = 0; i < config.size(); i++) {
            String line = (String) config.get(i);
            if (line.startsWith(getConfigWord() + "=")) {
                try {
                    String sSize = line.substring(line.indexOf("=") + 1);
                    if (Float.parseFloat(sSize) != this.size) {
                        compareRealAndDBSize();
                    }
                    setPsInitialized(true);
                } catch (Exception ex) {
                    Session.getLog().error("Error parsing " + getConfigWord() + " " + line, ex);
                }
            }
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public String getConfig() throws Exception {
        return getConfigWord() + "=" + Integer.toString(this.size);
    }

    public void compareRealAndDBSize() throws Exception {
        float realUsage = Float.parseFloat(quotaReport(1));
        if (realUsage > this.size) {
            this.cronLogger = Localizer.translateMessage("vps.initwarn.usage_larger_limit", new Object[]{new Float(realUsage), getResourceName(), new Integer(this.size)});
        } else if (this.size == 0) {
            String translateMessage = Localizer.translateMessage("vps.initwarn.limit_set_zero", new String[]{getResourceName()});
            this.cronLogger = translateMessage;
            this.cronLogger = translateMessage;
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill." + getVPSLabel() + ".recurrent_change", new Object[]{getPeriodInWords(), new Double(delta), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end, double delta) {
        return Localizer.translateMessage("bill." + getVPSLabel() + ".refund_change", new Object[]{new Double(-delta), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota
    protected String[] getActualQuotaReport() {
        String[] rep = new String[2];
        rep[1] = new Double(this.size).toString();
        try {
            VPSResource vps = recursiveGet("vps");
            HostEntry he = recursiveGet("host");
            List args = new ArrayList();
            args.add(vps.getVPSHostName());
            Collection retv = he.exec(getGetFilename(), args);
            if (retv.size() != 2) {
                return quotaNA;
            }
            Iterator i = retv.iterator();
            rep[0] = (String) i.next();
            rep[1] = (String) i.next();
            return rep;
        } catch (Throwable e) {
            getLog().warn(getErrorMessage() + getId(), e);
            return quotaNA;
        }
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

    @Override // psoft.hsphere.resource.VPSDependentResource
    public String getCronLogger() {
        return this.cronLogger;
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void dropCronLogger() {
        this.cronLogger = "";
    }

    public int getSize() {
        return this.size;
    }
}
