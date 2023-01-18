package psoft.mail;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import javax.mail.Address;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/mail/QueuedMessage.class */
public class QueuedMessage implements Serializable, Comparable {
    private static final long serialVersionUID = 1000;
    protected int priority;
    protected String from;
    protected String subject;
    protected String message;
    protected String charset;
    protected String contentType;

    /* renamed from: cc */
    protected Address[] f244cc;
    protected Address[] bcc;
    protected Address[] rcpt;
    protected List attachments;
    protected Date created;
    protected Properties props;
    protected Date timeToSend;
    protected int triesToSend;

    public Properties getProperties() {
        return this.props;
    }

    public String getFrom() {
        return this.from;
    }

    public Address[] getTo() {
        return this.rcpt;
    }

    public Address[] getCC() {
        return this.f244cc;
    }

    public Address[] getBCC() {
        return this.bcc;
    }

    public String getSubject() {
        return this.subject;
    }

    public Date getDate() {
        return this.created;
    }

    public List getAttachments() {
        return this.attachments;
    }

    public String getText() {
        return this.message;
    }

    public String getCharset() {
        return this.charset;
    }

    public String getContentType() {
        return this.contentType;
    }

    public Date getNextTime() {
        return this.timeToSend;
    }

    public boolean isNeedsToBeRemoved() {
        return this.triesToSend >= 26;
    }

    public void setNextTime() {
        if (this.triesToSend < 26) {
            this.triesToSend++;
            this.priority++;
        }
        this.timeToSend.setTime(System.currentTimeMillis() + ((2 << this.triesToSend) * 10) + 60000);
        DateFormat dateFormat = DateFormat.getInstance();
        Session.getLog().debug("Message from: " + this.from + " Subj :" + this.subject + " Next time to send: " + dateFormat.format(this.timeToSend) + " Priority: " + this.priority);
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        if (o == this) {
            return 0;
        }
        QueuedMessage m = (QueuedMessage) o;
        if (m.priority != this.priority) {
            if (m.priority > this.priority) {
                return -1;
            }
            return 1;
        }
        int comp = getDate().compareTo(m.getDate());
        if (comp != 0) {
            return comp;
        }
        if (hashCode() > m.hashCode()) {
            return -1;
        }
        return 1;
    }

    public QueuedMessage(Properties props, String from, Address[] cc, Address[] bcc, Address[] rcpt, String subject, String message, List attachments) {
        this(props, from, cc, bcc, rcpt, subject, message, attachments, 0, "");
    }

    public QueuedMessage(Properties props, String from, Address[] cc, Address[] bcc, Address[] rcpt, String subject, String message, List attachments, int priority) {
        this(props, from, cc, bcc, rcpt, subject, message, attachments, priority, "");
    }

    public QueuedMessage(Properties props, String from, Address[] cc, Address[] bcc, Address[] rcpt, String subject, String message, List attachments, String charset) {
        this(props, from, cc, bcc, rcpt, subject, message, attachments, 0, charset);
    }

    public QueuedMessage(Properties props, String from, Address[] cc, Address[] bcc, Address[] rcpt, String subject, String message, List attachments, int priority, String charset) {
        this(props, from, cc, bcc, rcpt, subject, message, attachments, priority, charset, "");
    }

    public QueuedMessage(Properties props, String from, Address[] cc, Address[] bcc, Address[] rcpt, String subject, String message, List attachments, String charset, String contentType) {
        this(props, from, cc, bcc, rcpt, subject, message, attachments, 0, charset, contentType);
    }

    public QueuedMessage(Properties props, String from, Address[] cc, Address[] bcc, Address[] rcpt, String subject, String message, List attachments, int priority, String charset, String contentType) {
        this.contentType = null;
        this.props = props;
        this.from = from;
        this.f244cc = cc;
        this.bcc = bcc;
        this.rcpt = rcpt;
        this.subject = subject;
        this.message = message;
        this.attachments = attachments;
        this.priority = priority;
        this.created = TimeUtils.getDate();
        this.charset = charset;
        this.contentType = contentType;
        this.triesToSend = 0;
        this.timeToSend = new Date(System.currentTimeMillis());
        Session.getLog().debug("QueuedMessage. Creating a new message: From:" + from + " subject:" + subject);
    }
}
