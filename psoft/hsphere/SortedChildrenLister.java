package psoft.hsphere;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import psoft.util.freemarker.TemplateList;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/SortedChildrenLister.class */
public class SortedChildrenLister implements TemplateMethodModel {
    ResourceId rId;

    public SortedChildrenLister(ResourceId rId) {
        this.rId = rId;
    }

    public TemplateModel exec(List list) throws TemplateModelException {
        try {
            return new TemplateList(this.rId.getChildrenSorted(HTMLEncoder.decode(list)));
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
