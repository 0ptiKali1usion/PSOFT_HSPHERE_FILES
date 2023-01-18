package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/SharedIPResource.class */
public class SharedIPResource extends IPResource {
    public SharedIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.IPResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("shared") ? new TemplateString("1") : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        getLog().info("Inside dedicated initDone");
        setIP(HostManager.getHost(recursiveGet("host_id")).getSharedIP());
        getLog().info("Got ip");
        super.initDone();
    }

    public SharedIPResource(ResourceId rId) throws Exception {
        super(rId);
        setIP(HostManager.getHost(recursiveGet("host_id")).getSharedIP());
    }
}
