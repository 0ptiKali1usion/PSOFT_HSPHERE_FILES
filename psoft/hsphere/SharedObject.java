package psoft.hsphere;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.util.NFUCache;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/SharedObject.class */
public class SharedObject {
    private static final int MAX_LOCKS = 100;
    protected static final TemplateString STATUS_OK;

    /* renamed from: id */
    protected long f51id;
    protected static NFUCache cache = null;
    private static Object[] locks = new Object[100];

    static {
        for (int i = 0; i < 100; i++) {
            locks[i] = new Object();
        }
        STATUS_OK = new TemplateString("OK");
    }

    public static void setCache(NFUCache newCache) {
        cache = newCache;
    }

    public static NFUCache getCache() {
        return cache;
    }

    public static Object getLock(long id) {
        return locks[(int) (id % 100)];
    }

    public long getId() {
        return this.f51id;
    }

    public SharedObject(long id) {
        this.f51id = id;
        cache.put(getKey(id, getClass()), this);
    }

    protected static String getKey(long id, Class c) {
        return id + "|" + c.getName();
    }

    public static SharedObject get(long id, Class c) {
        return (SharedObject) cache.get(getKey(id, c));
    }

    public static void remove(long id, Class c) {
        cache.remove(getKey(id, c));
    }

    public static void reloadAll() {
        cache.reload();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.f51id);
        }
        try {
            return "status".equals(key) ? STATUS_OK : AccessTemplateMethodWrapper.getMethod(this, key);
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }
}
