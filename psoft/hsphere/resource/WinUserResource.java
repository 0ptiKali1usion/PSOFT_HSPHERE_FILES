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

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinUserResource.class */
public class WinUserResource extends WebUser {
    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() {
        Session.getLog().debug("!!!!!!!!!!unnecessary method!!!!!!!!!!");
        return new Object[]{this.login};
    }

    public WinUserResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.uid = 0;
        Iterator i = initValues.iterator();
        HostEntry he = (HostEntry) i.next();
        this.login = ((String) i.next()).toLowerCase();
        this.group = (String) i.next();
        getLog().debug("WinUserResource " + this.login + this.group);
        i.next();
        this.homeLocation = he.getUserHome();
        if (i.hasNext()) {
            this.password = (String) i.next();
        } else {
            this.password = new Salt().getNext(10);
        }
        this.hostId = he.getId();
    }

    public WinUserResource(ResourceId rId) throws Exception {
        super(rId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser
    protected boolean doesLoginExist(String safeLogin, long targetHostId) throws Exception {
        boolean loginExists;
        WinHostEntry host = (WinHostEntry) HostManager.getHost(targetHostId);
        if (HostEntry.getEmulationMode()) {
            loginExists = false;
        } else {
            boolean isSOAP = WinService.isSOAPSupport();
            if (isSOAP) {
                try {
                    host.invokeMethod("get", new String[]{new String[]{"resourcename", "account"}, new String[]{"username", safeLogin}});
                    loginExists = true;
                } catch (AxisFault fault) {
                    if (WinService.getFaultDetailValue(fault, "subcode").equals("1404")) {
                        loginExists = false;
                    } else {
                        throw AxisFault.makeFault(fault);
                    }
                }
            } else {
                String r = host.exec("user-check.asp", (String[][]) new String[]{new String[]{"user-name", safeLogin}}).iterator().next().toString();
                loginExists = "1".equals(r);
            }
            if (loginExists) {
                Session.getLog().debug("Win login exists: " + safeLogin);
            }
        }
        return loginExists;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v12, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        Session.getLog().info("Physical Create. Adding user:" + this.login + ":" + this.password + ":" + this.group);
        WinHostEntry host = (WinHostEntry) HostManager.getHost(targetHostId);
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            host.invokeMethod("create", new String[]{new String[]{"resourcename", "account"}, new String[]{"username", this.login}, new String[]{"password", this.password}, new String[]{"group", this.group}});
        } else {
            this.uid = Integer.parseInt(host.exec("adduser.asp", (String[][]) new String[]{new String[]{"user-name", this.login}, new String[]{"home-dir", getHomeDirectory()}, new String[]{"group", this.group}, new String[]{"password", this.password}}).iterator().next().toString());
        }
        Session.getLog().info("Physical Create Complete. User added");
    }

    @Override // psoft.hsphere.resource.WebUser
    protected boolean isCustomGroup() throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.WebUser
    protected void restoreDir() throws Exception {
        int ind = getHomeDirectory().indexOf(this.login) - 1;
        if (ind >= 0) {
            this.homeDir = getHomeDirectory().substring(0, ind);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v11, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v13, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        try {
            WinHostEntry host = (WinHostEntry) HostManager.getHost(targetHostId);
            boolean isSOAP = WinService.isSOAPSupport();
            if (isSOAP) {
                host.invokeMethod("delete", new String[]{new String[]{"resourcename", "account"}, new String[]{"username", this.login}, new String[]{"password", this.password}});
            } else {
                host.exec("userdel.asp", (String[][]) new String[]{new String[]{"user-name", this.login}, new String[]{"isremove", "1"}});
            }
        } catch (Exception e) {
            Session.getLog().warn("WinUserResource: Failed to delete " + this.login + " on host " + this.hostId + "; uid=" + this.uid);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.hostId);
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            try {
                host.invokeMethod("suspend", new String[]{new String[]{"resourcename", "account"}, new String[]{"username", this.login}});
                super.suspend();
                return;
            } catch (Exception e) {
                getLog().info("Unable to suspend WinUser account", e);
                return;
            }
        }
        try {
            host.exec("updateuser.asp", (String[][]) new String[]{new String[]{"user-name", this.login}, new String[]{"status", "0"}});
            super.suspend();
        } catch (Exception e2) {
            getLog().info("Unable to suspend WinUser account", e2);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            WinHostEntry host = (WinHostEntry) HostManager.getHost(this.hostId);
            boolean isSOAP = WinService.isSOAPSupport();
            if (isSOAP) {
                try {
                    host.invokeMethod("resume", new String[]{new String[]{"resourcename", "account"}, new String[]{"username", this.login}});
                    super.resume();
                    return;
                } catch (Exception e) {
                    getLog().info("Unable to resume WinUser account", e);
                    return;
                }
            }
            try {
                host.exec("updateuser.asp", (String[][]) new String[]{new String[]{"user-name", this.login}, new String[]{"status", "1"}});
                super.resume();
            } catch (Exception e2) {
                getLog().info("Unable to resume WinUser account", e2);
            }
        }
    }

    @Override // psoft.hsphere.resource.WebUser, psoft.hsphere.Resource
    public Collection getTransportData() throws Exception {
        Session.getLog().debug("WinUser: Getting transport data");
        return Arrays.asList(this.login, this.login);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v12, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.WebUser
    protected void changePasswordPhysically(String password) throws Exception {
        getLog().info("changing password to :" + password);
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.hostId);
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            try {
                host.invokeMethod("update", new String[]{new String[]{"resourcename", "account"}, new String[]{"username", this.login}, new String[]{"password", password}});
            } catch (Exception e) {
                getLog().info("Failed to change password", e);
                throw new HSUserException("Failed to change password.");
            }
        } else {
            host.exec("user-changepwd.asp", (String[][]) new String[]{new String[]{"user-name", this.login}, new String[]{"password", password}});
        }
        getLog().debug("Password changed for user " + this.login);
    }

    @Override // psoft.hsphere.resource.WebUser
    protected synchronized String getHomeDirectory() {
        if (this.homeDir == null) {
            this.homeDir = this.homeLocation + "\\" + this.login;
        }
        return this.homeDir;
    }
}
