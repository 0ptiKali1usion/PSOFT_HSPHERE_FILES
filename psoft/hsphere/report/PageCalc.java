package psoft.hsphere.report;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/PageCalc.class */
public class PageCalc implements TemplateMethodModel {
    protected int sign;
    protected List list;

    public PageCalc(List l, int sign) {
        this.sign = sign;
        this.list = l;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        List l2 = HTMLEncoder.decode(l);
        int start = Integer.parseInt((String) l2.get(0));
        int step = this.sign * Integer.parseInt((String) l2.get(1));
        Session.getLog().info("-----counting -->" + start + ":" + step);
        int result = start + step;
        if (result < 0) {
            result = 0;
        }
        if (result > this.list.size()) {
            result = this.list.size();
        }
        return new TemplateString(Integer.toString(result));
    }
}
