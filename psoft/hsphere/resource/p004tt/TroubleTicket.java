package psoft.hsphere.resource.p004tt;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.GenericReport;
import psoft.knowledgebase.KnowledgeBase;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateList;

/* renamed from: psoft.hsphere.resource.tt.TroubleTicket */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/TroubleTicket.class */
public class TroubleTicket extends Resource {
    protected List tickets;

    public TroubleTicket(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public TroubleTicket(ResourceId rId) throws Exception {
        super(rId);
    }

    public KnowledgeBase FM_getKB(int id) throws Exception {
        return KnowledgeBase.getKB(id);
    }

    public KnowledgeBase FM_getReadOnlyKB(int id) throws Exception {
        return KnowledgeBase.getReadOnlyKB(id);
    }

    public TemplateModel FM_create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky) throws Exception {
        Ticket t = Ticket.create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky);
        if (this.tickets == null) {
            getTickets();
        } else {
            addTickets(new Long(t.getId()));
        }
        return t;
    }

    public TemplateModel FM_create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky, String atts) throws Exception {
        Ticket t = Ticket.create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky, atts);
        if (this.tickets == null) {
            getTickets();
        } else {
            addTickets(new Long(t.getId()));
        }
        return t;
    }

    public TemplateModel FM_getTicket(long id) throws Exception {
        Ticket t = Ticket.getTicket(id);
        if (t.accountId() != Session.getAccount().getId().getId()) {
            return null;
        }
        return t;
    }

    public TemplateModel FM_getTickets() throws Exception {
        TemplateList list = new TemplateList();
        for (Long l : getTickets()) {
            Ticket t = Ticket.getTicket(l);
            if (t.accountId() == Session.getAccount().getId().getId()) {
                list.add((TemplateModel) t);
            }
        }
        return list;
    }

    public TemplateModel FM_getReport(int id) throws Exception {
        return AdvReport.getReport(id);
    }

    public TemplateModel FM_getReport() throws Exception {
        Vector v = new Vector();
        for (Long l : getTickets()) {
            Ticket t = Ticket.getTicket(l);
            if (t.accountId() == Session.getAccount().getId().getId()) {
                Session.getLog().warn("--closed by user-->" + t.closedByUser);
                if (t.closedByUser == null) {
                    Session.getLog().warn("------> adding ticket: " + t);
                    v.add(t);
                }
            }
        }
        return new GenericReport(v, new TicketComparator());
    }

    public TemplateList FM_getTicketsByResourceId(long rid) throws Exception {
        TemplateList list = new TemplateList();
        for (Long l : getTickets()) {
            Ticket t = Ticket.getTicket(l);
            if (t != null && t.accountId() == Session.getAccount().getId().getId() && t.rid == rid) {
                list.add((TemplateModel) t);
            }
        }
        return list;
    }

    public static List getTicketsFromAccount(long accid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM ticket where account_id = ? and reseller_id = ? order by modified desc");
            ps.setLong(1, accid);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List usertickets = new ArrayList();
            while (rs.next()) {
                usertickets.add(new Long(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return usertickets;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_AccountTickets() throws Exception {
        try {
            return new ListAdapter(getTicketsFromAccount(Session.getAccount().getId().getId()));
        } catch (Exception e) {
            Session.getLog().error("Unable get tickets");
            return null;
        }
    }

    public TemplateList FM_getTicketsByResourceType(int rtype) throws Exception {
        TemplateList list = new TemplateList();
        for (Long l : getTickets()) {
            Ticket t = Ticket.getTicket(l);
            if (t.accountId() == Session.getAccount().getId().getId() && t.rtype == rtype) {
                list.add((TemplateModel) t);
            }
        }
        return list;
    }

    protected void addTickets(Long tt) throws Exception {
        loadTickets();
        synchronized (this.tickets) {
            this.tickets.add(tt);
        }
    }

    protected List getTickets() throws Exception {
        ArrayList arrayList;
        loadTickets();
        synchronized (this.tickets) {
            arrayList = new ArrayList(this.tickets);
        }
        return arrayList;
    }

    protected void loadTickets() throws Exception {
        if (this.tickets != null) {
            return;
        }
        this.tickets = new ArrayList();
        synchronized (this.tickets) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id FROM ticket WHERE account_id = ? AND type = 0");
            ps.setLong(1, Session.getAccount().getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.tickets.add(new Long(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_close(long id) throws Exception {
        if (!getTickets().contains(new Long(id))) {
            throw new HSUserException("Ticket not found");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Timestamp closed = TimeUtils.getSQLTimestamp();
        try {
            ps = con.prepareStatement("UPDATE ticket SET closed_by_user = ? WHERE id = ?");
            ps.setTimestamp(1, closed);
            ps.setLong(2, id);
            ps.executeUpdate();
            Ticket.getTicket(id).closedByUser = closed;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
