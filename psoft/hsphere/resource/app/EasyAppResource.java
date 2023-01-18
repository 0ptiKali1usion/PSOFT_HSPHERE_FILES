package psoft.hsphere.resource.app;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Category;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/EasyAppResource.class */
public class EasyAppResource extends Resource {
    private static Category log = Category.getInstance(EasyAppResource.class.getName());
    Map appMapping;
    EasyAppCreator creator;

    public EasyAppResource(int type, Collection col) throws Exception {
        super(type, col);
        this.appMapping = new HashMap();
        this.creator = new EasyAppCreator(this);
    }

    public EasyAppResource(ResourceId rid) throws Exception {
        super(rid);
        this.appMapping = new HashMap();
        this.creator = new EasyAppCreator(this);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, app_type, version, vhost, path FROM easy_app WHERE owner_id = ? and type_id = ?");
            ps.setLong(1, this.f41id.getId());
            ps.setLong(2, getId().getType());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int appId = rs.getInt(1);
                this.appMapping.put(new Integer(appId), new EasyAppHolder(appId, rs.getInt(2), rs.getString(3), new ResourceId(rs.getString(4)), rs.getString(5), this));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getInstalledApps(int appType) {
        List l = new ArrayList();
        for (EasyAppHolder h : this.appMapping.values()) {
            if (h.getType() == appType) {
                l.add(h);
            }
        }
        if (l.size() > 0) {
            return new TemplateList(l);
        }
        return null;
    }

    public int FM_getInstalledAppsCount(int appType) {
        int count = 0;
        for (EasyAppHolder h : this.appMapping.values()) {
            if (h.getType() == appType) {
                count++;
            }
        }
        return count;
    }

    public int FM_getAppEnabled(int appType) throws Exception {
        int enabled = 0;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT enabled FROM easy_app_list WHERE id = ?");
            ps.setLong(1, appType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                enabled = rs.getInt(1);
            }
            Session.closeStatement(ps);
            con.close();
            return enabled;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_disableAll() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE easy_app_list set enabled = 0");
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_enable(long aid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE easy_app_list set enabled = 1 where id = ?");
            ps.setLong(1, aid);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_deleteApp(int id) throws Exception {
        EasyApp app = getApp(id);
        if (this.appMapping.get(new Integer(id)) == null) {
            throw new Exception("Unknown app: " + id);
        }
        this.appMapping.remove(new Integer(id));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM easy_app WHERE id = ?");
            ps.setInt(1, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            try {
                app.delete();
            } catch (Exception e) {
                log.warn("Error deleting app: ", e);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean checkApp(String path, String vhost) throws SQLException {
        Connection con = Session.getDb();
        boolean ps = null;
        try {
            boolean ps2 = con.prepareStatement("SELECT count(*) FROM easy_app WHERE vhost = ? AND UPPER(path) = UPPER(?) and type_id = ?");
            ps2.setString(1, vhost);
            ps2.setString(2, path);
            ps2.setLong(3, getId().getType());
            ResultSet rs = ps2.executeQuery();
            rs.next();
            return rs.getInt(1) == 0;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void addApp(EasyApp app) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO easy_app (id, app_type, owner_id, version, path, vhost, type_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, app.getId());
            ps.setInt(2, app.getType());
            ps.setLong(3, this.f41id.getId());
            ps.setString(4, app.getVersion());
            ps.setString(5, app.getPath());
            ps.setString(6, app.getVhost().toString());
            ps.setLong(7, getId().getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.appMapping.put(new Integer(app.getId()), new EasyAppHolder(app));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void upgradeVersion(int id, String version) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE easy_app SET version = ? WHERE id = ? AND type_id = ?");
            ps.setString(1, version);
            ps.setInt(2, id);
            ps.setLong(3, getId().getType());
            ps.executeUpdate();
            ((EasyAppHolder) this.appMapping.get(new Integer(id))).setVersion(version);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private EasyApp getApp(int id) throws Exception, NoSuchMethodException, InvocationTargetException, InstantiationException, SQLException {
        return ((EasyAppHolder) this.appMapping.get(new Integer(id))).getApp();
    }

    public EasyApp FM_getApp(int id) throws Exception, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        return getApp(id);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "addApp".equals(key) ? this.creator : "apps".equals(key) ? new TemplateList(this.appMapping.values()) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        for (EasyAppHolder holder : this.appMapping.entrySet()) {
            holder.getApp().deleteRecord();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM easy_app WHERE owner_id = ?");
            ps.setLong(1, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
