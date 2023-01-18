package psoft.hsphere.functions;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.Iterator;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/functions/Echo.class */
public class Echo implements TemplateMethodModel {
    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List list) {
        Iterator i = HTMLEncoder.decode(list).iterator();
        String str = "";
        while (true) {
            String s = str;
            if (i.hasNext()) {
                str = s + " " + i.next();
            } else {
                return new SimpleScalar(s);
            }
        }
    }
}
