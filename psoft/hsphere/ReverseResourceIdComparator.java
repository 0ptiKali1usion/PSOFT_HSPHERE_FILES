package psoft.hsphere;

import java.util.Comparator;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsphere/ReverseResourceIdComparator.class */
public class ReverseResourceIdComparator implements Comparator {
    private static Category log = Category.getInstance(ResourceComparator.class.getName());

    @Override // java.util.Comparator
    public int compare(Object obj2, Object obj1) {
        ResourceId rid1 = (ResourceId) obj1;
        ResourceId rid2 = (ResourceId) obj2;
        int priority = TypeRegistry.getPriority(rid1.getType()) - TypeRegistry.getPriority(rid2.getType());
        return priority == 0 ? (int) (rid1.getId() - rid2.getId()) : priority;
    }

    public boolean equals(ResourceId rid1, ResourceId rid2) {
        return rid1.getId() == rid2.getId();
    }
}
