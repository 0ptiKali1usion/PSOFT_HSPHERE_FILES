package psoft.hsphere.report;

import java.util.Comparator;
import java.util.Map;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/report/DataComparator.class */
public class DataComparator implements Comparator {
    protected boolean asc;
    protected String field;

    public void setConstrain(String field, boolean asc) {
        this.field = field;
        this.asc = asc;
    }

    public int compareObjects(Object d1, Object d2) {
        int result;
        if (d1 == null && d2 == null) {
            result = 0;
        } else if (d1 == null) {
            result = 1;
        } else if (d2 == null) {
            result = -1;
        } else {
            try {
                result = ((Comparable) d1).compareTo((Comparable) d2);
            } catch (ClassCastException e) {
                result = compareInternal(d1, d2);
            }
        }
        return this.asc ? result : -result;
    }

    @Override // java.util.Comparator
    public int compare(Object o1, Object o2) {
        Object d1 = ((Map) o1).get(this.field);
        Object d2 = ((Map) o2).get(this.field);
        return compareObjects(d1, d2);
    }

    public int compareInternal(Object o1, Object o2) {
        Session.getLog().error("Not implemented:" + o1.getClass().getName());
        throw new Error("Not implemented:" + o1.getClass().getName());
    }
}
