package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/VPSDependentResourceIterator.class */
public class VPSDependentResourceIterator implements Iterator {

    /* renamed from: l */
    protected List f163l = new ArrayList();

    /* renamed from: i */
    protected Iterator f164i;
    public static final List typeList = Arrays.asList(new Integer(4001), new Integer(7000), new Integer(7001), new Integer(7002), new Integer(7003), new Integer(7005), new Integer(7006), new Integer(7007), new Integer(7008));

    public VPSDependentResourceIterator(Collection rids) {
        Iterator iter = rids.iterator();
        while (iter.hasNext()) {
            ResourceId rid = (ResourceId) iter.next();
            if (typeList.contains(new Integer(rid.getType()))) {
                this.f163l.add(rid);
            }
        }
        this.f164i = this.f163l.iterator();
    }

    @Override // java.util.Iterator
    public boolean hasNext() {
        return this.f164i.hasNext();
    }

    @Override // java.util.Iterator
    public Object next() throws NoSuchElementException {
        return this.f164i.next();
    }

    @Override // java.util.Iterator
    public void remove() throws UnsupportedOperationException, IllegalStateException {
        this.f164i.remove();
    }

    public List getValues() {
        return this.f163l;
    }
}
