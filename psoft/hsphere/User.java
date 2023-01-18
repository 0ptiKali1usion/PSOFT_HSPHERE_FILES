package psoft.hsphere;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import freemarker.template.TemplateServletUtils;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.epayment.PaymentInstrument;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.hsphere.payment.ExternalPaymentManager;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.email.Mailbox;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.ContactInfoObject;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.user.UserDuplicateLoginException;
import psoft.user.UserException;
import psoft.user.UserLoginException;
import psoft.user.UserSignupException;
import psoft.util.Config;
import psoft.util.NFUCache;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.ConfigModel;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.validators.Accessor;
import psoft.validators.CookieAccessor;
import psoft.validators.NameModifier;
import psoft.validators.ServletRequestAccessor;
import psoft.web.utils.Redirect;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/User.class */
public class User implements TemplateHashModel {
    protected HashSet accountIds;
    protected static String USER_LOGIN_QUERY;
    protected static boolean redirectType;
    protected PrefixHolder prefixes;

    /* renamed from: id */
    protected long f58id;
    protected long resellerId;
    protected long referal_id;
    protected int referal_group;
    protected String login;
    protected String password;
    protected Account activeAccount;
    protected long paymentRequest;
    protected int requestId;
    protected static NFUCache cache = null;

    public static void initParams() {
        USER_LOGIN_QUERY = Session.getPropertyString("USER_LOGIN_QUERY");
        try {
            redirectType = Session.getPropertyString("REDIRECT_TYPE").toUpperCase().equals("LOCATION");
        } catch (Exception e) {
            redirectType = true;
        }
    }

    public static void setCache(NFUCache newCache) {
        cache = newCache;
    }

    public static NFUCache getCache() {
        return cache;
    }

    protected User(long id, String login, String password, long resellerId) {
        this.prefixes = new PrefixHolder();
        this.f58id = id;
        this.login = login;
        this.password = password;
        this.resellerId = resellerId;
    }

    protected User(long id, String login, String password, long referal_id, int referal_group, long resellerId) {
        this(id, login, password, resellerId);
    }

    public String getLogin() {
        return this.login;
    }

