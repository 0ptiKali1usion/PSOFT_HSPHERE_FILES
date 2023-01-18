package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.system.WebServices;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/hsphere/resource/UnixUserResource.class */
public class UnixUserResource extends UnixUser {
    protected static Salt salt;

    static {
        salt = null;
        try {
            salt = new Salt();
        } catch (Exception e) {
        }
    }

    public UnixUserResource(ResourceId rId) throws Exception {
        super(rId);
    }

    public UnixUserResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        HostEntry he = (HostEntry) i.next();
        this.login = ((String) i.next()).toLowerCase();
        this.group = ((String) i.next()).toLowerCase();
        getLog().debug("UnixUserResource " + this.login + this.group);
        i.next();
        this.homeLocation = he.getUserHome();
        this.uid = -1;
        if (i.hasNext()) {
            this.password = (String) i.next();
        } else {
            this.password = new Salt().getNext(10);
        }
        this.hostId = he.getId();
    }

    @Override // psoft.hsphere.resource.WebUser
    protected boolean isCustomGroup() throws Exception {
        return !this.login.equals(this.group);
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long newHostId) throws Exception {
        Session.getLog().info("UnixUserResource: Creating user " + this.login);
        try {
            if (newHostId == this.hostId) {
                this.uid = WebServices.createUnixUser(HostManager.getHost(newHostId), this.login, this.group, getHomeDirectory(), this.password, this.uid);
            } else {
                int newUid = WebServices.createUnixUser(HostManager.getHost(newHostId), this.login, this.group, getHomeDirectory(), this.password);
                if (this.initialized) {
                    setUID(newUid);
                }
            }
        } catch (Exception e) {
            Session.getLog().warn("UnixUserResource: Failed to add " + this.login);
        }
        Session.getLog().info("UnixUserResource: User " + this.login + " created.");
    }

    @Override // psoft.hsphere.resource.WebUser
    protected void restoreDir() throws Exception {
        try {
            int ind = getHomeDirectory().indexOf(this.login) - 1;
            if (ind >= 0) {
                this.homeDir = getHomeDirectory().substring(0, ind);
            }
        } catch (Exception e) {
            Session.getLog().warn("UnixUser dir restore failed, dir = " + getHomeDirectory() + ",  login=" + this.login, e);
        }
    }

    @Override // psoft.hsphere.resource.WebUser
    protected synchronized String getHomeDirectory() {
        if (this.homeDir == null) {
            this.homeDir = this.homeLocation + "/" + this.login;
        }
        return this.homeDir;
    }

    @Override // psoft.hsphere.resource.UnixUser, psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        List args = new ArrayList();
        args.add(this.login);
        he.exec("userdel_r", args);
    }
}
