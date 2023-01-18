package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Category;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/SignupGuard.class */
public class SignupGuard implements TemplateHashModel {
    public static final int DOMAINS = 1;
    public static final int IPS = 2;
    public static final int EMAILS = 3;
    public static final int PHONES = 4;
    public static final int COUNTRY_CODES = 5;
    public static final int CC_NUMBERS = 6;
    public static final int NAMES = 7;
    public static final int USERNAMES = 8;
    public static final int EMAIL_DOMAINS = 9;
    public static final int MATCH_IP_COUNTRY_FLAG = 1;
    public static final int DOMAIN_REG_FLAG = 2;
    public static final int DOMAIN_TRANSFER_FLAG = 4;
    public static final int DOMAIN_STOPGAP_FLAG = 8;
    public static final int DOMAIN_3RDLEVEL_FLAG = 16;
    public static final int NO_DOMAIN_FLAG = 32;
    public static final int TRIAL_ACCOUNT_FLAG = 64;
    public static final int MODERATE_EVERYTHING_FLAG = 128;
    public static final int CVV_VALIDATION = 256;
    Set domains;
    Set ips;
    Set emails;
    Set phones;
    Set countryCodes;
    Set ccNumbers;
    Set names;
    Set usernames;
    List emailDomains;
    boolean matchIPCountry;
    boolean domainReg;
    boolean domainTransfer;
    boolean domainStopgap;
    boolean domain3rdLevel;
    boolean noDomain;
    boolean trialAccount;
    boolean moderateEverything;
    boolean cvvValidation;
    double maxAmount;
    private static Category log = Category.getInstance(SignupGuard.class.getName());
    public static HashMap map = new HashMap();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return key.equals("MATCH_IP_COUNTRY_FLAG") ? new TemplateString(this.matchIPCountry) : key.equals("DOMAIN_REG_FLAG") ? new TemplateString(this.domainReg) : key.equals("DOMAIN_TRANSFER_FLAG") ? new TemplateString(this.domainTransfer) : key.equals("DOMAIN_STOPGAP_FLAG") ? new TemplateString(this.domainStopgap) : key.equals("DOMAIN_3RDLEVEL_FLAG") ? new TemplateString(this.domain3rdLevel) : key.equals("NO_DOMAIN_FLAG") ? new TemplateString(this.noDomain) : key.equals("TRIAL_ACCOUNT_FLAG") ? new TemplateString(this.trialAccount) : key.equals("MODERATE_EVERYTHING_FLAG") ? new TemplateString(this.moderateEverything) : key.equals("maxAmount") ? new TemplateString(this.maxAmount) : key.equals("domains") ? new TemplateList(this.domains) : key.equals("ips") ? new TemplateList(this.ips) : key.equals("emails") ? new TemplateList(this.emails) : key.equals("phones") ? new TemplateList(this.phones) : key.equals("countryCodes") ? new TemplateList(this.countryCodes) : key.equals("ccNumbers") ? new TemplateList(this.ccNumbers) : key.equals("names") ? new TemplateList(this.names) : key.equals("usernames") ? new TemplateList(this.usernames) : key.equals("emailDomains") ? new TemplateList(this.emailDomains) : key.equals("CVV_VALIDATION") ? new TemplateString(this.cvvValidation) : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public static synchronized SignupGuard get() throws Exception {
        SignupGuard guard = (SignupGuard) map.get(new Long(Session.getResellerId()));
        if (guard == null) {
            guard = new SignupGuard();
            map.put(new Long(Session.getResellerId()), guard);
        }
        return guard;
    }

    public SignupGuard() throws Exception {
        load();
    }

