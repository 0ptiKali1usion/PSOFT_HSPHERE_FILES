package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ServiceDomain.class */
public class ServiceDomain extends Domain {
    public ServiceDomain(ResourceId id) throws Exception {
        super(id);
    }

    public ServiceDomain(int type, Collection values) throws Exception {
        super(type, values);
    }

    @Override // psoft.hsphere.resource.Domain, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("dns_zone".equals(key)) {
            try {
                ResourceId rid = FM_getChild("service_dns_zone");
                if (rid != null) {
                    return rid;
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting domain child ", e);
            }
        }
        return super.get(key);
    }
}
