package psoft.hsphere.resource.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.allocation.AllocatedPServer;
import psoft.hsphere.resource.allocation.AllocatedPServerHolder;
import psoft.hsphere.resource.allocation.AllocatedServerManager;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AllocatedServerResource.class */
public class AllocatedServerResource extends Resource {
    private AllocatedPServer aps;

    public AllocatedServerResource(ResourceId id) throws Exception {
        super(id);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id FROM allocated_pserver WHERE rid = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.aps = AllocatedPServerHolder.getInstance().get(rs.getLong("id"));
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public AllocatedServerResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public AllocatedPServer getAllocatedServer() {
        return this.aps;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        ResourceId resellerId = getParent().FM_getChild(FMACLManager.RESELLER);
        Reseller _r = Reseller.getReseller(resellerId);
        this.aps = AllocatedServerManager.getInstance().takeRandomAvailableAllocatedServer(_r.getId(), getId());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            AllocatedServerManager.getInstance().releaseTakenAllocatedPServer(this.aps.getId());
        }
    }
}
