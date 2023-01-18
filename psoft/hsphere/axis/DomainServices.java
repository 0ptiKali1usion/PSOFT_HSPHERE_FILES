package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.axis.AxisFault;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.DomainRegistrar;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.util.FakeRequest;

/* loaded from: hsphere.zip:psoft/hsphere/axis/DomainServices.class */
public class DomainServices {
    private static Category log = Category.getInstance(DomainServices.class.getName());

    public String getDescription() {
        return "Functions to work with domains.";
    }

    protected Domain getDomain(Account a, String name) throws Exception {
        return Utils.getDomain(a, name);
    }

    protected Resource getUnixUser(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        try {
            ResourceId rId = a.getId().findChild("unixuser");
            if (rId != null) {
                Resource user = rId.get();
                return user;
            }
            return a;
        } catch (Exception ex) {
            Session.getLog().error("Unable to find user for account:" + a.getId(), ex);
            throw new HSUserException("Unable to find user for account:" + a.getId());
        }
    }

    public void registerDomain(AuthToken at, String name, int period, int renew, ContactInfo ci, ContactInfo bi) throws Exception {
        HttpServletRequest rq = Session.getRequest();
        try {
            Resource user = getUnixUser(at);
            Hashtable initval = new Hashtable();
            initval.put("period", new String[]{String.valueOf(period)});
            initval.put("renew", new String[]{String.valueOf(renew)});
            initval.put("domain_name", new String[]{name});
            if (ci == null) {
                throw new HSUserException("Unable to get Contact Info");
            }
            initval.put("_srs_owner_first_name", new String[]{ci.getFirst_name()});
            initval.put("_srs_owner_last_name", new String[]{ci.getLast_name()});
            initval.put("_srs_owner_org_name", new String[]{ci.getOrg_name()});
            initval.put("_srs_owner_address1", new String[]{ci.getAddress1()});
            initval.put("_srs_owner_address2", new String[]{ci.getAddress2()});
            initval.put("_srs_owner_address3", new String[]{ci.getAddress3()});
            initval.put("_srs_owner_city", new String[]{ci.getCity()});
            initval.put("_srs_owner_state", new String[]{ci.getState()});
            initval.put("_srs_owner_country", new String[]{ci.getCountry()});
            initval.put("_srs_owner_postal_code", new String[]{ci.getPostal_code()});
            initval.put("_srs_owner_phone", new String[]{ci.getPhone()});
            initval.put("_srs_owner_fax", new String[]{ci.getFax()});
            initval.put("_srs_owner_email", new String[]{ci.getEmail()});
            if (bi != null) {
                initval.put("_srs_billing_first_name", new String[]{bi.getFirst_name()});
                initval.put("_srs_billing_last_name", new String[]{bi.getLast_name()});
                initval.put("_srs_billing_org_name", new String[]{bi.getOrg_name()});
                initval.put("_srs_billing_address1", new String[]{bi.getAddress1()});
                initval.put("_srs_billing_address2", new String[]{bi.getAddress2()});
                initval.put("_srs_billing_address3", new String[]{bi.getAddress3()});
                initval.put("_srs_billing_city", new String[]{bi.getCity()});
                initval.put("_srs_billing_state", new String[]{bi.getState()});
                initval.put("_srs_billing_country", new String[]{bi.getCountry()});
                initval.put("_srs_billing_postal_code", new String[]{bi.getPostal_code()});
                initval.put("_srs_billing_phone", new String[]{bi.getPhone()});
                initval.put("_srs_billing_fax", new String[]{bi.getFax()});
                initval.put("_srs_billing_email", new String[]{bi.getEmail()});
            }
            Session.setRequest(new FakeRequest(initval));
            if (bi != null) {
                user.addChild("domain", "opensrs", null);
            } else {
                user.addChild("domain", "opensrsmix", null);
            }
        } finally {
            Session.setRequest(rq);
        }
    }

    protected void addDomainResource(AuthToken at, String type, String name) throws Exception {
        addDomainResource(at, type, "", name);
    }

