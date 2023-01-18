package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import java.util.Collection;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/SUResource.class */
public class SUResource extends Resource {
    public SUResource(int type, Collection init) throws Exception {
        super(type, init);
    }

    public SUResource(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_getPassword(String login) throws Exception {
        User u = User.getUser(login);
        if (u.getResellerId() != Session.getUser().getResellerId() && Session.getAccount().getId().findChild("reseller_su") == null) {
            Session.getLog().warn("BREAKIN attempt from user: " + Session.getUser().getLogin());
            return null;
        }
        return new TemplateString(u.getPassword());
    }
}
