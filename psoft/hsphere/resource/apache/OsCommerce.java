package psoft.hsphere.resource.apache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/OsCommerce.class */
public class OsCommerce extends psoft.hsphere.resource.eshops.OsCommerce {
    public OsCommerce(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public OsCommerce(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        List l = new ArrayList();
        l.add(getResourcePlanValue("BUNDLE_FILE_NAME"));
        l.add(recursiveGet("path").toString());
        l.add(String.valueOf(recursiveGet("login")));
        l.add(String.valueOf(recursiveGet("group")));
        HostEntry he = HostManager.getHost(targetHostId);
        he.exec("oscommerce-init", l, getConfig());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }
}
