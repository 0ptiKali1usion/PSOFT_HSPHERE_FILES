package psoft.hsphere.report;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import psoft.util.freemarker.TemplateList;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/EntrySet.class */
public class EntrySet implements TemplateMethodModel {
    protected List list;

    public EntrySet(List list) {
        this.list = list;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        List l2 = HTMLEncoder.decode(l);
        int start = Integer.parseInt((String) l2.get(0));
        int end = Integer.parseInt((String) l2.get(1));
        return new TemplateList(this.list.subList(start, end));
    }
}
