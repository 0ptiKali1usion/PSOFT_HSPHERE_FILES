package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/SSIResource.class */
public class SSIResource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".shtml";

    public SSIResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        if (initValues.size() == 0) {
            this.ext = DEFAULT_EXT;
        }
        this.mimeType = "text/x-server-parsed-html";
    }

    public SSIResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.zeus.MimeTypeResource, psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("ssi.desc", new Object[]{recursiveGet("real_name").toString(), this.ext});
    }
}
