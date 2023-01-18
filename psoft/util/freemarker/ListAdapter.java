package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/util/freemarker/ListAdapter.class */
public class ListAdapter implements TemplateListModel, TemplateHashModel, Serializable, TemplateScalarModel {
    private static final long serialVersionUID = 6606;
    transient Collection col;

    /* renamed from: s */
    Serializable f261s;

    /* renamed from: i */
    transient Iterator f262i;
    transient boolean isRewound;
    int total;
    int start;
    int count;

    public ListAdapter(Collection col, int start, int total) {
        this.count = 0;
        try {
            this.f261s = (Serializable) col;
        } catch (ClassCastException e) {
        }
        this.col = col;
        this.f262i = col.iterator();
        this.isRewound = true;
        this.start = start;
        this.total = total;
        int n = 1;
        while (n <= col.size()) {
            int i = n;
            n++;
            if (i < start) {
                this.f262i.next();
            } else {
                return;
            }
        }
    }

    protected Collection getCollection() {
        if (this.col == null) {
            this.col = (Collection) this.f261s;
        }
        return this.col;
    }

    public ListAdapter(Collection col) {
        this(col, 1, col.size());
    }

    public boolean hasNext() {
        return this.f262i.hasNext() && this.count < this.total;
    }

    public boolean isEmpty() {
        return getCollection().isEmpty();
    }

    public TemplateModel get(int n) {
        rewind();
        for (int j = 0; j < n; j++) {
            next();
        }
        Object o = this.f262i.next();
        if (o == null) {
            return null;
        }
        if (o instanceof TemplateModel) {
            return (TemplateModel) o;
        }
        if (o instanceof Collection) {
            return new ListAdapter((Collection) o);
        }
        if (o instanceof Map) {
            return new MapAdapter((Map) o);
        }
        return new TemplateString(o);
    }

    public TemplateModel next() {
        this.count++;
        this.isRewound = false;
        Object o = this.f262i.next();
        if (o == null) {
            return null;
        }
        if (o instanceof TemplateModel) {
            return (TemplateModel) o;
        }
        if (o instanceof Collection) {
            return new ListAdapter((Collection) o);
        }
        if (o instanceof Map) {
            return new MapAdapter((Map) o);
        }
        return new TemplateString(o);
    }

    public boolean isRewound() {
        return this.isRewound;
    }

    public void rewind() {
        this.f262i = getCollection().iterator();
        this.count = 0;
        this.isRewound = true;
        int n = 1;
        while (true) {
            int i = n;
            n++;
            if (i < this.start) {
                this.f262i.next();
            } else {
                return;
            }
        }
    }

    public TemplateModel get(String key) {
        if ("size".equals(key)) {
            return new TemplateString(this.col.size());
        }
        return null;
    }

    public String getAsString() throws TemplateModelException {
        String result = new String();
        Iterator i = getCollection().iterator();
        while (i.hasNext()) {
            result = result + ((TemplateScalarModel) i.next()).getAsString();
        }
        return result;
    }
}
