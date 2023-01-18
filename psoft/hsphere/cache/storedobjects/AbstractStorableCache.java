package psoft.hsphere.cache.storedobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.cache.AbstractCache;
import psoft.hsphere.cache.CacheKey;
import psoft.hsphere.cache.CacheableObject;

/* loaded from: hsphere.zip:psoft/hsphere/cache/storedobjects/AbstractStorableCache.class */
public abstract class AbstractStorableCache extends AbstractCache implements StorableCache {
    @Override // psoft.hsphere.cache.storedobjects.StorableCache
    public void store(ObjectOutputStream os) throws IOException {
        List storedValues = new LinkedList();
        for (Object key : getCache().getKeys()) {
            if ((key instanceof CacheKey) && getCacheInterface().equals(((CacheKey) key).getObjectClass())) {
                CacheableObject so = get(((CacheKey) key).getId());
                if (so instanceof StoredObject) {
                    storedValues.add(so);
                }
            }
        }
        os.writeObject(storedValues);
    }

    @Override // psoft.hsphere.cache.storedobjects.StorableCache
    public void restore(ObjectInputStream is) throws IOException, ClassNotFoundException {
        List<CacheableObject> storedValues = (List) is.readObject();
        for (CacheableObject storedObject : storedValues) {
            put(storedObject);
        }
    }
}
