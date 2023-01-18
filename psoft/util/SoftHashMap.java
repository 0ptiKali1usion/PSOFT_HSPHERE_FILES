package psoft.util;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/* loaded from: hsphere.zip:psoft/util/SoftHashMap.class */
public class SoftHashMap extends AbstractMap {
    private final Map hash = new HashMap();
    private final ReferenceQueue queue = new ReferenceQueue();

    @Override // java.util.AbstractMap, java.util.Map
    public Object get(Object key) {
        Object result = null;
        SoftReference soft_ref = (SoftReference) this.hash.get(key);
        if (soft_ref != null) {
            result = soft_ref.get();
            if (result == null) {
                this.hash.remove(key);
            }
        }
        return result;
    }

    /* loaded from: hsphere.zip:psoft/util/SoftHashMap$SoftValue.class */
    public static class SoftValue extends SoftReference {
        private final Object key;

        private SoftValue(Object k, Object key, ReferenceQueue q) {
            super(k, q);
            this.key = key;
        }
    }

    private void processQueue() {
        while (true) {
            SoftValue sv = (SoftValue) this.queue.poll();
            if (sv != null) {
                this.hash.remove(sv.key);
            } else {
                return;
            }
        }
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Object put(Object key, Object value) {
        processQueue();
        return this.hash.put(key, new SoftValue(value, key, this.queue));
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Object remove(Object key) {
        processQueue();
        return this.hash.remove(key);
    }

    @Override // java.util.AbstractMap, java.util.Map
    public void clear() {
        processQueue();
        this.hash.clear();
    }

    @Override // java.util.AbstractMap, java.util.Map
    public int size() {
        processQueue();
        return this.hash.size();
    }

    @Override // java.util.AbstractMap, java.util.Map
    public Set entrySet() {
        return this.hash.entrySet();
    }
}
