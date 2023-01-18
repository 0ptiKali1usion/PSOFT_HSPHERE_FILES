package psoft.hsphere.p001ds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.cache.AbstractCache;
import psoft.hsphere.resource.HostEntry;

/* renamed from: psoft.hsphere.ds.NetSwitchManager */
/* loaded from: hsphere.zip:psoft/hsphere/ds/NetSwitchManager.class */
public class NetSwitchManager extends AbstractCache {
    protected static List nsIds = new ArrayList();
    protected static boolean areNsIdsInitialized = false;

    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return NetSwitch.class;
    }

    public synchronized NetSwitch addNetSwitch(String device, String communityName, String description, String webURL, HostEntry mrtgHost) throws Exception {
        if (mrtgHost == null || mrtgHost.getGroup() != 25) {
            throw new HSUserException("netswitch.incor_specified_mrtg_host");
        }
        if (getNetSwitchIdByDevice(device) > -1) {
            throw new HSUserException("netswitch.device_not_unique");
        }
        long mrtgHostId = mrtgHost.getId();
        long newId = Session.getNewIdAsLong("netswitch_seq");
        long resellerId = Session.getResellerId();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("INSERT INTO snmp_netswitches (id, device, community_name, description, web_url, mrtg_host_id, reseller_id) VALUES (?,?,?,?,?,?,?)");
        try {
            ps.setLong(1, newId);
            ps.setString(2, device);
            ps.setString(3, communityName);
            ps.setString(4, description);
            ps.setString(5, webURL);
            ps.setLong(6, mrtgHostId);
            ps.setLong(7, resellerId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            NetSwitch ns = new NetSwitch(newId, device, communityName, description, webURL, mrtgHostId, resellerId);
            put(ns);
            addNsId(newId);
            return ns;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void delNetSwitch(long switchId) throws Exception {
        if (!getNsIds().contains(new Long(switchId))) {
            throw new HSUserException("netswitch.incor_id");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("DELETE FROM  snmp_netswitches WHERE id = ? AND reseller_id = ?");
        try {
            ps.setLong(1, switchId);
            ps.setLong(2, Session.getResellerId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                NetSwitch ns = (NetSwitch) get(switchId);
                if (ns != null) {
                    remove(ns);
                }
                delNsId(switchId);
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public synchronized NetSwitch getNetSwitch(long switchId) throws Exception {
        if (!getNsIds().contains(new Long(switchId))) {
            throw new HSUserException("netswitch.incor_id");
        }
        NetSwitch ns = (NetSwitch) get(switchId);
        if (ns == null) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT device, community_name, description, web_url, mrtg_host_id, reseller_id FROM snmp_netswitches WHERE id = ?");
            try {
                ps.setLong(1, switchId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    String device = rs.getString(1);
                    String communityName = rs.getString(2);
                    String description = rs.getString(3);
                    String webURL = rs.getString(4);
                    long mrtgHostId = rs.getLong(5);
                    long resellerId = rs.getLong(6);
                    ns = new NetSwitch(switchId, device, communityName, description, webURL, mrtgHostId, resellerId);
                    put(ns);
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        return ns;
    }

    public List getNsIds() throws Exception {
        List list;
        synchronized (nsIds) {
            if (!areNsIdsInitialized) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT DISTINCT id FROM snmp_netswitches");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    nsIds.add(new Long(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
                areNsIdsInitialized = true;
            }
            list = nsIds;
        }
        return list;
    }

    private void addNsId(long id) {
        if (areNsIdsInitialized) {
            synchronized (nsIds) {
                nsIds.add(new Long(id));
            }
        }
    }

    private void delNsId(long id) {
        if (areNsIdsInitialized) {
            synchronized (nsIds) {
                nsIds.remove(new Long(id));
            }
        }
    }

    public boolean existsNsId(long id) throws Exception {
        return getNsIds().contains(new Long(id));
    }

    public synchronized NetSwitch updateNetSwitch(long switchId, String device, String communityName, String description, String webURL, HostEntry mrtgHost) throws Exception {
        if (!getNsIds().contains(new Long(switchId))) {
            throw new HSUserException("netswitch.incor_id");
        }
        if (mrtgHost == null || mrtgHost.getGroup() != 25) {
            throw new HSUserException("netswitch.incor_specified_mrtg_host");
        }
        long mrtgHostId = mrtgHost.getId();
        Session.getCacheFactory().getLockableCache(getCacheInterface()).lock(switchId);
        try {
            NetSwitch ns = getNetSwitch(switchId);
            if (device != null && !device.equals(ns.getDevice()) && getNetSwitchIdByDevice(device) > -1) {
                throw new HSUserException("netswitch.device_not_unique");
            }
            if (ns.getUsedPortNumber() > 0 && ns.getMrtgHostId() != mrtgHost.getId()) {
                throw new HSUserException("netswitch.cannot_change_mrtg_host");
            }
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE snmp_netswitches SET device=?, community_name=?, description=?, web_url=?, mrtg_host_id=? WHERE id=?");
            ps.setString(1, device);
            ps.setString(2, communityName);
            ps.setString(3, description);
            ps.setString(4, webURL);
            ps.setLong(5, mrtgHostId);
            ps.setLong(6, switchId);
            ps.executeUpdate();
            String oldDevice = ns.getDevice();
            String oldDescription = ns.getDescription();
            ns.updateData(device, communityName, description, webURL, mrtgHostId);
            if (!oldDevice.equals(device) || !oldDescription.equals(description)) {
                try {
                    DSNetInterfaceManager netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
                    for (DSNetInterface dSNetInterface : netInterfaceMan.getTakenInterfacesByNetswitch(switchId)) {
                        netInterfaceMan.updateInterface(dSNetInterface.getId(), "");
                    }
                } catch (Exception ex) {
                    throw new HSUserException("netswitch.data_updated_width_errors_mes", new String[]{ex.getMessage()});
                }
            }
            Session.closeStatement(ps);
            con.close();
            Session.getCacheFactory().getLockableCache(getCacheInterface()).unlock(switchId);
            return ns;
        } catch (Throwable th) {
            Session.getCacheFactory().getLockableCache(getCacheInterface()).unlock(switchId);
            throw th;
        }
    }

    public List getNetSwitches() throws Exception {
        List list = new ArrayList();
        long sessionResellerId = Session.getResellerId();
        for (Long l : getNsIds()) {
            NetSwitch ns = getNetSwitch(l.longValue());
            if (ns != null && (sessionResellerId == ns.getResellerId() || sessionResellerId == 1)) {
                list.add(ns);
            }
        }
        return list;
    }

    public List getNetSwitchesByMrtgHost(long hostId) throws Exception {
        List list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT DISTINCT id FROM snmp_netswitches WHERE mrtg_host_id = ?");
        try {
            ps.setLong(1, hostId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                NetSwitch ns = getNetSwitch(rs.getLong(1));
                if (ns != null) {
                    list.add(ns);
                }
            }
            return list;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    private long getNetSwitchIdByDevice(String device) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id FROM snmp_netswitches WHERE device = ?");
        try {
            ps.setString(1, device);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long j = rs.getLong(1);
                Session.closeStatement(ps);
                con.close();
                return j;
            }
            Session.closeStatement(ps);
            con.close();
            return -1L;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
