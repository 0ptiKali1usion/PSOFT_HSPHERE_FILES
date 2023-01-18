package psoft.hsphere.mail;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.ContentType;
import org.apache.log4j.Category;
import psoft.hsphere.PGPSecurity.SignificationFilter;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.admin.UpdatableService;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/mail/MailClient.class */
public class MailClient extends Thread implements UpdatableService {
    private static Category log = Category.getInstance(MailClient.class.getName());
    public static final String protocol = "pop3";
    protected long timeout;
    protected Hashtable units;
    protected Properties props;
    protected Session session;

    /* renamed from: mc */
    protected static MailClient f95mc;
    protected boolean cont;
    protected boolean running;
    protected static String DB_ENCODING;

    static {
        DB_ENCODING = null;
        try {
            DB_ENCODING = psoft.hsphere.Session.getPropertyString("DB_ENCODING");
            if ("".equals(DB_ENCODING)) {
                DB_ENCODING = null;
            }
        } catch (Exception e) {
        }
    }

    protected synchronized void loadAllUnits() {
        this.units = new Hashtable();
        for (Reseller r : Reseller.getResellerList()) {
            try {
                psoft.hsphere.Session.setResellerId(r.getId());
                int port = -1;
                try {
                    port = Integer.parseInt(Settings.get().getValue("TT_POP3_PORT"));
                } catch (Exception e) {
                }
                MailClientUnit mu = new MailClientUnit(Settings.get().getValue("TT_POP3_HOST"), port, Settings.get().getValue("TT_POP3_USER"), Settings.get().getValue("TT_POP3_PASSWORD"));
                if (!mu.isEmpty()) {
                    this.units.put(new Long(r.getId()), mu);
                }
            } catch (Exception e2) {
                log.warn("Error during MailUnit init, skip entry", e2);
            }
        }
        log.info("-->Mail Units initialized, units number=" + this.units.size());
    }

    @Override // psoft.hsphere.admin.UpdatableService
    public synchronized void update() {
        try {
            long oldRid = psoft.hsphere.Session.getResellerId();
            try {
                loadAllUnits();
                try {
                    psoft.hsphere.Session.setResellerId(oldRid);
                    this.cont = this.units.size() > 0;
                    if (!this.running) {
                        start();
                    }
                } catch (UnknownResellerException e) {
                    throw new Error("Cant restore reseller");
                }
            } catch (Throwable th) {
                try {
                    psoft.hsphere.Session.setResellerId(oldRid);
                    throw th;
                } catch (UnknownResellerException e2) {
                    throw new Error("Cant restore reseller");
                }
            }
        } catch (UnknownResellerException e3) {
            psoft.hsphere.Session.getLog().warn("Cant get current reseller", e3);
        }
    }

    public static MailClient getInstance() {
        if (f95mc == null) {
            f95mc = new MailClient();
            try {
                f95mc.loadAllUnits();
                f95mc.start();
            } finally {
                try {
                    psoft.hsphere.Session.setResellerId(0L);
                } catch (Exception e) {
                    psoft.hsphere.Session.getLog().warn("Error reseting reseller", e);
                }
            }
        }
        return f95mc;
    }

    public MailClient() {
        super("TT Client: ");
        this.cont = true;
        this.running = false;
        this.timeout = 60000L;
        setPriority(2);
        setDaemon(true);
        this.props = System.getProperties();
        this.props.setProperty("mail.pop3.timeout", "120000");
        this.props.setProperty("mail.pop3.connectiontimeout", "120000");
        this.session = Session.getDefaultInstance(this.props, (Authenticator) null);
        log.info("-->Mailclient intialized");
    }

