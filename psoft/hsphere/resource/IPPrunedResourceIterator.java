package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.ReverseResourceIdComparator;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IPPrunedResourceIterator.class */
public class IPPrunedResourceIterator implements Iterator {

    /* renamed from: l */
    protected List f152l;

    /* renamed from: i */
    protected Iterator f153i;

    protected void getChildren(ResourceId rId) {
        getChildren(rId, null);
    }

    protected void getChildren(ResourceId rId, List filter) {
        if (filter != null) {
            Integer rType = new Integer(rId.getType());
            if (filter.contains(rType)) {
                this.f152l.add(rId);
            }
        } else {
            this.f152l.add(rId);
        }
        try {
            Collection<ResourceId> ch = rId.getChildHolder().getChildrenByPriority();
            synchronized (ch) {
                for (ResourceId child : ch) {
                    if (!child.equals(rId) && !(child.get() instanceof IPResourcePrunerInterface)) {
                        getChildren(child, filter);
                    }
                }
            }
        } catch (Exception e) {
            Session.getLog().error("Error geting resource", e);
        }
    }

    public IPPrunedResourceIterator(Account a) {
        this(a.getId());
    }

    public IPPrunedResourceIterator(ResourceId rId) {
        this(rId, null);
    }

    public IPPrunedResourceIterator(ResourceId rId, List filter) {
        this.f152l = new ArrayList();
        getChildren(rId, filter);
        Object[] array = this.f152l.toArray();
        Arrays.sort(array, new ReverseResourceIdComparator());
        this.f152l = Arrays.asList(array);
        this.f153i = this.f152l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f153i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f153i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f153i.remove();
    }

    public void rewind() {
        this.f153i = this.f152l.iterator();
    }
}
