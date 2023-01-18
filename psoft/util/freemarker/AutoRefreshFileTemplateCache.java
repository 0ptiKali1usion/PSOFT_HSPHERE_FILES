package psoft.util.freemarker;

import freemarker.template.CacheListener;
import freemarker.template.Template;
import freemarker.template.TemplateCache;
import freemarker.template.cache.Cacheable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import org.apache.log4j.Category;
import psoft.util.Config;
import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/util/freemarker/AutoRefreshFileTemplateCache.class */
public class AutoRefreshFileTemplateCache implements TemplateCache {
    private NFUCache cache;
    private TemplateEntry NULL_ENTRY;
    protected String base;
    protected boolean autoRefresh;
    protected String encoding;

    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    public String getEncoding() {
        return this.encoding;
    }

    public AutoRefreshFileTemplateCache(String base, int size) {
        this(base, size, 900000, true);
    }

    public AutoRefreshFileTemplateCache(String base, int size, int timeOut, boolean autoRefresh) {
        this.NULL_ENTRY = new TemplateEntry();
        this.base = base;
        this.autoRefresh = autoRefresh;
        this.cache = new NFUCache(size, timeOut);
    }

    public void setAutoRefresh(boolean autoRefresh) {
        this.autoRefresh = autoRefresh;
    }

    protected static Category getLog() {
        return Config.getLog("OTHER");
    }

    public Template getTemplate(String name) {
        try {
            getLog().info("looking up:" + this.base + name);
        } catch (NullPointerException e) {
        }
        try {
            TemplateEntry t = (TemplateEntry) this.cache.get(name);
            if (t == null || (t == this.NULL_ENTRY && this.autoRefresh)) {
                t = new TemplateEntry(name);
                this.cache.put(name, t);
            } else if (t == this.NULL_ENTRY) {
                return null;
            } else {
                if (this.autoRefresh && t.isNew()) {
                    t.reload();
                }
            }
            return t.getTemplate();
        } catch (IOException e2) {
            this.cache.put(name, this.NULL_ENTRY);
            return null;
        }
    }

    public void clear() {
        this.cache.clear();
    }

    public void addCacheListener(CacheListener listener) {
    }

    public void removeCacheListener(CacheListener listener) {
    }

    public void startAutoUpdate() {
        this.autoRefresh = true;
    }

    public void stopAutoUpdate() {
        this.autoRefresh = false;
    }

    public String getBase() {
        return this.base;
    }

    /* loaded from: hsphere.zip:psoft/util/freemarker/AutoRefreshFileTemplateCache$TemplateEntry.class */
    public class TemplateEntry {
        long modTime;
        String name;

        /* renamed from: t */
        Template f258t;

        TemplateEntry() {
            AutoRefreshFileTemplateCache.this = r4;
        }

        public TemplateEntry(String name) throws IOException {
            AutoRefreshFileTemplateCache.this = r4;
            this.name = name;
            reload();
        }

        long lastModified() {
            return this.modTime;
        }

        void reload() throws IOException {
            this.modTime = getTemplateLastModified();
            if (AutoRefreshFileTemplateCache.this.encoding == null) {
                this.f258t = new Template(AutoRefreshFileTemplateCache.this.base + this.name);
            } else {
                this.f258t = new Template(new InputStreamReader(new FileInputStream(AutoRefreshFileTemplateCache.this.base + this.name), AutoRefreshFileTemplateCache.this.encoding));
            }
            this.f258t.setCache(AutoRefreshFileTemplateCache.this);
        }

        private long getTemplateLastModified() {
            return new File(AutoRefreshFileTemplateCache.this.base + this.name).lastModified();
        }

        boolean isNew() {
            long lMod = getTemplateLastModified();
            boolean _isOld = lMod > 0 && lastModified() < lMod;
            return _isOld;
        }

        Template getTemplate() {
            return this.f258t;
        }
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
