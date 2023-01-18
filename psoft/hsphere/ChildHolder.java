package psoft.hsphere;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/ChildHolder.class */
public class ChildHolder {
    protected Hashtable children = new Hashtable();
    protected List all = Collections.synchronizedList(new ArrayList());

    /* renamed from: id */
    protected ResourceId f30id;

    public ChildHolder(ResourceId id) {
        this.f30id = id;
    }

    public synchronized void deleteChild(ResourceId child) {
        this.all.remove(child);
        String type = Integer.toString(child.getType());
        List l = (List) this.children.get(type);
        if (l != null) {
            l.remove(child);
        }
    }

    public synchronized void addChild(ResourceId child) {
        String type = Integer.toString(child.getType());
        List l = (List) this.children.get(type);
        if (l == null) {
            l = Collections.synchronizedList(new ArrayList());
            this.children.put(type, l);
        }
        l.add(child);
        this.all.add(child);
    }

    public Collection getChildren() {
        return this.all;
    }

    public Collection getChildrenByPriority() {
        Object[] array = this.all.toArray();
        Arrays.sort(array, new ResourceIdComparator());
        return Arrays.asList(array);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/ChildHolder$ResourceIdComparator.class */
    public class ResourceIdComparator implements Comparator {
        ResourceIdComparator() {
            ChildHolder.this = r4;
        }

        @Override // java.util.Comparator
        public int compare(Object obj1, Object obj2) {
            ResourceId rid1 = (ResourceId) obj1;
            ResourceId rid2 = (ResourceId) obj2;
            int priority = TypeRegistry.getPriority(rid1.getType()) - TypeRegistry.getPriority(rid2.getType());
            return priority == 0 ? (int) (rid1.getId() - rid2.getId()) : priority;
        }

        public boolean equals(ResourceId rid1, ResourceId rid2) {
            return rid1.getId() == rid2.getId();
        }
    }

    public Collection getChildren(String type) {
        Collection c = (Collection) this.children.get(type);
        return null != c ? c : Collections.synchronizedList(new ArrayList());
    }

    public Collection getChildrenByName(String type) {
        try {
            return getChildren(TypeRegistry.getTypeId(type));
        } catch (Exception e) {
            Session.getLog().warn("Invalid resource type " + type, e);
            return Collections.synchronizedList(new ArrayList());
        }
    }
}
