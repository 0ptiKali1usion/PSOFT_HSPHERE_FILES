package psoft.persistance;

import psoft.validators.Accessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/persistance/PersistanceManager.class */
public abstract class PersistanceManager {

    /* renamed from: pm */
    protected UniversalPersistanceManager f246pm;

    public abstract void insert(Object obj);

    public abstract Object get(Accessor accessor, NameModifier nameModifier);

    public abstract void update(Object obj);

    public abstract void delete(Object obj);

    public abstract int getNewId();

    public PersistanceManager(UniversalPersistanceManager pm) {
        this.f246pm = pm;
    }

    public long getNewIdAsLong() {
        return getNewId();
    }

    public Object copyOf(Object obj) {
        throw new PersistanceError("Copy not implemented");
    }
}
