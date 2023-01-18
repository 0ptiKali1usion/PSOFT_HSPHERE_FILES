package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmServiceDNSRecord.class */
public class AdmServiceDNSRecord extends SharedObject implements TemplateHashModel {
    protected String data;
    protected long zoneId;
    protected long lServerId;
    protected String name;
    protected String type;
    protected String ttl;
    protected String pref;

    public AdmServiceDNSRecord(long lServerId, long id, String data, String name, long zoneId, String type, String ttl, String pref) {
        super(id);
        this.type = "A";
        this.ttl = "86400";
        this.pref = "";
        this.lServerId = lServerId;
        this.zoneId = zoneId;
        this.data = data;
        this.name = name;
        this.type = type;
        this.ttl = ttl;
        this.pref = pref;
    }

    protected static void setIPCPSSL(HostEntry host, C0015IP ip) throws Exception {
        List list = new ArrayList();
        list.add("add");
        list.add(ip.toString());
        list.add(ip.getMask());
        host.exec("reseller-ssl-ip", list);
    }

    public static void releaseIPCPSSL(HostEntry host, String ip) throws Exception {
        Session.getLog().debug("in AdmServiceDNSRecord.releaseIPCPSSL()");
        List list = new ArrayList();
        list.add("del");
        list.add(ip);
        host.exec("reseller-ssl-ip", list);
    }

    public static AdmServiceDNSRecord create(long newId, long lServerId, long zoneId, String name, boolean isResellerSSL) throws Exception {
        String data;
        HostEntry host = HostManager.getHost(lServerId);
        try {
            if (isResellerSSL) {
                C0015IP ip = host.getExclusiveCPSSLIP(newId);
                setIPCPSSL(host, ip);
                data = ip.toString();
            } else {
                data = host.getIP().toString();
            }
            try {
                AdmServiceDNSRecord result = create(newId, lServerId, zoneId, data, name, "A", "86400", "", isResellerSSL);
                return result;
            } catch (Exception e) {
                releaseIPCPSSL(host, data);
                throw new HSUserException("Unable to create Service DNS record");
            }
        } catch (NullPointerException e2) {
            throw new HSUserException("No Service IP available for host " + host.getName());
        }
    }

    public static AdmServiceDNSRecord create(long lServerId, long zoneId, String name, boolean isResellerSSL) throws Exception {
        String data;
        HostEntry host = HostManager.getHost(lServerId);
        long newId = Session.getNewIdAsLong();
        if (host.getGroupType() == 2) {
            try {
                data = host.getExclusiveDNSIP(newId).toString();
            } catch (NullPointerException e) {
                throw new HSUserException("No Exclusive DNS IP available for host " + host.getName());
            }
        } else if (host.getGroupType() == 10 && isResellerSSL) {
            try {
                C0015IP ip = host.getExclusiveCPSSLIP(newId);
                setIPCPSSL(host, ip);
                data = ip.toString();
            } catch (NullPointerException e2) {
                throw new HSUserException("No Service IP available for host " + host.getName());
            }
        } else {
            try {
                data = host.getIP().toString();
            } catch (NullPointerException e3) {
                throw new HSUserException("No Service IP available for host " + host.getName());
            }
        }
        try {
            AdmServiceDNSRecord result = create(newId, lServerId, zoneId, data, name, "A", "86400", "", isResellerSSL);
            return result;
        } catch (Exception e4) {
            if (host.getGroupType() == 10 && isResellerSSL) {
                releaseIPCPSSL(host, data);
            } else if (host.getGroupType() == 2) {
                host.releaseDNSIP(data);
            }
            throw new HSUserException("Unable to create Service DNS record");
        }
    }

    public static AdmServiceDNSRecord create(long lServerId, long zoneId, String data, String name, String type, String ttl, String pref, boolean isResellerSSL) throws Exception {
        long newId = Session.getNewIdAsLong();
        if ("A".equals(type)) {
            HostEntry host = HostManager.getHost(lServerId);
            if (host.getGroupType() == 10 && isResellerSSL) {
                C0015IP ip = host.getCPSSLIPbyIP(data, newId);
                setIPCPSSL(host, ip);
            } else if (host.getGroupType() == 2) {
                host.getDNSIPbyIP(data, newId);
            }
        }
        try {
            AdmServiceDNSRecord result = create(newId, lServerId, zoneId, data, name, type, ttl, pref, isResellerSSL);
            return result;
        } catch (Exception e) {
            HostEntry host2 = HostManager.getHost(lServerId);
            if ("A".equals(type)) {
                if (host2.getGroupType() == 10 && isResellerSSL) {
                    releaseIPCPSSL(host2, data);
                } else if (host2.getGroupType() == 2) {
                    host2.releaseDNSIP(data);
                }
            }
            throw new HSUserException("Unable to create Service DNS record");
        }
    }

