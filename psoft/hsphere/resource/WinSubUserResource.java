package psoft.hsphere.resource;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import org.apache.axis.AxisFault;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinSubUserResource.class */
public class WinSubUserResource extends WebUser {
    private String baseUser;
    private String homeSuffix;

    public WinSubUserResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Session.getLog().debug("WinSubUser: Initialize from initValues.");
        this.uid = 0;
        Iterator i = initValues.iterator();
        this.login = ((String) i.next()).toLowerCase();
        this.homeSuffix = (String) i.next();
        Session.getLog().debug("WinSubUserResource: homeSuffix=" + this.homeSuffix);
        if (this.homeSuffix == null) {
            this.homeSuffix = "";
        }
        getLog().debug("WinSubUserResource: " + this.login + " home: " + this.homeSuffix);
        if (i.hasNext()) {
            this.password = (String) i.next();
        } else {
            this.password = new Salt().getNext(10);
        }
        Session.getLog().debug("WinSubUser: Initialized from initValues.");
    }

    public WinSubUserResource(ResourceId rId) throws Exception {
        super(rId);
        this.baseUser = recursiveGet("login").toString();
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public void initDone() throws Exception {
        this.hostId = Long.parseLong(recursiveGet("host_id").toString());
        this.uid = Integer.parseInt(recursiveGet("uid").toString());
        this.group = recursiveGet("group").toString();
        this.baseUser = recursiveGet("login").toString();
        if (this.homeSuffix == null || "".equals(this.homeSuffix)) {
            this.homeDir = recursiveGet("dir").toString();
        } else {
            this.homeDir = recursiveGet("dir").toString() + "\\" + this.homeSuffix;
        }
        super.initDone();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser
    protected boolean doesLoginExist(String safeLogin, long targetHostId) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(targetHostId);
        if (HostEntry.getEmulationMode()) {
            return false;
        }
        try {
            host.invokeMethod("get", new String[]{new String[]{"resourcename", "ftpsubaccount"}, new String[]{"username", this.baseUser}, new String[]{"subaccount", safeLogin}});
            getLog().warn("Subuser " + safeLogin + " of user " + this.baseUser + " already exists on the " + host + " server");
            return true;
        } catch (AxisFault fault) {
            if (WinService.getFaultDetailValue(fault, "subcode").equals("1404")) {
                Session.getLog().info("Subuser " + safeLogin + " of user " + this.baseUser + " doesn't exist and can be created.");
                return false;
            }
            throw AxisFault.makeFault(fault);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v20, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        restoreDir();
        Session.getLog().info("Physical Create. Adding subuser: Login: " + this.login + "; Base user: " + this.baseUser + "; Homedir: " + this.homeSuffix + "; Target host ID:  " + targetHostId);
        WinHostEntry host = (WinHostEntry) HostManager.getHost(targetHostId);
        try {
            host.invokeMethod("create", new String[]{new String[]{"resourcename", "ftpsubaccount"}, new String[]{"username", this.baseUser}, new String[]{"subaccount", this.login}, new String[]{"password", this.password}, new String[]{"homedir", this.homeSuffix}});
            Session.getLog().info("Physical Create.  Success creating subuser");
        } catch (Exception e) {
            Session.getLog().error("Physical Create. Failed to create subuser");
        } catch (AxisFault fault) {
            getLog().error("Physical Create. Failed to create subuser. AxisFault code: " + WinService.getFaultDetailValue(fault, "subcode"));
        }
    }

    @Override // psoft.hsphere.resource.WebUser
    protected boolean isCustomGroup() throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.WebUser
    protected void restoreDir() throws Exception {
        String baseUserDir = recursiveGet("dir").toString();
        String hd = getHomeDirectory();
        try {
            if (hd == baseUserDir) {
                this.homeSuffix = "";
            } else {
                this.homeSuffix = hd.substring(baseUserDir.length() + 1);
            }
            Session.getLog().debug("homeSuffix: " + this.homeSuffix + "; baseUserDir: " + baseUserDir);
        } catch (Exception e) {
            Session.getLog().debug("Failed to restore homeSuffix. Base user dir: " + baseUserDir + "Subuser dir: " + hd);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        try {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "ftpsubaccount"}, new String[]{"username", this.baseUser}, new String[]{"subaccount", this.login}});
            Session.getLog().info("Subuser " + this.login + " of user " + this.baseUser + " deleted on host " + targetHostId);
        } catch (Exception e) {
            Session.getLog().warn("Physical Delete. Failed to delete subuser: " + this.login + " of user " + this.baseUser);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.hostId);
        try {
            host.invokeMethod("suspend", new String[]{new String[]{"resourcename", "ftpsubaccount"}, new String[]{"username", this.baseUser}, new String[]{"subaccount", this.login}});
            super.suspend();
        } catch (Exception e) {
            getLog().info("Unable to suspend WinUser account", e);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            WinHostEntry host = (WinHostEntry) HostManager.getHost(this.hostId);
            try {
                host.invokeMethod("resume", new String[]{new String[]{"resourcename", "ftpsubaccount"}, new String[]{"username", this.baseUser}, new String[]{"subaccount", this.login}});
                super.resume();
            } catch (Exception e) {
                getLog().info("Unable to resume WinUser account", e);
            }
        }
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public Collection getTransportData() throws Exception {
        Session.getLog().debug("WinSubUser " + this.login + ": Getting transport data");
        return Arrays.asList(this.login, this.login);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v12, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser
    protected void changePasswordPhysically(String password) throws Exception {
        Session.getLog().info("changing password to :" + password);
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.hostId);
        try {
            host.invokeMethod("update", new String[]{new String[]{"resourcename", "ftpsubaccount"}, new String[]{"username", this.baseUser}, new String[]{"subaccount", this.login}, new String[]{"password", password}});
            Session.getLog().debug("WinSubUserResource: Pass changed for  subuser " + this.login + " of user " + this.baseUser);
        } catch (Exception e) {
            Session.getLog().error("ChangePasswordPhysically. Failed to change pass for subuser " + this.login + " of user " + this.baseUser);
            throw new HSUserException("Failed to change password.");
        }
    }

    @Override // psoft.hsphere.resource.WebUser
    protected synchronized String getHomeDirectory() throws Exception {
        if (this.homeDir == null) {
            this.homeDir = this.homeLocation + "\\" + recursiveGet("dir").toString() + "\\" + this.login;
        }
        return this.homeDir;
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public boolean hasCMI() {
        return false;
    }
}
