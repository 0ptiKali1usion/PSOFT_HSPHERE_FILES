package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmDNSZone.class */
public class AdmDNSZone extends SharedObject implements TemplateHashModel {
    public static final int ALIASES_RECORDS = 1;
    public static final int CUSTOM_RECORDS = 2;
    public static final int SERVICE_RECORDS = 3;
    public static final int CP_SSL_RECORDS = 4;
    protected String name;
    protected String email;
    protected String refresh;
    protected String retry;
    protected String expire;
    protected String minimum;
    protected boolean allowHosting;
    protected boolean allowSSL;
    protected boolean shareSSL;
    protected int sslIpTag;
    protected List useISSL;
    protected long master;
    protected long slave1;
    protected long slave2;
    protected boolean initialized;
    protected boolean locked;
    protected long owner;
    protected List aliases;
    protected List cpAliases;
    protected List customRecords;
    protected List serviceRecords;
    protected static final int SLAVE_ABSENT = 0;
    public static final String sharedSSLPath = "/hsphere/local/config/httpd/ssl.shared/";

    public AdmDNSZone(long id, String name, String email, String refresh, String retry, String expire, String minimum, boolean allowHosting, boolean allowSSL, int sslIpTag, long master, long slave1, long slave2, boolean locked, long owner) {
        this(id, name, email, refresh, retry, expire, minimum, allowHosting, allowSSL, false, sslIpTag, master, slave1, slave2, locked, owner);
    }

    public AdmDNSZone(long id, String name, String email, String refresh, String retry, String expire, String minimum, boolean allowHosting, boolean allowSSL, boolean shareSSL, int sslIpTag, long master, long slave1, long slave2, boolean locked, long owner) {
        super(id);
        this.useISSL = null;
        this.aliases = new ArrayList();
        this.cpAliases = new ArrayList();
        this.customRecords = new ArrayList();
        this.serviceRecords = new ArrayList();
        this.name = name;
        this.email = email;
        this.refresh = refresh;
        this.retry = retry;
        this.expire = expire;
        this.minimum = minimum;
        this.allowHosting = allowHosting;
        this.allowSSL = allowSSL;
        this.sslIpTag = sslIpTag;
        this.master = master;
        this.slave1 = slave1;
        this.slave2 = slave2;
        this.locked = locked;
        this.shareSSL = shareSSL;
        this.owner = owner;
        try {
            loadAliases();
            loadCustomRecords();
            loadServiceRecords();
            loadUseISSL();
        } catch (Exception e) {
            Session.getLog().error("Error accured during loading Aliases", e);
        }
    }

    public boolean allowHosting() {
        return this.allowHosting;
    }

    public boolean allowSSL() {
        return this.allowSSL;
    }

    public boolean shareSSL() {
        return this.shareSSL;
    }

    public static List getHostsList() {
        try {
            return HostManager.getRandomHostsList(2);
        } catch (Exception e) {
            Session.getLog().warn("Error loading host list", e);
            return null;
        }
    }

