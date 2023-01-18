package psoft.hsphere.report;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/PageList.class */
public class PageList implements TemplateMethodModel {
    protected List list;

    public PageList(List l) {
        this.list = l;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        List l2;
        int step = Integer.parseInt((String) HTMLEncoder.decode(l).get(0));
        TemplateList list = new TemplateList();
        Session.getLog().info("l.size--------->" + l2.size());
        Session.getLog().info("step--------->" + step);
        int i = 0;
        int j = 1;
        while (i < this.list.size()) {
            Session.getLog().info("i--------->" + i);
            TemplateMap map = new TemplateMap();
            map.put("start", new TemplateString(i));
            map.put("id", new TemplateString(j));
            list.add((TemplateModel) map);
            i += step;
            j++;
        }
        return list;
    }
}
