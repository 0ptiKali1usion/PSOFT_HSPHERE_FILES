package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/FTPVHostAnonymousResource.class */
public class FTPVHostAnonymousResource extends Resource implements IPDeletedResource, HostDependentResource {
    protected int ftp_upload;
    protected int ftp_status;
    protected String ftp_name;
    protected String dir;
    protected int hostnum;
    protected Hashtable hostnums;

    public FTPVHostAnonymousResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.hostnums = new Hashtable();
        Iterator i = initValues.iterator();
        this.dir = null;
        if (i.hasNext()) {
            this.ftp_upload = Integer.parseInt((String) i.next());
            if (i.hasNext()) {
                this.ftp_status = Integer.parseInt((String) i.next());
                if (i.hasNext()) {
                    this.ftp_name = (String) i.next();
                    return;
                }
                return;
            }
            initDirParam();
            return;
        }
        initDirParam();
    }

    public FTPVHostAnonymousResource(ResourceId rId) throws Exception {
        super(rId);
        this.hostnums = new Hashtable();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT upload, status, dir, name, hostnum FROM  iis_ftp_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.ftp_upload = rs.getInt(1);
                this.ftp_status = rs.getInt(2);
                this.dir = rs.getString(3);
                this.ftp_name = rs.getString(4);
                this.hostnum = rs.getInt(5);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_getIP() throws Exception {
        return getIp().get("ip");
    }

    public Resource getIp() throws Exception {
        Resource r = getParent().get();
        ResourceId ipId = r.FM_getChild("ip");
        Resource foundIp = ipId.get();
        if (!"1".equals(foundIp.get("shared").toString())) {
            return foundIp;
        }
        throw new HSUserException("ftpvhostanonymousresource.ip");
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        getLog().debug("Win FTPHostingResource.delete");
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM iis_ftp_vhost WHERE id = ?");
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

    public String getName() throws Exception {
        return "ftp." + recursiveGet("real_name");
    }

    public String getPath() throws Exception {
        return recursiveGet("dir") + "/" + this.dir;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("path")) {
                return new TemplateString(getPath());
            }
            try {
                if (key.equals("name")) {
                    return new TemplateString(getName());
                }
                if (key.equals("ftp_upload")) {
                    return new TemplateString(new Integer(this.ftp_upload));
                }
                if (key.equals("ftp_status")) {
                    return new TemplateString(new Integer(this.ftp_status));
                }
                if (key.equals("ftp_name")) {
                    return new TemplateString(this.ftp_name);
                }
                if (key.equals("ftp_hostnum")) {
                    return new TemplateString(this.hostnum);
                }
                if (key.equals("ip")) {
                    try {
                        return FM_getIP();
                    } catch (Exception e) {
                        return null;
                    }
                }
                return super.get(key);
            } catch (Exception e2) {
                return null;
            }
        } catch (Exception e3) {
            return null;
        }
    }

    public void initDirParam() throws Exception {
        try {
            this.ftp_upload = Integer.parseInt(getPlanValue("FTP_UPLOAD"));
        } catch (NullPointerException e) {
        }
        try {
            this.ftp_status = Integer.parseInt(getPlanValue("FTP_STATUS"));
        } catch (NullPointerException e2) {
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        recursiveGet("host");
        recursiveGet("login").toString();
        String name = getName();
        getIp().toString();
        if (this.dir == null) {
            this.dir = name;
        }
        if (this.ftp_name == null) {
            this.ftp_name = name;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO iis_ftp_vhost (id, host_id, upload, status, dir, name) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, Integer.parseInt(recursiveGet("host_id").toString()));
            ps.setInt(3, this.ftp_upload);
            ps.setInt(4, this.ftp_status);
            ps.setString(5, this.dir);
            ps.setString(6, this.ftp_name);
            ps.executeUpdate();
            ps.close();
            getLog().debug("Win FTPHostingResource.initDone");
            physicalCreate(getHostId());
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public void clearConfig() throws Exception {
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        String ip = getIp().toString();
        he.exec("ftp-delete.asp", (String[][]) new String[]{new String[]{"hostnum", Integer.toString(this.hostnum)}, new String[]{"ip", ip}});
    }

    public void recoverConfig() throws Exception {
        FM_updateSettings(this.ftp_upload, this.ftp_status, this.ftp_name);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v9, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_updateSettings(int ftp_upload, int ftp_status, String ftp_name) throws Exception {
        getLog().debug("Win FTPHostingResource, updating");
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        if (this.suspended) {
            he.exec("ftp-update.asp", (String[][]) new String[]{new String[]{"hostnum", Integer.toString(this.hostnum)}, new String[]{"ip", getIp().toString()}, new String[]{"name", ftp_name}, new String[]{"isupload", Integer.toString(ftp_upload)}, new String[]{"status", "0"}});
        } else {
            he.exec("ftp-update.asp", (String[][]) new String[]{new String[]{"hostnum", Integer.toString(this.hostnum)}, new String[]{"ip", getIp().toString()}, new String[]{"name", ftp_name}, new String[]{"isupload", Integer.toString(ftp_upload)}, new String[]{"status", Integer.toString(ftp_status)}});
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE iis_ftp_vhost SET upload = ?, status = ?, name = ? WHERE id = ?");
            ps.setInt(1, ftp_upload);
            ps.setInt(2, ftp_status);
            ps.setString(3, ftp_name);
            ps.setLong(4, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.ftp_upload = ftp_upload;
            this.ftp_status = ftp_status;
            this.ftp_name = ftp_name;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        try {
            this.suspended = true;
            FM_updateSettings(this.ftp_upload, this.ftp_status, this.ftp_name);
            this.suspended = false;
            super.suspend();
        } catch (Throwable th) {
            this.suspended = false;
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            try {
                this.suspended = false;
                FM_updateSettings(this.ftp_upload, this.ftp_status, this.ftp_name);
                this.suspended = true;
                super.resume();
            } catch (Throwable th) {
                this.suspended = true;
                throw th;
            }
        }
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        Session.getLog().debug("IIS FTP was deleted by IP");
        delete(false);
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
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        int newHostNum = createFTP(targetHostId);
        int oldHostNum = this.hostnum;
        try {
            try {
                ps = con.prepareStatement("UPDATE iis_ftp_vhost SET hostnum = ? WHERE id = ?");
                ps.setInt(1, newHostNum);
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                this.hostnum = newHostNum;
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error setting hostnum for iis_ftp_vhost into database ", ex);
                this.hostnum = oldHostNum;
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String ip = getIp().toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            hostnum = Integer.toString(getActualHostNum(targetHostId));
        } else {
            hostnum = Integer.toString(this.hostnum);
        }
        he.exec("ftp-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"ip", ip}});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        this.hostnums = new Hashtable();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Integer.parseInt(recursiveGet("host_id").toString());
    }

    private int createFTP() throws Exception {
        return createFTP(getHostId());
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    private int createFTP(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String login = recursiveGet("login").toString();
        String name = getName();
        String ip = getIp().toString();
        if (this.dir == null) {
            this.dir = name;
        }
        if (this.ftp_name == null) {
            this.ftp_name = name;
        }
        Collection c = he.exec("ftp-create.asp", (String[][]) new String[]{new String[]{"user-name", login}, new String[]{"name", name}, new String[]{"dir", this.dir}, new String[]{"ip", ip}, new String[]{"isupload", Integer.toString(this.ftp_upload)}, new String[]{"status", Integer.toString(this.ftp_status)}});
        int hostnum = Integer.parseInt((String) c.iterator().next());
        Session.getLog().debug("hostnum -----> " + hostnum);
        return hostnum;
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("ftp_vhost_anonymous.desc", new Object[]{getName()});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    public int getActualHostNum(long hostId) throws Exception {
        if (this.hostnums.keySet().contains(new Long(hostId))) {
            return ((Integer) this.hostnums.get(new Long(hostId))).intValue();
        }
        WinHostEntry he = (WinHostEntry) HostManager.getHost(hostId);
        Collection c = he.exec("web-gethostnum.asp", (String[][]) new String[]{new String[]{"hostname", recursiveGet("real_name").toString()}});
        int hn = Integer.parseInt((String) c.iterator().next());
        this.hostnums.put(new Long(hostId), new Integer(hn));
        return hn;
    }
}
