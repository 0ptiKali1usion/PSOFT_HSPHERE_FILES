package psoft.util;

import freemarker.template.Template;
import freemarker.template.TemplateCache;
import java.util.Hashtable;
import java.util.ResourceBundle;
import org.apache.log4j.Category;
import psoft.persistance.UniversalPersistanceManager;

/* loaded from: hsphere.zip:psoft/util/Config.class */
public class Config {
    protected static ThreadLocal resource = new ThreadLocal();

    protected static Hashtable get() {
        Hashtable ht = (Hashtable) resource.get();
        if (ht == null) {
            ht = new Hashtable();
            resource.set(ht);
        }
        return ht;
    }

    public static void set(Object key1, Hashtable ht) {
        get().put(key1, ht);
    }

    public static void set(Object key1, Object key, Object value) {
        get(key1).put(key, value);
    }

    public static Hashtable get(Object key) {
        Hashtable ht = (Hashtable) get().get(key);
        if (ht == null) {
            ht = new Hashtable();
            get().put(key, ht);
        }
        return ht;
    }

    public static String getProperty(Object key1, String key) {
        ResourceBundle config = (ResourceBundle) get(key1).get("config");
        if (config != null) {
            return config.getString(key).trim();
        }
        return null;
    }

    public static Template getTemplate(Object key1, String name) {
        TemplateCache tCache = (TemplateCache) get(key1).get(TemplateCache.class);
        return tCache.getTemplate(name);
    }

    public static UniversalPersistanceManager getUPM(Object key1) {
        return (UniversalPersistanceManager) get(key1).get(UniversalPersistanceManager.class);
    }

    public static Category getLog(Object key) {
        return (Category) get(key).get(Category.class);
    }
}
