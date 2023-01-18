package psoft.hsphere.resource.IIS;

import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/OsCommerce.class */
public class OsCommerce extends psoft.hsphere.resource.eshops.OsCommerce {
    public OsCommerce(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public OsCommerce(ResourceId rId) throws Exception {
        super(rId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("create", new String[]{new String[]{"resourcename", "oscommerce"}, new String[]{"conffile", getConfig()}, new String[]{"hostname", String.valueOf(recursiveGet("real_name"))}});
        } else {
            he.exec("set-oscommerce.asp", (String[][]) new String[]{new String[]{"user-name", String.valueOf(recursiveGet("login"))}, new String[]{"hostnum", String.valueOf(recursiveGet("hostnum"))}, new String[]{"hostname", String.valueOf(recursiveGet("real_name"))}, new String[]{"conffile", getConfig()}});
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "oscommerce"}, new String[]{"hostname", String.valueOf(recursiveGet("real_name"))}});
        }
    }
}
