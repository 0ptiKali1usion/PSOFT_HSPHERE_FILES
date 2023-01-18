package psoft.validators;

import java.util.Enumeration;
import java.util.Vector;
import javax.servlet.http.Cookie;

/* loaded from: hsphere.zip:psoft/validators/CookieAccessor.class */
public class CookieAccessor implements Accessor {
    Cookie[] cookies;

    public CookieAccessor(Cookie[] cookies) {
        this.cookies = cookies;
    }

    @Override // psoft.validators.Accessor
    public Object getObject(Object key) {
        return get(key.toString());
    }

    @Override // psoft.validators.Accessor
    public String get(String name) {
        if (this.cookies == null) {
            return null;
        }
        for (int i = 0; i < this.cookies.length; i++) {
            if (this.cookies[i].getName().equals(name)) {
                return this.cookies[i].getValue();
            }
        }
        return null;
    }

    @Override // psoft.validators.Accessor
    public Enumeration keys() {
        Vector v = new Vector();
        for (int i = 0; i < this.cookies.length; i++) {
            v.add(this.cookies[i].getName());
        }
        return v.elements();
    }
}
