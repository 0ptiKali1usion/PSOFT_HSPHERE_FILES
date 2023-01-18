package psoft.hsphere.resource.dns;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.Reconfigurable;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.resource.email.MailDomainAlias;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.system.DNSServices;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/dns/MXRecord.class */
public class MXRecord extends DNSRecord implements Reconfigurable {
    protected static final String DEFAULT_TTL = "86400";
    protected static final String DEFAULT_PREF = "10";
    protected static final int PREFS_DIFF = 20;
    protected String pref2;
    protected List mxList;

    public MXRecord(ResourceId id) throws Exception {
        super(id);
        this.mxList = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT d.id, d.name, d.data, d.ttl, d.pref FROM dns_records d, mx_list m WHERE m.id = ? AND m.mxrecord_id = d.id");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                HashMap mx = new HashMap();
                mx.put("id", Long.toString(rs.getLong("id")));
                mx.put("name", rs.getString("name"));
                mx.put("data", rs.getString("data"));
                mx.put("ttl", rs.getString("ttl"));
                mx.put("pref", rs.getString("pref"));
                this.mxList.add(mx);
                if (this.pref == null || this.pref.equalsIgnoreCase("")) {
                    this.pref = rs.getString("pref");
                } else if (new Integer(this.pref).intValue() > new Integer(rs.getString("pref")).intValue()) {
                    this.pref2 = this.pref;
                    this.pref = rs.getString("pref");
                } else {
                    this.pref2 = rs.getString("pref");
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (this.pref2 == null || this.pref2.equalsIgnoreCase("")) {
                this.pref2 = new Integer(new Integer(this.pref).intValue() + 20).toString();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public MXRecord(int type, Collection values) throws Exception {
        super(type, values);
        this.mxList = new ArrayList();
        Iterator i = values.iterator();
        if (i.hasNext()) {
            this.pref = (String) i.next();
            if (this.pref.equalsIgnoreCase(AntiSpam.DEFAULT_LEVEL_VALUE)) {
                this.pref = DEFAULT_PREF;
            }
        } else {
            this.pref = DEFAULT_PREF;
        }
        if (i.hasNext()) {
            this.ttl = (String) i.next();
            if (this.ttl.equalsIgnoreCase(AntiSpam.DEFAULT_LEVEL_VALUE)) {
                this.ttl = DEFAULT_TTL;
            }
        } else {
            this.ttl = DEFAULT_TTL;
        }
        if (i.hasNext()) {
            this.pref2 = (String) i.next();
            if (this.pref2.equalsIgnoreCase(AntiSpam.DEFAULT_LEVEL_VALUE) || new Integer(this.pref2).intValue() <= new Integer(this.pref).intValue()) {
                this.pref2 = new Integer(new Integer(this.pref).intValue() + 20).toString();
                return;
            }
            return;
        }
        this.pref2 = new Integer(new Integer(this.pref).intValue() + 20).toString();
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void initDone() throws Exception {
        getLog().info("in initdone in a new MX DNS record");
        LinkedList ll = new LinkedList();
        ll.add(recursiveGet("name").toString());
        ll.add("MX");
        ll.add(this.ttl);
        ll.add(((HostEntry) recursiveGet("mail_server")).get("name").toString());
        ll.add(this.pref);
        initDNS(ll);
        super.initDone();
    }

    @Override // psoft.hsphere.resource.Reconfigurable
    public void reconfigure() throws Exception {
        String newMX = recursiveGet("mail_server").get("name").toString();
        Session.getLog().debug("New MX data = " + newMX);
        if (newMX != null) {
            physicalDelete(this.data);
            this.data = newMX;
            physicalCreate(newMX);
        }
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        deleteMXList();
    }

    @Override // psoft.hsphere.resource.dns.DNSRecord, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "mx_list".equals(key) ? new TemplateList(this.mxList) : super.get(key);
    }

    public void addMXList(long relayOnMserverId) throws Exception {
        HostEntry main_mserver;
        HostEntry relayOnMserver = HostManager.getHost(relayOnMserverId);
        ResourceId dnsZone = recursiveGet("dns_zone");
        HostEntry host = dnsZone.get("master");
        String zone = dnsZone.get("name").toString();
        Resource res = getParent().get();
        if (res instanceof MailService) {
            main_mserver = HostManager.getHost(((MailService) getParent().get()).getHostId());
        } else if (res instanceof MailDomainAlias) {
            main_mserver = HostManager.getHost(((MailDomainAlias) getParent().get()).getHostId());
        } else {
            throw new Exception("Unable to add MX record for the " + zone + " DNS zone.");
        }
        this.mxList.clear();
        long mxlist_id = getId().getId();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String newRecordName = this.name == null ? zone : this.name;
        try {
            try {
                if (C0004CP.isIrisEnabled()) {
                    LinkedList mservers = HostManager.getSignupHosts(23);
                    for (int i = 0; i < mservers.size(); i++) {
                        if (!mservers.get(i).equals(main_mserver)) {
                            long mx_id = getNewId();
                            PreparedStatement ps2 = con.prepareStatement("INSERT INTO dns_records (id, name, type, data, ttl, pref) VALUES (?, ?, ?, ?, ?, ?)");
                            ps2.setLong(1, mx_id);
                            ps2.setString(2, newRecordName);
                            ps2.setString(3, "MX");
                            ps2.setString(4, ((HostEntry) mservers.get(i)).get("name").toString());
                            ps2.setString(5, this.ttl);
                            ps2.setString(6, this.pref);
                            ps2.executeUpdate();
                            ps2.close();
                            HashMap mx = new HashMap();
                            mx.put("name", newRecordName);
                            mx.put("data", ((HostEntry) mservers.get(i)).get("name").toString());
                            mx.put("ttl", this.ttl);
                            mx.put("pref", this.pref);
                            mx.put("id", Long.toString(mx_id));
                            this.mxList.add(mx);
                            ps = con.prepareStatement("INSERT INTO mx_list (id, mxrecord_id) VALUES (?, ?)");
                            ps.setLong(1, mxlist_id);
                            ps.setLong(2, mx_id);
                            ps.executeUpdate();
                            DNSServices.addToZone(host, zone, newRecordName, "MX", this.ttl, ((HostEntry) mservers.get(i)).get("name").toString(), this.pref);
                        }
                    }
                } else if (!main_mserver.equals(relayOnMserver)) {
                    long mx_id2 = getNewId();
                    PreparedStatement ps3 = con.prepareStatement("INSERT INTO dns_records (id, name, type, data, ttl, pref) VALUES (?, ?, ?, ?, ?, ?)");
                    ps3.setLong(1, mx_id2);
                    ps3.setString(2, newRecordName);
                    ps3.setString(3, "MX");
                    ps3.setString(4, relayOnMserver.get("name").toString());
                    ps3.setString(5, this.ttl);
                    ps3.setString(6, this.pref2);
                    ps3.executeUpdate();
                    ps3.close();
                    HashMap mx2 = new HashMap();
                    mx2.put("name", newRecordName);
                    mx2.put("data", relayOnMserver.get("name").toString());
                    mx2.put("ttl", this.ttl);
                    mx2.put("pref", this.pref2);
                    mx2.put("id", Long.toString(mx_id2));
                    this.mxList.add(mx2);
                    ps = con.prepareStatement("INSERT INTO mx_list (id, mxrecord_id) VALUES (?, ?)");
                    ps.setLong(1, mxlist_id);
                    ps.setLong(2, mx_id2);
                    ps.executeUpdate();
                    DNSServices.addToZone(host, zone, newRecordName, "MX", this.ttl, relayOnMserver.get("name").toString(), this.pref2);
                }
                ps = ps;
            } catch (SQLException se) {
                getLog().warn("Error inserting MX record ", se);
                throw new Exception("Unable to add MX record " + newRecordName + " for the " + zone + " DNS zone");
            }
        } finally {
            Session.closeStatement(null);
            con.close();
        }
    }

    public void deleteMXList() throws Exception {
        Exception causedException = null;
        if (this.initialized && !getParent().get().isDeletePrev() && this.mxList != null) {
            try {
                ResourceId dnsZone = recursiveGet("dns_zone");
                HostEntry host = dnsZone.get("master");
                String zone = dnsZone.get("name").toString();
                for (int i = 0; i < this.mxList.size(); i++) {
                    HashMap mx = (HashMap) this.mxList.get(i);
                    DNSServices.deleteFromZone(host, zone, (String) mx.get(new String("name")), "MX", (String) mx.get(new String("pref")), (String) mx.get(new String("data")));
                }
            } catch (Exception ex) {
                Session.getLog().debug("Error deleteing the DNS records:", ex);
                causedException = ex;
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM dns_records WHERE id IN (SELECT mxrecord_id FROM mx_list WHERE id = ?)");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM mx_list WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.mxList.clear();
            if (causedException != null) {
                throw causedException;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void physicalCreateMXList(String newData) throws Exception {
        ResourceId dnsZone = recursiveGet("dns_zone");
        HostEntry host = dnsZone.get("master");
        String zone = dnsZone.get("name").toString();
        String newRecordName = this.name == null ? zone : this.name;
        HashMap mx = (HashMap) this.mxList.get(0);
        DNSServices.addToZone(host, zone, newRecordName, "MX", this.ttl, newData, this.pref2);
        mx.put("data", newData);
        this.mxList.clear();
        this.mxList.add(mx);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE dns_records SET data = ? WHERE id = ?");
            ps.setString(1, newData);
            ps.setLong(2, new Long((String) mx.get("id")).longValue());
            ps.executeUpdate();
            this.data = newData;
            Session.getLog().debug("DNS RECORDS IN DB HAS BEEN UPDATED id=" + new Long((String) mx.get("id")).longValue() + " data=" + newData);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void physicalDeleteMXList() throws Exception {
        if (this.mxList != null) {
            try {
                ResourceId dnsZone = recursiveGet("dns_zone");
                HostEntry host = dnsZone.get("master");
                String zone = dnsZone.get("name").toString();
                for (int i = 0; i < this.mxList.size(); i++) {
                    HashMap mx = (HashMap) this.mxList.get(i);
                    DNSServices.deleteFromZone(host, zone, (String) mx.get(new String("name")), "MX", (String) mx.get(new String("pref")), (String) mx.get(new String("data")));
                }
            } catch (Exception ex) {
                Session.getLog().debug("Error deleteing the DNS records:", ex);
            }
        }
    }
}
