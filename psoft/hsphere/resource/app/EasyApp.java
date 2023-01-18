package psoft.hsphere.resource.app;

import cryptix.tools.UnixCrypt;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Category;
import psoft.encryption.MD5;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.util.PasswordGenerator;
import psoft.util.freemarker.Template2String;
import psoft.util.freemarker.TemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/EasyApp.class */
public abstract class EasyApp implements TemplateHashModel {
    private static Map classMapping;

    /* renamed from: id */
    protected int f181id;
    protected int type;
    protected String version;
    private ResourceId vhost;
    protected String path;
    protected EasyAppResource owner;
    private EasyAppHostAdapter hostAdapter;
    private static Category log = Category.getInstance(EasyApp.class.getName());
    private static final Class[] _argT = {EasyAppResource.class, Integer.class, ResourceId.class, String.class};
    private static final Class[] _argT1 = {EasyAppResource.class, Integer.class, String.class, ResourceId.class, String.class};
    private static Class[] paramTypes = {String.class};

    public abstract void create(Collection collection) throws Exception;

    public abstract void load() throws Exception;

    public abstract void delete() throws Exception;

    public abstract void deleteRecord() throws Exception;

    public abstract String getName();

    protected abstract String getTarball();

    public EasyApp(EasyAppResource owner, int id, int type, String version, ResourceId vhost, String path) throws Exception {
        this.owner = owner;
        this.type = type;
        this.version = version;
        this.vhost = vhost;
        this.path = path;
        this.f181id = id;
        this.hostAdapter = EasyAppHostAdapter.getHostAdapter(vhost);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.f181id);
        }
        if ("owner".equals(key)) {
            return this.owner;
        }
        if ("update".equals(key)) {
            return new TemplateMethodWrapper(this, "FM_updateSettings");
        }
        if ("vhost".equals(key)) {
            return this.vhost;
        }
        if (!"mail_server_name".equals(key)) {
            return "path".equals(key) ? new TemplateString(this.path) : "type".equals(key) ? new TemplateString(this.type) : "version".equals(key) ? new TemplateString(this.version) : "status".equals(key) ? Resource.STATUS_OK : TemplateMethodWrapper.getMethod(this, key);
        }
        try {
            return this.vhost.FM_getChild("mail_service").get("mail_server_name");
        } catch (Exception e) {
            log.warn(e);
            return new TemplateString("ERROR");
        }
    }

    public String FM_getPassword(int size) {
        return PasswordGenerator.getPassword(size);
    }

    public String FM_getMD5(String value) {
        return new MD5(value).asHex();
    }

    public String FM_getCrypt(String salt, String value) {
        UnixCrypt unixcrypt = new UnixCrypt(salt);
        return unixcrypt.crypt(value);
    }

    public String FM_getCrypt2(int length, String salt, String value) {
        UnixCrypt unixcrypt = new UnixCrypt(salt.substring(0, length));
        return unixcrypt.crypt(value);
    }

    public String FM_getPathNoSlash(String target) throws Exception {
        return getPath(target);
    }

    public String FM_getSafePathNoSlash(String target) throws Exception {
        return getSafePath(target);
    }

    public String FM_getURLNoSlash(String target) throws Exception {
        String url = noTrailingSlash(this.hostAdapter.getHostName() + this.path);
        if (target != null && target.length() > 0) {
            url = url + (target.charAt(0) != '/' ? "/" + target : target);
        }
        return "http://" + noTrailingSlash(url);
    }

    public String FM_getTmpDir() throws Exception {
        return this.hostAdapter.getTmpDir();
    }

    public int getId() {
        return this.f181id;
    }

    public int getType() {
        return this.type;
    }

    public String getVersion() {
        return this.version;
    }

    public ResourceId getVhost() {
        return this.vhost;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public static EasyApp getApp(EasyAppResource r, int appType, int id, String version, ResourceId vhost, String path) throws Exception, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class c = getAppClass(r, appType);
        Object[] argV = {r, new Integer(id), version, vhost, path};
        EasyApp app = (EasyApp) c.getConstructor(_argT1).newInstance(argV);
        app.load();
        return app;
    }

    public static EasyApp createApp(EasyAppResource r, int appType, ResourceId vhost, String path, Collection col) throws Exception, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        Class c = getAppClass(r, appType);
        Integer i = new Integer(Session.getNewId("easy_app_seq"));
        Object[] argV = {r, i, vhost, path};
        Session.getLog().debug("createApp: " + r + " " + appType + " " + i + " " + vhost + " " + path);
        Constructor cons = c.getConstructor(_argT);
        EasyApp app = (EasyApp) cons.newInstance(argV);
        try {
            app.create(col);
            return app;
        } catch (Exception e) {
            log.error("Error adding easyapp " + app, e);
            try {
                app.delete();
            } catch (Exception e1) {
                log.error("Error deleting easyapp " + app, e1);
            }
            throw e;
        }
    }

    public long getHostId() throws Exception {
        return this.hostAdapter.getServerId();
    }

    public String getPath() {
        return this.path;
    }

    public void postConfigSafe(String templateName, String destination) throws Exception {
        String config = Template2String.process(Session.getTemplate(templateName), (TemplateModel) this);
        this.hostAdapter.postConfigSafe(config, getSafePath(destination));
    }

    public void postConfig(String templateName, String destination) throws Exception {
        String config = Template2String.process(Session.getTemplate(templateName), (TemplateModel) this);
        this.hostAdapter.postConfig(config, destination, getPath());
    }

    public void processSQL(MySQLWrapper mysql, String templateName) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().info("EMULATION MODE, posting MySQL bundle:");
        } else {
            mysql.batchSQL(Template2String.process(Session.getTemplate(templateName), (TemplateModel) this));
        }
    }

    public void uncompress(String target) throws Exception {
        this.hostAdapter.uncompress(getTarball(), target, getPath());
    }

    public void removePath(String target) throws Exception {
        remove(target);
    }

    private String getPath(String target) throws Exception {
        String path = this.hostAdapter.getDocumentRoot() + getPath();
        if (target != null && target.length() > 0) {
            path = path + (target.charAt(0) != '/' ? "/" + target : target);
        }
        return noTrailingSlash(path);
    }

    private String getSafePath(String target) throws Exception {
        String path = this.hostAdapter.getHomeDirectory() + "_" + getName() + getId() + "_" + this.hostAdapter.getLocalDir();
        if (target != null && target.length() > 0) {
            path = path + (target.charAt(0) != '/' ? "/" + target : target);
        }
        return noTrailingSlash(path);
    }

    private String noTrailingSlash(String path) {
        if (path.charAt(path.length() - 1) == '/') {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    private void remove(String target) throws Exception {
        this.hostAdapter.removePath(getTarball(), target, getPath());
    }

    public void removeSafe(String target) throws Exception {
        this.hostAdapter.removePathSafe(getTarball(), getSafePath(target));
    }

    public void moveSafe(String from, String to) throws Exception {
        this.hostAdapter.moveDirSafe(getPath(from), getSafePath(to));
    }

    private static synchronized void initClassMapping() throws SQLException {
        if (classMapping != null) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            classMapping = new HashMap();
            ps = con.prepareStatement("SELECT type_id, app_type, class_name FROM easy_app_col");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Integer col = new Integer(rs.getInt(1));
                HashMap map = (Map) classMapping.get(col);
                if (map == null) {
                    map = new HashMap();
                    classMapping.put(col, map);
                }
                try {
                    map.put(new Integer(rs.getInt(2)), Class.forName(rs.getString(3)));
                } catch (ClassNotFoundException e) {
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static Class getAppClass(EasyAppResource r, int appType) throws SQLException {
        initClassMapping();
        return (Class) ((Map) classMapping.get(new Integer(r.getId().getType()))).get(new Integer(appType));
    }

    protected void upgradeVersion(String version) throws SQLException {
        this.owner.upgradeVersion(this.f181id, version);
        this.version = version;
    }

    public static boolean isUpgradable(EasyAppResource r, int appType, String version) throws SQLException, NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        Class c = getAppClass(r, appType);
        Method m = c.getMethod("isUpgradable", paramTypes);
        Boolean result = (Boolean) m.invoke(null, version);
        return result.booleanValue();
    }
}
