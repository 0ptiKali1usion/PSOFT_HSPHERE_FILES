package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.plan.InitValue;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.IPDependentResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/InstantAliasResource.class */
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
        super.process();
        this.alias = "d" + getId().getId() + this.prefix + recursiveGet("host_id") + this.domain;
    }

    @Override // psoft.hsphere.resource.apache.ServerAliasResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("alias".equals(key)) {
            try {
                Resource ip = recursiveGetResource("ip");
                if (ip != null && !"1".equals(ip.get("shared").toString())) {
                    return ip.get("ip");
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting domain child ip", e);
                return null;
            }
        }
        return "domain".equals(key) ? new TemplateString(this.domain) : super.get(key);
    }

    @Override // psoft.hsphere.resource.apache.ServerAliasResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return this.alias;
    }

    @Override // psoft.hsphere.resource.apache.ServerAliasResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("idomain_alias.desc", new Object[]{this.domain, this.alias});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        updateAliasName(targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        updateAliasName(targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    private void updateAliasName(long hostId) throws Exception {
        PreparedStatement ps = null;
        ResourceType rt = Session.getAccount().getPlan().getResourceType(30);
        List ivalues = rt.getInitModifier("").getInitValues();
        String newPref = ((InitValue) ivalues.get(1)).getValue();
        String newDomain = ((InitValue) ivalues.get(0)).getValue();
        String newAlias = "d" + getId().getId() + newPref + hostId + newDomain;
        Session.getLog().debug("Inside InstantAlias::updateAliasName() newAlias=" + newAlias);
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
