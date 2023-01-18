package psoft.hsphere.resource;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.Account;
import psoft.hsphere.AccountCreationException;
import psoft.hsphere.Bill;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.User;
import psoft.hsphere.admin.signupmanager.SignupRecord;
import psoft.hsphere.admin.signupmanager.TemplateSignupRecord;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.hsphere.resource.moderate_signup.RequestRecord;
import psoft.hsphere.resource.moderate_signup.TempAccount;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.user.UserException;
import psoft.util.TimeUtils;
import psoft.util.freemarker.HtmlEncodedHashListScalar;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplatePair;

/* loaded from: hsphere.zip:psoft/hsphere/resource/SignupResource.class */
public class SignupResource extends Resource {
    protected static long setSignupId(HttpServletRequest request) {
        return SignupManager.saveRequest(request);
    }

    public SignupResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public SignupResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("plans")) {
            return new TemplateList(Plan.getPlanList());
        }
        if (key.equals("failed_accounts")) {
            try {
                return new TemplateHash(getFailedAccounts());
            } catch (Exception e) {
                return new TemplateList(new ArrayList());
            }
        }
        return super.get(key);
    }

    /* renamed from: psoft.hsphere.resource.SignupResource$1RequestPaymentInfo */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/SignupResource$1RequestPaymentInfo.class */
    class C1RequestPaymentInfo {
        int status;
        public Timestamp statusDate;
        double reqAmount;
        double recAmount;

        public C1RequestPaymentInfo(int status, double reqAmount, double recAmount, Timestamp statusDate) {
            SignupResource.this = r5;
            this.status = status;
            this.statusDate = statusDate;
            this.reqAmount = reqAmount;
            this.recAmount = recAmount;
        }
    }

    public TemplateModel FM_displayAccounts() throws Exception {
        TemplateList list = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            ps2 = con.prepareStatement("SELECT rr.request_id, expm.status, MAX(expm.req_amount), MAX(expm.rec_amount), MAX(expm.status_date) FROM extern_pm AS expm JOIN request_record AS rr ON (rr.request_id=expm.req_id) WHERE rr.deleted IS NULL AND expm.reseller_id=? AND expm.req_type=2 GROUP BY rr.request_id, expm.status ORDER BY rr.request_id");
            ps2.setLong(1, Session.getResellerId());
            ResultSet rs2 = ps2.executeQuery();
            HashMap requests = new HashMap();
            while (rs2.next()) {
                long requestId = rs2.getLong(1);
                int curStatus = rs2.getInt(2);
                requests.put(requestId + "_" + curStatus, new C1RequestPaymentInfo(curStatus, rs2.getDouble(3), rs2.getDouble(4), rs2.getTimestamp(5)));
            }
            ps1 = con.prepareStatement("SELECT count(*) FROM f_user_account WHERE request_id = ?");
            ps = con.prepareStatement("SELECT rr.bid, u.username, rr.user_id, rr.cid, rr.created, rr.request_id, p.description FROM request_record AS rr JOIN plans AS p ON (rr.plan_id=p.id) JOIN users AS u ON (u.id=rr.user_id) WHERE rr.deleted is NULL AND u.reseller_id=? ORDER BY rr.plan_id");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateMap record = new TemplateMap();
                record.put("bid", rs.getString(1));
                record.put("login", rs.getString(2));
                record.put("uid", rs.getString(3));
                record.put("cid", rs.getString(4));
                record.put("created", rs.getTimestamp(5));
                long requestId2 = rs.getLong(6);
                record.put("rid", Long.toString(requestId2));
                record.put("plan_description", rs.getString(7));
                int status = 0;
                if (requests.containsKey(requestId2 + "_1")) {
                    status = 1;
                } else if (requests.containsKey(requestId2 + "_2")) {
                    status = 2;
                }
                if (requests.containsKey(requestId2 + "_" + status)) {
                    C1RequestPaymentInfo inf = (C1RequestPaymentInfo) requests.get(requestId2 + "_" + status);
                    record.put("req_amount", new Double(inf.reqAmount));
                    record.put("rec_amount", new Double(inf.recAmount));
                    record.put("status", Integer.toString(inf.status));
                    record.put("status_date", inf.statusDate);
                }
                ps1.setInt(1, rs.getInt(6));
                ResultSet rs1 = ps1.executeQuery();
                rs1.next();
                record.put("isBlocked", rs1.getString(1));
                list.add((TemplateModel) record);
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            return new TemplateList(list);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_createAccount(int rId) throws Exception {
        try {
            return SignupManager.createAccount(rId);
        } catch (HSUserException e) {
            throw e;
        } catch (Exception ex) {
            Ticket ticket = Ticket.create(ex, "Account Creation failed");
            try {
                User.setTTvalueByRecordId(rId, ticket.getId());
            } catch (Exception ee) {
                Session.getLog().debug("Failed to store Trouble Ticket number for record with id " + rId, ee);
            }
            return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
        }
    }

    TempAccount getTempAccount(int rid) throws Exception {
        RequestRecord record = new RequestRecord(rid);
        return new TempAccount(record.getUserId(), record.getPlanId(), record.getBillingId(), record.getContactId());
    }

    public TemplateModel FM_createTempAccount(int rId) throws Exception {
        return getTempAccount(rId);
    }

    public TemplateModel FM_dumpFakeData(int rId) throws Exception {
        RequestRecord record = new RequestRecord(rId);
        User oldUser = record.getUser();
        record.dump();
        if (oldUser.getAccountIds().isEmpty() && record.isOnlyUserRecord()) {
            oldUser.delete();
        }
        return this;
    }

    public TemplateModel FM_getSignupRecord(int rId) throws Exception {
        long signupId = new RequestRecord(rId).getSignupId();
        SignupRecord sr = SignupManager.getRecordBySignupId(signupId);
        if (sr != null) {
            return new HtmlEncodedHashListScalar(new TemplateSignupRecord(sr));
        }
        return null;
    }

    public TemplateModel FM_getSignupRecordByUserId(long uid) throws Exception {
        return new HtmlEncodedHashListScalar(new TemplateSignupRecord(SignupManager.getRecordByUserId(uid)));
    }

    public TemplateModel FM_getSignupRecordById(long sid) throws Exception {
        return new HtmlEncodedHashListScalar(new TemplateSignupRecord(SignupManager.getRecordBySignupId(sid)));
    }

    public TemplateModel FM_getFakeRequest(int rId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            TemplateList list = new TemplateList();
            ps1 = con.prepareStatement("SELECT count(*) FROM f_user_account WHERE request_id = ?");
            ps1.setInt(1, rId);
            ResultSet rs1 = ps1.executeQuery();
            rs1.next();
            TemplatePair isBlocked = new TemplatePair("isBlocked", rs1.getString(1));
            list.add((TemplateModel) isBlocked);
            ps = con.prepareStatement("SELECT name, value FROM request WHERE id = ? ORDER BY name");
            ps.setInt(1, rId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateHashModel pair = new HtmlEncodedHashListScalar(new TemplatePair(rs.getString(1), rs.getString(2)));
                list.add((TemplateModel) pair);
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_createUser(String login, String password, int refId, int ref_group, String desc, int planId, String mod, long resellerId) throws Exception {
        return createUser(login, password, desc, planId, mod, resellerId);
    }

    public TemplateModel FM_createUser(String login, String password, String desc, int planId, String mod, long resellerId) throws Exception {
        return createUser(login, password, desc, planId, mod, resellerId);
    }

    public static synchronized User createUser(String login, String password, String desc, int planId, String mod, long resellerId) throws Exception {
        int currentPlan = Session.getAccount().getPlan().getId();
        long oldResellerId = Session.getResellerId();
        long oldAccountId = Session.getAccount().getId().getId();
        try {
            Session.setResellerId(resellerId);
            Plan p = Plan.getPlan(planId);
            if (!p.isAccessible(currentPlan)) {
                throw new HSUserException("signupresource.user");
            }
            HttpServletRequest request = Session.getRequest();
            long signupId = setSignupId(request);
            try {
                User.createUser(login, password, resellerId);
                User u = User.getUser(login);
                Session.save();
                Session.setUser(u);
                try {
                    Account a = u.addAccount(planId, "User " + u.getLogin() + " " + desc + " PLanId:" + planId, mod, false);
                    if (a != null) {
                        SignupManager.done(signupId, u.getId(), a.getId().getId());
                        SignupManager.writeLog(oldAccountId, a.getId().getId());
                    }
                    Session.restore();
                    return u;
                } catch (Exception e) {
                    e = e;
                    if (e instanceof AccountCreationException) {
                        e = ((AccountCreationException) e).getReason();
                    }
                    try {
                        u.delete();
                    } catch (Exception e1) {
                        Session.getLog().debug("Unable to delete user", e1);
                    }
                    SignupManager.error(signupId, e.getMessage());
                    throw e;
                }
            } catch (UserException ue) {
                Session.getLog().debug("SignUpService:Got an exception: " + ue);
                SignupManager.error(signupId, ue.getMessage());
                throw new HSUserException("signupresource.exist", "DUP", new Object[]{ue.toString()});
            }
        } finally {
            Session.setResellerId(oldResellerId);
        }
    }

    public TemplateModel FM_setNewCI(int rId, String name, String lastName, String company, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email) throws Exception {
        FM_setNewCI(rId, name, lastName, company, address1, address2, city, state, "", postalCode, country, phone, email);
        return this;
    }

    public TemplateModel FM_setNewCI(int rId, String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email) throws Exception {
        RequestRecord record = new RequestRecord(rId);
        ContactInfoObject ci = record.getContactInfo();
        if (ci.getId() == 0) {
            throw new HSUserException("signupres.ci_dis");
        }
        ci.updateCI(name, lastName, company, address1, address2, city, state, state2, postalCode, country, phone, email);
        HashMap tmpHash = new HashMap();
        tmpHash.put("_ci_first_name", name);
        tmpHash.put("_ci_last_name", lastName);
        tmpHash.put("_ci_company", company);
        tmpHash.put("_ci_address1", address1);
        tmpHash.put("_ci_address2", address2);
        tmpHash.put("_ci_city", city);
        tmpHash.put("_ci_state", state);
        tmpHash.put("_ci_state2", state2);
        tmpHash.put("_ci_postal_code", postalCode);
        tmpHash.put("_ci_country", country);
        tmpHash.put("_ci_phone", phone);
        tmpHash.put("_ci_email", email);
        updateRequest(rId, tmpHash);
        return this;
    }

    public TemplateModel FM_setNewBI(int rId, String name, String lastName, String company, String address1, String address2, String city, String state, String postalCode, String country, String phone, String email, String payType, String bi_cc_name, String bi_cc_number, String bi_cc_exp_month, String bi_cc_exp_year, String bi_cc_type, String bi_cc_issue_no, String bi_cc_start_month, String bi_cc_start_year) throws Exception {
        FM_setNewBI(rId, name, lastName, company, address1, address2, city, state, "", postalCode, country, phone, email, payType, bi_cc_name, bi_cc_number, bi_cc_exp_month, bi_cc_exp_year, bi_cc_type, bi_cc_issue_no, bi_cc_start_month, bi_cc_start_year);
        return this;
    }

    public TemplateModel FM_setNewBI(int rId, String name, String lastName, String company, String address1, String address2, String city, String state, String state2, String postalCode, String country, String phone, String email, String payType, String bi_cc_name, String bi_cc_number, String bi_cc_exp_month, String bi_cc_exp_year, String bi_cc_type, String bi_cc_issue_no, String bi_cc_start_month, String bi_cc_start_year) throws Exception {
        RequestRecord record = new RequestRecord(rId);
        BillingInfoObject bi = record.getBillingInfo();
        if (bi.getId() == 0) {
            throw new HSUserException("signupres.bi_dis");
        }
        bi.updateBI(name, lastName, company, address1, address2, city, state, state2, postalCode, country, phone, email);
        HashMap tmpHash = new HashMap();
        tmpHash.put("_bi_first_name", name);
        tmpHash.put("_bi_last_name", lastName);
        tmpHash.put("_bi_company", company);
        tmpHash.put("_bi_address1", address1);
        tmpHash.put("_bi_address2", address2);
        tmpHash.put("_bi_city", city);
        tmpHash.put("_bi_state", state);
        tmpHash.put("_bi_state2", state2);
        tmpHash.put("_bi_postal_code", postalCode);
        tmpHash.put("_bi_country", country);
        tmpHash.put("_bi_phone", phone);
        tmpHash.put("_bi_email", email);
        tmpHash.put("_bi_type", payType);
        boolean is_ss = false;
        if (bi_cc_issue_no != null && !"".equals(bi_cc_issue_no)) {
            is_ss = true;
        }
        if (payType.equals("CC")) {
            tmpHash.put("_bi_cc_name", bi_cc_name);
            tmpHash.put("_bi_cc_number", GenericCreditCard.getHiddenNumber(bi_cc_number));
            tmpHash.put("_bi_cc_exp_month", bi_cc_exp_month);
            tmpHash.put("_bi_cc_exp_year", bi_cc_exp_year);
            tmpHash.put("_bi_cc_type", bi_cc_type);
            if (is_ss) {
                tmpHash.put("_bi_cc_issue_no", bi_cc_issue_no);
                tmpHash.put("_bi_cc_start_month", bi_cc_start_month);
                tmpHash.put("_bi_cc_start_year", bi_cc_start_year);
            }
        }
        updateRequest(rId, tmpHash);
        if (payType.equals("CC")) {
            GenericCreditCard gcc = (GenericCreditCard) bi.getPaymentInstrument();
            gcc.update(bi_cc_number, bi_cc_name, bi_cc_exp_month, bi_cc_exp_year, bi_cc_type, bi_cc_issue_no, bi_cc_start_month, bi_cc_start_year);
        }
        return this;
    }

    public TemplateModel FM_setNewOtherParams(int rId, String mod, String planCreatedBy, String bi_name, String bi_lastName, String bi_org_name, String bi_address1, String bi_address2, String bi_address3, String bi_city, String bi_state, String bi_postalCode, String bi_country, String bi_phone, String bi_email, String owner_name, String owner_lastName, String owner_org_name, String owner_address1, String owner_address2, String owner_address3, String owner_city, String owner_state, String owner_postalCode, String owner_country, String owner_phone, String owner_email, String domain_name, String domain, String ext, String bi_fax, String owner_fax, String vpshostname) throws Exception {
        HashMap tmpHash = new HashMap();
        if (planCreatedBy.equals("vps")) {
            tmpHash.put("vpshostname", vpshostname);
        } else if ("opensrs".equals(mod)) {
            tmpHash.put("_srs_billing_first_name", bi_name);
            tmpHash.put("_srs_billing_last_name", bi_lastName);
            tmpHash.put("_srs_billing_org_name", bi_org_name);
            tmpHash.put("_srs_billing_address1", bi_address1);
            tmpHash.put("_srs_billing_address2", bi_address2);
            tmpHash.put("_srs_billing_address3", bi_address3);
            tmpHash.put("_srs_billing_city", bi_city);
            tmpHash.put("_srs_billing_state", bi_state);
            tmpHash.put("_srs_billing_postal_code", bi_postalCode);
            tmpHash.put("_srs_billing_country", bi_country);
            tmpHash.put("_srs_billing_phone", bi_phone);
            tmpHash.put("_srs_billing_email", bi_email);
            tmpHash.put("_srs_owner_first_name", owner_name);
            tmpHash.put("_srs_owner_last_name", owner_lastName);
            tmpHash.put("_srs_owner_org_name", owner_org_name);
            tmpHash.put("_srs_owner_address1", owner_address1);
            tmpHash.put("_srs_owner_address2", owner_address2);
            tmpHash.put("_srs_billing_address3", bi_address3);
            tmpHash.put("_srs_owner_city", owner_city);
            tmpHash.put("_srs_owner_state", owner_state);
            tmpHash.put("_srs_owner_postal_code", owner_postalCode);
            tmpHash.put("_srs_owner_country", owner_country);
            tmpHash.put("_srs_owner_phone", owner_phone);
            tmpHash.put("_srs_owner_email", owner_email);
            tmpHash.put("domain_name", domain_name);
            tmpHash.put("domain", domain);
            tmpHash.put("ext", ext);
            tmpHash.put("_srs_billing_fax", bi_fax);
            tmpHash.put("_srs_owner_fax", owner_fax);
        } else {
            tmpHash.put("domain_name", domain_name);
        }
        updateRequest(rId, tmpHash);
        return this;
    }

    public TemplateModel FM_getPlanList(long old_plan_id) throws Exception {
        Collection<Plan> plans = Plan.getPlanList();
        TemplateList list = new TemplateList();
        for (Plan tmpPlan : plans) {
            list.add((TemplateModel) tmpPlan);
        }
        return list;
    }

    public TemplateModel FM_getPlanBPList(long plan_id) throws Exception {
        TemplateList list = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT value FROM plan_value WHERE plan_id = ? AND name = '_PERIOD_TYPES'");
            ps2.setLong(1, plan_id);
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                String tmpPeriods = rs.getString("value");
                Integer.valueOf(tmpPeriods).intValue();
            }
            rs.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT name, value FROM plan_value WHERE plan_id = ? AND name like '_PERIOD_TYPE_%'");
            ps3.setLong(1, plan_id);
            ResultSet rs2 = ps3.executeQuery();
            while (rs2.next()) {
                String typeName = rs2.getString("name");
                String typeValue = rs2.getString("value");
                if (!"_PERIOD_TYPES".equals(typeName)) {
                    String typeCount = typeName.substring(13);
                    TemplateModel templateMap = new TemplateMap();
                    templateMap.put("p_count", typeCount);
                    templateMap.put("p_full_count", "_PERIOD_TYPE_" + typeCount);
                    templateMap.put("p_name", typeValue);
                    list.add(templateMap);
                }
            }
            rs2.close();
            ps = con.prepareStatement("SELECT value FROM plan_value WHERE plan_id = ? AND name = ?");
            while (list.hasNext()) {
                TemplateMap tmpMap = (TemplateMap) list.next();
                String valueName = "_PERIOD_SIZE_" + tmpMap.get("p_count");
                ps.setLong(1, plan_id);
                ps.setString(2, valueName);
                ResultSet rs3 = ps.executeQuery();
                while (rs3.next()) {
                    String sizeValue = rs3.getString("value");
                    tmpMap.put("p_size", sizeValue);
                }
                rs3.close();
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setNewPlanAndBP(int rId, String plan_id, String bp) throws Exception {
        HashMap tmpHash = new HashMap();
        tmpHash.put("plan_id", plan_id);
        tmpHash.put("_bp", bp);
        int planId = Integer.parseInt(plan_id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE request_record SET plan_id  = ? WHERE request_id = ?");
            ps.setInt(1, planId);
            ps.setInt(2, rId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            updateRequest(rId, tmpHash);
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setNewPassword(int rId, String password) throws Exception {
        HashMap tmpHash = new HashMap();
        tmpHash.put("password", password);
        tmpHash.put("password2", password);
        updateRequest(rId, tmpHash);
        return this;
    }

    protected void updateRequest(int rId, Map values) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE request SET value = ? WHERE id = ? AND name = ?");
            ps.setInt(2, rId);
            for (String tmpKey : values.keySet()) {
                String tmpValue = (String) values.get(tmpKey);
                ps.setString(1, tmpValue);
                ps.setString(3, tmpKey);
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected Hashtable getFailedAccounts() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        Hashtable fAccounts = new Hashtable();
        try {
            ps = con.prepareStatement("SELECT a.account_id, a.signup_id, c.plan_id, b.username, c.created, d.description, a.request_id FROM f_user_account a , users b, accounts c, plans d WHERE a.user_id = b.id AND a.account_id = c.id AND c.plan_id = d.id AND b.reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Session.getLog().debug("Got failed account planId=" + rs.getString("plan_id") + " username=" + rs.getString("username"));
                String planId = rs.getString("plan_id");
                TemplateModel templateHash = new TemplateHash();
                templateHash.put("id", rs.getString("account_id"));
                templateHash.put("signup_id", rs.getString("signup_id"));
                templateHash.put("username", rs.getString("username"));
                templateHash.put("created", rs.getTimestamp("created"));
                templateHash.put("request_id", rs.getString("request_id"));
                if (!fAccounts.containsKey(planId)) {
                    fAccounts.put(planId, new TemplateList());
                }
                ((TemplateList) fAccounts.get(planId)).add(templateHash);
            }
            Session.closeStatement(ps);
            con.close();
            return fAccounts;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getFailedAccount(long id) throws Exception {
        return new TemplateHash(getFailedAccount(id));
    }

    /* JADX WARN: Finally extract failed */
    public Hashtable getFailedAccount(long id) throws Exception {
        long uid = -1;
        long accid = -1;
        PreparedStatement ps = null;
        Hashtable result = new Hashtable();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT user_id, account_id, signup_id, request_id FROM f_user_account WHERE account_id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                uid = rs.getLong("user_id");
                accid = rs.getLong("account_id");
                result.put("user_id", rs.getString("user_id"));
                result.put("acc_id", rs.getString("account_id"));
                result.put("signup_id", rs.getString("signup_id"));
                result.put("request_id", rs.getString("request_id"));
                result.put("srecord", new TemplateSignupRecord(SignupManager.getRecordBySignupId(rs.getInt("signup_id"))));
            }
            Session.closeStatement(ps);
            con.close();
            User oldUser = Session.getUser();
            User u = User.getUser(uid);
            Account a = null;
            if (u != null) {
                try {
                    Session.setUser(u);
                    a = (Account) Account.get(new ResourceId(accid, 0));
                    Session.setUser(oldUser);
                } catch (Throwable th) {
                    Session.setUser(oldUser);
                    throw th;
                }
            }
            result.put("account", a);
            return result;
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    public TemplateModel FM_completeFailedSignup(long id, String charge) throws Exception {
        return completeFailedSignup(id, charge != null && charge.length() > 0);
    }

    public Account completeFailedSignup(long id, boolean charge) throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        Hashtable fa = getFailedAccount(id);
        Account a = (Account) fa.get("account");
        int requestId = Integer.parseInt((String) fa.get("request_id"));
        Connection con = Session.getTransConnection();
        try {
            try {
                ps = con.prepareStatement("INSERT INTO user_account(account_id, user_id) SELECT account_id, user_id FROM f_user_account WHERE account_id = ?");
                ps.setLong(1, id);
                ps.executeUpdate();
                ps1 = con.prepareStatement("DELETE FROM f_user_account WHERE account_id = ?");
                ps1.setLong(1, id);
                ps1.executeUpdate();
                if (requestId != 0) {
                    RequestRecord rr = new RequestRecord(requestId);
                    rr.dump();
                }
                con.commit();
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.commitTransConnection(con);
                User oldUser = Session.getUser();
                Account oldAccount = Session.getAccount();
                try {
                    Session.setUser(a.getUser());
                    Session.setAccount(a);
                    if (charge) {
                        a.getBill().charge(a.getBillingInfo());
                    } else {
                        fixBalance(a);
                    }
                    return a;
                } finally {
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                }
            } catch (Exception ex) {
                con.rollback();
                Session.getLog().error("Error completing signup ", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    private void fixBalance(Account a) throws Exception {
        Date now = TimeUtils.getDate();
        Bill bill = a.getBill();
        double amount = bill.getBalance();
        if (amount == 0.0d) {
            return;
        }
        bill.addEntry(6, now, -1L, -1, "Initial balance adjustment", now, (Date) null, (String) null, amount);
        bill.setCredit(0.0d);
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("INSERT INTO payment (account_id, amount, id, description, short_desc, entered) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, a.getId().getId());
            ps.setDouble(2, amount);
            ps.setString(3, "");
            ps.setString(4, "Starting balance");
            ps.setString(5, "OTHER");
            ps.setDate(6, new java.sql.Date(now.getTime()));
            ps.executeUpdate();
            con.close();
            a.initBill(now, true);
        } catch (Throwable th) {
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_deleteFailedSignup(long signupId) throws Exception {
        deleteFailedSignup(signupId);
        return this;
    }

    public void deleteFailedSignup(long id) throws Exception {
        PreparedStatement ps = null;
        Session.save();
        Connection con = Session.getDb();
        Hashtable failedAcc = getFailedAccount(id);
        Account a = (Account) failedAcc.get("account");
        long uid = Long.parseLong((String) failedAcc.get("user_id"));
        Long.parseLong((String) failedAcc.get("acc_id"));
        try {
            try {
                Session.setUser(User.getUser(uid));
                Session.setAccount(a);
                a.delete(true);
                ps = con.prepareStatement("DELETE FROM f_user_account WHERE account_id = ?");
                ps.setLong(1, id);
                ps.executeUpdate();
                Session.closeStatement(ps);
                Session.restore();
            } catch (Exception ex) {
                Session.getLog().error("Error deleting signup ", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.restore();
            throw th;
        }
    }

    public TemplateModel FM_approveTaxExemptionNew(String exemptionCode, int tempAccountId) throws Exception {
        try {
            TempAccount a = getTempAccount(tempAccountId);
            BillingInfoObject bi = a.getBi();
            if (bi == null) {
                throw new Exception();
            }
            bi.approveTaxExemption(exemptionCode);
            a.sendBillingMessage("MODERATED_TEXEMPT_APPROVED");
            return this;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get Billing Info for the Temp Account id=" + tempAccountId, ex);
            throw new HSUserException("bi.unable_get_info");
        }
    }

    public TemplateModel FM_rejectTaxExemptionNew(String exemptionCode, int tempAccountId) throws Exception {
        try {
            TempAccount a = getTempAccount(tempAccountId);
            BillingInfoObject bi = a.getBi();
            if (bi == null) {
                throw new Exception();
            }
            bi.rejectTaxExemption(exemptionCode);
            a.sendBillingMessage("MODERATED_TEXEMPT_REJECTED");
            return this;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to get Billing Info for the Temp Account id=" + tempAccountId, ex);
            throw new HSUserException("bi.unable_get_info");
        }
    }
}
