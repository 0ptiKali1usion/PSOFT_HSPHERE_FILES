package psoft.hsphere.cache;

import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/hsphere/cache/AbstractCache.class */
public abstract class AbstractCache implements Cache, LockableCache {
    private NFUCache cache;

    public AbstractCache() {
    }

    public AbstractCache(NFUCache _cache) {
        setCache(_cache);
    }

    @Override // psoft.hsphere.cache.Cache
    public CacheKey getCacheKey(long id) {
        return new CacheKey(id, getCacheInterface());
    }

    @Override // psoft.hsphere.cache.Cache
    public CacheKey put(CacheableObject o) {
        CacheKey key = getCacheKey(o.getId());
        LockedCacheObjectWrapper lo = new LockedCacheObjectWrapper(o);
        getCache().put(key, lo);
        return key;
    }

    @Override // psoft.hsphere.cache.Cache
    public CacheableObject get(long id) {
        LockedCacheObjectWrapper lo = getLockedValue(id);
        if (lo != null) {
            return lo.getObject();
        }
        return null;
    }

    protected LockedCacheObjectWrapper getLockedValue(long id) {
        CacheKey key = getCacheKey(id);
        return (LockedCacheObjectWrapper) getCache().get(key);
    }

    public NFUCache getCache() {
        return this.cache;
    }

    @Override // psoft.hsphere.cache.Cache
    public void setCache(NFUCache cache) {
        this.cache = cache;
    }

    @Override // psoft.hsphere.cache.Cache
    public void remove(CacheableObject o) {
        CacheKey key = getCacheKey(o.getId());
        getCache().remove(key);
    }

    @Override // psoft.hsphere.cache.LockableCache
    public CacheableObject getLockedObject(long id) {
        CacheableObject obj = get(id);
        if (obj != null) {
            lock(id);
        }
        return obj;
    }

    @Override // psoft.hsphere.cache.LockableCache
    public void lock(long id) {
        LockedCacheObjectWrapper lo = getLockedValue(id);
        if (lo != null) {
            lo.lock();
        }
    }

    @Override // psoft.hsphere.cache.LockableCache
    public void unlock(long id) {
        LockedCacheObjectWrapper lo = getLockedValue(id);
        if (lo != null) {
            lo.unlock();
        }
    }
}
