package psoft.util;

/* loaded from: hsphere.zip:psoft/util/NFUEntry.class */
public class NFUEntry implements Comparable {
    protected Object object;
    protected int myAge;
    protected Object key;

    public NFUEntry(Object key) {
        this.key = key;
    }

    public int getAge() {
        return this.myAge;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        NFUEntry ent = (NFUEntry) o;
        if (getAge() != ent.getAge()) {
            return getAge() - ent.getAge();
        }
        return getKey().toString().compareTo(ent.getKey().toString());
    }

    public boolean equals(Object o) {
        NFUEntry ent = (NFUEntry) o;
        if (getAge() != ent.getAge()) {
            return false;
        }
        return getKey().toString().equals(ent.getKey().toString());
    }

    public Object get() {
        this.myAge++;
        return this.object;
    }

    public Object getKey() {
        return this.key;
    }

    public void set(Object object) {
        this.myAge = 5;
        invalidate();
        this.object = object;
    }

    public void age() {
        this.myAge >>= 1;
        if (this.object != null && (this.object instanceof AgeableCacheEntry)) {
            ((AgeableCacheEntry) this.object).age();
        }
    }

    public boolean locked() {
        if (this.object != null && (this.object instanceof LockableCacheEntry)) {
            return ((LockableCacheEntry) this.object).locked();
        }
        return false;
    }

    public void reload() {
        if (this.object != null && (this.object instanceof ReloadableCacheEntry)) {
            ((ReloadableCacheEntry) this.object).reload();
        }
    }

    public void invalidate() {
        if (this.object != null && (this.object instanceof UpdatableCacheEntry)) {
            ((UpdatableCacheEntry) this.object).invalidate();
        }
    }

    protected void finalize() {
        invalidate();
    }

    public String toString() {
        return this.object.toString();
    }
}
