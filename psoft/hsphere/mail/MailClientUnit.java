package psoft.hsphere.mail;

/* loaded from: hsphere.zip:psoft/hsphere/mail/MailClientUnit.class */
public class MailClientUnit {
    protected String host;
    protected int port;
    protected String user;
    protected String password;
    protected String mbox = "INBOX";

    public MailClientUnit(String host, int port, String user, String password) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
    }

    public boolean isEmpty() {
        return this.host == null || this.user == null || this.password == null;
    }
}
