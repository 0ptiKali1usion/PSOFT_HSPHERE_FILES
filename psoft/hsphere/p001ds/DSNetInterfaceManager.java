package psoft.hsphere.p001ds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.cache.AbstractCache;
import psoft.util.TimeUtils;

/* renamed from: psoft.hsphere.ds.DSNetInterfaceManager */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DSNetInterfaceManager.class */
public class DSNetInterfaceManager extends AbstractCache {
    protected static List interfaceIds = new ArrayList();
    protected static boolean areInterfaceIdsInitialized = false;

    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return DSNetInterface.class;
    }

    public synchronized DSNetInterface addInterface(DedicatedServer ds, NetSwitch netSwitch, int port, String description) throws Exception {
        if (ds == null || ds.getCreated() == null) {
            throw new HSUserException("ds.netinterface.incor_specified_ds");
        }
        if (netSwitch == null) {
            throw new HSUserException("ds.netinterface.incor_specified_ns");
        }
        if (netSwitch.isPortInUse(port)) {
            throw new HSUserException("ds.netinterface.port_in_use_mes", new String[]{String.valueOf(port)});
        }
        if (!isAuthorizedReseller(ds, netSwitch)) {
            throw new HSUserException("ds.netinterface.not_authorized_add");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("INSERT INTO ds_netinterfaces (id, ds_id, switch_id, switch_port, target_name, description, created, reseller_id) VALUES (?,?,?,?,?,?,?,?)");
        long resellerId = Session.getResellerId();
        long dsId = ds.getId();
        long nsId = netSwitch.getId();
        if (description == null || "".equals(description)) {
            description = generateDescription(netSwitch, port);
        }
        try {
            long newId = Session.getNewIdAsLong("ds_netinterface_seq");
            String targetName = generateTargetName(newId);
            Date created = TimeUtils.getDate();
            ps.setLong(1, newId);
            ps.setLong(2, dsId);
            ps.setLong(3, nsId);
            ps.setInt(4, port);
            ps.setString(5, targetName);
            ps.setString(6, description);
            ps.setTimestamp(7, new Timestamp(created.getTime()));
            ps.setLong(8, resellerId);
            ps.executeUpdate();
            DSNetInterface netInterface = new DSNetInterface(newId, dsId, nsId, port, targetName, description, created, resellerId);
            put(netInterface);
            addInterfaceId(newId);
            if (ds.getRid() != 0) {
                netInterface.postConfig();
                netInterface.setStartDate();
            }
            return netInterface;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public synchronized void delInterface(DSNetInterface netInterface) throws Exception {
        if (netInterface == null) {
            throw new HSUserException("ds.netinterface.cannot_access");
        }
        long niId = netInterface.getId();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("UPDATE ds_netinterfaces SET deleted = ? WHERE id = ?");
        Timestamp deleted = TimeUtils.getSQLTimestamp();
        try {
            ps.setTimestamp(1, deleted);
            ps.setLong(2, niId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            try {
                DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(netInterface.getDsId());
                if (ds != null && ds.getRid() != 0) {
                    netInterface.delConfig();
                    netInterface.resetStartDate();
                }
            } finally {
                remove(netInterface);
                delInterfaceId(niId);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void delInterface(long id) throws Exception {
        delInterface(getInterface(id));
    }

    public synchronized DSNetInterface getInterface(long id) throws Exception {
        if (!getInterfaceIds().contains(new Long(id))) {
            throw new HSUserException("ds.netinterface.incor_id");
        }
        DSNetInterface netInterface = (DSNetInterface) get(id);
        if (netInterface == null) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT ds_id, switch_id, switch_port, target_name, description, created, reseller_id FROM ds_netinterfaces WHERE id = ? AND deleted is null");
            try {
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    long dsId = rs.getLong(1);
                    long nsId = rs.getLong(2);
                    int nsPort = rs.getInt(3);
                    String targetName = rs.getString(4);
                    String description = rs.getString(5);
                    Date created = rs.getTimestamp(6);
                    long resellerId = rs.getLong(7);
                    netInterface = new DSNetInterface(id, dsId, nsId, nsPort, targetName, description, created, resellerId);
                    put(netInterface);
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        return netInterface;
    }

    public List getInterfaceIds() throws Exception {
        List list;
        synchronized (interfaceIds) {
            if (!areInterfaceIdsInitialized) {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT DISTINCT id FROM ds_netinterfaces WHERE deleted is null");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    interfaceIds.add(new Long(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
                areInterfaceIdsInitialized = true;
            }
            list = interfaceIds;
        }
        return list;
    }

    private void addInterfaceId(long id) {
        if (areInterfaceIdsInitialized) {
            synchronized (interfaceIds) {
                interfaceIds.add(new Long(id));
            }
        }
    }

    private void delInterfaceId(long id) {
        if (areInterfaceIdsInitialized) {
            synchronized (interfaceIds) {
                interfaceIds.remove(new Long(id));
            }
        }
    }

    public boolean existsInterfaceId(long id) throws Exception {
        return getInterfaceIds().contains(new Long(id));
    }

    public synchronized DSNetInterface updateInterface(long netInterfaceId, String description) throws Exception {
        DSNetInterface netInterface = getInterface(netInterfaceId);
        if (netInterface == null) {
            throw new HSUserException("ds.netinterface.cannot_access");
        }
        Session.getCacheFactory().getLockableCache(getCacheInterface()).lock(netInterfaceId);
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("UPDATE ds_netinterfaces SET description = ? WHERE id = ?");
        if (description == null || "".equals(description)) {
            NetSwitch netswitch = ((NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class)).getNetSwitch(netInterface.getNetswitchId());
            description = generateDescription(netswitch, netInterface.getNetswitchPort());
        }
        try {
            ps.setString(1, description);
            ps.setLong(2, netInterfaceId);
            ps.executeUpdate();
            netInterface.updateData(description);
            DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(netInterface.getDsId());
            if (ds.getRid() != 0) {
                netInterface.postConfig();
            }
            return netInterface;
        } finally {
            Session.closeStatement(ps);
            con.close();
            Session.getCacheFactory().getLockableCache(getCacheInterface()).unlock(netInterfaceId);
        }
    }

    public List getAllAccessibleInterfaces() throws Exception {
        List list = new ArrayList();
        long sessionResellerId = Session.getResellerId();
        for (Long l : getInterfaceIds()) {
            DSNetInterface ni = getInterface(l.longValue());
            if (ni != null && ni.getResellerId() == sessionResellerId) {
                list.add(ni);
            }
        }
        return list;
    }

    public List getInterfacesByDS(long dsId) throws Exception {
        List list = new ArrayList();
        for (Long l : getInterfaceIds()) {
            DSNetInterface ni = getInterface(l.longValue());
            if (ni != null && ni.getDsId() == dsId) {
                list.add(ni);
            }
        }
        return list;
    }

    public List getInterfacesByNetswitch(long nsId) throws Exception {
        List list = new ArrayList();
        for (Long l : getInterfaceIds()) {
            DSNetInterface ni = getInterface(l.longValue());
            if (ni != null && ni.getNetswitchId() == nsId) {
                list.add(ni);
            }
        }
        return list;
    }

    public List getTakenInterfacesByNetswitch(long nsId) throws Exception {
        DedicatedServer ds;
        List list = new ArrayList();
        for (Long l : getInterfaceIds()) {
            DSNetInterface ni = getInterface(l.longValue());
            if (ni != null && ni.getNetswitchId() == nsId && (ds = DSHolder.getAcessibleDedicatedServer(ni.getDsId())) != null && ds.getRid() != 0) {
                list.add(ni);
            }
        }
        return list;
    }

    protected static String generateTargetName(long niId) throws Exception {
        return String.valueOf(niId);
    }

    protected String generateDescription(NetSwitch netswitch, int port) throws Exception {
        return Localizer.translateMessage("ds.netinterfave_description_mes", new String[]{netswitch.getDescription(), netswitch.getDevice(), String.valueOf(port)});
    }

    protected boolean isAuthorizedReseller(DedicatedServer ds, NetSwitch ns) throws Exception {
        long dsResellerId = ds.getResellerId();
        return dsResellerId == ns.getResellerId() && dsResellerId == Session.getResellerId();
    }
}
