package psoft.hsphere.axis;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.HashMap;
import org.apache.axis.AxisFault;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.user.UserException;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/axis/Utils.class */
public class Utils {
    private static HashMap domainsByAcc = new HashMap();
    private static Category log = Category.getInstance(Utils.class.getName());

    public static Domain getDomain(Account a, String name) throws Exception {
        String accId = a.getId().get("id").toString();
        if (name != null) {
            if (domainsByAcc != null && domainsByAcc.containsKey(accId)) {
                HashMap hm = (HashMap) domainsByAcc.get(accId);
                if (hm.containsKey(name.toLowerCase())) {
                    return (Domain) hm.get(name);
                }
            } else {
                fillDomainsByAcc(a);
            }
            if (domainsByAcc != null && domainsByAcc.containsKey(accId)) {
                HashMap hm2 = (HashMap) domainsByAcc.get(accId);
                if (hm2.containsKey(name.toLowerCase())) {
                    return (Domain) hm2.get(name);
                }
            }
        }
        throw new Exception("No such domain: " + name);
    }

    public static HashMap getDomainsByAcc(Account a) throws Exception {
        String accId = a.getId().get("id").toString();
        if (domainsByAcc != null && domainsByAcc.containsKey(accId)) {
            return (HashMap) domainsByAcc.get(accId);
        }
        fillDomainsByAcc(a);
        if (domainsByAcc != null && domainsByAcc.containsKey(accId)) {
            return (HashMap) domainsByAcc.get(accId);
        }
        throw new Exception("Domains not found for account : " + accId);
    }

    public static void fillDomainsByAcc(Account a) throws Exception {
        HashMap hm = new HashMap();
        Domain[] domains = getDomains(a);
        for (Domain domain : domains) {
            hm.put(domain.getName().toLowerCase(), domain);
        }
        domainsByAcc.put(a.getId().get("id").toString(), hm);
    }

    public static Domain[] getDomains(Account a) throws Exception {
        Collection<ResourceId> col = a.getId().findAllChildren("domain");
        col.addAll(a.getId().findAllChildren("service_domain"));
        col.addAll(a.getId().findAllChildren("subdomain"));
        col.addAll(a.getId().findAllChildren("nodomain"));
        col.addAll(a.getId().findAllChildren("3ldomain"));
        col.addAll(a.getId().findAllChildren("parked_domain"));
        Domain[] result = new Domain[col.size()];
        int count = 0;
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            int i = count;
            count++;
            result[i] = (Domain) r;
        }
        return result;
    }

    public static DomainAlias getDomainAlias(Account a, String name) throws Exception {
        if (name != null) {
            DomainAlias[] domainAliases = getDomainAliases(a);
            for (DomainAlias domainAlias : domainAliases) {
                if (name.equalsIgnoreCase(domainAlias.getName())) {
                    return domainAlias;
                }
            }
        }
        throw new Exception("No such domain alias: " + name);
    }

    public static DomainAlias[] getDomainAliases(Account a) throws Exception {
        Collection<ResourceId> col = a.getId().findAllChildren("domain_alias");
        col.addAll(a.getId().findAllChildren("3ldomain_alias"));
        DomainAlias[] result = new DomainAlias[col.size()];
        int count = 0;
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            int i = count;
            count++;
            result[i] = (DomainAlias) r;
        }
        return result;
    }

    public static User _getUser(AuthToken at) throws Exception {
        C0004CP.getCP().setConfig();
        User user = User.getUser(at.getLogin());
        if (at.getPassword().equals(user.getPassword())) {
            Session.setUser(user);
            return user;
        }
        throw new UserException("Invalid password for user " + user.getLogin());
    }

    public static User getUser(AuthToken at) throws Exception {
        User user = _getUser(at);
        if (at.getRole() != null && isAdmin(getAccount(user, at.getAccountId()))) {
            user = User.getUser(at.getRole().getLogin());
            Session.setUser(user);
        }
        return user;
    }

    public static boolean isAdmin(Account a) {
        return a.getPlan().isResourceAvailable(FMACLManager.ADMIN) != null;
    }

    public static Account getAccount(User user, long accountId) throws Exception {
        ResourceId rid;
        if (accountId == 0) {
            Collection col = user.getAccountIds();
            if (col.size() > 1) {
                throw new UserException("User " + user.getLogin() + " has more then one account");
            }
            rid = (ResourceId) col.iterator().next();
        } else {
            rid = new ResourceId(accountId, 0);
        }
        return user.getAccount(rid);
    }

    public static Account getAccount(AuthToken at) throws Exception {
        User user = _getUser(at);
        int accountId = at.getAccountId();
        Role r = at.getRole();
        Account a = getAccount(user, accountId);
        if (r != null && isAdmin(a)) {
            user = User.getUser(r.getLogin());
            a = getAccount(user, r.getAccountId());
        }
        Session.setUser(user);
        Session.setAccount(a);
        return a;
    }

    public static String getAmount(double amount) throws Exception {
        HsphereToolbox tool = new HsphereToolbox();
        return tool.FM_numberToUSLocale(Double.toString(amount)).toString();
    }

    public static AxisFault generateAxisFault(Exception ex) {
        AxisFault af = AxisFault.makeFault(ex);
        af.clearFaultDetails();
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        ex.printStackTrace(out);
        out.close();
        af.setFaultDetailString(sw.toString());
        return af;
    }
}
