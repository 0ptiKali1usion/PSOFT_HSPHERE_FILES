package psoft.util.freemarker;

import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import java.util.List;
import psoft.web.utils.CSVEncoder;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/CSVEscapeMethod.class */
public class CSVEscapeMethod implements TemplateMethodModel {
    public static CSVEscapeMethod escape = new CSVEscapeMethod();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List args) {
        try {
            return new SimpleScalar(CSVEncoder.encode((String) HTMLEncoder.decode(args).get(0)));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
