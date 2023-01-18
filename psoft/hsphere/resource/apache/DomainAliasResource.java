package psoft.hsphere.resource.apache;

import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.DomainAlias;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/DomainAliasResource.class */
public class DomainAliasResource extends DomainAlias {
    public DomainAliasResource(int type, Collection values) throws Exception {
        super(type, values);
    }

    public DomainAliasResource(ResourceId rid) throws Exception {
        super(rid);
    }
}
