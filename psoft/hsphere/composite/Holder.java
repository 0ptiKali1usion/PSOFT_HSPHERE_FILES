package psoft.hsphere.composite;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

/* loaded from: hsphere.zip:psoft/hsphere/composite/Holder.class */
public class Holder extends Item {
    private Hashtable childHolder;
    private static Hashtable allChildren = new Hashtable();

    public Holder(long id) {
        super(id);
        this.childHolder = null;
        this.childHolder = new Hashtable();
    }

    @Override // psoft.hsphere.composite.Item
    public synchronized Item addChild(Item child) throws Exception {
        this.childHolder.put(new Long(child.getId()), child);
        allChildren.put(new Long(child.getId()), child);
        child.setParent(this);
        return child;
    }

    @Override // psoft.hsphere.composite.Item
    public synchronized void delChild(Item child) throws Exception {
        child.setParent(null);
        this.childHolder.remove(new Long(child.getId()));
        allChildren.remove(new Long(child.getId()));
    }

    public static Item findChild(long id) {
        return (Item) allChildren.get(new Long(id));
    }

    public synchronized Hashtable getChildren() {
        Hashtable tmpChilds = new Hashtable();
        tmpChilds.putAll(this.childHolder);
        return tmpChilds;
    }

    public Collection getAllChildren() {
        ArrayList children = new ArrayList();
        for (Item it : getChildren().values()) {
            children.add(it);
            if (it instanceof Holder) {
                children.addAll(((Holder) it).getChildren().values());
            }
        }
        return children;
    }

    public boolean contains(Item item) {
        if (item != null) {
            for (Item curItem : getChildren().values()) {
                if (item.equals(curItem)) {
                    return true;
                }
                if ((curItem instanceof Holder) && ((Holder) curItem).contains(item)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
