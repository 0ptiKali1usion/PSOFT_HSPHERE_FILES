package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.dns.CNAMERecord;
import psoft.hsphere.resource.dns.DNSRecord;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.dns.MXRecord;
import psoft.hsphere.resource.email.MailDomainAlias;
import psoft.hsphere.resource.email.MailRelay;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.email.SPFResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/DomainAlias.class */
public abstract class DomainAlias extends Resource implements IPDependentResource {
    protected String alias;
    static int mxResType;
    static int cnameResType;

    static {
        mxResType = -1;
        cnameResType = -1;
        try {
            mxResType = Integer.parseInt(TypeRegistry.getTypeId("mx"));
            cnameResType = Integer.parseInt(TypeRegistry.getTypeId("cname_record"));
        } catch (Exception ex) {
            Session.getLog().error("Unable to get an id of the 'mx' resource type.", ex);
        }
    }

    public DomainAlias(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.alias = ((String) i.next()).toLowerCase();
    }

    public DomainAlias(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name FROM domains WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.alias = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                if (testAliasName(this.alias) >= 0) {
                    throw new HSUserException("domain.taken", new Object[]{this.alias});
                }
                PreparedStatement ps = con.prepareStatement("INSERT INTO domains (id, name) VALUES (?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.alias);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting domain", e);
                throw new HSUserException("domain.exists");
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (!key.equals("alias") && !key.equals("name")) {
            if ("dns_zone".equals(key)) {
                try {
                    return FM_getChild("dns_zone");
                } catch (Exception e) {
                    Session.getLog().error("Error getting zone of domain alias", e);
                }
            }
            return super.get(key);
        }
        return new TemplateString(this.alias);
    }

