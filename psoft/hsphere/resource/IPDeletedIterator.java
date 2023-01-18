package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IPDeletedIterator.class */
public class IPDeletedIterator implements Iterator {

    /* renamed from: l */
    protected List f148l = new ArrayList();

    /* renamed from: i */
    protected Iterator f149i;

    public IPDeletedIterator(ResourceId rId) {
        Iterator i = new IPPrunedResourceIterator(rId);
        while (i.hasNext()) {
            try {
                Resource r = Resource.get((ResourceId) i.next());
                if (r instanceof IPDeletedResource) {
                    this.f148l.add(r);
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting resource", e);
            }
        }
        this.f149i = this.f148l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f149i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f149i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f149i.remove();
    }
}
