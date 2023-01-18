package psoft.hsphere.resource.app;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.mysql.MySQLDatabase;
import psoft.hsphere.resource.mysql.MySQLResource;
import psoft.hsphere.resource.mysql.MySQLUser;
import psoft.hsphere.resource.mysql.UserPrivileges;
import psoft.util.PasswordGenerator;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/MySQLWrapper.class */
public class MySQLWrapper implements TemplateHashModel {
    private static Category log = Category.getInstance(MySQLWrapper.class.getName());

    /* renamed from: db */
    ResourceId f185db;
    ResourceId user;

    public MySQLWrapper(String init) {
        int pos = init.indexOf(":");
        this.f185db = new ResourceId(init.substring(0, pos));
        this.user = new ResourceId(init.substring(pos + 1));
    }

    protected MySQLWrapper(ResourceId db, ResourceId user) {
        this.f185db = db;
        this.user = user;
    }

    public static MySQLWrapper create(ResourceId rid, String name, String description) throws Exception {
        Account account = Session.getAccount();
        ResourceId mid = account.FM_getChild("MySQL");
        if (mid == null) {
            mid = account.addChild("MySQL", "", new ArrayList());
        }
        MySQLResource mysql = (MySQLResource) mid.get();
        try {
            List params = new ArrayList();
            String dbName = mysql.getUniqueDatabaseName(name);
            if (dbName == null) {
                throw new Exception("Unable to generate new mysql db name based on: " + name);
            }
            params.add(dbName);
            params.add(description);
            MySQLDatabase db = (MySQLDatabase) mysql.addChild("MySQLDatabase", "", params).get();
            db.lock(rid);
            List params2 = new ArrayList();
            String userName = mysql.getUniqueUsername(name);
            if (userName == null) {
                throw new Exception("Unable to generate new mysql user name based on: " + name);
            }
            params2.add(userName);
            params2.add(PasswordGenerator.getPassword(10));
            MySQLUser user = (MySQLUser) mysql.addChild("MySQLUser", "", params2).get();
            MySQLWrapper wrapper = new MySQLWrapper(db.getId(), user.getId());
            log.debug("BEFORE GRANT");
            wrapper.grant();
            log.debug("AFTER GRANT");
            wrapper.lock(rid);
            log.debug("AFTER LOCK");
            return wrapper;
        } catch (Exception e) {
            log.error("Error in mysql wrapper", e);
            deleteResource(null);
            deleteResource(null);
            throw e;
        }
    }

    private static void deleteResource(Resource r) {
        if (r == null) {
            return;
        }
        try {
            r.FM_cdelete(0);
        } catch (Throwable t) {
            log.warn("Unable to delete resource: " + r, t);
        }
    }

    private TemplateModel getServerName() {
        try {
            return HostManager.getHost(getDB().getHostId()).get("name");
        } catch (Exception e) {
            log.error("Unable to get server name", e);
            return new TemplateString("ERROR");
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("db".equals(key)) {
            return this.f185db;
        }
        if (FMACLManager.USER.equals(key)) {
            return this.user;
        }
        if ("server".equals(key)) {
            return getServerName();
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public void checkLock() throws HSUserException, Exception {
        MySQLDatabase mysqlDB = getDB();
        if (mysqlDB.lockedBy() != null) {
            throw new HSUserException("eshop.db_alreadylocked", new Object[]{mysqlDB.getName()});
        }
        MySQLUser mysqlUser = getUser();
        if (mysqlUser.lockedBy() != null) {
            throw new HSUserException("eshop.user_alreadylocked", new Object[]{mysqlUser.getName()});
        }
    }

    public void lock(ResourceId rid) throws Exception {
        getDB().lock(rid);
        getUser().lock(rid);
    }

    public void unlock() throws Exception {
        getDB().unlock();
        getUser().unlock();
    }

    public MySQLDatabase getDB() throws Exception {
        return (MySQLDatabase) this.f185db.get();
    }

    public MySQLUser getUser() throws Exception {
        return (MySQLUser) this.user.get();
    }

    public void batchSQL(String s) throws Exception {
        try {
            getDB().batchSQL(this.user, new LineNumberReader(new StringReader(s)));
            log.debug("Success executing db querty");
        } catch (Exception e) {
            log.error("Error executing db query", e);
            throw e;
        }
    }

    public void grant() throws Exception {
        new UserPrivileges(getUser().getName(), getDB().getName(), getDB().getHostId()).setDatabasePrivileges("all,grant");
    }

    public String toString() {
        return this.f185db.toString() + ":" + this.user.toString();
    }

    public void delete() throws Exception {
        try {
            if (this.f185db != null) {
                deleteResource(getDB());
            }
        } catch (Throwable e) {
            log.warn("Unable to delete db", e);
        }
    }
}
