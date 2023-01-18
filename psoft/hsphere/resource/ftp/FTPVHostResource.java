package psoft.hsphere.resource.ftp;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
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
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ftp/FTPVHostResource.class */
public class FTPVHostResource extends Resource implements IPDeletedResource, HostDependentResource, IPDependentResource {
    protected String serverName;
    protected String serverAdmin;

    public FTPVHostResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.serverName = (String) i.next();
        this.serverAdmin = (String) i.next();
    }

    public FTPVHostResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT server_name, server_admin FROM ftp_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.serverName = rs.getString(1);
                this.serverAdmin = rs.getString(2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void clearConfig() throws Exception {
        physicalDelete(Long.parseLong(recursiveGet("host_id").toString()));
    }

    public void recoverConfig() throws Exception {
        FM_updateServer();
    }

    public TemplateModel FM_updateServer() throws Exception {
        physicalCreate(Long.parseLong(recursiveGet("host_id").toString()));
        return this;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ftp_vhost (id, server_name, server_admin) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.serverName);
            ps.setString(3, this.serverAdmin);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            FM_updateServer();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateString getIp() throws Exception {
        ResourceId ipId = getParent().get().FM_getChild("ip");
        if ("1".equals(ipId.get("shared").toString())) {
            throw new HSUserException("ftpvhostresource.ip");
        }
        return ipId.get("ip");
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(Long.parseLong(recursiveGet("host_id").toString()));
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM ftp_vhost WHERE id = ?");
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
    public TemplateModel get(String key) throws TemplateModelException {
        if ("serverName".equals(key)) {
            return new TemplateString(this.serverName);
        }
        if ("serverAdmin".equals(key)) {
            return new TemplateString(this.serverAdmin);
        }
        try {
            return "userName".equals(key) ? recursiveGet("login") : "userGroup".equals(key) ? recursiveGet("group") : "homeDir".equals(key) ? recursiveGet("dir") : "ip".equals(key) ? getIp() : super.get(key);
        } catch (Exception e) {
            throw new TemplateModelException(e.getMessage());
        }
    }

    public String getConfigEntry() throws Exception {
        if (this.suspended) {
            return "";
        }
        SimpleHash root = new SimpleHash();
        root.put("ftp", this);
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Session.getTemplate("/ftp/ftp.config").process(root, out);
        out.close();
        return sw.toString();
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        try {
            this.suspended = true;
            FM_updateServer();
            this.suspended = false;
            super.suspend();
        } catch (Throwable th) {
            this.suspended = false;
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        try {
            this.suspended = false;
            FM_updateServer();
            this.suspended = true;
            super.resume();
        } catch (Throwable th) {
            this.suspended = true;
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        Session.getLog().debug("FTP was deleted by IP");
        delete(false);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        getLog().info("FTPVHostResource.updateServer");
        HostEntry he = HostManager.getHost(targetHostId);
        String userName = recursiveGet("login").toString();
        String homeDir = recursiveGet("dir").toString();
        String passwd = recursiveGet("password").toString();
        List args = new ArrayList();
        args.add(Long.toString(getId().getId()));
        args.add(userName);
        args.add(userName);
        args.add(MailServices.shellQuote(passwd));
        args.add(homeDir);
        args.add(getIp().toString());
        if (null != FM_getChild("ftp_vhost_anonymous")) {
            args.add("anonymous");
        }
        he.exec("ftp-vhost-store", args, getConfigEntry());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        List args = new ArrayList();
        args.add(Long.toString(getId().getId()));
        he.exec("ftp-vhost-del", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    protected String _getName() {
        try {
            return getIp().toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ftp_vhost.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    public String getServerName() {
        return this.serverName;
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("ftp_vhost.desc", new Object[]{get("serverName").toString()});
    }

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        physicalDelete(Long.parseLong(recursiveGet("host_id").toString()));
        physicalCreate(Long.parseLong(recursiveGet("host_id").toString()));
    }
}
