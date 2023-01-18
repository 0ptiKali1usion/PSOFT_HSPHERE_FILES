package psoft.hsphere.resource.IIS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.plan.InitValue;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.IPDependentResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/InstantAliasResource.class */
public class InstantAliasResource extends ServerAliasResource implements IPDependentResource, HostDependentResource {
    private String domain;
    private String prefix;

    public InstantAliasResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.domain = (String) i.next();
        this.prefix = (String) i.next();
    }

    public InstantAliasResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public void process() throws Exception {
        this.alias = "d" + getId().getId() + this.prefix + recursiveGet("host_id") + this.domain;
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource
    public String getAlias() throws Exception {
        return this.alias;
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("idomain_alias.desc", new Object[]{this.domain, this.alias});
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource, psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        updateAliasName(targetHostId);
        super.physicalCreate(targetHostId);
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (targetHostId == getHostId()) {
            super.physicalDelete(targetHostId);
        }
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource, psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.IIS.ServerAliasResource, psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    private void updateAliasName(long hostId) throws Exception {
        PreparedStatement ps = null;
        if (this.prefix == null || this.domain == null) {
            ResourceType rt = Session.getAccount().getPlan().getResourceType(30);
            List ivalues = rt.getInitModifier("").getInitValues();
            if (this.prefix == null) {
                this.prefix = ((InitValue) ivalues.get(1)).getValue();
            }
            if (this.domain == null) {
                this.domain = ((InitValue) ivalues.get(0)).getValue();
            }
        }
        String newAlias = "d" + getId().getId() + this.prefix + hostId + this.domain;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE vhost_alias SET alias = ? WHERE id = ?");
            ps.setString(1, newAlias);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.alias = newAlias;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        try {
            long targetHostId = getHostId();
            Session.getLog().debug("Instant Alias was restarted by IP");
            updateAliasName(targetHostId);
        } catch (Exception e) {
            Session.getLog().error("Error getting host id", e);
        }
    }
}
