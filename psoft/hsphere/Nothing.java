package psoft.hsphere;

import java.util.Collection;
import java.util.Iterator;

/* loaded from: hsphere.zip:psoft/hsphere/Nothing.class */
public class Nothing extends Resource {
    public Nothing(ResourceId id) throws Exception {
        super(id);
    }

    public Nothing(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        while (i.hasNext()) {
            System.err.println("Param ->" + i.next());
        }
    }
}
