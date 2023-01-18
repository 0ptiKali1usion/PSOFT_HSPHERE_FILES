package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;

/* loaded from: hsphere.zip:psoft/util/freemarker/TemplatePair.class */
public class TemplatePair implements TemplateHashModel {
    protected TemplateModel name;
    protected TemplateModel value;

    public TemplatePair(TemplateModel name, TemplateModel value) {
        this.name = name;
        this.value = value;
    }

    public TemplatePair(String name, String value) {
        this((TemplateModel) new TemplateString(name), (TemplateModel) new TemplateString(value));
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return "name".equals(key) ? this.name : this.value;
    }
}
