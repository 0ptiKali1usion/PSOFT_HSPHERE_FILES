package psoft.hsphere.axis;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/axis/TicketInfo.class */
public class TicketInfo implements Serializable {

    /* renamed from: id */
    protected String f70id;
    protected String title;
    protected String priority;
    protected String created;
    protected String assigned;
    protected String answered;
    protected String modified;
    protected String closed;
    protected String type;

    public TicketInfo() {
    }

    public TicketInfo(String id, String title, String priority, String created, String assigned, String answered, String modified, String closed, String type) {
        this.f70id = id;
        this.title = title;
        this.priority = priority;
        this.created = created;
        this.type = type;
        this.assigned = assigned;
        this.answered = answered;
        this.modified = modified;
        this.closed = closed;
    }

    public String getId() {
        return this.f70id;
    }

    public void setId(String id) {
        this.f70id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPriority() {
        return this.priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCreated() {
        return this.created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public String getAssigned() {
        return this.assigned;
    }

    public void setAssigned(String assigned) {
        this.assigned = assigned;
    }

    public String getAnswered() {
        return this.answered;
    }

    public void setAnswered(String answered) {
        this.answered = answered;
    }

    public String getModified() {
        return this.modified;
    }

    public void setModified(String modified) {
        this.modified = modified;
    }

    public String getClosed() {
        return this.closed;
    }

    public void setClosed(String closed) {
        this.closed = closed;
    }

    public String getType() {
        return this.type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
