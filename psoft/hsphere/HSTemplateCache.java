package psoft.hsphere;

import freemarker.template.CacheListener;
import freemarker.template.Template;
import freemarker.template.TemplateCache;
import freemarker.template.cache.Cacheable;
import java.util.Iterator;
import java.util.ResourceBundle;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/HSTemplateCache.class */
public class HSTemplateCache implements TemplateCache {
    protected TemplateCache userCache;
    protected TemplateCache defaultCache;
    protected String defaultTemplateDir;

    public HSTemplateCache(ResourceBundle rb) {
        this.defaultTemplateDir = "";
        try {
            this.userCache = Toolbox.getTemplateCache(rb, "USER_TEMPLATE");
        } catch (Throwable th) {
        }
        this.defaultCache = Toolbox.getTemplateCache(rb);
        try {
            this.defaultTemplateDir = rb.getString("DEFAULT_TEMPLATES");
        } catch (Throwable th2) {
        }
    }

    public Template getTemplate(String name) {
        Template t = findTemplate(name);
        if (t != null) {
            t.setCache(this);
        }
        return t;
    }

    protected Template findTemplate(String name) {
        if (name == null || name.indexOf("..") != -1) {
            return null;
        }
        String replacementTemplateDir = Session.getReplacementTemplateDir();
        String designTemplateDir = Session.getDesignTemplateDir();
        if (designTemplateDir != null && this.defaultTemplateDir.equals(designTemplateDir)) {
            designTemplateDir = null;
        }
        if (replacementTemplateDir != null && !"".equals(replacementTemplateDir)) {
            if (designTemplateDir != null) {
                Template t = getTemplate(this.userCache, designTemplateDir + replacementTemplateDir, name);
                if (t != null) {
                    return t;
                }
                Template t2 = getTemplate(this.defaultCache, designTemplateDir + replacementTemplateDir, name);
                if (t2 != null) {
                    return t2;
                }
            }
            Template t3 = getTemplate(this.userCache, this.defaultTemplateDir + replacementTemplateDir, name);
            if (t3 != null) {
                return t3;
            }
            Template t4 = getTemplate(this.defaultCache, this.defaultTemplateDir + replacementTemplateDir, name);
            if (t4 != null) {
                return t4;
            }
        }
        if (designTemplateDir != null) {
            Template t5 = getTemplate(this.userCache, designTemplateDir, name);
            if (t5 != null) {
                return t5;
            }
            Template t6 = getTemplate(this.defaultCache, designTemplateDir, name);
            if (t6 != null) {
                return t6;
            }
        }
        Template t7 = getDefaultTemplate(this.userCache, name);
        return t7 != null ? t7 : getDefaultTemplate(this.defaultCache, name);
    }

    protected Template getTemplate(TemplateCache tCache, String dir, String name) {
        if (tCache == null) {
            return null;
        }
        return tCache.getTemplate(dir + name);
    }

    protected Template getDefaultTemplate(TemplateCache tCache, String name) {
        if (tCache == null) {
            return null;
        }
        return tCache.getTemplate(this.defaultTemplateDir + name);
    }

    public void addCacheListener(CacheListener cl) {
    }

    public void removeCacheListener(CacheListener cl) {
    }

    public void stopAutoUpdate() {
        if (this.userCache != null) {
            this.userCache.stopAutoUpdate();
        }
        this.defaultCache.stopAutoUpdate();
    }

    public CacheListener[] getCacheListeners() {
        return null;
    }

    public Cacheable getItem(String name) {
        return getTemplate(name);
    }

    public Cacheable getItem(String name, String type) {
        return getTemplate(name);
    }

    public Iterator listCachedFiles() {
        return null;
    }

    public void update() {
    }

    public void update(String name) {
    }

    public void update(String name, String tag) {
    }
}
