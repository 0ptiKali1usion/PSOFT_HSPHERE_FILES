package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.MixedIPResource;
import psoft.hsphere.resource.UnixUser;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.ftp.FTPVHostAnonymousResource;
import psoft.hsphere.resource.ftp.FTPVHostDirectoryResource;
import psoft.hsphere.resource.ftp.FTPVHostResource;
import psoft.hsphere.resource.ftp.FTPVHostUserResource;

/* loaded from: hsphere.zip:psoft/hsphere/axis/FtpServices.class */
public class FtpServices {
    private static Category log = Category.getInstance(FtpServices.class.getName());

    public String getDescription() {
        return "Functions to work with FTP";
    }

    private FTPVHostResource getVhost(Account a, String domainName) throws Exception {
        Domain dom = Utils.getDomain(a, domainName);
        if (dom == null) {
            throw new Exception("No such ftp domain: " + domainName);
        }
        ResourceId rid = dom.getId().findChild("ftp_vhost");
        if (rid == null) {
            throw new Exception("No such vhost for ftp domain: " + domainName);
        }
        return (FTPVHostResource) rid.get();
    }

    private FTPVHostAnonymousResource getAnonimousVhost(Account a, String domainName) throws Exception {
        FTPVHostResource vhost = getVhost(a, domainName);
        ResourceId rid = vhost.getId().findChild("ftp_vhost_anonymous");
        if (rid == null) {
            throw new Exception("No such anonymous vhost for ftp domain: " + domainName);
        }
        return (FTPVHostAnonymousResource) rid.get();
    }

    private FTPVHostDirectoryResource getVhostDirectory(Account a, String domainName, String dirName) throws Exception {
        FTPVHostResource vhost = getVhost(a, domainName);
        for (ResourceId resourceId : vhost.getId().findChildren("ftp_vhost_directory")) {
            FTPVHostDirectoryResource dir = (FTPVHostDirectoryResource) resourceId.get();
            if (dir != null && dir.get("name").toString().equalsIgnoreCase(dirName)) {
                return dir;
            }
        }
        throw new Exception("No such directory: " + dirName);
    }

    private FTPVHostUserResource getVhostUser(Account a, String domainName, String userName) throws Exception {
        FTPVHostResource vhost = getVhost(a, domainName);
        for (ResourceId resourceId : vhost.getId().findChildren("ftp_vhost_user")) {
            FTPVHostUserResource user = (FTPVHostUserResource) resourceId.get();
            if (user != null && user.get("vlogin").toString().equalsIgnoreCase(userName)) {
                return user;
            }
        }
        throw new Exception("No such user: " + userName);
    }

    private void checkPlatform(AuthToken at) throws Exception {
        ResourceId id = Utils.getAccount(at).getId().findChild("unixuser");
        if (id.get() instanceof WinUserResource) {
            throw new Exception("Windows not support this action");
        }
    }

    public Object[] getVhostDirectories(AuthToken at, String domainName) throws Exception {
        FTPVHostResource vhost = getVhost(Utils.getAccount(at), domainName);
        ArrayList dirNames = new ArrayList();
        for (ResourceId resourceId : vhost.getId().findChildren("ftp_vhost_directory")) {
            FTPVHostDirectoryResource dir = (FTPVHostDirectoryResource) resourceId.get();
            if (dir != null) {
                dirNames.add(dir.get("name").toString());
            }
        }
        return dirNames.toArray();
    }

    public Object[] getVhostUsers(AuthToken at, String domainName) throws Exception {
        FTPVHostResource vhost = getVhost(Utils.getAccount(at), domainName);
        ArrayList userNames = new ArrayList();
        for (ResourceId resourceId : vhost.getId().findChildren("ftp_vhost_user")) {
            FTPVHostUserResource user = (FTPVHostUserResource) resourceId.get();
            if (user != null) {
                userNames.add(user.get("vlogin").toString());
            }
        }
        return userNames.toArray();
    }

    public boolean isEnableAnonymous(AuthToken at, String domainName) throws Exception {
        checkPlatform(at);
        FTPVHostResource vhost = getVhost(Utils.getAccount(at), domainName);
        ResourceId rid = vhost.FM_findChild("ftp_vhost_anonymous");
        if (rid == null) {
            return false;
        }
        return true;
    }

    public void addVdir(AuthToken at, String domainName, String vdirName, int read, int write, int list, int forAll) throws Exception {
        checkPlatform(at);
        FTPVHostResource vhost = getVhost(Utils.getAccount(at), domainName);
        List values = new ArrayList();
        values.add(vdirName);
        values.add(Integer.toString(read));
        values.add(Integer.toString(write));
        values.add(Integer.toString(list));
        values.add(Integer.toString(forAll));
        vhost.addChild("ftp_vhost_directory", "", values);
    }

    public void addVdirPerm(AuthToken at, String domainName, String vdirName, String userName) throws Exception {
        checkPlatform(at);
        FTPVHostDirectoryResource dir = getVhostDirectory(Utils.getAccount(at), domainName, vdirName);
        FTPVHostUserResource user = getVhostUser(Utils.getAccount(at), domainName, userName);
        dir.FM_addUser(user.getId().getAsString());
    }

    public void addVuser(AuthToken at, String domainName, String userName, String userPassword) throws Exception {
        checkPlatform(at);
        FTPVHostResource vhost = getVhost(Utils.getAccount(at), domainName);
        List values = new ArrayList();
        values.add(userName);
        values.add(userPassword);
        vhost.addChild("ftp_vhost_user", "", values);
    }

