package psoft.hsphere.resource.dns;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.C0015IP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/DNSZone.class */
public class DNSZone extends Resource {
    protected String name;
    protected String email;
    protected String refresh;
    protected String retry;
    protected String expire;
    protected String minimum;
    protected long master;
    protected long slave1;
    protected long slave2;
    public static final int SLAVE_ABSENT = 0;
    static int dom_alias_aResType;

    static {
        dom_alias_aResType = -1;
        try {
            dom_alias_aResType = Integer.parseInt(TypeRegistry.getTypeId("domain_alias_a_record"));
        } catch (Exception ex) {
            Session.getLog().error("Unable to get an id of the 'domain_alias_a_record' resource type.", ex);
        }
    }

    public DNSZone(ResourceId id) throws Exception {
        super(id);
        getLog().info("Got a Zone record " + id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, email, refresh, retry, expire, minimum, master, slave1, slave2 FROM dns_zones WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.email = rs.getString(2);
                this.refresh = rs.getString(3);
                this.retry = rs.getString(4);
                this.expire = rs.getString(5);
                this.minimum = rs.getString(6);
                this.master = rs.getLong(7);
                this.slave1 = rs.getLong(8);
                this.slave2 = rs.getLong(9);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public DNSZone(int type, Collection values) throws Exception {
        super(type, values);
        getLog().info("Creating new Zone record");
        Iterator i = values.iterator();
        this.name = ((String) i.next()).toLowerCase().trim();
        assignNameServers();
    }

    protected void createDNS() throws Exception {
        HostEntry m = HostManager.getHost(this.master);
        HostEntry s1 = null;
        String ip1 = null;
        HostEntry s2 = null;
        String ip2 = null;
        if (this.slave1 != 0) {
            s1 = HostManager.getHost(this.slave1);
            C0015IP ips1 = s1.getIP();
            if (ips1 == null) {
                throw new Exception("Can't find Service IP for DNS host " + s1.getName());
            }
            ip1 = ips1.toString();
        }
        if (this.slave2 != 0) {
            s2 = HostManager.getHost(this.slave2);
            C0015IP ips2 = s2.getIP();
            if (ips2 == null) {
                throw new Exception("Can't find Service IP for DNS host " + s2.getName());
            }
            ip2 = ips2.toString();
        }
        String ipm = m.getPServer().getPFirstIP();
        if (ipm == null) {
            throw new Exception("Can't find Service IP for DNS host " + m.getName());
        }
        DNSServices.createZone(m, this.name, m.getName(), this.email, m.getName(), s1 != null ? s1.getName() : null, ip1, s2 != null ? s2.getName() : null, ip2, this.refresh, this.retry, this.expire, this.minimum);
        if (this.slave1 != 0) {
            DNSServices.createSlaveZone(s1, this.name, ipm.toString());
        }
        if (this.slave2 != 0) {
            DNSServices.createSlaveZone(s2, this.name, ipm.toString());
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
        String ipm = m.getPServer().getPFirstIP();
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

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() {
        return new Object[]{this.name};
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM dns_zones WHERE name = ?");
                ps.setString(1, this.name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("DNS Zone exists: " + this.name);
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO dns_zones (id, name, email, refresh, retry, expire, minimum, master, slave1, slave2) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.name);
                ps2.setString(3, this.email);
                ps2.setString(4, this.refresh);
                ps2.setString(5, this.retry);
                ps2.setString(6, this.expire);
                ps2.setString(7, this.minimum);
                ps2.setLong(8, this.master);
                ps2.setLong(9, this.slave1);
                ps2.setLong(10, this.slave2);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                createDNS();
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting zone ", e);
                throw new HSUserException("dnszone.exists");
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void assignNameServers() throws Exception {
        if (testZoneName() > -1) {
            throw new HSUserException("dnszone.taken", new Object[]{this.name});
        }
        Iterator i = HostManager.getRandomHostsList(2).iterator();
        this.master = ((HostEntry) i.next()).getId();
        this.slave1 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
        this.slave2 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
        this.refresh = "10800";
        this.retry = C0004CP.INETADDRTTL;
        this.expire = "604800";
        this.minimum = "86400";
        String hostmaster = Settings.get().getValue("hostmaster");
        if (hostmaster == null) {
            this.email = "hostmaster.change-me-asap.com";
        } else {
            this.email = hostmaster.toString();
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(this.name);
        }
        if ("email".equals(key)) {
            return new TemplateString(this.email);
        }
        if ("refresh".equals(key)) {
            return new TemplateString(this.refresh);
        }
        if ("retry".equals(key)) {
            return new TemplateString(this.retry);
        }
        if ("expire".equals(key)) {
            return new TemplateString(this.expire);
        }
        if ("minimum".equals(key)) {
            return new TemplateString(this.minimum);
        }
        try {
            if (!"origin".equals(key) && !"master".equals(key)) {
                if ("slave1".equals(key) && this.slave1 != 0) {
                    return HostManager.getHost(this.slave1);
                }
                if ("slave2".equals(key) && this.slave2 != 0) {
                    return HostManager.getHost(this.slave2);
                }
                return super.get(key);
            }
            return HostManager.getHost(this.master);
        } catch (Exception e) {
            return null;
        }
    }

    @Override // psoft.hsphere.Resource
    public void deletePrev() {
        if (Session.getPropertyString("MYDNS_USER").equals("")) {
            this.deletePrev = true;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            try {
                DNSServices.deleteZone(HostManager.getHost(this.master), this.name);
                if (this.slave1 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(this.slave1), this.name);
                }
                if (this.slave2 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(this.slave2), this.name);
                }
            } catch (Exception e) {
                Session.getLog().warn(e);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM dns_zones WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int testZoneName() throws Exception {
        return testZoneName(this.name, true);
    }

    public static int testZoneName(String name, boolean isRecursive) throws SQLException {
        int containZoneId = -1;
        Session.getLog().debug("DNSZone testZoneName recursive " + isRecursive);
        String realZoneName = name;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        while (true) {
            try {
                if (realZoneName.indexOf(".") < 0) {
                    break;
                }
                ps = con.prepareStatement("SELECT name, id FROM dns_zones WHERE name = ?");
                ps.setString(1, realZoneName);
                ps.executeQuery();
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Session.getLog().debug("Real zone name = " + realZoneName + " Zone name " + name);
                    containZoneId = name.equals(realZoneName) ? 0 : rs.getInt("id");
                } else if (!isRecursive) {
                    break;
                } else {
                    realZoneName = realZoneName.substring(realZoneName.indexOf(".") + 1, realZoneName.length());
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        if (containZoneId == -1) {
            return containZoneId;
        }
        ps.close();
        PreparedStatement ps2 = con.prepareStatement("SELECT  count(*) FROM parent_child p, dns_zones d WHERE d.name = ? AND p.child_id = d.id");
        ps2.setString(1, realZoneName);
        ResultSet rs2 = ps2.executeQuery();
        if (rs2.next() && rs2.getInt(1) > 0) {
            int i = containZoneId;
            Session.closeStatement(ps2);
            con.close();
            return i;
        }
        ps2.close();
        PreparedStatement ps3 = con.prepareStatement("SELECT  count(*) FROM e_zones e, dns_zones d WHERE d.name = ? AND e.id = d.id");
        ps3.setString(1, realZoneName);
        ResultSet rs3 = ps3.executeQuery();
        if (rs3.next() && rs3.getInt(1) > 0) {
            int i2 = containZoneId;
            Session.closeStatement(ps3);
            con.close();
            return i2;
        }
        ps3.close();
        Session.getLog().info("The zone " + realZoneName + " has been swept");
        ps = con.prepareStatement("DELETE FROM dns_zones WHERE name = ?");
        ps.setString(1, realZoneName);
        ps.executeUpdate();
        Session.closeStatement(ps);
        con.close();
        if (isRecursive) {
            return testZoneName(name, isRecursive);
        }
        return -1;
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("dns_zone.desc", new Object[]{getName()});
    }

    public String getName() {
        return this.name;
    }

    public ArrayList getNameServers() throws Exception {
        ArrayList nservers = new ArrayList();
        nservers.add(HostManager.getHost(this.master));
        if (this.slave1 != 0) {
            nservers.add(HostManager.getHost(this.slave1));
        }
        if (this.slave2 != 0) {
            nservers.add(HostManager.getHost(this.slave2));
        }
        return nservers;
    }

    public static int getDNSZoneReseller(int DNSZoneId) throws SQLException {
        int resellerId = -1;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT reseller_id FROM e_zones WHERE id = ?");
            ps.setInt(1, DNSZoneId);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                resellerId = rs.getInt("reseller_id");
            } else {
                ps.close();
                ps = con.prepareStatement("SELECT a.reseller_id FROM accounts a, parent_child p WHERE p.child_id = ? AND p.account_id = a.id");
                ps.setInt(1, DNSZoneId);
                ps.executeQuery();
                ResultSet rs2 = ps.executeQuery();
                if (rs2.next()) {
                    resellerId = rs2.getInt("reseller_id");
                }
            }
            Session.closeStatement(ps);
            con.close();
            return resellerId;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static String getDNSZoneName(int DNSZoneId) throws SQLException {
        String DNSZoneName = "";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM dns_zones WHERE id = ?");
            ps.setInt(1, DNSZoneId);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                DNSZoneName = rs.getString("name");
            }
            Session.closeStatement(ps);
            con.close();
            return DNSZoneName;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void restoreDefaultARecords() throws Exception {
        String domainName = getParent().get("name").toString();
        try {
            Collection initValues = _getPlan().getDefaultInitValues(this, dom_alias_aResType, "wildcard");
            DomainAliasADNSRecord ar = new DomainAliasADNSRecord(dom_alias_aResType, initValues);
            DNSRecord.removeNonDefaultDNSRecords(ar.getPrefix() + domainName, "A", "");
            addChild("domain_alias_a_record", "wildcard", new ArrayList());
        } catch (Exception e) {
            Session.getLog().error("Error occured while restoring default wildcard A record for " + domainName, e);
        }
        try {
            DNSRecord.removeNonDefaultDNSRecords(domainName, "A", "");
            addChild("domain_alias_a_record", "", new ArrayList());
        } catch (Exception e2) {
            Session.getLog().error("Error occured while restoring default A record for " + domainName, e2);
        }
    }

    public void FM_restoreDefaultARecords() throws Exception {
        restoreDefaultARecords();
    }
}
