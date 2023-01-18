package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.pgsql.PGSQLDatabase;
import psoft.hsphere.resource.pgsql.PGSQLResource;
import psoft.hsphere.resource.pgsql.PGSQLUser;

/* loaded from: hsphere.zip:psoft/hsphere/axis/PostgreSQLServices.class */
public class PostgreSQLServices {
    private static Category log = Category.getInstance(PostgreSQLServices.class.getName());

    public String getDescription() {
        return "Functions to work with Postgres databases";
    }

    private PGSQLResource getPostgreSQL(Account a) throws Exception {
        ResourceId rid = a.getId().findChild("pgsql");
        if (rid == null) {
            rid = a.addChild("pgsql", "", new ArrayList());
        }
        return (PGSQLResource) rid.get();
    }

    private PGSQLUser getUser(Account a, String owner) throws Exception {
        for (ResourceId resourceId : a.getId().findAllChildren("pgsqluser")) {
            PGSQLUser user = (PGSQLUser) resourceId.get();
            if (user.getName().equals(owner)) {
                return user;
            }
        }
        throw new Exception("No such user: " + owner);
    }

    private PGSQLDatabase getDatabase(Account a, String name) throws Exception {
        for (ResourceId resourceId : a.getId().findChild("pgsql").findChildren("pgsqldatabase")) {
            PGSQLDatabase db = (PGSQLDatabase) resourceId.get();
            log.info("######" + db.getName() + ":" + name);
            if (name.equalsIgnoreCase(db.getName())) {
                return db;
            }
        }
        throw new Exception("No such database: " + name);
    }

    public void addDB(AuthToken at, String name, String description, String owner) throws Exception {
        Account a = Utils.getAccount(at);
        PGSQLResource pgsql = getPostgreSQL(a);
        if (pgsql.FM_isDatabaseExist(name) != null) {
            throw new Exception("Database exists: " + name);
        }
        List values = new ArrayList();
        values.add(name);
        values.add(description);
        getUser(a, owner);
        values.add(owner);
        pgsql.addChild("pgsqldatabase", "", values);
    }

    public void deleteDB(AuthToken at, String name) throws Exception {
        getDatabase(Utils.getAccount(at), name).FM_cdelete(0);
    }

    public void changeDBDescription(AuthToken at, String name, String description) throws Exception {
        getDatabase(Utils.getAccount(at), name).FM_changeDatabaseDescription(description);
    }

    public void addUser(AuthToken at, String name, String password) throws Exception {
        Account a = Utils.getAccount(at);
        PGSQLResource pgsql = getPostgreSQL(a);
        if (pgsql.FM_isUserExist(name) != null) {
            throw new Exception("User exists: " + name);
        }
        List values = new ArrayList();
        values.add(name);
        values.add(password);
        pgsql.addChild("pgsqluser", "", values);
    }

    public void deleteUser(AuthToken at, String name) throws Exception {
        getUser(Utils.getAccount(at), name).FM_cdelete(0);
    }

    public void changePassword(AuthToken at, String name, String newPassword) throws Exception {
        getUser(Utils.getAccount(at), name).FM_changeUserPassword(newPassword);
    }

    public void changeQuota(AuthToken at, String name, double quota) throws Exception {
        PGSQLDatabase db = getDatabase(Utils.getAccount(at), name);
        db.getId().findChild("pgsqldb_quota").get().FM_cdelete(0);
        List values = new ArrayList();
        values.add(Double.toString(quota));
        db.addChild("pgsqldb_quota", "", values);
    }
}
