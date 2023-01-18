package psoft.hsphere.resource.p004tt;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.notes.AccountNotes;
import psoft.hsphere.resource.customjob.CustomJobAdmin;
import psoft.knowledgebase.KnowledgeBaseManager;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.tt.TroubleTicketAdmin */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/TroubleTicketAdmin.class */
public class TroubleTicketAdmin extends Resource {
    protected String name;
    protected String description;
    protected String email;
    protected String signature;

    public String getName() {
        return this.name;
    }

    public TroubleTicketAdmin(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.name = (String) i.next();
        this.description = (String) i.next();
        this.email = (String) i.next();
        if (i.hasNext()) {
            this.signature = (String) i.next();
        } else {
            this.signature = "";
        }
    }

    public TemplateModel FM_createQueue(String name, String prefix, int snd_open, int snd_close, String new_resp, String close_resp, int undelete, String email) throws Exception {
        Queue q = Queue.create(name, prefix, snd_open, snd_close, new_resp, close_resp, undelete, email);
        return q;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO tt_admin (id, name, description, email, reseller_id, signature) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.name);
            ps.setString(3, this.description);
            ps.setString(4, this.email);
            ps.setLong(5, Session.getResellerId());
            ps.setString(6, this.signature);
            if (this.signature == null) {
                this.signature = "";
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            Ticket.initAdmins();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TroubleTicketAdmin(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, description, email, signature FROM tt_admin WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.description = rs.getString(2);
                this.email = rs.getString(3);
                this.signature = rs.getString(4);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public boolean isTicketAccessible(Ticket t) throws Exception {
        if (t == null) {
            return false;
        }
        String fl = getPlanValue("NO_TT_VIEW");
        return fl == null || "".equals(fl) || t.assignedTo() == 0 || t.assignedTo() == getId().getId();
    }

    public TemplateModel FM_getTicket(long id) throws Exception {
        Ticket t = Ticket.getTicket(id);
        if (isTicketAccessible(t)) {
            return t;
        }
        return null;
    }

    public TemplateModel FM_getQueue(long id) throws Exception {
        Queue q = Queue.getQueue(id);
        return q;
    }

    public TemplateModel FM_unsubscribeAll(long qid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM ttadmin_queue where queue_id = ?");
            ps.setLong(1, qid);
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

    public TemplateModel FM_subscribe(long aid, long qid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ttadmin_queue values(?,?)");
            ps.setLong(1, aid);
            ps.setLong(2, qid);
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

    public TemplateModel FM_updateInfo(String name, String description, String email) throws Exception {
        return FM_updateInfo(name, description, email, this.signature);
    }

    public TemplateModel FM_updateInfo(String name, String description, String email, String signature) throws Exception {
        if (signature == null) {
            signature = "";
        } else if (signature.length() > 255) {
            signature = signature.substring(0, 255);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE tt_admin SET name = ?, description = ?, email = ?, signature = ? WHERE id = ?");
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setString(3, email);
            ps.setString(4, signature);
            ps.setLong(5, getId().getId());
            ps.executeUpdate();
            this.name = name;
            this.description = description;
            this.email = email;
            this.signature = signature;
            Session.closeStatement(ps);
            con.close();
            Ticket.initAdmins();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getTTAdmins() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM tt_admin WHERE reseller_id = ? order by name");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List TTAdmins = new ArrayList();
            while (rs.next()) {
                TTAdmins.add(new Long(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return TTAdmins;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static String NameById(long qid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM tt_admin WHERE id = ?");
            ps.setLong(1, qid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_NameById(long qid) throws TemplateModelException {
        try {
            return new TemplateString(NameById(qid));
        } catch (Exception e) {
            Session.getLog().error("Unable get name");
            return null;
        }
    }

    public static int isSubscribed(long qid, long aid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT count(*) FROM ttadmin_queue WHERE queue_id = ? AND admin_id = ?");
            ps.setLong(1, qid);
            ps.setLong(2, aid);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int i = rs.getInt(1);
            Session.closeStatement(ps);
            con.close();
            return i;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_isSubscribed(long qid, long aid) throws TemplateModelException {
        try {
            return new TemplateString(isSubscribed(qid, aid));
        } catch (Exception e) {
            Session.getLog().error("Unable get settings");
            return null;
        }
    }

    public static int canViewTicket(long qid, long aid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT count(*) FROM ttadmin_queue WHERE queue_id = ? AND admin_id = ?");
            PreparedStatement ps2 = con.prepareStatement("SELECT def_qu FROM queues WHERE id = ?");
            ps.setLong(1, qid);
            ps.setLong(2, aid);
            ps2.setLong(1, qid);
            ResultSet rs = ps.executeQuery();
            ResultSet rs2 = ps2.executeQuery();
            rs.next();
            rs2.next();
            int count1 = rs.getInt(1) + rs2.getInt(1);
            Session.closeStatement(ps);
            con.close();
            return count1;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_canViewTicket(long qid, long aid) throws TemplateModelException {
        try {
            return new TemplateString(canViewTicket(qid, aid));
        } catch (Exception e) {
            Session.getLog().error("Unable get settings");
            return null;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("info")) {
                return (TemplateModel) Ticket.getAdmins().get(Long.toString(getId().getId()));
            }
            if (key.equals("new_tickets")) {
                return new ListAdapter(Ticket.getNewTickets());
            }
            if (key.equals("tickets")) {
                return new ListAdapter(Ticket.getTicketsByAdmin(getId().getId(), true));
            }
            if (key.equals("all_tickets")) {
                return new ListAdapter(Ticket.getTicketsByAdmin(getId().getId(), false));
            }
            if (key.equals("admins")) {
                return new MapAdapter(Ticket.getAdmins());
            }
            if (key.equals("admin_list")) {
                return new ListAdapter(Ticket.getAdmins().values());
            }
            if (key.equals("ttadmin_list")) {
                return new ListAdapter(getTTAdmins());
            }
            if (key.equals("queue_count")) {
                return new TemplateString(Queue.getQueues(false).size());
            }
            if (key.equals("queue_list")) {
                return new ListAdapter(Queue.getQueues(false));
            }
            if ("kb_manager".equals(key)) {
                return new KnowledgeBaseManager();
            }
            if (key.equals("cjadmin")) {
                return new CustomJobAdmin(this);
            }
            return super.get(key);
        } catch (Exception e) {
            Session.getLog().error("Key: " + key, e);
            return null;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM tt_admin WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Ticket.initAdmins();
            Iterator i = Ticket.getAdmins().keySet().iterator();
            if (i.hasNext()) {
                String newId = (String) i.next();
                ps = con.prepareStatement("UPDATE ticket SET assigned = ? WHERE assigned = ?");
                ps.setLong(1, Integer.parseInt(newId));
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void FM_addAccountNote(long accountId, String note) throws Exception {
        AccountNotes.addNote(accountId, note);
    }

    public TemplateModel FM_getAccountNotes(long accountId) throws Exception {
        return AccountNotes.getNotes(accountId);
    }
}
