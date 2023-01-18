package psoft.hsphere.resource.mssql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import javax.xml.soap.SOAPElement;
import org.apache.axis.message.MessageElement;
import org.apache.axis.message.SOAPEnvelope;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mssql/MSSQLQuota.class */
public class MSSQLQuota extends Quota {
    public MSSQLQuota(ResourceId id) throws Exception {
        super(id);
    }

    public MSSQLQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
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
        if ("limitMb".equals(key)) {
            return new TemplateString(quotaReport(0));
        }
        if ("usedMb".equals(key)) {
            return new TemplateString(quotaReport(1));
        }
        if ("usedDB".equals(key)) {
            return new TemplateString(quotaReport(2));
        }
        if ("usedTL".equals(key)) {
            return new TemplateString(quotaReport(3));
        }
        if ("limitDB".equals(key)) {
            return new TemplateString(quotaReport(4));
        }
        if ("limitTL".equals(key)) {
            return new TemplateString(quotaReport(5));
        }
        if (!"reload".equals(key)) {
            return "info".equals(key) ? new TemplateString(info()) : super.get(key);
        }
        this.report.put("time", null);
        return this;
    }

    public TemplateModel FM_setDatabaseQuota() throws Exception {
        physicalCreate(getHostId());
        return this;
    }

    @Override // psoft.hsphere.resource.Quota
    public boolean warnLimit() {
        int warn;
        try {
            double value = USFormat.parseDouble(quotaReport(1));
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
            return Localizer.translateMessage("quota.overlimitinfo", new String[]{"MSSQL Quota", "DB", recursiveGet("db_name").toString(), quotaReport(1), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v11, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v9, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String dbName = recursiveGet("db_name").toString();
        String quota = recursiveGet("quota_perc").toString();
        Session.getLog().debug("Database name: " + dbName);
        Session.getLog().debug("Quota perc: " + quota);
        Integer tmpPerc = new Integer(Integer.parseInt(quota));
        int perc = tmpPerc.intValue();
        int quotadb = (this.size * perc) / 100;
        int quotatr = this.size - quotadb;
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("update", new String[]{new String[]{"resourcename", "mssqldatabase"}, new String[]{"database", dbName}, new String[]{"quotadb", String.valueOf(quotadb)}, new String[]{"quotalog", String.valueOf(quotatr)}});
        } else {
            he.exec("mssql-setdatabasequota.asp", (String[][]) new String[]{new String[]{"database", dbName}, new String[]{"quotadb", String.valueOf(quotadb)}, new String[]{"quotatr", String.valueOf(quotatr)}});
        }
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

    protected String _getName() {
        try {
            return recursiveGet("db_name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLQuota.refund", new Object[]{new Double(this.size), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) {
        return Localizer.translateMessage("bill.MSSQLQuota.recurrent", new Object[]{getPeriodInWords(), new Double(getFreeNumber()), new Double(this.size), new Double(this.size - getFreeNumber()), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.MSSQLQuota.refundall", new Object[]{new Double(this.size), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        return Localizer.translateMessage("bill.MSSQLQuota.recurrent_change", new Object[]{getPeriodInWords(), new Double(delta), _getName(), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.resource.Quota, psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("bill.MSSQLQuota.refund_change", new Object[]{new Double(-delta), _getName(), f42df.format(begin), f42df.format(end)});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v23, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.Quota
    protected String[] getActualQuotaReport() {
        String[] rep = new String[6];
        try {
            String dbName = recursiveGet("db_name").toString();
            WinHostEntry whe = (WinHostEntry) recursiveGet("host");
            if (WinService.isSOAPSupport()) {
                SOAPEnvelope envelope = whe.invokeMethod("get", new String[]{new String[]{"resourcename", "mssqldatabase"}, new String[]{"database", dbName}});
                SOAPElement info = WinService.getChildElement(envelope, "resource");
                double sum = 0.0d;
                Iterator i = info.getChildElements();
                while (i.hasNext()) {
                    MessageElement element = (MessageElement) i.next();
                    String name = element.getName();
                    String value = element.getValue();
                    rep[0] = String.valueOf(this.size);
                    if (name.equals("usagedb")) {
                        rep[2] = value;
                        sum += Double.parseDouble(value);
                    } else if (name.equals("usagelog")) {
                        rep[3] = value;
                        sum += Double.parseDouble(value);
                    } else if (name.equals("quotadb")) {
                        rep[4] = value;
                    } else if (name.equals("quotalog")) {
                        rep[5] = value;
                    }
                }
                rep[1] = String.valueOf(sum);
            } else {
                Collection res = whe.exec("mssql-getusagequota.asp", (String[][]) new String[]{new String[]{"database", dbName}});
                Object[] resArr = res.toArray();
                int quotadb = Integer.parseInt(resArr[0].toString().trim());
                int quotatr = Integer.parseInt(resArr[1].toString().trim());
                int quotadbtr = quotatr + quotadb;
                Collection res1 = whe.exec("mssql-getdatabasequota.asp", (String[][]) new String[]{new String[]{"database", dbName}});
                Object[] resArr1 = res1.toArray();
                int limitdb = Integer.parseInt(resArr1[0].toString().trim());
                int limittr = Integer.parseInt(resArr1[1].toString().trim());
                rep[0] = String.valueOf(this.size);
                rep[1] = String.valueOf(quotadbtr);
                rep[2] = String.valueOf(quotadb);
                rep[3] = String.valueOf(quotatr);
                rep[4] = String.valueOf(limitdb);
                rep[5] = String.valueOf(limittr);
            }
            return rep;
        } catch (Exception e) {
            getLog().warn("can not get quota for " + getId(), e);
            return quotaNA;
        }
    }
}
