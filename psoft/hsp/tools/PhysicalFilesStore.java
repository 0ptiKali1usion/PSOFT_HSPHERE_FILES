package psoft.hsp.tools;

import java.util.Hashtable;
import java.util.Set;

/* loaded from: hsphere.zip:psoft/hsp/tools/PhysicalFilesStore.class */
public class PhysicalFilesStore {
    private Hashtable store = new Hashtable();

    public synchronized void put(PhysicalFileKey key, Object value) {
        this.store.put(key, value);
    }

    public synchronized Object get(PhysicalFileKey key) {
        for (PhysicalFileKey key1 : this.store.keySet()) {
            if (key.equals(key1)) {
                return this.store.get(key1);
            }
        }
        return null;
    }

    public Set keySet() {
        return this.store.keySet();
    }
}
