package psoft.validators;

import java.util.Enumeration;
import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/validators/HashAccessor.class */
public class HashAccessor implements Accessor {
    protected Hashtable hash;

    public HashAccessor(Hashtable hash) {
        this.hash = hash;
    }

    @Override // psoft.validators.Accessor
    public String get(String name) {
        return (String) this.hash.get(name);
    }

    @Override // psoft.validators.Accessor
    public Object getObject(Object key) {
        return this.hash.get(key);
    }

    @Override // psoft.validators.Accessor
    public Enumeration keys() {
        return this.hash.keys();
    }

    public String toString() {
        return this.hash.toString();
    }
}
