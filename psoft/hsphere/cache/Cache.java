package psoft.hsphere.cache;

import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/hsphere/cache/Cache.class */
public interface Cache {
    CacheKey getCacheKey(long j);

    Class getCacheInterface();

    CacheableObject get(long j);

    CacheKey put(CacheableObject cacheableObject);

    void remove(CacheableObject cacheableObject);

    void setCache(NFUCache nFUCache);
}
