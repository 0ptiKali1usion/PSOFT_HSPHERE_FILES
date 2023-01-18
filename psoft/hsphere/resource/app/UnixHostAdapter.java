package psoft.hsphere.resource.app;

import java.io.File;
import psoft.hsphere.migrator.CommonUserCreator;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.apache.VirtualHostingResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/app/UnixHostAdapter.class */
public class UnixHostAdapter extends EasyAppHostAdapter {
    private static final File TARBALL_ROOT = new File("/hsphere/shared/scripts/3rd_party");
    private VirtualHostingResource virtualHosting;

    public UnixHostAdapter(VirtualHostingResource virtualHosting) throws Exception {
        this.virtualHosting = virtualHosting;
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getHomeDirectory() throws Exception {
        return this.virtualHosting.recursiveGet("dir").toString();
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getTmpDir() throws Exception {
        return CommonUserCreator.MIGRATION_DIR;
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getDocumentRoot() throws Exception {
        return this.virtualHosting.getPath();
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

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void moveDirSafe(String from, String to) throws Exception {
        HostEntry host = HostManager.getHost(this.virtualHosting.getHostId());
        host.exec("movedir", new String[]{from, to});
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void postConfig(String config, String configName, String subdir) throws Exception {
        HostEntry host = HostManager.getHost(this.virtualHosting.getHostId());
        String unixPath = new File(getDocumentRoot(), new File(subdir, configName).getAbsolutePath()).getAbsolutePath();
        host.exec("io2file", new String[]{unixPath}, config);
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void postConfigSafe(String config, String safePath) throws Exception {
        HostEntry host = HostManager.getHost(this.virtualHosting.getHostId());
        host.exec("io2file", new String[]{safePath}, config);
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void removePath(String bundle, String target, String subdir) throws Exception {
        HostEntry host = HostManager.getHost(this.virtualHosting.getHostId());
        String unixPath = new File(getDocumentRoot(), new File(subdir, target).getAbsolutePath()).getAbsolutePath();
        String[] strArr = new String[3];
        strArr[0] = bundle;
        strArr[1] = unixPath;
        strArr[2] = subdir.equals("/") ? "1" : "";
        host.exec("removedir", strArr);
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void removePathSafe(String bundle, String safePath) throws Exception {
        HostEntry host = HostManager.getHost(this.virtualHosting.getHostId());
        host.exec("removedir", new String[]{bundle, safePath});
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public void uncompress(String bundle, String target, String subdir) throws Exception {
        HostEntry host = HostManager.getHost(this.virtualHosting.getHostId());
        String unixPath = new File(getDocumentRoot(), new File(subdir, target).getAbsolutePath()).getAbsolutePath();
        host.exec("uncompress", new String[]{new File(TARBALL_ROOT, bundle).getAbsolutePath(), unixPath, getUserName(), getUserGroup()});
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public long getServerId() throws Exception {
        return this.virtualHosting.getHostId();
    }

    @Override // psoft.hsphere.resource.app.EasyAppHostAdapter
    public String getLocalDir() throws Exception {
        return this.virtualHosting.recursiveGet("local_dir").toString();
    }
}
