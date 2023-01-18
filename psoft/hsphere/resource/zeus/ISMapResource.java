package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/ISMapResource.class */
public class ISMapResource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".map";
    protected static final String DEFAULT_TYPE = "application/x-httpd-imap";

    public ISMapResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.mimeType = DEFAULT_TYPE;
    }

    public ISMapResource(ResourceId rId) throws Exception {
        super(rId);
    }
}
