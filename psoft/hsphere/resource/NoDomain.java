package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/NoDomain.class */
public class NoDomain extends Domain {
    protected String domain;
    protected String prefix;

    public NoDomain(ResourceId id) throws Exception {
        super(id);
    }

    public NoDomain(int type, Collection values) throws Exception {
        super(type, values);
        getLog().info("Creating new No Domain");
        Iterator i = values.iterator();
        this.domain = (String) i.next();
        this.prefix = (String) i.next();
    }

    @Override // psoft.hsphere.resource.Domain, psoft.hsphere.Resource
    public void initDone() throws Exception {
        this.name = recursiveGet("login") + this.prefix + recursiveGet("host_id") + this.domain;
        this.name = this.name.toLowerCase();
        super.initDone();
    }

    @Override // psoft.hsphere.resource.Domain, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            try {
                Resource ip = FM_getChild("ip").get();
                if (ip != null && !"1".equals(ip.get("shared").toString())) {
                    return ip.get("ip");
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting domain child ip", e);
            }
        }
        return super.get(key);
    }
}
