package psoft.hsphere.resource.dns;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.admin.AdmDNSZone;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/ServiceDNSZone.class */
public class ServiceDNSZone extends Resource {
    protected String name;
    protected AdmDNSZone aDNSZone;

    public ServiceDNSZone(ResourceId id) throws Exception {
        super(id);
        this.aDNSZone = AdmDNSZone.getByRid(getId());
        this.name = this.aDNSZone.getName();
    }

    public ServiceDNSZone(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.name = ((String) i.next()).toLowerCase().trim();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        this.aDNSZone = AdmDNSZone.getByName(this.name);
        if (this.aDNSZone != null) {
            this.aDNSZone.lock(getId());
        } else {
            notFound();
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            this.aDNSZone.unlock();
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return this.aDNSZone.get(key) != null ? this.aDNSZone.get(key) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("dns_zone.desc", new Object[]{this.name});
    }
}
