package psoft.hsphere.cache;

import java.util.Date;
import psoft.util.LockableCacheEntry;

/* loaded from: hsphere.zip:psoft/hsphere/cache/LockedCacheObjectWrapper.class */
public class LockedCacheObjectWrapper implements LockableCacheEntry, CacheableObject {
    protected boolean isLocked = false;
    protected Date lockedTime;
    protected boolean isObjectLockableCacheEntry;
    public static final long TIMEOUT = 1200000;
    CacheableObject obj;

    public LockedCacheObjectWrapper(CacheableObject _obj) {
        this.isObjectLockableCacheEntry = false;
        this.obj = _obj;
        if (_obj instanceof LockableCacheEntry) {
            this.isObjectLockableCacheEntry = true;
        }
    }

    public void lock() {
        this.isLocked = true;
        this.lockedTime = new Date();
    }

    public void unlock() {
        this.isLocked = false;
    }

    public Date getLockedTime() {
        return this.lockedTime;
    }

    protected boolean isObjectLocked() {
        if (this.isObjectLockableCacheEntry) {
            return ((LockableCacheEntry) getObject()).locked();
        }
        return false;
    }

    @Override // psoft.util.LockableCacheEntry
    public boolean locked() {
        if (this.isLocked) {
            Date now = new Date();
            if (now.getTime() - getLockedTime().getTime() > 1200000) {
                this.isLocked = false;
            }
        }
        return this.isLocked || isObjectLocked();
    }

    public CacheableObject getObject() {
        return this.obj;
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.obj.getId();
    }
}
