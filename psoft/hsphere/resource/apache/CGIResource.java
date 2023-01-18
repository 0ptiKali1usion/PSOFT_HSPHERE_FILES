package psoft.hsphere.resource.apache;

import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/CGIResource.class */
public class CGIResource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".cgi";

    public CGIResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        if (initValues.size() == 0) {
            this.ext = DEFAULT_EXT;
        }
    }

    public CGIResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return "AddHandler cgi-script " + this.ext + "\n";
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cgi.desc", new Object[]{this.ext, recursiveGet("real_name").toString()});
    }
}