    @Override // psoft.hsphere.Resource
    public void deletePrev() throws Exception {
        if (Session.getPropertyString("MYDNS_USER").equals("")) {
            this.deletePrev = true;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM domains WHERE id = ?");
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

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        Session.getLog().debug("START OF IP RECONFIG");
        ResourceId zoneId = get("dns_zone");
        if (zoneId != null) {
            try {
                DNSZone zone = (DNSZone) get(zoneId);
                Collection aRecords = zone.getChildHolder().getChildrenByName("domain_alias_a_record");
                synchronized (aRecords) {
                    if (aRecords.size() == 0) {
                        synchronized (Session.getAccount()) {
                            zone.addChild("domain_alias_a_record", "", new ArrayList());
                            zone.addChild("domain_alias_a_record", "wildcard", new ArrayList());
                        }
                    }
                }
            } catch (Exception ex) {
                Session.getLog().error("Error during change IP ", ex);
            }
        }
        Session.getLog().debug("END OF IP RECONFIG");
    }

    public String getName() {
        return this.alias;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.domain_alias.refund", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.domain_alias.setup", new Object[]{getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.domain_alias.recurrent", new Object[]{getPeriodInWords(), getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.domain_alias.refundall", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    /* JADX WARN: Code restructure failed: missing block: B:35:0x006c, code lost:
        psoft.hsphere.Session.getLog().debug("Real domain name = " + r8 + " Alias name " + r5);
     */
    /* JADX WARN: Code restructure failed: missing block: B:36:0x0093, code lost:
        if (r5.equals(r8) == false) goto L21;
     */
    /* JADX WARN: Code restructure failed: missing block: B:37:0x0096, code lost:
        r7 = 0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:38:0x009b, code lost:
        r7 = r0.getInt("id");
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public static int testAliasName(java.lang.String r5, boolean r6) throws java.sql.SQLException {
        /*
            r0 = -1
            r7 = r0
            org.apache.log4j.Category r0 = psoft.hsphere.Session.getLog()
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r2 = r1
            r2.<init>()
            java.lang.String r2 = "Alias name recursive "
            java.lang.StringBuilder r1 = r1.append(r2)
            r2 = r6
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r1 = r1.toString()
            r0.debug(r1)
            r0 = r5
            r8 = r0
            java.sql.Connection r0 = psoft.hsphere.Session.getDb()
            r9 = r0
            r0 = 0
            r10 = r0
            long r0 = psoft.hsphere.Session.getAccountId()
            r11 = r0
        L2a:
            r0 = r8
            java.lang.String r1 = "."
            int r0 = r0.indexOf(r1)     // Catch: java.lang.Throwable -> Ld2
            if (r0 < 0) goto Lc3
            r0 = r9
            java.lang.String r1 = "select name, id from domains d, parent_child p where d.id = p.child_id and name = ? and p.account_id <> ?"
            java.sql.PreparedStatement r0 = r0.prepareStatement(r1)     // Catch: java.lang.Throwable -> Ld2
            r10 = r0
            r0 = r10
            r1 = 1
            r2 = r8
            r0.setString(r1, r2)     // Catch: java.lang.Throwable -> Ld2
            r0 = r10
            r1 = 2
            r2 = r11
            r0.setLong(r1, r2)     // Catch: java.lang.Throwable -> Ld2
            r0 = r10
            java.sql.ResultSet r0 = r0.executeQuery()     // Catch: java.lang.Throwable -> Ld2
            r0 = r10
            java.sql.ResultSet r0 = r0.executeQuery()     // Catch: java.lang.Throwable -> Ld2
            r13 = r0
            r0 = r13
            boolean r0 = r0.next()     // Catch: java.lang.Throwable -> Ld2
            if (r0 == 0) goto La8
            org.apache.log4j.Category r0 = psoft.hsphere.Session.getLog()     // Catch: java.lang.Throwable -> Ld2
            java.lang.StringBuilder r1 = new java.lang.StringBuilder     // Catch: java.lang.Throwable -> Ld2
            r2 = r1
            r2.<init>()     // Catch: java.lang.Throwable -> Ld2
            java.lang.String r2 = "Real domain name = "
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> Ld2
            r2 = r8
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> Ld2
            java.lang.String r2 = " Alias name "
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> Ld2
            r2 = r5
            java.lang.StringBuilder r1 = r1.append(r2)     // Catch: java.lang.Throwable -> Ld2
            java.lang.String r1 = r1.toString()     // Catch: java.lang.Throwable -> Ld2
            r0.debug(r1)     // Catch: java.lang.Throwable -> Ld2
            r0 = r5
            r1 = r8
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Throwable -> Ld2
            if (r0 == 0) goto L9b
            r0 = 0
            r7 = r0
            goto Lc3
        L9b:
            r0 = r13
            java.lang.String r1 = "id"
            int r0 = r0.getInt(r1)     // Catch: java.lang.Throwable -> Ld2
            r7 = r0
            goto Lc3
        La8:
            r0 = r6
            if (r0 != 0) goto Laf
            goto Lc3
        Laf:
            r0 = r8
            r1 = r8
            java.lang.String r2 = "."
            int r1 = r1.indexOf(r2)     // Catch: java.lang.Throwable -> Ld2
            r2 = 1
            int r1 = r1 + r2
            r2 = r8
            int r2 = r2.length()     // Catch: java.lang.Throwable -> Ld2
            java.lang.String r0 = r0.substring(r1, r2)     // Catch: java.lang.Throwable -> Ld2
            r8 = r0
            goto L2a
        Lc3:
            r0 = r10
            psoft.hsphere.Session.closeStatement(r0)
            r0 = r9
            r0.close()
            goto Le3
        Ld2:
            r14 = move-exception
            r0 = r10
            psoft.hsphere.Session.closeStatement(r0)
            r0 = r9
            r0.close()
            r0 = r14
            throw r0
        Le3:
            r0 = r7
            return r0
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.resource.DomainAlias.testAliasName(java.lang.String, boolean):int");
    }

    public int testAliasName(String name) throws Exception {
        return testAliasName(name, true);
    }

    public void restoreDefaultMxRecords() throws Exception {
        try {
            Collection initValues = _getPlan().getDefaultInitValues(this, mxResType, "");
            MXRecord mxr = new MXRecord(mxResType, initValues);
            ResourceId mailId = FM_getChild("mail_domain_alias");
            long hostId = 0;
            if (mailId != null) {
                hostId = ((MailDomainAlias) mailId.get()).getHostId();
            } else {
                mailId = FM_getChild("mail_service");
                if (mailId != null) {
                    hostId = ((MailService) mailId.get()).getHostId();
                }
            }
            if (hostId > 0) {
                DNSRecord.removeNonDefaultMXRecords(this.alias, HostManager.getHost(hostId).get("name").toString(), mxr.get("pref").toString(), mxr.get("ttl").toString());
                Resource r = mailId.get().addChild("mx", "", new ArrayList()).get();
                if (mailId.get().getResourceType().getType().equals("mail_service")) {
                    ResourceId resId = FM_findChild("mail_relay");
                    if (resId != null) {
                        MailRelay mr = (MailRelay) resId.get();
                        ((MXRecord) r).addMXList(mr.getHostId());
                    }
                } else {
                    ResourceId resId2 = getParent().findChild("mail_service").findChild("mail_relay");
                    if (resId2 != null) {
                        MailRelay mr2 = (MailRelay) resId2.get();
                        ResourceId resId3 = getId().findChild("mx");
                        if (resId3 != null) {
                            MXRecord mx = (MXRecord) resId3.get();
                            mx.addMXList(mr2.getHostId());
                        }
                    }
                }
            }
        } catch (Exception e) {
            Session.getLog().error("Error occured while restoring default MX record for " + this.alias, e);
        }
    }

    public void FM_restoreDefaultMxRecords() throws Exception {
        restoreDefaultMxRecords();
    }

    public void restoreDefaultCNameRecords() throws Exception {
        String domainName = get("name").toString();
        ResourceId msResId = FM_getChild("mail_service");
        if (msResId != null) {
            restoreDefaultCNameRecordsForDomain((MailService) msResId.get(), domainName);
        }
    }

    private void restoreDefaultCNameRecordsForDomain(MailService res, String domainName) {
        try {
            Collection initValues = _getPlan().getDefaultInitValues(this, cnameResType, "mail");
            CNAMERecord cnr = new CNAMERecord(cnameResType, initValues);
            DNSRecord.removeNonDefaultDNSRecords(cnr.getPrefix() + domainName, "CNAME", "");
            res.addChild("cname_record", "mail", new ArrayList());
        } catch (Exception e) {
            Session.getLog().error("Error occured while restoring default A record for " + domainName, e);
        }
    }

    public void FM_restoreDefaultCNameRecords() throws Exception {
        restoreDefaultCNameRecords();
    }

    public void restoreDefaultTxtRecords() throws Exception {
        String domainName = get("name").toString();
        ResourceId spfResId = FM_findChild("spf");
        if (spfResId != null) {
            restoreDefaultTxtRecordsForDomain((SPFResource) spfResId.get(), domainName);
        }
    }

    public void restoreDefaultTxtRecordsForDomain(SPFResource spf, String domainName) throws Exception {
        try {
            DNSRecord.removeNonDefaultTxtRecords(domainName, spf.getTXTRecordData());
            Collection initValues = new ArrayList();
            initValues.add(spf.getTXTRecordData());
            spf.addChild("txt_record", "", initValues);
        } catch (Exception e) {
            Session.getLog().error("Error occured while restoring default MX record for " + domainName, e);
        }
    }

    public void FM_restoreDefaultTxtRecords() throws Exception {
        restoreDefaultTxtRecords();
    }
}
