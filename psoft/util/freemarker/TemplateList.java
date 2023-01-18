package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList.class */
public class TemplateList implements TemplateListModel, Serializable, TemplateHashModel {
    protected transient Collection col;
    protected transient Adder adder;
    protected transient HashAdder hashAdder;
    protected transient ListAdder listAdder;
    protected transient ContainChecker containChecker;

    /* renamed from: s */
    protected Serializable f274s;

    /* renamed from: i */
    protected transient Iterator f275i;
    protected boolean isRewound;
    protected int end;
    protected int start;
    protected int count;

    public void add(TemplateModel tm) {
        this.col.add(tm);
        this.end++;
        rewind();
    }

    public void add(Object o) {
        add((TemplateModel) new TemplateString(o));
    }

    public TemplateList() {
        this(new ArrayList());
    }

    public TemplateList(TemplateList obj) {
        this.adder = new Adder();
        this.hashAdder = new HashAdder();
        this.listAdder = new ListAdder();
        this.containChecker = new ContainChecker();
        this.count = 0;
        this.col = Collections.synchronizedCollection(obj.col);
        try {
            this.f274s = (Serializable) this.col;
        } catch (ClassCastException e) {
        }
        this.f275i = this.col.iterator();
        this.isRewound = true;
        this.end = obj.end;
        this.start = obj.start;
        this.count = obj.count;
        for (int n = 0; n < this.start; n++) {
            this.f275i.next();
        }
    }

    public TemplateList(Collection col) {
        this(col, 0, col.size());
    }

    public TemplateList(Collection col, int start, int end) {
        this.adder = new Adder();
        this.hashAdder = new HashAdder();
        this.listAdder = new ListAdder();
        this.containChecker = new ContainChecker();
        this.count = 0;
        try {
            this.f274s = (Serializable) col;
        } catch (ClassCastException e) {
        }
        this.col = Collections.synchronizedCollection(col);
        this.f275i = col.iterator();
        this.isRewound = true;
        this.start = start;
        this.end = end;
        for (int n = 0; n < start; n++) {
            this.f275i.next();
        }
    }

    public Collection getCollection() {
        if (this.col == null) {
            this.col = (Collection) this.f274s;
        }
        return this.col;
    }

    public boolean hasNext() {
        return this.f275i.hasNext() && this.count < this.end;
    }

    public boolean isEmpty() {
        return getCollection().isEmpty();
    }

    public TemplateModel get(int i) {
        Object obj;
        Collection col = getCollection();
        if (col instanceof List) {
            obj = ((List) col).get(i);
        } else {
            obj = col.toArray()[i];
        }
        return getTemplateObject(obj);
    }

    protected TemplateModel getTemplateObject(Object o) {
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
        return new TemplateString(o.toString());
    }

    public TemplateModel next() {
        this.count++;
        this.isRewound = false;
        return getTemplateObject(this.f275i.next());
    }

    public boolean isRewound() {
        return this.isRewound;
    }

    public int size() {
        return this.col.size();
    }

    public void rewind() {
        this.f275i = getCollection().iterator();
        this.count = 0;
        this.isRewound = true;
        for (int n = 0; n < this.start; n++) {
            this.f275i.next();
        }
    }

    public TemplateModel get(String key) {
        if ("add".equals(key)) {
            return this.adder;
        }
        if ("addHash".equals(key)) {
            return this.hashAdder;
        }
        if ("addList".equals(key)) {
            return this.listAdder;
        }
        if ("size".equals(key)) {
            return new TemplateString(this.col.size());
        }
        if ("index".equals(key)) {
            return new TemplateString(this.count - 1);
        }
        if ("contains".equals(key)) {
            return this.containChecker;
        }
        if ("sortNatural".equals(key)) {
            return new SortNatural();
        }
        if ("sortReverse".equals(key)) {
            return new SortReverse();
        }
        if ("stop".equals(key)) {
            this.count = this.end;
            return this.containChecker;
        }
        return getElement(key);
    }

    protected TemplateModel getElement(String index) {
        if (index == null) {
            return null;
        }
        try {
            return get(Integer.parseInt(index));
        } catch (Exception e) {
            return null;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList$Adder.class */
    public class Adder implements TemplateMethodModel {
        Adder() {
            TemplateList.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            int countPair = 0;
            for (Object value : l) {
                TemplateList.this.add((TemplateModel) new TemplateString(value));
                countPair++;
                TemplateList.this.end++;
            }
            if (countPair == 0) {
                return null;
            }
            return new TemplateString(Integer.toString(countPair));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList$HashAdder.class */
    public class HashAdder implements TemplateMethodModel {
        HashAdder() {
            TemplateList.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            Iterator i = l.iterator();
            int countPair = 0;
            TemplateModel templateHash = new TemplateHash();
            while (i.hasNext()) {
                Object key = i.next();
                if (i.hasNext()) {
                    Object value = i.next();
                    templateHash.put(key.toString(), value == null ? "" : value);
                    countPair++;
                }
            }
            TemplateList.this.add(templateHash);
            return templateHash;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList$ListAdder.class */
    public class ListAdder implements TemplateMethodModel {
        ListAdder() {
            TemplateList.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            int countPair = 0;
            TemplateModel templateList = new TemplateList();
            for (Object key : l) {
                templateList.add(key);
                countPair++;
            }
            TemplateList.this.add(templateList);
            return templateList;
        }
    }

    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList$SortNatural.class */
    class SortNatural implements TemplateMethodModel {
        SortNatural() {
            TemplateList.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            SortedSet listSorted = new TreeSet(new TemplateHashComparator(l, false));
            listSorted.addAll(TemplateList.this.col);
            return new TemplateList(listSorted);
        }
    }

    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList$SortReverse.class */
    class SortReverse implements TemplateMethodModel {
        SortReverse() {
            TemplateList.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            SortedSet listSorted = new TreeSet(new TemplateHashComparator(l, true));
            listSorted.addAll(TemplateList.this.col);
            return new TemplateList(listSorted);
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateList$ContainChecker.class */
    public class ContainChecker implements TemplateMethodModel {
        ContainChecker() {
            TemplateList.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            boolean includes = false;
            for (Object obj : l) {
                String val = obj.toString();
                if (val != null) {
                    includes = false;
                    Iterator ci = TemplateList.this.col.iterator();
                    while (true) {
                        if (ci.hasNext()) {
                            if (val.equals(ci.next().toString())) {
                                includes = true;
                                break;
                            }
                        } else {
                            break;
                        }
                    }
                    if (!includes) {
                        break;
                    }
                }
            }
            return new TemplateString(includes ? "1" : "0");
        }
    }
}
