package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IPDependentIterator.class */
public class IPDependentIterator implements Iterator {

    /* renamed from: l */
    protected List f150l = new ArrayList();

    /* renamed from: i */
    protected Iterator f151i;

    public IPDependentIterator(ResourceId rId) {
        Iterator i = new IPPrunedResourceIterator(rId);
        while (i.hasNext()) {
            try {
                Resource r = Resource.get((ResourceId) i.next());
                if (r instanceof IPDependentResource) {
                    this.f150l.add(r);
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting resource", e);
            }
        }
        this.f151i = this.f150l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f151i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f151i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f151i.remove();
    }
}
