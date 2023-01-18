package psoft.hsphere.resource.apache;

import java.util.Collection;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ISMapResource.class */
public class ISMapResource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".map";

    public ISMapResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        if (initValues.size() == 0) {
            this.ext = DEFAULT_EXT;
        }
    }

    public ISMapResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return "AddHandler imap-file " + this.ext + "\n";
    }
}
