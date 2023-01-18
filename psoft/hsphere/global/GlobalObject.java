package psoft.hsphere.global;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.TypeRegistry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/global/GlobalObject.class */
public class GlobalObject implements TemplateHashModel {
    protected static List objectNames = new ArrayList();
    protected static Hashtable objHash = new Hashtable();
    protected String name;
    protected String label;
    protected String labelEnabled;
    protected String labelDisabled;
    protected String section;
    protected boolean isDisabledByDefault;
    protected boolean isConfigured;
    protected boolean isHsResource;
    protected boolean managedGlobally;

    private GlobalObject(String name, String label, String labelEnabled, String labelDisabled, String section, boolean isDisabledByDefault, boolean isConfigured, boolean isHsResource, boolean managedGlobally) {
        this.name = name;
        this.label = label;
        if (labelEnabled != null && !"".equals(labelEnabled) && labelDisabled != null && !"".equals(labelDisabled)) {
            this.labelEnabled = labelEnabled;
            this.labelDisabled = labelDisabled;
        } else {
            this.labelEnabled = null;
            this.labelDisabled = null;
        }
        this.section = section;
        this.isDisabledByDefault = isDisabledByDefault;
        this.isConfigured = isConfigured;
        this.isHsResource = isHsResource;
        this.managedGlobally = managedGlobally;
    }

    public static GlobalObject addObject(String name, String label, String labelEnabled, String labelDisabled, String section, boolean isDisabledByDefault, boolean isConfigured, boolean isHsResource, boolean managedGlobally) throws Exception {
        if (name == null || "".equals(name)) {
            throw new Exception("Field 'name' cannot be empty.");
        }
        GlobalObject gr = new GlobalObject(name, label, labelEnabled, labelDisabled, section, isDisabledByDefault, isConfigured, isHsResource, managedGlobally);
        objHash.put(name, gr);
        if (!objectNames.contains(name)) {
            objectNames.add(name);
        }
        return gr;
    }

    public String getName() {
        return this.name;
    }

    public String getSection() {
        return this.section;
    }

    public boolean isHsResource() {
        return this.isHsResource;
    }

    public boolean isDisabledByDefault() {
        return this.isDisabledByDefault;
    }

    public boolean isConfigured() {
        return this.isConfigured;
    }

    public boolean isManagedGlobally() {
        return this.managedGlobally;
    }

    public static GlobalObject getResource(String name) {
        return (GlobalObject) objHash.get(name);
    }

    public static List getObjectNames() {
        return objectNames;
    }

    public static boolean contains(String name) {
        return objectNames.contains(name);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("name")) {
            return new TemplateString(this.name);
        }
        if (key.equals("label")) {
            return new TemplateString(this.label);
        }
        if (key.equals("label_enabled")) {
            return new TemplateString(this.labelEnabled);
        }
        if (key.equals("label_disabled")) {
            return new TemplateString(this.labelDisabled);
        }
        if (key.equals("section")) {
            return new TemplateString(this.section);
        }
        if (key.equals("is_hs_resource")) {
            return new TemplateString(this.isHsResource ? "1" : "");
        } else if (key.equals("description")) {
            if (this.isHsResource) {
                try {
                    return new TemplateString(TypeRegistry.getDescription(TypeRegistry.getTypeId(this.name)));
                } catch (NoSuchTypeException e) {
                    return null;
                }
            }
            return null;
        } else if (!key.equals("show_as_radio") || this.labelEnabled == null) {
            return null;
        } else {
            return new TemplateString("1");
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String toString() {
        return this.name;
    }
}
