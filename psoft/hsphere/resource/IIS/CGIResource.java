package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/CGIResource.class */
public class CGIResource extends MimeTypeResource implements HostDependentResource {
    protected String handler;

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        he.exec("cgialias-install.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}, new String[]{"handler", this.handler}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
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
        he.exec("cgialias-uninstall.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    public CGIResource(int type, Collection init) throws Exception {
        super(type, init);
        Iterator i = init.iterator();
        if (i.hasNext()) {
            this.ext = (String) i.next();
            if (i.hasNext()) {
                this.handler = (String) i.next();
                return;
            }
            return;
        }
        initParam();
    }

    public CGIResource(ResourceId rid) throws Exception {
        super(rid);
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

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO iis_handler (id, handler) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.handler);
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

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("handler") ? new TemplateString(this.handler) : super.get(key);
    }

    public void initParam() throws Exception {
        try {
            this.ext = getPlanValue("EXT");
        } catch (NullPointerException e) {
        }
        try {
            this.handler = getPlanValue("HANDLER");
        } catch (NullPointerException e2) {
        }
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
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

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cgi.desc", new Object[]{this.ext, recursiveGet("real_name").toString()});
    }
}
