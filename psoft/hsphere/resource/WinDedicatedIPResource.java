package psoft.hsphere.resource;

import java.util.Collection;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/WinDedicatedIPResource.class */
public class WinDedicatedIPResource extends DedicatedIPResource {
    public WinDedicatedIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.DedicatedIPResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        getLog().info("Inside win IP alias creating");
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
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
                }
            }
        }
    }

    public WinDedicatedIPResource(ResourceId rId) throws Exception {
        super(rId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.DedicatedIPResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            getLog().info("Inside win IP alias deleting");
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            he.exec("ip-delete.asp", (String[][]) new String[]{new String[]{"ip", getIP().getIP()}});
        }
    }
}
