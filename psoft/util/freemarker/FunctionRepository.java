package psoft.util.freemarker;

import freemarker.template.TemplateModel;
import java.lang.reflect.Constructor;
import java.util.Hashtable;
import psoft.validators.Accessor;

/* loaded from: hsphere.zip:psoft/util/freemarker/FunctionRepository.class */
public class FunctionRepository {
    Hashtable functions = new Hashtable();

    /* renamed from: a */
    Accessor f260a;

    public FunctionRepository(Accessor a) {
        this.f260a = a;
    }

    public TemplateModel getMethod(String key) {
        return get(key, null, null);
    }

    public TemplateModel getMethod(String key, Class[] argt, Object[] argv) {
        return get(key, argt, argv);
    }

    public TemplateModel getMethod(String key, Object[] argv) {
        Class[] argt = new Class[argv.length];
        for (int i = 0; i < argv.length; i++) {
            argt[i] = argv[i].getClass();
        }
        return get(key, argt, argv);
    }

    protected TemplateModel get(String key, Class[] argt, Object[] argv) {
        String className = this.f260a.get(key);
        if (null == className) {
            return null;
        }
        TemplateModel tm = (TemplateModel) this.functions.get(className);
        if (null != tm) {
            return tm;
        }
        try {
            Constructor c = Class.forName(className).getConstructor(argt);
            return (TemplateModel) c.newInstance(argv);
        } catch (Exception e) {
            return new ExceptionPrinter(e, "Problem fetching TemplateModel :" + className + "<br>\n with argument types: " + argt + "<br>\n and arg values: " + argv + "<br>\n");
        }
    }
}
