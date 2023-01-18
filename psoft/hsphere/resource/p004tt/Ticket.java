package psoft.hsphere.resource.p004tt;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import javax.mail.Part;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeBodyPart;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.LangBundlesCompiler;
import psoft.hsphere.Localizer;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.PGPSecurity.C0007PGPSecurity;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.Uploader;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.util.ReloadableCacheEntry;
import psoft.util.Request2String;
import psoft.util.StringUtils;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.Template2String;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.resource.tt.Ticket */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/Ticket.class */
public class Ticket extends SharedObject implements TemplateHashModel, ReloadableCacheEntry {
    public static final String PGP_MESSAGE_BEGIN = "-----BEGIN PGP MESSAGE-----";
    public static final String PGP_MESSAGE_END = "-----END PGP MESSAGE-----";
    public static final String PGP_SIGNIFICATION_BEGIN = "-----BEGIN PGP SIGNED MESSAGE-----";
    public static final String PGP_SIGNIFICATION_END = "-----END PGP SIGNATURE-----";
    public static final int MESSAGE = 0;
    public static final int REPLY = 1;
    public static final int HOLD = 1;
    public static final int ST_NEW = 1;
    public static final int ST_INPROGRESS = 2;
    public static final int ST_ACCEPTANCE = 3;
    public static final int ST_ONHOLD = 4;
    public static final int ST_MOREINFO = 5;
    public static final int ST_FIXED = 6;
    public static final int ST_REOPEN = 7;
    public static final int ST_RESOLVED = 8;
    public static final int ST_CLOSED = 9;
    public static final int WEB_ONLY = 1;
    protected long aid;
    protected long rid;
    protected int rtype;
    protected long userId;
    protected Date created;
    protected Date lastmod;
    protected Date closed;
    protected Date closedByUser;
    protected Date lastanswered;
    protected long assigned;
    protected List messages;
    protected String title;
    protected int priority;
    protected int onHold;
    protected int state;
    protected int reply;
    protected int escrule;
    protected int qid;
    protected int count;

    /* renamed from: cc */
    protected String f217cc;
    protected int sticky;
    int type;
    public static final int PUBLIC = 0;
    public static final int INTERNAL = 1;
    public static final int REP_PERIOD = -30;
    protected String email;
    protected int flags;
    protected long reseller_id;
    protected static Map adminsMap = new HashMap();
    protected static ArrayList spamAddressList = new ArrayList();
    protected static Map newTicketsMap = new HashMap();
    private static Object newTicketLock = new Object();

    public boolean isEmpty() {
        return false;
    }

    public String getCC() {
        return this.f217cc;
    }

    public long assignedTo() {
        return this.assigned;
    }

    public long accountId() {
        return this.aid;
    }

    protected boolean isHigh() {
        return isHigh(this.email, this.f217cc);
    }

