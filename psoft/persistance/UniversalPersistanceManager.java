package psoft.persistance;

import java.util.Hashtable;
import psoft.validators.Accessor;
import psoft.validators.HashAccessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/persistance/UniversalPersistanceManager.class */
public class UniversalPersistanceManager {
    protected Hashtable lookupTable = new Hashtable();

    public void register(Class c, PersistanceManager pm) {
        this.lookupTable.put(c, pm);
    }

    protected PersistanceManager lookup(Object obj) {
        return (PersistanceManager) this.lookupTable.get(obj.getClass());
    }

    public void insert(Object obj) {
        PersistanceManager pm = lookup(obj);
        if (pm != null) {
            pm.insert(obj);
        }
    }

    public void update(Object obj) {
        PersistanceManager pm = lookup(obj);
        if (pm != null) {
            pm.update(obj);
        }
    }

    public void delete(Object obj) {
        PersistanceManager pm = lookup(obj);
        if (pm != null) {
            pm.delete(obj);
        }
    }

    public Object copyOf(Object obj) {
        PersistanceManager pm = lookup(obj);
        if (pm != null) {
            return pm.copyOf(obj);
        }
        return null;
    }

    public int getNewId(Class c) {
        PersistanceManager pm = (PersistanceManager) this.lookupTable.get(c);
        if (pm != null) {
            return pm.getNewId();
        }
        return -1;
    }

    public long getNewIdAsLong(Class c) {
        PersistanceManager pm = (PersistanceManager) this.lookupTable.get(c);
        if (pm != null) {
            return pm.getNewIdAsLong();
        }
        return -1L;
    }

    public Object get(Object obj, Class c) {
        Hashtable h = new Hashtable();
        h.put("1", obj);
        return get((Accessor) new HashAccessor(h), NameModifier.none, c);
    }

    public Object get(Object obj1, Object obj2, Class c) {
        Hashtable h = new Hashtable();
        h.put("1", obj1);
        h.put("2", obj2);
        return get((Accessor) new HashAccessor(h), NameModifier.none, c);
    }

    public Object get(Object obj1, Object obj2, Object obj3, Class c) {
        Hashtable h = new Hashtable();
        h.put("1", obj1);
        h.put("2", obj2);
        h.put("3", obj3);
        return get((Accessor) new HashAccessor(h), NameModifier.none, c);
    }

    public Object get(Object obj1, Object obj2, Object obj3, Object obj4, Class c) {
        Hashtable h = new Hashtable();
        h.put("1", obj1);
        h.put("2", obj2);
        h.put("3", obj3);
        h.put("4", obj4);
        return get((Accessor) new HashAccessor(h), NameModifier.none, c);
    }

    public Object get(long id, Class c) {
        Hashtable h = new Hashtable();
        h.put("id", Long.toString(id));
        return get((Accessor) new HashAccessor(h), NameModifier.none, c);
    }

    public Object get(Accessor data, NameModifier nm, Class c) {
        PersistanceManager pm = (PersistanceManager) this.lookupTable.get(c);
        if (pm != null) {
            return pm.get(data, nm);
        }
        throw new PersistanceError("No PersistanceManager is registered for class: " + c.getName());
    }
}
