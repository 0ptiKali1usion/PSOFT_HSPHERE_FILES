package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/MimeTypeResource.class */
public class MimeTypeResource extends Resource implements HostDependentResource {
    protected String ext;
    protected String mimeType;
    protected long vhost_id;

    public String getExt() {
        return this.ext;
    }

    public MimeTypeResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.mimeType = "Reserved";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.ext = (String) i.next();
            if (i.hasNext()) {
                this.mimeType = (String) i.next();
            }
        }
    }

    public MimeTypeResource(ResourceId rId) throws Exception {
        super(rId);
        this.mimeType = "Reserved";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT vhost_id, ext, mime_type FROM iis_mime WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.vhost_id = rs.getLong(1);
                this.ext = rs.getString(2);
                this.mimeType = rs.getString(3);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        he.exec("mime-create.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}, new String[]{"type", this.mimeType}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        he.exec("mime-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}});
    }

    public void setHostId(long newHostId) throws Exception {
    }

    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM iis_mime WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (this.initialized) {
                physicalDelete(getHostId());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("ext") ? new TemplateString(this.ext) : key.equals("mime") ? new TemplateString(this.mimeType) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Resource vhost = recursiveGetResource("hosting");
        long vhostId = vhost.getId().getId();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM iis_mime WHERE vhost_id = ? AND ext = ?");
                ps.setLong(1, vhostId);
                ps.setString(2, this.ext);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("Mime type exists" + this.ext);
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO iis_mime (id, vhost_id, ext, mime_type) VALUES (?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setLong(2, vhostId);
                ps2.setString(3, this.ext);
                ps2.setString(4, this.mimeType);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                throw new HSUserException("mimetyperesource.extension", new Object[]{this.ext});
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    protected String getLabelByType() {
        try {
            return getId().getNamedType();
        } catch (Throwable th) {
            return "mimetype";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".setup", new Object[]{this.ext, _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".refund", new Object[]{this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".recurrent", new Object[]{getPeriodInWords(), this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill." + getLabelByType() + ".refundall", new Object[]{_getName(), this.ext, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("mimetype.desc", new Object[]{recursiveGet("real_name").toString(), this.mimeType});
    }
}
