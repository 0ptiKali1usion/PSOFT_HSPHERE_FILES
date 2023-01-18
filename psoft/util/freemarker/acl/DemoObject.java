package psoft.util.freemarker.acl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/util/freemarker/acl/DemoObject.class */
public class DemoObject implements TemplateHashModel {
    public TemplateModel get(String s) throws TemplateModelException {
        if (FMACLManager.ADMIN.equals(s)) {
            return new TemplateString("ADMIN");
        }
        if (FMACLManager.RESELLER.equals(s)) {
            return new TemplateString("RESELLER");
        }
        if (FMACLManager.USER.equals(s)) {
            return new TemplateString("USER");
        }
        if ("admin_reseller".equals(s)) {
            return new TemplateString("ADMIN_RESELLER");
        }
        if ("admin_reseller_user".equals(s)) {
            return new TemplateString("ARU");
        }
        if (FMACLManager.EVERYONE.equals(s)) {
            return new TemplateString("EVERYONE");
        }
        if ("none".equals(s)) {
            return new TemplateString("NONE");
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