    public void enableAnonymous(AuthToken at, String domainName) throws Exception {
        if (!isEnableAnonymous(at, domainName)) {
            FTPVHostResource vhost = getVhost(Utils.getAccount(at), domainName);
            vhost.addChild("ftp_vhost_anonymous", "", new ArrayList());
            return;
        }
        throw new Exception("Anonymous ftp enabled for domain - " + domainName);
    }

    public void disableAnonymous(AuthToken at, String domainName) throws Exception {
        if (isEnableAnonymous(at, domainName)) {
            FTPVHostAnonymousResource vhostAnonim = getAnonimousVhost(Utils.getAccount(at), domainName);
            vhostAnonim.FM_cdelete(0);
            return;
        }
        throw new Exception("Anonymous ftp disabled for domain - " + domainName);
    }

    public void enableIncoming(AuthToken at, String domainName) throws Exception {
        checkPlatform(at);
        FTPVHostAnonymousResource vhostAnonim = getAnonimousVhost(Utils.getAccount(at), domainName);
        vhostAnonim.FM_update(1);
    }

    public void disableIncoming(AuthToken at, String domainName) throws Exception {
        checkPlatform(at);
        FTPVHostAnonymousResource vhostAnonim = getAnonimousVhost(Utils.getAccount(at), domainName);
        vhostAnonim.FM_update(0);
    }

    public void ftpUnixVhostAdd(AuthToken at, String domainName, String serverName, String adminEmail) throws Exception {
        checkPlatform(at);
        Domain dom = Utils.getDomain(Utils.getAccount(at), domainName);
        if (dom == null) {
            throw new Exception("No such ftp domain: " + domainName);
        }
        if (((MixedIPResource) dom.getId().findChild("ip").get()).isDedicated()) {
            List values = new ArrayList();
            values.add(serverName);
            values.add(adminEmail);
            dom.addChild("ftp_vhost", "", values);
            return;
        }
        throw new Exception("IP is not dedicated");
    }

    public void ftpWinVhostAdd(AuthToken at, String domainName, int status, int upload, String ftpName) throws Exception {
        Domain dom = Utils.getDomain(Utils.getAccount(at), domainName);
        if (dom == null) {
            throw new Exception("No such win domain: " + domainName);
        }
        if (((MixedIPResource) dom.getId().findChild("ip").get()).isDedicated()) {
            List values = new ArrayList();
            values.add(Integer.toString(upload));
            values.add(Integer.toString(status));
            values.add(ftpName);
            dom.addChild("ftp_vhost_anonymous", "", values);
            return;
        }
        throw new Exception("IP is not dedicated");
    }

    public void ftpWinVhostUpdate(AuthToken at, String domainName, int status, int upload, String ftpName) throws Exception {
        Domain dom = Utils.getDomain(Utils.getAccount(at), domainName);
        if (dom == null) {
            throw new Exception("No such win domain: " + domainName);
        }
        if (((MixedIPResource) dom.getId().findChild("ip").get()).isDedicated()) {
            psoft.hsphere.resource.IIS.FTPVHostAnonymousResource iisFtp = (psoft.hsphere.resource.IIS.FTPVHostAnonymousResource) dom.getId().findChild("ftp_vhost_anonymous").get();
            if (iisFtp == null) {
                List values = new ArrayList();
                values.add(Integer.toString(upload));
                values.add(Integer.toString(status));
                values.add(ftpName);
                dom.addChild("ftp_vhost_anonymous", "", values);
                return;
            }
            iisFtp.FM_updateSettings(upload, status, ftpName);
            return;
        }
        throw new Exception("IP is not dedicated");
    }

    public void updateVdir(AuthToken at, String domainName, String vdirName, int read, int write, int list, int forAll) throws Exception {
        checkPlatform(at);
        FTPVHostDirectoryResource dir = getVhostDirectory(Utils.getAccount(at), domainName, vdirName);
        dir.FM_update(read, write, list, forAll);
    }

    public void updateVuser(AuthToken at, String domainName, String userName, String newPassword) throws Exception {
        checkPlatform(at);
        FTPVHostUserResource user = getVhostUser(Utils.getAccount(at), domainName, userName);
        user.FM_update(newPassword);
    }

    public void delVdirPerm(AuthToken at, String domainName, String vdirName, String userName) throws Exception {
        checkPlatform(at);
        FTPVHostDirectoryResource dir = getVhostDirectory(Utils.getAccount(at), domainName, vdirName);
        FTPVHostUserResource user = getVhostUser(Utils.getAccount(at), domainName, userName);
        dir.FM_delUser(user.getId().getAsString());
    }

    public void delVdir(AuthToken at, String domainName, String vdirName) throws Exception {
        log.debug("Inside delVdir");
        checkPlatform(at);
        FTPVHostDirectoryResource dir = getVhostDirectory(Utils.getAccount(at), domainName, vdirName);
        dir.FM_cdelete(0);
    }

    public String getLogin(AuthToken at) throws Exception {
        ResourceId id = Utils.getAccount(at).getId().findChild("unixuser");
        if (id.get() instanceof WinUserResource) {
            WinUserResource usr = (WinUserResource) id.get();
            return usr.get("login").toString();
        }
        UnixUser usr2 = (UnixUser) id.get();
        return usr2.get("login").toString();
    }
}
