package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/MimeTypeResource.class */
public class MimeTypeResource extends psoft.hsphere.resource.apache.MimeTypeResource {
    public MimeTypeResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public MimeTypeResource(ResourceId rId) throws Exception {
        super(rId);
    }

    public String normalizedExt() {
        return this.ext.substring(1);
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return "modules!mime!types!" + normalizedExt() + "\t" + this.mimeType + "\n";
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("mimetype.desc", new Object[]{recursiveGet("real_name").toString(), this.mimeType});
    }
}
