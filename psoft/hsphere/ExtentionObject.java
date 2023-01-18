package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsphere/ExtentionObject.class */
public class ExtentionObject implements TemplateHashModel {
    private static Category log = Category.getInstance(ExtentionObject.class.getName());

    /* renamed from: r */
    Resource f34r;
    private Class[] argT = {Resource.class};

    public ExtentionObject(Resource r) {
        this.f34r = r;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        String className = this.f34r.getPlanValue(key);
        if (className == null) {
            return null;
        }
        Object[] argV = {this.f34r};
        try {
            return (TemplateModel) Class.forName(className).getConstructor(this.argT).newInstance(argV);
        } catch (Exception e) {
            log.warn(e);
            throw new TemplateModelException(e.getMessage());
        }
    }
}
