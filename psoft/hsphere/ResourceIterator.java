package psoft.hsphere;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/* loaded from: hsphere.zip:psoft/hsphere/ResourceIterator.class */
public class ResourceIterator implements Iterator {

    /* renamed from: l */
    protected List f46l = Collections.synchronizedList(new ArrayList());

    /* renamed from: i */
    protected Iterator f47i;

    protected void getChildren(ResourceId rId) {
        this.f46l.add(rId);
        try {
            Collection<ResourceId> ch = rId.getChildHolder().getChildren();
            synchronized (ch) {
                for (ResourceId child : ch) {
                    if (!child.equals(rId)) {
                        getChildren(child);
                    }
                }
            }
        } catch (Exception e) {
            Session.getLog().error("Error geting resource", e);
        }
    }

    public ResourceIterator(Account a) {
        getChildren(a.getId());
        this.f47i = this.f46l.iterator();
    }

    public ResourceIterator(ResourceId rId) {
        getChildren(rId);
        this.f47i = this.f46l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f47i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f47i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f47i.remove();
    }
}
