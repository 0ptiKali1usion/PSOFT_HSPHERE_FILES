package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.io.Serializable;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateHash.class */
public class TemplateHash implements TemplateHashModel, Serializable {
    protected Hashtable hash;

    public TemplateHash() {
        this.hash = new Hashtable();
    }

    public TemplateHash(Hashtable hash) {
        if (hash != null) {
            this.hash = new Hashtable(hash);
        } else {
            this.hash = new Hashtable();
        }
    }

    public TemplateHash(TemplateHash tHash) {
        if (tHash != null) {
            this.hash = new Hashtable(tHash.hash);
        } else {
            this.hash = new Hashtable();
        }
    }

    public boolean isEmpty() {
        return this.hash.isEmpty();
    }

    public TemplateModel get(String key) {
        if (key == null) {
            return null;
        }
        if ("KEYS".equals(key)) {
            return new ListAdapter(this.hash.keySet());
        }
        if ("put".equals(key)) {
            return new Putter();
        }
        if ("remove".equals(key)) {
            return new Remover();
        }
        if ("size".equals(key)) {
            return new TemplateString(this.hash.size());
        }
        Object value = this.hash.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof TemplateModel) {
            return (TemplateModel) value;
        }
        return new TemplateString(value);
    }

    public void put(String key, Object obj) {
        this.hash.put(key, obj == null ? "" : obj);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateHash$Putter.class */
    public class Putter implements TemplateMethodModel {
        Putter() {
            TemplateHash.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            Iterator i = l.iterator();
            int pairCounter = 0;
            while (i.hasNext()) {
                Object key = i.next();
                if (i.hasNext()) {
                    Object value = i.next();
                    if (key != null) {
                        TemplateHash.this.hash.put(key, value == null ? "" : value);
                        pairCounter++;
                    }
                }
            }
            if (pairCounter == 0) {
                return null;
            }
            return new TemplateString(Integer.toString(pairCounter));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/TemplateHash$Remover.class */
    public class Remover implements TemplateMethodModel {
        Remover() {
            TemplateHash.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            int counter = 0;
            for (Object key : l) {
                if (TemplateHash.this.hash.remove(key) != null) {
                    counter++;
                }
            }
            if (counter == 0) {
                return null;
            }
            return new TemplateString(Integer.toString(counter));
        }
    }
}
