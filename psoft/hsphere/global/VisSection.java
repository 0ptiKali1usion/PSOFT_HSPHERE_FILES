package psoft.hsphere.global;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/global/VisSection.class */
public class VisSection implements TemplateHashModel {
    public static final String SHOW_EVERYWHERE = "*";
    public static final String STORE_EVERYWHERE = "*";
    static List sections = new ArrayList();
    static Hashtable sectionHash = new Hashtable();

    /* renamed from: id */
    String f91id;
    String label;
    String description;
    String show;
    String store;
    String onlineHelp;
    List objects;
    List sets;

    private VisSection(String id) throws Exception {
        if (id == null || "".equals(id)) {
            throw new Exception("Field 'id' cannot be empty.");
        }
        this.f91id = id;
        this.objects = new ArrayList();
        this.sets = new ArrayList();
    }

    public static VisSection addSection(String id, String label, String description, String show, String store, String onlineHelp) throws Exception {
        VisSection vs = new VisSection(id);
        vs.label = label;
        vs.description = description;
        vs.show = show;
        vs.store = store;
        vs.onlineHelp = onlineHelp;
        if (!sectionHash.containsKey(id)) {
            sections.add(vs);
        }
        sectionHash.put(id, vs);
        return vs;
    }

    public String getId() {
        return this.f91id;
    }

    public String getLabel() {
        return this.label;
    }

    public String getDescription() {
        return this.description;
    }

    public String getShow() {
        return this.show;
    }

    public String getStore() {
        return this.store;
    }

    public List getObjects() {
        return this.objects;
    }

    public List getSets() {
        return this.sets;
    }

    public void addObject(GlobalObject gr) {
        this.objects.add(gr);
    }

    public void addSet(GlobalKeySet gr) {
        this.sets.add(gr);
    }

    public static VisSection getSection(String id) {
        return (VisSection) sectionHash.get(id);
    }

    public static List getSections() {
        return sections;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(this.f91id);
        }
        if (key.equals("label")) {
            return new TemplateString(this.label);
        }
        if (key.equals("description")) {
            return new TemplateString(this.description);
        }
        if (key.equals("show")) {
            return new TemplateString(this.show);
        }
        if (key.equals("online_help")) {
            return new TemplateString(this.onlineHelp);
        }
        if (key.equals("objects")) {
            return new TemplateList(this.objects);
        }
        if (key.equals("sets")) {
            return new TemplateList(this.sets);
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String toString() {
        return this.f91id;
    }
}
