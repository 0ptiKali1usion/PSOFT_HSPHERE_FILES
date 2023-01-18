package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmCustomDNSRecord.class */
public class AdmCustomDNSRecord extends SharedObject implements TemplateHashModel {
    protected String data;
    protected long zoneId;
    protected String name;
    protected String type;
    protected String ttl;
    protected String pref;

    public AdmCustomDNSRecord(long id, String data, String name, long zoneId, String type, String ttl, String pref) {
        super(id);
        this.type = "A";
        this.ttl = "86400";
        this.pref = "";
        this.zoneId = zoneId;
        this.data = data;
        this.name = name;
        this.type = type;
        this.ttl = ttl;
        this.pref = pref;
    }

    public static AdmCustomDNSRecord create(long zoneId, String data, String name, String type, String ttl, String pref) throws Exception {
        String zone = AdmDNSZone.get(zoneId).getName();
        String name2 = (name == null || "".equals(name)) ? zone : name + "." + zone;
        long newId = Session.getNewIdAsLong();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                if ("MX".equals(type)) {
                    ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND data = ? AND type = 'MX'");
                    ps.setString(1, name2);
                    ps.setString(2, data);
                }
                if ("A".equals(type)) {
                    ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND type = 'A'");
                    ps.setString(1, name2);
                }
                if ("TXT".equals(type)) {
                    ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND type = 'TXT'");
                    ps.setString(1, name2);
                }
                if ("CNAME".equals(type)) {
                    ps = con.prepareStatement("SELECT name FROM dns_records WHERE name = ? AND type = 'CNAME'");
                    ps.setString(1, name2);
                }
                if (ps != null) {
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        throw new HSUserException("customdnsrecord.dns");
                    }
                }
                HostEntry host = AdmDNSZone.get(zoneId).getMaster();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO dns_records (id, name, type, ttl, pref, data) VALUES (?, ?, ?, ?, ?, ?)");
                ps2.setLong(1, newId);
                ps2.setString(2, name2);
                ps2.setString(3, type);
                ps2.setString(4, ttl);
                ps2.setString(5, pref);
                ps2.setString(6, data);
                ps2.executeUpdate();
                ps2.close();
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO e_dns_records(id, zone_id, type_rec) VALUES ( ?, ?, ?)");
                ps3.setLong(1, newId);
                ps3.setLong(2, zoneId);
                ps3.setInt(3, 2);
                ps3.executeUpdate();
                DNSServices.addToZone(host, zone, name2, type, ttl, data, pref);
                Session.closeStatement(ps3);
                con.close();
                return new AdmCustomDNSRecord(newId, data, name2, zoneId, type, ttl, pref);
            } catch (HSUserException hse) {
                Session.getLog().error("Unable to add duplicate AdmInstantAlias", hse);
                throw hse;
            } catch (Exception ex) {
                Session.closeStatement(null);
                PreparedStatement ps4 = con.prepareStatement("DELETE FROM e_dns_records WHERE id = ?");
                ps4.setLong(1, newId);
                ps4.executeUpdate();
                ps4.close();
                PreparedStatement ps5 = con.prepareStatement("DELETE FROM dns_records  WHERE id = ?");
                ps5.setLong(1, newId);
                ps5.executeUpdate();
                Session.getLog().error("Unable to add AdmInstantAlias", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static AdmCustomDNSRecord get(long id) throws Exception {
        AdmCustomDNSRecord tmp = (AdmCustomDNSRecord) get(id, AdmCustomDNSRecord.class);
        if (tmp != null) {
            return tmp;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT d.data, d.name, e.zone_id, d.type, d.ttl, d.pref FROM e_dns_records e, dns_records d WHERE e.id = d.id AND e.id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tmp = new AdmCustomDNSRecord(id, rs.getString(1), rs.getString(2), rs.getLong(3), rs.getString(4), rs.getString(5), rs.getString(6));
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
        HostEntry host = AdmDNSZone.get(this.zoneId).getMaster();
        String zone = AdmDNSZone.get(this.zoneId).getName();
        DNSServices.deleteFromZone(host, zone, this.name, this.type, this.pref, this.data);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM e_dns_records WHERE id = ?");
            ps2.setLong(1, this.f51id);
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM dns_records WHERE id = ?");
            ps.setLong(1, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            remove(this.f51id, AdmCustomDNSRecord.class);
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
        return "id".equals(key) ? new TemplateString(getId()) : "data".equals(key) ? new TemplateString(this.data) : "name".equals(key) ? new TemplateString(this.name) : "type".equals(key) ? new TemplateString(this.type) : "ttl".equals(key) ? new TemplateString(this.ttl) : "pref".equals(key) ? new TemplateString(this.pref) : super.get(key);
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
}
