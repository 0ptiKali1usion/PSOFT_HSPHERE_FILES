package psoft.hsphere;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/ResourceTypeList.class */
public class ResourceTypeList {
    private List typeList = new ArrayList();

    public void addType(int typeId) {
        synchronized (this.typeList) {
            this.typeList.add(new Integer(typeId));
        }
    }

    public void delType(int typeId) {
        synchronized (this.typeList) {
            this.typeList.remove(new Integer(typeId));
        }
    }

    public String toString() {
        StringBuffer st = new StringBuffer();
        Iterator i = this.typeList.iterator();
        while (i.hasNext()) {
            st.append(((Integer) i.next()).toString()).append(i.hasNext() ? "," : "");
        }
        return st.toString();
    }

    public int size() {
        return this.typeList.size();
    }

    public Integer get(int index) {
        Integer num;
        synchronized (this.typeList) {
            num = (Integer) this.typeList.get(index);
        }
        return num;
    }

    public boolean contains(Integer value) {
        boolean contains;
        synchronized (this.typeList) {
            contains = this.typeList.contains(value);
        }
        return contains;
    }
}
