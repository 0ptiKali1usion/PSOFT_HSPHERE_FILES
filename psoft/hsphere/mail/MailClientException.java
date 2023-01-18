package psoft.hsphere.mail;

/* loaded from: hsphere.zip:psoft/hsphere/mail/MailClientException.class */
public class MailClientException extends Exception {
    public static final int INVALID_FOLDER = 1;
    public static final int CRITICAL_ERROR = 2;
    public static final int MESSAGING_ERROR = 3;
    protected int code;

    public int errorCode() {
        return this.code;
    }

    public MailClientException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
