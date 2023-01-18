package psoft.util.freemarker;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/ConsistsMethod.class */
public class ConsistsMethod implements TemplateMethodModel {
    public static final ConsistsMethod consists = new ConsistsMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        List args2 = HTMLEncoder.decode(args);
        String str = (String) args2.get(0);
        String param = (String) args2.get(1);
        if (str == null || param == null) {
            return TemplateConstants.TS_0;
        }
        if (str.indexOf(param) != -1) {
            return TemplateConstants.TS_1;
        }
        return TemplateConstants.TS_0;
    }
}
