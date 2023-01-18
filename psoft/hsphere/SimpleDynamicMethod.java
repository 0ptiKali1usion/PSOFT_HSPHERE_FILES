package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.util.Config;
import psoft.util.freemarker.FunctionRepository;

/* loaded from: hsphere.zip:psoft/hsphere/SimpleDynamicMethod.class */
public class SimpleDynamicMethod implements TemplateHashModel {
    String prefix;

    /* renamed from: fr */
    FunctionRepository f52fr = (FunctionRepository) Config.get("CLIENT").get("functions");

    public SimpleDynamicMethod(String prefix) {
        this.prefix = prefix;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        System.err.println("SimpleDynamicMethod.get(" + this.prefix + "." + key + ")");
        if (this.f52fr == null) {
            return null;
        }
        return this.f52fr.getMethod(this.prefix + "." + key);
    }

    public boolean isEmpty() {
        return false;
    }
}
