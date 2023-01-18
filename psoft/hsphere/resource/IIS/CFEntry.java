package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/CFEntry.class */
public class CFEntry extends MimeTypeResource {
    public CFEntry(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public CFEntry(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "ext".equals(key) ? new TemplateString(this.ext) : super.get(key);
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cfentry.desc");
    }
}
