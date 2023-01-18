package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmDNSManager.class */
public class AdmDNSManager extends Resource {
    protected static HashMap resellerMap = new HashMap();

    public AdmDNSManager(int type, Collection init) throws Exception {
        super(type, init);
    }

    public AdmDNSManager(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("dns_zones")) {
                return new ListAdapter(getDNSZones());
            }
            if (key.equals("i_ssl_zones")) {
                try {
                    return new TemplateList(getISSLZones());
                } catch (Exception ex) {
                    Session.getLog().error("Error while loading shared SSL DNS Zones ", ex);
                    return null;
                }
            } else if (key.equals("dns_hosts")) {
                return new ListAdapter(AdmDNSZone.getHostsList());
            } else {
                return super.get(key);
            }
        } catch (Exception e) {
            Session.getLog().error("Problem in AdmDNSManage.get()", e);
            return null;
        }
    }

    public List getDNSZones() throws Exception {
        return getDNSZones(Session.getResellerId());
    }

    protected List getDNSZones(long resellerId) throws Exception {
        String reseller = Long.toString(resellerId);
        ArrayList list = (List) resellerMap.get(reseller);
        if (list == null) {
            list = new ArrayList();
            loadDNSZones(list);
            resellerMap.put(reseller, list);
        }
        return list;
    }

    protected void loadDNSZones(List list) throws Exception {
        list.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM e_zones WHERE reseller_id = ?");
            Session.getLog().debug("Going to get zones");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session.getLog().debug("Trying to get zone with id=" + rs.getLong("id"));
                list.add(AdmDNSZone.get(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public long addDNSZone(String name, String admEmail, boolean allow_hosting, long master, long slave1, long slave2) throws Exception {
        long newId = Session.getNewIdAsLong();
        getDNSZones().add(AdmDNSZone.create(newId, name, admEmail, allow_hosting, master, slave1, slave2));
        return newId;
    }

    public TemplateModel FM_addDNSZone(String name, String email, String allow_hosting, long master, long slave1, long slave2) throws Exception {
        addDNSZone(name, email, "1".equals(allow_hosting), master, slave1, slave2);
        return this;
    }

    private void deleteDNSZone(long id) throws Exception {
        AdmDNSZone zone = AdmDNSZone.get(id);
        if (zone == null) {
            return;
        }
        zone.delete();
        getDNSZones().remove(zone);
    }

    public TemplateModel FM_deleteDNSZone(long id) throws Exception {
        deleteDNSZone(id);
        return this;
    }

    public TemplateModel FM_addCustomDNSRecord(long zoneId, String data, String name, String type, String ttl, String pref) throws Exception {
        AdmDNSZone dnsZone = AdmDNSZone.get(zoneId);
        String fqdn = name + "." + dnsZone.getName();
        for (AdmCustomDNSRecord cRec : dnsZone.getCustomRecords()) {
            if ("A".equals(type) && "A".equals(cRec.getType()) && cRec.getName().equals(fqdn)) {
                throw new HSUserException("admdnsmanager.dns1");
            }
            if ("MX".equals(type) && "MX".equals(cRec.getType()) && cRec.getName().equals(fqdn) && data.equals(cRec.getData())) {
                throw new HSUserException("admdnsmanager.dns2");
            }
        }
        AdmCustomDNSRecord cRec2 = dnsZone.addCustRecord(data, name, type, ttl, pref);
        dnsZone.loadCustomRecords();
        return cRec2;
    }

    public TemplateModel FM_delCustomDNSRecord(long zoneId, long recId) throws Exception {
        AdmDNSZone.get(zoneId).delCustomRecord(recId);
        AdmDNSZone.get(zoneId).loadCustomRecords();
        return this;
    }

    public TemplateModel FM_addAlias(long zoneId, String prefix, int tag) throws Exception {
        AdmInstantAlias alias = AdmDNSZone.get(zoneId).addAllias(prefix, tag);
        return alias;
    }

    public TemplateModel FM_addCpAlias(long zoneId, String prefix) throws Exception {
        ResellerCpAlias alias = AdmDNSZone.get(zoneId).addCpAlias(prefix);
        return alias;
    }

    public TemplateModel FM_delCpAlias(long zoneId, long aliasId) throws Exception {
        AdmDNSZone.get(zoneId).delCpAlias(aliasId);
        return this;
    }

    public TemplateModel FM_delAlias(long zoneId, long aliasId) throws Exception {
        AdmDNSZone.get(zoneId).delAllias(aliasId);
        return this;
    }

    public TemplateModel FM_getZone(long id) throws Exception {
        AdmDNSZone a = AdmDNSZone.get(id);
        if (a.getOwner() == Session.getResellerId() || Session.getResellerId() == 1) {
            return a;
        }
        return null;
    }

    public TemplateModel FM_getAlias(long id) throws Exception {
        return AdmInstantAlias.get(id);
    }

    public TemplateModel FM_getCustomDNSRecord(long id) throws Exception {
        return AdmCustomDNSRecord.get(id);
    }

    public TemplateModel FM_setAllowZoneHosting(long zoneId, String allowHosting) throws Exception {
        AdmDNSZone.get(zoneId).setAllowHosting("1".equals(allowHosting));
        return this;
    }

    public TemplateModel FM_addCpAliasDNSRecord(long aliasId, long lServerId, String ip) throws Exception {
        ResellerCpAlias.get(aliasId).addRecord(lServerId, ip);
        ResellerCpAlias.get(aliasId).getLservers();
        return this;
    }

    public TemplateModel FM_delDNSRecord(long aliasId, long recordId) throws Exception {
        AdmInstantAlias.get(aliasId).delRecord(recordId);
        AdmInstantAlias.get(aliasId).getLservers();
        return this;
    }

    public TemplateModel FM_addDNSRecord(long aliasId, long lServerId, String ip) throws Exception {
        AdmInstantAlias.get(aliasId).addRecord(lServerId, ip);
        AdmInstantAlias.get(aliasId).getLservers();
        return this;
    }

    public TemplateModel FM_addAllDNSRecords(long aliasId) throws Exception {
        AdmInstantAlias.get(aliasId).createAllDNSRecords();
        return this;
    }

    public TemplateModel FM_getWebServers() throws Exception {
        Collection col = HostManager.getHostsByGroupType(1);
        col.addAll(HostManager.getHostsByGroupType(5));
        return new TemplateList(col);
    }

    public TemplateModel FM_getFreeSSLIpTags() throws Exception {
        List tags = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String psString = "SELECT DISTINCT flag FROM l_server_ips WHERE (";
        if (Session.getResellerId() == 1) {
            psString = psString + "flag = 2 OR ";
        }
        try {
            ps = con.prepareStatement(psString + "flag >= 10 AND flag < 1000) AND flag NOT IN (SELECT DISTINCT ssl_ip_tag FROM e_zones) ORDER BY flag");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                tags.add(rs.getString(1));
            }
            ListAdapter listAdapter = new ListAdapter(tags);
            Session.closeStatement(ps);
            con.close();
            return listAdapter;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getISSLZones() throws Exception {
        List sslZones = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM e_zones WHERE allow_ssl = ? AND share_ssl = ? AND reseller_id = ?");
                ps.setLong(1, 1L);
                ps.setLong(2, 1L);
                ps.setLong(3, 1L);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Session.getLog().debug("Inside getISSLZones. Got zone " + rs.getLong(1));
                    sslZones.add(AdmDNSZone.get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading SSL Zones", ex);
                Session.closeStatement(ps);
                con.close();
            }
            return sslZones;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setISSLUsagePermit(String permit, long zoneId) throws Exception {
        setISSLUsagePermit("1".equals(permit), zoneId);
        return new TemplateOKResult();
    }

    public void setISSLUsagePermit(boolean permit, long zoneId) throws Exception {
        setISSLUsagePermit(Session.getResellerId(), permit, zoneId);
    }

    public void setISSLUsagePermit(long resellerId, boolean permit, long zoneId) throws Exception {
        AdmDNSZone a = AdmDNSZone.get(zoneId);
        if (Session.getResellerId() != 1 && !a.shareSSL()) {
            throw new Exception("Nonauthorative attempt to set ISSLUsagePermit for zone " + a.getName());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if (permit) {
                ps = con.prepareStatement("INSERT INTO use_i_ssl(zone_id, reseller_id) VALUES (?, ?)");
                ps.setLong(1, zoneId);
                ps.setLong(2, resellerId);
            } else {
                ps = con.prepareStatement("DELETE FROM use_i_ssl WHERE reseller_id = ?");
                ps.setLong(1, resellerId);
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (permit) {
                if (!a.useISSL.contains(new Long(resellerId))) {
                    a.useISSL.add(new Long(resellerId));
                    return;
                }
                return;
            }
            a.useISSL.remove(new Long(resellerId));
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void regenAllSysCustomRecords() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM l_server");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long curId = rs.getLong("id");
                LogicalServer curLS = LogicalServer.get(curId);
                try {
                    curLS.addSysCustomRecords();
                } catch (Exception e) {
                    Session.getLog().error("Failed to re-generate system custom dns records for logical server id " + Long.toString(curId), e);
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

    public TemplateModel FM_regenAllSysCustomRecords() {
        try {
            regenAllSysCustomRecords();
            return new TemplateOKResult();
        } catch (Exception e) {
            Session.getLog().error("Failed to re-generate system dns records ", e);
            return new TemplateErrorResult(e.toString());
        }
    }

    public static void regenSysCustomRecordsForPs(long psId) throws Exception {
        PhysicalServer phServer = PhysicalServer.get(psId);
        for (String str : phServer.getLServers()) {
            long curLsId = Long.parseLong(str);
            LogicalServer curLS = LogicalServer.get(curLsId);
            try {
                curLS.addSysCustomRecords();
            } catch (Exception e) {
                Session.getLog().error("Failed to re-generate system custom dns records for logical server id " + Long.toString(curLsId), e);
            }
        }
    }

    public TemplateModel FM_regenSysCustomRecordsForPs(long psId) {
        try {
            regenSysCustomRecordsForPs(psId);
            return new TemplateOKResult();
        } catch (Exception e) {
            Session.getLog().error("Failed to re-generate system dns records ", e);
            return new TemplateErrorResult(e.toString());
        }
    }
}
