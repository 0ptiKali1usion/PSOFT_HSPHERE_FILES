package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/HostDependentResourceIterator.class */
public class HostDependentResourceIterator implements Iterator {

    /* renamed from: l */
    protected List f143l = new ArrayList();

    /* renamed from: i */
    protected Iterator f144i;

    public HostDependentResourceIterator(List rids, List typeList) {
        Iterator iter = rids.iterator();
        while (iter.hasNext()) {
            ResourceId rid = (ResourceId) iter.next();
            if (typeList.contains(new Integer(rid.getType()))) {
                this.f143l.add(rid);
            }
        }
        this.f144i = this.f143l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f144i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f144i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f144i.remove();
    }

    public List getValues() {
        return this.f143l;
    }
}
