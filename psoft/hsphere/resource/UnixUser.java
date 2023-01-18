package psoft.hsphere.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.system.MailServices;
import psoft.hsphere.resource.system.WebServices;

/* loaded from: hsphere.zip:psoft/hsphere/resource/UnixUser.class */
public abstract class UnixUser extends WebUser {
    @Override // psoft.hsphere.resource.WebUser
    public String getLogin() {
        return this.login;
    }

    public String getGroup() {
        return this.group;
    }

    public String getDir() {
        return this.homeDir;
    }

    public UnixUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public UnixUser(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.WebUser
    protected boolean doesLoginExist(String safeLogin, long targetHostId) throws Exception {
        if (WebServices.isUnixUserExists(HostManager.getHost(targetHostId), safeLogin)) {
            return true;
        }
        return false;
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        List args = new ArrayList();
        args.add(this.login);
        he.exec("userdel", args);
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        try {
            HostEntry host = HostManager.getHost(this.hostId);
            List args = new ArrayList();
            args.add(this.login);
            host.exec("user-suspend", args);
            super.suspend();
        } catch (Exception e) {
            Session.getLog().error("Error suspending unix user " + this.login);
            throw e;
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            try {
                HostEntry host = HostManager.getHost(this.hostId);
                List args = new ArrayList();
                args.add(this.login);
                args.add(MailServices.shellQuote(this.password));
                host.exec("user-resume", args);
                super.resume();
            } catch (Exception e) {
                Session.getLog().error("Error resumimg unix user " + this.login);
                throw e;
            }
        }
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public Collection getTransportData() throws Exception {
        return Arrays.asList(get("dir").toString(), get("dir").toString());
    }

    @Override // psoft.hsphere.resource.WebUser
    protected void changePasswordPhysically(String password) throws Exception {
        getLog().debug("changing password for: " + this.login);
        HostEntry host = HostManager.getHost(this.hostId);
        List args = new ArrayList();
        args.add(this.login);
        args.add(MailServices.shellQuote(password));
        host.exec("user-changepwd", args);
        getLog().debug("password changed for: " + this.login);
    }

    public void setUID(int userId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                ps = con.prepareStatement("UPDATE unix_user SET user_id = ? WHERE id = ?");
                ps.setInt(1, userId);
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                this.uid = userId;
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Unable to set unix user id", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
