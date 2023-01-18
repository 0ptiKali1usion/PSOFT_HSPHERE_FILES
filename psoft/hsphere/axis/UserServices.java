package psoft.hsphere.axis;

import freemarker.template.TemplateHashModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.SummaryQuota;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.UnixUserResource;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.user.UserDuplicateLoginException;
import psoft.util.FakeRequest;

/* loaded from: hsphere.zip:psoft/hsphere/axis/UserServices.class */
public class UserServices {
    private static Category log = Category.getInstance(UserServices.class.getName());

    public String getDescription() {
        return "Functions to work with users";
    }

    public long[] getAccounts(AuthToken at) throws Exception {
        User user = Utils.getUser(at);
        Collection<ResourceId> col = user.getAccountIds();
        long[] accounts = new long[col.size()];
        int count = 0;
        for (ResourceId rid : col) {
            int i = count;
            count++;
            accounts[i] = rid.getId();
        }
        return accounts;
    }

    public void changePassword(AuthToken at, String newPassword) throws Exception {
        User user = Utils.getUser(at);
        user.setPassword(newPassword);
    }

    public Object[] getPlan(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        return new Object[]{new Integer(a.getPlan().getId()), a.getPlan().getDescription()};
    }

    public Object[] getPlans(AuthToken at) throws Exception {
        User user = Utils.getUser(at);
        Collection<Plan> pls = Plan.getPlans().values();
        Collection plans = new ArrayList();
        for (Plan p : pls) {
            if (user.isAccessible(p)) {
                Object[] plan = {new Integer(p.getId()), p.getDescription()};
                plans.add(plan);
                pls.add(p);
            }
        }
        return pls.toArray();
    }

    public void changePlan(AuthToken at, int planId, int periodId) throws Exception {
        Utils.getAccount(at).FM_changePlan(planId, periodId);
    }

    public void changeContactInfo(AuthToken at, ContactInfo ci) throws Exception {
        Account a = Utils.getAccount(at);
        a.getContactInfo().updateCI(ci.getFirst_name(), ci.getLast_name(), ci.getOrg_name(), ci.getAddress1(), ci.getAddress2(), ci.getCity(), ci.getState(), ci.getState2(), ci.getPostal_code(), ci.getCountry(), ci.getPhone(), ci.getEmail());
    }

    public int getPeriodId(AuthToken at) throws Exception {
        return Utils.getAccount(at).getPeriodId();
    }

    public Object[] getPlanPeriodIds(AuthToken at, int planId) throws Exception {
        Utils.getAccount(at);
        Plan p = Plan.getPlan(planId);
        int countPeriods = Integer.parseInt(p.getValue("_PERIOD_TYPES"));
        Object[] periods = new Object[countPeriods];
        for (int i = 0; i < countPeriods; i++) {
            Object[] period = new Object[3];
            period[0] = new Integer(i);
            period[1] = p.getValue("_PERIOD_TYPE_" + i);
            period[2] = p.getValue("_PERIOD_SIZE_" + i);
            periods[i] = period;
        }
        return periods;
    }