    protected void load() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT flags, max_amount FROM signup_guard WHERE reseller_id = ?");
            log.info("------SIGNUPGUARD----->" + Session.getResellerId());
            ps2.setLong(1, Session.getResellerId());
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                this.maxAmount = rs.getDouble(2);
                int flag = rs.getInt(1);
                this.matchIPCountry = (flag & 1) != 0;
                this.domainReg = (flag & 2) != 0;
                this.domainTransfer = (flag & 4) != 0;
                this.domainStopgap = (flag & 8) != 0;
                this.domain3rdLevel = (flag & 16) != 0;
                this.noDomain = (flag & 32) != 0;
                this.trialAccount = (flag & 64) != 0;
                this.moderateEverything = (flag & MODERATE_EVERYTHING_FLAG) != 0;
                this.cvvValidation = (flag & CVV_VALIDATION) != 0;
            } else {
                this.maxAmount = 100000.0d;
            }
            Session.closeStatement(ps2);
            ps = con.prepareStatement("SELECT list_type, value FROM signup_guard_black_list WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs2 = ps.executeQuery();
            this.domains = new HashSet();
            this.ips = new HashSet();
            this.emails = new HashSet();
            this.phones = new HashSet();
            this.countryCodes = new HashSet();
            this.ccNumbers = new HashSet();
            this.names = new HashSet();
            this.names = new HashSet();
            this.usernames = new HashSet();
            this.emailDomains = new ArrayList();
            while (rs2.next()) {
                switch (rs2.getInt(1)) {
                    case 1:
                        this.domains.add(rs2.getString(2));
                        break;
                    case 2:
                        this.ips.add(rs2.getString(2));
                        break;
                    case 3:
                        this.emails.add(rs2.getString(2));
                        break;
                    case 4:
                        this.phones.add(rs2.getString(2));
                        break;
                    case 5:
                        this.countryCodes.add(rs2.getString(2));
                        break;
                    case 6:
                        this.ccNumbers.add(rs2.getString(2));
                        break;
                    case 7:
                        this.names.add(rs2.getString(2));
                        break;
                    case 8:
                        this.usernames.add(rs2.getString(2));
                        break;
                    case 9:
                        this.emailDomains.add(rs2.getString(2));
                        break;
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateList FM_getWarnings(int requestId) throws Exception {
        List list = getWarnings(requestId);
        if (list == null) {
            return null;
        }
        return new TemplateList(list);
    }

    public static List getWarnings(int requestId) throws Exception {
        PreparedStatement ps = null;
        List list = new ArrayList();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT value FROM signup_guard_comment, request_record WHERE signup_guard_comment.user_id = request_record.user_id AND request_record.request_id = ?");
            ps.setInt(1, requestId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                list.add(new TemplateString(rs.getString(1)));
            }
            if (list.size() > 0) {
                Session.closeStatement(ps);
                con.close();
                return list;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getBlackList(int type) throws Exception {
        switch (type) {
            case 1:
                return new TemplateList(this.domains);
            case 2:
                return new TemplateList(this.ips);
            case 3:
                return new TemplateList(this.emails);
            case 4:
                return new TemplateList(this.phones);
            case 5:
                return new TemplateList(this.countryCodes);
            case 6:
                return new TemplateList(this.ccNumbers);
            case 7:
                return new TemplateList(this.names);
            case 8:
                return new TemplateList(this.usernames);
            case 9:
                return new TemplateList(this.emailDomains);
            default:
                return null;
        }
    }

    public void FM_updateBlackList(int type, String blackList) throws Exception {
        Collection list;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM signup_guard_black_list WHERE reseller_id = ? AND list_type = ?");
            ps2.setLong(1, Session.getResellerId());
            ps2.setInt(2, type);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("INSERT INTO signup_guard_black_list (reseller_id, list_type, value) VALUES (?, ?, ?)");
            ps.setLong(1, Session.getResellerId());
            ps.setInt(2, type);
            if (type == 9) {
                list = new ArrayList();
            } else {
                list = new HashSet();
            }
            StringTokenizer st = new StringTokenizer(blackList, ";,\n");
            while (st.hasMoreTokens()) {
                String token = st.nextToken().toUpperCase().trim();
                if (token.length() != 0 && !list.contains(token)) {
                    list.add(token);
                    ps.setString(3, token);
                    ps.executeUpdate();
                }
            }
            switch (type) {
                case 1:
                    this.domains = (Set) list;
                    break;
                case 2:
                    this.ips = (Set) list;
                    break;
                case 3:
                    this.emails = (Set) list;
                    break;
                case 4:
                    this.phones = (Set) list;
                    break;
                case 5:
                    this.countryCodes = (Set) list;
                    break;
                case 6:
                    this.ccNumbers = (Set) list;
                    break;
                case 7:
                    this.names = (Set) list;
                    break;
                case 8:
                    this.usernames = (Set) list;
                    break;
                case 9:
                    this.emailDomains = (List) list;
                    break;
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void FM_updateFlags(int _matchIPCountry, int _domainReg, int _domainTransfer, int _domainStopgap, int _domain3rdLevel, int _noDomain, int _trialAccount, int _moderateEverything, double maxAmount, int _cvvValidation) throws Exception {
        this.matchIPCountry = 1 == _matchIPCountry;
        this.domainReg = 1 == _domainReg;
        this.domainTransfer = 1 == _domainTransfer;
        this.domainStopgap = 1 == _domainStopgap;
        this.domain3rdLevel = 1 == _domain3rdLevel;
        this.noDomain = 1 == _noDomain;
        this.trialAccount = 1 == _trialAccount;
        this.moderateEverything = 1 == _moderateEverything;
        this.maxAmount = maxAmount;
        this.cvvValidation = 1 == _cvvValidation;
        int flag = (1 * _matchIPCountry) + (2 * _domainReg) + (4 * _domainTransfer) + (8 * _domainStopgap) + (16 * _domain3rdLevel) + (32 * _noDomain) + (64 * _trialAccount) + (MODERATE_EVERYTHING_FLAG * _moderateEverything) + (CVV_VALIDATION * _cvvValidation);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM signup_guard WHERE reseller_id = ?");
            ps2.setLong(1, Session.getResellerId());
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("INSERT INTO signup_guard (reseller_id, flags, max_amount) VALUES (?, ?, ?)");
            ps.setLong(1, Session.getResellerId());
            ps.setInt(2, flag);
            ps.setDouble(3, maxAmount);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static List checkSignup(long userId, int planId, String mod, BillingInfoObject bi, Plan p, boolean isExternal) {
        List l = null;
        try {
            SignupGuard guard = get();
            l = guard._checkSignup(planId, mod);
            if (bi.getBillingType() == 1 && "1".equals(Settings.get().getValue("moderated_cc"))) {
                l.add(Localizer.translateMessage("signup.guard.cc_moderated"));
            }
            if (guard.trialAccount && bi.getBillingType() == -1) {
                l.add(Localizer.translateMessage("signup.guard.trialAccountModerate"));
            }
            if (bi.getBillingType() == 0 && isExternal && p.isFullyAccessible() && p.getValue("_CREATED_BY_") != null && !FMACLManager.ADMIN.equals(p.getValue("_CREATED_BY_"))) {
                l.add(Localizer.translateMessage("signup.guard.wobilling"));
            }
            saveComments(userId, l);
        } catch (Throwable t) {
            try {
                log.warn("Error processing signup", t);
                l = new ArrayList();
                l.add(t.getMessage());
                Ticket.create(t, null, "SignupGuard:checkSingup");
                saveComments(userId, l);
            } catch (Throwable th) {
                saveComments(userId, l);
                throw th;
            }
        }
        return l;
    }

    /* JADX WARN: Finally extract failed */
    public static void saveComments(long userId, List l) {
        if (l == null && l.size() == 0) {
            return;
        }
        PreparedStatement ps = null;
        Connection con = null;
        try {
            con = Session.getDb();
            ps = con.prepareStatement("INSERT INTO signup_guard_comment (user_id, value) VALUES (?, ?)");
            ps.setLong(1, userId);
            Iterator i = l.iterator();
            while (i.hasNext()) {
                String str = (String) i.next();
                if (str == null || str.length() == 0) {
                    str = "Unknown Problem";
                }
                ps.setString(2, str);
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                }
            }
        } catch (Throwable t) {
            try {
                log.warn("Error writting to database", t);
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException e2) {
                    }
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException e3) {
                        throw th;
                    }
                }
                throw th;
            }
        }
    }

    protected List _checkSignup(int planId, String mod) throws Exception {
        Plan p = Plan.getPlan(planId);
        HttpServletRequest request = Session.getRequest();
        List list = new ArrayList();
        Set set = this.domains;
        String tmp = request.getParameter("domain_name");
        if (set.contains(normalize(tmp))) {
            list.add(message("domains", tmp));
        }
        Set set2 = this.usernames;
        String tmp2 = request.getParameter("login");
        if (set2.contains(normalize(tmp2))) {
            list.add(message("usernames", tmp2));
        }
        Set set3 = this.ips;
        String tmp3 = request.getRemoteAddr();
        if (set3.contains(tmp3)) {
            list.add(message("ips", tmp3));
        }
        Set set4 = this.emails;
        String tmp4 = request.getParameter("_ci_email");
        if (set4.contains(normalize(tmp4))) {
            list.add(message("ci_emails", tmp4));
        }
        Set set5 = this.emails;
        String tmp5 = request.getParameter("_bi_email");
        if (set5.contains(normalize(tmp5))) {
            list.add(message("bi_emails", tmp5));
        }
        Set set6 = this.phones;
        String tmp6 = request.getParameter("_ci_phone");
        if (set6.contains(HsphereToolbox.stripPhone(tmp6))) {
            list.add(message("ci_phones", tmp6));
        }
        Set set7 = this.phones;
        String tmp7 = request.getParameter("_bi_phone");
        if (set7.contains(HsphereToolbox.stripPhone(tmp7))) {
            list.add(message("bi_phones", tmp7));
        }
        Set set8 = this.countryCodes;
        String tmp8 = request.getParameter("_ci_country");
        if (set8.contains(tmp8)) {
            list.add(message("ci_country", tmp8));
        }
        Set set9 = this.countryCodes;
        String tmp9 = request.getParameter("_bi_country");
        if (set9.contains(normalize(tmp9))) {
            list.add(message("bi_country", tmp9));
        }
        Set set10 = this.countryCodes;
        String tmp10 = SignupManager.getCountryCode(request.getRemoteAddr());
        if (set10.contains(tmp10)) {
            list.add(message("ip_country", tmp10));
        }
        Set set11 = this.names;
        String tmp11 = getName(request.getParameter("_ci_first_name"), request.getParameter("_ci_last_name"));
        if (set11.contains(tmp11)) {
            list.add(message("ci_name", tmp11));
        }
        Set set12 = this.names;
        String tmp12 = getName(request.getParameter("_bi_first_name"), request.getParameter("_bi_last_name"));
        if (set12.contains(tmp12)) {
            list.add(message("bi_name", tmp12));
        }
        Set set13 = this.names;
        String tmp13 = request.getParameter("_bi_cc_name");
        if (set13.contains(normalize(tmp13))) {
            list.add(message("cc_name", tmp13));
        }
        Set set14 = this.ccNumbers;
        String tmp14 = request.getParameter("_bi_cc_number");
        if (set14.contains(tmp14)) {
            list.add(message("cc_number", GenericCreditCard.getHiddenNumber(tmp14)));
        }
        String detectedCountryCode = SignupManager.getCountryCode(request.getRemoteAddr());
        String BICountryCode = request.getParameter("_bi_country");
        Session.getLog().debug("Signup guard: REMOTE_ADDR is" + request.getRemoteAddr() + " detected country is " + detectedCountryCode + " BI country code is " + BICountryCode);
        if (this.matchIPCountry && !detectedCountryCode.equals(BICountryCode) && !"TRIAL".equals(request.getParameter("_bi_type"))) {
            list.add(message("matchIPCountry", BICountryCode, detectedCountryCode));
        }
        if (this.domainReg && "new_opensrs_domain".equals(request.getParameter("type_domain"))) {
            list.add(message("domainReg"));
        }
        if (this.domainTransfer && "transfer_new_misc_domain".equals(request.getParameter("type_domain"))) {
            list.add(message("domainTransfer"));
        }
        if (this.domainStopgap && "without_domain".equals(request.getParameter("type_domain"))) {
            list.add(message("domainStopgap"));
        }
        if (this.domain3rdLevel && "3ldomain".equals(request.getParameter("type_domain"))) {
            list.add(message("domain3rdLevel"));
        }
        if (this.noDomain && "empty_domain".equals(request.getParameter("type_domain"))) {
            list.add(message("noDomain"));
        }
        if (p.getBilling() != 0) {
            if (this.trialAccount && "TRIAL".equals(request.getParameter("_bi_type"))) {
                list.add(message("trialAccount"));
            }
            try {
                Invoice invoice = Invoice.getInvoice();
                if (invoice.getTotal() >= this.maxAmount) {
                    list.add(message("maxAmount", HsphereToolbox.translateCurrency(invoice.getTotal())));
                }
            } catch (Exception npe) {
                log.warn("NPE getting the invoice", npe);
            }
        }
        String exemptionCode = request.getParameter("_bi_exemption_code");
        if (exemptionCode != null && !"".equals(exemptionCode)) {
            list.add(message("taxExemptionApprovalNeeded"));
        }
        if (this.moderateEverything) {
            list.add(message("moderateEverything"));
        }
        return list;
    }

    protected String message(String key, String str) {
        return Localizer.translateMessage("signup.guard." + key, new Object[]{str});
    }

    protected String message(String key) {
        return Localizer.translateMessage("signup.guard." + key);
    }

    protected String message(String key, String str1, String str2) {
        return Localizer.translateMessage("signup.guard." + key, new Object[]{str1, str2});
    }

    protected boolean checkEmail(String str) {
        String str2 = normalize(str);
        for (String ext : this.emailDomains) {
            if (str2.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    protected String normalize(String str) {
        return str == null ? "" : str.trim().toUpperCase();
    }

    protected String getName(String first, String last) {
        if (first == null) {
            first = "";
        }
        if (last == null) {
            last = "";
        }
        return normalize(first.trim() + " " + last.trim());
    }
}
