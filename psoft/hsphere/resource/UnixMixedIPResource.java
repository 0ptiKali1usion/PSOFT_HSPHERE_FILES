package psoft.hsphere.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/UnixMixedIPResource.class */
public class UnixMixedIPResource extends MixedIPResource implements HostDependentResource {
    public UnixMixedIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.MixedIPResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (this.typeIp == 1) {
            addIP(getHostId());
        }
    }

    public UnixMixedIPResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.MixedIPResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        if (this.initialized && this.typeIp == 1) {
            getLog().info("Inside win IP alias deleting");
            HostEntry he = recursiveGet("host");
            List list = new ArrayList();
            list.add("del");
            list.add(getIP().getIP());
            he.exec("ip-update", list);
        }
        super.delete();
    }

    @Override // psoft.hsphere.resource.MixedIPResource
    public void addIP(long targetHostId) throws Exception {
        getLog().info("Inside Unix IP alias creating");
        List list = new ArrayList();
        list.add("add");
        list.add(getIP().getIP());
        list.add(getIP().getMask());
        HostEntry he = HostManager.getHost(targetHostId);
        he.exec("ip-update", list);
    }

    @Override // psoft.hsphere.resource.MixedIPResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        try {
            C0015IP oldIp = he.getIPbyRid(getId());
            getLog().info("Inside UnixMixedIPResource: Unbinding IP " + oldIp.toString() + " from host with ID " + targetHostId);
            he.releaseIP(oldIp);
            List list = new ArrayList();
            list.add("del");
            list.add(oldIp.getIP());
            he.exec("ip-update", list);
        } catch (NotFoundException e) {
        }
    }

    @Override // psoft.hsphere.Resource
    public boolean hasCMI() {
        return false;
    }
}
