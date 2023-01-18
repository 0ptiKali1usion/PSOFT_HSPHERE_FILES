package psoft.hsphere.resource.appwizard;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/appwizard/AppWizardResource.class */
public class AppWizardResource extends Resource {

    /* renamed from: db */
    protected ResourceId f187db;
    protected ResourceId user;

    public AppWizardResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.f187db = null;
        this.user = null;
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.f187db = new ResourceId((String) i.next());
        }
        if (i.hasNext()) {
            this.user = new ResourceId((String) i.next());
        }
    }

    public AppWizardResource(ResourceId rId) throws Exception {
        super(rId);
        this.f187db = null;
        this.user = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT db_id, user_id FROM apwizard_apps WHERE r_id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.f187db = new ResourceId(rs.getString(1));
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
            if (this.f187db.get() != null) {
                ((MySQLDatabase) this.f187db.get()).unlock();
            }
            if (this.user.get() != null) {
                ((MySQLUser) this.user.get()).unlock();
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM appwizard_apps WHERE r_id = ?");
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
    }

    protected String getConfig() throws Exception {
        Template t = Session.getTemplate("eshops/" + getResourcePlanValue("CONFIG_NAME"));
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        SimpleHash root = new SimpleHash();
        root.put("domain", recursiveGet("name"));
        root.put("hosting", getParent());
        root.put("log_dir", new TemplateString(recursiveGet("dir").toString() + getPlanValue("DIR") + "/" + recursiveGet("real_name")));
        root.put("path", new TemplateString(recursiveGet("dir").toString() + "/" + recursiveGet("real_name") + "/"));
        try {
            ResourceId mysql_host = this.f187db.get().getParent();
            root.put("mysql_host", mysql_host.get("host").getName());
            root.put("mysql_user", this.user.get("name"));
            root.put("mysql_password", this.user.get("password"));
            root.put("mysql_db", this.f187db.get("db_name"));
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
        return key.equals("db") ? this.f187db : key.equals(FMACLManager.USER) ? this.user : super.get(key);
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

    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public void setHostId(long newHostId) throws Exception {
    }

    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{recursiveGet("name")};
    }

    public void physicalCreate(long targetHostId) throws Exception {
        List l = new ArrayList();
        l.add(getResourcePlanValue("BUNDLE_FILE_NAME"));
        l.add(recursiveGet("path").toString());
        l.add(String.valueOf(recursiveGet("login")));
        l.add(String.valueOf(recursiveGet("group")));
        HostManager.getHost(targetHostId);
    }

    public void physicalDelete(long targetHostId) throws Exception {
    }
}
