package psoft.hsphere.ipmanagement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.cache.Cache;
import psoft.hsphere.cache.LockableCache;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPSubnetManager.class */
public class IPSubnetManager {
    public static int ACTION_ASSIGN = 1;
    public static int ACTION_UNASSIGN = 2;
    private static IPSubnetManager ourInstance = new IPSubnetManager();
    private static Category log = Category.getInstance(IPSubnetManager.class.getName());
    private Hashtable lockedRanges = new Hashtable();
    private Hashtable assignedRangesInfo = new Hashtable();

    public static IPSubnetManager getInstance() {
        return ourInstance;
    }

    private IPSubnetManager() {
    }

    public void addRange(IPRange range) {
    }

    public void delRange(IPRange range) {
    }

    public IPSubnet saveNewSubnet(IPSubnet ips) throws SQLException {
        log.debug("Inside IPSubnetManager::saveNewSubnet");
        PreparedStatement ps = null;
        Connection con = Session.getTransConnection();
        try {
            try {
                ps = con.prepareStatement("INSERT INTO ip_subnets(id, s_ip, e_ip, netmask, broadcast, gw, reseller_id)  VALUES(?,?,?,?,?,?,?)");
                long subnetId = Session.getNewIdAsLong("subnet_seq");
                Cache c = Session.getCacheFactory().getCache(IPSubnet.class);
                ((LockableCache) c).unlock(ips.getId());
                c.remove(ips);
                ips.setId(subnetId);
                ps.setLong(1, subnetId);
                ps.setString(2, ips.getStartIP());
                ps.setString(3, ips.getEndIP());
                ps.setString(4, ips.getNetmask());
                ps.setString(5, ips.getBroadcast());
                ps.setString(6, ips.getGw());
                ps.setLong(7, ips.getResellerId());
                for (IPRange ipr : ips.getRanges()) {
                    save(ipr);
                }
                ps.executeUpdate();
                con.commit();
                Session.getCacheFactory().getCache(IPSubnet.class).put(ips);
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
                return ips;
            } catch (SQLException ex) {
                Session.getLog().error("Error saving IPSubnet ", ex);
                con.rollback();
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public IPRange save(IPRange ipr) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO ip_ranges(id, parent_id, s_ip, e_ip, r_type) VALUES(?,?,?,?,?)");
            ps.setLong(1, ipr.getId());
            ps.setLong(2, ipr.getParent().getId());
            ps.setString(3, ipr.getStartIP());
            ps.setString(4, ipr.getEndIP());
            ps.setInt(5, ipr.getType());
            ps.executeUpdate();
            Session.getCacheFactory().getCache(IPRange.class).put(ipr);
            Session.closeStatement(ps);
            con.close();
            return ipr;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete(IPSubnet ips) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getTransConnection();
        try {
            try {
                Session.getCacheFactory().getCache(IPSubnet.class).remove(ips);
                ps = con.prepareStatement("DELETE FROM ip_subnets WHERE id = ?");
                ps.setLong(1, ips.getId());
                for (IPRange ipr : ips.getRanges()) {
                    delete(ipr);
                }
                ps.executeUpdate();
                con.commit();
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            } catch (SQLException ex) {
                Session.getLog().error("Error deleting IP subnet ", ex);
                con.rollback();
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public void delete(IPRange ipr) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            Session.getCacheFactory().getCache(IPRange.class).remove(ipr);
            ps = con.prepareStatement("DELETE FROM ip_ranges WHERE id = ?");
            ps.setLong(1, ipr.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public IPSubnet calculateSubnet(String startIP, String endIP, int slash) throws Exception {
        IPSubnet ips = IPCalc.getInstance().calcSubnet(startIP, endIP, slash);
        if (subnectCanBeAdded(startIP, endIP, ips)) {
            Cache c = Session.getCacheFactory().getCache(IPSubnet.class);
            c.put(ips);
            ((LockableCache) c).lock(ips.getId());
        }
        return ips;
    }

    public IPSubnet calculateSubnet(String startIP, String endIP, String netmask) throws Exception {
        IPSubnet ips = IPCalc.getInstance().calcSubnet(startIP, endIP, netmask);
        if (subnectCanBeAdded(startIP, endIP, ips)) {
            Cache c = Session.getCacheFactory().getCache(IPSubnet.class);
            c.put(ips);
            ((LockableCache) c).lock(ips.getId());
        }
        return ips;
    }

    public IPSubnet calculateSubnet(String startIP, String endIP) throws Exception {
        IPSubnet ips = IPCalc.getInstance().calcSubnet(startIP, endIP);
        if (subnectCanBeAdded(startIP, endIP, ips)) {
            Cache c = Session.getCacheFactory().getCache(IPSubnet.class);
            c.put(ips);
            ((LockableCache) c).lock(ips.getId());
        }
        return ips;
    }

    private synchronized boolean subnectCanBeAdded(String startIP, String endIP, IPSubnet resultedIPs) throws Exception {
        StringBuffer overlappingSubnets = new StringBuffer();
        long _startIP = resultedIPs.getStartIPasLong();
        long _endIP = resultedIPs.getEndIPasLong();
        Collection<IPSubnet> subnets = getAllSubnets();
        for (IPSubnet _ips : subnets) {
            if (((_ips.getStartIPasLong() <= _startIP && _startIP <= _ips.getEndIPasLong()) || (_ips.getStartIPasLong() <= _endIP && _endIP <= _ips.getEndIPasLong())) && overlappingSubnets.indexOf(_ips.getShortDescription()) == -1) {
                overlappingSubnets.append(_ips.getShortDescription()).append(';');
            }
            if (_startIP <= _ips.getStartIPasLong() && _endIP >= _ips.getEndIPasLong() && overlappingSubnets.indexOf(_ips.getShortDescription()) == -1) {
                overlappingSubnets.append(_ips.getShortDescription()).append(';');
            }
        }
        if (overlappingSubnets.length() > 0) {
            throw new HSUserException("ds.pool.subnet_can_not_be_added", new String[]{startIP, endIP, resultedIPs.getShortDescription(), overlappingSubnets.toString()});
        }
        return true;
    }

    public void loadIPRangesForSubnet(IPSubnet ips) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, s_ip, e_ip, r_type, rid FROM ip_ranges WHERE parent_id = ?");
            ps.setLong(1, ips.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                IPRange ipr = new IPRange(rs.getString("s_ip"), rs.getString("e_ip"));
                ipr.setType(rs.getInt("r_type"));
                ipr.setRid(rs.getLong("rid"));
                ips.addRange(ipr);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public IPSubnet getSubnet(long id) throws Exception {
        return getSubnet(id, false);
    }

    private synchronized IPSubnet getSubnet(long id, boolean force) throws Exception {
        IPSubnet ips = (IPSubnet) Session.getCacheFactory().getCache(IPSubnet.class).get(id);
        if (ips == null) {
            PreparedStatement ps = null;
            Connection con = Session.getDb();
            try {
                ps = con.prepareStatement("SELECT s_ip, e_ip, netmask, broadcast, gw, reseller_id FROM ip_subnets WHERE id = ?");
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long resellerId = rs.getLong("reseller_id");
                    if (Session.getResellerId() == resellerId || resellerId == 1 || Session.getResellerId() == 1 || force) {
                        ips = new IPSubnet(id);
                        ips.setStartIP(rs.getString("s_ip"));
                        ips.setEndIP(rs.getString("e_ip"));
                        ips.setNetmask(rs.getString("netmask"));
                        ips.setBroadcast(new IPRange(rs.getString("broadcast"), rs.getString("broadcast")));
                        ips.setGw(new IPRange(rs.getString("gw"), rs.getString("gw")));
                        ips.setResellerId(resellerId);
                        loadIPRangesForSubnet(ips);
                        Session.getCacheFactory().getCache(IPSubnet.class).put(ips);
                    } else {
                        throw new Exception("Access violation for reseller with id " + Session.getResellerId() + " while trying to get subnet with id" + id + " which belongs to reseller with id " + resellerId);
                    }
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        return ips;
    }

    public synchronized Collection getAllSubnets() throws Exception {
        return getSubnets(false);
    }

    public synchronized Collection getSubnets() throws Exception {
        return getSubnets(true);
    }

    private synchronized Collection getSubnets(boolean checkReseller) throws Exception {
        ArrayList res = new ArrayList();
        PreparedStatement ps = null;
        log.debug("Inside IPSubnetManager::getSubnets()");
        Connection con = Session.getDb();
        try {
            if (checkReseller) {
                ps = con.prepareStatement("SELECT id FROM ip_subnets WHERE reseller_id = ?");
                ps.setLong(1, Session.getResellerId());
            } else {
                ps = con.prepareStatement("SELECT id FROM ip_subnets");
            }
            ResultSet rs = ps.executeQuery();
            log.debug("Querry has been executed");
            while (rs.next()) {
                log.debug("Trying to get subnet with id " + rs.getLong("id"));
                IPSubnet _ips = getSubnet(rs.getLong("id"), !checkReseller);
                if (_ips != null) {
                    log.debug("Adding subnet with id " + rs.getLong("id"));
                    res.add(_ips);
                }
            }
            return res;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void splitRange(IPSubnet ips, IPRange ipr, String limit) throws Exception {
        synchronized (ips) {
            ips.delRange(ipr);
            IPRange _ipr1 = new IPRange(ipr.getStartIP(), limit);
            IPRange _ipr2 = new IPRange(IPCalc.getInstance().string2IP(limit) + 1, ipr.getEndIPasLong());
            _ipr1.setType(ipr.getType());
            _ipr2.setType(ipr.getType());
            ips.addRange(_ipr1);
            ips.addRange(_ipr2);
            try {
                saveRangeSet(ips);
            } catch (Exception ex) {
                ips.delRange(_ipr1);
                ips.delRange(_ipr2);
                ips.addRange(ipr);
                throw ex;
            }
        }
    }

    public void saveRangeSet(IPSubnet ips) throws Exception {
        Connection con = Session.getTransConnection();
        synchronized (ips) {
            try {
                PreparedStatement ps = con.prepareStatement("DELETE FROM ip_ranges WHERE parent_id = ?");
                ps.setLong(1, ips.getId());
                ps.executeUpdate();
                PreparedStatement ps1 = con.prepareStatement("INSERT INTO ip_ranges(id, parent_id, s_ip, e_ip, r_type, rid) VALUES (?,?,?,?,?,?)");
                for (IPRange _ipr : ips.getRanges()) {
                    ps1.setLong(1, _ipr.getId());
                    ps1.setLong(2, _ipr.getParent().getId());
                    ps1.setString(3, _ipr.getStartIP());
                    ps1.setString(4, _ipr.getEndIP());
                    ps1.setInt(5, _ipr.getType());
                    ps1.setLong(6, _ipr.getRid());
                    ps1.executeUpdate();
                }
                con.commit();
                Session.closeStatement(ps);
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        }
    }

    public void mergeRanges(IPSubnet ips, IPRange range1, IPRange range2) throws Exception {
        synchronized (ips) {
            IPRange result = new IPRange(range1.getStartIP(), range2.getEndIP());
            result.setType(range1.getType());
            ips.delRange(range1);
            ips.delRange(range2);
            ips.addRange(result);
            try {
                saveRangeSet(ips);
            } catch (Exception ex) {
                ips.delRange(result);
                ips.addRange(range1);
                ips.addRange(range2);
                throw ex;
            }
        }
    }

    public void lockRange(long rangeId) throws Exception {
        synchronized (this.lockedRanges) {
            removeOutdatedLocks();
            if (this.lockedRanges.contains(new Long(rangeId))) {
                throw new Exception("The IP range with ID " + rangeId + " already locked");
            }
            this.lockedRanges.put(new Long(rangeId), new Long(TimeUtils.currentTimeMillis()));
        }
    }

    public void unlockRange(long rangeId) {
        synchronized (this.lockedRanges) {
            this.lockedRanges.remove(new Long(rangeId));
        }
    }

    private void removeOutdatedLocks() {
        Collection<Object> toBeRemoved = new ArrayList();
        long currentMillis = TimeUtils.currentTimeMillis();
        synchronized (this.lockedRanges) {
            for (Long key : this.lockedRanges.keySet()) {
                Long val = (Long) this.lockedRanges.get(key);
                if (currentMillis - val.longValue() > 60000) {
                    toBeRemoved.add(key);
                }
            }
            for (Object obj : toBeRemoved) {
                this.lockedRanges.remove(obj);
            }
        }
    }

    public Collection getRangesByDS(long dsId) throws Exception {
        Collection result = new ArrayList();
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT ipr.id, ipr.parent_id FROM dedicated_servers ds, parent_child p, ip_ranges ipr WHERE ds.id = ? AND ds.rid = p.parent_id and p.child_type = ? AND p.child_id = ipr.rid");
            ps.setLong(1, dsId);
            ps.setInt(2, 7108);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                IPSubnet ips = getSubnet(rs.getLong("parent_id"));
                result.add(ips.getRange(rs.getLong("id")));
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public DedicatedServer getRangeOwner(IPRange ipr) throws Exception {
        if (ipr.getType() == 3) {
            Connection con = Session.getDb();
            try {
                PreparedStatement ps = con.prepareStatement("SELECT p.parent_id FROM ip_ranges ipr, parent_child p WHERE ipr.id = ? AND ipr.rid = p.child_id");
                ps.setLong(1, ipr.getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    DedicatedServer serverByRid = DSHolder.getServerByRid(rs.getLong("parent_id"));
                    Session.closeStatement(ps);
                    con.close();
                    return serverByRid;
                }
                throw new Exception("DS owner for IP range with id " + ipr.getId() + " not found");
            } catch (Throwable th) {
                Session.closeStatement(null);
                con.close();
                throw th;
            }
        }
        return null;
    }

    public void changeRangeSet(long subnetId, long rangeId, long dsId, int action) throws Exception {
        DSHolder dsh = new DSHolder(Session.getResellerId());
        DedicatedServer ds = dsh.getDServer(dsId);
        ResourceId dsRid = new ResourceId(ds.getRid(), ResourceId.getTypeById(ds.getRid()));
        IPSubnet ips = getInstance().getSubnet(subnetId);
        IPRange ipr = ips.getRange(rangeId);
        if (ipr == null) {
            throw new Exception("Unable to get ip range with id " + rangeId);
        }
        if (action == ACTION_ASSIGN && ipr.getType() != 1) {
            throw new HSUserException("ds.ip_pool.unable_assign.range_not_free", new String[]{ipr.toString()});
        }
        if (action == ACTION_UNASSIGN && ipr.getType() != 3) {
            throw new HSUserException("ds.ip_pool.unable_unassign.range_not_assigned", new String[]{ipr.toString()});
        }
        if (ds.getResellerId() != ipr.getParent().getResellerId()) {
            throw new Exception("The reseller ID of iprange " + ipr.getParent().getResellerId() + " does not match the reseller ID of the dedicated server " + ds.getResellerId());
        }
        Session.save();
        try {
            Account a = dsRid.getAccount();
            User u = dsRid.getAccount().getUser();
            Session.setUser(u);
            Session.setAccount(a);
            ResourceId dsIPRangeResourceId = dsRid.findChild("ds_ip_range");
            if (dsIPRangeResourceId == null) {
                if (action == ACTION_UNASSIGN) {
                    throw new HSUserException("ds.ip_pool.unable_get_iprange_resource");
                }
                if (action == ACTION_ASSIGN) {
                    Collection args = new ArrayList();
                    args.add(ipr);
                    Resource dsResource = dsRid.get();
                    dsResource.addChild("ds_ip_range", "", args);
                }
            } else {
                Resource dsIPRangeResource = dsIPRangeResourceId.get();
                Collection args2 = dsIPRangeResource.getCurrentInitValues();
                if (action == ACTION_ASSIGN) {
                    args2.add(ipr);
                } else if (action == ACTION_UNASSIGN) {
                    synchronized (this.assignedRangesInfo) {
                        this.assignedRangesInfo.remove(new Long(rangeId));
                    }
                    args2.remove(ipr);
                }
                dsIPRangeResource.change(args2);
            }
        } finally {
            Session.restore();
        }
    }

    public Hashtable getAssignedRangeInfo(long iprId) throws Exception {
        Hashtable assignedRangeInfo;
        synchronized (this.assignedRangesInfo) {
            assignedRangeInfo = (Hashtable) this.assignedRangesInfo.get(new Long(iprId));
            if (assignedRangeInfo == null) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT u.username, a.id AS account_id, ds.id AS ds_id, ds.name, ipru.info FROM parent_child pc, ip_ranges ipr, dedicated_servers ds, accounts a, users u, ipr_usageinfo ipru WHERE ipr.id = ? AND ipr.id = ipru.id AND ipr.rid = pc.child_id AND pc.parent_id = ds.rid AND pc.account_id = a.id AND a.reseller_id = u.id");
                ps.setLong(1, iprId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    assignedRangeInfo = new Hashtable();
                    assignedRangeInfo.put(FMACLManager.RESELLER, rs.getString("username"));
                    assignedRangeInfo.put("account_id", rs.getString("account_id"));
                    assignedRangeInfo.put("ds_id", rs.getString("ds_id"));
                    assignedRangeInfo.put("ds_name", rs.getString("name"));
                    assignedRangeInfo.put("usage_info", rs.getString("info"));
                    this.assignedRangesInfo.put(new Long(iprId), assignedRangeInfo);
                }
                Session.closeStatement(ps);
                con.close();
            }
        }
        return assignedRangeInfo;
    }

    public void updateIPRangeUsageInfo(long rangeId, String usageInfo) throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM ipr_usageinfo WHERE id = ?");
            ps1 = con.prepareStatement("INSERT INTO ipr_usageinfo(id, info) VALUES (?,?)");
            ps.setLong(1, rangeId);
            ps1.setLong(1, rangeId);
            ps1.setString(2, usageInfo);
            ps.executeUpdate();
            ps1.executeUpdate();
            synchronized (this.assignedRangesInfo) {
                Hashtable info = (Hashtable) this.assignedRangesInfo.get(new Long(rangeId));
                if (info != null) {
                    info.put("usage_info", usageInfo);
                    this.assignedRangesInfo.put(new Long(rangeId), info);
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }
}
