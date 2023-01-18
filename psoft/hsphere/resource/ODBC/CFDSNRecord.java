package psoft.hsphere.resource.ODBC;

import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ODBC/CFDSNRecord.class */
public class CFDSNRecord extends Resource implements HostDependentResource {
    public CFDSNRecord(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public CFDSNRecord(ResourceId id) throws Exception {
        super(id);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        ResourceId parent = getParent();
        if (parent != null) {
            String dsnName = String.valueOf(parent.get("DSN"));
            if (dsnName.indexOf(32) != -1) {
                throw new HSUserException("cf_dsn_record.name_restriction");
            }
        }
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        physicalDelete(getHostId());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        boolean isSOAP = WinService.isSOAPSupport();
        Session.getLog().debug("Inside CFDSNRecord::physicalCreate(long) SOAP support " + isSOAP);
        if (isSOAP) {
            WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
            ?? r2 = new String[4];
            String[] strArr = new String[2];
            strArr[0] = "resourcename";
            strArr[1] = "cfdsn";
            r2[0] = strArr;
            String[] strArr2 = new String[2];
            strArr2[0] = "dsnname";
            strArr2[1] = getParent().get("DSN").toString();
            r2[1] = strArr2;
            String[] strArr3 = new String[2];
            strArr3[0] = "uid";
            strArr3[1] = getParent().get("MSSQLUID") == null ? "" : getParent().get("MSSQLUID").toString();
            r2[2] = strArr3;
            String[] strArr4 = new String[2];
            strArr4[0] = "pwd";
            strArr4[1] = getParent().get("MSSQLPWD") == null ? "" : getParent().get("MSSQLPWD").toString();
            r2[3] = strArr4;
            he.invokeMethod("create", r2);
            return;
        }
        throw new Exception("SOAP is not activated. Resource cannot be created");
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "cfdsn"}, new String[]{"dsnname", getParent().get("DSN").toString()}});
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cf_dsn_record.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.cf_dsn_record.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cf_dsn_record.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.cf_dsn_record.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    protected String _getName() {
        try {
            return getParent().get("DSN").toString() + " (" + getParent().get("driver-name") + ")";
        } catch (Exception e) {
            return "";
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    public void update(Hashtable params) throws Exception {
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            if (params.keySet().contains("MSSQLUID") && params.keySet().contains("MSSQLPWD")) {
                WinHostEntry he = (WinHostEntry) HostManager.getHost(getHostId());
                he.invokeMethod("update", new String[]{new String[]{"resourcename", "cfdsn"}, new String[]{"dsnname", getParent().get("DSN").toString()}, new String[]{"uid", (String) params.get("MSSQLUID")}, new String[]{"pwd", (String) params.get("MSSQLPWD")}});
                return;
            }
            return;
        }
        throw new Exception("SOAP support is unavailable.");
    }
}
