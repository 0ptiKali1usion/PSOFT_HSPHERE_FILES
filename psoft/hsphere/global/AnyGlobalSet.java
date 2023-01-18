package psoft.hsphere.global;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/global/AnyGlobalSet.class */
public abstract class AnyGlobalSet implements GlobalKeySet {
    protected static List setNames = new ArrayList();
    protected static Hashtable setHash = new Hashtable();
    protected String name;
    protected String label;
    protected String section;
    protected boolean isDisabledByDefault;
    protected boolean isConfigured;
    protected boolean managedGlobally;

    @Override // psoft.hsphere.global.GlobalKeySet
    public abstract String getPrefix();

    @Override // psoft.hsphere.global.GlobalKeySet
    public abstract String getKeyDescription(String str) throws Exception;

    @Override // psoft.hsphere.global.GlobalKeySet
    public abstract List getAvailableKeys() throws Exception;

    protected AnyGlobalSet() {
        this.name = null;
        this.label = null;
        this.section = null;
        this.isDisabledByDefault = true;
        this.isConfigured = false;
        this.managedGlobally = true;
    }

    public AnyGlobalSet(String name, String label, String section, Boolean isDisabledByDefault, Boolean isConfigured, Boolean managedGlobally) throws Exception {
        this.name = name;
        this.label = label;
        this.section = section;
        this.isDisabledByDefault = isDisabledByDefault.booleanValue();
        this.isConfigured = isConfigured.booleanValue();
        this.managedGlobally = managedGlobally.booleanValue();
    }

    public static GlobalKeySet addSet(String name, String label, String section, boolean isDisabledByDefault, boolean isConfigured, String objClass, boolean managedGlobally) throws Exception {
        if (name == null || "".equals(name)) {
            throw new Exception("Field 'name' cannot be empty.");
        }
        Class c = Class.forName(objClass.toString());
        Class[] paramTypes = {String.class, String.class, String.class, Boolean.class, Boolean.class, Boolean.class};
        Constructor constr = c.getConstructor(paramTypes);
        Object[] paramValues = {name, label, section, new Boolean(isDisabledByDefault), new Boolean(isConfigured), new Boolean(managedGlobally)};
        GlobalKeySet gs = (GlobalKeySet) constr.newInstance(paramValues);
        setHash.put(name, gs);
        if (!setNames.contains(name)) {
            setNames.add(name);
        }
        return gs;
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public String getName() {
        return this.name;
    }

    public String getSection() {
        return this.section;
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public boolean isDisabledByDefault() {
        return this.isDisabledByDefault;
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public boolean isConfigured() {
        return this.isConfigured;
    }

    public boolean isManagedGlobally() {
        return this.managedGlobally;
    }

    public static GlobalKeySet getSet(String name) {
        return (GlobalKeySet) setHash.get(name);
    }

    public static List getSetNames() {
        return setNames;
    }

    public static List getAllSets() {
        return (List) setHash.values();
    }

    public static boolean contains(String name) {
        return setNames.contains(name);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("name")) {
            return new TemplateString(this.name);
        }
        if (key.equals("prefix")) {
            return new TemplateString(getPrefix());
        }
        if (key.equals("label")) {
            return new TemplateString(this.label);
        }
        if (key.equals("section")) {
            return new TemplateString(this.section);
        }
        if (key.equals("enabled_keys")) {
            try {
                return new TemplateList(getEnabledKeys());
            } catch (Exception ex) {
                Session.getLog().debug("Unable to get enabled keys for global set '" + getName() + "'", ex);
                return new TemplateList();
            }
        } else if (key.equals("available_keys")) {
            try {
                return new TemplateList(getAvailableKeys());
            } catch (Exception ex2) {
                Session.getLog().error("Unable to get available keys for global set '" + getName() + "' in the current configuration.", ex2);
                return new TemplateList();
            }
        } else {
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public TemplateModel FM_getKeyDescription(String key) throws Exception {
        String res = getKeyDescription(key);
        if (res != null) {
            return new TemplateString(res);
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public String toString() {
        return this.name;
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public int isKeyDisabled(String key) throws Exception {
        return isKeyDisabled(key, Session.getReseller());
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public int isKeyDisabled(String key, Reseller r) throws Exception {
        if (!this.isConfigured) {
            return 3;
        }
        if (!isKeyAvailable(key)) {
            return 2;
        }
        return GlobalValueProvider.keyStatus(getPrefix() + key, r, this.isDisabledByDefault, this.managedGlobally);
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public int isKeyDisabled(String key, int reselPlanId) throws Exception {
        if (!this.isConfigured) {
            return 3;
        }
        if (!isKeyAvailable(key)) {
            return 2;
        }
        return GlobalValueProvider.keyStatus(getPrefix() + key, reselPlanId, this.isDisabledByDefault);
    }

    public TemplateModel FM_isKeyDisabled(String key) throws Exception {
        int res = isKeyDisabled(key);
        return new TemplateString(res == 0 ? "" : String.valueOf(res));
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public final List getEnabledKeys() throws Exception {
        Reseller r = Session.getReseller();
        List result = new ArrayList();
        for (Object obj : getAvailableKeys()) {
            String key = obj.toString();
            if (isKeyDisabled(key, r) == 0) {
                result.add(key);
            }
        }
        return result;
    }
}
