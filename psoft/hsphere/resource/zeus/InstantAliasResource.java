package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/InstantAliasResource.class */
public class InstantAliasResource extends psoft.hsphere.resource.apache.InstantAliasResource {
    public InstantAliasResource(int type, Collection values) throws Exception {
        super(type, values);
    }

    public InstantAliasResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.resource.apache.InstantAliasResource, psoft.hsphere.resource.apache.ServerAliasResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return this.alias;
    }

    @Override // psoft.hsphere.resource.apache.InstantAliasResource, psoft.hsphere.resource.apache.ServerAliasResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("idomain_alias.desc", new Object[]{get("domain"), this.alias});
    }
}
