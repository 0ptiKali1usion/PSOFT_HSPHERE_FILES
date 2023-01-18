package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmDNSRecord.class */
public class AdmDNSRecord extends SharedObject implements TemplateHashModel {

    /* renamed from: ip */
    protected String f167ip;
    protected long parentId;
    protected long zoneId;
    protected String name;
    protected static final String type = "A";
    protected static final String ttl = "86400";

    public AdmDNSRecord(long id, long parentId, String ip, String name, long zoneId) {
        super(id);
        this.parentId = parentId;
        this.zoneId = zoneId;
        this.f167ip = ip;
        this.name = name;
    }

    public String getIP() {
        return this.f167ip;
    }

    public static AdmDNSRecord create(long parentId, long zoneId, String ip, String name) throws Exception {
        String zone = AdmDNSZone.get(zoneId).getName();
        String name2 = "*." + name + "." + zone;
        long newId = Session.getNewIdAsLong();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                HostEntry host = AdmDNSZone.get(zoneId).getMaster();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO dns_records (id, name, type, ttl, data) VALUES (?, ?, ?, ?, ?)");
                ps2.setLong(1, newId);
                ps2.setString(2, name2);
                ps2.setString(3, type);
                ps2.setString(4, ttl);
                ps2.setString(5, ip);
                ps2.executeUpdate();
                ps2.close();
                ps = con.prepareStatement("INSERT INTO e_dns_records(id, alias_id, zone_id, type_rec) VALUES ( ?, ?, ?, ?)");
                ps.setLong(1, newId);
                ps.setLong(2, parentId);
                ps.setLong(3, zoneId);
                ps.setInt(4, 1);
                ps.executeUpdate();
                DNSServices.addToZone(host, zone, name2, type, ttl, ip, null);
                Session.closeStatement(ps);
                con.close();
                return new AdmDNSRecord(newId, parentId, ip, name2, zoneId);
            } catch (Exception ex) {
                Session.closeStatement(ps);
                PreparedStatement ps3 = con.prepareStatement("DELETE FROM e_dns_records WHERE id = ?");
                ps3.setLong(1, newId);
                ps3.executeUpdate();
                ps3.close();
                PreparedStatement ps4 = con.prepareStatement("DELETE FROM dns_records WHERE id = ?");
                ps4.setLong(1, newId);
                ps4.executeUpdate();
                Session.getLog().error("Unable to add AdmInstantAlias", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static AdmDNSRecord get(long id) throws Exception {
        AdmDNSRecord tmp = (AdmDNSRecord) get(id, AdmDNSRecord.class);
        if (tmp != null) {
            return tmp;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT e.alias_id, d.data, d.name, e.zone_id  FROM e_dns_records e, dns_records d  WHERE e.id = d.id AND e.id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tmp = new AdmDNSRecord(id, rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4));
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
        DNSServices.deleteFromZone(host, zone, this.name, type, null, this.f167ip);
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
            remove(this.f51id, AdmDNSRecord.class);
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
        return "id".equals(key) ? new TemplateString(getId()) : "ip".equals(key) ? new TemplateString(this.f167ip) : "name".equals(key) ? new TemplateString(this.name) : "type".equals(key) ? new TemplateString(type) : "ttl".equals(key) ? new TemplateString(ttl) : super.get(key);
    }

    public String getName() {
        return this.name;
    }
}
