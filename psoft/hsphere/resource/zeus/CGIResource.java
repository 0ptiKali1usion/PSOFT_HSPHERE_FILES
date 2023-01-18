package psoft.hsphere.resource.zeus;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/CGIResource.class */
public class CGIResource extends MimeTypeResource {
    protected String handler;

    public CGIResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.ext = (String) i.next();
        this.handler = (String) i.next();
        this.mimeType = "application/x-httpd-cgi";
    }

    public CGIResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT handler FROM iis_handler WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.handler = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        TemplateList handlers = recursiveGet("handlers");
        if (handlers == null) {
            throw new HSUserException("zeus.handler");
        }
        boolean found = false;
        while (true) {
            if (!handlers.hasNext()) {
                break;
            }
            TemplateHashModel map = handlers.next();
            if (this.handler.equals(map.get("name").toString())) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new HSUserException("zeus.handler");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO iis_handler (id, handler) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.handler);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("handler") ? new TemplateString(this.handler) : super.get(key);
    }

    @Override // psoft.hsphere.resource.zeus.MimeTypeResource, psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return "modules!map!handlers!" + normalizedExt() + "\t" + this.handler + "\nmodules!map!prestat!" + normalizedExt() + "\tno\n";
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM iis_handler WHERE id = ?");
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

    @Override // psoft.hsphere.resource.zeus.MimeTypeResource, psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cgi.desc", new Object[]{this.ext, recursiveGet("real_name").toString()});
    }
}
