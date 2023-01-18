package psoft.hsphere.resource;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.dns.ADNSRecord;
import psoft.hsphere.resource.dns.CNAMERecord;
import psoft.hsphere.resource.dns.DNSRecord;
import psoft.hsphere.resource.dns.MXRecord;
import psoft.hsphere.resource.email.MailRelay;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.email.SPFResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/Domain.class */
public class Domain extends Resource implements IPResourcePrunerInterface {
    protected String name;
    static int mxResType;
    static int cnameResType;
    static int aResType;

    static {
        mxResType = -1;
        cnameResType = -1;
        aResType = -1;
        try {
            mxResType = Integer.parseInt(TypeRegistry.getTypeId("mx"));
            cnameResType = Integer.parseInt(TypeRegistry.getTypeId("cname_record"));
            aResType = Integer.parseInt(TypeRegistry.getTypeId("a_record"));
        } catch (Exception ex) {
            Session.getLog().error("Unable to get an id of the resource type.", ex);
        }
    }

    public String getName() {
        return this.name;
    }

    public Domain(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name  FROM domains WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Domain(int type, Collection values) throws Exception {
        super(type, values);
        getLog().info("Creating new Domain");
        Iterator i = values.iterator();
        this.name = ((String) i.next()).toLowerCase();
    }

    public static String getDescription(InitToken token) throws Exception {
        Object obj;
        Iterator i = token.getValues().iterator();
        if (i.hasNext()) {
            obj = i.next();
        } else {
            obj = "";
        }
        return Localizer.translateMessage("description.domain", new Object[]{obj});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("description.domain", new Object[]{getName()});
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM domains WHERE name = ?");
                ps.setString(1, this.name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("Domain exists: " + this.name);
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO domains (id, name) VALUES (?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.name);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
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
        if (!"name".equals(key) && !"real_name".equals(key)) {
            if ("dns_zone".equals(key)) {
                try {
                    ResourceId rid = FM_getChild("dns_zone");
                    if (rid != null) {
                        return rid;
                    }
                } catch (Exception e) {
                    Session.getLog().error("Error getting domain child ", e);
                }
            }
            if ("ip".equals(key)) {
                try {
                    ResourceId rid2 = FM_getChild("ip");
                    if (rid2 != null) {
                        return rid2;
                    }
                } catch (Exception e2) {
                    Session.getLog().error("Error getting domain child ip", e2);
                }
            }
            if ("mail_service".equals(key)) {
                try {
                    ResourceId rid3 = FM_getChild("mail_service");
                    if (rid3 != null) {
                        return rid3;
                    }
                } catch (Exception e3) {
                    Session.getLog().error("Error getting domain child ip", e3);
                }
            }
            return super.get(key);
        }
        return new TemplateString(this.name);
    }

    @Override // psoft.hsphere.Resource
    protected void onDeleteNote() {
        Session.setBillingNote(Localizer.translateMessage("billview.note.delete_domain", new Object[]{getName()}));
    }

    @Override // psoft.hsphere.Resource
    public void deletePrev() throws Exception {
        if (!C0004CP.isMyDNSEnabled()) {
            if (!getResourceType().getType().equals("subdomain") || (getResourceType().getType().equals("subdomain") && ((Domain) getParent().get()).isDeletePrev())) {
                this.deletePrev = true;
            }
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

    public static Hashtable getDomainInfoByName(String domainName) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        for (String realDomainName = domainName; realDomainName.indexOf(".") >= 0; realDomainName = realDomainName.substring(realDomainName.indexOf(".") + 1)) {
            try {
                ps = con.prepareStatement("SELECT name, id, account_id FROM domains, parent_child WHERE name = ? AND domains.id+0 = parent_child.child_id");
                ps.setString(1, realDomainName);
                ps.executeQuery();
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Session.getLog().debug("Real domain name = " + realDomainName + " Domain name " + domainName);
                    Hashtable domain = new Hashtable(3);
                    domain.put("domain_name", rs.getString(1));
                    domain.put("resource_id", new Long(rs.getLong(2)));
                    domain.put("account_id", new Long(rs.getLong(3)));
                    Session.closeStatement(ps);
                    con.close();
                    return domain;
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        return null;
    }

    protected String getLabelByType() {
        try {
            return getId().getNamedType();
        } catch (Throwable th) {
            return "domain";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".refund", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".setup", new Object[]{getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill." + getLabelByType() + ".recurrent", new Object[]{getPeriodInWords(), getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill." + getLabelByType() + ".refundall", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    public void notifyEmail() throws Exception {
        if (FM_getChild("opensrs") != null) {
            return;
        }
        Session.save();
        try {
            Account a = getId().getAccount();
            Session.setUser(a.getUser());
            Session.setResellerId(a.getResellerId());
            Session.setAccount(a);
            Session.getLog().debug("Reseller id ---> " + Session.getResellerId());
            String eml = Session.getAccount().getContactInfo().getEmail();
            SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
            root.put("domain", getId());
            CustomEmailMessage.send("DOMAIN_TRANSFER", eml, (TemplateModelRoot) root);
        } finally {
            Session.restore();
        }
    }

    public void restoreDefaultARecords() throws Exception {
        for (Domain d : getChildDomains()) {
            ResourceId ipResId = d.FM_getChild("ip");
            if (ipResId != null) {
                restoreDefaultARecordsForDomain((MixedIPResource) ipResId.get(), d.name);
            }
        }
    }

    private void restoreDefaultARecordsForDomain(MixedIPResource res, String domainName) {
        try {
            Collection initValues = _getPlan().getDefaultInitValues(this, aResType, "wildcard");
            ADNSRecord ar = new ADNSRecord(aResType, initValues);
            DNSRecord.removeNonDefaultDNSRecords(ar.getPrefix() + domainName, "A", "");
            res.addChild("a_record", "wildcard", new ArrayList());
        } catch (Exception e) {
            Session.getLog().error("Error occured while restoring default wildcard A record for " + domainName, e);
        }
        try {
            DNSRecord.removeNonDefaultDNSRecords(domainName, "A", "");
            res.addChild("a_record", "", new ArrayList());
        } catch (Exception e2) {
            Session.getLog().error("Error occured while restoring default A record for " + domainName, e2);
        }
    }

    public void FM_restoreDefaultARecords() throws Exception {
        restoreDefaultARecords();
    }

    public void restoreDefaultMxRecords() throws Exception {
        for (Domain d : getChildDomains()) {
            ResourceId msResId = d.FM_getChild("mail_service");
            if (msResId != null) {
                restoreDefaultMxRecordsForDomain((MailService) msResId.get(), d);
            }
        }
    }

    private void restoreDefaultMxRecordsForDomain(MailService res, Domain resDomain) {
        try {
            Collection initValues = _getPlan().getDefaultInitValues(this, mxResType, "");
            MXRecord mxr = new MXRecord(mxResType, initValues);
            DNSRecord.removeNonDefaultMXRecords(resDomain.name, HostManager.getHost(res.getHostId()).get("name").toString(), mxr.get("pref").toString(), mxr.get("ttl").toString());
            Resource r = res.addChild("mx", "", new ArrayList()).get();
            ResourceId resId = resDomain.FM_findChild("mail_relay");
            if (resId != null) {
                MailRelay mr = (MailRelay) resId.get();
                long hostId = mr.getHostId();
                ((MXRecord) r).addMXList(hostId);
            }
        } catch (Exception e) {
            Session.getLog().error("Error occured while restoring default MX record for " + resDomain.name, e);
        }
    }

    public void FM_restoreDefaultMxRecords() throws Exception {
        restoreDefaultMxRecords();
    }

    public void restoreDefaultCNameRecords() throws Exception {
        for (Domain d : getChildDomains()) {
            ResourceId msResId = d.FM_getChild("mail_service");
            if (msResId != null) {
                restoreDefaultCNameRecordsForDomain((MailService) msResId.get(), d.name);
            }
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
        for (Domain d : getChildDomains()) {
            ResourceId spfResId = d.FM_findChild("spf");
            if (spfResId != null) {
                restoreDefaultTxtRecordsForDomain((SPFResource) spfResId.get(), d.name);
            }
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

    public List getChildDomains() throws Exception {
        List domains = new ArrayList();
        new ArrayList();
        domains.add(this);
        List<ResourceId> recs = (List) getId().findChildren("subdomain");
        for (ResourceId subdomId : recs) {
            Domain d = new Domain(subdomId);
            domains.add(d);
        }
        return domains;
    }

    public TemplateString FM_getChildDomainsNumber() throws Exception {
        return new TemplateString(getChildDomains().size());
    }
}
