package psoft.hsphere.monitoring;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.PhysicalServer;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/MonitorableHost.class */
public class MonitorableHost implements Monitorable {
    protected HostEntry host;
    public static final int LS_NOT_ASSIGNED = 0;
    public static final int PS_AVAILABLE = 1;
    public static final int PS_NOT_AVAILABLE = 0;
    public static final int PS_ASSIGNED = 2;

    public MonitorableHost(HostEntry he) {
        this.host = he;
    }

    @Override // psoft.hsphere.monitoring.Monitorable
    public void println(Object msg) throws Exception {
        StringBuffer buf = new StringBuffer(msg.toString());
        buf.append(" Server:").append(this.host.getId()).append(" - ");
        buf.append(this.host.getPFirstIP() + ":" + getClass().getName());
        System.out.println(buf.toString());
    }

    @Override // psoft.hsphere.monitoring.Monitorable
    public void check() throws Exception {
        if (this.host.getStatus() != 0) {
            println("Checking ");
        }
    }

    @Override // psoft.hsphere.monitoring.Monitorable
    public void fix() throws Exception {
        println("Fixing");
        PhysicalServer ph = this.host.getPServer();
        println("Phisical host" + ph);
        updatePServerStatus(ph.getId(), 0);
        println("Got it to be unavailable" + ph);
        incarnate(this.host.getId());
        println("Incarnated");
        NetSwitchManager.portDown(ph.getPFirstIP());
        NetSwitchManager.portDown(ph.getPSecondIP());
        println("Setting port down");
    }

    protected void incarnate(long lserver) throws Exception {
        Session.getLog().error("Starting incarnation");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT p_server.id, l_server.file_server_path FROM l_server, p_server_group_map, p_server WHERE l_server.id = ? AND p_server_group_map.group_id = l_server.group_id  AND p_server.id = p_server_group_map.ps_id AND p_server.status = 1");
            ps.setLong(1, lserver);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                throw new Exception("Cannot find the server");
            }
            Session.getLog().debug("got pserver: " + rs.getString(1));
            println(" rs.next() " + rs.getString(1));
            PhysicalServer phs = PhysicalServer.getPServer(rs.getLong(1));
            Session.getLog().debug("Becoming");
            Collection<Object> c = phs.exec("/hsphere/shared/scripts/system/become-server", new String[]{rs.getString(2)}, null);
            Session.getLog().debug("Got it");
            for (Object obj : c) {
                println(obj);
            }
            updatePServerStatus(rs.getLong(1), 2);
            Session.getLog().debug("Changing status of pserver");
            updateLServerPServer(lserver, rs.getLong(1));
            Session.getLog().debug("Changing status of lserver");
            Session.getLog().debug("Done with incarnate");
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void deIncarnate(long lserver) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" SELECT p_server.id, p_server.ip1 FROM l_server, p_server WHERE l_server.id = ? AND p_server.id = l_server.p_server_id");
            ps.setLong(1, lserver);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                println(" rs.next() " + rs.getString(2));
                PhysicalServer phs = PhysicalServer.getPServerForLServer(lserver);
                phs.exec("/hsphere/shared/scripts/system/become-ready", new String[0], null);
                updatePServerStatus(rs.getLong(1), 1);
                updateLServerPServer(lserver, 0L);
                return;
            }
            throw new Exception("Unable to deincornate server");
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void putServerDown(long lserver) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(" SELECT p_server.id, p_server.ip1 FROM l_server, p_server WHERE l_server.id = ? AND p_server.id = l_server.p_server_id");
            ps.setLong(1, lserver);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                println(" rs.next() true:" + rs.getString(2));
                PhysicalServer phs = PhysicalServer.getPServerForLServer(lserver);
                phs.exec("/hsphere/shared/scripts/system/become-ready", new String[0], null);
                updatePServerStatus(rs.getLong(1), 0);
                updateLServerPServer(lserver, 0L);
                return;
            }
            println(" rs.next() false");
            throw new Exception("Cannot find p_server.ip1 for the given lserver.");
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void updateLServerPServer(long lserver, long pserver) throws SQLException {
        Connection con = Session.getDb();
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
        Connection con = Session.getDb();
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
