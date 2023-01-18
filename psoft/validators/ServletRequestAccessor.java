package psoft.validators;

import java.util.Enumeration;
import javax.servlet.ServletRequest;

/* loaded from: hsphere.zip:psoft/validators/ServletRequestAccessor.class */
public class ServletRequestAccessor implements Accessor {
    ServletRequest request;

    public ServletRequestAccessor(ServletRequest request) {
        this.request = request;
    }

    @Override // psoft.validators.Accessor
    public Object getObject(Object key) {
        return this.request.getParameterValues(key.toString());
    }

    @Override // psoft.validators.Accessor
    public String get(String name) {
        return this.request.getParameter(name);
    }

    @Override // psoft.validators.Accessor
    public Enumeration keys() {
        return this.request.getParameterNames();
    }
}
