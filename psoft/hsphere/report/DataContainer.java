package psoft.hsphere.report;

import java.util.Collections;
import java.util.List;
import java.util.Vector;

/* loaded from: hsphere.zip:psoft/hsphere/report/DataContainer.class */
public class DataContainer {

    /* renamed from: dc */
    protected DataComparator f117dc;
    protected List data;

    public DataContainer(Vector data, DataComparator dc) {
        this.f117dc = new DataComparator();
        this.data = data.subList(0, data.size());
        this.f117dc = dc;
    }

    public DataContainer(Vector data) {
        this(data, new DataComparator());
    }

    public DataContainer(List data, DataComparator dc) {
        this.f117dc = new DataComparator();
        this.data = data;
        this.f117dc = dc;
    }

    public DataContainer(List data) {
        this(data, new DataComparator());
    }

    public void reorder(String field, boolean asc) {
        this.f117dc.setConstrain(field, asc);
        Collections.sort(this.data, this.f117dc);
    }

    public int size() {
        return this.data.size();
    }

    public List subList(int start, int end) throws IndexOutOfBoundsException {
        return this.data.subList(start, end);
    }

    public List all() {
        return this.data;
    }
}
