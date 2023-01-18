package psoft.hsphere.resource.mpf;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/MPFConfigurator.class */
public class MPFConfigurator implements TemplateHashModel {
    MPFManager manager = MPFManager.getManager();

    public void FM_setSettings(String url, String pdc, String ldap) throws Exception {
        this.manager.setLdapString(ldap);
        this.manager.setURL(url);
        this.manager.setPDC(pdc);
        this.manager.init();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return "ldap".equals(key) ? new TemplateString(this.manager.getLdapString()) : "pdc".equals(key) ? new TemplateString(this.manager.getPdc()) : "url".equals(key) ? new TemplateString(this.manager.getURL()) : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
