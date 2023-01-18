package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.util.MissingResourceException;
import psoft.util.Config;

/* loaded from: hsphere.zip:psoft/util/freemarker/ConfigModel.class */
public class ConfigModel implements TemplateHashModel {
    protected String name;

    public boolean isEmpty() {
        return false;
    }

    public ConfigModel(String name) {
        this.name = name;
    }

    public TemplateModel get(String key) {
        String tmp = null;
        try {
            tmp = Config.getProperty(this.name, key);
        } catch (MissingResourceException e) {
        }
        if (tmp != null) {
            return new SimpleScalar(tmp);
        }
        return null;
    }
}
