package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.mssql.MSSQLDatabase;
import psoft.hsphere.resource.mssql.MSSQLLogin;
import psoft.hsphere.resource.mssql.MSSQLQuota;
import psoft.hsphere.resource.mssql.MSSQLResource;
import psoft.hsphere.resource.mssql.MSSQLUser;

/* loaded from: hsphere.zip:psoft/hsphere/axis/MSSQLServices.class */
public class MSSQLServices {
    private static Category log = Category.getInstance(MSSQLServices.class.getName());

    public String getDescription() {
        return "Functions to work with MSSQL databases";
    }

    private MSSQLResource getMSSQL(Account a) throws Exception {
        ResourceId rid = a.getId().findChild("MSSQL");
        if (rid == null) {
            rid = a.addChild("MSSQL", "", new ArrayList());
        }
        return (MSSQLResource) rid.get();
    }

    private MSSQLUser getUser(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMSSQL(a).getId().findChildren("MSSQLUser")) {
            MSSQLUser user = (MSSQLUser) resourceId.get();
            if (user != null && user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        throw new Exception("No such user: " + name);
    }

    private MSSQLLogin getLogin(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMSSQL(a).getId().findChildren("MSSQLLogin")) {
            MSSQLLogin login = (MSSQLLogin) resourceId.get();
            if (login != null && login.getName().equalsIgnoreCase(name)) {
                return login;
            }
        }
        throw new Exception("No such login: " + name);
    }

    private String getLoginId(Account a, String name) throws Exception {
        try {
            MSSQLLogin login = getLogin(a, name);
            return login.getId().getAsString();
        } catch (Exception e) {
            throw new Exception("No such login: " + name);
        }
    }

    private MSSQLDatabase getDatabase(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMSSQL(a).getId().findChildren("MSSQLDatabase")) {
            MSSQLDatabase db = (MSSQLDatabase) resourceId.get();
            if (db != null && db.getName().equalsIgnoreCase(name)) {
                return db;
            }
        }
        throw new Exception("No such db: " + name);
    }

    public void addDB(AuthToken at, String name, String description, String owner, int quota) throws Exception {
        Account a = Utils.getAccount(at);
        MSSQLResource sql = getMSSQL(a);
        if (sql.FM_isDatabaseExist(name) != null) {
            throw new Exception("Database exists: " + name);
        }
        List values = new ArrayList();
        values.add(name);
        values.add(getLoginId(a, owner));
        values.add(Integer.toString(quota));
        sql.addChild("MSSQLDatabase", "", values);
    }

    public void deleteDB(AuthToken at, String name) throws Exception {
        getDatabase(Utils.getAccount(at), name).FM_cdelete(0);
    }

    public void addUser(AuthToken at, String databaseName, String userLogin, String userName) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId mssqlId = getMSSQL(a).getId();
        MSSQLDatabase database = getDatabase(a, databaseName);
        String loginId = null;
        for (ResourceId resourceId : mssqlId.findChildren("MSSQLLogin")) {
            MSSQLLogin login = (MSSQLLogin) resourceId.get();
            if (login != null && userLogin.equalsIgnoreCase(login.getName())) {
                loginId = login.getId().getAsString();
                for (ResourceId resourceId2 : mssqlId.findChildren("MSSQLUser")) {
                    MSSQLUser user = (MSSQLUser) resourceId2.get();
                    if (user != null && userName.equalsIgnoreCase(user.getName())) {
                        throw new Exception("User exists: " + userName);
                    }
                }
                continue;
            }
        }
        if (loginId == null) {
            throw new Exception("Not found login : " + userLogin);
        }
        List values = new ArrayList();
        values.add(userName);
        values.add(loginId);
        values.add(database.getId().getAsString());
        database.addChild("MSSQLUser", "", values);
    }

    public void deleteUser(AuthToken at, String userName) throws Exception {
        getUser(Utils.getAccount(at), userName).FM_cdelete(0);
    }

    public void addLogin(AuthToken at, String loginName, String password) throws Exception {
        Account a = Utils.getAccount(at);
        MSSQLResource sql = getMSSQL(a);
        if (sql.FM_isLoginExist(loginName) != null) {
            throw new Exception("Login exists: " + loginName);
        }
        List values = new ArrayList();
        values.add(loginName);
        values.add(password);
        sql.addChild("MSSQLLogin", "", values);
    }

    public void deleteLogin(AuthToken at, String loginName) throws Exception {
        getLogin(Utils.getAccount(at), loginName).FM_cdelete(0);
    }

    public void changePassword(AuthToken at, String loginName, String newPassword) throws Exception {
        MSSQLLogin mssqlLogin = getLogin(Utils.getAccount(at), loginName);
        if (mssqlLogin != null) {
            mssqlLogin.FM_changeLoginPassword(newPassword);
        } else {
            Session.getLog().error("MSSQL : Cann't change password for login - " + loginName);
        }
    }

    public void changeQuota(AuthToken at, String name, double quota) throws Exception {
        MSSQLDatabase db = getDatabase(Utils.getAccount(at), name);
        db.getId().findChild("MSSQLQuota").get().FM_cdelete(0);
        List values = new ArrayList();
        values.add(Double.toString(quota));
        db.addChild("MSSQLQuota", "", values);
    }

    public Object[] getLogins(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ArrayList logins = new ArrayList();
        for (ResourceId resourceId : a.getId().findChildren("MSSQLLogin")) {
            MSSQLLogin login = (MSSQLLogin) resourceId.get();
            if (login != null) {
                logins.add(login.getName().toString());
            }
        }
        return logins.toArray();
    }

    public Object[] getDatabases(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ArrayList dbs = new ArrayList();
        for (ResourceId resourceId : a.getId().findChildren("MSSQLDatabase")) {
            MSSQLDatabase db = (MSSQLDatabase) resourceId.get();
            if (db != null) {
                dbs.add(db.getName().toString());
            }
        }
        return dbs.toArray();
    }

    public Object[] getUsers(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ArrayList users = new ArrayList();
        for (ResourceId resourceId : a.getId().findChildren("MSSQLUser")) {
            MSSQLUser user = (MSSQLUser) resourceId.get();
            if (user != null) {
                users.add(user.getName().toString());
            }
        }
        return users.toArray();
    }

    public Object getQuota(AuthToken at, String dbName) throws Exception {
        MSSQLDatabase db = getDatabase(Utils.getAccount(at), dbName);
        MSSQLQuota quota = (MSSQLQuota) db.getId().findChild("MSSQLQuota").get();
        return quota.get("limitMb").toString() + " Mb";
    }
}
