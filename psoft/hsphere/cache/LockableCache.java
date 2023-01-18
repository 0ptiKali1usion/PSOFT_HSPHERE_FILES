package psoft.hsphere.cache;

/* loaded from: hsphere.zip:psoft/hsphere/cache/LockableCache.class */
public interface LockableCache {
    CacheableObject getLockedObject(long j);

    void lock(long j);

    void unlock(long j);
}
