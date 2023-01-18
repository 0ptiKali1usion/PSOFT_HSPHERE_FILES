package psoft.hsphere.resource.ftp;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
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
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ftp/FTPVHostDirectoryResource.class */
public class FTPVHostDirectoryResource extends Resource implements HostDependentResource {
    protected String dir;
    protected int read;
    protected int write;
    protected int list;
    protected int forAll;

    public FTPVHostDirectoryResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.dir = (String) i.next();
        this.read = Integer.parseInt((String) i.next());
        this.write = Integer.parseInt((String) i.next());
        this.list = Integer.parseInt((String) i.next());
        this.forAll = Integer.parseInt((String) i.next());
    }

    public FTPVHostDirectoryResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT dir, fread, fwrite, list, forall FROM ftp_vdir WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.dir = rs.getString(1);
                this.read = rs.getInt(2);
                this.write = rs.getInt(3);
                this.list = rs.getInt(4);
                this.forAll = rs.getInt(5);
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
        Collection<ResourceId> dirs = getParent().getChildHolder().getChildrenByName("ftp_vhost_directory");
        synchronized (dirs) {
            for (ResourceId rid : dirs) {
                if (!rid.equals(getId()) && this.dir.equals(rid.get("name").toString())) {
                    throw new HSUserException("ftpvhostdirectoryresource.exist");
                    break;
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ftp_vdir(id, dir, fread, fwrite, list, forall) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.dir);
            ps.setInt(3, this.read);
            ps.setInt(4, this.write);
            ps.setInt(5, this.list);
            ps.setInt(6, this.forAll);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            physicalCreate(Long.parseLong(recursiveGet("host_id").toString()));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void restartFTP() throws Exception {
        ((FTPVHostResource) getParent().get()).FM_updateServer();
    }

    public TemplateModel FM_update(int read, int write, int list, int forAll) throws Exception {
        int oldread = this.read;
        int oldwrite = this.write;
        int oldlist = this.list;
        int oldforAll = this.forAll;
        try {
            this.read = read;
            this.write = write;
            this.list = list;
            this.forAll = forAll;
            restartFTP();
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE ftp_vdir SET fread = ?, fwrite = ?, list = ?, forall = ? WHERE id = ?");
            ps.setInt(1, this.read);
            ps.setInt(2, this.write);
            ps.setInt(3, this.list);
            ps.setInt(4, this.forAll);
            ps.setLong(5, getId().getId());
            ps.executeUpdate();
            if (this.forAll == 1) {
                ps.close();
                ps = con.prepareStatement("DELETE FROM ftp_vdir_perm WHERE dir_id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Exception e) {
            this.read = oldread;
            this.write = oldwrite;
            this.list = oldlist;
            this.forAll = oldforAll;
            throw e;
        }
    }

    public TemplateModel FM_addUser(String user_rid) throws Exception {
        ResourceId urid = new ResourceId(user_rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ftp_vdir_perm(id, dir_id, dir_type, user_id, user_type) VALUES(?, ?, ?, ?, ?)");
            ps.setLong(1, getNewId());
            ps.setLong(2, getId().getId());
            ps.setLong(3, getId().getType());
            ps.setLong(4, urid.getId());
            ps.setLong(5, urid.getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            restartFTP();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delUser(String user_rid) throws Exception {
        ResourceId urid = new ResourceId(user_rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM ftp_vdir_perm WHERE dir_id = ? AND user_id = ?");
            ps.setLong(1, getId().getId());
            ps.setLong(2, urid.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            restartFTP();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_userList() throws Exception {
        TemplateList ul = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT user_id, user_type FROM ftp_vdir_perm WHERE dir_id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ResourceId rid = new ResourceId(rs.getLong(1), rs.getInt(2));
                try {
                    Resource r = rid.get();
                    if (r != null) {
                        ul.add((TemplateModel) rid);
                    }
                } catch (Exception e) {
                    Session.getLog().error("Get resource error, rid=" + rid, e);
                }
            }
            Session.closeStatement(ps);
            con.close();
            return ul;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            restartFTP();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM ftp_vdir_perm WHERE dir_id = ?");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM ftp_vdir WHERE id = ?");
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
        return "name".equals(key) ? new TemplateString(this.dir) : "r".equals(key) ? new TemplateString(this.read) : "w".equals(key) ? new TemplateString(this.write) : "l".equals(key) ? new TemplateString(this.list) : "forAll".equals(key) ? new TemplateString(this.forAll) : super.get(key);
    }

    protected String _getName() {
        try {
            return ((FTPVHostResource) getParent().get()).getIp().toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_directory.refund", new Object[]{this.dir, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_directory.setup", new Object[]{this.dir, _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_directory.recurrent", new Object[]{getPeriodInWords(), this.dir, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ftp_vhost_directory.refundall", new Object[]{this.dir, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        List args = new ArrayList();
        args.add(recursiveGet("login").toString());
        args.add(this.dir);
        he.exec("ftp-vhost-dir-add", args);
        ((HostDependentResource) getParent().get()).physicalCreate(targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ((HostDependentResource) getParent().get()).physicalCreate(targetHostId);
    }
}
