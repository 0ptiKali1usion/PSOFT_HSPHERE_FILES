package psoft.hsphere.resource;

import java.util.Collection;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinMixedIPResource.class */
public class WinMixedIPResource extends MixedIPResource {
    public WinMixedIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.MixedIPResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (this.typeIp == 1) {
            addIP(getHostId());
        }
    }

    public WinMixedIPResource(ResourceId rId) throws Exception {
        super(rId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.MixedIPResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        if (this.initialized && this.typeIp == 1) {
            getLog().info("Inside win IP alias deleting");
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            he.exec("ip-delete.asp", (String[][]) new String[]{new String[]{"ip", getIP().getIP()}});
        }
        super.delete();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v4, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.MixedIPResource
    public void addIP(long targetHostId) throws Exception {
        getLog().info("Inside win IP alias creating");
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        boolean cycle = true;
        while (cycle) {
            C0015IP ip = getIP();
            try {
                cycle = false;
                he.exec("ip-create.asp", (String[][]) new String[]{new String[]{"ip", ip.getIP()}, new String[]{"mask", getIP().getMask()}});
            } catch (Exception e) {
                if (e.getMessage().indexOf("IP address already exist") != -1) {
                    cycle = true;
                    dumpIP(ip);
                    setIP(HostManager.getHost(recursiveGet("host_id")).getExclusiveIP(getId()));
                } else {
                    throw e;
                }
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v7, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.MixedIPResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        try {
            C0015IP oldIp = he.getIPbyRid(getId());
            getLog().info("Inside WinMixedIPResource: Unbinding IP " + oldIp.toString() + " from host with ID " + targetHostId);
            he.releaseIP(oldIp);
            he.exec("ip-delete.asp", (String[][]) new String[]{new String[]{"ip", oldIp.getIP()}});
        } catch (NotFoundException e) {
        }
    }
}
