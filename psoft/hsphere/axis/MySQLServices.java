package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.hsphere.resource.mysql.UserPrivileges;

/* loaded from: hsphere.zip:psoft/hsphere/axis/MySQLServices.class */
public class MySQLServices {
    private static Category log = Category.getInstance(MySQLServices.class.getName());

    public String getDescription() {
        return "Functions to work with MySQL databases";
    }

    private MySQLResource getMySQL(Account a) throws Exception {
        ResourceId rid = a.getId().findChild("MySQL");
        if (rid == null) {
            rid = a.addChild("MySQL", "", new ArrayList());
        }
        return (MySQLResource) rid.get();
    }

    private MySQLUser getUser(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMySQL(a).getId().findChildren("MySQLUser")) {
            MySQLUser user = (MySQLUser) resourceId.get();
            if (user.getName().equalsIgnoreCase(name)) {
                return user;
            }
        }
        throw new Exception("No such user: " + name);
    }

    private MySQLDatabase getDatabase(Account a, String name) throws Exception {
        for (ResourceId resourceId : getMySQL(a).getId().findChildren("MySQLDatabase")) {
            MySQLDatabase db = (MySQLDatabase) resourceId.get();
            if (db.getName().equalsIgnoreCase(name)) {
                return db;
            }
        }
        throw new Exception("No such db: " + name);
    }

    public void addDB(AuthToken at, String name, String description) throws Exception {
        Account a = Utils.getAccount(at);
        MySQLResource sql = getMySQL(a);
        String fullname = name + "-" + Session.getAccount().getUser().getLogin();
        if (sql.FM_isDatabaseExist(fullname).toString().equalsIgnoreCase("0")) {
            List values = new ArrayList();
            values.add(name);
            values.add(description);
            sql.addChild("MySQLDatabase", "", values);
            return;
        }
        throw new Exception("Database exists: " + fullname);
    }

    public void deleteDB(AuthToken at, String name) throws Exception {
        getDatabase(Utils.getAccount(at), name).FM_cdelete(0);
    }

    public void addUser(AuthToken at, String name, String password) throws Exception {
        Account a = Utils.getAccount(at);
        MySQLResource sql = getMySQL(a);
        String fullname = name + "-" + Session.getAccount().getUser().getLogin();
        if (sql.FM_isDatabaseExist(fullname).toString().equalsIgnoreCase("0")) {
            List values = new ArrayList();
            values.add(name);
            values.add(password);
            sql.addChild("MySQLUser", "", values);
            return;
        }
        throw new Exception("User exists: " + fullname);
    }

    public void deleteUser(AuthToken at, String name) throws Exception {
        getUser(Utils.getAccount(at), name).FM_cdelete(0);
    }

    public void changePassword(AuthToken at, String name, String newPassword) throws Exception {
        getUser(Utils.getAccount(at), name).FM_changeUserPassword(newPassword);
    }

    public void changeQuota(AuthToken at, String name, double quota) throws Exception {
        MySQLDatabase db = getDatabase(Utils.getAccount(at), name);
        db.getId().findChild("mysqldb_quota").get().FM_cdelete(0);
        List values = new ArrayList();
        values.add(Double.toString(quota));
        db.addChild("mysqldb_quota", "", values);
    }

    public void setPrivileges(AuthToken at, String username, String dbname, String[] privileges) throws Exception {
        String priv = null;
        int i = 0;
        MySQLUser user = getUser(Utils.getAccount(at), username);
        user.FM_loadUserPrivilegesOn(dbname);
        UserPrivileges userPriv = user.getUserPrivileges();
        userPriv.revokeAllDatabasePrivileges();
        do {
            if (priv != null) {
                priv = priv + "," + privileges[i];
            } else {
                priv = privileges[i];
            }
            i++;
        } while (i < privileges.length);
        userPriv.setDatabasePrivileges(priv);
    }

    public Object[] getPrivileges(AuthToken at, String username, String dbname) throws Exception {
        ArrayList privileges = new ArrayList();
        MySQLUser user = getUser(Utils.getAccount(at), username);
        user.FM_loadUserPrivilegesOn(dbname);
        UserPrivileges userPriv = user.getUserPrivileges();
        Hashtable ht = userPriv.getDatabasePrivileges();
        Enumeration e = ht.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = (String) ht.get(key);
            if (value.equals("Y")) {
                privileges.add(key);
            }
        }
        return privileges.toArray();
    }
}
