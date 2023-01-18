package psoft.hsphere.resource.appwizard;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
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
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.apache.VirtualHostingResource;
import psoft.hsphere.resource.app.MySQLWrapper;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.util.freemarker.Template2String;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/appwizard/App.class */
public abstract class App extends Resource implements HostDependentResource {

    /* renamed from: db */
    protected ResourceId f186db;
    protected ResourceId user;
    protected ResourceId vhost;
    protected String path;
    private static final File TARBALL_ROOT = new File("/hsphere/shared/scripts/3rd_party");

    protected abstract String getTarball();

    public App(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.f186db = null;
        this.user = null;
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.f186db = new ResourceId((String) i.next());
        }
        if (i.hasNext()) {
            this.user = new ResourceId((String) i.next());
        }
    }

    public App(ResourceId rId) throws Exception {
        super(rId);
        this.f186db = null;
        this.user = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT db_id, user_id FROM appwizard_apps WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.f186db = new ResourceId(rs.getString(1));
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
            if (this.f186db.get() != null) {
                ((MySQLDatabase) this.f186db.get()).unlock();
            }
            if (this.user.get() != null) {
                ((MySQLUser) this.user.get()).unlock();
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM appwizard WHERE id = ?");
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
        if (((MySQLDatabase) this.f186db.get()).lockedBy() != null) {
            throw new HSUserException("eshop.db_alreadylocked", new Object[]{String.valueOf(this.f186db.get("db_name"))});
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO appwizard_apps(id, db_id, user_id) VALUES (? ,? ,?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, String.valueOf(this.f186db));
            ps.setString(3, String.valueOf(this.user));
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (HostEntry.getEmulationMode()) {
                Session.getLog().warn("EMULATION MODE, posting MySQL bundle: ");
            } else {
                ((MySQLDatabase) this.f186db.get()).batchSQL(this.user, new LineNumberReader(new FileReader(getResourcePlanValue("SQL_BUNDLE"))));
            }
            physicalCreate(getHostId());
            ((MySQLDatabase) this.f186db.get()).lock(getId());
            ((MySQLUser) this.user.get()).lock(getId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("db") ? this.f186db : key.equals(FMACLManager.USER) ? this.user : super.get(key);
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

    public void uncompress(String bundle, String target, long hostId) throws Exception {
        HostEntry he = HostManager.getHost(hostId);
        he.exec("uncompress", new String[]{getTarballName(bundle), getPath(target), getVirtualHost().recursiveGet("login").toString(), getVirtualHost().recursiveGet("group").toString()});
    }

    public void uncompress(String target) throws Exception {
        uncompress(getTarball(), target, getHostId());
    }

    private String getTarballName(String bundle) {
        return new File(TARBALL_ROOT, bundle).getAbsolutePath();
    }

    private String getPath(String target) throws Exception {
        return noTrailingSlash(new File(getVirtualHost().getPath(), new File(getPath(), target).getAbsolutePath()).getAbsolutePath());
    }

    private String noTrailingSlash(String path) {
        if (path.charAt(path.length() - 1) == '/') {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private VirtualHostingResource getVirtualHost() throws Exception {
        return (VirtualHostingResource) this.vhost.get();
    }

    public String getPath() {
        return this.path;
    }

    public void processSQL(MySQLWrapper mysql, String templateName) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().info("EMULATION MODE, posting MySQL bundle:");
        } else {
            mysql.batchSQL(Template2String.process(Session.getTemplate(templateName), (TemplateModel) this));
        }
    }

    public void postConfig(String templateName, String destination) throws Exception {
        postConfig(templateName, destination, getHostId());
    }

    public void postConfig(String templateName, String destination, long hostId) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().info("EMULATION MODE, posting: " + templateName);
            Session.getLog().info("EMULATION MODE, executing: io2file " + getPath(destination));
            return;
        }
        String config = Template2String.process(Session.getTemplate(templateName), (TemplateModel) this);
        HostEntry he = HostManager.getHost(hostId);
        he.exec("io2file", new String[]{getPath(destination)}, config);
    }

    public void removePath(String target) throws Exception {
        remove(target, getHostId());
    }

    private void remove(String target, long hostId) throws Exception {
        HostEntry he = HostManager.getHost(hostId);
        String[] strArr = new String[3];
        strArr[0] = getTarballName(getTarball());
        strArr[1] = getPath(target);
        strArr[2] = getPath().equals("/") ? "1" : "";
        he.exec("removedir", strArr);
    }
}
