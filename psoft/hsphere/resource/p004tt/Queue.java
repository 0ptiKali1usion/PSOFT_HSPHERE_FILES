package psoft.hsphere.resource.p004tt;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.util.ReloadableCacheEntry;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.tt.Queue */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/Queue.class */
public class Queue extends SharedObject implements TemplateHashModel, ReloadableCacheEntry {
    protected String name;
    protected String prefix;
    protected int snd_open;
    protected int snd_close;
    protected int def_qu;
    protected String email;
    protected long rid;
    protected int count;
    protected String new_resp;
    protected String close_resp;
    protected static Map newQueuesMap = new HashMap();
    private static Object newQueueLock = new Object();

    public boolean isEmpty() {
        return false;
    }

    public static List getQueues(boolean def_qu) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if (def_qu) {
                ps = con.prepareStatement("SELECT id FROM queuesWHERE def_qu = 1 AND reseller_id = ?");
            } else {
                ps = con.prepareStatement("SELECT id FROM queues WHERE reseller_id = ?");
            }
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List Queues = new ArrayList();
            while (rs.next()) {
                Queues.add(new Long(rs.getLong(1)));
            }
            return Queues;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        return "id".equals(key) ? new TemplateString(this.f51id) : "name".equals(key) ? new TemplateString(this.name) : "prefix".equals(key) ? new TemplateString(this.prefix) : "def_qu".equals(key) ? new TemplateString(this.def_qu) : "snd_open".equals(key) ? new TemplateString(this.snd_open) : "snd_close".equals(key) ? new TemplateString(this.snd_close) : "new_resp".equals(key) ? new TemplateString(this.new_resp) : "close_resp".equals(key) ? new TemplateString(this.close_resp) : "email".equals(key) ? new TemplateString(this.email) : super.get(key);
    }

    @Override // psoft.hsphere.SharedObject
    public long getId() {
        return this.f51id;
    }

    @Override // psoft.util.ReloadableCacheEntry
    public void reload() {
        try {
            synchronized (newQueueLock) {
                load();
                newQueuesMap.clear();
            }
        } catch (Exception e) {
            Session.getLog().warn("Unable to reload Queue #" + this.f51id, e);
        }
    }

    public Queue(long id) throws Exception {
        super(id);
        load();
    }

    public static void reloadAll() {
        SharedObject.reloadAll();
    }

    public static Queue getQueue(long id) throws Exception {
        Queue queue;
        synchronized (SharedObject.getLock(id)) {
            Queue t = (Queue) get(id, Queue.class);
            if (t == null) {
                t = new Queue(id);
            }
            queue = t;
        }
        return queue;
    }

    public static Queue getQueue(Long id) throws Exception {
        return getQueue(id.longValue());
    }

    public static Queue create(String name, String prefix, int snd_open, int snd_close, String new_resp, String close_resp, int def_qu, String email) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        long id = Session.getNewIdAsLong("queue_id");
        try {
            ps = con.prepareStatement("INSERT INTO queues (id, name, prefix, snd_open, snd_close, new_resp, close_resp, def_qu, email, reseller_id) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, id);
            name = (name == null || "".equals(name)) ? "No title" : "No title";
            ps.setString(2, name.length() > 127 ? name.substring(0, 127) : name);
            ps.setString(3, prefix);
            ps.setInt(4, snd_open);
            ps.setInt(5, snd_close);
            ps.setString(6, new_resp);
            ps.setString(7, close_resp);
            ps.setInt(8, def_qu);
            ps.setString(9, email);
            ps.setLong(10, Session.getResellerId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            Queue q = getQueue(id);
            return q;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_update(String name, String prefix, int snd_open, int snd_close, String new_resp, String close_resp, int def_qu, String email) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE queues SET name = ?, prefix = ?, snd_open = ?, snd_close = ?, new_resp = ?, close_resp = ?, def_qu = ?, email = ? WHERE id = ?");
            ps.setLong(9, getId());
            name = (name == null || "".equals(name)) ? "No title" : "No title";
            ps.setString(1, name.length() > 127 ? name.substring(0, 127) : name);
            ps.setString(2, prefix);
            ps.setInt(3, snd_open);
            ps.setInt(4, snd_close);
            ps.setString(5, new_resp);
            ps.setString(6, close_resp);
            ps.setInt(7, def_qu);
            ps.setString(8, email);
            ps.executeUpdate();
            this.name = name;
            this.prefix = prefix;
            this.snd_open = snd_open;
            this.snd_close = snd_close;
            this.new_resp = new_resp;
            this.close_resp = close_resp;
            this.def_qu = def_qu;
            this.email = email;
            Session.closeStatement(ps);
            con.close();
            Queue q = getQueue(this.f51id);
            return q;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setDef(int def_qu) throws Exception {
        Connection con = Session.getTransConnection();
        if (def_qu == 1) {
            try {
                PreparedStatement ps1 = con.prepareStatement("SELECT id FROM queues WHERE def_qu = 1 AND reseller_id = ?");
                ps1.setLong(1, Session.getResellerId());
                ResultSet rs1 = ps1.executeQuery();
                PreparedStatement ps2 = con.prepareStatement("UPDATE queues SET def_qu = 0 WHERE reseller_id = ? AND def_qu = 1");
                ps2.setLong(1, Session.getResellerId());
                ps2.executeUpdate();
                while (rs1.next()) {
                    try {
                        int id = rs1.getInt(1);
                        remove(id, Queue.class);
                    } catch (Throwable ex) {
                        Session.getLog().error("Queue system error:", ex);
                    }
                }
            } finally {
                Session.commitTransConnection(con);
            }
        }
        PreparedStatement ps = con.prepareStatement("UPDATE queues SET def_qu = ? WHERE id = ?");
        ps.setLong(1, def_qu);
        ps.setLong(2, getId());
        ps.executeUpdate();
        this.def_qu = def_qu;
        Session.closeStatement(ps);
        return this;
    }

    public TemplateModel FM_setEmail(String email) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE queues SET email = ? WHERE id = ?");
            ps.setString(1, email);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.email = email;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setName(String name) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE queues SET name = ? WHERE id = ?");
            ps.setString(1, name);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.name = name;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setPrefix(String prefix) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE queues SET prefix = ? WHERE id = ?");
            ps.setString(1, this.name);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.prefix = prefix;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delete(int flag) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Connection con1 = Session.getDb();
            PreparedStatement ps1 = con1.prepareStatement("SELECT id FROM ticket WHERE queue_id = ?");
            ps1.setLong(1, getId());
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                int id1 = rs1.getInt(1);
                Ticket t = Ticket.getTicket(id1);
                if (flag == 0) {
                    t.FM_delete();
                } else {
                    t.FM_setQueueId(flag);
                }
            }
            Session.closeStatement(ps1);
            con1.close();
            ps = con.prepareStatement("DELETE FROM queues WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            remove(getId(), getClass());
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void load() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, prefix, snd_open, snd_close, new_resp, close_resp, def_qu, email FROM queues WHERE id = ?");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.prefix = rs.getString(2);
                this.snd_open = rs.getInt(3);
                this.snd_close = rs.getInt(4);
                this.new_resp = rs.getString(5);
                this.close_resp = rs.getString(6);
                this.def_qu = rs.getInt(7);
                this.email = rs.getString(8);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
