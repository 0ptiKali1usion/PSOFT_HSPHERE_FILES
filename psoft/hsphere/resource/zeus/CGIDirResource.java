package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/CGIDirResource.class */
public class CGIDirResource extends psoft.hsphere.resource.apache.CGIDirResource {
    public CGIDirResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public CGIDirResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.apache.CGIDirResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return "modules!map!alias!" + this.dir + "!filepath " + getPath() + "\nmodules!map!alias!" + this.dir + "!type      cgi\n";
    }

    @Override // psoft.hsphere.resource.apache.CGIDirResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cgidir.desc", new Object[]{this.dir, recursiveGet("real_name").toString()});
    }
}