    public static AdmServiceDNSRecord create(long newId, long lServerId, long zoneId, String data, String name, String type, String ttl, String pref, boolean isResellerSSL) throws Exception {
        String name2;
        String zone = AdmDNSZone.get(zoneId).getName();
        if (name == null || "".equals(name)) {
            name2 = zone;
        } else {
            name2 = name + "." + zone;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                HostEntry host = AdmDNSZone.get(zoneId).getMaster();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO e_dns_records(id, zone_id, type_rec) VALUES ( ?, ?, ?)");
                ps2.setLong(1, newId);
                ps2.setLong(2, zoneId);
                if (isResellerSSL) {
                    ps2.setInt(3, 4);
                } else {
                    ps2.setInt(3, 3);
                }
                ps2.executeUpdate();
                ps2.close();
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO l_server_aliases(e_dns_rec_id, l_server_id, e_zone_id, prefix) VALUES ( ?, ?, ?, ?)");
                ps3.setLong(1, newId);
                ps3.setLong(2, lServerId);
                ps3.setLong(3, zoneId);
                ps3.setString(4, name2);
                ps3.executeUpdate();
                ps3.close();
                ps = con.prepareStatement("INSERT INTO dns_records (id, name, type, ttl, pref, data) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setLong(1, newId);
                ps.setString(2, name2);
                ps.setString(3, type);
                ps.setString(4, ttl);
                ps.setString(5, pref);
                ps.setString(6, data);
                ps.executeUpdate();
                DNSServices.addToZone(host, zone, name2, type, ttl, data, pref);
                Session.closeStatement(ps);
                con.close();
                return new AdmServiceDNSRecord(lServerId, newId, data, name2, zoneId, type, ttl, pref);
            } catch (Exception ex) {
                PreparedStatement ps4 = con.prepareStatement("DELETE FROM e_dns_records WHERE id = ?");
                ps4.setLong(1, newId);
                ps4.executeUpdate();
                PreparedStatement ps5 = con.prepareStatement("DELETE FROM dns_records  WHERE id = ?");
                ps5.setLong(1, newId);
                ps5.executeUpdate();
                PreparedStatement ps6 = con.prepareStatement("DELETE FROM l_server_aliases WHERE e_dns_rec_id = ?");
                ps6.setLong(1, newId);
                ps6.executeUpdate();
                Session.getLog().error("Unable to add AdmInstantAlias", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static AdmServiceDNSRecord get(long id) throws Exception {
        AdmServiceDNSRecord tmp = (AdmServiceDNSRecord) get(id, AdmServiceDNSRecord.class);
        if (tmp != null) {
            return tmp;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT d.data, d.name, e.zone_id, d.type, d.ttl, d.pref, l.l_server_id  FROM e_dns_records e, dns_records d, l_server_aliases l  WHERE e.id = d.id AND e.id = ? AND l.e_dns_rec_id = e.id");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tmp = new AdmServiceDNSRecord(rs.getLong(7), id, rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getString(6));
            }
            Session.closeStatement(ps);
            con.close();
            return tmp;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete() throws Exception {
        Session.getLog().debug("AdmServiceDNSRecord.delete()");
        try {
            HostEntry host = AdmDNSZone.get(this.zoneId).getMaster();
            String zone = AdmDNSZone.get(this.zoneId).getName();
            DNSServices.deleteFromZone(host, zone, this.name, this.type, this.pref, this.data);
        } catch (Exception ex) {
            Session.getLog().warn("Error deleting DNS record", ex);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip, flag FROM l_server_ips WHERE r_id = ? and l_server_id = ?");
            ps.setLong(1, this.f51id);
            ps.setLong(2, this.lServerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HostEntry hostIP = HostManager.getHost(this.lServerId);
                String ip = rs.getString(1);
                int flag = rs.getInt(2);
                if (flag == 6) {
                    hostIP.releaseDNSIP(ip);
                } else if (flag == 8) {
                    hostIP.releaseCPSSLIP(ip);
                    try {
                        releaseIPCPSSL(hostIP, ip);
                    } catch (Exception e) {
                        throw new HSUserException("Error releasing CP SSL IP");
                    }
                } else {
                    continue;
                }
            }
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM e_dns_records WHERE id = ?");
            ps2.setLong(1, this.f51id);
            ps2.executeUpdate();
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM dns_records WHERE id = ?");
            ps3.setLong(1, this.f51id);
            ps3.executeUpdate();
            PreparedStatement ps4 = con.prepareStatement("DELETE FROM l_server_aliases WHERE e_dns_rec_id = ?");
            ps4.setLong(1, this.f51id);
            ps4.executeUpdate();
            Session.closeStatement(ps4);
            con.close();
            remove(this.f51id, AdmServiceDNSRecord.class);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        try {
            if ("l_server".equals(key)) {
                return LogicalServer.get(this.lServerId);
            }
            return "data".equals(key) ? new TemplateString(this.data) : "name".equals(key) ? new TemplateString(this.name) : "type".equals(key) ? new TemplateString(this.type) : "ttl".equals(key) ? new TemplateString(this.ttl) : "pref".equals(key) ? new TemplateString(this.pref) : "zone_id".equals(key) ? new TemplateString(this.zoneId) : super.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public String getData() {
        return this.data;
    }

    public String getName() {
        return this.name;
    }

    public String getType() {
        return this.type;
    }

    public String getPref() {
        return this.pref;
    }

    public long getLServerId() {
        return this.lServerId;
    }

    public boolean equals(Object obj) {
        AdmServiceDNSRecord r = (AdmServiceDNSRecord) obj;
        return r.getId() == this.f51id;
    }
}
