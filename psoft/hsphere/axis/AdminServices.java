package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
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
import psoft.hsphere.admin.signupmanager.SignupRecord;
import psoft.hsphere.admin.signupmanager.SignupRecordHolder;
import psoft.hsphere.report.adv.AllDomainSearchReport;
import psoft.hsphere.report.adv.AllUserSearchReport;
import psoft.hsphere.report.adv.DeletedSearchReport;
import psoft.hsphere.report.adv.DomainSearchReport;
import psoft.hsphere.report.adv.SuspendedSearchReport;
import psoft.hsphere.report.adv.UserSearchByCIReport;
import psoft.hsphere.report.adv.UserSearchReport;
import psoft.hsphere.report.adv.UsersReport;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.Quota;
import psoft.hsphere.resource.SummaryQuota;
import psoft.hsphere.resource.Traffic;
import psoft.hsphere.resource.UnixUserResource;
import psoft.hsphere.resource.WinUserResource;
import psoft.hsphere.resource.admin.AccountManager;
import psoft.hsphere.resource.admin.BillingManager;
import psoft.hsphere.resource.admin.ReportManager;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.user.UserDuplicateLoginException;
import psoft.util.FakeRequest;
import psoft.util.USFormat;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/axis/AdminServices.class */
public class AdminServices {
    private static Category log = Category.getInstance(AdminServices.class.getName());

    public String getDescription() {
        return "Administrative functions";
    }