    public static AdmDNSZone create(long newId, String nameZone, String email, boolean allowHosting, long master, long slave1, long slave2) throws Exception {
        Session.getLog().debug("Master :" + master + " Slave1:" + slave1 + " Slave2:" + slave2);
        String name = nameZone.toLowerCase();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            try {
                int containZoneId = DNSZone.testZoneName(nameZone, true);
                if (containZoneId == 0) {
                    throw new HSUserException("admdnszone.dns", new Object[]{nameZone});
                }
                if (containZoneId > 0) {
                    int zonesResellerId = DNSZone.getDNSZoneReseller(containZoneId);
                    if (Session.getResellerId() > 1 && Session.getResellerId() != zonesResellerId) {
                        throw new HSUserException("admdnszone.dns", new Object[]{nameZone});
                    }
                    if (Session.getResellerId() == 1 && zonesResellerId != 1) {
                        Session.addMessage("Warning: The " + DNSZone.getDNSZoneName(containZoneId) + " zone already exists.  It was created under the " + Reseller.getReseller(zonesResellerId).getUser() + " reseller.");
                    } else {
                        Session.addMessage("Warning: The " + DNSZone.getDNSZoneName(containZoneId) + " zone already exists.");
                    }
                }
                ps = con.prepareStatement("INSERT INTO e_zones (id, allow_hosting, allow_ssl, ssl_ip_tag, reseller_id, share_ssl) VALUES (?, ?, 0, 0, ?, 0)");
                ps.setLong(1, newId);
                ps.setInt(2, allowHosting ? 1 : 0);
                ps.setLong(3, Session.getResellerId());
                ps.executeUpdate();
                ps1 = con.prepareStatement("INSERT INTO dns_zones (id, name, email, refresh, retry, expire, minimum, master, slave1, slave2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps1.setLong(1, newId);
                ps1.setString(2, name);
                ps1.setString(3, email);
                ps1.setString(4, "10800");
                ps1.setString(5, C0004CP.INETADDRTTL);
                ps1.setString(6, "604800");
                ps1.setString(7, "86400");
                ps1.setLong(8, master);
                ps1.setLong(9, slave1);
                ps1.setLong(10, slave2);
                ps1.executeUpdate();
                HostEntry m = HostManager.getHost(master);
                HostEntry s1 = null;
                String ip1 = null;
                HostEntry s2 = null;
                String ip2 = null;
                if (slave1 != 0) {
                    s1 = HostManager.getHost(slave1);
                    ip1 = s1.getIP().toString();
                }
                if (slave2 != 0) {
                    s2 = HostManager.getHost(slave2);
                    ip2 = s2.getIP().toString();
                }
                DNSServices.createZone(m, name, m.getName(), email, m.getName(), s1 != null ? s1.getName() : null, ip1, s2 != null ? s2.getName() : null, ip2, "10800", C0004CP.INETADDRTTL, "604800", "86400");
                String ipm = m.getPServer().getPFirstIP();
                if (slave1 != 0) {
                    DNSServices.createSlaveZone(s1, name, ipm);
                }
                if (slave2 != 0) {
                    DNSServices.createSlaveZone(s2, name, ipm);
                }
                return new AdmDNSZone(newId, name, email, "10800", C0004CP.INETADDRTTL, "604800", "86400", allowHosting, false, 0, master, slave1, slave2, false, Session.getResellerId());
            } catch (Exception ex) {
                Session.getLog().error("Unsuccessful try to create DNS zone", ex);
                Session.closeStatement(null);
                PreparedStatement ps0 = con.prepareStatement("DELETE FROM dns_zones WHERE id = ?");
                ps0.setLong(1, newId);
                ps0.executeUpdate();
                Session.closeStatement(ps);
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM e_zones WHERE id = ?");
                ps2.setLong(1, newId);
                ps2.executeUpdate();
                throw ex;
            }
        } finally {
            Session.closeStatement(null);
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
        }
    }

    public void postIPChanges(boolean force) throws Exception {
        HostEntry m = HostManager.getHost(this.master);
        HostEntry s1 = null;
        HostEntry s2 = null;
        if (this.slave1 != 0) {
            s1 = HostManager.getHost(this.slave1);
        }
        if (this.slave2 != 0) {
            s2 = HostManager.getHost(this.slave2);
        }
        C0015IP ipm = m.getIP();
        if (ipm == null) {
            throw new Exception("Can't find Service IP for DNS host " + m.getName());
        }
        if (this.slave1 != 0) {
            DNSServices.updateSlaveZone(s1, this.name, ipm.toString(), force);
        }
        if (this.slave2 != 0) {
            DNSServices.updateSlaveZone(s2, this.name, ipm.toString(), force);
        }
    }

