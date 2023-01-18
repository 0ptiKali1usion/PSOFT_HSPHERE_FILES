package psoft.hsphere.resource.IIS;

import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/PHPBBResource.class */
public class PHPBBResource extends psoft.hsphere.resource.PHPBBResource {
    public PHPBBResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public PHPBBResource(ResourceId rId) throws Exception {
        super(rId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.PHPBBResource
    protected void phpBBconf(String action, long serverId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(serverId);
        if ("install".equals(action)) {
            he.exec("phpbb-install.asp", (String[][]) new String[]{new String[]{"user-name", String.valueOf(recursiveGet("login"))}, new String[]{"hostnum", String.valueOf(recursiveGet("hostnum"))}, new String[]{"hostname", String.valueOf(recursiveGet("real_name"))}});
        } else if ("remove".equals(action)) {
            he.exec("phpbb-remove.asp", (String[][]) new String[]{new String[]{"user-name", String.valueOf(recursiveGet("login"))}, new String[]{"hostnum", String.valueOf(recursiveGet("hostnum"))}, new String[]{"hostname", String.valueOf(recursiveGet("real_name"))}});
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        phpBBconf("install", targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }
}
