package psoft.hsphere.axis;

import freemarker.template.TemplateHashModel;
import java.util.ArrayList;
import java.util.Iterator;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Uploader;
import psoft.hsphere.report.GenericReport;
import psoft.hsphere.report.adv.AllUserTTReport;
import psoft.hsphere.report.adv.TTReport;
import psoft.hsphere.resource.admin.ReportManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.resource.p004tt.TroubleTicket;
import psoft.hsphere.resource.p004tt.TroubleTicketAdmin;
import psoft.util.Salt;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateMap;

/* loaded from: hsphere.zip:psoft/hsphere/axis/SupportServices.class */
public class SupportServices {
    private static Category log = Category.getInstance(SupportServices.class.getName());

    public String getDescription() {
        return "Functions to work with reports and tickets";
    }

    private TroubleTicketAdmin getAdmin(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId adminRid = a.getId().findChild("ttadmin");
        if (adminRid == null) {
            throw new Exception("Not found ttadmin resource");
        }
        TroubleTicketAdmin admin = (TroubleTicketAdmin) adminRid.get();
        return admin;
    }

    private ReportManager getReportManager(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId adminRid = a.getId().findChild("ttadmin");
        if (adminRid == null) {
            throw new Exception("Not found ttadmin resource");
        }
        ResourceId reportviewerRid = a.getId().findChild("reportviewer");
        if (reportviewerRid == null) {
            throw new Exception("Not found report viewer");
        }
        ReportManager repMan = (ReportManager) reportviewerRid.get();
        return repMan;
    }

    private TicketInfo getTicket(TemplateHashModel mapAdapter) throws Exception {
        String id = null;
        String closed = null;
        String type = null;
        String answered = null;
        String modified = null;
        String title = null;
        String priority = null;
        String created = null;
        String assigned = null;
        if (mapAdapter.get("id") != null) {
            id = mapAdapter.get("id").toString();
        }
        if (mapAdapter.get("email") != null) {
            mapAdapter.get("email").toString();
        }
        if (mapAdapter.get("closed") != null) {
            closed = mapAdapter.get("closed").toString();
        }
        if (mapAdapter.get("type") != null) {
            type = mapAdapter.get("type").toString();
        }
        if (mapAdapter.get("answered") != null) {
            answered = mapAdapter.get("answered").toString();
        }
        if (mapAdapter.get("modified") != null) {
            modified = mapAdapter.get("modified").toString();
        }
        if (mapAdapter.get("title") != null) {
            title = mapAdapter.get("title").toString();
        }
        if (mapAdapter.get("priority") != null) {
            priority = mapAdapter.get("priority").toString();
        }
        if (mapAdapter.get("created") != null) {
            created = mapAdapter.get("created").toString();
        }
        if (mapAdapter.get("assigned") != null) {
            assigned = mapAdapter.get("assigned").toString();
        }
        if (mapAdapter.get("deleted") != null) {
            mapAdapter.get("deleted").toString();
        }
        TicketInfo ticketInfo = new TicketInfo(id, title, priority, created, assigned, answered, modified, closed, type);
        return ticketInfo;
    }

    private TicketInfo getTicket(Ticket ticket) throws Exception {
        String id = null;
        String closed = null;
        String type = null;
        String answered = null;
        String modified = null;
        String title = null;
        String priority = null;
        String created = null;
        String assigned = null;
        if (ticket.get("id") != null) {
            id = ticket.get("id").toString();
        }
        if (ticket.get("email") != null) {
            ticket.get("email").toString();
        }
        if (ticket.get("closed") != null) {
            closed = ticket.get("closed").toString();
        }
        if (ticket.get("rtype") != null) {
            type = ticket.get("rtype").toString();
        }
        if (ticket.get("lastanswered") != null) {
            answered = ticket.get("lastanswered").toString();
        }
        if (ticket.get("lastmod") != null) {
            modified = ticket.get("lastmod").toString();
        }
        if (ticket.get("title") != null) {
            title = ticket.get("title").toString();
        }
        if (ticket.get("priority") != null) {
            priority = ticket.get("priority").toString();
        }
        if (ticket.get("created") != null) {
            created = ticket.get("created").toString();
        }
        if (ticket.get("assigned") != null) {
            TemplateMap map = ticket.get("assigned");
            if (map.get("name") != null) {
                assigned = map.get("name").toString();
            }
        }
        if (ticket.get("deleted") != null) {
            ticket.get("deleted").toString();
        }
        TicketInfo ticketInfo = new TicketInfo(id, title, priority, created, assigned, answered, modified, closed, type);
        return ticketInfo;
    }

