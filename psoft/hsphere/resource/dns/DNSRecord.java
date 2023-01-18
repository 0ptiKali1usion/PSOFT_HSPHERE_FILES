package psoft.hsphere.resource.dns;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.UnixMixedIPResource;
import psoft.hsphere.resource.WinMixedIPResource;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/DNSRecord.class */
public abstract class DNSRecord extends Resource {
    protected String name;
    protected String type;
    protected String ttl;
    protected String data;
    protected String pref;

    public String getName() {
        return this.name;
    }

    public DNSRecord(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, type, ttl, pref, data FROM dns_records WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.type = rs.getString(2);
                this.ttl = rs.getString(3);
                this.pref = rs.getString(4);
                this.data = rs.getString(5);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public DNSRecord(int type, Collection values) throws Exception {
        super(type, values);
    }

    public void initDNS(Collection c) throws Exception {
        Iterator i = c.iterator();
        this.name = (String) i.next();
        this.type = (String) i.next();
        this.ttl = (String) i.next();
        this.data = (String) i.next();
        if (i.hasNext()) {
            this.pref = (String) i.next();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO dns_records (id, name, type, ttl, pref, data) VALUES (?, ?, ?, ?, ?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.name);
                ps.setString(3, this.type);
                ps.setString(4, this.ttl);
                ps.setString(5, this.pref);
                ps.setString(6, this.data);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                ResourceId dnsZone = recursiveGet("dns_zone");
                getLog().debug("DNSRecord.initDone() master" + dnsZone.get("master"));
                HostEntry host = dnsZone.get("master");
                String zone = dnsZone.get("name").toString();
                getLog().debug("DNSRecord.initDone() zone" + zone + host + zone + this.name + this.type + this.ttl + this.data + this.pref);
                DNSServices.addToZone(host, zone, this.name, this.type, this.ttl, this.data, this.pref);
            } catch (SQLException se) {
                getLog().warn("Error inserting new DNSRecord ", se);
                throw new Exception("Unable to add DNSRecord " + this.name + " " + this.type + " " + this.ttl + " " + this.pref + " " + this.data);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "name".equals(key) ? new TemplateString(this.name) : "type".equals(key) ? new TemplateString(this.type) : "ttl".equals(key) ? new TemplateString(this.ttl) : "data".equals(key) ? new TemplateString(this.data) : "pref".equals(key) ? new TemplateString(this.pref) : super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Exception causedException = null;
        if (this.initialized) {
            boolean skip = false;
            if ((this instanceof CustomDNSRecord) || (this instanceof DomainAliasADNSRecord)) {
                Resource r = getParent().get();
                if (r instanceof ServiceDNSZone) {
                    if (r.isDeletePrev()) {
                        skip = true;
                    }
                } else if ((r instanceof DNSZone) && r.isDeletePrev()) {
                    skip = true;
                }
            } else if (this instanceof DNSRecord) {
                Resource r2 = getParent().get();
                if ((r2 instanceof UnixMixedIPResource) || (r2 instanceof WinMixedIPResource)) {
                    if (r2.isDeletePrev()) {
                        skip = true;
                    }
                } else if ((r2 instanceof MailService) && r2.isDeletePrev()) {
                    skip = true;
                }
            }
            if (!skip) {
                try {
                    ResourceId dnsZone = recursiveGet("dns_zone");
                    HostEntry host = dnsZone.get("master");
                    String zone = dnsZone.get("name").toString();
                    DNSServices.deleteFromZone(host, zone, this.name, this.type, this.pref, this.data);
                } catch (Exception ex) {
                    Session.getLog().debug("Error deleteing the DNS records:", ex);
                    causedException = ex;
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM dns_records WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (causedException != null) {
                throw causedException;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static boolean testDuplicateRecords(String name, String type, String data) throws Exception {
        PreparedStatement ps;
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            if (type.equalsIgnoreCase("MX")) {
                Session.closeStatement(null);
                con.close();
                return false;
            }
            if (!type.equalsIgnoreCase("TXT") || data.indexOf("spf") <= -1) {
                ps = con.prepareStatement("SELECT  count(*) FROM dns_records WHERE name = ? AND type = ? AND id NOT IN (SELECT child_id FROM parent_child WHERE child_id = id and parent_type = 8)");
                ps.setString(1, name);
                ps.setString(2, type);
            } else {
                ps = con.prepareStatement("SELECT  count(*) FROM dns_records WHERE name = ? AND type = 'TXT' AND data LIKE 'spf%'");
                ps.setString(1, name);
            }
            ResultSet rs = ps2.executeQuery();
            if (rs.next() && rs.getInt(1) <= 1) {
                return false;
            }
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT COUNT(*) FROM dns_records, parent_child WHERE name = ? AND child_id = id");
            ps3.setString(1, name);
            ResultSet rs2 = ps3.executeQuery();
            if (rs2.next() && rs2.getInt(1) > 1) {
                Session.closeStatement(ps3);
                con.close();
                return true;
            }
            ps3.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT COUNT(*) FROM e_dns_records e, dns_records d WHERE d.name = ? AND e.id = d.id");
            ps4.setString(1, name);
            ResultSet rs3 = ps4.executeQuery();
            if (rs3.next() && rs3.getInt(1) > 1) {
                Session.closeStatement(ps4);
                con.close();
                return true;
            }
            ps4.close();
            Session.getLog().info("The records  has been swept");
            ps2 = con.prepareStatement("DELETE FROM dns_records WHERE name = ? AND NOT EXISTS (SELECT id FROM parent_child p WHERE p.child_id = dns_records.id)  AND NOT EXISTS (SELECT id FROM e_dns_records e WHERE e.id = dns_records.id)");
            ps2.setString(1, name);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            con.close();
            return false;
        } finally {
            Session.closeStatement(ps2);
            con.close();
        }
    }

    public void physicalCreate(String newData) throws Exception {
        ResourceId dnsZone = recursiveGet("dns_zone");
        getLog().debug("DNSRecord.initDone() master" + dnsZone.get("master"));
        HostEntry host = dnsZone.get("master");
        String zone = dnsZone.get("name").toString();
        getLog().debug("DNSRecord.initDone() zone" + zone + host + zone + this.name + this.type + this.ttl + this.data + this.pref);
        DNSServices.addToZone(host, zone, this.name, this.type, this.ttl, newData, this.pref);
        Session.getLog().debug("Updating dns_records DB");
        updateDbData(newData);
    }

    private void updateDbData(String newData) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE dns_records SET data = ? WHERE id = ?");
            ps.setString(1, newData);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.data = newData;
            Session.getLog().debug("DNS RECORDS IN DB HAS BEEN UPDATED id=" + getId().getId() + " data=" + this.data);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void physicalDelete(String oldData) throws Exception {
        ResourceId dnsZone = recursiveGet("dns_zone");
        HostEntry host = dnsZone.get("master");
        String zone = dnsZone.get("name").toString();
        DNSServices.deleteFromZone(host, zone, this.name, this.type, this.pref, oldData);
    }

    public String getData() {
        return this.data;
    }

    public static void removeNonDefaultDNSRecords(String name, String type, String idNew) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT d.id, pc.child_type FROM dns_records AS d INNER JOIN parent_child AS pc ON (d.id = pc.child_id) WHERE name = ? AND type = ? " + (idNew.equals("") ? "" : "AND id <> " + idNew));
            ps.setString(1, name);
            ps.setString(2, type);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Resource res = new ResourceId(rs.getInt(1), rs.getInt(2)).get();
                res.delete(true);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void removeNonDefaultMXRecords(String name, String data, String pref, String ttl) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT d.id, pc.child_type FROM dns_records AS d INNER JOIN parent_child AS pc ON (d.id = pc.child_id) WHERE type = 'MX' AND name = ? AND data = ? AND ttl = ? AND pref = ? ");
            ps.setString(1, name);
            ps.setString(2, data);
            ps.setString(3, ttl);
            ps.setString(4, pref);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Resource res = new ResourceId(rs.getInt(1), rs.getInt(2)).get();
                res.delete(true);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void removeNonDefaultTxtRecords(String name, String data) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT d.id, pc.child_type FROM dns_records AS d INNER JOIN parent_child AS pc ON (d.id = pc.child_id) WHERE type = 'TXT' AND name = ? AND data = ? ");
            ps.setString(1, name);
            ps.setString(2, data);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Resource res = new ResourceId(rs.getInt(1), rs.getInt(2)).get();
                res.delete(true);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
