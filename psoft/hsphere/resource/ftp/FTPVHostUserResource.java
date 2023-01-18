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
import psoft.hsphere.resource.IPDependentResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ftp/FTPVHostUserResource.class */
public class FTPVHostUserResource extends Resource implements HostDependentResource, IPDependentResource {
    protected String login;
    protected String password;

    public FTPVHostUserResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.login = (String) i.next();
        this.password = (String) i.next();
    }

    public FTPVHostUserResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password FROM ftp_vuser WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.login = rs.getString(1);
                this.password = rs.getString(2);
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
        Collection<ResourceId> users = getParent().getChildHolder().getChildrenByName("ftp_vhost_user");
        synchronized (users) {
            for (ResourceId rid : users) {
                if (!rid.equals(getId()) && this.login.equals(rid.get("vlogin").toString())) {
                    throw new HSUserException("ftpvhostuserresource.exists");
                    break;
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ftp_vuser(id, login, password) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.login);
            ps.setString(3, this.password);
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

    public TemplateModel FM_update(String password) throws Exception {
        HostEntry he = recursiveGet("host");
        List args = new ArrayList();
        args.add(Long.toString(getParent().getId()));
        args.add(recursiveGet("login").toString());
        args.add(this.login);
        args.add(password);
        args.add(recursiveGet("dir").toString());
        he.exec("ftp-vhost-user-set", args);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ftp_vuser SET password = ? WHERE id = ?");
            ps.setString(1, password);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.password = password;
            return this;
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
            physicalDelete(Long.parseLong(recursiveGet("host_id").toString()));
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM ftp_vdir_perm WHERE user_id = ?");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM ftp_vuser WHERE id = ?");
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
        return "vlogin".equals(key) ? new TemplateString(this.login) : "vpassword".equals(key) ? new TemplateString(this.password) : super.get(key);
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
        return Localizer.translateMessage("bill.ftp_vhost_user.refund", new Object[]{this.login, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_user.setup", new Object[]{this.login, _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_user.recurrent", new Object[]{getPeriodInWords(), this.login, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ftp_vhost_user.refundall", new Object[]{this.login, _getName(), f42df.format(begin), f42df.format(end)});
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
        args.add(Long.toString(getParent().getId()));
        args.add(recursiveGet("login").toString());
        args.add(this.login);
        args.add(this.password);
        args.add(recursiveGet("dir").toString());
        he.exec("ftp-vhost-user-set", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        List args = new ArrayList();
        args.add(Long.toString(getParent().getId()));
        args.add(this.login);
        he.exec("ftp-vhost-user-del", args);
    }

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        physicalDelete(Long.parseLong(recursiveGet("host_id").toString()));
        physicalCreate(Long.parseLong(recursiveGet("host_id").toString()));
    }
}