    protected void addDomainResource(AuthToken at, String type, String mod, String name) throws Exception {
        Resource user = getUnixUser(at);
        if (!Session.getAccount().getPlan().areResourcesAvailable(type)) {
            throw new HSUserException("This resources is unavailable in " + Session.getAccount().getPlan().getDescription() + " plan");
        }
        List params = new ArrayList();
        params.add(name);
        user.addChild(type, mod, params);
    }

    public void addDomain(AuthToken at, String name) throws Exception {
        addDomainResource(at, "domain", name);
    }

    public void add3rdLevelDomain(AuthToken at, String name) throws Exception {
        addDomainResource(at, "3ldomain", name);
    }

    public void addServiceDomain(AuthToken at, String name) throws Exception {
        addDomainResource(at, "service_domain", name);
    }

    public void deleteDomain(AuthToken at, String name) throws Exception {
        Utils.getDomain(Utils.getAccount(at), name).delete(true);
    }

    public void addSubDomain(AuthToken at, String name, String domain) throws Exception {
        Domain domainRes = Utils.getDomain(Utils.getAccount(at), domain);
        List params = new ArrayList();
        params.add(name + "." + domain);
        domainRes.addChild("subdomain", "", params);
    }

    public void addNoDomain(AuthToken at) throws Exception {
        List params = new ArrayList();
        getUnixUser(at).addChild("nodomain", "", params);
    }

    public void deleteSubDomain(AuthToken at, String name, String domain) throws Exception {
        deleteDomain(at, name + "." + domain);
    }

    public void addParkedDomain(AuthToken at, String name, String ip) throws Exception {
        HttpServletRequest rq = Session.getRequest();
        try {
            Resource user = getUnixUser(at);
            Hashtable initval = new Hashtable();
            initval.put("domain_name", new String[]{name});
            initval.put("domain_ip", new String[]{ip});
            Session.setRequest(new FakeRequest(initval));
            user.addChild("parked_domain", "parked", null);
            Session.setRequest(rq);
        } catch (Throwable th) {
            Session.setRequest(rq);
            throw th;
        }
    }

    protected Domain getParkedDomain(AuthToken at, String name) throws Exception {
        Resource user = getUnixUser(at);
        Collection<ResourceId> col = user.getId().findAllChildren("parked_domain");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            if (name.equalsIgnoreCase(((Domain) r).getName())) {
                return (Domain) r;
            }
        }
        throw new Exception("No such domain alais: " + name);
    }

    public void deleteParkedDomain(AuthToken at, String name) throws Exception {
        getParkedDomain(at, name).delete(true);
    }

    public void addDomainAlias(AuthToken at, String alias, String domain, boolean isDNS, boolean isMailAlias) throws Exception {
        Domain domainRes = Utils.getDomain(Utils.getAccount(at), domain);
        String mod = "";
        if (isDNS && isMailAlias) {
            mod = "dns_n_mda";
        } else if (isDNS && !isMailAlias) {
            mod = "with_dns";
        } else if (!isDNS && isMailAlias) {
            mod = "dns_n_mda";
        }
        List params = new ArrayList();
        params.add(alias);
        domainRes.addChild("domain_alias", mod, params);
    }

    public void deleteDomainAlias(AuthToken at, String name) throws Exception {
        getDomainAlias(at, name).delete(true);
    }

    protected DomainAlias getDomainAlias(AuthToken at, String name) throws Exception {
        Resource user = getUnixUser(at);
        Collection<ResourceId> col = user.getId().findAllChildren("domain_alias");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            if (name.equalsIgnoreCase(((DomainAlias) r).getName())) {
                return (DomainAlias) r;
            }
        }
        throw new Exception("No such domain alais: " + name);
    }

    public boolean lookup(AuthToken at, String domainName) throws AxisFault {
        try {
            Utils.getUser(at);
            return DomainRegistrar.lookup(domainName);
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }
}
