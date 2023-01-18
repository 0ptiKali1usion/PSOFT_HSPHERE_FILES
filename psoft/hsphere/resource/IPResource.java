package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IPResource.class */
public abstract class IPResource extends Resource {

    /* renamed from: ip */
    private C0015IP f154ip;

    @Override // psoft.hsphere.Resource
    public String toString() {
        return this.f154ip == null ? super.toString() : this.f154ip.toString();
    }

    public void dumpIP(C0015IP ip) throws Exception {
        HostManager.getHost(recursiveGet("host_id")).dumpIP(ip);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("ip") ? new TemplateString(toString()) : super.get(key);
    }

    public IPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public IPResource(ResourceId rId) throws Exception {
        super(rId);
    }

    public void setIP(C0015IP ip) {
        this.f154ip = ip;
    }

    public C0015IP getIP() {
        return this.f154ip;
    }

    public String getDomainName() {
        try {
            TemplateModel name = recursiveGet("real_name");
            if (name != null) {
                return name.toString();
            }
            return null;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get the domain name for an IP resource", ex);
            return null;
        }
    }
}