    public static boolean isHigh(String email, String cc) {
        String ttHighlight = Settings.get().getValue("TT_HIGHLIGHT");
        if (ttHighlight == null || "".equals(ttHighlight.trim())) {
            return false;
        }
        StringTokenizer st = new StringTokenizer(ttHighlight, ";, ");
        List mailList = new ArrayList();
        if (email != null) {
            mailList.add(email.toUpperCase());
        }
        if (cc != null) {
            StringTokenizer st2 = new StringTokenizer(cc, ";, ");
            while (st2.hasMoreTokens()) {
                mailList.add(st2.nextToken().toUpperCase());
            }
        }
        if (!mailList.isEmpty()) {
            int mlength = mailList.size();
            String[] mail = new String[mlength];
            mailList.toArray(mail);
            while (st.hasMoreTokens()) {
                String token = st.nextToken().trim().toUpperCase();
                for (int i = 0; i < mlength; i++) {
                    if (mail[i].indexOf(token) != -1) {
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    public static boolean isSpam(String email, String cc) {
        getSpamAddresses();
        for (int index = 0; index < spamAddressList.size(); index++) {
            String address = (String) spamAddressList.get(index);
            if (email == null) {
                email = new String("");
            }
            if (cc == null) {
                cc = new String("");
            }
            if (email.indexOf(address) != -1 || cc.indexOf(address) != -1) {
                Session.getLog().info("Address - " + address + " in spam list");
                return true;
            }
        }
        Session.getLog().info("Address - " + email + " isn't in spam list");
        return false;
    }

    public static void getSpamAddresses() {
        String ttSpamList = Settings.get().getValue("TT_SPAMLIST");
        if (ttSpamList == null || "".equals(ttSpamList.trim())) {
            return;
        }
        StringTokenizer st = new StringTokenizer(ttSpamList, "\n");
        while (st.hasMoreTokens()) {
            String fullString = st.nextToken();
            spamAddressList.add(fullString.trim());
        }
    }

    public static List getTicketsByAdmin(long assigned, boolean open) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if (open) {
                ps = con.prepareStatement("SELECT id FROM ticket WHERE assigned = ? AND closed IS NULL AND reseller_id = ? ORDER BY priority desc");
            } else {
                ps = con.prepareStatement("SELECT id FROM ticket WHERE assigned = ? AND reseller_id = ? ORDER BY priority desc");
            }
            ps.setLong(1, assigned);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List tickets = new ArrayList();
            while (rs.next()) {
                tickets.add(new Long(rs.getLong(1)));
            }
            return tickets;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public static List getTicketsFromUser(long userid, int max, long Id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM ticket where user_id = ? AND modified >= ? AND reseller_id = ? order by modified desc");
            Calendar cal = TimeUtils.getCalendar();
            cal.getTime();
            cal.add(5, -30);
            ps.setLong(1, userid);
            ps.setDate(2, new java.sql.Date(cal.getTime().getTime()));
            ps.setLong(3, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List usertickets = new ArrayList();
            for (int count = 0; rs.next() && count < max; count++) {
                long tick_id = rs.getLong(1);
                if (Id != tick_id) {
                    usertickets.add(new Long(tick_id));
                }
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

    public static List getTicketsFromEmail(String email, int max, long Id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM ticket where email = ? AND modified >= ? AND reseller_id = ? order by modified desc");
            Calendar cal = TimeUtils.getCalendar();
            cal.getTime();
            cal.add(5, -30);
            ps.setString(1, email);
            ps.setDate(2, new java.sql.Date(cal.getTime().getTime()));
            ps.setLong(3, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            List emailtickets = new ArrayList();
            for (int count = 0; rs.next() && count < max; count++) {
                long tick_id = rs.getLong(1);
                if (Id != tick_id) {
                    emailtickets.add(new Long(tick_id));
                }
            }
            Session.closeStatement(ps);
            con.close();
            return emailtickets;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getNewTickets() throws Exception {
        List list;
        synchronized (newTicketLock) {
            String reseller = Long.toString(Session.getResellerId());
            ArrayList newTickets = (List) newTicketsMap.get(reseller);
            if (newTickets == null) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT id FROM ticket WHERE reseller_id = ? AND (assigned IS NULL OR assigned = 0) AND closed IS NULL ORDER BY id");
                ps.setLong(1, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                newTickets = new ArrayList();
                while (rs.next()) {
                    newTickets.add(new Long(rs.getLong(1)));
                }
                newTicketsMap.put(reseller, newTickets);
                Session.closeStatement(ps);
                con.close();
            }
            list = newTickets;
        }
        return list;
    }

    public TemplateModel FM_usertickets(int max) throws TemplateModelException {
        try {
            return new ListAdapter(getTicketsFromUser(this.userId, max, getId()));
        } catch (Exception e) {
            Session.getLog().error("Unable get tickets");
            return null;
        }
    }

    public TemplateModel FM_emailtickets(int max) throws TemplateModelException {
        try {
            return new ListAdapter(getTicketsFromEmail(this.email, max, getId()));
        } catch (Exception e) {
            Session.getLog().error("Unable get tickets");
            return null;
        }
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("aid".equals(key)) {
            if (this.aid != -1) {
                return new TemplateString(this.aid);
            }
            return null;
        } else if ("rid".equals(key)) {
            if (this.rid != -1) {
                return new TemplateString(this.rid);
            }
            return null;
        } else if ("rtype".equals(key)) {
            if (this.rtype > 0) {
                return new TemplateString(this.rtype);
            }
            return null;
        } else if ("highlight".equals(key)) {
            return new TemplateString(isHigh());
        } else {
            if ("rDesc".equals(key)) {
                if (this.rtype > 0) {
                    try {
                        return new TemplateString(TypeRegistry.getDescription(this.rtype));
                    } catch (NoSuchTypeException e) {
                        return new TemplateString(Localizer.translateMessage("tt.unknown_rtype", new Object[]{new Integer(this.rtype)}));
                    }
                }
                return new TemplateString(Localizer.translateMessage("tt.generic"));
            } else if ("userId".equals(key)) {
                if (this.userId != -1) {
                    return new TemplateString(this.userId);
                }
                return null;
            } else if ("cc".equals(key)) {
                return new TemplateString(this.f217cc);
            } else {
                if ("username".equals(key)) {
                    if (this.userId != -1) {
                        try {
                            return new TemplateString(User.getUser(this.userId).getLogin());
                        } catch (Exception e2) {
                            Session.getLog().info("Unable to retrieve user name for id:" + this.userId);
                            return null;
                        }
                    }
                    return null;
                } else if ("created".equals(key)) {
                    return new TemplateString(this.created);
                } else {
                    if ("lastmod".equals(key)) {
                        return new TemplateString(this.lastmod);
                    }
                    if ("closed".equals(key)) {
                        return new TemplateString(this.closed);
                    }
                    if ("lastanswered".equals(key)) {
                        return new TemplateString(this.lastanswered);
                    }
                    if ("state".equals(key)) {
                        return new TemplateString(this.state);
                    }
                    if ("type".equals(key)) {
                        return new TemplateString(this.type);
                    }
                    if ("reply".equals(key)) {
                        return new TemplateString(this.reply);
                    }
                    if ("escrule".equals(key)) {
                        return new TemplateString(this.escrule);
                    }
                    if ("qid".equals(key)) {
                        return new TemplateString(this.qid);
                    }
                    if ("sticky".equals(key)) {
                        return new TemplateString(this.sticky);
                    }
                    if ("count".equals(key)) {
                        return new TemplateString(this.count);
                    }
                    if ("isAnswered".equals(key)) {
                        return new TemplateString(this.lastanswered != null && this.lastanswered.after(this.lastmod));
                    } else if ("onHold".equals(key)) {
                        if (this.onHold == 0) {
                            return null;
                        }
                        return new TemplateString(this.onHold);
                    } else if ("assigned".equals(key)) {
                        if (this.assigned == 0) {
                            return null;
                        }
                        return (TemplateModel) getAdmins().get(Long.toString(this.assigned));
                    } else if ("email".equals(key)) {
                        return new TemplateString(this.email);
                    } else {
                        if ("isWebOnly".equals(key)) {
                            return new TemplateString((this.flags & 1) == 1);
                        } else if ("title".equals(key)) {
                            return new TemplateString(this.title);
                        } else {
                            if ("flags".equals(key)) {
                                return new TemplateString(this.flags);
                            }
                            if ("priority".equals(key)) {
                                return new TemplateString(this.priority);
                            }
                            if ("admin_list".equals(key)) {
                                return new ListAdapter(getAdmins().values());
                            }
                            if ("admins".equals(key)) {
                                return new MapAdapter(getAdmins());
                            }
                            if (LangBundlesCompiler.INT_USER_BUNDLE.equals(key)) {
                                try {
                                    return new ListAdapter(getMessages());
                                } catch (Exception e3) {
                                    Session.getLog().error("Unable to get messages, TT #" + this.f51id, e3);
                                    return null;
                                }
                            }
                            return super.get(key);
                        }
                    }
                }
            }
        }
    }

    public String getEmail() {
        return this.email;
    }

    protected synchronized List getMessages() throws Exception {
        if (this.messages == null) {
            loadMessages();
        }
        return this.messages;
    }

    protected void loadMessages() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        this.messages = new ArrayList();
        try {
            ps1 = con.prepareStatement("SELECT id, name, mime_type, state FROM ttattachment WHERE ticket_id = ? AND created = ?");
            ps = con.prepareStatement("SELECT message, note, created, type FROM ttmessage WHERE id = ? ORDER BY created DESC");
            ps.setLong(1, this.f51id);
            ps1.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String message = Session.getClobValue(rs, 1);
                String note = Session.getClobValue(rs, 2);
                Message m = new Message(rs.getTimestamp(3), message, note, rs.getInt(4));
                ps1.setTimestamp(2, rs.getTimestamp(3));
                ResultSet rs1 = ps1.executeQuery();
                while (rs1.next()) {
                    Attachment att = new Attachment(rs1.getInt(1), rs1.getString(2), rs1.getString(3), rs1.getInt(4));
                    m.add(att);
                }
                this.messages.add(m);
            }
            Session.closeStatement(ps1);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.util.ReloadableCacheEntry
    public void reload() {
        try {
            synchronized (newTicketLock) {
                load();
                newTicketsMap.clear();
            }
        } catch (Exception e) {
            Session.getLog().warn("Unable to reload ticket #" + this.f51id, e);
        }
    }

    public Ticket(long id) throws Exception {
        super(id);
        load();
    }

    public static void reloadAll() {
        SharedObject.reloadAll();
    }

    public static Ticket getTicket(long id) throws Exception {
        Ticket t;
        synchronized (SharedObject.getLock(id)) {
            t = (Ticket) get(id, Ticket.class);
            if (t == null) {
                t = new Ticket(id);
            }
        }
        if (t != null) {
            long resellerId = Session.getResellerId();
            if (resellerId != 1 && t.reseller_id != resellerId) {
                Session.getLog().debug("An attempt to get ticket #" + id + " was blocked since the ticket belongs to another reseller (id " + resellerId + "). The request was performed by account #" + Session.getAccountId());
                return null;
            }
        }
        return t;
    }

    public static Ticket getTicket(Long id) throws Exception {
        return getTicket(id.longValue());
    }

    public static synchronized Map getAdmins() {
        try {
            String reseller = Long.toString(Session.getResellerId());
            Map admins = (Map) adminsMap.get(reseller);
            if (admins == null) {
                try {
                    admins = initAdmins();
                } catch (Exception e) {
                    Session.getLog().error("Error loading support admins", e);
                }
            }
            return admins;
        } catch (Exception e2) {
            Session.getLog().error("Can't get resellerId ", e2);
            return null;
        }
    }

    public static synchronized Map initAdmins() throws Exception {
        HashMap hashMap = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, name, description, email, signature FROM tt_admin WHERE reseller_id = ? ORDER by id");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateMap hash = new TemplateMap();
                hash.put("id", rs.getString(1));
                hash.put("name", rs.getString(2));
                hash.put("description", rs.getString(3));
                hash.put("email", rs.getString(4));
                hash.put("signature", rs.getString(5));
                hashMap.put(rs.getString(1), hash);
            }
            adminsMap.put(Long.toString(Session.getResellerId()), hashMap);
            Session.closeStatement(ps);
            con.close();
            return hashMap;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static Ticket create(String title, int priority, String message, String email, int flags, int state, int reply, int escrule, int qid, int sticky) throws Exception {
        return create(title, priority, message, email, flags, state, reply, escrule, qid, sticky, null);
    }

    public static Ticket create(String title, int priority, String message, String email, int flags, int state, int reply, int escrule, int qid, int sticky, List att) throws Exception {
        return create(title, priority, message, 0L, 0, email, flags, state, reply, escrule, qid, sticky, att);
    }

    public static Ticket create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky) throws Exception {
        return create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky, (List) null);
    }

    public static Ticket create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky, String att) throws Exception {
        return create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky, 0, att, null);
    }

    public static Ticket create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky, List att) throws Exception {
        return create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky, 0, null, att);
    }

    public static Ticket create(Throwable e, Object o) {
        return create(e, o, null);
    }

    public static Ticket create(Throwable e, Object o, Object other) {
        int type = -1;
        long id = -1;
        ResourceId rid = null;
        if (o != null) {
            try {
                if (o instanceof Resource) {
                    rid = ((Resource) o).getId();
                } else if (o instanceof ResourceId) {
                    rid = (ResourceId) o;
                }
                if (rid != null) {
                    type = rid.getType();
                    id = rid.getId();
                }
            } catch (Exception fatal) {
                Session.getLog().error("Unable to create internal ticket", fatal);
                Session.getLog().error("Problematic exception", e);
                return null;
            }
        }
        StringWriter st = new StringWriter();
        PrintWriter out = new PrintWriter(st);
        if (Session.getUser() != null) {
            out.println("User: " + Session.getUser().getLogin());
            out.println("CP URL: " + Session.getUser().get("CP_URL"));
        }
        out.println("VERSION: " + C0004CP.getVersion());
        if (Session.getAccount() != null) {
            out.println("Account: " + Session.getAccount().getId().getId());
        }
        out.println("Object: " + o);
        if (other != null) {
            out.println("Other: " + other);
        }
        out.println("Request: " + Request2String.toString(Session.getRequest()));
        out.println("----------------------");
        e.printStackTrace(out);
        out.flush();
        return createInternal(e.getMessage(), 50, st.toString(), id, type, null, 0, 1, 0, 1, 0, 0);
    }

    public static Ticket createInternal(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky) throws Exception {
        long oldId = Session.getResellerId();
        try {
            Session.setResellerId(1L);
            Ticket create = create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky, 1);
            Session.setResellerId(oldId);
            return create;
        } catch (Throwable th) {
            Session.setResellerId(oldId);
            throw th;
        }
    }

    public static Ticket create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky, int type) throws Exception {
        return create(title, priority, message, rid, rtype, email, flags, state, reply, escrule, qid, sticky, type, null, null);
    }

    protected static Ticket create(String title, int priority, String message, long rid, int rtype, String email, int flags, int state, int reply, int escrule, int qid, int sticky, int type, String web_atts, List atts) throws Exception {
        String title2;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        long id = Session.getNewIdAsLong("ticket_seq");
        Timestamp created = TimeUtils.getSQLTimestamp();
        try {
            ps = con.prepareStatement("INSERT INTO ticket (id, title, created, priority, user_id, account_id, resource_id, resource_type, type, modified, email, flags, state, reply, escrule, queue_id, sticky, reseller_id, cc) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 1, 0, 1, ?, ?, ?, ?)");
            ps.setLong(1, id);
            if (title == null || "".equals(title)) {
                title2 = "No title";
            } else {
                StringTokenizer tokenizer = new StringTokenizer(title, "\n");
                title2 = tokenizer.nextToken();
            }
            ps.setString(2, title2.length() > 127 ? title2.substring(0, 127) : title2);
            ps.setTimestamp(3, created);
            ps.setInt(4, priority);
            if (Session.getUser() != null) {
                ps.setLong(5, Session.getUser().getId());
            } else {
                ps.setLong(5, -1L);
            }
            if (Session.getAccountId() == 0) {
                ps.setLong(6, -1L);
            } else {
                ps.setLong(6, Session.getAccountId());
            }
            ps.setLong(7, rid);
            ps.setInt(8, rtype);
            ps.setInt(9, type);
            ps.setTimestamp(10, created);
            if (email != null) {
                ps.setString(11, email);
            } else {
                ps.setNull(11, 12);
            }
            ps.setInt(12, flags);
            if (qid == 0) {
                Connection con1 = Session.getDb();
                PreparedStatement ps1 = con1.prepareStatement("SELECT min(id) from queues where def_qu = 1 AND reseller_id = ?");
                ps1.setLong(1, Session.getResellerId());
                ps1.executeQuery();
                ResultSet rs1 = ps1.executeQuery();
                if (rs1.next()) {
                    qid = rs1.getInt(1);
                }
                Session.closeStatement(ps1);
                con1.close();
            }
            ps.setInt(13, qid);
            ps.setInt(14, sticky);
            ps.setLong(15, Session.getResellerId());
            ps.setString(16, "");
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            Ticket t = getTicket(id);
            synchronized (newTicketLock) {
                if (!getNewTickets().contains(new Long(t.getId()))) {
                    getNewTickets().add(new Long(t.getId()));
                } else {
                    Session.getLog().debug("TT#" + t.getId() + " from:" + email + " in THE LIST 3");
                }
            }
            Session.getLog().debug("TT#" + t.getId() + " from:" + email);
            if (web_atts != null) {
                t.FM_addMessage(message, web_atts);
            } else if (atts != null && !atts.isEmpty()) {
                t.FM_addMessage(message, atts);
            } else {
                t.FM_addMessage(message);
            }
            Session.getLog().debug("TT#" + t.getId() + " from:" + email + " 2");
            return t;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setEmail(String email, int flags) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET email = ?, flags = ? WHERE id = ?");
            if (email != null) {
                ps.setString(1, email);
            } else {
                ps.setNull(1, 12);
            }
            ps.setInt(2, flags);
            ps.setLong(3, getId());
            ps.executeUpdate();
            this.email = email;
            this.flags = flags;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setPriority(int priority) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET priority = ? WHERE id = ?");
            ps.setLong(1, priority);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.priority = priority;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setpriority(int prior) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET priority = ? WHERE id = ?");
            ps.setLong(1, prior);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.priority = prior;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setTitle(String title) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET title = ? WHERE id = ?");
            ps.setString(1, title);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.title = title;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setAccount(long aid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT user_id FROM user_account WHERE account_id = ?");
            ps.setLong(1, aid);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long uid = rs.getLong(1);
                PreparedStatement ps2 = con.prepareStatement("UPDATE ticket SET account_id = ?,user_id = ? WHERE id = ?");
                ps2.setLong(1, aid);
                ps2.setLong(2, uid);
                ps2.setLong(3, getId());
                ps2.executeUpdate();
                this.aid = aid;
                this.userId = uid;
                Session.closeStatement(ps2);
            } else {
                Session.addMessage("Wrong account number");
            }
            return this;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_assign(long id) throws Exception {
        return FM_assign(id, true);
    }

    /* JADX WARN: Finally extract failed */
    public TemplateModel FM_assign(long id, boolean force) throws Exception {
        if ((!force && this.assigned != 0) || this.assigned == id) {
            throw new HSUserException("tt.already_assigned");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Connection con2 = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            if (this.state == 1 && id != 0) {
                ps = con.prepareStatement("UPDATE ticket SET assigned = ?, modified = ?, state = 2  WHERE id = ?");
            } else {
                ps = con.prepareStatement("UPDATE ticket SET assigned = ?, modified = ?  WHERE id = ?");
            }
            Timestamp lastmod = TimeUtils.getSQLTimestamp();
            ps.setLong(1, id);
            this.lastmod = lastmod;
            ps.setTimestamp(2, lastmod);
            ps.setLong(3, getId());
            ps.executeUpdate();
            this.assigned = id;
            if (this.state == 1 && id != 0) {
                this.state = 2;
            }
            synchronized (newTicketLock) {
                if (id != 0) {
                    getNewTickets().remove(new Long(getId()));
                } else {
                    getNewTickets().add(new Long(getId()));
                    ps2 = con2.prepareStatement("UPDATE ticket SET assigned = null WHERE id = ?");
                    ps2.setLong(1, getId());
                    ps2.executeUpdate();
                }
            }
            FM_open(false);
            Session.closeStatement(ps);
            con.close();
            Session.closeStatement(ps2);
            con2.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            Session.closeStatement(ps2);
            con2.close();
            throw th;
        }
    }

    public TemplateModel FM_setUser(long uid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET user_id = ? WHERE id = ?");
            ps.setLong(1, uid);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.userId = uid;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_open(boolean unassign) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET closed = ? WHERE id = ?");
            ps.setNull(1, 93);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.closed = null;
            Session.closeStatement(ps);
            con.close();
            if (unassign) {
                FM_assign(0L, true);
            }
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_close() throws Exception {
        if (this.assigned == 0) {
            try {
                long adminId = Session.getAccount().getId().findChild("ttadmin").getId();
                FM_assign(adminId);
            } catch (Exception e) {
                Session.getLog().warn("Failed to set ticket to admin", e);
            }
        }
        Timestamp closed = TimeUtils.getSQLTimestamp();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET closed = ?,state = 9 WHERE id = ?");
            ps.setTimestamp(1, closed);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.closed = closed;
            this.state = 9;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM ttmessage WHERE id = ?");
            ps2.setLong(1, getId());
            ps2.executeUpdate();
            ps = con.prepareStatement("DELETE FROM ticket WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            getNewTickets().remove(new Long(getId()));
            remove(getId(), getClass());
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setOnHold(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET hold = ? WHERE id = ?");
            ps.setInt(1, value);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.onHold = value;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setSticky(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET sticky = ? WHERE id = ?");
            ps.setInt(1, value);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.sticky = value;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setstate(int value) throws Exception {
        if (value != 9) {
            if (this.state == 9) {
                FM_open(false);
            }
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE ticket SET state = ? WHERE id = ?");
                ps.setInt(1, value);
                ps.setLong(2, getId());
                ps.executeUpdate();
                this.state = value;
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        } else {
            FM_close();
        }
        return this;
    }

    public TemplateModel FM_setaccountId(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET aid = ? WHERE id = ?");
            ps.setInt(1, value);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.aid = value;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setreply(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET reply = ? WHERE id = ?");
            ps.setInt(1, value);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.reply = value;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setEscrule(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET escrule = ? WHERE id = ?");
            ps.setInt(1, value);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.escrule = value;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setQueueId(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET queue_id = ? WHERE id = ?");
            ps.setInt(1, value);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.qid = value;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setCC(String cc) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET cc = ? WHERE id = ?");
            ps.setString(1, cc);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.f217cc = cc;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addReply(String message, String note, String web_atts) throws Exception {
        Connection con = Session.getDb();
        Timestamp created = TimeUtils.getSQLTimestamp();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO ttmessage(id, message, note, created, aid, type) VALUES (?, ?, ?, ?, ?, 1)");
            ps.setLong(1, getId());
            Session.setClobValue(ps, 2, message);
            Session.setClobValue(ps, 3, note);
            ps.setTimestamp(4, created);
            if (Session.getAccount() == null) {
                ps.setLong(5, -1L);
            } else {
                ps.setLong(5, Session.getAccount().getId().getId());
            }
            ps.executeUpdate();
            List l = new ArrayList();
            if (web_atts != null) {
                Session.getLog().debug("Create list of attachments...");
                StringTokenizer atTkz = new StringTokenizer(web_atts, ";");
                while (atTkz.hasMoreTokens()) {
                    String str = atTkz.nextToken();
                    str.trim();
                    Session.getLog().debug("Attachment ---> " + str);
                    l.add(str);
                }
            }
            Message msg = new Message(created, message, note, 1);
            getMessages().add(0, msg);
            if (l != null && !l.isEmpty()) {
                addAttachments(l, null, msg, new Timestamp(created.getTime()));
            }
            ps.close();
            if (message != null && !message.equals("")) {
                ps = con.prepareStatement("UPDATE ticket SET answered = ?, reply = 0 WHERE id = ?");
                ps.setTimestamp(1, created);
                ps.setLong(2, getId());
                ps.executeUpdate();
                this.lastanswered = created;
                this.reply = 0;
                if (this.closed != null) {
                    reopen();
                }
                if (msg != null && msg.getAttachments().size() > 0) {
                    mailReply(msg.getAttachments());
                } else {
                    mailReply();
                }
            }
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addMessage(String message) throws Exception {
        return FM_addMessage(message, null, null);
    }

    public TemplateModel FM_addMessage(String message, String web_atts) throws Exception {
        return FM_addMessage(message, web_atts, null);
    }

    public TemplateModel FM_addMessage(String message, List atts) throws Exception {
        return FM_addMessage(message, null, atts);
    }

    protected void addAttachments(List str_l, List l, Timestamp created) throws Exception {
        if (str_l == null && l == null) {
            return;
        }
        if (str_l != null) {
            Iterator i = str_l.iterator();
            while (i.hasNext()) {
                Part att = createAttachmentFromFile((String) i.next());
                addAttachment(att, created);
            }
        } else if (l != null) {
            Iterator i2 = l.iterator();
            while (i2.hasNext()) {
                addAttachment((Part) i2.next(), created);
            }
        }
    }

    protected void addAttachments(List str_l, List l, Message m, Timestamp created) throws Exception {
        if (l == null && str_l == null) {
            return;
        }
        if (str_l != null) {
            Iterator i = str_l.iterator();
            while (i.hasNext()) {
                Part att = createAttachmentFromFile((String) i.next());
                m.add(addAttachment(att, created));
            }
        } else if (l != null) {
            Iterator i2 = l.iterator();
            while (i2.hasNext()) {
                m.add(addAttachment((Part) i2.next(), created));
            }
        }
    }

    protected Part createAttachmentFromFile(String fileName) throws Exception {
        byte[] buf = Uploader.getData(fileName);
        InternetHeaders headers = new InternetHeaders();
        String fName = "";
        if (fileName.indexOf("|") > 0) {
            fName = fileName.substring(0, fileName.indexOf("|"));
        }
        Session.getLog().debug("File name ---> " + fName);
        headers.addHeader("Content-Type", getFileContentType(fName) + ";name=\"" + fName + "\"");
        return new MimeBodyPart(headers, buf);
    }

    protected String getFileContentType(String fileName) throws Exception {
        int extPos = fileName.lastIndexOf(".");
        if (extPos != -1) {
            String ext = fileName.substring(extPos + 1);
            Session.getLog().debug("File extension ---> " + ext);
            String ext2 = ext.toLowerCase();
            if (ext2.equals("jpg") || ext2.equals("jpeg")) {
                return "image/jpeg";
            }
            if (ext2.equals("gif")) {
                return "image/gif";
            }
            if (ext2.equals("html") || ext2.equals("htm") || ext2.equals("shtml")) {
                return "text/html";
            }
            if (ext2.equals("zip") || ext2.equals("tgz") || ext2.equals("arj") || ext2.equals("rar") || ext2.equals("tar")) {
                return "application/zip";
            }
            if (ext2.equals("pdf")) {
                return "application/pdf";
            }
            if (ext2.equals("rtf")) {
                return "application/rtf";
            }
            if (ext2.equals("doc")) {
                return "application/msword";
            }
            if (ext2.equals("eml")) {
                return "message/rfc822";
            }
            if (ext2.equals("txt")) {
                return "text/plain";
            }
            return "application/octet-stream";
        }
        return "application/octet-stream";
    }

    protected Attachment addAttachment(Part p, Timestamp created) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ttattachment (id, ticket_id, state, created, mime_type, name) VALUES (?, ?, ?, ?, ?, ?)");
            int atId = Session.getNewId("ttatt_seq");
            ps.setInt(1, atId);
            ps.setLong(2, getId());
            ps.setInt(3, 0);
            ps.setTimestamp(4, created);
            String mimeType = p.getContentType();
            if (mimeType == null) {
                ps.setNull(5, 12);
            } else {
                ps.setString(5, mimeType);
            }
            String fileName = p.getFileName();
            if (fileName != null && "".equals(fileName) && fileName.length() > 40) {
                fileName = fileName.substring(0, 39);
            }
            if (fileName == null) {
                ps.setNull(6, 12);
            } else {
                ps.setString(6, fileName);
            }
            ps.executeUpdate();
            Attachment.save(atId, p.getInputStream());
            Attachment attachment = new Attachment(atId, fileName, mimeType, 0);
            Session.closeStatement(ps);
            con.close();
            return attachment;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Removed duplicated region for block: B:103:0x0184 A[Catch: Throwable -> 0x01f3, TryCatch #2 {Throwable -> 0x01f3, blocks: (B:79:0x0081, B:81:0x00ca, B:83:0x00e9, B:86:0x0117, B:88:0x0121, B:97:0x0156, B:99:0x015e, B:101:0x0166, B:104:0x0190, B:106:0x01bc, B:108:0x01c4, B:109:0x01c9, B:110:0x01d4, B:112:0x01e0, B:102:0x0175, B:103:0x0184, B:91:0x0133, B:93:0x013c, B:96:0x014b, B:82:0x00d8), top: B:129:0x0081, inners: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:106:0x01bc A[Catch: Throwable -> 0x01f3, TryCatch #2 {Throwable -> 0x01f3, blocks: (B:79:0x0081, B:81:0x00ca, B:83:0x00e9, B:86:0x0117, B:88:0x0121, B:97:0x0156, B:99:0x015e, B:101:0x0166, B:104:0x0190, B:106:0x01bc, B:108:0x01c4, B:109:0x01c9, B:110:0x01d4, B:112:0x01e0, B:102:0x0175, B:103:0x0184, B:91:0x0133, B:93:0x013c, B:96:0x014b, B:82:0x00d8), top: B:129:0x0081, inners: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:112:0x01e0 A[Catch: Throwable -> 0x01f3, TryCatch #2 {Throwable -> 0x01f3, blocks: (B:79:0x0081, B:81:0x00ca, B:83:0x00e9, B:86:0x0117, B:88:0x0121, B:97:0x0156, B:99:0x015e, B:101:0x0166, B:104:0x0190, B:106:0x01bc, B:108:0x01c4, B:109:0x01c9, B:110:0x01d4, B:112:0x01e0, B:102:0x0175, B:103:0x0184, B:91:0x0133, B:93:0x013c, B:96:0x014b, B:82:0x00d8), top: B:129:0x0081, inners: #1 }] */
    /* JADX WARN: Removed duplicated region for block: B:99:0x015e A[Catch: Throwable -> 0x01f3, TryCatch #2 {Throwable -> 0x01f3, blocks: (B:79:0x0081, B:81:0x00ca, B:83:0x00e9, B:86:0x0117, B:88:0x0121, B:97:0x0156, B:99:0x015e, B:101:0x0166, B:104:0x0190, B:106:0x01bc, B:108:0x01c4, B:109:0x01c9, B:110:0x01d4, B:112:0x01e0, B:102:0x0175, B:103:0x0184, B:91:0x0133, B:93:0x013c, B:96:0x014b, B:82:0x00d8), top: B:129:0x0081, inners: #1 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public freemarker.template.TemplateModel FM_addMessage(java.lang.String r8, java.lang.String r9, java.util.List r10) throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 553
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.resource.p004tt.Ticket.FM_addMessage(java.lang.String, java.lang.String, java.util.List):freemarker.template.TemplateModel");
    }

    protected void reopen() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ticket SET closed = NULL, hold = 0 WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            this.closed = null;
            this.onHold = 0;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void mailReply() throws Exception {
        mailTicket("Reply:", "mail/ticket_reply.txt");
    }

    protected void mailReply(List atts) throws Exception {
        mailTicket("Reply:", "mail/ticket_reply.txt", atts);
    }

    protected void mailConfirmation() throws Exception {
        String dontSendConfirmation = Settings.get().getValue("TT_DONT_SEND_CONFIRMATION");
        if (dontSendConfirmation != null && !"".equals(dontSendConfirmation)) {
            return;
        }
        String eml = getEmail();
        if (eml != null) {
            String eml2check = eml.toLowerCase();
            if (eml2check.indexOf("mailer-daemon@") != -1 || eml2check.indexOf("postmaster@") != -1) {
                Session.getLog().warn("Skip confirm due to possible loop");
                return;
            }
        }
        mailTicket("Confirmation:", "mail/ticket_confirm.txt");
    }

    protected void mailTicket(String prefix, String template) throws Exception {
        mailTicket(prefix, template, null);
    }

    /* JADX WARN: Finally extract failed */
    protected void mailTicket(String prefix, String template, List atts) throws Exception {
        String tmp_email;
        Session.getLog().debug(" Attachments :" + (atts == null));
        if (this.type == 1) {
            SimpleHash root = new SimpleHash();
            root.put("root", this);
            CustomEmailMessage.send("INTERNAL_TICKET", Settings.get().getValue("TT_EMAIL"), (TemplateModelRoot) root);
            return;
        }
        if ((this.flags & 1) == 1) {
            tmp_email = null;
        } else {
            tmp_email = this.email;
        }
        List ccList = CustomEmailMessage.getMessage("INTERNAL_TICKET").getCCList();
        ccList.addAll(StringUtils.stringToList(getCC(), ";,"));
        String fromEmail = Settings.get().getValue("TT_EMAIL");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT email FROM queues WHERE id = ? AND reseller_id= ?");
            ps.setLong(1, this.qid);
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next() && rs.getString(1) != null && !"".equals(rs.getString(1))) {
                fromEmail = rs.getString(1);
            }
            Session.closeStatement(ps);
            con.close();
            if (atts != null) {
                String completeMessageText = produceMessage(Template2String.process(Session.getTemplate(template), (TemplateModel) this));
                Session.getLog().debug("Message + Attachments");
                Session.getMailer().sendMessage(tmp_email, (List) null, ccList, prefix + " " + this.title + " #" + this.f51id, completeMessageText, fromEmail, atts, Session.getCurrentCharset(), "multipart/mixed");
                return;
            }
            String completeMessageText2 = produceMessage(Template2String.process(Session.getTemplate(template), (TemplateModel) this));
            Session.getMailer().sendMessage(tmp_email, (List) null, ccList, prefix + " " + this.title + " #" + this.f51id, completeMessageText2, fromEmail, new ArrayList(), Session.getCurrentCharset(), "");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private String produceMessage(String messageIn) throws Exception {
        String messageOut;
        int currentIndex = messageIn.indexOf("======== CUT HERE =========");
        if (currentIndex != -1) {
            String messageOut2 = C0007PGPSecurity.signMessage(messageIn.substring(0, currentIndex) + messageIn.substring(currentIndex + "======== CUT HERE =========".length()));
            if (messageOut2 != null) {
                messageOut = "======== CUT HERE =========\n" + messageOut2;
            } else {
                return messageIn;
            }
        } else {
            messageOut = C0007PGPSecurity.signMessage(null);
            if (messageOut == null) {
                return messageIn;
            }
        }
        return messageOut;
    }

    protected void load() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT title, created, priority, user_id, account_id, resource_id, resource_type, closed, assigned, type, answered, modified, flags, state, reply, escrule, queue_id, sticky, email, closed_by_user, cc, hold, reseller_id FROM ticket WHERE id = ?");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.title = rs.getString(1);
                this.created = rs.getTimestamp(2);
                this.priority = rs.getInt(3);
                this.userId = rs.getLong(4);
                this.aid = rs.getLong(5);
                this.rid = rs.getLong(6);
                this.rtype = rs.getInt(7);
                this.closed = rs.getTimestamp(8);
                this.assigned = rs.getLong(9);
                this.type = rs.getInt(10);
                this.lastanswered = rs.getTimestamp(11);
                this.lastmod = rs.getTimestamp(12);
                this.flags = rs.getInt(13);
                this.state = rs.getInt(14);
                this.reply = rs.getInt(15);
                this.escrule = rs.getInt(16);
                this.qid = rs.getInt(17);
                this.sticky = rs.getInt(18);
                this.email = rs.getString(19);
                this.closedByUser = rs.getTimestamp(20);
                this.f217cc = rs.getString(21);
                if (this.f217cc == null) {
                    this.f217cc = "";
                }
                this.onHold = rs.getInt(22);
                this.reseller_id = rs.getLong("reseller_id");
                return;
            }
            throw new HSUserException("tt.not_found", new Object[]{new Long(this.f51id)});
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void getTicketCount() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT count(*) FROM ticket where reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            this.count = rs.getInt(1);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_message(String id) throws Exception {
        Message temp = null;
        try {
            getMessages();
            int messageIndex = Integer.valueOf(id).intValue();
            int i = 0;
            while (messageIndex >= 0) {
                temp = (Message) this.messages.get(i);
                if (temp.type == 0) {
                    messageIndex--;
                }
                i++;
            }
            List messList = new ArrayList();
            messList.add(temp);
            return new ListAdapter(messList);
        } catch (Exception e) {
            Session.getLog().error("Unable to get messages, TT #" + id, e);
            throw e;
        }
    }

    public static TemplateModel FM_excludeText(String block) throws Exception {
        String result = new String("");
        int currentIndex = 0;
        int previewsIndex = 0;
        do {
            currentIndex = block.indexOf(PGP_MESSAGE_BEGIN, currentIndex);
            if (currentIndex != -1) {
                result = result + block.substring(previewsIndex, currentIndex) + "|Secured information|";
                currentIndex = block.indexOf(PGP_MESSAGE_END, currentIndex) + PGP_MESSAGE_END.length();
            } else {
                result = result + block.substring(previewsIndex);
            }
            previewsIndex = currentIndex;
        } while (currentIndex != -1);
        return new TemplateString(result);
    }

    public long getReseller_id() {
        return this.reseller_id;
    }
}
