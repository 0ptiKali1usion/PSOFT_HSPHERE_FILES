package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/PHP3Resource.class */
public class PHP3Resource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".php";
    protected static final String DEFAULT_TYPE = "application/x-httpd-php";
    protected static final String DEFAULT_HANDLER = "/fcgi-bin/php";

    public PHP3Resource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        if (this.ext == null) {
            this.ext = DEFAULT_EXT;
        }
        if (this.mimeType == null) {
            this.mimeType = DEFAULT_TYPE;
        }
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource
    protected long getVHostId() throws Exception {
        return getParent().get().getParent().getId();
    }

    public PHP3Resource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.zeus.MimeTypeResource, psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        String nExt = normalizedExt();
        return "modules!map!handlers!" + nExt + "\t" + DEFAULT_HANDLER + "\nmodules!map!prestat!" + nExt + "\tno\n";
    }
}
