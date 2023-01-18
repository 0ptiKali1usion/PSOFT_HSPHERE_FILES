package psoft.hsphere;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/* loaded from: hsphere.zip:psoft/hsphere/ThirdPartyIterator.class */
public class ThirdPartyIterator implements Iterator {

    /* renamed from: l */
    protected List f56l = new ArrayList();

    /* renamed from: i */
    protected Iterator f57i;

    public ThirdPartyIterator(ResourceId rId) {
        Iterator i = new ResourceIterator(rId);
        while (i.hasNext()) {
            try {
                Resource r = Resource.get((ResourceId) i.next());
                if (r instanceof ThirdPartyResource) {
                    this.f56l.add(r);
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting resource", e);
            }
        }
        this.f57i = this.f56l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f57i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f57i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f57i.remove();
    }
}
