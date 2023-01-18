package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/SharedSSLResource.class */
public class SharedSSLResource extends Resource implements IPDeletedResource {
    protected long zoneId;
    protected String name;

    public SharedSSLResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.name = (String) i.next();
    }

    public SharedSSLResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT zone_id, name FROM shared_ssl WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.zoneId = rs.getLong(1);
                this.name = rs.getString(2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v10, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        String zoneName = this.name.substring(this.name.indexOf(".") + 1);
        AdmDNSZone zone = AdmDNSZone.getByName(zoneName);
        if (zone == null) {
            throw new HSUserException("sharedssl.nozone");
        }
        if (!zone.allowSSL()) {
            throw new HSUserException("sharedssl.forbidden");
        }
        this.zoneId = zone.getId();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        if (!zone.checkServer(he)) {
            throw new HSUserException("sharedssl.missserver");
        }
        VirtualHostingResource vh = (VirtualHostingResource) getParent().get();
        if (vh.getId().findChild("ssl") != null) {
            throw new HSUserException("sslresource.enabled");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO shared_ssl(id, zone_id, name) VALUES(?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, this.zoneId);
            ps.setString(3, this.name);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            String hostnum = recursiveGet("hostnum").toString();
            he.exec("ssl-sharedon.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"domain", AdmDNSZone.get(this.zoneId).getName()}, new String[]{"name", this.name}, new String[]{"forced", "0"}, new String[]{"need128", "0"}, new String[]{"hostname", recursiveGet("real_name").toString()}});
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v4, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            String hostnum = recursiveGet("hostnum").toString();
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            he.exec("ssl-sharedoff.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"name", this.name}, new String[]{"hostname", recursiveGet("real_name").toString()}});
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM shared_ssl WHERE id = ?");
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
        return key.equals("name") ? new TemplateString(this.name) : key.equals("zone_id") ? new TemplateString(this.zoneId) : key.equals("shared") ? new TemplateOKResult() : super.get(key);
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        Session.getLog().debug("SSL was deleted by IP");
        TemplateHash ip = recursiveGet("ip");
        if (ip != null && !"1".equals(ip.get("shared").toString())) {
            delete(false);
        }
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.sharedssl.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.sharedssl.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.sharedssl.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.sharedssl.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
