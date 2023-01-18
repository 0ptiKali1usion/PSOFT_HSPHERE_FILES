package psoft.hsphere.resource.vps;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/resource/vps/VPSQuotaResource.class */
public class VPSQuotaResource extends VPSGenericResource {
    public VPSQuotaResource(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT initialized FROM vps_resources WHERE rid = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getTimestamp(1) != null) {
                this.physicallyInitialized = true;
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public VPSQuotaResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource, psoft.hsphere.resource.Quota, psoft.hsphere.Resource
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

    @Override // psoft.hsphere.resource.vps.VPSGenericResource, psoft.hsphere.resource.Quota
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

    @Override // psoft.hsphere.resource.vps.VPSGenericResource, psoft.hsphere.resource.Quota
    public String info() {
        try {
            return Localizer.translateMessage("vps.quota.info", new String[]{quotaReport(1), Integer.toString(this.size)});
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource, psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        VPSResource vps = recursiveGet("vps");
        Session.getLog().debug("Inside VPSQuotaResource::PhysicalCreate vps initialized = " + vps.isPsInitialized());
        if (vps.isPsInitialized()) {
            Session.getLog().debug("VPS is initialized, physical creation of quota");
            HostEntry he = HostManager.getHost(targetHostId);
            ArrayList args = new ArrayList();
            args.add(recursiveGet("vpsHostName").toString());
            args.add(Long.toString(this.size));
            he.exec("vps-dlimit-set", args);
            setPsInitialized(true);
            return;
        }
        setPsInitialized(false);
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource, psoft.hsphere.resource.Quota, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        ArrayList args = new ArrayList();
        args.add(recursiveGet("vpsHostName").toString());
        args.add("0");
        he.exec("vps-dlimit-set", args);
    }

    public static String getRecurrentChangeDescripton(InitToken token, Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.vps.quota.recurrent", new Object[]{token.getPeriodInWords(), new Double(token.getFreeUnits()), new Double(getAmount(token)), new Double(getAmount(token) - token.getFreeUnits()), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource
    public String getVPSLabel() {
        return "vps.quota";
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource
    public String getGetFilename() {
        return "vps-dlimit-get";
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource
    public String getConfigWord() {
        return "DLIMIT";
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource
    public String getErrorMessage() {
        return "Cannot get VPS Quota for ";
    }

    @Override // psoft.hsphere.resource.vps.VPSGenericResource
    public String getResourceName() {
        return "VPS Quota";
    }
}
