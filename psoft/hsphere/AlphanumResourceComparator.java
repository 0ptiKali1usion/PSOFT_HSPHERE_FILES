package psoft.hsphere;

import java.util.Comparator;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsphere/AlphanumResourceComparator.class */
public class AlphanumResourceComparator implements Comparator {
    private static Category log = Category.getInstance(AlphanumResourceComparator.class.getName());
    public static final AlphanumResourceComparator ASCENDING = new AlphanumResourceComparator();

    @Override // java.util.Comparator
    public int compare(Object o1, Object o2) {
        try {
            return compareResources(((ResourceId) o1).get(), ((ResourceId) o2).get());
        } catch (Exception e) {
            log.warn("Error in comparison", e);
            return 0;
        }
    }

    private int compareResources(Resource o1, Resource o2) throws Exception {
        if (o1 instanceof ComparableResource) {
            if (o2 instanceof ComparableResource) {
                ComparableResource c1 = (ComparableResource) o1;
                ComparableResource c2 = (ComparableResource) o2;
                return c1.comparableString().compareTo(c2.comparableString());
            }
            return 1;
        } else if (o2 instanceof ComparableResource) {
            return -1;
        } else {
            return 0;
        }
    }
}
