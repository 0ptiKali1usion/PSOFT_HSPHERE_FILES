package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.RsyncTransport;
import psoft.hsphere.resource.admin.ContentMoveItem;
import psoft.util.UniqueNameGenerator;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WebUser.class */
public abstract class WebUser extends Resource implements HostDependentResource {
    protected String login;
    protected String password;
    protected String homeLocation;
    protected String homeDir;
    protected String group;
    protected long hostId;
    protected final int NUMBER_TRY = 1000;
    protected int uid;
    protected static final Boolean lock = new Boolean(true);

    public abstract void physicalCreate(long j) throws Exception;

    protected abstract boolean doesLoginExist(String str, long j) throws Exception;

    protected abstract boolean isCustomGroup() throws Exception;

    protected abstract void restoreDir() throws Exception;

    public abstract void physicalDelete(long j) throws Exception;

    @Override // psoft.hsphere.Resource
    public abstract Collection getTransportData() throws Exception;

    protected abstract void changePasswordPhysically(String str) throws Exception;

    abstract String getHomeDirectory() throws Exception;

    public WebUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.homeDir = null;
        this.NUMBER_TRY = HostEntry.VPS_IP;
    }

    public WebUser(ResourceId rId) throws Exception {
        super(rId);
        this.homeDir = null;
        this.NUMBER_TRY = HostEntry.VPS_IP;
        Session.getLog().debug("WebUser: Initializing class. uid=" + this.uid);
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT login, group_name, password, dir, user_id, hostid FROM unix_user WHERE id = ?");
                ps.setLong(1, getId().getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.login = rs.getString(1);
                    this.group = rs.getString(2);
                    this.password = rs.getString(3);
                    this.homeDir = rs.getString(4);
                    this.uid = rs.getInt(5);
                    this.hostId = rs.getInt(6);
                    Session.getLog().debug("WebUser: Initialized class from DB.");
                } else {
                    notFound();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().debug("WebUser: Failed to initialize class from DB.: " + e);
                Session.closeStatement(null);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        boolean isGroupCustom = isCustomGroup();
        Session.getLog().debug("WebUser: Locking session");
        synchronized (lock) {
            this.login = createLogin();
            if (!isGroupCustom) {
                this.group = this.login;
            }
            Session.getLog().debug("WebUser: Starting Physical creation");
            physicalCreate(this.hostId);
            Session.getLog().debug("WebUser: Physical creation complete");
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            Session.getLog().debug("WebUser: Writing user info to the unix_user table");
            try {
                ps = con.prepareStatement("INSERT INTO unix_user (id, login, password, group_name, dir, hostid, user_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.login);
                ps.setString(3, this.password);
                ps.setString(4, this.group);
                ps.setString(5, getHomeDirectory());
                ps.setLong(6, this.hostId);
                ps.setLong(7, this.uid);
                ps.executeUpdate();
                Session.getLog().debug("WebUser: User info written to the unix_user table (uid=" + this.uid + ")");
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException e) {
                Session.getLog().warn("WebUser: Failed to add user " + this.login + " to the unix_user table");
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    protected String createLogin() throws Exception {
        String safeLogin;
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id FROM unix_user WHERE login = ? AND hostid = ?");
        ps.setLong(2, this.hostId);
        UniqueNameGenerator ung = new UniqueNameGenerator(this.login, Session.getUserLoginLength(), HostEntry.VPS_IP);
        while (true) {
            safeLogin = ung.next();
            if (safeLogin == null) {
                break;
            } else if (doesLoginExist(safeLogin, this.hostId)) {
                Session.getLog().debug("Login exists physically: " + safeLogin);
            } else {
                try {
                    ps.setString(1, safeLogin);
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        break;
                    }
                    throw new SQLException("Username exists in the DB");
                    break;
                } catch (SQLException e) {
                    Session.getLog().warn("Tried login: " + safeLogin);
                }
            }
        }
        Session.closeStatement(ps);
        con.close();
        Session.getLog().debug("WebUser: Login created: " + safeLogin);
        return safeLogin;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        if (this.initialized) {
            Session.getLog().debug("WebUser: Physical delete: " + this.login + " on host " + this.hostId + "; uid=" + this.uid);
            physicalDelete(this.hostId);
        }
        restoreDir();
        Session.getLog().debug("WebUser: Deleting " + this.login + " from the unix_user table");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("DELETE FROM unix_user WHERE id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().debug("WebUser: Failed to delete " + this.login + " from the unix_user table");
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.unixuser.refund", new Object[]{this.login, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.unixuser.setup", new Object[]{this.login});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.unixuser.recurrent", new Object[]{getPeriodInWords(), this.login, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.unixuser.refundall", new Object[]{this.login, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        if (doesLoginExist(this.login, newHostId)) {
            HostEntry h = HostManager.getHost(newHostId);
            throw new Exception(Localizer.translateMessage("content.move.unix_user_exists", new String[]{this.login, h.getName()}));
        }
        return true;
    }

    @Override // psoft.hsphere.Resource
    public ContentMoveItem initContentMove(long srcHostId, long targetHostId) throws Exception {
        Session.getLog().debug("WebUser: Initializing content move");
        return ContentMoveItem.createContentMoveItem(this.login, srcHostId, targetHostId);
    }

    @Override // psoft.hsphere.Resource
    public Class getResourceTransportClass() {
        Session.getLog().debug("WebUser: getting ResourceTransportClass");
        return RsyncTransport.class;
    }

    @Override // psoft.hsphere.Resource
    public boolean hasCMI() {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return this.hostId;
    }

    public TemplateModel FM_changePassword(String password) throws Exception {
        changePasswordPhysically(password);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE unix_user SET password = ? WHERE id = ?");
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
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("uid")) {
            return new TemplateString(this.uid);
        }
        if (key.equals("login")) {
            return new TemplateString(this.login);
        }
        if (key.equals("group")) {
            return new TemplateString(this.group);
        }
        if (key.equals("password")) {
            return new TemplateString(this.password);
        }
        if (key.equals("dir")) {
            try {
                return new TemplateString(getHomeDirectory());
            } catch (Exception ex) {
                throw new TemplateModelException(ex);
            }
        } else if (key.equals("host_id")) {
            return new TemplateString(this.hostId);
        } else {
            try {
                return key.equals("host") ? HostManager.getHost(this.hostId) : super.get(key);
            } catch (Exception e) {
                getLog().warn("no host entry for the account", e);
                return null;
            }
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE unix_user SET hostid = ?, user_id = ? WHERE id = ?");
            ps.setLong(1, newHostId);
            ps.setInt(2, this.uid);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.getLog().debug("Setting new hostnum: old=" + this.hostId + " new hostnum = " + newHostId);
            this.hostId = newHostId;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getLogin() {
        return this.login;
    }
}
