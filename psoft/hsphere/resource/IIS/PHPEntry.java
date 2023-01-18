package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/PHPEntry.class */
public class PHPEntry extends MimeTypeResource implements HostDependentResource {
    public PHPEntry(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public PHPEntry(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        he.exec("php-addextension.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        he.exec("php-delextension.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "ext".equals(key) ? new TemplateString(this.ext) : super.get(key);
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("php3entry.desc", new Object[]{this.ext, recursiveGet("real_name").toString()});
    }
}