    public String getPassword() {
        return this.password;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public static User getUser(String login) throws Exception {
        return getUser(login, false);
    }

    public static User getUser(long id) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT username FROM users WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user = getUser(rs.getString(1));
                Session.closeStatement(ps);
                con.close();
                return user;
            }
            throw new HSUserException("user.cannot_login");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static User getUser(String login, boolean reload) throws Exception {
        if (login == null) {
            throw new UserException("No user login specified. ");
        }
        User user = null;
        if (!reload) {
            user = (User) cache.get(login);
        }
        if (user != null) {
            return user;
        }
        try {
            if (USER_LOGIN_QUERY == null || "".equals(USER_LOGIN_QUERY)) {
                initParams();
            }
            Session.getLog().warn("No user in cache " + USER_LOGIN_QUERY);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement(USER_LOGIN_QUERY);
            ps.setString(1, login);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                User user2 = new User(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getLong(4));
                cache.put(login, user2);
                Session.closeStatement(ps);
                Session.closeStatement(null);
                con.close();
                return user2;
            }
            throw new HSUserException("user.cannot_login");
        } catch (SQLException se) {
            Session.getLog().error("getUser", se);
            throw new UserException("Internal problem, try again");
        }
    }

    public static String getRequestURL(HttpServletRequest request) {
        String url = request.getRequestURI() + "?";
        String query = "";
        Enumeration en = request.getParameterNames();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            String[] values = request.getParameterValues(key);
            for (int i = 0; i < values.length; i++) {
                query = query + URLEncoder.encode(key) + "=" + URLEncoder.encode(values[i]);
                if (i < values.length - 1) {
                    query = query + "&";
                }
            }
            if (en.hasMoreElements()) {
                query = query + "&";
            }
        }
        if (query != null) {
            url = url + query;
        } else {
            Session.getLog().warn("NULL query");
        }
        return url;
    }

    public static void loginUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User u = getUser(request.getParameter("login"), true);
        if (u.getResellerId() != Session.getResellerId()) {
            throw new UserException(Localizer.translateMessage("user.cannot_login"));
        }
        if (u.getPassword().equals(request.getParameter("password"))) {
            setToken(u, request, response);
            String redirectStr = request.getParameter("requestURL");
            if (redirectStr == null || redirectStr.length() == 0) {
                redirectStr = u.getURI();
                String acId = request.getParameter("account_id");
                if (acId != null && acId.length() > 0) {
                    redirectStr = redirectStr + new ResourceId(Integer.parseInt(acId), 0) + "/";
                }
            }
            Redirect.sendRedirect(response, redirectStr, redirectType);
            throw new UserLoginException("try just one more time");
        }
        throw new UserException(Localizer.translateMessage("user.cannot_login"));
    }

    public static void forgetPass(HttpServletRequest request, HttpServletResponse response) throws UserException {
        Session.getLog().debug("----->Find user and send eml ");
        long oldResellerId = 0;
        try {
            oldResellerId = Session.getResellerId();
        } catch (UnknownResellerException e) {
        }
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            try {
                User user = getUser(request.getParameter("login"));
                Session.setUser(user);
                HashSet hsaccounts = user.getAccountIds();
                Iterator iter = hsaccounts.iterator();
                if (iter.hasNext()) {
                    Account a = user.getAccount((ResourceId) iter.next());
                    String eml = a.getContactInfo().getEmail();
                    SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
                    CustomEmailMessage.send("LOST_PASSWORD", eml, (TemplateModelRoot) root);
                    throw new UserException("Mail has been successfully sent");
                }
                throw new UserException("User has no accounts");
            } catch (Exception ex) {
                if (ex instanceof UserException) {
                    throw ((UserException) ex);
                }
                Session.getLog().info("LOST_PASSWORD", ex);
                throw new UserException("User not found");
            }
        } finally {
            try {
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                Session.getResellerId();
            } catch (UnknownResellerException e2) {
                if (oldResellerId > 0) {
                    try {
                        Session.setResellerId(oldResellerId);
                    } catch (Exception e3) {
                    }
                }
            }
        }
    }

    public static User _getUser(HttpServletRequest request) throws Exception {
        Accessor data = new CookieAccessor(request.getCookies());
        String macCookie = data.get("MACTOKEN");
        if (macCookie == null || "".equals(macCookie)) {
            throw new UserLoginException("Empty Cookies ...");
        }
        Session.getLog().debug("MACToken" + macCookie);
        MACToken token = new MACToken(macCookie);
        User u = getUser(token.getLogin());
        if (token.isValid(u.getPassword())) {
            return u;
        }
        throw new UserException("Invalid Token Digest");
    }

    public static User getUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        try {
            return _getUser(request);
        } catch (UserException ue) {
            TemplateModelRoot root = Session.getModelRoot();
            Template t = null;
            String requestURL = getRequestURL(request);
            root.put("requestURL", new TemplateString(requestURL));
            root.put("error", new TemplateString(ue.getMessage()));
            root.put("requestURL_enc", new TemplateString(URLEncoder.encode(requestURL)));
            root.put("servletURL", new TemplateString(request.getRequestURI()));
            String tName = request.getParameter("template_name");
            if (HsphereToolbox.isOnlineHelpTemplate(tName)) {
                try {
                    t = Session.getTemplate(tName);
                } catch (Exception e) {
                }
            }
            if (t == null) {
                String tName2 = request.getParameter("template");
                if (tName2 != null) {
                    t = Session.getTemplate(tName2);
                } else {
                    String tName3 = Config.getProperty("psoft.user", "logintemplate");
                    t = Session.getTemplate(tName3);
                    if (t == null) {
                        Ticket.create(new Exception("H-Sphere template problem"), "User login", Localizer.translateMessage("error.cannot_find_template", new String[]{tName3}));
                        throw new TemplateException("Cannot find a template. Contact your system administrator.");
                    }
                }
            }
            t.process(root, response.getWriter());
            throw new UserLoginException("To proceed enter the correct login and password.");
        }
    }

    public static void createUser(String login, String password, String referal_id, String referal_group, long resellerId) throws UserException {
        createUser(login, password, resellerId);
    }

    public static void createUser(String login, String password, long resellerId) throws UserException {
        try {
            long id = Session.getNewIdAsLong("user_id");
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO users (id, username, password, reseller_id) VALUES(?, ?, ?, ?)");
            ps.setLong(1, id);
            ps.setString(2, login);
            ps.setString(3, password);
            ps.setLong(4, resellerId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (SQLException se) {
            throw new UserException("Unable to add new user: " + se.getMessage());
        }
    }

    public static User createNewUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User u = null;
        boolean newUser = true;
        try {
            Session.getLog().debug("Get new user");
            u = _getUser(request);
            newUser = false;
            Session.getLog().debug("New user got" + u + " newUser:false");
        } catch (Exception e) {
        }
        Plan p = Plan.getPlan(request.getParameter("plan_id"));
        Session.getLog().debug("Plan ID:" + request.getParameter("plan_id") + " ResellerId:" + Session.getResellerId() + " Plan:" + p);
        if ((newUser && !p.isAccessible(0)) || ((!newUser && !u.isAccessible(p)) || p.isDisabled())) {
            Session.getLog().debug("Plan:" + p + " User:" + u + " Plan is not accessible");
            throw new HSUserException("user.plan.inaccessible");
        }
        if (newUser) {
            try {
                createUser(request.getParameter("login"), request.getParameter("password"), Session.getResellerId());
                u = getUser(request.getParameter("login"));
            } catch (UserException e2) {
                throw new UserDuplicateLoginException("Duplicate login.");
            }
        }
        Session.setUser(u);
        Session.getModelRoot().put(FMACLManager.USER, u);
        if (newUser) {
            setToken(u, request, response);
        }
        try {
            String description = p.get("description").toString();
            Session.getLog().debug("User :" + u.getLogin() + " User-resellerID:" + u.getResellerId() + " Session.resellerId()" + Session.getResellerId());
            Account ac = u.addAccount(p.getId(), "User " + u.getLogin() + " " + description, request.getParameter("_mod"), false);
            Session.getModelRoot().put("account", ac);
            return u;
        } catch (Exception e3) {
            e = e3;
            Session.getLog().error("Unable to create account: ", e);
            if (newUser) {
                try {
                    if (e instanceof AccountCreationException) {
                        e = ((AccountCreationException) e).getReason();
                    } else {
                        u.delete();
                    }
                } catch (Exception e1) {
                    Session.getLog().error("Unable to delete user: ", e1);
                    throw e;
                }
            }
            throw e;
        }
    }

    public String getURI() {
        return Session.getProperty("CP_URI") + '/' + getLogin() + '/';
    }

    public static void setToken(User u, HttpServletRequest request, HttpServletResponse response) {
        MACToken token = new MACToken(u.getLogin(), u.getPassword());
        Cookie c = new Cookie("MACTOKEN", token.getValue());
        c.setPath(u.getURI());
        Session.getLog().debug("MACToken path " + u.getURI());
        response.addCookie(c);
    }

    public static void signupUser(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        String template_name = request.getParameter("template_name");
        if (template_name == null) {
            template_name = "signup/user_signup.html";
        }
        Template template = Session.getTemplate(template_name);
        TemplateModelRoot root = Session.getModelRoot();
        try {
            User u = _getUser(request);
            root.put(FMACLManager.USER, u);
            root.put("new_user", new CreateUser(request, response));
        } catch (Exception e) {
            root.put("new_user", new CreateUser(request, response));
        }
        template.process(root, response.getWriter());
        throw new UserSignupException("Signup");
    }

    public static User logoutUser(HttpServletRequest request, HttpServletResponse response) throws Exception {
        User u = getUser(request, response);
        Cookie c = new Cookie("MACTOKEN", "");
        c.setPath(u.getURI());
        response.addCookie(c);
        return u;
    }

    public static User process(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String action = request.getParameter("action");
        Session.getLog().debug("action = " + action);
        if (action != null) {
            if (action.equals("change_mbox_password")) {
                changeMboxPassword(request, response);
                return null;
            }
            Session.getResellerId();
            if (action.equals("login")) {
                loginUser(request, response);
                return null;
            } else if (action.equals("sendPassw")) {
                forgetPass(request, response);
                return null;
            } else if (action.equals("changePassword")) {
                User u = getUser(request, response);
                if (u.isDemo()) {
                    return u;
                }
                u.setPassword(request, response);
                return u;
            } else if (action.equals("signup")) {
                signupUser(request, response);
                return null;
            } else if (action.equals("logout")) {
                return logoutUser(request, response);
            } else {
                if (action.equals("plan_list")) {
                    return getPlanList(request, response);
                }
                if (action.equals("forgetPassw")) {
                    return getForgetPswd(request, response);
                }
                if (action.equals("externTT")) {
                    Session.getLog().debug("User:Action is externTT");
                    return createExternTT(request, response);
                } else if (action.equals("createExTT")) {
                    Session.getLog().debug("User:Action is createExTT");
                    saveExternTT(request, response);
                    return null;
                } else if (action.equals("plan_compare")) {
                    comparePlans(request, response);
                    return null;
                } else if ("dst_compare".equals(action)) {
                    compareDedicatedServersTemplates(request, response);
                    return null;
                }
            }
        }
        Session.getResellerId();
        return getUser(request, response);
    }

    public static void changeMboxPassword(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        String mbox = request.getParameter("mbox");
        String oldPass = request.getParameter("old_password");
        String newPass = request.getParameter("password");
        String newPass2 = request.getParameter("password2");
        TemplateModelRoot root = Session.getModelRoot();
        long oldResellerId = 0;
        try {
            oldResellerId = Session.getResellerId();
        } catch (UnknownResellerException e) {
        }
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            if (mbox == null || oldPass == null || newPass == null) {
                try {
                    Session.setUser(oldUser);
                    Session.setAccount(oldAccount);
                    Session.getResellerId();
                } catch (UnknownResellerException e2) {
                    if (oldResellerId > 0) {
                        try {
                            Session.setResellerId(oldResellerId);
                        } catch (Exception e3) {
                        }
                    }
                }
            } else {
                try {
                    try {
                        if (!newPass.equals(newPass2)) {
                            throw new HSUserException("user.badpsw");
                        }
                        int ind = mbox.indexOf(64);
                        if (ind == -1) {
                            throw new HSUserException("Bad email");
                        }
                        String domainName = mbox.substring(ind + 1);
                        String mboxName = mbox.substring(0, ind);
                        Hashtable hs = Domain.getDomainInfoByName(domainName);
                        if (hs == null) {
                            throw new HSUserException("mailpass.notfound");
                        }
                        long rId = ((Long) hs.get("resource_id")).longValue();
                        ResourceId domainId = new ResourceId(rId, ResourceId.getTypeById(rId));
                        ResourceId accountId = new ResourceId(((Long) hs.get("account_id")).longValue(), 0);
                        Session.setResellerId(1L);
                        Account a = (Account) Account.get(accountId);
                        Session.setAccount(a);
                        Session.setUser(a.getUser());
                        Session.getLog().debug("Domain id = " + domainId);
                        ResourceId mailServiceId = domainId.FM_getChild("mail_service");
                        if (mailServiceId == null) {
                            throw new HSUserException("mailpass.notfound");
                        }
                        Session.getLog().debug("Mail Service id = " + mailServiceId);
                        ResourceId mailDomainId = mailServiceId.FM_getChild("mail_domain");
                        if (mailDomainId == null) {
                            throw new HSUserException("mailpass.notfound");
                        }
                        Session.getLog().debug("MailDomain id = " + mailDomainId);
                        MailDomain mailDomain = (MailDomain) mailDomainId.get();
                        ResourceId mboxId = mailDomain.getBoxByName(mboxName);
                        Session.getLog().debug("MailBox id = " + mboxId + " name was " + mboxName);
                        if (mboxId == null) {
                            throw new HSUserException("mailpass.notfound");
                        }
                        Mailbox mailBox = (Mailbox) mboxId.get();
                        if (!oldPass.equals(mailBox.getPassword())) {
                            throw new HSUserException("mailpass.badpsw");
                        }
                        Session.getLog().debug("Changing pass for Box id = " + mboxId);
                        mailBox.changePassword(newPass);
                        throw new HSUserException("mailpass.changed");
                    } catch (HSUserException e4) {
                        Session.addMessage(e4.getMessage());
                        try {
                            Session.setUser(oldUser);
                            Session.setAccount(oldAccount);
                            Session.getResellerId();
                        } catch (UnknownResellerException e5) {
                            if (oldResellerId > 0) {
                                try {
                                    Session.setResellerId(oldResellerId);
                                } catch (Exception e6) {
                                }
                            }
                        }
                    }
                } catch (Exception e7) {
                    Session.getLog().warn("Error changing mail", e7);
                    try {
                        Session.setUser(oldUser);
                        Session.setAccount(oldAccount);
                        Session.getResellerId();
                    } catch (UnknownResellerException e8) {
                        if (oldResellerId > 0) {
                            try {
                                Session.setResellerId(oldResellerId);
                            } catch (Exception e9) {
                            }
                        }
                    }
                }
            }
            String template_name = request.getParameter("template_name");
            if (template_name == null) {
                template_name = "design/mail_passw.html";
            }
            Session.getLog().debug("Template name " + template_name);
            Template template = Session.getTemplate(template_name);
            template.process(root, response.getWriter());
            throw new UserLoginException("must login");
        } catch (Throwable th) {
            try {
                Session.setUser(oldUser);
                Session.setAccount(oldAccount);
                Session.getResellerId();
            } catch (UnknownResellerException e10) {
                if (oldResellerId > 0) {
                    try {
                        Session.setResellerId(oldResellerId);
                    } catch (Exception e11) {
                    }
                }
            }
            throw th;
        }
    }

    protected static User getPlanList(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        String template_name = request.getParameter("template_name");
        if (template_name == null) {
            template_name = "design/choose_plan.html";
        }
        Session.getLog().debug("Template name " + template_name);
        Template template = Session.getTemplate(template_name);
        TemplateModelRoot root = Session.getModelRoot();
        try {
            TemplateList lp = Plan.getGroupedPlanTree(Plan.getAccessiblePlanList(0));
            root.put("planlist", lp);
        } catch (Exception e) {
        }
        template.process(root, response.getWriter());
        throw new UserSignupException("Signup");
    }

    protected static User getForgetPswd(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        String template_name = request.getParameter("template_name");
        if (template_name == null) {
            template_name = "design/forget_passw.html";
        }
        Session.getLog().debug("Template name " + template_name);
        Template template = Session.getTemplate(template_name);
        TemplateModelRoot root = Session.getModelRoot();
        template.process(root, response.getWriter());
        throw new UserLoginException("must login");
    }

    protected static User createExternTT(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        String template_name = request.getParameter("template_name");
        if (template_name == null) {
            template_name = "tt/new_ext_tt.html";
        }
        Template template = Session.getTemplate(template_name);
        TemplateModelRoot root = Session.getModelRoot();
        template.process(root, response.getWriter());
        throw new UserLoginException("must login");
    }

    private static void saveExternTT(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        Session.getLog().debug("User:saving ext TT title=" + request.getParameter("title") + " priority=" + request.getParameter("priority") + " description=" + request.getParameter("description") + " qid=" + request.getParameter("qid"));
        try {
            Ticket.create(request.getParameter("title"), Integer.parseInt(request.getParameter("priority")), request.getParameter("description"), request.getParameter("email"), 1, 0, 1, 1, 0, 0);
        } catch (Exception e) {
            Session.getLog().error("User:Attempt to saving ext TTfailed. Title=" + request.getParameter("title") + " priority=" + request.getParameter("priority") + " description=" + request.getParameter("description") + " because " + e.toString(), e);
        }
        TemplateModelRoot root = Session.getModelRoot();
        root.put("config", new ConfigModel("psoft.user"));
        String requestURL = getRequestURL(request);
        root.put("requestURL", new TemplateString(requestURL));
        root.put("requestURL_enc", new TemplateString(URLEncoder.encode(requestURL)));
        root.put("servletURL", new TemplateString(request.getRequestURI()));
        Session.getTemplate(Config.getProperty("psoft.user", "logintemplate")).process(root, response.getWriter());
        throw new UserLoginException("must login");
    }

    public long getId() {
        return this.f58id;
    }

    public List getBillingInfos() {
        List listBI = new ArrayList();
        try {
            Connection con = Session.getDb();
            Session.getLog().debug("Getting billing info from DB ");
            PreparedStatement ps = con.prepareStatement("SELECT billing_info_id FROM user_billing_infos WHERE user_id = ?");
            ps.setLong(1, this.f58id);
            ResultSet rs = ps.executeQuery();
            Session.getLog().debug("User ID:" + this.f58id);
            while (rs.next()) {
                listBI.add(new BillingInfoObject(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Exception e) {
            Session.getLog().error("Can`t get Billing info from DB", e);
        }
        return listBI;
    }

    public TemplateModel FM_getListBill(long id) throws Exception {
        TemplateList ls = new TemplateList();
        Account oldAccount = Session.getAccount();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, account_id, opened FROM bill WHERE account_id in (SELECT id FROM accounts WHERE bi_id = ?) ORDER BY opened DESC");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Account a = Session.getUser().getAccount(new ResourceId(rs.getLong(2), 0));
                    Session.setAccount(a);
                    TemplateModel templateHash = new TemplateHash();
                    templateHash.put("account_id", Long.toString(a.getId().getId()));
                    templateHash.put("account", a);
                    templateHash.put("bill", new Bill(rs.getLong(1)));
                    ls.add(templateHash);
                } catch (NullPointerException e) {
                }
            }
            Session.closeStatement(ps);
            Session.setAccount(oldAccount);
            con.close();
            return ls;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.setAccount(oldAccount);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getUserBills(String type) throws Exception {
        TemplateList ls = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                int typeOfBills = Integer.parseInt(type);
                String query = "";
                switch (typeOfBills) {
                    case 1:
                        query = "SELECT id,account_id,description,amount FROM bill WHERE account_id IN (SELECT account_id FROM user_account WHERE user_id=?)";
                        break;
                    case 2:
                        query = "SELECT id,account_id,description,amount FROM bill WHERE closed IS NOT NULL AND account_id IN (SELECT account_id FROM user_account WHERE user_id=?)";
                        break;
                    case 3:
                        query = "SELECT id,account_id,description, amount FROM bill WHERE closed IS NULL AND account_id IN (SELECT account_id FROM user_account WHERE user_id=?)";
                        break;
                }
                ps = con.prepareStatement(query);
                ps.setLong(1, this.f58id);
                ResultSet rs = ps.executeQuery();
                Account oldAccount = Session.getAccount();
                while (rs.next()) {
                    TemplateHash th = new TemplateHash();
                    th.put("bill_id", rs.getString("id"));
                    th.put("account_id", rs.getString("account_id"));
                    th.put("description", rs.getString("description"));
                    Account a = new Account(new ResourceId(rs.getLong("account_id"), 0));
                    Session.setAccount(a);
                    th.put("bill", new Bill(rs.getLong("id")));
                    ls.add((TemplateModel) th);
                }
                Session.setAccount(oldAccount);
                Session.closeStatement(ps);
                con.close();
                return ls;
            } catch (Exception e) {
                throw new Exception("User:Unable to get user bills," + e.toString());
            }
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    public double getReferalFee() throws Exception {
        double result = 0.0d;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT SUM(sum) FROM referal_sum WHERE referal_id = ?");
                ps.setLong(1, this.f58id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    double result2 = rs.getDouble(1);
                    result = Math.rint(result2 * 100.0d) / 100.0d;
                }
                double d = result;
                Session.closeStatement(ps);
                con.close();
                return d;
            } catch (SQLException e) {
                Session.getLog().debug("User: Unable do get referral sum because:" + e.toString() + ". I will return 0");
                Session.closeStatement(ps);
                con.close();
                return 0.0d;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getReferalFee() throws Exception {
        return new TemplateString(USFormat.format(getReferalFee()));
    }

    public TemplateModel FM_getBillingInfo(long id) throws Exception {
        Session.getLog().debug("Got Billing Info:" + id);
        return new BillingInfoObject(id);
    }

    public TemplateModel FM_getLastContactInfo() throws Exception {
        Session.getLog().debug("Got Last Contact Info:" + this.f58id);
        ContactInfoObject ci = null;
        try {
            Date last = null;
            Iterator i = getAccountIds().iterator();
            while (i.hasNext()) {
                Account a = getAccount((ResourceId) i.next());
                if (last == null) {
                    last = a.getCreated();
                    ci = a.getContactInfo();
                } else if (a.getCreated().after(last) && a.getContactInfo().getId() > 0) {
                    last = a.getCreated();
                    ci = a.getContactInfo();
                }
            }
        } catch (Exception ex) {
            Session.getLog().error("Last Contact Info Error", ex);
        }
        return ci;
    }

    public TemplateModel FM_getBill(long id, long account_id) throws Exception {
        Account oldAccount = Session.getAccount();
        try {
            Account a = Session.getUser().getAccount(new ResourceId(account_id, 0));
            Session.getLog().debug("Getting Bill ID:" + id);
            Session.setAccount(a);
            Bill bi = new Bill(id);
            Session.setAccount(oldAccount);
            return bi;
        } catch (Throwable th) {
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public TemplateModel FM_getAccountPerBill(long bill_id) throws Exception {
        Connection con = Session.getDb();
        try {
            Session.getLog().debug("User: bill_id=" + Long.toString(bill_id));
            PreparedStatement ps = con.prepareStatement("SELECT account_id FROM bill WHERE id=?");
            ps.setLong(1, bill_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                TemplateString templateString = new TemplateString(rs.getLong("account_id"));
                Session.closeStatement(ps);
                con.close();
                return templateString;
            }
            TemplateString templateString2 = new TemplateString("");
            Session.closeStatement(ps);
            con.close();
            return templateString2;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("status".equals(key)) {
            return Resource.STATUS_OK;
        }
        if ("login".equals(key)) {
            return new TemplateString(getLogin());
        }
        if ("referal_id".equals(key)) {
            return new TemplateString(this.referal_id);
        }
        if ("request_id".equals(key)) {
            return new TemplateString(this.requestId);
        }
        if ("reseller_id".equals(key)) {
            return new TemplateString(getResellerId());
        }
        if ("password".equals(key)) {
            return new TemplateString(getPassword());
        }
        if ("billing_infos".equals(key)) {
            return new TemplateList(getBillingInfos());
        }
        try {
            if ("CP_HOST".equals(key)) {
                return new TemplateString(Reseller.getReseller(getResellerId()).getURL());
            }
        } catch (Exception e) {
            Session.getLog().error("User.get: ", e);
        }
        if ("CP_URL".equals(key)) {
            return new TemplateString(getCpURL());
        }
        if ("reseller_url".equals(key)) {
            return new TemplateString(getResellerURL());
        }
        if ("reseller_context_url".equals(key)) {
            return new TemplateString(getCpContextURL());
        }
        if ("active_account".equals(key)) {
            return this.activeAccount;
        }
        if ("payment_request".equals(key)) {
            return new TemplateString(this.paymentRequest);
        }
        if ("prefix".equals(key)) {
            try {
                return new TemplateString(getUserPrefix());
            } catch (Exception ex) {
                Session.getLog().error("Error getting user prefix ", ex);
                throw new TemplateModelException("Error getting user prefix for user " + getLogin());
            }
        } else if ("isdemo".equals(key)) {
            try {
                if (isDemo()) {
                    return new TemplateString("true");
                }
                return new TemplateString("false");
            } catch (Exception ex2) {
                Session.getLog().error("Error demo mod ", ex2);
                throw new TemplateModelException("Error getting demo mod for user " + getLogin());
            }
        } else {
            try {
                return TemplateMethodWrapper.getMethod(this, key);
            } catch (IllegalArgumentException e2) {
                Session.getLog().debug("", e2);
                return null;
            }
        }
    }

    public String getResellerURL() {
        try {
            Reseller resel = Reseller.getReseller(getResellerId());
            String protocol = resel.getProtocol();
            String port = resel.getPort().trim();
            return protocol + resel.getURL() + ((port == null || "".equals(port)) ? "" : ":" + port);
        } catch (Exception e) {
            Session.getLog().error("Cant get reseller URL", e);
            return null;
        }
    }

    public String getCpURL() {
        try {
            String uri = Session.getPropertyString("CP_URI");
            return getResellerURL() + (uri == null ? "" : uri);
        } catch (Exception e) {
            Session.getLog().error("Cant get reseller URL", e);
            return null;
        }
    }

    public String getCpContextURL() {
        try {
            String uri = Session.getPropertyString("CP_URI");
            String uri2 = uri == null ? "" : uri;
            int pos = uri2.lastIndexOf(47);
            if (pos > 0) {
                uri2 = uri2.substring(0, pos + 1);
            }
            return getResellerURL() + uri2;
        } catch (Exception e) {
            Session.getLog().error("Cant get reseller URL with Context", e);
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean isAccessible(Plan p) {
        try {
            if (!p.isAccessible(0) || p.isDisabled()) {
                HashSet h = getAccountIds();
                Iterator i = h.iterator();
                while (i.hasNext()) {
                    ResourceId tmp = (ResourceId) i.next();
                    Account tmpAcc = (Account) Account.get(tmp);
                    if (p.isAccessible(tmpAcc.getPlan().getId()) && !p.isDisabled()) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        } catch (Exception e) {
            Session.getLog().error("ERROR", e);
            return false;
        }
    }

    public List getAccessiblePlanList() {
        List availablePlans = new ArrayList();
        for (Plan pl : Plan.getPlanList()) {
            if (isAccessible(pl)) {
                availablePlans.add(pl);
            }
        }
        return availablePlans;
    }

    public TemplateModel FM_getAccessiblePlanList() {
        return Plan.getGroupedPlanTree(getAccessiblePlanList());
    }

    protected void processNotifications(Account a) {
        try {
            Collection<ResourceId> col = a.getId().findAllChildren("domain");
            for (ResourceId resourceId : col) {
                Resource res = resourceId.get();
                if (res instanceof Domain) {
                    ((Domain) res).notifyEmail();
                }
            }
        } catch (Throwable t) {
            Session.getLog().error("Error processing notifications", t);
            Ticket.create(t, a);
        }
    }

    public Account addAccount(int planId, BillingInfoObject bi, ContactInfoObject ci, String description, String mod, int period_id) throws Exception {
        int sinupId = SignupManager.getRequestSignupId(Session.getRequest());
        SignupManager.checkConcurrentSignup(Session.getRequest());
        Plan p = Plan.getPlan(planId);
        try {
            Account a = new Account(p, description, bi, ci, mod, period_id);
            processNotifications(a);
            this.activeAccount = a;
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO user_account (user_id, account_id, type_id) VALUES (? ,?, ?)");
            ps.setLong(1, getId());
            ps.setLong(2, a.getId().getId());
            ps.setInt(3, a.getId().getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            getAccountIds().add(a.getId());
            SignupManager.done(sinupId, getId(), a.getId().getId());
            try {
                String email = a.getContactInfo().getEmail();
                SimpleHash root = CustomEmailMessage.getDefaultRoot(a);
                root.put("ci", ci);
                root.put("bi", bi);
                CustomEmailMessage.send("NEW_ACCOUNT", email, (TemplateModelRoot) root);
            } catch (NullPointerException npe) {
                Session.getLog().warn("NPE", npe);
            } catch (Throwable t) {
                Session.getLog().warn("Error", t);
            }
            return a;
        } catch (Exception ex) {
            SignupManager.error(sinupId, ex.getMessage());
            throw ex;
        }
    }

    public Account addAccount(int planId, String description, String mod, boolean isExternal) throws Exception {
        return addAccount(planId, description, mod, false, isExternal);
    }

    public Account addAccount(int planId, String description, String mod, boolean force, boolean isExternal) throws Exception {
        BillingInfoObject bi;
        long bi_id;
        BillingInfoObject newBi;
        ContactInfoObject ci;
        Plan p = Plan.getPlan(planId);
        int bp = 0;
        if (p.getBilling() == 0) {
            bi = new BillingInfoObject(0L);
            Session.getLog().info("Creating free account");
        } else if (p.getBilling() == 2 && "TRIAL".equals(Session.getRequest().getParameter("_bi_type"))) {
            Session.getLog().info("Creating trial account");
            try {
                bp = Integer.parseInt(new ServletRequestAccessor(Session.getRequest()).get("_bp"));
            } catch (NumberFormatException e) {
                bp = 0;
            }
            bi = new BillingInfoObject(-1L);
        } else {
            Session.getLog().info("Creating normal account");
            Accessor a = new ServletRequestAccessor(Session.getRequest());
            try {
                bp = Integer.parseInt(a.get("_bp"));
            } catch (NumberFormatException e2) {
                bp = 0;
            }
            try {
                if (a.get("use_bi_id") == null || "".equals(a.get("use_bi_id"))) {
                    bi_id = Long.parseLong(a.get("_bi_type"));
                    Session.getLog().info("Biling info type" + bi_id + " Billing period:" + bp);
                } else {
                    bi_id = Long.parseLong(a.get("use_bi_id"));
                }
                newBi = new BillingInfoObject(bi_id);
            } catch (NumberFormatException e3) {
                bi = null;
            }
            if (getBillingInfos().contains(newBi)) {
                bi = newBi;
                if (bi == null) {
                    bi = new BillingInfoObject(new NameModifier("_bi_"));
                }
            } else {
                throw new HSUserException("user.billing");
            }
        }
        if (p.getCInfo() == 0) {
            ci = new ContactInfoObject(0L);
            Session.getLog().info("No contact Info");
        } else {
            Session.getLog().info("Creating Contact info");
            ci = new ContactInfoObject(new NameModifier("_ci_"));
        }
        boolean guardAllow = true;
        if (!force) {
            List guardResult = SignupGuard.checkSignup(getId(), planId, mod, bi, p, isExternal);
            guardAllow = guardResult.size() == 0;
        }
        if ("Check".equalsIgnoreCase(bi.getType())) {
            guardAllow = false;
        }
        if (bi.getBillingType() == 3) {
            guardAllow = false;
        }
        if (guardAllow || force) {
            Session.getLog().info("Adding account billing period:" + bp);
            try {
                return addAccount(planId, bi, ci, description, mod, bp);
            } catch (Exception ex) {
                if (!(ex instanceof HSUserException)) {
                    Session.getLog().info("dumping request:" + bi.getBillingType());
                    dumpRequest(bi, ci, planId);
                    throw new AccountCreationException(ex);
                }
                throw ex;
            }
        }
        Session.getLog().info("dumping request:" + bi.getBillingType());
        dumpRequest(bi, ci, planId);
        return null;
    }

    protected void dumpRequest(BillingInfoObject bi, ContactInfoObject ci, int planId) throws Exception {
        String email;
        Session.getLog().debug("Begin User.dumpRequest");
        if (Session.getRequest().getParameter("signup_id") == null || Session.getRequest().getParameter("signup_id").length() == 0) {
            throw new Exception("No signup_id available in request");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        this.requestId = Session.getNewId("request_seq");
        try {
            ps1 = con.prepareStatement("DELETE FROM f_user_account WHERE signup_id = ?");
            ps1.setLong(1, Integer.parseInt(Session.getRequest().getParameter("signup_id")));
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO request_record (request_id, user_id, bid, cid, plan_id, created) VALUES (?, ?, ?, ?, ?, ?)");
            ps2.setInt(1, this.requestId);
            ps2.setLong(2, getId());
            ps2.setLong(3, bi.getId());
            ps2.setLong(4, ci.getId());
            ps2.setInt(5, planId);
            ps2.setTimestamp(6, TimeUtils.getSQLTimestamp());
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            ps = con.prepareStatement("INSERT INTO request (id, name, value) VALUES (?, ?, ?)");
            ps.setInt(1, this.requestId);
            Enumeration e = Session.getRequest().getParameterNames();
            while (e.hasMoreElements()) {
                String name = (String) e.nextElement();
                ps.setString(2, name);
                String[] values = Session.getRequest().getParameterValues(name);
                for (int i = 0; i < values.length; i++) {
                    if ("_bi_cc_cvv".equals(name)) {
                        ps.setString(3, GenericCreditCard.getHiddenCVV(values[i]));
                    } else if ("_bi_cc_number".equals(name)) {
                        ps.setString(3, GenericCreditCard.getHiddenNumber(values[i]));
                    } else {
                        ps.setString(3, values[i]);
                    }
                    ps.executeUpdate();
                }
            }
            ps.setString(2, "REMOTE_ADDR");
            ps.setString(3, Session.getRequest().getRemoteAddr());
            ps.executeUpdate();
            String name2 = Session.getRequest().getHeader("HTTP_VIA");
            if (name2 != null) {
                ps.setString(2, "HTTP_VIA");
                ps.setString(3, name2);
                ps.executeUpdate();
            }
            String name3 = Session.getRequest().getHeader("HTTP_X_FORWARDED_FOR");
            if (name3 != null) {
                ps.setString(2, "HTTP_X_FORWARDED_FOR");
                ps.setString(3, name3);
                ps.executeUpdate();
            }
            Session.getLog().debug("Deleting F_USER_ACCOUNT entry");
            Session.getLog().debug(ps1.toString());
            ps1.executeUpdate();
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            try {
                String exemptionCode = Session.getRequest().getParameter("_bi_exemption_code");
                if ("".equals(exemptionCode)) {
                    exemptionCode = null;
                }
                if (bi != null) {
                    email = bi.getEmail();
                    if (email == null) {
                        email = ci.getEmail();
                    }
                    if (exemptionCode == null) {
                        exemptionCode = bi.getExemptionCode();
                        if ("".equals(exemptionCode)) {
                            exemptionCode = null;
                        }
                    }
                } else {
                    email = ci.getEmail();
                }
                Session.getLog().debug("mail to: " + email);
                SimpleHash root = new SimpleHash();
                TemplateServletUtils.copyRequest(Session.getRequest(), root);
                root.put("bi", bi);
                root.put("ci", ci);
                Plan p = Plan.getPlan(planId);
                root.put("plan", p);
                root.put("toolbox", HsphereToolbox.toolbox);
                root.put("settings", new MapAdapter(Settings.get().getMap()));
                root.put("request_id", new TemplateString(this.requestId));
                root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
                root.put(FMACLManager.USER, this);
                root.put("reseller_url", new TemplateString(getResellerURL()));
                boolean isSend = false;
                if (bi != null) {
                    if (bi.getBillingType() == 1 && exemptionCode == null) {
                        CustomEmailMessage.send("NEW_MODERATED_CC", email, (TemplateModelRoot) root);
                        isSend = true;
                    } else if (bi.getBillingType() == -1) {
                        CustomEmailMessage.send("TRIAL_MODERATED", email, (TemplateModelRoot) root);
                        isSend = true;
                    }
                }
                if (!isSend) {
                    if (exemptionCode == null) {
                        CustomEmailMessage.send("NEW_MODERATED", email, (TemplateModelRoot) root);
                    } else {
                        CustomEmailMessage.send("NEW_MODERATED_TEXEMPT", email, (TemplateModelRoot) root);
                    }
                }
                if (bi != null && bi.getBillingType() == 3) {
                    this.paymentRequest = ExternalPaymentManager.requestSignupPayment(Invoice.getInvoice().getTotal(), this.requestId);
                }
            } catch (Throwable e2) {
                Session.getLog().warn("Error Sending Email: ", e2);
            }
            try {
                PaymentInstrument pi = bi.getPaymentInstrument();
                if (pi instanceof GenericCreditCard) {
                    Bill.checkCC(bi);
                }
            } catch (Throwable ex) {
                Session.getLog().error("Critical error validating CC:" + ex.getMessage());
            }
            Session.getLog().debug("End User.dumpRequest");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addAccount(int planId, String descr, String mod) throws Exception {
        try {
            return addAccount(planId, descr, mod, false);
        } catch (Exception e) {
            Session.getLog().debug("Adding account: ", e);
            throw new TemplateModelException(e.getMessage());
        }
    }

    public void setPassword(HttpServletRequest request, HttpServletResponse response) throws TemplateModelException, SQLException {
        String p1 = request.getParameter("p1");
        String p2 = request.getParameter("p2");
        if (!p1.equals(p2)) {
            Session.addMessage(Localizer.translateMessage("user.badpsw"));
            return;
        }
        try {
            setPassword(p1);
            setToken(this, request, response);
            Session.addMessage(Localizer.translateMessage("user.passyes"));
        } catch (Exception e) {
            Session.getLog().debug("Changing password: ", e);
            throw new TemplateModelException(e.getMessage());
        }
    }

    public void setPassword(String p1) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE users SET password = ? WHERE id = ?");
            ps.setString(1, p1);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.password = p1;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addAccount(int planId, String descr) throws Exception {
        return FM_addAccount(planId, descr, "");
    }

    public TemplateModel FM_setAccount(String accountId) throws Exception {
        Session.getLog().debug("NEW ACCOUNT ID:" + accountId);
        Session.setAccount((Account) Account.get(new ResourceId(accountId)));
        return Session.getAccount();
    }

    public TemplateModel FM_getAccountIds() throws TemplateModelException {
        try {
            return new ListAdapter(getAccountIds());
        } catch (SQLException e) {
            Session.getLog().debug("", e);
            throw new TemplateModelException(e.getMessage());
        }
    }

    public HashSet getAccountIds() throws SQLException {
        return getAccountIds(false);
    }

    public static String getUsername(long accountId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT username FROM users, user_account WHERE users.id = user_id AND user_account.account_id = ?");
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            Session.closeStatement(ps);
            con.close();
            throw new Exception("Not found corresponding user name");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized HashSet getAccountIds(boolean force) throws SQLException {
        Session.getLog().debug("--User.getAccountIds");
        if (force || null == this.accountIds) {
            this.accountIds = new HashSet();
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT account_id, type_id FROM user_account WHERE user_id = ? ORDER BY account_id");
                ps.setLong(1, getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ResourceId newId = new ResourceId(rs.getLong(1), rs.getInt(2));
                    this.accountIds.add(newId);
                }
                Session.closeStatement(ps);
                con.close();
                Session.getLog().debug("Read ids");
                return this.accountIds;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return this.accountIds;
    }

    public TemplateModel FM_listMasterPlans() {
        return new ListAdapter(Plan.getPlanList());
    }

    public TemplateModel FM_getMasterPlan(int id) throws Exception {
        return Plan.getPlan(id);
    }

    public TemplateModel FM_getAccount(ResourceId accountId) throws Exception {
        return getAccount(accountId);
    }

    public Account getAccount(ResourceId accountId) throws Exception {
        if (getAccountIds().contains(accountId)) {
            return (Account) Account.get(accountId);
        }
        return null;
    }

    public long getReferalId() {
        return this.referal_id;
    }

    public int getReferalGroup() {
        return this.referal_group;
    }

    public int getReferalGroupOfReferal() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT referal_group FROM referrals WHERE user_id = ?");
                ps.setLong(1, this.referal_id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    int i = rs.getInt("referal_group");
                    Session.closeStatement(ps);
                    con.close();
                    return i;
                }
                Session.closeStatement(ps);
                con.close();
                return 0;
            } catch (SQLException e) {
                throw new Exception("Cant find referral group," + e.toString());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM referrals WHERE user_id = ?");
            ps2.setLong(1, getId());
            ps2.executeUpdate();
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("DELETE FROM user_billing_infos WHERE user_id = ?");
            ps3.setLong(1, getId());
            ps3.executeUpdate();
            ps3.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT request_id FROM request_record WHERE user_id = ?");
            ps1 = con.prepareStatement("DELETE FROM request WHERE id = ?");
            ps4.setLong(1, getId());
            ResultSet rs = ps4.executeQuery();
            while (rs.next()) {
                ps1.setInt(1, rs.getInt(1));
                ps1.executeUpdate();
            }
            rs.close();
            ps4.close();
            PreparedStatement ps5 = con.prepareStatement("DELETE FROM request_record WHERE user_id = ?");
            ps5.setLong(1, getId());
            ps5.executeUpdate();
            ps5.close();
            this.prefixes.delPrefix(getId());
            ps = con.prepareStatement("DELETE FROM users WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            cache.remove(getLogin());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    protected static User comparePlans(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        String template_name = request.getParameter("template_name");
        if (template_name == null) {
            template_name = "submit/misc/compare_plans.sbm";
        }
        Template template = Session.getTemplate(template_name);
        TemplateModelRoot root = Session.getModelRoot();
        try {
            TemplateList lp = Plan.getGroupedPlanTree(Plan.getAccessiblePlanList(0));
            root.put("planlist", lp);
        } catch (Exception e) {
        }
        template.process(root, response.getWriter());
        throw new UserLoginException("must login");
    }

    protected static User compareDedicatedServersTemplates(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        Template template = Session.getTemplate("replacements/ds/signup/compare_dsts.html");
        TemplateModelRoot root = Session.getModelRoot();
        try {
            int planId = Integer.parseInt(request.getParameter("plan_id"));
            Plan p = Plan.getPlan(planId);
            ArrayList res = new ArrayList();
            List<DedicatedServerTemplate> availableDSTemplates = HsphereToolbox.availableDSTemplates(planId);
            Session.getLog().info("Got list of available DST size=" + availableDSTemplates.size());
            for (DedicatedServerTemplate dst : availableDSTemplates) {
                Session.getLog().info("DST with id" + dst.getId() + " has been added");
                Hashtable dstInfo = new Hashtable();
                dstInfo.put("dst", dst);
                String s = "";
                String r = "";
                try {
                    s = p.getResourceValue(7100, "_DST_" + dst.getId() + "_SETUP_PRICE_");
                } catch (Exception e) {
                }
                try {
                    r = p.getResourceValue(7100, "_DST_" + dst.getId() + "_REC_PRICE_");
                } catch (Exception e2) {
                }
                dstInfo.put("setup", s);
                dstInfo.put("recurrent", r);
                res.add(new TemplateHash(dstInfo));
            }
            root.put("dst_info", new TemplateList(res));
        } catch (Exception e3) {
            Session.getLog().error("An error occured in compareDedicatedServersTemplates ", e3);
        }
        template.process(root, response.getWriter());
        throw new UserLoginException("must login");
    }

    public String getUserPrefix() throws Exception {
        return this.prefixes.getUserPrefix(this.f58id);
    }

    public boolean isDemo() throws Exception {
        boolean demo = true;
        if (!Session.getPropertyString("EMULATION_MODE").toUpperCase().equals("TRUE")) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT a.id FROM accounts a, user_account u WHERE a.id = u.account_id AND u.user_id = ? AND demo = 0");
                ps.setLong(1, getId());
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    demo = false;
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return demo;
    }

    public void setActiveAccount(Account a) throws Exception {
        this.activeAccount = a;
    }

    public static void setTTvalueBySignupId(long signupId, long ttId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM request WHERE name=? AND value=? ORDER BY id DESC");
            ps.setString(1, "signup_id");
            ps.setLong(2, signupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                setTTvalueByRecordId(rs.getLong(1), ttId);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void setTTvalueByRecordId(long recordId, long ttId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps1 = null;
        try {
            ps1 = con.prepareStatement("INSERT INTO request (id, name, value) VALUES (?, ?, ?)");
            ps1.setLong(1, recordId);
            ps1.setString(2, "_trouble_ticket");
            ps1.setLong(3, ttId);
            ps1.executeUpdate();
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }
}