    public long[] getAccounts(AuthToken at) throws Exception {
        try {
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
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public void changePassword(AuthToken at, String newPassword) throws Exception {
        try {
            User user = Utils.getUser(at);
            user.setPassword(newPassword);
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public Object[] getPlan(AuthToken at) throws Exception {
        try {
            Account a = Utils.getAccount(at);
            return new Object[]{new Integer(a.getPlan().getId()), a.getPlan().getDescription()};
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public Object[] getPlans(AuthToken at) throws Exception {
        try {
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
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public void changePlan(AuthToken at, int planId, int periodId) throws Exception {
        try {
            Utils.getAccount(at).FM_changePlan(planId, periodId);
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public int getPeriodId(AuthToken at) throws Exception {
        return Utils.getAccount(at).getPeriodId();
    }

    public Object[] getPlanPeriodIds(AuthToken at, int planId) throws Exception {
        try {
            Utils.getAccount(at);
            Plan p = Plan.getPlan(planId);
            int countPeriods = Integer.parseInt(p.getValue("_PERIOD_TYPES"));
            Object[] periods = new Object[countPeriods];
            for (int i = 0; i < countPeriods; i++) {
                Object[] period = new Object[2];
                period[0] = new Integer(i);
                period[1] = p.getValue("_PERIOD_TYPE_" + i);
                period[2] = p.getValue("_PERIOD_SIZE_" + i);
                periods[i] = period;
            }
            return periods;
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public void changeFTPPassword(AuthToken at, String newPassword) throws Exception {
        try {
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
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public String[] getDomains(AuthToken at) throws Exception {
        try {
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
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    public NamedParameter[] signupUser(AuthToken at, int planId, int periodId, String login, String password, String descr, ContactInfo ci, ContactInfo bi, PaymentInfo pi, boolean validateUser) throws Exception {
        return signupUser(at, planId, periodId, login, password, descr, ci, bi, pi, validateUser, "empty", new Object[0]);
    }

    public NamedParameter[] signupUser(AuthToken at, int planId, int periodId, String login, String password, String descr, ContactInfo ci, ContactInfo bi, PaymentInfo pi, boolean validateUser, String mod, Object[] otherParams) throws Exception {
        return signupUser(at, planId, periodId, login, password, descr, ci, bi, null, null, pi, validateUser, mod, otherParams);
    }

    public NamedParameter[] signupUser(AuthToken at, int planId, int periodId, String login, String password, String descr, ContactInfo ci, ContactInfo bi, ContactInfo srsci, ContactInfo srsbi, PaymentInfo pi, boolean validateUser, String mod, Object[] otherParams) throws Exception {
        if (pi == null) {
            pi = new PaymentInfo("", "", "", "", "", "", "", "", "", "", "", "None");
        }
        HashMap retParams = new HashMap();
        try {
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
                User.createUser(login, password, u.getResellerId());
                user = User.getUser(login);
            }
            Session.setUser(user);
            Hashtable initval = new Hashtable();
            initval.put("plan_id", new String[]{String.valueOf(planId)});
            initval.put("_bp", new String[]{String.valueOf(periodId)});
            initval.put("login", new String[]{String.valueOf(login)});
            initval.put("password", new String[]{String.valueOf(password)});
            initval.put("_mod", new String[]{String.valueOf(mod)});
            if (pi.getType().equalsIgnoreCase("Trial")) {
                new BillingInfoObject(-1L);
                initval.put("_bi_type", new String[]{"TRIAL"});
            } else if (pi.getType().equalsIgnoreCase("None")) {
                new BillingInfoObject(0L);
            } else if (bi != null) {
                BillingInfoObject objBi = new BillingInfoObject(bi.getFirst_name(), bi.getLast_name(), bi.getOrg_name(), bi.getAddress1(), bi.getAddress2(), bi.getCity(), bi.getState(), bi.getState2(), bi.getPostal_code(), bi.getCountry(), bi.getPhone(), bi.getEmail(), pi.getType(), null);
                objBi.setPaymentInfo(pi.iterator());
                initval.put("_bi_first_name", new String[]{bi.getFirst_name()});
                initval.put("_bi_last_name", new String[]{bi.getLast_name()});
                initval.put("_bi_company", new String[]{bi.getOrg_name()});
                initval.put("_bi_address1", new String[]{bi.getAddress1()});
                initval.put("_bi_address2", new String[]{bi.getAddress2()});
                initval.put("_bi_address3", new String[]{bi.getAddress3()});
                initval.put("_bi_city", new String[]{bi.getCity()});
                initval.put("_bi_state", new String[]{bi.getState()});
                initval.put("_bi_state2", new String[]{bi.getState2()});
                initval.put("_bi_postal_code", new String[]{bi.getPostal_code()});
                initval.put("_bi_country", new String[]{bi.getCountry()});
                initval.put("_bi_phone", new String[]{bi.getPhone()});
                initval.put("_bi_fax", new String[]{bi.getFax()});
                initval.put("_bi_email", new String[]{bi.getEmail()});
                initval.put("_bi_type", new String[]{pi.getType()});
                if (pi != null) {
                    initval.put("_bi_cc_type", new String[]{pi.getCc_type()});
                    initval.put("_bi_cc_name", new String[]{pi.getName()});
                    initval.put("_bi_cc_number", new String[]{pi.getNumber()});
                    initval.put("_bi_cc_exp_year", new String[]{pi.getExpYear()});
                    initval.put("_bi_cc_exp_month", new String[]{pi.getExpMonth()});
                    initval.put("_bi_cc_exp_day", new String[]{pi.getExpDay()});
                    initval.put("_bi_cc_issue_no", new String[]{pi.issueNo});
                    initval.put("_bi_cc_start_year", new String[]{pi.getStartYear()});
                    initval.put("_bi_cc_start_month", new String[]{pi.getStartMonth()});
                    initval.put("_bi_cc_start_day", new String[]{pi.getStartDay()});
                }
            }
            if (ci != null) {
                new ContactInfoObject(ci.getFirst_name(), ci.getLast_name(), ci.getOrg_name(), ci.getAddress1(), ci.getAddress2(), ci.getCity(), ci.getState(), ci.getState2(), ci.getPostal_code(), ci.getCountry(), ci.getPhone(), ci.getEmail());
                initval.put("_ci_first_name", new String[]{ci.getFirst_name()});
                initval.put("_ci_last_name", new String[]{ci.getLast_name()});
                initval.put("_ci_company", new String[]{ci.getOrg_name()});
                initval.put("_ci_address1", new String[]{ci.getAddress1()});
                initval.put("_ci_address2", new String[]{ci.getAddress2()});
                initval.put("_ci_address3", new String[]{ci.getAddress3()});
                initval.put("_ci_city", new String[]{ci.getCity()});
                initval.put("_ci_state", new String[]{ci.getState()});
                initval.put("_ci_state2", new String[]{ci.getState2()});
                initval.put("_ci_postal_code", new String[]{ci.getPostal_code()});
                initval.put("_ci_country", new String[]{ci.getCountry()});
                initval.put("_ci_phone", new String[]{ci.getPhone()});
                initval.put("_ci_fax", new String[]{ci.getFax()});
                initval.put("_ci_email", new String[]{ci.getEmail()});
            }
            if (mod.equalsIgnoreCase("opensrs")) {
                if (srsci != null) {
                    initval.put("_ci_first_name", new String[]{srsci.getFirst_name()});
                    initval.put("_ci_last_name", new String[]{srsci.getLast_name()});
                    initval.put("_ci_company", new String[]{srsci.getOrg_name()});
                    initval.put("_ci_address1", new String[]{srsci.getAddress1()});
                    initval.put("_ci_address2", new String[]{srsci.getAddress2()});
                    initval.put("_ci_address3", new String[]{srsci.getAddress3()});
                    initval.put("_ci_city", new String[]{srsci.getCity()});
                    initval.put("_ci_state", new String[]{srsci.getState()});
                    initval.put("_ci_state2", new String[]{srsci.getState2()});
                    initval.put("_ci_postal_code", new String[]{srsci.getPostal_code()});
                    initval.put("_ci_country", new String[]{srsci.getCountry()});
                    initval.put("_ci_phone", new String[]{srsci.getPhone()});
                    initval.put("_ci_fax", new String[]{srsci.getFax()});
                    initval.put("_ci_email", new String[]{srsci.getEmail()});
                } else {
                    initval.put("_srs_owner_first_name", new String[]{ci.getFirst_name()});
                    initval.put("_srs_owner_last_name", new String[]{ci.getLast_name()});
                    initval.put("_srs_owner_org_name", new String[]{ci.getOrg_name()});
                    initval.put("_srs_owner_address1", new String[]{ci.getAddress1()});
                    initval.put("_srs_owner_address2", new String[]{ci.getAddress2()});
                    initval.put("_srs_owner_address3", new String[]{ci.getAddress3()});
                    initval.put("_srs_owner_city", new String[]{ci.getCity()});
                    initval.put("_srs_owner_state", new String[]{ci.getState()});
                    initval.put("_srs_owner_state2", new String[]{ci.getState2()});
                    initval.put("_srs_owner_country", new String[]{ci.getCountry()});
                    initval.put("_srs_owner_postal_code", new String[]{ci.getPostal_code()});
                    initval.put("_srs_owner_phone", new String[]{ci.getPhone()});
                    initval.put("_srs_owner_fax", new String[]{ci.getFax()});
                    initval.put("_srs_owner_email", new String[]{ci.getEmail()});
                }
                if (srsbi != null) {
                    initval.put("_srs_billing_first_name", new String[]{srsbi.getFirst_name()});
                    initval.put("_srs_billing_last_name", new String[]{srsbi.getLast_name()});
                    initval.put("_srs_billing_org_name", new String[]{srsbi.getOrg_name()});
                    initval.put("_srs_billing_address1", new String[]{srsbi.getAddress1()});
                    initval.put("_srs_billing_address2", new String[]{srsbi.getAddress2()});
                    initval.put("_srs_billing_address3", new String[]{srsbi.getAddress3()});
                    initval.put("_srs_billing_city", new String[]{srsbi.getCity()});
                    initval.put("_srs_billing_state", new String[]{srsbi.getState()});
                    initval.put("_srs_billing_state2", new String[]{srsbi.getState2()});
                    initval.put("_srs_billing_country", new String[]{srsbi.getCountry()});
                    initval.put("_srs_billing_postal_code", new String[]{srsbi.getPostal_code()});
                    initval.put("_srs_billing_phone", new String[]{srsbi.getPhone()});
                    initval.put("_srs_billing_fax", new String[]{srsbi.getFax()});
                    initval.put("_srs_billing_email", new String[]{srsbi.getEmail()});
                } else {
                    initval.put("_srs_billing_first_name", new String[]{ci.getFirst_name()});
                    initval.put("_srs_billing_last_name", new String[]{ci.getLast_name()});
                    initval.put("_srs_billing_org_name", new String[]{ci.getOrg_name()});
                    initval.put("_srs_billing_address1", new String[]{ci.getAddress1()});
                    initval.put("_srs_billing_address2", new String[]{ci.getAddress2()});
                    initval.put("_srs_billing_address3", new String[]{ci.getAddress3()});
                    initval.put("_srs_billing_city", new String[]{ci.getCity()});
                    initval.put("_srs_billing_state", new String[]{ci.getState()});
                    initval.put("_srs_billing_state2", new String[]{ci.getState2()});
                    initval.put("_srs_billing_country", new String[]{ci.getCountry()});
                    initval.put("_srs_billing_postal_code", new String[]{ci.getPostal_code()});
                    initval.put("_srs_billing_phone", new String[]{ci.getPhone()});
                    initval.put("_srs_billing_fax", new String[]{ci.getFax()});
                    initval.put("_srs_billing_email", new String[]{ci.getEmail()});
                }
            }
            if (otherParams.length > 0) {
                for (Object obj : otherParams) {
                    NamedParameter param = (NamedParameter) obj;
                    initval.put(param.getName(), new String[]{param.getValue()});
                }
            }
            HttpServletRequest rq = Session.getRequest();
            try {
                Session.setRequest(new FakeRequest(initval));
                long signupId = SignupManager.saveRequest(Session.getRequest());
                retParams.put("signupId", String.valueOf(signupId));
                user.addAccount(planId, descr, mod, false);
                SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, false);
                retParams.put("accountId", String.valueOf(sr.getAccountId()));
                retParams.put("userId", String.valueOf(sr.getUserId()));
                Session.setRequest(rq);
                Session.setUser(oldUser);
                return NamedParameter.getParameters(retParams);
            } catch (Throwable th) {
                Session.setRequest(rq);
                throw th;
            }
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    private ReportManager getReportManager(AuthToken at) throws Exception {
        try {
            Account a = Utils.getAccount(at);
            ResourceId adminRid = a.getId().findChild(FMACLManager.ADMIN);
            if (adminRid == null) {
                throw new Exception("Not found admin resource");
            }
            ResourceId reportviewerRid = a.getId().findChild("reportviewer");
            if (reportviewerRid == null) {
                throw new Exception("Not found report viewer");
            }
            ReportManager repMan = (ReportManager) reportviewerRid.get();
            return repMan;
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }

    private UserInfo[] getUsers(ListAdapter adapter) {
        String sz = adapter.get("size").toString();
        int size = Integer.parseInt(sz);
        UserInfo[] users = new UserInfo[size];
        int counter = 0;
        while (adapter.hasNext()) {
            MapAdapter mapAdapter = adapter.next();
            String username = null;
            String accountId = null;
            String created = null;
            String plan = null;
            String email = null;
            String billingType = null;
            String balance = null;
            String credit = null;
            String pEnd = null;
            String accountDescription = null;
            String suspended = null;
            String deleted = null;
            String domain = null;
            String domainType = null;
            if (mapAdapter.get("username") != null) {
                username = mapAdapter.get("username").toString();
            }
            if (mapAdapter.get("accountId") != null) {
                accountId = mapAdapter.get("accountId").toString();
            }
            if (mapAdapter.get("created") != null) {
                created = mapAdapter.get("created").toString();
            }
            if (mapAdapter.get("plan") != null) {
                plan = mapAdapter.get("plan").toString();
            }
            if (mapAdapter.get("email") != null) {
                email = mapAdapter.get("email").toString();
            }
            if (mapAdapter.get("billingType") != null) {
                billingType = mapAdapter.get("billingType").toString();
            }
            if (mapAdapter.get("balance") != null) {
                balance = mapAdapter.get("balance").toString();
            }
            if (mapAdapter.get("credit") != null) {
                credit = mapAdapter.get("credit").toString();
            }
            if (mapAdapter.get("pEnd") != null) {
                pEnd = mapAdapter.get("pEnd").toString();
            }
            if (mapAdapter.get("accountDescription") != null) {
                accountDescription = mapAdapter.get("accountDescription").toString();
            }
            if (mapAdapter.get("suspended") != null) {
                suspended = mapAdapter.get("suspended").toString();
            }
            if (mapAdapter.get("deleted") != null) {
                deleted = mapAdapter.get("deleted").toString();
            }
            if (mapAdapter.get("resellerId") != null) {
                deleted = mapAdapter.get("resellerId").toString();
            }
            if (mapAdapter.get("domain") != null) {
                domain = mapAdapter.get("domain").toString();
            }
            if (mapAdapter.get("domainType") != null) {
                domainType = mapAdapter.get("domainType").toString();
            }
            UserInfo userInfo = new UserInfo(username, accountId, created, plan, email, billingType, balance, credit, pEnd, accountDescription, suspended, deleted, null, domain, domainType);
            users[counter] = userInfo;
            counter++;
        }
        return users;
    }

    public UserInfo[] searchUsers(AuthToken at, String group, String physicalServer, String logicalServer) throws Exception {
        ReportManager repMan = getReportManager(at);
        UsersReport report = repMan.FM_getAdvReport("usersreport");
        ArrayList values = new ArrayList();
        values.add(group);
        values.add(physicalServer);
        values.add(logicalServer);
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchDeletedAccounts(AuthToken at, String bilingType, String plan, String accountId, String email, int emailTypeForMassMail, String regFrom, String regTo, String delFrom, String delTo) throws Exception {
        ReportManager repMan = getReportManager(at);
        DeletedSearchReport report = repMan.FM_getAdvReport("deletedsearch");
        ArrayList values = new ArrayList();
        values.add(accountId);
        values.add(plan);
        values.add(bilingType);
        values.add(email);
        values.add(regFrom);
        values.add(regTo);
        values.add(delFrom);
        values.add(delTo);
        values.add(Integer.toString(emailTypeForMassMail));
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchSuspended(AuthToken at, String bilingType, String plan, String accountId, String user, String email, int emailTypeForMassMail, String regFrom, String regTo, String delFrom, String delTo) throws Exception {
        ReportManager repMan = getReportManager(at);
        SuspendedSearchReport report = repMan.FM_getAdvReport("suspendedsearch");
        ArrayList values = new ArrayList();
        values.add(accountId);
        values.add(plan);
        values.add(bilingType);
        values.add(email);
        values.add(user);
        values.add(regFrom);
        values.add(regTo);
        values.add(delFrom);
        values.add(delTo);
        values.add(Integer.toString(emailTypeForMassMail));
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchInResellersUsers(AuthToken at, String reseller, String accountId, String user, String email, int emailTypeForMassMail, String regFrom, String regTo) throws Exception {
        ReportManager repMan = getReportManager(at);
        AllUserSearchReport report = repMan.FM_getAdvReport("allusersearch");
        ArrayList values = new ArrayList();
        values.add(accountId);
        values.add(reseller);
        values.add(email);
        values.add(user);
        values.add(regFrom);
        values.add(regTo);
        values.add(Integer.toString(emailTypeForMassMail));
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchInResellersDomains(AuthToken at, String domainName, int emailTypeForMassMail) throws Exception {
        ReportManager repMan = getReportManager(at);
        AllDomainSearchReport report = repMan.FM_getAdvReport("alldomainsearch");
        ArrayList values = new ArrayList();
        values.add(domainName);
        values.add(Integer.toString(emailTypeForMassMail));
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchByContactInfo(AuthToken at, String firstName, String lastName, String company, String city, String state, String postalCode, String country) throws Exception {
        ReportManager repMan = getReportManager(at);
        UserSearchByCIReport report = repMan.FM_getAdvReport("usersearch_byci");
        ArrayList values = new ArrayList();
        values.add(firstName);
        values.add(lastName);
        values.add(company);
        values.add(city);
        values.add(state);
        values.add(postalCode);
        values.add(country);
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchByDomainName(AuthToken at, String domainName, int emailTypeForMassMail) throws Exception {
        ReportManager repMan = getReportManager(at);
        DomainSearchReport report = repMan.FM_getAdvReport("domainsearch");
        ArrayList values = new ArrayList();
        values.add(domainName);
        values.add(Integer.toString(emailTypeForMassMail));
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public UserInfo[] searchGeneric(AuthToken at, String bilingType, String suspended, String plan, String userName, String accountId, String email, int emailTypeForMassMail, String regFrom, String regTo) throws Exception {
        ReportManager repMan = getReportManager(at);
        UserSearchReport report = repMan.FM_getAdvReport("usersearch");
        ArrayList values = new ArrayList();
        values.add(accountId);
        values.add(plan);
        values.add(bilingType);
        values.add(email);
        values.add(userName);
        values.add(regFrom);
        values.add(regTo);
        values.add(Integer.toString(emailTypeForMassMail));
        values.add(suspended);
        report.exec(values);
        ListAdapter adapter = (ListAdapter) report.FM_all();
        return getUsers(adapter);
    }

    public String searchAccIdBySignupId(long signupId) throws Exception {
        SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, false);
        return Long.toString(sr.getAccountId());
    }

    public String searchUserIdBySignupId(long signupId) throws Exception {
        SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, false);
        return Long.toString(sr.getUserId());
    }

    private BillingManager getBillingManager(long accountId) throws Exception {
        ResourceId rid = new ResourceId(accountId, ResourceId.getTypeById(accountId));
        if (rid == null) {
            throw new Exception("Not found account with accountId - " + accountId);
        }
        ResourceId billId = rid.findChild("billman");
        if (billId == null) {
            throw new Exception("Not found billing manager");
        }
        BillingManager manager = (BillingManager) billId.get();
        return manager;
    }

    public void monthlyBasedDebit(AuthToken at, long accountId, double amount, int duration, int frequency, String description, String note, String adminNote, int startOn, boolean isInfinite, boolean isInvoice) throws Exception {
        long adminAccountId = Utils.getAccount(at).getId().getId();
        BillingManager manager = getBillingManager(adminAccountId);
        String str_amount = Utils.getAmount(amount);
        if (isInfinite) {
            duration = 0;
        }
        manager.FM_addService(accountId, startOn, frequency, duration, description, note, adminNote, str_amount, isInvoice);
    }

    public void billingPeriodBasedDebit(AuthToken at, long accountId, double amount, String description, String note, String adminNote) throws Exception {
        long adminAccountId = Utils.getAccount(at).getId().getId();
        BillingManager manager = getBillingManager(adminAccountId);
        String str_amount = Utils.getAmount(amount);
        manager.FM_addCustomResource(accountId, description, note, adminNote, str_amount);
    }

    public void oneTimeDebit(AuthToken at, long accountId, double amount, String description, String note, boolean isInvoice, boolean inclTaxes) throws Exception {
        long adminAccountId = Utils.getAccount(at).getId().getId();
        BillingManager manager = getBillingManager(adminAccountId);
        String str_amount = Utils.getAmount(amount);
        manager.FM_addDebit(str_amount, description, note, null, accountId, isInvoice, inclTaxes);
    }

    public void addCredit(AuthToken at, long accountId, double amount, String description, String text, String id) throws Exception {
        long adminAccountId = Utils.getAccount(at).getId().getId();
        BillingManager manager = getBillingManager(adminAccountId);
        String str_amount = USFormat.getInstanceOut().format(amount);
        manager.FM_addCredit(str_amount, description, text, id, accountId);
    }

    public static void deleteAccount(AuthToken at, long accountId) throws Exception {
        Account account = getAccount(at, accountId, false);
        User user = account.getUser();
        Account adminAccount = Utils.getAccount(at);
        ResourceId rid = adminAccount.FM_findChild(FMACLManager.ADMIN);
        if (rid != null) {
            Session.setAccount(adminAccount);
            AccountManager manager = (AccountManager) rid.get();
            manager.FM_deleteUserAccount(user.getLogin(), account.getId().getId());
        }
    }

    public static void suspendAccount(AuthToken at, long accountId, String reason) throws Exception {
        Account account = getAccount(at, accountId, false);
        User user = account.getUser();
        Account adminAccount = Utils.getAccount(at);
        ResourceId rid = adminAccount.FM_findChild(FMACLManager.ADMIN);
        if (rid != null) {
            Session.setAccount(adminAccount);
            AccountManager manager = (AccountManager) rid.get();
            manager.FM_suspendAccount(user.getLogin(), account.getId().getId(), reason);
        }
    }

    public static void resumeAccount(AuthToken at, long accountId) throws Exception {
        Account account = getAccount(at, accountId, false);
        User user = account.getUser();
        Account adminAccount = Utils.getAccount(at);
        ResourceId rid = adminAccount.FM_findChild(FMACLManager.ADMIN);
        if (rid != null) {
            Session.setAccount(adminAccount);
            AccountManager manager = (AccountManager) rid.get();
            manager.FM_resumeAccount(user.getLogin(), account.getId().getId());
        }
    }

    public static void changeQuota(AuthToken at, long accountId, double value) throws Exception {
        List param = new ArrayList();
        param.add(String.valueOf(value));
        Account account = getAccount(at, accountId);
        long old = 1;
        try {
            old = Session.getResellerId();
        } catch (Exception e) {
        }
        Session.setResellerId(account.getResellerId());
        Quota quota = (Quota) account.FM_findChild("quota").get();
        quota.change(param);
        Session.setResellerId(old);
    }

    public static double getQuota(AuthToken at, long accountId) throws Exception {
        Account account = getAccount(at, accountId);
        Quota quota = (Quota) account.FM_findChild("quota").get();
        return quota.getAmount();
    }

    public static void changeTraffic(AuthToken at, long accountId, double value) throws Exception {
        List param = new ArrayList();
        param.add(String.valueOf(value));
        Account account = getAccount(at, accountId);
        long old = 1;
        try {
            old = Session.getResellerId();
        } catch (Exception e) {
        }
        Session.setResellerId(account.getResellerId());
        Traffic traffic = (Traffic) account.FM_findChild("traffic").get();
        traffic.change(param);
        Session.setResellerId(old);
    }

    public static double getTraffic(AuthToken at, long accountId) throws Exception {
        Account account = getAccount(at, accountId);
        Traffic traffic = (Traffic) account.FM_findChild("traffic").get();
        return traffic.getAmount();
    }

    public static void changeDiskUsage(AuthToken at, long accountId, double value) throws Exception {
        List param = new ArrayList();
        param.add(String.valueOf(value));
        Account account = getAccount(at, accountId);
        long old = 1;
        try {
            old = Session.getResellerId();
        } catch (Exception e) {
        }
        Session.setResellerId(account.getResellerId());
        SummaryQuota summaryQuota = (SummaryQuota) account.FM_findChild("summary_quota").get();
        summaryQuota.change(param);
        Session.setResellerId(old);
    }

    public static double getDiskUsage(AuthToken at, long accountId) throws Exception {
        Account account = getAccount(at, accountId);
        SummaryQuota summaryQuota = (SummaryQuota) account.FM_findChild("summary_quota").get();
        return summaryQuota.getAmount();
    }

    protected static Account getAccount(AuthToken at, long accountId) throws Exception {
        return getAccount(at, accountId, true);
    }

    protected static Account getAccount(AuthToken at, long accountId, boolean allowUserAccess) throws Exception {
        Account a = Utils.getAccount(at);
        Account account = Account.getAccount(accountId);
        if (a.getId().findChild(FMACLManager.ADMIN) != null || a.getId().findChild("su") != null) {
            if (a.getResellerId() != account.getResellerId()) {
                throw new Exception("Operation not permitted");
            }
        } else if (a.getId().getId() != account.getId().getId() || !allowUserAccess) {
            throw new Exception("Operation not permitted");
        }
        return account;
    }

    public static boolean isAccountSuspended(AuthToken at, long accountId) throws Exception {
        Account account = getAccount(at, accountId);
        return account.isSuspended();
    }

    public static boolean checkUser(AuthToken at, String login) throws Exception {
        try {
            Utils.getUser(at);
            if (User.getUser(login) != null) {
                return true;
            }
            return false;
        } catch (HSUserException e) {
            return false;
        } catch (Exception ex) {
            throw Utils.generateAxisFault(ex);
        }
    }
}