    private TicketInfo[] getTickets(ListAdapter adapter) throws Exception {
        String sz = adapter.get("size").toString();
        int size = Integer.parseInt(sz);
        TicketInfo[] tickets = new TicketInfo[size];
        int counter = 0;
        while (adapter.hasNext()) {
            Object item = adapter.next();
            if (item instanceof MapAdapter) {
                MapAdapter mapAdapter = (MapAdapter) item;
                tickets[counter] = getTicket(mapAdapter);
            } else if (item instanceof Ticket) {
                Ticket ticket = (Ticket) item;
                tickets[counter] = getTicket(ticket);
            }
            counter++;
        }
        return tickets;
    }

    public TicketInfo[] searchTickets(AuthToken at, String id, String title, String email, String contains, String assignedTo, String createdFrom, String createdTo, String matchCase, String answeredFrom, String answeredTo, String notAnswered, String modifiedFrom, String modifiedTo, String closedFrom, String closedTo, String notClosed, String type) throws Exception {
        ReportManager repMan = getReportManager(at);
        TTReport report = repMan.FM_getAdvReport("ttreport");
        ArrayList values = new ArrayList();
        values.add(createdFrom);
        values.add(createdTo);
        values.add(closedFrom);
        values.add(closedTo);
        values.add(notClosed);
        values.add(assignedTo);
        values.add(title);
        values.add(type);
        values.add(answeredFrom);
        values.add(answeredTo);
        values.add(notAnswered);
        values.add(modifiedFrom);
        values.add(modifiedTo);
        values.add(contains);
        values.add(matchCase);
        values.add(id);
        values.add(email);
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getTickets(adapter);
    }

    public TicketInfo[] searchInResellers(AuthToken at, String id, String title, String email, String contains, String assignedTo, String createdFrom, String createdTo, String matchCase, String answeredFrom, String answeredTo, String notAnswered, String modifiedFrom, String modifiedTo, String closedFrom, String closedTo, String notClosed, String type, String reseller) throws Exception {
        ReportManager repMan = getReportManager(at);
        AllUserTTReport report = repMan.FM_getAdvReport("all_ttreport");
        ArrayList values = new ArrayList();
        values.add(reseller);
        values.add(createdFrom);
        values.add(createdTo);
        values.add(closedFrom);
        values.add(closedTo);
        values.add(notClosed);
        values.add(assignedTo);
        values.add(title);
        values.add(type);
        values.add(answeredFrom);
        values.add(answeredTo);
        values.add(notAnswered);
        values.add(modifiedFrom);
        values.add(modifiedTo);
        values.add(contains);
        values.add(matchCase);
        values.add(id);
        values.add(email);
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getTickets(adapter);
    }

    public TicketInfo[] searchTroubleTickets(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId adminRid = a.getId().findChild("tt");
        if (adminRid == null) {
            throw new Exception("Not found tt resource");
        }
        TroubleTicket ticket = (TroubleTicket) adminRid.get();
        GenericReport report = ticket.FM_getReport();
        ArrayList values = new ArrayList();
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getTickets(adapter);
    }

    private boolean convStrToBool(String str) {
        int i = Integer.parseInt(str);
        if (i == 0) {
            return false;
        }
        return true;
    }

    private boolean isOnHold(Ticket ticket) throws Exception {
        boolean onHold;
        if (ticket.get("onHold") != null) {
            onHold = convStrToBool(ticket.get("onHold").toString());
        } else {
            onHold = false;
        }
        return onHold;
    }

