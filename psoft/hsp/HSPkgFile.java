package psoft.hsp;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Set;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsp/HSPkgFile.class */
public class HSPkgFile implements TemplateHashModel {

    /* renamed from: pf */
    private PackageFile f14pf;

    public HSPkgFile(PackageFile p) {
        this.f14pf = p;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(this.f14pf.getName());
        }
        if ("destPath".equals(key)) {
            return new TemplateString(this.f14pf.getPkfInfo().getDestinationPath());
        }
        if ("srvGroups".equals(key)) {
            return new TemplateList(getDestinationServerGroups());
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public Set getDestinationServerGroups() {
        return this.f14pf.getPkfInfo().getServerGroup();
    }
}
