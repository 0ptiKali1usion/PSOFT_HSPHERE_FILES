package psoft.user;

import java.util.Enumeration;
import psoft.validators.Accessor;

/* loaded from: hsphere.zip:psoft/user/AttributeAccessor.class */
public class AttributeAccessor implements Accessor {
    protected Attributable data;

    public AttributeAccessor(Attributable a) {
        this.data = a;
    }

    @Override // psoft.validators.Accessor
    public String get(String key) {
        Object temp = this.data.getAttrib(key);
        if (null == temp) {
            return null;
        }
        return temp.toString();
    }

    @Override // psoft.validators.Accessor
    public Object getObject(Object key) {
        if (key instanceof String) {
            return this.data.getAttrib((String) key);
        }
        return null;
    }

    @Override // psoft.validators.Accessor
    public Enumeration keys() {
        return this.data.listAttribs().elements();
    }
}
