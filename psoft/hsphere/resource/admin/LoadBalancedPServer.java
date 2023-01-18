package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/LoadBalancedPServer.class */
public class LoadBalancedPServer extends PhysicalServer {
    protected String lbParentId;
    protected String lbIsParent;

    public LoadBalancedPServer(long id, String name, String ip1, String mask1, String ip2, String mask2, String login, String password, int status, int osType, int getServerInfo, String lbParentId, String lbIsParent) throws Exception {
        super(id, name, ip1, mask1, ip2, mask2, login, password, status, osType, getServerInfo);
        this.lbParentId = lbParentId;
        this.lbIsParent = lbIsParent;
    }

    @Override // psoft.hsphere.resource.admin.PhysicalServer, psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("lbParentId")) {
            return new TemplateString(this.lbParentId);
        }
        if (key.equals("lbIsParent")) {
            return new TemplateString(this.lbIsParent);
        }
        if (key.equals("hasChild")) {
            try {
                List lbServers = getChildLoadBalancedPServers();
                if (lbServers == null || lbServers.size() < 2) {
                    return new TemplateString("0");
                }
            } catch (Exception e) {
                Session.getLog().warn("Get info from Load Balanced Server " + getId(), e);
                return null;
            }
        }
        return super.get(key);
    }

    public static int insertLoadBalancedPServer(long id, long parentId) throws Exception {
        PhysicalServer p = get(id);
        if (p.getLServers().size() != 0 && parentId != id) {
            return 1;
        }
        List mainGroups = p.getUniqueTypes();
        if (!mainGroups.contains(Integer.toString(1)) && !mainGroups.contains(Integer.toString(3))) {
            return 3;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps1 = con.prepareStatement("SELECT * FROM loadbalanced_pserver WHERE id = ?");
            ps1.setLong(1, id);
            ResultSet rs = ps1.executeQuery();
            if (rs.next()) {
                ps = con.prepareStatement("UPDATE loadbalanced_pserver SET parent_ps_id = ? , is_parent =" + (parentId == id ? " 1 " : " null ") + "WHERE id = ?");
                ps.setLong(1, parentId);
                ps.setLong(2, id);
            } else {
                ps = con.prepareStatement("INSERT INTO loadbalanced_pserver values ( ?, ? " + (parentId == id ? ", 1 " : " ") + ")");
                ps.setLong(1, id);
                ps.setLong(2, parentId);
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return 0;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deleteLoadBalancedPServer() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM loadbalanced_pserver WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static LoadBalancedPServer getLoadBalancedPServerbyId(long id) throws Exception {
        String loadbalParentId = "";
        String loadbalIsParent = "";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT * FROM loadbalanced_pserver WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                loadbalParentId = rs.getString(2);
                loadbalIsParent = rs.getString(3);
            }
            PhysicalServer p = get(id);
            LoadBalancedPServer lb = new LoadBalancedPServer(p.getId(), p.getName(), p.getIp1(), p.getMask1(), p.getIp2(), p.getMask2(), p.getLogin(), p.getPassword(), p.getStatus(), p.getOsType(), p.getGetServerInfo(), loadbalParentId, loadbalIsParent);
            Session.closeStatement(ps);
            con.close();
            return lb;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getParentLoadBalancedPServers(int ps_id) throws Exception {
        List lbServers = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l.id FROM loadbalanced_pserver AS l NATURAL INNER JOIN p_server AS p INNER JOIN p_server_group_map AS g ON ( p.id = g.ps_id ) WHERE is_parent = 1 AND g.group_id IN ( SELECT group_id FROM p_server_group_map WHERE ps_id = ?)");
            ps.setInt(1, ps_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lbServers.add(getLoadBalancedPServerbyId(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return lbServers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_deleteLoadBalancedPServer() throws Exception {
        deleteLoadBalancedPServer();
        return this;
    }

    public List getChildLoadBalancedPServers() throws Exception {
        List lbServers = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM loadbalanced_pserver WHERE parent_ps_id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                lbServers.add(getLoadBalancedPServerbyId(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return lbServers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getChildLoadBalancedPServers() throws Exception {
        return new TemplateList(getChildLoadBalancedPServers());
    }
}
