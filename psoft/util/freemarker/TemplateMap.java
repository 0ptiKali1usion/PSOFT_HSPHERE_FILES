package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplateMap.class */
public class TemplateMap implements TemplateHashModel, Serializable {
    protected Map hash;

    public TemplateMap() {
        this.hash = new HashMap();
    }

    public TemplateMap(Map hash) {
        this.hash = hash != null ? hash : new HashMap();
    }

    public TemplateMap(TemplateMap obj) {
        this.hash = obj.hash;
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
        if ("size".equals(key)) {
            return new TemplateString(this.hash.size());
        }
        Object value = this.hash.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof TemplateModel ? (TemplateModel) value : value instanceof List ? new TemplateList((List) value) : value instanceof Map ? new TemplateMap((Map) value) : new TemplateString(value);
    }

    public void put(String key, Object obj) {
        this.hash.put(key, obj == null ? "" : obj);
    }
}
