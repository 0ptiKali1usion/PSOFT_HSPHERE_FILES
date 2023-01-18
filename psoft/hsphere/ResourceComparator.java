package psoft.hsphere;

import java.util.Comparator;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsphere/ResourceComparator.class */
public class ResourceComparator implements Comparator {
    private static Category log = Category.getInstance(ResourceComparator.class.getName());

    /* renamed from: c */
    Comparator f43c;

    public ResourceComparator(Comparator c) {
        this.f43c = c;
    }

    @Override // java.util.Comparator
    public int compare(Object o1, Object o2) {
        ResourceId r1 = (ResourceId) o1;
        ResourceId r2 = (ResourceId) o2;
        if (r1.equals(r2)) {
            return 0;
        }
        try {
            return this.f43c.compare(r1.get(), r2.get());
        } catch (Exception e) {
            log.warn("Error comparing values: " + e);
            return 0;
        }
    }
}