    public void changeFTPPassword(AuthToken at, String newPassword) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = a.FM_getChild("unixuser");
        Resource r = rid.get();
        if (r instanceof UnixUserResource) {
            ((UnixUserResource) r).FM_changePassword(newPassword);
        } else if (r instanceof WinUserResource) {
            ((WinUserResource) r).FM_changePassword(newPassword);
        } else {
            throw new Exception("Unable to change FTP password");
        }
    }

    public String[] getDomains(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        Collection<ResourceId> col = a.getId().findAllChildren("domain");
        col.addAll(a.getId().findAllChildren("service_domain"));
        col.addAll(a.getId().findAllChildren("subdomain"));
        col.addAll(a.getId().findAllChildren("nodomain"));
        col.addAll(a.getId().findAllChildren("3ldomain"));
        if (col.size() == 0) {
            return null;
        }
        String[] result = new String[col.size()];
        int count = 0;
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            int i = count;
            count++;
            result[i] = ((Domain) r).getName();
        }
        return result;
    }

    public void signupUser(AuthToken at, int planId, int periodId, String login, String password, String descr, ContactInfo ci, ContactInfo bi, PaymentInfo pi, boolean validateUser) throws Exception {
        signupUser(at, planId, periodId, login, password, descr, ci, bi, pi, validateUser, "empty", new Object[0]);
    }

    public void signupUser(AuthToken at, int planId, int periodId, String login, String password, String descr, ContactInfo ci, ContactInfo bi, PaymentInfo pi, boolean validateUser, String mod, Object[] otherParams) throws Exception {
        User u = Utils.getUser(at);
        User user = null;
        try {
            user = User.getUser(login);
        } catch (Exception e) {
        }
        Plan p = Plan.getPlan(planId);
        boolean newUser = user == null;
        if (validateUser && ((newUser && !p.isAccessible(0)) || (!newUser && !user.isAccessible(p)))) {
            throw new HSUserException("user.plan.inaccessible");
        }
        if (user != null && !user.getPassword().equals(password)) {
            throw new UserDuplicateLoginException("Duplicate login.");
        }
        User oldUser = Session.getUser();
        if (user == null) {
            try {
                User.createUser(login, password, u.getResellerId());
                user = User.getUser(login);
            } finally {
                Session.setUser(oldUser);
            }
        }
        Session.setUser(user);
        Hashtable initval = new Hashtable();
        initval.put("plan_id", new String[]{String.valueOf(planId)});
        initval.put("period_id", new String[]{String.valueOf(periodId)});
        initval.put("login", new String[]{String.valueOf(login)});
        initval.put("password", new String[]{String.valueOf(password)});
        BillingInfoObject objBi = null;
        if (bi != null) {
            objBi = new BillingInfoObject(bi.getFirst_name(), bi.getOrg_name(), bi.getAddress1(), bi.getAddress2(), bi.getCity(), bi.getState(), bi.getState(), bi.getPostal_code(), bi.getCountry(), bi.getPhone(), bi.getEmail(), pi.getType(), null);
            objBi.setPaymentInfo(pi.iterator());
            initval.put("_bi_first_name", new String[]{bi.getFirst_name()});
            initval.put("_bi_last_name", new String[]{bi.getAddress1()});
            initval.put("_bi_company", new String[]{bi.getOrg_name()});
            initval.put("_bi_address1", new String[]{bi.getAddress1()});
            initval.put("_bi_city", new String[]{bi.getCity()});
            initval.put("_bi_state", new String[]{bi.getState()});
            initval.put("_bi_postal_code", new String[]{bi.getPostal_code()});
            initval.put("_bi_country", new String[]{bi.getCountry()});
            initval.put("_bi_phone", new String[]{bi.getPhone()});
            initval.put("_bi_email", new String[]{bi.getEmail()});
            initval.put("_bi_type", new String[]{pi.getType()});
        }
        ContactInfoObject objCi = null;
        if (ci != null) {
            objCi = new ContactInfoObject(ci.getFirst_name(), ci.getLast_name(), ci.getCountry(), ci.getAddress1(), ci.getAddress2(), ci.getCity(), ci.getState(), ci.getPostal_code(), ci.getCountry(), ci.getPhone(), ci.getEmail());
            initval.put("_ci_first_name", new String[]{ci.getFirst_name()});
            initval.put("_ci_last_name", new String[]{ci.getAddress1()});
            initval.put("_ci_company", new String[]{ci.getOrg_name()});
            initval.put("_ci_address1", new String[]{ci.getAddress1()});
            initval.put("_ci_city", new String[]{ci.getCity()});
            initval.put("_ci_state", new String[]{ci.getState()});
            initval.put("_ci_postal_code", new String[]{ci.getPostal_code()});
            initval.put("_ci_country", new String[]{ci.getCountry()});
            initval.put("_ci_phone", new String[]{ci.getPhone()});
            initval.put("_ci_email", new String[]{ci.getEmail()});
        }
        if (otherParams.length > 0) {
            for (Object obj : otherParams) {
                Object[] otherParam = (Object[]) obj;
                initval.put((String) otherParam[0], new String[]{(String) otherParam[1]});
            }
        }
        HttpServletRequest rq = Session.getRequest();
        Session.setRequest(new FakeRequest(initval));
        SignupManager.saveRequest(Session.getRequest());
        user.addAccount(planId, objBi, objCi, user.getLogin() + ":" + descr, mod, periodId);
        Session.setRequest(rq);
    }

    public double getTotalTrafficQuota(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        return ((Traffic) a.getId().findChild("traffic").get()).getAmount();
    }

    public void changeTotalTrafficQuota(AuthToken at, double size) throws Exception {
        Account a = Utils.getAccount(at);
        a.getId().findChild("traffic").get().FM_cdelete(0);
        List values = new ArrayList();
        values.add(Double.toString(size));
        a.addChild("traffic", "", values);
    }

    public double getTotalTrafficUsage(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        return ((Traffic) a.getId().findChild("traffic").get()).getTraffic();
    }

    public Date getTotalTrafficUsageFrom(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        return ((Traffic) a.getId().findChild("traffic").get()).getStartDate();
    }

    private ResourceId getSummaryQuota(Account a) throws Exception {
        ResourceId rid = a.getId().findChild("summary_quota");
        if (rid == null) {
            throw new Exception("Not found summary quota");
        }
        return rid;
    }

    public double getTotalDiskUsage(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        return ((SummaryQuota) getSummaryQuota(a).get()).getUsage();
    }

    public double getTotalDiskQuota(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        return ((SummaryQuota) getSummaryQuota(a).get()).getAmount();
    }

    public void changeTotalDiskQuota(AuthToken at, double size) throws Exception {
        Account a = Utils.getAccount(at);
        getSummaryQuota(a).get().FM_cdelete(0);
        List values = new ArrayList();
        values.add(Double.toString(size));
        a.addChild("summary_quota", "", values);
    }

    private NamedParameter[] getBillingOrContactInfo(TemplateHashModel hash) throws Exception {
        NamedParameter[] params = new NamedParameter[0];
        return NamedParameter.addParameter("email", hash.get("email").toString(), NamedParameter.addParameter("phone", hash.get("phone").toString(), NamedParameter.addParameter("country", hash.get("country").toString(), NamedParameter.addParameter("postal_code", hash.get("postal_code").toString(), NamedParameter.addParameter("state2", hash.get("state2").toString(), NamedParameter.addParameter("state", hash.get("state").toString(), NamedParameter.addParameter("city", hash.get("city").toString(), NamedParameter.addParameter("address2", hash.get("address2").toString(), NamedParameter.addParameter("address1", hash.get("address1").toString(), NamedParameter.addParameter("company", hash.get("company").toString(), NamedParameter.addParameter("org_name", hash.get("org_name").toString(), NamedParameter.addParameter("last_name", hash.get("last_name").toString(), NamedParameter.addParameter("first_name", hash.get("first_name").toString(), NamedParameter.addParameter("name", hash.get("name").toString(), params))))))))))))));
    }

    public NamedParameter[] getContactInfo(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ContactInfoObject contact = a.get("ci");
        return getBillingOrContactInfo(contact);
    }

    public NamedParameter[] getBillingInfo(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        BillingInfoObject bill = a.get("bi");
        return getBillingOrContactInfo(bill);
    }

    public void changeBillingInfo(AuthToken at, String first_name, String last_name, String company, String address1, String address2, String city, String state, String state2, String postal_code, String country, String phone, String email) throws Exception {
        Account a = Utils.getAccount(at);
        BillingInfoObject bill = a.get("bi");
        bill.updateBI(first_name, last_name, company, address1, address2, city, state, state2, postal_code, country, phone, email);
    }

    public String getBalance(AuthToken at) throws Exception {
        return Utils.getAmount(Utils.getAccount(at).getBill().getBalance());
    }

    public void addAntiVirusResources(AuthToken at, String range) throws Exception {
        Account a = Utils.getAccount(at);
        a.FM_addAntiVirusResources(range);
    }

    public void addAntiSpamResources(AuthToken at, String range) throws Exception {
        Account a = Utils.getAccount(at);
        a.FM_addAntiSpamResources(range);
    }

    public void deleteAntiSpamResources(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        for (ResourceId resourceId : a.getId().findChildren("antispam")) {
            resourceId.get().FM_cdelete(0);
        }
    }

    public void deleteAntiVirusResources(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        for (ResourceId resourceId : a.getId().findChildren("antivirus")) {
            resourceId.get().FM_cdelete(0);
        }
    }
}
