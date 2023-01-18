package psoft.hsphere.resource.vps;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.TemplateXML;

/* loaded from: hsphere.zip:psoft/hsphere/resource/vps/VPSServerDetailsResource.class */
public class VPSServerDetailsResource extends Resource {
    public VPSServerDetailsResource(ResourceId id) throws Exception {
        super(id);
    }

    public VPSServerDetailsResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE vps_resources SET initialized = ? WHERE rid = ?");
                Timestamp now = TimeUtils.getSQLTimestamp();
                ps.setTimestamp(1, now);
                ps.setLong(2, getId().getId());
                if (ps.executeUpdate() == 0) {
                    ps = con.prepareStatement("INSERT INTO vps_resources(rid, vps_id, initialized) VALUES (?, ?, ?)");
                    ps.setLong(1, getId().getId());
                    ps.setLong(2, recursiveGet("vps").getId().getId());
                    ps.setTimestamp(3, now);
                    ps.executeUpdate();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Unable to set Initialized", ex);
                Session.closeStatement(ps);
                con.close();
            }
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
        PreparedStatement ps1 = null;
        try {
            ps1 = con.prepareStatement("DELETE FROM vps_resources WHERE rid = ?");
            ps1.setLong(1, getId().getId());
            ps1.executeUpdate();
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "details".equals(key) ? getServerDetailsXML() : "vpshostname".equals(key) ? new TemplateString(getVPSHostName()) : super.get(key);
    }

    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public void physicalCreate(long targetHostId) throws Exception {
    }

    public void physicalDelete(long targetHostId) throws Exception {
    }

    public void setHostId(long newHostId) throws Exception {
    }

    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.vps.serverdetails.refund", new Object[]{getVPSHostName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.vps.serverdetails.setup", new Object[]{getVPSHostName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.vps.serverdetails.recurrent", new Object[]{getPeriodInWords(), getVPSHostName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.vps.serverdetails.refundall", new Object[]{getVPSHostName(), f42df.format(begin), f42df.format(end)});
    }

    public String getVPSHostName() {
        try {
            VPSResource vps = recursiveGet("vps");
            return vps.getVPSHostName();
        } catch (Throwable e) {
            getLog().warn("Failed to get VPSHostName" + getId(), e);
            return "";
        }
    }

    public TemplateModel getServerDetailsXML() {
        try {
            HostEntry he = HostManager.getHost(getHostId());
            ArrayList args = new ArrayList();
            args.add(recursiveGet("vpsHostName").toString());
            Collection<String> col = he.exec("vps-info.pl", args);
            StringBuffer buf = new StringBuffer();
            for (String str : col) {
                buf.append(str).append("\n");
            }
            DocumentBuilder dbuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            Document doc = dbuilder.parse(new InputSource(new StringReader(buf.toString())));
            return new TemplateXML(doc).get("ServerInfo");
        } catch (Exception e) {
            Session.getLog().warn("Error while querying the VPS server details" + getVPSHostName(), e);
            return null;
        }
    }
}
