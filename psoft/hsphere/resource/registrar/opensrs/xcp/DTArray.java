package psoft.hsphere.resource.registrar.opensrs.xcp;

import java.util.Collection;
import java.util.Iterator;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/xcp/DTArray.class */
public class DTArray extends Tag {
    int count;

    public static DTArray newInstance() {
        return new DTArray();
    }

    public DTArray() {
        super("dt_array");
    }

    @Override // psoft.hsphere.resource.registrar.opensrs.xcp.Tag
    public Tag add(Tag t) {
        super.add(new ItemTag(Integer.toString(this.count), t));
        this.count++;
        return this;
    }

    public Tag add(Collection l) {
        Iterator i = l.iterator();
        while (i.hasNext()) {
            add((String) i.next());
        }
        return this;
    }
}
