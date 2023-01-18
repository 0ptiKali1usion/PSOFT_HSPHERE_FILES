package psoft.hsphere.resource.app;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.ResourceId;
import psoft.hsphere.TemplateErrorResult;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/EasyAppCreator.class */
public class EasyAppCreator implements TemplateMethodModel {
    private static Category log = Category.getInstance(EasyAppCreator.class.getName());

    /* renamed from: r */
    private EasyAppResource f182r;

    public EasyAppCreator(EasyAppResource r) {
        this.f182r = r;
    }

    public TemplateModel exec(List list) {
        List list2 = HTMLEncoder.decode(list);
        int appType = Integer.parseInt((String) list2.get(0));
        String vhostStr = (String) list2.get(1);
        String path = (String) list2.get(2);
        try {
            if (!this.f182r.checkApp(path, vhostStr)) {
                return new TemplateErrorResult("This path is already in use");
            }
            ResourceId vhost = new ResourceId(vhostStr);
            EasyApp app = EasyApp.createApp(this.f182r, appType, vhost, path, list2.subList(3, list2.size()));
            this.f182r.addApp(app);
            return app;
        } catch (Exception e) {
            log.error("Error creating app: ", e);
            return new TemplateErrorResult("Error creating app: " + e.getMessage());
        }
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
