package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/HTMLEncodeMethod.class */
public class HTMLEncodeMethod implements TemplateMethodModel {
    public static HTMLEncodeMethod encode = new HTMLEncodeMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        try {
            return new SimpleScalar(HTMLEncoder.encode((String) args.get(0)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
