package psoft.hsphere.manager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Category;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/manager/Manager.class */
public class Manager {

    /* renamed from: id */
    protected int f97id;
    protected String name;
    protected boolean supportReseller;
    protected Map map = new HashMap();
    protected static Map managers;
    protected Set activeKeys;
    private static Category log = Category.getInstance(Manager.class);
    static Class[] argT = {Integer.class, String.class};

    private static synchronized void initManagers() throws Exception {
        if (managers != null) {
            return;
        }
        managers = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, name, reseller_support FROM manager");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                managers.put(rs.getString(2), new Manager(rs.getInt(1), rs.getString(2), rs.getInt(3) != 0));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static Manager getManager(String name) throws Exception {
        initManagers();
        return (Manager) managers.get(name);
    }

    protected Manager(int id, String name, boolean supportReseller) {
        this.name = name;
        this.supportReseller = supportReseller;
        this.f97id = id;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return this.f97id;
    }

    public long getResellerId() throws Exception {
        if (this.supportReseller) {
            return Session.getResellerId();
        }
        return 0L;
    }

    public Collection getEntities() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, class_name, description FROM managed_entity WHERE manager_id = ?");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            List l = new ArrayList();
            while (rs.next()) {
                l.add(initEntity(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            Session.closeStatement(ps);
            con.close();
            return l;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized Collection getActiveKeys() throws Exception {
        if (this.activeKeys != null) {
            return this.activeKeys;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT active_managed_entity.mkey FROM active_managed_entity, managed_entity WHERE active_managed_entity.id = managed_entity.id AND managed_entity.manager_id = ?");
            ps.setInt(1, getId());
            ResultSet rs = ps.executeQuery();
            this.activeKeys = new HashSet();
            while (rs.next()) {
                this.activeKeys.add(rs.getString(1));
            }
            Set set = this.activeKeys;
            Session.closeStatement(ps);
            con.close();
            return set;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Entity getEntity(String key) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT managed_entity.id, class_name, description FROM managed_entity, active_managed_entity WHERE managed_entity.id = active_managed_entity.id AND manager_id = ? AND managed_entity.reseller_id = ? AND active_managed_entity.mkey = ?");
            ps.setInt(1, getId());
            ps.setLong(2, getResellerId());
            ps.setString(3, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Entity initEntity = initEntity(rs.getInt(1), rs.getString(2), rs.getString(3));
                Session.closeStatement(ps);
                con.close();
                return initEntity;
            }
            throw new ManagerException("No known " + getName() + " for key " + key + "found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public Entity getEntity(int id) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, class_name, description FROM managed_entity WHERE id = ? AND manager_id = ? AND reseller_id = ?");
            ps.setInt(1, id);
            ps.setInt(2, getId());
            ps.setLong(3, getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Entity initEntity = initEntity(rs.getInt(1), rs.getString(2), rs.getString(3));
                Session.closeStatement(ps);
                con.close();
                return initEntity;
            }
            throw new ManagerException("No known " + getName() + " for id " + id + "found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void deleteEntity(int eid) throws Exception {
        deleteEntity(eid, true);
    }

    public void deleteEntity(int eid, boolean fullRemove) throws Exception {
        synchronized (this) {
            this.activeKeys = null;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if (fullRemove) {
            try {
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM active_managed_entity WHERE id = ?");
                ps2.setInt(1, eid);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        PreparedStatement ps3 = con.prepareStatement("DELETE FROM managed_entity WHERE id = ?");
        ps3.setInt(1, eid);
        ps3.executeUpdate();
        Session.closeStatement(ps3);
        ps = con.prepareStatement("DELETE FROM managed_entity_data WHERE id = ?");
        ps.setInt(1, eid);
        ps.executeUpdate();
        Session.closeStatement(ps);
        synchronized (this) {
            this.map.remove(new Integer(eid));
        }
        Session.closeStatement(ps);
        con.close();
    }

    public void activateEntity(int eid, String key) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                Entity e = getEntity(key);
                ps = con.prepareStatement("DELETE FROM active_managed_entity WHERE mkey = ? AND id = ?");
                ps.setString(1, key);
                ps.setInt(2, e.getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
            } catch (ManagerException e2) {
                Session.closeStatement(ps);
            }
            ps = con.prepareStatement("INSERT INTO active_managed_entity (mkey, id) VALUES (?, ?)");
            ps.setString(1, key);
            ps.setInt(2, eid);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            synchronized (this) {
                if (this.activeKeys != null) {
                    this.activeKeys.add(key);
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deactivateEntity(int eid, String key) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Entity e = getEntity(key);
            ps = con.prepareStatement("DELETE FROM active_managed_entity WHERE mkey = ? AND id = ?");
            ps.setString(1, key);
            ps.setInt(2, e.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (ManagerException e2) {
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
        synchronized (this) {
            if (this.activeKeys != null) {
                this.activeKeys.remove(key);
            }
        }
    }

    public Entity updateEntity(int eid, String className, String description, long resellerId, Map data) throws Exception {
        deleteEntity(eid, false);
        return addEntity(eid, className, description, resellerId, data);
    }

    protected Entity addEntity(int eid, String className, String description, long resellerId, Map data) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO managed_entity (id, manager_id, class_name, description, reseller_id) VALUES (?, ?, ?, ?, ?)");
            ps2.setInt(1, eid);
            ps2.setInt(2, getId());
            ps2.setString(3, className);
            ps2.setString(4, description);
            ps2.setLong(5, resellerId);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("INSERT INTO managed_entity_data (id, mkey, value) VALUES (?, ?, ?)");
            ps.setInt(1, eid);
            for (String key : data.keySet()) {
                ps.setString(2, key);
                Object value = data.get(key);
                if (value instanceof String[]) {
                    String[] intermediate = (String[]) value;
                    for (String str : intermediate) {
                        ps.setString(3, str);
                        ps.executeUpdate();
                    }
                } else {
                    ps.setString(3, (String) data.get(key));
                    ps.executeUpdate();
                }
            }
            Entity initEntity = initEntity(eid, className, description);
            Session.closeStatement(ps);
            con.close();
            return initEntity;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Entity addEntity(String className, String description, long resellerId, Map data) throws Exception {
        return addEntity(getNewEntityId(), className, description, resellerId, data);
    }

    protected static int getNewEntityId() throws Exception {
        return Session.getNewId("managed_entity_seq");
    }

    protected synchronized Entity initEntity(int id, String className, String description) throws Exception {
        Integer objId = new Integer(id);
        Entity e = (Entity) this.map.get(objId);
        if (e != null) {
            return e;
        }
        Object[] argV = {objId, description};
        Entity e2 = (Entity) Class.forName(className).getConstructor(argT).newInstance(argV);
        this.map.put(objId, e2);
        return e2;
    }
}
