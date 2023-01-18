package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/RedirectResource.class */
public abstract class RedirectResource extends VHResource implements HostDependentResource {
    protected String stat;
    protected String protocol;
    protected String url_path;
    protected String url;

    public abstract void physicalCreate(long j) throws Exception;

    public abstract void physicalDelete(long j) throws Exception;

    public abstract String getHostName() throws Exception;

    public RedirectResource(int type, Collection values) throws Exception {
        super(type, values);
    }

    public RedirectResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO vhost_redirect (id, status, url_path, url, protocol) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.stat);
            ps.setString(3, this.url_path);
            ps.setString(4, this.url);
            ps.setString(5, this.protocol);
            ps.executeUpdate();
            con.close();
            physicalCreate(getHostId());
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("url_path") ? new TemplateString(this.url_path) : key.equals("url") ? new TemplateString(this.url) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM vhost_redirect WHERE id = ?");
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

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.redirect_url.setup", new Object[]{"www." + getHostName() + "/" + this.url_path});
    }

    protected String getDescriptionFrom() throws Exception {
        return "www." + getHostName() + "/" + this.url_path;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.redirect_url.recurrent", new Object[]{getPeriodInWords(), getDescriptionFrom(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.redirect_url.refund", new Object[]{getDescriptionFrom(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.redirect_url.refundall", new Object[]{getDescriptionFrom(), f42df.format(begin), f42df.format(end)});
    }
}
