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

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/CGIDirResource.class */
public class CGIDirResource extends VHResource {
    public static final String DEFAULT_DIR = "/cgi-bin";
    public static final String DEFAULT_ALIAS = "/cgi-bin";
    protected String dir;
    protected String alias;

    public String getDir() {
        return this.dir;
    }

    public CGIDirResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.alias = (String) i.next();
            if (i.hasNext()) {
                this.dir = (String) i.next();
                return;
            } else {
                this.dir = this.alias;
                return;
            }
        }
        this.dir = "/cgi-bin";
        this.alias = "/cgi-bin";
    }

    public CGIDirResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT dir, alias FROM apache_cgidir WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.dir = rs.getString(1);
                this.alias = rs.getString(2);
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
            ps = con.prepareStatement("INSERT INTO apache_cgidir (id, dir, alias) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.dir);
            ps.setString(3, this.alias);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (!this.dir.equals("/cgi-bin")) {
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("dir")) {
            return new TemplateString(this.dir);
        }
        if (key.equals("alias")) {
            return new TemplateString(this.alias);
        }
        try {
            return key.equals("path") ? new TemplateString(getPath()) : super.get(key);
        } catch (Exception e) {
            Session.getLog().error("Can't get cgi-bin path", e);
            return null;
        }
    }

    public String getPath() throws Exception {
        return recursiveGet("path") + this.dir;
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return "ScriptAlias " + this.alias + "\t\"" + getPath() + "\"\n";
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_cgidir WHERE id = ?");
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
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cgidir.refund", new Object[]{this.alias, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.cgidir.setup", new Object[]{this.alias, _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cgidir.recurrent", new Object[]{getPeriodInWords(), this.alias, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.cgidir.refundall", new Object[]{_getName(), this.alias, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cgidir.desc", new Object[]{recursiveGet("dir").toString(), recursiveGet("real_name").toString()});
    }
}
