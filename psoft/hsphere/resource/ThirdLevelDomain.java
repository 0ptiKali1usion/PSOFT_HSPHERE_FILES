package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.AdmDNSZone;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ThirdLevelDomain.class */
public class ThirdLevelDomain extends Domain {
    public ThirdLevelDomain(ResourceId id) throws Exception {
        super(id);
    }

    public ThirdLevelDomain(int type, Collection values) throws Exception {
        super(type, values);
    }

    @Override // psoft.hsphere.resource.Domain, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("dns_zone".equals(key)) {
            try {
                ResourceId rid = FM_getChild("3l_dns_zone");
                if (rid != null) {
                    return rid;
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting domain child ", e);
            }
        }
        if ("zone".equals(key)) {
            try {
                String zoneName = this.name.substring(this.name.indexOf(".") + 1);
                return AdmDNSZone.getByName(zoneName);
            } catch (Exception e2) {
                Session.getLog().error("Error find zone id ", e2);
            }
        }
        return super.get(key);
    }
}
