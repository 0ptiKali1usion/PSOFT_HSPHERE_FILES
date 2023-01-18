package psoft.hsphere.resource.app;

import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.IIS.VirtualHostingResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/EasyAppHostAdapter.class */
public abstract class EasyAppHostAdapter {
    public abstract String getHomeDirectory() throws Exception;

    public abstract String getTmpDir() throws Exception;

    public abstract String getDocumentRoot() throws Exception;

    public abstract String getUserName() throws Exception;

    public abstract String getUserGroup() throws Exception;

    public abstract String getHostName() throws Exception;

    public abstract long getServerId() throws Exception;

    public abstract String getLocalDir() throws Exception;

    public abstract void moveDirSafe(String str, String str2) throws Exception;

    public abstract void postConfig(String str, String str2, String str3) throws Exception;

    public abstract void postConfigSafe(String str, String str2) throws Exception;

    public abstract void removePath(String str, String str2, String str3) throws Exception;

    public abstract void removePathSafe(String str, String str2) throws Exception;

    public abstract void uncompress(String str, String str2, String str3) throws Exception;

    public static EasyAppHostAdapter getHostAdapter(ResourceId vhost) throws Exception {
        HostDependentResource hdRes = (HostDependentResource) vhost.get();
        if (hdRes instanceof VirtualHostingResource) {
            return new WindowsHostAdapter((VirtualHostingResource) hdRes);
        }
        return new UnixHostAdapter((psoft.hsphere.resource.apache.VirtualHostingResource) hdRes);
    }
}
