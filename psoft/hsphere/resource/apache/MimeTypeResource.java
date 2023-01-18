package psoft.hsphere.resource.apache;

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
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/MimeTypeResource.class */
public class MimeTypeResource extends VHResource {
    protected String ext;
    protected String mimeType;

    public String getExt() {
        return this.ext;
    }

    public MimeTypeResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
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
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ext, mime_type FROM apache_mime WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.ext = rs.getString(1);
                this.mimeType = rs.getString(2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected long getVHostId() throws Exception {
        return getParent().getId();
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_mime WHERE id = ?");
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
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                long vhostId = getVHostId();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM apache_mime WHERE vhost_id = ? AND ext = ?");
                ps.setLong(1, vhostId);
                ps.setString(2, this.ext);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("Mime type exists" + this.ext);
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO apache_mime(id, ext, mime_type, type, vhost_id) VALUES (?, ?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.ext);
                ps2.setString(3, this.mimeType);
                ps2.setInt(4, getId().getType());
                ps2.setLong(5, vhostId);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
            } catch (SQLException e) {
                throw new HSUserException("mimetyperesource.extension", new Object[]{this.ext});
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("ext") ? new TemplateString(this.ext) : key.equals("mime") ? new TemplateString(this.mimeType) : super.get(key);
    }

    public TemplateModel FM_update(String ext, String mime) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE apache_mime SET ext = ?, mime_type = ? WHERE id = ?");
            ps.setString(1, ext);
            ps.setString(2, mime);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.ext = ext;
            this.mimeType = this.mimeType;
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return "AddType " + this.mimeType + " " + this.ext + "\n";
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
        return Localizer.translateMessage("bill." + getLabelByType() + ".refundall", new Object[]{this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("mimetype.desc", new Object[]{recursiveGet("real_name").toString(), this.mimeType});
    }
}
