package psoft.hsphere.resource;

import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.system.WebServices;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/hsphere/resource/UnixSubUserResource.class */
public class UnixSubUserResource extends UnixUser {
    private String homeSuffix;

    public UnixSubUserResource(ResourceId rId) throws Exception {
        super(rId);
    }

    public UnixSubUserResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.login = ((String) i.next()).toLowerCase();
        this.homeSuffix = (String) i.next();
        if (this.homeSuffix == null) {
            this.homeSuffix = "";
        } else {
            this.homeSuffix = "/" + this.homeSuffix;
        }
        getLog().debug("UnixSubUserResource: " + this.login + " home:" + this.homeSuffix);
        if (i.hasNext()) {
            this.password = (String) i.next();
        } else {
            this.password = new Salt().getNext(10);
        }
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public void initDone() throws Exception {
        this.hostId = Long.parseLong(recursiveGet("host_id").toString());
        this.uid = Integer.parseInt(recursiveGet("uid").toString());
        this.group = recursiveGet("group").toString();
        this.homeDir = recursiveGet("dir").toString() + this.homeSuffix;
        super.initDone();
    }

    @Override // psoft.hsphere.resource.WebUser
    protected boolean isCustomGroup() throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        if (targetHostId != this.hostId) {
            int newUid = Integer.parseInt(recursiveGet("uid").toString());
            setUID(newUid);
        }
        HostEntry he = HostManager.getHost(targetHostId);
        Session.getLog().info("Add sub user:" + this.login + ":" + this.group + ":" + getHomeDirectory() + ":" + this.uid);
        WebServices.createUnixSubUser(he, this.login, this.group, getHomeDirectory(), this.password, this.uid);
        Session.getLog().info("Sub User added");
    }

    @Override // psoft.hsphere.resource.WebUser
    protected void restoreDir() throws Exception {
        String oldDir = recursiveGet("dir").toString();
        this.homeSuffix = getHomeDirectory().substring(oldDir.length());
        Session.getLog().debug("homeSuffix " + this.homeSuffix + " " + oldDir);
    }

    @Override // psoft.hsphere.resource.WebUser
    protected synchronized String getHomeDirectory() throws Exception {
        if (this.homeDir == null) {
            this.homeDir = this.homeLocation + "/" + recursiveGet("dir").toString() + "/" + this.login;
        }
        return this.homeDir;
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public boolean hasCMI() {
        return false;
    }
}