    @Override // java.lang.Thread
    public void start() {
        if (this.units.size() != 0) {
            super.start();
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        this.running = true;
        log.info("-->Started");
        psoft.hsphere.Session.setLog(log);
        while (this.cont) {
            Enumeration e = this.units.keys();
            while (e.hasMoreElements()) {
                try {
                    Long rid = (Long) e.nextElement();
                    psoft.hsphere.Session.setResellerId(rid);
                    log.info("-->Get messages for reseller #" + psoft.hsphere.Session.getReseller().getUser());
                    getMessages((MailClientUnit) this.units.get(rid));
                } catch (Throwable ex) {
                    log.error("Error retrieving message " + ex.getMessage());
                }
            }
            try {
                sleep(this.timeout);
            } catch (InterruptedException e2) {
            } catch (Throwable th) {
            }
        }
        this.running = false;
    }

    protected void finalize() {
        this.cont = false;
    }

    public void getMessages(MailClientUnit mu) throws MailClientException {
        try {
            Store store = this.session.getStore(protocol);
            store.connect(mu.host, mu.port, mu.user, mu.password);
            Folder folder = store.getDefaultFolder();
            if (folder == null) {
                throw new MailClientException(1, "No Default Folder");
            }
            Folder folder2 = folder.getFolder(mu.mbox);
            if (folder2 == null) {
                throw new MailClientException(1, "Invalid Folder: " + mu.mbox);
            }
            folder2.open(2);
            int totalMessages = folder2.getMessageCount();
            if (totalMessages == 0) {
                folder2.close(false);
                return;
            }
            Message[] msgs = folder2.getMessages();
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.FLAGS);
            fp.add("H-Sphere");
            log.info("Fetching messages");
            folder2.fetch(msgs, fp);
            for (Message message : msgs) {
                try {
                    processMessage(message);
                } catch (Exception e) {
                    log.warn("Error retrieving message", e);
                }
            }
            folder2.close(true);
            store.close();
        } catch (MessagingException me) {
            log.warn("Trying to retrieve mail", me);
            throw new MailClientException(3, "Unable to retrieve mail: " + me.getMessage());
        } catch (NoSuchProviderException e2) {
            this.cont = false;
            throw new MailClientException(2, "No Support For pop3");
        }
    }

