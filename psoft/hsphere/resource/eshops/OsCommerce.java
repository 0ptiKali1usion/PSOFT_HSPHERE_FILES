package psoft.hsphere.resource.eshops;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
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
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/eshops/OsCommerce.class */
public abstract class OsCommerce extends Resource implements HostDependentResource {

    /* renamed from: db */
    protected ResourceId f195db;
    protected ResourceId user;

    public OsCommerce(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.f195db = null;
        this.user = null;
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.f195db = new ResourceId((String) i.next());
        }
        if (i.hasNext()) {
            this.user = new ResourceId((String) i.next());
        }
    }

    public OsCommerce(ResourceId rId) throws Exception {
        super(rId);
        this.f195db = null;
        this.user = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT db_id, user_id FROM oscommerce WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.f195db = new ResourceId(rs.getString(1));
                this.user = new ResourceId(rs.getString(2));
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (this.f195db.get() != null) {
                ((MySQLDatabase) this.f195db.get()).unlock();
            }
            if (this.user.get() != null) {
                ((MySQLUser) this.user.get()).unlock();
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM oscommerce WHERE id = ?");
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
        if (((MySQLDatabase) this.f195db.get()).lockedBy() != null) {
            throw new HSUserException("eshop.db_alreadylocked", new Object[]{String.valueOf(this.f195db.get("db_name"))});
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO oscommerce(id, db_id, user_id) VALUES (? ,? ,?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, String.valueOf(this.f195db));
            ps.setString(3, String.valueOf(this.user));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (HostEntry.getEmulationMode()) {
                Session.getLog().warn("EMULATION MODE, posting MySQL bundle: ");
            } else {
                ((MySQLDatabase) this.f195db.get()).batchSQL(this.user, new LineNumberReader(new FileReader(getResourcePlanValue("SQL_BUNDLE"))));
            }
            physicalCreate(getHostId());
            ((MySQLDatabase) this.f195db.get()).lock(getId());
            ((MySQLUser) this.user.get()).lock(getId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getConfig() throws Exception {
        Template t = Session.getTemplate("eshops/" + getResourcePlanValue("CONFIG_NAME"));
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        SimpleHash root = new SimpleHash();
        root.put("domain", recursiveGet("name"));
        root.put("hosting", getParent());
        root.put("log_dir", new TemplateString(recursiveGet("dir").toString() + getPlanValue("DIR") + "/" + recursiveGet("real_name")));
        root.put("path", new TemplateString(recursiveGet("dir").toString() + "/" + recursiveGet("real_name") + "/"));
        try {
            ResourceId mysql_host = this.f195db.get().getParent();
            root.put("mysql_host", mysql_host.get("host").getName());
            root.put("mysql_user", this.user.get("name"));
            root.put("mysql_password", this.user.get("password"));
            root.put("mysql_db", this.f195db.get("db_name"));
            t.process(root, writer);
            writer.flush();
            writer.close();
            return out.toString();
        } catch (NullPointerException ex) {
            Session.getLog().debug(ex);
            throw new HSUserException("mysqldatabase.fail_getting");
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("db") ? this.f195db : key.equals(FMACLManager.USER) ? this.user : super.get(key);
    }

    public TemplateModel FM_restoreConfig() throws Exception {
        physicalCreate(getHostId());
        return new TemplateOKResult();
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
        return Localizer.translateMessage("bill.oscommerce.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.oscommerce.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.oscommerce.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.oscommerce.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
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
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{recursiveGet("name")};
    }
}
