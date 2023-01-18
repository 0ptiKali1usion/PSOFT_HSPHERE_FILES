package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/EndsWithMethod.class */
public class EndsWithMethod implements TemplateMethodModel {
    public static final EndsWithMethod start = new EndsWithMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        List args2 = HTMLEncoder.decode(args);
        String str = (String) args2.get(0);
        String param = (String) args2.get(1);
        return str.endsWith(param) ? TemplateConstants.TS_1 : TemplateConstants.TS_0;
    }
}
