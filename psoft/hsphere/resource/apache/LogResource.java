package psoft.hsphere.resource.apache;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import gnu.regexp.RE;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.VHResource;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/LogResource.class */
public class LogResource extends VHResource implements HostDependentResource {
    protected final int STORE_DAYS = 7;
    protected final int USER_ROTATE_PERIOD = 1;
    protected String file;
    protected String logtype;

    public LogResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.STORE_DAYS = 7;
        this.USER_ROTATE_PERIOD = 1;
        Iterator i = initValues.iterator();
        this.file = (String) i.next();
        this.logtype = (String) i.next();
    }

    public LogResource(ResourceId rid) throws Exception {
        super(rid);
        this.STORE_DAYS = 7;
        this.USER_ROTATE_PERIOD = 1;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT file_name, logtype FROM apache_log WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.file = rs.getString(1);
                this.logtype = rs.getString(2);
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
        physicalCreate(recursiveGet("host").getId());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO apache_log (id, file_name, logtype) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.file);
            ps.setString(3, this.logtype);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getLocalFile() throws Exception {
        return getLocalDir() + this.file;
    }

    public String getLocalDir() throws Exception {
        String dir = getPlanValue("DIR");
        if (dir == null) {
            dir = "/logs";
        }
        return recursiveGet("dir").toString() + dir + "/" + recursiveGet("real_name") + "/";
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        physicalDelete(recursiveGet("host").getId());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_log WHERE id = ?");
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

    @Override // psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("local_file")) {
                return new TemplateString(getLocalFile());
            }
            if (key.equals("logtype")) {
                return new TemplateString(this.logtype);
            }
            if (key.equals("filename")) {
                return new TemplateString(this.file);
            }
            if (key.equals("log_dir")) {
                try {
                    return new TemplateString(getLocalDir());
                } catch (Exception e) {
                    getLog().warn("logresource", e);
                    return null;
                }
            }
            return super.get(key);
        } catch (Exception e2) {
            getLog().warn("logresource", e2);
            return null;
        }
    }

    public TemplateModel FM_delLogFiles() throws Exception {
        delLogFiles();
        return this;
    }

    public void delLogFiles() throws Exception {
        HostEntry he = recursiveGet("host");
        List<String> list = new ArrayList<>();
        list.add(recursiveGet("login").toString());
        list.add(recursiveGet("dir").toString());
        list.add(recursiveGet("real_name").toString());
        getLog().debug("LogResource.delLogFiles, exec " + list);
        he.exec("apache-vhost-logs-del", list);
        getLog().debug("LogResource.delLogFiles, execDone " + list);
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return this.logtype + " " + getLocalFile() + "\n";
    }

    public TemplateList FM_showLogFile(String logFile, String line, String count) throws Exception {
        try {
            RE exp = new RE("\\.\\.");
            if (exp.getMatch(logFile) != null) {
                return null;
            }
            List args = new ArrayList();
            args.add(String.valueOf(line));
            args.add(recursiveGet("dir").toString() + getPlanValue("DIR") + "/" + recursiveGet("real_name") + "/" + logFile);
            args.add(count);
            HostEntry he = recursiveGet("host");
            Collection<Object> col = he.exec("get-tail", args);
            TemplateList list = new TemplateList();
            for (Object obj : col) {
                list.add((TemplateModel) new TemplateString(obj));
            }
            return list;
        } catch (Exception e) {
            Session.getLog().warn("Error while querying log file", e);
            return null;
        }
    }

    public TemplateModel FM_showLogList() throws Exception {
        try {
            List args = new ArrayList();
            args.add(recursiveGet("dir").toString() + getPlanValue("DIR") + "/" + recursiveGet("real_name") + "/");
            args.add(this.file);
            HostEntry he = recursiveGet("host");
            Collection<Object> col = he.exec("file-list", args);
            TemplateList list = new TemplateList();
            for (Object obj : col) {
                list.add((TemplateModel) new TemplateString(obj));
            }
            return list;
        } catch (Exception e) {
            Session.getLog().warn("Error while querying log file list", e);
            return null;
        }
    }

    protected String getLabelByType() {
        try {
            return getId().getNamedType();
        } catch (Throwable th) {
            return "errorlog";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill." + getLabelByType() + ".refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        List l = new ArrayList();
        HostEntry he = HostManager.getHost(targetHostId);
        l.add("/" + recursiveGet("real_name"));
        l.add(recursiveGet("login").toString());
        l.add(recursiveGet("group").toString());
        l.add(getPlanValue("DIR"));
        l.add(getConfFile());
        Template config = Session.getTemplate("domain/logrotate.config");
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        SimpleHash root = new SimpleHash();
        root.put("log_name", getLocalFile());
        root.put(FMACLManager.USER, recursiveGet("login").toString());
        root.put("group", recursiveGet("group").toString());
        root.put("count", "7");
        root.put("period", "daily");
        root.put("compress", "true");
        config.process(root, writer);
        writer.flush();
        writer.close();
        he.exec("log-init", l, out.toString());
    }

    private String getConfFile() throws Exception {
        return recursiveGet("real_name") + "." + getLabelByType();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (this.initialized) {
            List l = new ArrayList();
            HostEntry he = HostManager.getHost(targetHostId);
            l.add(getConfFile());
            he.exec("log-stop", l);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return recursiveGet("host").getId();
    }
}