    private boolean isAnswered(Ticket ticket) throws Exception {
        boolean isAnswered;
        if (ticket.get("isAnswered") != null) {
            isAnswered = convStrToBool(ticket.get("isAnswered").toString());
        } else {
            isAnswered = false;
        }
        return isAnswered;
    }

    private TicketInfo[] convListToTicketsArray(ArrayList tickets) throws Exception {
        if (tickets.size() == 0) {
            return null;
        }
        int counter = 0;
        TicketInfo[] ticketsInfo = new TicketInfo[tickets.size()];
        Iterator i = tickets.iterator();
        while (i.hasNext()) {
            ticketsInfo[counter] = (TicketInfo) i.next();
            counter++;
        }
        return ticketsInfo;
    }

    public TicketInfo[] searchOnHoldTickets(AuthToken at) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        ListAdapter adapter = admin.get("tickets");
        ArrayList tickets = new ArrayList();
        while (adapter.hasNext()) {
            long ticketId = Long.parseLong(adapter.next().toString());
            Ticket ticket = (Ticket) admin.FM_getTicket(ticketId);
            if (isOnHold(ticket)) {
                TicketInfo ticketInfo = getTicket(ticket);
                tickets.add(ticketInfo);
            }
        }
        return convListToTicketsArray(tickets);
    }

    public TicketInfo[] searchAnsweredTickets(AuthToken at) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        ListAdapter adapter = admin.get("tickets");
        ArrayList tickets = new ArrayList();
        while (adapter.hasNext()) {
            long ticketId = Long.parseLong(adapter.next().toString());
            Ticket ticket = (Ticket) admin.FM_getTicket(ticketId);
            if (isAnswered(ticket) && !isOnHold(ticket)) {
                TicketInfo ticketInfo = getTicket(ticket);
                tickets.add(ticketInfo);
            }
        }
        return convListToTicketsArray(tickets);
    }

    public TicketInfo[] searchNewTickets(AuthToken at) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        ListAdapter adapter = admin.get("new_tickets");
        ArrayList tickets = new ArrayList();
        while (adapter.hasNext()) {
            long ticketId = Long.parseLong(adapter.next().toString());
            Ticket ticket = (Ticket) admin.FM_getTicket(ticketId);
            TicketInfo ticketInfo = getTicket(ticket);
            tickets.add(ticketInfo);
        }
        return convListToTicketsArray(tickets);
    }

    public TicketInfo[] searchOpenedTickets(AuthToken at) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        ListAdapter adapter = admin.get("tickets");
        ArrayList tickets = new ArrayList();
        while (adapter.hasNext()) {
            long ticketId = Long.parseLong(adapter.next().toString());
            Ticket ticket = (Ticket) admin.FM_getTicket(ticketId);
            if (!isAnswered(ticket) && !isOnHold(ticket)) {
                TicketInfo ticketInfo = getTicket(ticket);
                tickets.add(ticketInfo);
            }
        }
        return convListToTicketsArray(tickets);
    }

    public void setAdminInfo(AuthToken at, String name, String description, String email, String signature) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        admin.FM_updateInfo(name, description, email, signature);
    }

    public void closeTicket(AuthToken at, long ticketId) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        Ticket ticket = admin.FM_getTicket(ticketId);
        ticket.FM_close();
    }

    public void assignTicket(AuthToken at, long ticketId, long ttadminId) throws Exception {
        TroubleTicketAdmin admin = getAdmin(at);
        Ticket ticket = admin.FM_getTicket(ticketId);
        ticket.FM_assign(ttadminId, false);
    }

    public String uploadFile(AuthToken at, String fileName, byte[] out) throws Exception {
        Salt salt = new Salt();
        String signKey = fileName + "|" + salt.getNext(20);
        Uploader.setData(signKey, out);
        return signKey;
    }

    public void makeTicket(AuthToken at, String title, int priority, String message, String email, String atts) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId adminRid = a.getId().findChild("tt");
        if (adminRid == null) {
            throw new Exception("Not found tt resource");
        }
        TroubleTicket ticket = (TroubleTicket) adminRid.get();
        ticket.FM_create(title, priority, message, 0L, 0, email, 0, 1, 0, 1, 0, 0, atts);
    }
}
