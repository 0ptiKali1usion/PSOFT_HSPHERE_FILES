package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import java.io.File;
import java.util.Collection;
import java.util.Date;
import org.apache.log4j.Category;
import psoft.encryption.MD5;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.apache.VirtualHostingResource;
import psoft.hsphere.resource.app.MySQLWrapper;
import psoft.util.freemarker.Template2String;

/* loaded from: hsphere.zip:psoft/hsphere/resource/AppResource.class */
public abstract class AppResource extends Resource implements HostDependentResource {
    private static Category log = Category.getInstance(AppResource.class.getName());
    private static final File TARBALL_ROOT = new File("/hsphere/shared/scripts/3rd_party");
    protected String tarball;

    protected abstract String _getPrefix();

    public AppResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public AppResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill." + _getPrefix() + ".setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + _getPrefix() + ".recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + _getPrefix() + ".refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill." + _getPrefix() + ".refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    private String getPath(String target) throws Exception {
        return noTrailingSlash(new File(((VirtualHostingResource) getParent().get()).getPath(), target).getAbsolutePath());
    }

    private String noTrailingSlash(String path) {
        if (path.charAt(path.length() - 1) == '/') {
            return path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String FM_getMD5(String value) {
        return new MD5(value).asHex();
    }

    public String FM_getPathNoSlash(String target) throws Exception {
        return getPath(target);
    }

    public String FM_getURLNoSlash(String target) throws Exception {
        return "http://" + noTrailingSlash(noTrailingSlash(noTrailingSlash(recursiveGet("name").getAsString()) + "/" + target));
    }

    public void postConfig(String templateName, String destination) throws Exception {
        postConfig(templateName, destination, getHostId());
    }

    public void postConfig(String templateName, String destination, long hostId) throws Exception {
        String config = Template2String.process(Session.getTemplate("app/" + _getPrefix() + "/" + templateName), (TemplateModel) this);
        HostEntry he = HostManager.getHost(hostId);
        he.exec("io2file", new String[]{getPath(destination)}, config);
    }

    public void processSQL(MySQLWrapper mysql, String templateName) throws Exception {
        if (HostEntry.getEmulationMode()) {
            Session.getLog().info("EMULATION MODE, postging MySQL bundle:");
        } else {
            mysql.batchSQL(Template2String.process(Session.getTemplate("app/" + _getPrefix() + "/" + templateName), (TemplateModel) this));
        }
    }

    private String getTarballName(String bundle) {
        return new File(TARBALL_ROOT, bundle).getAbsolutePath();
    }

    public void uncompress(String bundle, String target, long hostId) throws Exception {
        HostEntry he = HostManager.getHost(hostId);
        he.exec("uncompress", new String[]{getTarballName(bundle), getPath(target), recursiveGet("login").toString(), recursiveGet("group").toString()});
    }

    public void uncompress(String target) throws Exception {
        uncompress(this.tarball, target, getHostId());
    }

    public void removePath(String target) throws Exception {
        remove(target, getHostId());
    }

    private void remove(String target, long hostId) throws Exception {
        HostEntry he = HostManager.getHost(hostId);
        String[] strArr = new String[3];
        strArr[0] = getTarballName(this.tarball);
        strArr[1] = getPath(target);
        strArr[2] = target.equals("/") ? "1" : "";
        he.exec("removedir", strArr);
    }
}
