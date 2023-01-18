package psoft.hsphere.axis;

import java.util.Arrays;
import java.util.Collection;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.UnixSubUserResource;
import psoft.hsphere.resource.UnixUser;
import psoft.hsphere.resource.UnixUserResource;

/* loaded from: hsphere.zip:psoft/hsphere/axis/UserFtpServices.class */
public class UserFtpServices {
    private static Category log = Category.getInstance(UserFtpServices.class.getName());

    public String getDescription() {
        return "Functions to work with User FTP";
    }

    private UnixSubUserResource getSubUserResource(AuthToken at, String username) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> subusersCol = a.getId().findAllChildren("unixsubuser");
        for (ResourceId subuserRid : subusersCol) {
            UnixSubUserResource subuser = (UnixSubUserResource) subuserRid.get();
            if (username.equals(subuser.getLogin())) {
                return subuser;
            }
        }
        throw new Exception("Failed to find FTPSubUser [" + username + "]");
    }

    public void changeFtpPass(AuthToken at, String newPass) throws Exception {
        Account acc = Utils.getAccount(at);
        UnixUser uu = (UnixUser) acc.getId().findChild("unixuser").get();
        uu.FM_changePassword(newPass);
    }

    public void addFtpSubUser(AuthToken at, String username, String dir, String password) throws Exception {
        Account acc = Utils.getAccount(at);
        ResourceId rid = acc.getId().findChild("unixuser");
        UnixUserResource unixuser = (UnixUserResource) rid.get();
        if (unixuser == null) {
            throw new Exception("unixuser not found");
        }
        unixuser.addChild("unixsubuser", "", Arrays.asList(username, dir, password));
    }

    public void delFtpSubUser(AuthToken at, String username) throws Exception {
        UnixSubUserResource subuser = getSubUserResource(at, username);
        if (subuser == null) {
            throw new Exception("Failed to get subuser");
        }
        subuser.delete(false);
    }

    public void changeSubUserPass(AuthToken at, String subUserName, String newPass) throws Exception {
        UnixSubUserResource subuser = getSubUserResource(at, subUserName);
        subuser.FM_changePassword(newPass);
    }

    public Object[] getFTPSubUsers(AuthToken at) throws Exception {
        Account acc = Utils.getAccount(at);
        UnixUserResource unixuser = (UnixUserResource) acc.getId().findChild("unixuser").get();
        Collection<ResourceId> subusersCol = unixuser.getId().findAllChildren("unixsubuser");
        Object[] subusers = new Object[subusersCol.size()];
        int index = 0;
        for (ResourceId zoneRid : subusersCol) {
            UnixSubUserResource subuser = (UnixSubUserResource) zoneRid.get();
            subusers[index] = subuser.getLogin();
            index++;
        }
        return subusers;
    }
}
