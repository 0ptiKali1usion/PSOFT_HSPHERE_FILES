package psoft.hsphere.report;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import psoft.hsphere.Session;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/TemplateCacheReport.class */
public class TemplateCacheReport implements TemplateMethodModel {
    public static TemplateCacheReport report = new TemplateCacheReport();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        List l2 = HTMLEncoder.decode(l);
        Session.getLog().info("--------->Innnn");
        Integer i = new Integer((String) l2.get(0));
        return Report.get(i);
    }
}
