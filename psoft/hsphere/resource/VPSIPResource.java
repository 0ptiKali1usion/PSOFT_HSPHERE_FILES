package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.vps.VPSResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.StringShrink;

/* loaded from: hsphere.zip:psoft/hsphere/resource/VPSIPResource.class */
public class VPSIPResource extends IPResource implements VPSDependentResource {
    protected int typeIp;
    protected String strIp;
    private boolean physicallyInitialized;

    public VPSIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.typeIp = HostEntry.VPS_IP;
        this.strIp = "";
        this.physicallyInitialized = false;
        this.typeIp = HostEntry.VPS_IP;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        HostEntry he = recursiveGet("host");
        setIP(he.getExclusiveVPSIP(getId().getId()));
        Session.getLog().debug("VPSIPResource: GOT IP " + getIP());
        this.strIp = getIP().toString();
        VPSResource vps = recursiveGet("vps");
        Session.getLog().debug("VPSIPResource " + vps.isPsInitialized());
        if (vps.isPsInitialized()) {
            ArrayList args = new ArrayList();
            args.add(recursiveGet("vpsHostName").toString());
            args.add(getIP().toString() + ":" + getIP().getMask());
            he.exec("vps-addip.pl", args);
            return;
        }
        setPsInitialized(false);
    }

    public VPSIPResource(ResourceId rId) throws Exception {
        super(rId);
        this.typeIp = HostEntry.VPS_IP;
        this.strIp = "";
        this.physicallyInitialized = false;
        Connection con = Session.getDb();
        C0015IP vip = recursiveGet("host").getIPbyRid(rId, HostEntry.TAKEN_VPS_IP);
        setIP(vip);
        this.typeIp = HostEntry.TAKEN_VPS_IP;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT initialized FROM vps_resources WHERE rid = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.physicallyInitialized = rs.getTimestamp(1) != null;
            }
        } finally {
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        HostEntry he = recursiveGet("host");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        this.strIp = _getIP();
        he.releaseVPSIP(this.strIp);
        try {
            ps = con.prepareStatement("DELETE FROM vps_resources WHERE rid = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (this.initialized) {
                ArrayList args = new ArrayList();
                args.add(recursiveGet("vpsHostName").toString());
                args.add(getIP().toString() + ":" + getIP().getMask());
                he.exec("vps-rmip.pl", args);
            }
            super.delete();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.IPResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return 1.0d;
    }

    public static double getAmount(InitToken token) {
        return 1.0d;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            try {
                domainName = StringShrink.doShrink(domainName2, 64);
            } catch (Exception e) {
                domainName = "";
            }
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.refund", new Object[]{_getIP(), f42df.format(begin), f42df.format(end), domainName});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            domainName = StringShrink.doShrink(domainName2, 64);
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.setup", new Object[]{_getIP(), domainName});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            domainName = StringShrink.doShrink(domainName2, 64);
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.recurrent", new Object[]{getPeriodInWords(), _getIP(), f42df.format(begin), f42df.format(end), domainName});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            try {
                domainName = StringShrink.doShrink(domainName2, 64);
            } catch (Exception e) {
                domainName = "";
            }
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.refundall", new Object[]{_getIP(), f42df.format(begin), f42df.format(end), domainName});
    }

    protected String _getIP() {
        try {
            return getIP().toString();
        } catch (Exception e) {
            return "";
        }
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
            if (line.startsWith("IP") && line.indexOf(getIP().toString() + ":") > 0) {
                setPsInitialized(true);
                return;
            }
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public String getConfig() {
        return "";
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public String getCronLogger() {
        return "";
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void dropCronLogger() {
    }
}
