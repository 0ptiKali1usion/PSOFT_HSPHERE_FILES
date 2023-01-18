package psoft.hsphere.resource.IIS;

import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.DomainAlias;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/DomainAliasResource.class */
public class DomainAliasResource extends DomainAlias implements HostDependentResource {
    public DomainAliasResource(int type, Collection values) throws Exception {
        super(type, values);
    }

    public DomainAliasResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        String hostnum = getParent().get().FM_getChild("hosting").get("hostnum").toString();
        he.exec("alias-create.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"alias", this.alias}});
        he.exec("alias-create.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"alias", "www." + this.alias}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = (VirtualHostingResource) getParent().get().FM_getChild("hosting").get();
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = getParent().get().FM_getChild("hosting").get("hostnum").toString();
        }
        he.exec("alias-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"alias", this.alias}});
        he.exec("alias-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"alias", "www." + this.alias}});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.DomainAlias, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.resource.DomainAlias, psoft.hsphere.Resource
    public void delete() throws Exception {
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        super.delete();
    }
}
