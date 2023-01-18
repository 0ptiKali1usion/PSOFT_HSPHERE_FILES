package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import psoft.hsphere.resource.PhysicalServer;
import psoft.p000db.Database;

/* loaded from: hsphere.zip:psoft/hsphere/ServerManager.class */
public class ServerManager {

    /* renamed from: db */
    protected Database f48db;
    public static final int LS_NOT_ASSIGNED = 0;
    public static final int PS_AVAILABLE = 1;
    public static final int PS_NOT_AVAILABLE = 0;
    public static final int PS_ASSIGNED = 2;

    public ServerManager(Database db) {
        this.f48db = db;
    }

    public void incarnate(long lserver) throws Exception {
        Connection con = this.f48db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT p_server.id, l_server.file_server FROM l_server,  p_server_group_map, p_server  WHERE l_server.id = ? AND  p_server_group_map.group_id = l_server.group_id  AND  p_server.id = p_server_group_map.ps_id  AND p_server.status = 1");
            ps.setLong(1, lserver);
            ResultSet rs = ps.executeQuery();
            System.err.println(" rs.next() " + rs.next() + rs.getString(1));
            PhysicalServer phs = PhysicalServer.getPServer(rs.getLong(1));
            Collection<Object> c = phs.exec("/hsphere/shared/scripts.system/become-server", new String[]{rs.getString(2)}, null);
            for (Object obj : c) {
                System.err.println(obj);
            }
            updatePServerStatus(rs.getLong(1), 2);
            updateLServerPServer(lserver, rs.getLong(1));
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deIncarnate(long lserver) throws Exception {
        Connection con = this.f48db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" SELECT p_server.id, p_server.ip1 FROM l_server, p_server  WHERE l_server.id = ? AND p_server.id = l_server.p_server_id");
            ps.setLong(1, lserver);
            ResultSet rs = ps.executeQuery();
            System.err.println(" rs.next() " + rs.next() + rs.getString(2));
            PhysicalServer phs = PhysicalServer.getPServerForLServer(lserver);
            phs.exec("/hsphere/shared/scripts/system/become-ready", new String[0], null);
            updatePServerStatus(rs.getLong(1), 1);
            updateLServerPServer(lserver, 0L);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void serverDown(long lserver) throws Exception {
        Connection con = this.f48db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" SELECT p_server.id, p_server.ip1 FROM l_server, p_server  WHERE l_server.id = ? AND p_server.id = l_server.p_server_id");
            ps.setLong(1, lserver);
            ResultSet rs = ps.executeQuery();
            System.err.println(" rs.next() " + rs.next() + rs.getString(2));
            PhysicalServer phs = PhysicalServer.getPServerForLServer(lserver);
            phs.exec("/sbin/become-ready", new String[0], null);
            updatePServerStatus(rs.getLong(1), 0);
            updateLServerPServer(lserver, 0L);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void updateLServerPServer(long lserver, long pserver) throws SQLException {
        Connection con = this.f48db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" UPDATE l_server SET p_server_id = ? WHERE id = ?");
            ps.setLong(1, pserver);
            ps.setLong(2, lserver);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void updatePServerStatus(long id, int status) throws SQLException {
        Connection con = this.f48db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" UPDATE p_server SET status = ? WHERE id = ?");
            ps.setInt(1, status);
            ps.setLong(2, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
