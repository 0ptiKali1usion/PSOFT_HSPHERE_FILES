package psoft.hsphere.resource.apache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/PHPBBResource.class */
public class PHPBBResource extends psoft.hsphere.resource.PHPBBResource {
    public PHPBBResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public PHPBBResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.PHPBBResource
    protected void phpBBconf(String action, long serverId) throws Exception {
        HostEntry he = HostManager.getHost(serverId);
        List l = new ArrayList();
        l.add(getResourcePlanValue("BUNDLE_FILE_NAME"));
        l.add(String.valueOf(recursiveGet("path")));
        l.add(String.valueOf(recursiveGet("login")));
        l.add(String.valueOf(recursiveGet("group")));
        l.add(action);
        he.exec("phpbb-init", l, getConfig());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        phpBBconf("install", targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }
}
