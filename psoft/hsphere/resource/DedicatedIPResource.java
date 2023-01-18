package psoft.hsphere.resource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Hashtable;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/DedicatedIPResource.class */
public class DedicatedIPResource extends IPResource {
    public DedicatedIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        getLog().info("Inside dedicated initDone");
        setIP(HostManager.getHost(recursiveGet("host_id")).getExclusiveIP(getId()));
        getLog().info("Got ip");
    }

    public DedicatedIPResource(ResourceId rId) throws Exception {
        super(rId);
        setIP(HostManager.getHost(recursiveGet("host_id")).getIPbyRid(getId()));
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            HostManager.getHost(recursiveGet("host_id")).releaseIP(getIP());
        }
    }

    public static Hashtable getIpInfoByName(String ip) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip, r_id, account_id FROM l_server_ips, parent_child WHERE ip = ? AND l_server_ips.r_id + 0 = child_id");
            ps.setString(1, ip.trim());
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            Session.getLog().debug("Ip :" + ip);
            if (rs.next()) {
                Hashtable resource = new Hashtable(2);
                resource.put("resource_id", new Long(rs.getLong(2)));
                resource.put("account_id", new Long(rs.getLong(3)));
                Session.getLog().debug("Dedicated IP:" + ip + " account_id:" + rs.getLong(3) + " resourceId:" + rs.getLong(2));
                Session.closeStatement(ps);
                con.close();
                return resource;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