    protected String rcptToString(Address[] addr, boolean for_select) {
        if (addr == null) {
            return null;
        }
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < addr.length; i++) {
            if (i != 0) {
                buf.append(',');
            }
            if (for_select) {
                buf.append('\'');
            }
            buf.append(addr[i].toString());
            if (for_select) {
                buf.append('\'');
            }
        }
        return buf.toString();
    }

    protected void processMessage(Message msg) throws Exception {
        log.info("Getting message: " + msg);
        StringBuffer from = new StringBuffer();
        String fromEmail = null;
        Address[] a = msg.getFrom();
        if (a != null) {
            for (int j = 0; j < a.length; j++) {
                if (fromEmail == null) {
                    fromEmail = a[j].toString();
                }
                from.append(a[j].toString()).append("; ");
            }
        }
        String cc = rcptToString(msg.getRecipients(Message.RecipientType.CC), false);
        String rcp = rcptToString(msg.getRecipients(Message.RecipientType.CC), true);
        String to = rcptToString(msg.getRecipients(Message.RecipientType.TO), false);
        String subject = msg.getSubject();
        List l = new ArrayList();
        Date d = msg.getSentDate();
        String date = d != null ? d.toString() : "";
        String description = getDescription(msg, l, true);
        if (description == null) {
            description = "Unsupported body content";
        }
        log.debug("Process message: ");
        if (!Ticket.isSpam(fromEmail, cc)) {
            psoft.hsphere.Session.getLog().info("Trying to remove significations in the text of message/n" + description);
            String description2 = SignificationFilter.getFiltered(description);
            psoft.hsphere.Session.getLog().info("The message after significations removed/n" + description2);
            processTicket(subject, description2, fromEmail, to, cc, rcp, l);
            psoft.hsphere.Session.getLog().info("Trying to remove significations in the text of message/n" + description2);
            psoft.hsphere.Session.getLog().info("This is not spam - " + fromEmail);
        } else {
            psoft.hsphere.Session.getLog().info("This is spam - " + fromEmail);
        }
        msg.setFlag(Flags.Flag.DELETED, true);
    }

    protected String stripEmail(String email) {
        int begin = email.indexOf(60);
        if (begin >= 0) {
            int end = email.indexOf(62, begin);
            email = email.substring(begin + 1, end);
        }
        return email.trim();
    }

    /* JADX WARN: Finally extract failed */
    protected void processTicket(String subject, String text, String from, String to, String cc, String rcp, List l) throws Exception {
        long ticketId = 0;
        if (subject != null) {
            try {
                int pos = subject.lastIndexOf("#");
                if (pos != -1) {
                    try {
                        ticketId = Long.parseLong(subject.substring(pos + 1));
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e2) {
                psoft.hsphere.Session.getLog().warn("Error get ticket: ", e2);
            }
        } else {
            subject = null;
        }
        String from2 = stripEmail(from);
        if (ticketId == 0) {
            String uname = null;
            String aid = null;
            BufferedReader in = new BufferedReader(new StringReader(text));
            new StringBuffer();
            while (true) {
                String line = in.readLine();
                if (line == null) {
                    break;
                }
                String uline = line.toUpperCase();
                if (uline.indexOf("USER") != 0) {
                    if (uline.indexOf("ACCOUNT") != 0) {
                        if (uline.indexOf("==") != -1 && uline.indexOf("CUT HERE") != -1) {
                            break;
                        }
                    } else {
                        aid = getValue(line);
                    }
                } else {
                    uname = getValue(line);
                }
                if (uname != null && aid != null) {
                    break;
                }
            }
            setContext(uname, aid);
            int quid = 0;
            Connection con = psoft.hsphere.Session.getDb();
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM queues WHERE email = ?");
                ps.setString(1, to);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    quid = rs.getInt(1);
                } else if (rcp != null) {
                    Connection con1 = psoft.hsphere.Session.getDb();
                    PreparedStatement ps1 = null;
                    try {
                        try {
                            ps1 = con1.prepareStatement("SELECT id FROM queues WHERE email in (?)");
                            ps1.setString(1, rcp);
                            ResultSet rs1 = ps1.executeQuery();
                            if (rs1.next()) {
                                quid = rs1.getInt(1);
                            } else {
                                quid = 0;
                            }
                            psoft.hsphere.Session.closeStatement(ps1);
                            con1.close();
                        } catch (Exception ex) {
                            psoft.hsphere.Session.getLog().warn("Error get ticket: ", ex);
                            psoft.hsphere.Session.closeStatement(ps1);
                            con1.close();
                        }
                    } catch (Throwable th) {
                        psoft.hsphere.Session.closeStatement(null);
                        con1.close();
                        throw th;
                    }
                }
                psoft.hsphere.Session.closeStatement(ps);
                con.close();
                Ticket t = Ticket.create(getStringWithDBEnc(subject), 75, getStringWithDBEnc(text), from2, 0, 1, 1, 1, quid, 0, l);
                if (cc != null) {
                    t.FM_setCC(cc);
                }
                resetContext();
            } catch (Throwable th2) {
                psoft.hsphere.Session.closeStatement(null);
                con.close();
                throw th2;
            }
        } else {
            BufferedReader in2 = new BufferedReader(new StringReader(text));
            StringBuffer out = new StringBuffer();
            while (true) {
                String line2 = in2.readLine();
                if (line2 == null) {
                    break;
                }
                String uline2 = line2.toUpperCase();
                if (uline2.indexOf("==") != -1 && uline2.indexOf("CUT HERE") != -1) {
                    break;
                }
                out.append(line2).append("\n");
            }
            if ("".equals(out.toString().trim())) {
                return;
            }
            Ticket t2 = null;
            try {
                t2 = Ticket.getTicket(ticketId);
            } catch (Exception e3) {
                psoft.hsphere.Session.getLog().warn("Error get ticket: ", e3);
            }
            String dontCheckEmail = Settings.get().getValue("TT_DONT_CHECK_EMAIL");
            if (t2 != null && (dontCheckEmail != null || from2.equals(stripEmail(t2.getEmail())))) {
                try {
                    t2.FM_addMessage(getStringWithDBEnc(out.toString()), l);
                } catch (Exception e4) {
                    psoft.hsphere.Session.getLog().warn("Error get ticket: ", e4);
                }
                if (cc != null) {
                    t2.FM_setCC(cc);
                }
            } else {
                try {
                    t2 = Ticket.create(getStringWithDBEnc(subject), 75, getStringWithDBEnc(text), from2, 0, 1, 1, 1, 0, 0, l);
                } catch (Exception e5) {
                    psoft.hsphere.Session.getLog().warn("Error get ticket: ", e5);
                }
                if (cc != null && t2 != null) {
                    t2.FM_setCC(cc);
                }
            }
            psoft.hsphere.Session.getLog().warn("Error get ticket: ", e2);
        }
    }

    protected void setContext(String login, String aid) {
        long a = 0;
        if (login == null) {
            return;
        }
        try {
            User u = User.getUser(login);
            if (u == null) {
                return;
            }
            if (u.getResellerId() != psoft.hsphere.Session.getResellerId()) {
                throw new UnknownResellerException("User doesn't belong to current reseller");
            }
            psoft.hsphere.Session.setUser(u);
            if (aid != null) {
                a = Long.parseLong(aid);
            } else {
                psoft.hsphere.Session.getLog().info("--1--");
                Set s = u.getAccountIds();
                psoft.hsphere.Session.getLog().info("--2--:" + s.size());
                if (s.size() == 1) {
                    Iterator i = s.iterator();
                    a = ((ResourceId) i.next()).getId();
                }
            }
            if (a != 0) {
                psoft.hsphere.Session.setAccountId(a);
            }
        } catch (Exception e) {
            psoft.hsphere.Session.getLog().warn("Trying to set user/account: ", e);
        }
    }

    protected void resetContext() {
        try {
            long oldRid = psoft.hsphere.Session.getResellerId();
            psoft.hsphere.Session.setUser(null);
            psoft.hsphere.Session.setResellerId(oldRid);
        } catch (UnknownResellerException e) {
        }
        psoft.hsphere.Session.setAccountId(0L);
    }

    protected String getValue(String str) throws Exception {
        StringTokenizer st = new StringTokenizer(str, " \t:=#");
        if (st.hasMoreTokens()) {
            st.nextToken();
            if (st.hasMoreTokens()) {
                String result = st.nextToken();
                psoft.hsphere.Session.getLog().info("----->Returning: " + result);
                return result;
            }
            return null;
        }
        return null;
    }

    protected String getLocalizedDescription(Part p) throws Exception {
        String result = new String();
        String pctStr = p.getContentType();
        ContentType pContentType = new ContentType(pctStr);
        String pCharset = pContentType.getParameter("charset");
        if (pCharset != null && !"".equals(pCharset)) {
            int strBufSize = p.getSize();
            if (strBufSize < 0) {
                log.debug("Error: Unable to determine the part size.");
                return null;
            }
            int strBufSize2 = strBufSize < 5242880 ? strBufSize : 5242880 + 1024;
            InputStream in = p.getInputStream();
            StringBuffer strBuf = new StringBuffer(strBufSize2);
            byte[] buf = new byte[1024];
            int sizeRead = 0;
            int totalBytes = 0;
            while (sizeRead != -1) {
                sizeRead = in.read(buf, 0, 1024);
                if (sizeRead == -1) {
                    break;
                } else if (sizeRead == 0) {
                    TimeUtils.sleep(100L);
                } else {
                    log.debug("charset : " + pCharset);
                    strBuf.append(new String(buf, 0, sizeRead, pCharset));
                    totalBytes += sizeRead;
                    if (totalBytes >= 5242880) {
                        break;
                    }
                }
            }
            in.close();
            log.debug("Message has been received. Size:  " + Integer.toString(totalBytes));
            result = strBuf.toString();
        } else {
            Object o = p.getContent();
            if (o instanceof String) {
                result = (String) o;
            }
        }
        return result;
    }

    protected String getDescription(Part p, List l, boolean first) throws Exception {
        Object o;
        try {
            String contentType = p.getContentType().toLowerCase();
            log.debug("part content: " + contentType);
            if (contentType.indexOf("multipart/") != -1 || contentType.indexOf("text/plain") != -1 || contentType.indexOf("text/html") != -1) {
                o = p.getContent();
            } else {
                o = p.getInputStream();
            }
            if (o instanceof String) {
                if (first) {
                    return getLocalizedDescription(p);
                }
                l.add(p);
                return null;
            } else if (o instanceof Multipart) {
                String result = null;
                Multipart mp = (Multipart) o;
                int count = mp.getCount();
                for (int i = 0; i < count; i++) {
                    log.debug("processing part #" + Integer.toString(i));
                    String str = getDescription(mp.getBodyPart(i), l, first && result == null);
                    if (str != null) {
                        result = str;
                    }
                }
                return result;
            } else if (o instanceof Message) {
                return getDescription((Part) o, l, first);
            } else {
                if (o instanceof InputStream) {
                    String str2 = p.getContentType();
                    if (first && (str2.indexOf("text/plain") != -1 || str2.indexOf("text/html") != -1)) {
                        StringBuffer result2 = new StringBuffer();
                        InputStream is = (InputStream) o;
                        while (true) {
                            int c = is.read();
                            if (c != -1) {
                                result2.append((char) c);
                            } else {
                                return result2.toString();
                            }
                        }
                    } else {
                        l.add(p);
                        return null;
                    }
                } else {
                    return null;
                }
            }
        } catch (UnsupportedEncodingException e) {
            log.debug("part getUnsupportedDescription");
            return getUnsupportedDescription(p);
        }
    }

    protected String getUnsupportedDescription(Part p) throws Exception {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        p.writeTo(bas);
        String allBody = bas.toString();
        BufferedReader in = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(allBody.getBytes())));
        StringBuffer res = new StringBuffer();
        res.append("[ Note: this message was in unsupported character set. ]");
        res.append("\n[ Some characters may be displayed incorrectly. ]\n\n");
        boolean isHeader = true;
        while (true) {
            String tmp = in.readLine();
            if (tmp != null) {
                if (isHeader) {
                    if ("".equals(tmp)) {
                        isHeader = false;
                    }
                } else {
                    res.append(tmp).append("\n");
                }
            } else {
                return res.toString();
            }
        }
    }

    public static String getStringWithDBEnc(String value) {
        if (DB_ENCODING == null) {
            return value;
        }
        try {
            byte[] bytes = value.getBytes(DB_ENCODING);
            psoft.hsphere.Session.getLog().debug("Str with encoding: " + new String(bytes));
            return new String(bytes);
        } catch (Exception e) {
            psoft.hsphere.Session.getLog().error("Error encoding string", e);
            return value;
        }
    }
}
