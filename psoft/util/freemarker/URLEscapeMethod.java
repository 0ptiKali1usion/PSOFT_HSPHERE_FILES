package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.HTMLEncoder;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/URLEscapeMethod.class */
public class URLEscapeMethod implements TemplateMethodModel {
    public static URLEscapeMethod escape = new URLEscapeMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        try {
            return new SimpleScalar(URLEncoder.encode(HTMLEncoder.decode((String) args.get(0))));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
