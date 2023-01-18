package psoft.util.freemarker.acl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/* loaded from: hsphere.zip:psoft/util/freemarker/acl/FMACLWrapper.class */
public class FMACLWrapper implements TemplateHashModel {
    TemplateHashModel obj;
    FMACLManager acl;

    public FMACLWrapper(TemplateHashModel obj, FMACLManager acl) {
        this.obj = obj;
        this.acl = acl;
    }

    public TemplateModel get(String s) throws TemplateModelException {
        if (this.acl.testKey(this.obj.getClass(), s)) {
            return this.obj.get(s);
        }
        throw new TemplateModelException("The key is not allowed: " + s);
    }

    public boolean isEmpty() throws TemplateModelException {
        return this.obj.isEmpty();
    }
}
