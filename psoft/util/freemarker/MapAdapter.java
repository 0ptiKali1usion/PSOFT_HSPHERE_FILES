package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/util/freemarker/MapAdapter.class */
public class MapAdapter implements TemplateHashModel {
    private static final long serialVersionUID = 1000;
    protected Map map;

    public MapAdapter(Map map) {
        this.map = map;
    }

    public TemplateModel get(String key) {
        if (key == null || this.map == null) {
            return null;
        }
        if ("KEYS".equals(key)) {
            return new ListAdapter(this.map.keySet());
        }
        Object obj = this.map.get(key);
        if (obj == null) {
            return null;
        }
        if (obj instanceof TemplateModel) {
            return (TemplateModel) obj;
        }
        if (obj instanceof Map) {
            return new MapAdapter((Map) obj);
        }
        if (obj instanceof Collection) {
            return new ListAdapter((Collection) obj);
        }
        return new TemplateString(obj);
    }

    public boolean isEmpty() {
        if (this.map != null) {
            return this.map.isEmpty();
        }
        return true;
    }
}
