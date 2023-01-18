package psoft.hsphere.composite;

/* loaded from: hsphere.zip:psoft/hsphere/composite/Item.class */
public class Item {

    /* renamed from: id */
    protected long f79id;
    Item parent = null;

    public Item(long id) {
        this.f79id = id;
    }

    public Item addChild(Item child) throws Exception {
        throw new Exception("Error adding child to a non composite object");
    }

    public void delChild(Item child) throws Exception {
        throw new Exception("Error deleting child from a non composite object");
    }

    public long getId() {
        return this.f79id;
    }

    public Item getParent() {
        return this.parent;
    }

    public void setParent(Item parent) {
        this.parent = parent;
    }

    public void delete() throws Exception {
        getParent().delChild(this);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof Item) {
            Item item = (Item) o;
            return this.f79id == item.f79id;
        }
        return false;
    }
}
