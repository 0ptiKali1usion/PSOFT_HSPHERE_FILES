package psoft.hsphere.cache;

import java.io.Serializable;

/* loaded from: hsphere.zip:psoft/hsphere/cache/CacheKey.class */
public class CacheKey implements Comparable, Serializable {
    private Class objectClass;

    /* renamed from: id */
    long f78id;

    public CacheKey(long id, Class objectClass) {
        this.f78id = id;
        this.objectClass = objectClass;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        CacheKey k = (CacheKey) o;
        if (getClass().equals(k.getClass())) {
            return (int) (getId() - k.getId());
        }
        return getClass().getName().compareTo(k.getClass().getName());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof CacheKey) {
            CacheKey cacheKey = (CacheKey) o;
            if (this.f78id != cacheKey.f78id) {
                return false;
            }
            return this.objectClass != null ? this.objectClass.equals(cacheKey.objectClass) : cacheKey.objectClass == null;
        }
        return false;
    }

    public int hashCode() {
        return 0;
    }

    public long getId() {
        return this.f78id;
    }

    public Class getObjectClass() {
        return this.objectClass;
    }
}
