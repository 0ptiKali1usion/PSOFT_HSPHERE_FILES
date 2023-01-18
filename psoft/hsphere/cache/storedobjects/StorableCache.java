package psoft.hsphere.cache.storedobjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/* loaded from: hsphere.zip:psoft/hsphere/cache/storedobjects/StorableCache.class */
public interface StorableCache {
    void store(ObjectOutputStream objectOutputStream) throws IOException;

    void restore(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException;
}