    public static AdmDNSZone get(long id) throws Exception {
        AdmDNSZone tmp = (AdmDNSZone) get(id, AdmDNSZone.class);
        if (tmp != null) {
            return tmp;
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT dns_zones.name, dns_zones.email, dns_zones.refresh,dns_zones.retry, dns_zones.expire, dns_zones.minimum,allow_hosting, allow_ssl, ssl_ip_tag,dns_zones.master, slave1, slave2, e_zones.r_id, share_ssl, reseller_id FROM dns_zones, e_zones WHERE dns_zones.id = e_zones.id AND dns_zones.id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AdmDNSZone admDNSZone = new AdmDNSZone(id, rs.getString(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7) == 1, rs.getInt(8) == 1, rs.getInt("share_ssl") == 1, rs.getInt(9), rs.getLong(10), rs.getLong(11), rs.getLong(12), rs.getLong(13) != 0, rs.getLong("reseller_id"));
                Session.closeStatement(ps);
                con.close();
                return admDNSZone;
            }
            Session.getLog().error("Unable to get AdmDNSZone with id=" + id);
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static void remove(long id) throws Exception {
        remove(id, AdmDNSZone.class);
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(this.f51id);
        }
        if (key.equals("name")) {
            return new TemplateString(this.name);
        }
        if (key.equals("email")) {
            return new TemplateString(this.email);
        }
        if (key.equals("refresh")) {
            return new TemplateString(this.refresh);
        }
        if (key.equals("retry")) {
            return new TemplateString(this.retry);
        }
        if (key.equals("expire")) {
            return new TemplateString(this.expire);
        }
        if (key.equals("minimum")) {
            return new TemplateString(this.minimum);
        }
        if (key.equals("locked")) {
            return new TemplateString(this.locked);
        }
        if (key.equals("aliases")) {
            return new TemplateList(this.aliases);
        }
        if (key.equals("cpaliases")) {
            return new TemplateList(this.cpAliases);
        }
        if (key.equals("c_records")) {
            return new TemplateList(this.customRecords);
        }
        if (key.equals("s_records")) {
            return new TemplateList(this.serviceRecords);
        }
        if (key.equals("is_i_ssl_allowed")) {
            return new TemplateString(usingISSLisAllowed());
        }
        if (key.equals("allow_hosting")) {
            return new TemplateString(this.allowHosting);
        }
        if (key.equals("allow_ssl")) {
            return new TemplateString(this.allowSSL);
        }
        if (key.equals("sslIpTag")) {
            return new TemplateString(this.sslIpTag);
        }
        if (key.equals("share_ssl")) {
            return new TemplateString(shareSSL());
        }
        try {
            if (key.equals("master")) {
                return HostManager.getHost(this.master);
            }
            if ("slave1".equals(key) && this.slave1 != 0) {
                return HostManager.getHost(this.slave1);
            }
            if ("slave2".equals(key) && this.slave2 != 0) {
                return HostManager.getHost(this.slave2);
            }
            return super.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public String getDomainName() {
        return this.name;
    }

    public List getAliases() {
        return this.aliases;
    }

    public List getCpAliases() {
        return this.cpAliases;
    }

    public List getCustomRecords() {
        return this.customRecords;
    }

    public void delete() throws Exception {
        for (AdmInstantAlias als : this.aliases) {
            als.delete();
        }
        for (ResellerCpAlias als2 : this.cpAliases) {
            als2.delete();
        }
        for (AdmCustomDNSRecord cRec : this.customRecords) {
            cRec.delete();
        }
        for (AdmServiceDNSRecord cRec2 : this.serviceRecords) {
            cRec2.delete();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            DNSServices.deleteZone(HostManager.getHost(this.master), this.name);
            if (Session.getPropertyString("MYDNS_USER").equals("")) {
                if (this.slave1 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(this.slave1), this.name);
                }
                if (this.slave2 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(this.slave2), this.name);
                }
            }
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM dns_zones WHERE id = ?");
            ps2.setLong(1, this.f51id);
            ps2.executeUpdate();
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM e_zones WHERE id = ?");
            ps3.setLong(1, this.f51id);
            ps3.executeUpdate();
            ps = con.prepareStatement("DELETE FROM use_i_ssl WHERE zone_id = ?");
            ps.setLong(1, this.f51id);
            ps.executeUpdate();
            ps.close();
            Session.closeStatement(ps);
            con.close();
            remove(this.f51id);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void loadAliases() throws Exception {
        this.aliases.clear();
        this.cpAliases.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id,tag FROM e_ialiases WHERE zone_id = ?");
                ps.setLong(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    AdmInstantAlias als = AdmInstantAlias.get(rs.getLong("id"));
                    if (als instanceof ResellerCpAlias) {
                        this.cpAliases.add(als);
                    } else {
                        this.aliases.add(als);
                    }
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading Aliasses", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void loadCustomRecords() throws Exception {
        this.customRecords.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT e.id FROM e_dns_records e, dns_records d WHERE  e.zone_id = ? AND e.alias_id IS NULL AND e.id = d.id AND e.type_rec = 2 ORDER BY d.type ");
                ps.setLong(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    this.customRecords.add(AdmCustomDNSRecord.get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading custom records", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void loadServiceRecords() throws Exception {
        this.serviceRecords.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT e.id FROM e_dns_records e, dns_records d WHERE  e.zone_id = ? AND e.alias_id IS NULL AND e.id = d.id AND (e.type_rec = 3 OR e.type_rec = 4) ORDER BY d.type ");
                ps.setLong(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    this.serviceRecords.add(AdmServiceDNSRecord.get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading custom records", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void loadUseISSL() throws Exception {
        PreparedStatement ps = null;
        List r = new ArrayList();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT reseller_id FROM use_i_ssl WHERE zone_id = ?");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                r.add(new Long(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Exception e) {
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
        this.useISSL = r;
    }

    public AdmCustomDNSRecord addCustRecord(String data, String name, String type, String ttl, String pref) throws Exception {
        AdmCustomDNSRecord cRec = AdmCustomDNSRecord.create(getId(), data, name, type, ttl, pref);
        this.customRecords.add(cRec);
        return cRec;
    }

    public void delCustomRecord(long id) throws Exception {
        AdmCustomDNSRecord cRec = AdmCustomDNSRecord.get(id);
        if (cRec == null) {
            return;
        }
        if (this.customRecords.contains(cRec)) {
            cRec.delete();
            this.customRecords.remove(cRec);
            return;
        }
        throw new Exception("This custom record doesn`t belong to this zone.");
    }

    public void delServiceRecord(long id) throws Exception {
        delServiceRecord(id, false);
    }

    public void delServiceRecord(long id, boolean isResellerSSL) throws Exception {
        loadServiceRecords();
        AdmServiceDNSRecord cRec = AdmServiceDNSRecord.get(id);
        if (cRec == null) {
            return;
        }
        if (this.serviceRecords.contains(cRec)) {
            if (isResellerSSL) {
                HostEntry hostIP = HostManager.getHost(cRec.getLServerId());
                AdmServiceDNSRecord.releaseIPCPSSL(hostIP, cRec.getData());
            }
            cRec.delete();
            this.serviceRecords.remove(cRec);
            return;
        }
        throw new Exception("This service record " + id + " doesn`t belong to zone " + getId());
    }

    public AdmInstantAlias addAllias(String prefix, int tag) throws Exception {
        AdmInstantAlias als = AdmInstantAlias.create(this.f51id, prefix, tag);
        this.aliases.add(als);
        return als;
    }

    public ResellerCpAlias addCpAlias(String prefix) throws Exception {
        ResellerCpAlias als = (ResellerCpAlias) AdmInstantAlias.create(this.f51id, prefix, 4);
        this.cpAliases.add(als);
        return als;
    }

    public void delAllias(long id) throws Exception {
        AdmInstantAlias als = AdmInstantAlias.get(id);
        if (als == null) {
            return;
        }
        if (this.aliases.contains(als)) {
            als.delete();
            this.aliases.remove(als);
            return;
        }
        throw new Exception("This alias doesn`t belong to this zone.");
    }

    public void delCpAlias(long id) throws Exception {
        ResellerCpAlias als = (ResellerCpAlias) ResellerCpAlias.get(id);
        if (als == null) {
            return;
        }
        if (this.cpAliases.contains(als)) {
            als.delete();
            this.cpAliases.remove(als);
            return;
        }
        throw new Exception("This alias doesn`t belong to this zone.");
    }

    public AdmServiceDNSRecord addServiceRecord(String data, String name, String type, String ttl, String pref, long lServerId, boolean isResellerSSL) throws Exception {
        AdmServiceDNSRecord cRec = AdmServiceDNSRecord.create(lServerId, getId(), data, name, type, ttl, pref, isResellerSSL);
        this.serviceRecords.add(cRec);
        return cRec;
    }

    public AdmServiceDNSRecord addServiceRecord(String data, String name, long lServerId, boolean isResellerSSL) throws Exception {
        AdmServiceDNSRecord cRec = AdmServiceDNSRecord.create(lServerId, getId(), name, isResellerSSL);
        this.serviceRecords.add(cRec);
        return cRec;
    }

    public AdmServiceDNSRecord addServiceRecord(String name, long lServerId, long newId, boolean isResellerSSL) throws Exception {
        AdmServiceDNSRecord cRec = AdmServiceDNSRecord.create(newId, lServerId, getId(), name, isResellerSSL);
        this.serviceRecords.add(cRec);
        return cRec;
    }

    public HostEntry getMaster() throws Exception {
        return HostManager.getHost(this.master);
    }

    public String getName() {
        return this.name;
    }

    public int getIpTag() {
        return this.sslIpTag;
    }

    public void setAllowHosting(boolean allowHosting) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE e_zones SET allow_hosting = ? WHERE id = ?");
            ps.setInt(1, allowHosting ? 1 : 0);
            ps.setLong(2, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.allowHosting = allowHosting;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setAllowSSL(boolean allowSSL, int sslIpTag) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE e_zones SET allow_ssl = ?, ssl_ip_tag = ? WHERE id = ?");
            ps.setInt(1, allowSSL ? 1 : 0);
            ps.setInt(2, sslIpTag);
            ps.setLong(3, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.allowSSL = allowSSL;
            this.sslIpTag = sslIpTag;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setShareSSL(boolean shareSSL) throws Exception {
        if (!allowSSL()) {
            throw new Exception("SSL is not allowed for zone " + this.name);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE e_zones SET share_ssl = ? WHERE id = ?");
            ps.setInt(1, shareSSL ? 1 : 0);
            ps.setLong(2, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.shareSSL = shareSSL;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setShareSSL(String shareSSL) throws Exception {
        setShareSSL("1".equals(shareSSL));
        return new TemplateOKResult();
    }

    public static List getZones() throws Exception {
        List zones = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM e_zones WHERE reseller_id=?");
                ps.setLong(1, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    zones.add(get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading Zones", ex);
                Session.closeStatement(ps);
                con.close();
            }
            return zones;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getFreeZones() throws Exception {
        List freeZones = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM e_zones WHERE r_id IS NULL AND reseller_id=?");
                ps.setLong(1, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    freeZones.add(get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading free Zones", ex);
                Session.closeStatement(ps);
                con.close();
            }
            return freeZones;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getHostedZones() throws Exception {
        List hostedZones = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM e_zones WHERE allow_hosting = 1 AND reseller_id = ?");
                ps.setLong(1, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    hostedZones.add(get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading free Zones", ex);
                Session.closeStatement(ps);
                con.close();
            }
            return hostedZones;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List getSSLZones() throws Exception {
        List sslZones = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT id FROM e_zones WHERE allow_ssl = 1 AND reseller_id = ?");
                ps.setLong(1, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    sslZones.add(get(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error loading SSL Zones", ex);
                Session.closeStatement(ps);
                con.close();
            }
            List iSSLZones = AdmDNSManager.getISSLZones();
            for (int i = 0; i < iSSLZones.size(); i++) {
                AdmDNSZone z = (AdmDNSZone) iSSLZones.get(i);
                if (z.usingISSLisAllowed()) {
                    sslZones.add(z);
                }
            }
            return sslZones;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static AdmDNSZone getByName(String zoneName) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT e_zones.id FROM dns_zones, e_zones WHERE name = ? AND e_zones.id = dns_zones.id");
            ps.setString(1, zoneName);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                Session.getLog().error("AdmDNSZone for name=" + zoneName + " not found");
                Session.closeStatement(ps);
                con.close();
                return null;
            }
            AdmDNSZone a = get(rs.getLong(1));
            if (a.getOwner() == Session.getResellerId() || Session.getResellerId() == 1 || (a.shareSSL() && a.usingISSLisAllowed())) {
                return a;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public static AdmDNSZone getByRid(ResourceId rid) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM e_zones WHERE r_id = ? AND r_type = ? AND reseller_id = ?");
            ps.setLong(1, rid.getId());
            ps.setInt(2, rid.getType());
            ps.setLong(3, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                AdmDNSZone admDNSZone = get(rs.getLong(1));
                Session.closeStatement(ps);
                con.close();
                return admDNSZone;
            }
            Session.getLog().error("AdmDNSZone for rid=" + rid.toString() + " not found");
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public boolean isLocked() {
        return this.locked;
    }

    public void lock(ResourceId rid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE e_zones SET r_id = ?, r_type = ? WHERE id = ?");
            ps.setLong(1, rid.getId());
            ps.setInt(2, rid.getType());
            ps.setLong(3, getId());
            ps.executeUpdate();
            this.locked = true;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void unlock() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE e_zones SET r_id = ?, r_type=? WHERE id = ?");
            ps.setNull(1, 4);
            ps.setNull(2, 4);
            ps.setLong(3, getId());
            ps.executeUpdate();
            this.locked = false;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List getServiceRecords() {
        return this.serviceRecords;
    }

    public TemplateModel FM_installCertificate(long serverId, String file, String key, int tag) throws Exception {
        HostEntry he = HostManager.getHost(serverId);
        if (he instanceof WinHostEntry) {
            installIIS((WinHostEntry) he, file, key, tag);
        } else {
            installApache(he, file, "server.crt");
            installApache(he, key, "server.key");
            commitActionApache(he, "commit_pair");
            restartApache(he);
        }
        he.setSharedSSL(this.f51id, true);
        return this;
    }

    public TemplateModel FM_installCertificateOnly(long serverId, String file) throws Exception {
        HostEntry he = HostManager.getHost(serverId);
        if (he instanceof WinHostEntry) {
            updateIIS((WinHostEntry) he, file);
        } else {
            installApache(he, file, "server.crt");
            commitActionApache(he, "commit_cert");
            restartApache(he);
        }
        return this;
    }

    public TemplateModel FM_installCertFile(long serverId, String file) throws Exception {
        HostEntry he = HostManager.getHost(serverId);
        if (he instanceof WinHostEntry) {
            updateIISca((WinHostEntry) he, file);
        } else {
            installApache(he, file, "ca-bundle.crt");
        }
        he.setSharedSSLca(this.f51id, true);
        return this;
    }

    public TemplateModel FM_removeCertFile(long serverId) throws Exception {
        HostEntry he = HostManager.getHost(serverId);
        he.setSharedSSLca(this.f51id, false);
        return this;
    }

    protected void installApache(HostEntry he, String file, String filename) throws Exception {
        if (file == null || "".equals(file)) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
        List l = new ArrayList();
        l.add("root");
        l.add(sharedSSLPath + this.name);
        l.add(filename);
        Iterator i = he.exec("ssl-install", l, file).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
    }

    protected void commitActionApache(HostEntry he, String action) throws Exception {
        List l = new ArrayList();
        l.add("root");
        l.add(sharedSSLPath + this.name);
        l.add(action);
        Iterator i = he.exec("ssl-install", l).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate.different");
        }
    }

    protected void restartApache(HostEntry he) throws Exception {
        Session.getLog().info("Before synch");
        synchronized (he) {
            Session.getLog().info("After synch");
            List l = new ArrayList();
            l.add("force");
            he.exec("apache-reconfig", l);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    protected void installIIS(WinHostEntry he, String file, String key, int tag) throws Exception {
        Session.getLog().debug("Inside AdmDNSZone::installIIS");
        C0015IP ip = he.getSharedIP(tag);
        Iterator i = he.exec("ssl-sharedcreate.asp", (String[][]) new String[]{new String[]{"domain", this.name}, new String[]{MerchantGatewayManager.MG_KEY_PREFIX, key}, new String[]{MerchantGatewayManager.MG_CERT_PREFIX, file}, new String[]{"ip", ip.toString()}}).iterator();
        if (i.hasNext()) {
            String error = (String) i.next();
            Session.getLog().debug("ERROR updating IIS SSL" + error);
            if ("IK".equals(error)) {
                throw new HSUserException("sslresource.certificate.key");
            }
            if ("IC".equals(error)) {
                throw new HSUserException("sslresource.certificate.certificat");
            }
            if ("DK".equals(error)) {
                throw new HSUserException("sslresource.certificate.different");
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    protected void updateIIS(WinHostEntry he, String file) throws Exception {
        Iterator i = he.exec("ssl-sharedupdate.asp", (String[][]) new String[]{new String[]{"domain", this.name}, new String[]{MerchantGatewayManager.MG_CERT_PREFIX, file}}).iterator();
        if (i.hasNext()) {
            String error = (String) i.next();
            Session.getLog().debug("ERROR updating IIS SSL" + error);
            if ("IK".equals(error)) {
                throw new HSUserException("sslresource.certificate.key");
            }
            if ("IC".equals(error)) {
                throw new HSUserException("sslresource.certificate.certificat");
            }
            if ("DK".equals(error)) {
                throw new HSUserException("sslresource.certificate.different");
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    protected void updateIISca(WinHostEntry he, String file) throws Exception {
        Iterator i = he.exec("ssl-authoritycreate.asp", (String[][]) new String[]{new String[]{"authority", file}}).iterator();
        if (i.hasNext()) {
            String error = (String) i.next();
            Session.getLog().debug("ERROR updating IIS SSL" + error);
            if ("IC".equals(error)) {
                throw new HSUserException("sslresource.certificate.certificat");
            }
        }
    }

    public TemplateModel FM_enableSSL(int sslIpTag) throws Exception {
        setAllowSSL(true, sslIpTag);
        return this;
    }

    public TemplateModel FM_disableSSL() throws Exception {
        setAllowSSL(false, 0);
        unmarkServers();
        return this;
    }

    public List installedServers() throws Exception {
        List servers = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT l_server_id FROM shared_ssl_hosts WHERE zone_id = ? ORDER BY l_server_id");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                servers.add(HostManager.getHost(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return servers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean checkServer(HostEntry he) throws Exception {
        for (HostEntry host : installedServers()) {
            if (host.getId() == he.getId()) {
                return true;
            }
        }
        return false;
    }

    public TemplateModel FM_installedServers() throws Exception {
        return new ListAdapter(installedServers());
    }

    protected void unmarkServers() throws Exception {
        for (HostEntry he : installedServers()) {
            he.setSharedSSL(this.f51id, false);
        }
    }

    public long getMasterId() {
        return this.master;
    }

    public long getSlave1Id() {
        return this.slave1;
    }

    public long getSlave2Id() {
        return this.slave2;
    }

    public String getEmail() {
        return this.email;
    }

    public boolean usingISSLisAllowed() {
        try {
            return usingISSLisAllowed(Session.getResellerId());
        } catch (Exception ex) {
            Session.getLog().error("Unable to get reseller Id ", ex);
            return false;
        }
    }

    public boolean usingISSLisAllowed(long resellerId) {
        return this.useISSL.contains(new Long(resellerId));
    }

    public long getOwner() {
        return this.owner;
    }
}
