package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.AdmDNSZone;

/* loaded from: hsphere.zip:psoft/hsphere/resource/AbstThirdLevelDomainAlias.class */
public abstract class AbstThirdLevelDomainAlias extends DomainAlias {
    public AbstThirdLevelDomainAlias(int type, Collection values) throws Exception {
        super(type, values);
    }

    public AbstThirdLevelDomainAlias(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.resource.DomainAlias, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("dns_zone".equals(key)) {
            try {
                return FM_getChild("3l_dns_zone");
            } catch (Exception e) {
                Session.getLog().error("Error getting zone of 3rd level domain alias", e);
            }
        }
        if ("zone".equals(key)) {
            try {
                String zoneName = this.alias.substring(this.alias.indexOf(".") + 1);
                return AdmDNSZone.getByName(zoneName);
            } catch (Exception e2) {
                Session.getLog().error("Error find zone id ", e2);
            }
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.DomainAlias
    public int testAliasName(String name) throws Exception {
        return testAliasName(name, false);
    }
}
