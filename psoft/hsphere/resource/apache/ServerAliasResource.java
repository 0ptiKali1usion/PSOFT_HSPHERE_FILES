package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ServerAliasResource.class */
public class ServerAliasResource extends VHResource {
    protected String alias;

    public ServerAliasResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.alias = (String) i.next();
    }

    public ServerAliasResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT alias FROM vhost_alias WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.alias = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO vhost_alias (id, alias) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.alias);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("alias") ? new TemplateString(this.alias) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM vhost_alias WHERE id = ?");
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

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return this.alias;
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.vhost_alias.setup", new Object[]{this.alias, _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.vhost_alias.recurrent", new Object[]{getPeriodInWords(), this.alias, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.vhost_alias.refund", new Object[]{this.alias, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.vhost_alias.refundall", new Object[]{_getName(), this.alias, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("vhost_alias.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
