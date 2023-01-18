package psoft.hsphere.resource.app;

import org.apache.axis.message.SOAPEnvelope;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IIS.VirtualHostingResource;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/WindowsHostAdapter.class */
public class WindowsHostAdapter extends EasyAppHostAdapter {
    private VirtualHostingResource virtualHosting;
    private String homeDirectory = null;

    public WindowsHostAdapter(VirtualHostingResource virtualHosting) throws Exception {
        this.virtualHosting = virtualHosting;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getHomeDirectory() throws Exception {
        if (this.homeDirectory == null) {
            WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
            SOAPEnvelope envelope = host.invokeMethod("easyappservice", "gethomedirectory", new String[]{new String[]{"username", getUserName()}, new String[]{"hostname", getHostName()}});
            this.homeDirectory = WinService.getChildElement(envelope, "homedirectory").getValue();
        }
        return this.homeDirectory;
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getTmpDir() throws Exception {
        return getHomeDirectory() + CommonUserCreator.MIGRATION_DIR;
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getDocumentRoot() throws Exception {
        return getHomeDirectory() + "/" + getHostName();
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getUserName() throws Exception {
        return this.virtualHosting.recursiveGet("login").toString();
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getUserGroup() throws Exception {
        return this.virtualHosting.recursiveGet("group").toString();
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getHostName() throws Exception {
        return this.virtualHosting.recursiveGet("real_name").toString();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void moveDirSafe(String from, String to) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
        host.invokeMethod("easyappservice", "movedirsafe", new String[]{new String[]{"sourcepath", from}, new String[]{"destpath", to}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void postConfig(String config, String configName, String subdir) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
        host.invokeMethod("easyappservice", "postconfig", new String[]{new String[]{"username", getUserName()}, new String[]{"hostname", getHostName()}, new String[]{"subdir", subdir}, new String[]{"config", config}, new String[]{"configfile", configName}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void postConfigSafe(String config, String safePath) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
        host.invokeMethod("easyappservice", "postconfigsafe", new String[]{new String[]{"config", config}, new String[]{"safepath", safePath}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void removePath(String bundle, String target, String subdir) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
        host.invokeMethod("easyappservice", "removepath", new String[]{new String[]{"username", getUserName()}, new String[]{"hostname", getHostName()}, new String[]{"target", target}, new String[]{"subdir", subdir}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void removePathSafe(String bundle, String safePath) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
        host.invokeMethod("easyappservice", "removepathsafe", new String[]{new String[]{"safepath", safePath}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r3v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void uncompress(String bundle, String target, String subdir) throws Exception {
        WinHostEntry host = (WinHostEntry) HostManager.getHost(this.virtualHosting.getHostId());
        host.invokeMethod("easyappservice", "uncompress", new String[]{new String[]{"username", getUserName()}, new String[]{"hostname", getHostName()}, new String[]{"subdir", subdir}, new String[]{"arcname", bundle}});
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public long getServerId() throws Exception {
        return this.virtualHosting.getHostId();
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getLocalDir() throws Exception {
        return getHostName();
    }
}
