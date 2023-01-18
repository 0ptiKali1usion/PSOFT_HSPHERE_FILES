package psoft.hsphere.resource.allocation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import org.apache.log4j.Category;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.exception.PhysicalServerNotExist;
import psoft.hsphere.exception.allocation.AllocatedPServerNotFoundException;
import psoft.hsphere.exception.allocation.NoAvailableAllocatedServerForPlan;
import psoft.hsphere.resource.PhysicalServer;

/* loaded from: hsphere.zip:psoft/hsphere/resource/allocation/AllocatedPServerHolder.class */
public class AllocatedPServerHolder {
    private Hashtable pool = new Hashtable();
    private static final Category log = Category.getInstance(AllocatedPServerHolder.class.getName());
    private static AllocatedPServerHolder ourInstance = new AllocatedPServerHolder();

    public static AllocatedPServerHolder getInstance() {
        return ourInstance;
    }

    private AllocatedPServerHolder() {
        try {
            loadPool();
        } catch (SQLException e) {
            Session.getLog().error("Unable to load allocated servers pool:", e);
        }
    }

    public AllocatedPServer get(long apsId) {
        return (AllocatedPServer) getPool().get(new Long(apsId));
    }

    public AllocatedPServer add(long psId, int planId) throws SQLException, PhysicalServerNotExist {
        AllocatedPServer aps;
        PreparedStatement ps = null;
        PhysicalServer pServer = PhysicalServer.getPServer(psId);
        if (pServer != null) {
            Connection con = Session.getDb();
            try {
                synchronized (getPool()) {
                    ps = con.prepareStatement("INSERT INTO allocated_pserver(id, plan_id) VALUES(?, ?)");
                    ps.setLong(1, psId);
                    ps.setInt(2, planId);
                    ps.executeUpdate();
                    aps = new AllocatedPServer(psId, planId, 0L, null);
                    getPool().put(new Long(psId), aps);
                }
                Session.closeStatement(ps);
                con.close();
                return aps;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        throw new PhysicalServerNotExist(psId);
    }

    public void remove(long apsId) throws SQLException, AllocatedPServerNotFoundException {
        AllocatedPServer aps = get(apsId);
        if (aps != null) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("DELETE FROM allocated_pserver WHERE id = ?");
            ps.setLong(1, apsId);
            ps.executeUpdate();
            getPool().remove(new Long(apsId));
            return;
        }
        throw new AllocatedPServerNotFoundException(apsId);
    }

    public AllocatedPServer takeRandomAvailableAllocatedServer(long resellerId, ResourceId rid) throws UnknownResellerException, NoAvailableAllocatedServerForPlan, SQLException {
        AllocatedPServer allocatedPServer;
        Reseller r = Reseller.getReseller(resellerId);
        AllocatedPServer aps = null;
        int rPlanId = r.getResellerPlanId();
        Session.getLog().debug("Inside takeRandomAvailableAllocatedServer: resellerId=" + resellerId + " planid=" + rPlanId);
        synchronized (getPool()) {
            Iterator i = getPool().values().iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                AllocatedPServer _aps = (AllocatedPServer) i.next();
                if (_aps.getState() == 1 && _aps.getPlanId() == rPlanId) {
                    aps = _aps;
                    break;
                }
            }
            if (aps != null) {
                aps.setResellerId(resellerId);
                aps.setUsedBy(rid);
                updateAllocatedPServer(aps);
                allocatedPServer = aps;
            } else {
                throw new NoAvailableAllocatedServerForPlan(rPlanId);
            }
        }
        return allocatedPServer;
    }

    public void updateAllocatedPServer(AllocatedPServer aps) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE allocated_pserver SET plan_id = ?, reseller_id = ?, rid = ? WHERE id = ?");
            ps.setInt(1, aps.getPlanId());
            ps.setLong(2, aps.getResellerId());
            ps.setLong(3, aps.getUsedBy() == null ? 0L : aps.getUsedBy().getId());
            ps.setLong(4, aps.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isAllocated(long psId) {
        boolean contains;
        synchronized (getPool()) {
            contains = getPool().keySet().contains(new Long(psId));
        }
        return contains;
    }

    private void loadPool() throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT ps.id, aps.reseller_id, aps.plan_id, aps.rid FROM p_server ps, allocated_pserver aps WHERE ps.id = aps.id");
            ResultSet rs = ps.executeQuery();
            synchronized (getPool()) {
                while (rs.next()) {
                    getPool().put(new Long(rs.getLong("id")), new AllocatedPServer(rs.getLong("id"), rs.getInt("plan_id"), rs.getLong("reseller_id"), rs.getLong("rid") == 0 ? null : new ResourceId(rs.getLong("rid"), 153)));
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Hashtable getPool() {
        return this.pool;
    }

    public void unassignPServer(long apsId) throws SQLException {
        AllocatedPServer aps = get(apsId);
        if (aps != null) {
            synchronized (getPool()) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("DELETE FROM allocated_pserver WHERE id = ?");
                ps.setLong(1, aps.getId());
                ps.executeUpdate();
                getPool().remove(new Long(aps.getId()));
                Session.closeStatement(ps);
                con.close();
            }
        }
    }
}
