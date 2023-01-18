package psoft.mail;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/mail/Attachment.class */
public class Attachment implements Serializable {
    protected Object body;
    protected String type;

    public Attachment(Object body, String type) {
        this.body = body;
        this.type = type;
    }

    public Object getBody() {
        return this.body;
    }

    public String getType() {
        return this.type;
    }
}
