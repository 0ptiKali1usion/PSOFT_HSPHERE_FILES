package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.text.NumberFormat;
import java.util.List;
import psoft.util.USFormat;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/LocaleMethod.class */
public class LocaleMethod implements TemplateMethodModel {
    public static final LocaleMethod currency = new LocaleMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        try {
            double tmpValue = USFormat.parseDouble((String) HTMLEncoder.decode(l).get(0));
            NumberFormat nf = NumberFormat.getCurrencyInstance();
            return new TemplateString(nf.format(tmpValue));
        } catch (Exception e) {
            return null;
        }
    }
}
