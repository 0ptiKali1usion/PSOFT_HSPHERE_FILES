package psoft.hsphere.resource.p004tt;

import psoft.hsphere.report.DataComparator;

/* renamed from: psoft.hsphere.resource.tt.TicketComparator */
/* loaded from: hsphere.zip:psoft/hsphere/resource/tt/TicketComparator.class */
public class TicketComparator extends DataComparator {
    private static final int TICKET_ID = 1;
    private static final int TICKET_CREATED = 2;
    private static final int TICKET_LASTMOD = 3;
    private static final int TICKET_CLOSED = 4;
    private static final int TICKET_LAST_ANSWERED = 5;
    private static final int TICKET_ASSIGNED = 6;
    private static final int TICKET_TITLE = 7;
    private static final int TICKET_PRIORITY = 8;
    protected int fieldKey;

    @Override // psoft.hsphere.report.DataComparator
    public void setConstrain(String field, boolean asc) {
        super.setConstrain(field, asc);
        if (field.equals("id")) {
            this.fieldKey = 1;
        } else if (field.equals("created")) {
            this.fieldKey = 2;
        } else if (field.equals("lastmod")) {
            this.fieldKey = 3;
        } else if (field.equals("closed")) {
            this.fieldKey = 4;
        } else if (field.equals("lastanswered")) {
            this.fieldKey = 5;
        } else if (field.equals("assigned")) {
            this.fieldKey = 6;
        } else if (field.equals("title")) {
            this.fieldKey = 7;
        } else if (field.equals("priority")) {
            this.fieldKey = 8;
        } else {
            this.fieldKey = 0;
        }
    }

    @Override // psoft.hsphere.report.DataComparator, java.util.Comparator
    public int compare(Object o1, Object o2) {
        Ticket t1 = (Ticket) o1;
        Ticket t2 = (Ticket) o2;
        switch (this.fieldKey) {
            case 1:
                return compareObjects(new Long(t1.getId()), new Long(t2.getId()));
            case 2:
                return compareObjects(t1.created, t2.created);
            case 3:
                return compareObjects(t1.lastmod, t2.lastmod);
            case 4:
                return compareObjects(t1.closed, t2.closed);
            case 5:
                return compareObjects(t1.lastanswered, t2.lastanswered);
            case 6:
                return compareObjects(new Long(t1.assigned), new Long(t2.assigned));
            case 7:
                return compareObjects(t1.title, t2.title);
            case 8:
                return compareObjects(new Integer(t1.priority), new Integer(t2.priority));
            default:
                return 0;
        }
    }
}
