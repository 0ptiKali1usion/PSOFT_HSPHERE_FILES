package psoft.user;

import freemarker.template.SimpleHash;
import freemarker.template.SimpleScalar;
import freemarker.template.Template;
import freemarker.template.TemplateCache;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import freemarker.template.TemplateServletUtils;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.NoSuchElementException;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.Vector;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.AccountPreferences;
import psoft.persistance.PersistanceError;
import psoft.persistance.UniversalPersistanceManager;
import psoft.util.Config;
import psoft.util.freemarker.AutoRefreshFileTemplateCache;
import psoft.util.freemarker.ConfigModel;
import psoft.util.freemarker.LingualScalar;
import psoft.util.freemarker.Toolbox;
import psoft.validators.Accessor;
import psoft.validators.ComplexValidatorException;
import psoft.validators.CookieAccessor;
import psoft.validators.NameModifier;
import psoft.validators.ServletRequestAccessor;
import psoft.validators.ValidationException;
import psoft.web.utils.Redirect;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/user/WebUserProcessor.class */
public class WebUserProcessor {
    static String TRIGGER = "useraction";
    protected UniversalPersistanceManager upm;
    protected UserAdapter adapter;
    protected TemplateCache tCache;
    protected Hashtable signupPages;
    protected Hashtable editPages;
    protected String firstSignupPage;
    protected NameModifier cookieNM;
    protected UserEmailer mailer;
    protected boolean redirectType;

    protected static Category getLog() {
        return Config.getLog("psoft.user");
    }

    public void setEncoding(String encoding) {
        if (this.tCache instanceof AutoRefreshFileTemplateCache) {
            ((AutoRefreshFileTemplateCache) this.tCache).setEncoding(encoding);
        } else {
            System.err.println("can not set Encoding " + encoding);
        }
    }

    public WebUserProcessor(UserAdapter adapter, TemplateCache tCache, ResourceBundle rb, NameModifier cookieNM) {
        try {
            this.redirectType = rb.getString("REDIRECT_TYPE").toUpperCase().equals("LOCATION");
        } catch (Exception e) {
            this.redirectType = true;
        }
        this.adapter = adapter;
        this.tCache = tCache;
        this.cookieNM = cookieNM;
        this.upm = Config.getUPM("psoft.user");
        String address = null;
        try {
            address = Config.getProperty("psoft.user", "return_address");
        } catch (Throwable th) {
            if (address == null) {
                address = "info@psoft.net";
            }
        }
        this.mailer = new UserEmailer(address, tCache);
        parse(rb);
    }

    public WebUserProcessor(UserAdapter adapter, TemplateCache tCache, ResourceBundle rb) {
        this(adapter, tCache, rb, NameModifier.blank());
    }

    protected void parse(ResourceBundle rb) {
        String s_Pages = rb.getString("SIGNUP_PAGES");
        String e_Pages = rb.getString("EDIT_PAGES");
        try {
            StringTokenizer st = new StringTokenizer(s_Pages, " ");
            this.firstSignupPage = st.nextToken();
        } catch (NoSuchElementException e) {
            this.firstSignupPage = "";
        }
        this.signupPages = loadPCN(s_Pages, rb);
        this.editPages = loadPCN(e_Pages, rb);
    }

    protected Hashtable loadPCN(String userPages, ResourceBundle rb) {
        String nextPage;
        StringTokenizer st = new StringTokenizer(userPages, " ");
        Hashtable hash = new Hashtable();
        if (userPages == null || userPages.equals("")) {
            return null;
        }
        String page = st.nextToken();
        do {
            try {
                nextPage = st.nextToken();
            } catch (NoSuchElementException e) {
                nextPage = "";
            }
            PageConfigNode pcn = new PageConfigNode(page, rb.getString(page + "_TEMPLATE"), nextPage, rb.getString(page + "_FIELDS"));
            hash.put(page, pcn);
            page = nextPage;
        } while (!page.equals(""));
        return hash;
    }

    protected Template getTemplate(String action, String tBase) {
        getLog().info("Template: " + tBase + "/" + Config.getProperty("psoft.user", action));
        return this.tCache.getTemplate(tBase + "/" + Config.getProperty("psoft.user", action));
    }

    public User processMultipart(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        return getUser(request, response, "");
    }

    public User process(HttpServletRequest request, HttpServletResponse response) throws UserException, IOException {
        return process(request, response, "");
    }

    public User process(HttpServletRequest request, HttpServletResponse response, String tBase) throws UserException, IOException {
        String action = request.getParameter("action");
        if (action == null || !action.equals(TRIGGER)) {
            return getUser(request, response, tBase);
        }
        String action2 = request.getParameter("useraction");
        if (action2 == null) {
            return null;
        }
        if (action2.equals("login")) {
            loginUser(request, response, tBase);
            return null;
        } else if (action2.equals("signup")) {
            signupUser(request, response, tBase);
            return null;
        } else if (action2.equals("edit")) {
            editUser(request, response, tBase);
            return null;
        } else if (action2.equals("logout")) {
            logoutUser(request, response, tBase);
            return null;
        } else if (action2.equals("forgot")) {
            remind(request, response, tBase);
            return null;
        } else {
            return null;
        }
    }

    protected void setCookies(HttpServletResponse response, User u) {
        response.addCookie(new Cookie(this.cookieNM.getName("login"), u.getLogin()));
        response.addCookie(new Cookie(this.cookieNM.getName("password"), u.getPassword()));
    }

    protected User getUser(HttpServletRequest request, HttpServletResponse response, String tBase) throws UserException, IOException {
        PageConfigNode pcn = null;
        User u = null;
        try {
            Accessor data = new CookieAccessor(request.getCookies());
            u = this.adapter.login(data, this.cookieNM);
            pcn = checkData(u.getAccessor());
            if (pcn == null) {
                return u;
            }
            throw new UserSignupException("incomplete data");
        } catch (UserLoginException ue) {
            getLog().warn("getUser", ue);
            TemplateModelRoot root = TemplateServletUtils.copyRequest(request);
            root.put("config", new ConfigModel("psoft.user"));
            String requestURL = getRequestURL(request);
            root.put("requestURL", new SimpleScalar(requestURL));
            root.put("requestURL_enc", new SimpleScalar(URLEncoder.encode(requestURL)));
            root.put("servletURL", new SimpleScalar(request.getRequestURI()));
            LingualScalar ls = new LingualScalar();
            root.put("locale", new SimpleScalar(ls.getLocale().toString()));
            root.put("charset", new SimpleScalar(ls.getCharset()));
            root.put(AccountPreferences.LANGUAGE, ls);
            root.put("toolbox", Toolbox.toolbox);
            String tName = request.getParameter("template");
            if (tName == null) {
                getTemplate("logintemplate", tBase).process(root, response.getWriter());
            } else {
                this.tCache.getTemplate(tBase + "/" + tName).process(root, response.getWriter());
            }
            throw new UserException("must login");
        } catch (UserSignupException ue2) {
            getLog().warn("getUser", ue2);
            TemplateModelRoot root2 = TemplateServletUtils.copyRequest(request);
            root2.put("config", new ConfigModel("psoft.user"));
            try {
                fillRequest((SimpleHash) root2.get("request"), pcn.getFields(), u.getAccessor());
            } catch (TemplateModelException e) {
            }
            String requestURL2 = getRequestURL(request);
            root2.put("requestURL", new SimpleScalar(requestURL2));
            root2.put("requestURL_enc", new SimpleScalar(URLEncoder.encode(requestURL2)));
            root2.put("servletURL", new SimpleScalar(request.getRequestURI()));
            root2.put("pagename", new SimpleScalar(pcn.getName()));
            root2.put("existing", new SimpleScalar("true"));
            root2.put("config", new ConfigModel("psoft.user"));
            this.tCache.getTemplate(tBase + "/" + pcn.getTemplate()).process(root2, response.getWriter());
            throw new UserException("issuficient data");
        } catch (Exception e2) {
            e2.printStackTrace();
            getLog().warn("getUser", e2);
            return null;
        }
    }

    protected void loginUser(HttpServletRequest request, HttpServletResponse response, String tBase) throws UserException, IOException {
        try {
            User u = this.adapter.login(new ServletRequestAccessor(request), NameModifier.blank());
            setCookies(response, u);
            Redirect.sendRedirect(response, request.getParameter("requestURL"), this.redirectType);
            throw new UserException("try just one more time");
        } catch (UserLoginException pe) {
            getLog().warn("loginUser", pe);
            TemplateModelRoot root = TemplateServletUtils.copyRequest(request);
            root.put("config", new ConfigModel("psoft.user"));
            root.put("requestURL", new SimpleScalar(request.getParameter("requestURL")));
            root.put("requestURL_enc", new SimpleScalar(URLEncoder.encode(request.getParameter("requestURL"))));
            root.put("servletURL", new SimpleScalar(request.getRequestURI()));
            root.put("error", new SimpleScalar(pe.getMessage()));
            LingualScalar ls = new LingualScalar();
            root.put("locale", new SimpleScalar(ls.getLocale().toString()));
            root.put("charset", new SimpleScalar(ls.getCharset()));
            root.put(AccountPreferences.LANGUAGE, ls);
            root.put("toolbox", Toolbox.toolbox);
            String tName = request.getParameter("template");
            if (tName == null) {
                getTemplate("logintemplate", tBase).process(root, response.getWriter());
            } else {
                this.tCache.getTemplate(tBase + "/" + tName).process(root, response.getWriter());
            }
            throw new UserLoginException("must login");
        }
    }

    protected void signupUser(HttpServletRequest request, HttpServletResponse response, String tBase) throws UserException, IOException {
        TemplateModelRoot root = TemplateServletUtils.copyRequest(request);
        root.put("config", new ConfigModel("psoft.user"));
        PageConfigNode pcn = null;
        boolean oldUser = false;
        try {
            oldUser = request.getParameter("existing").equals("true");
        } catch (Exception e) {
            getLog().warn("signupUser", e);
        }
        String pagename = request.getParameter("pagename");
        if (pagename != null && !pagename.equals("")) {
            try {
                this.adapter.validate(new ServletRequestAccessor(request), NameModifier.blank(), ((PageConfigNode) this.signupPages.get(pagename)).getFields());
                pcn = null;
            } catch (ComplexValidatorException cve) {
                String error = expandCVE(cve);
                root.put("error", new SimpleScalar(error));
            }
        }
        if (pcn == null && oldUser) {
            try {
                this.adapter.update(this.adapter.login(new CookieAccessor(request.getCookies()), this.cookieNM), new ServletRequestAccessor(request), NameModifier.blank());
            } catch (ComplexValidatorException cve2) {
                Enumeration en = cve2.getExceptions().keys();
                while (en.hasMoreElements()) {
                    getLog().warn(((ValidationException) cve2.getExceptions().get(en.nextElement())).getMessage());
                }
            }
            Redirect.sendRedirect(response, request.getParameter("requestURL"), this.redirectType);
            throw new UserException("Try one more time");
        }
        if (pcn == null) {
            pcn = checkData(new ServletRequestAccessor(request));
        }
        if (pcn == null) {
            try {
                User u = this.adapter.signUp(new ServletRequestAccessor(request), new NameModifier("", "", null));
                this.mailer.send(u, tBase + "/signup_email.txt");
                setCookies(response, u);
            } catch (ComplexValidatorException cve3) {
                Enumeration en2 = cve3.getExceptions().keys();
                while (en2.hasMoreElements()) {
                    getLog().warn(((ValidationException) cve3.getExceptions().get(en2.nextElement())).getMessage());
                }
            }
            Redirect.sendRedirect(response, request.getParameter("requestURL"), this.redirectType);
            throw new UserException("try just one more time");
        }
        root.put("existing", new SimpleScalar(request.getParameter("existing")));
        root.put("requestURL", new SimpleScalar(request.getParameter("requestURL")));
        root.put("requestURL_enc", new SimpleScalar(URLEncoder.encode(request.getParameter("requestURL"))));
        root.put("servletURL", new SimpleScalar(request.getRequestURI()));
        root.put("pagename", new SimpleScalar(pcn.getName()));
        this.tCache.getTemplate(tBase + "/" + pcn.getTemplate()).process(root, response.getWriter());
        throw new UserLoginException("must Sign Up");
    }

    public void editUser(HttpServletRequest request, HttpServletResponse response, String tBase) throws UserException, IOException {
        String pageName = request.getParameter("pagename");
        PageConfigNode pcn = (PageConfigNode) this.editPages.get(pageName);
        Accessor data = new CookieAccessor(request.getCookies());
        listAccessor(data);
        User u = this.adapter.login(data, this.cookieNM);
        TemplateModelRoot root = TemplateServletUtils.copyRequest(request);
        root.put("config", new ConfigModel("psoft.user"));
        if (request.getParameter("data") == null) {
            String requestURL = request.getParameter("requestURL");
            if (requestURL == null) {
                requestURL = "";
            }
            root.put("requestURL", new SimpleScalar(requestURL));
            root.put("requestURL_enc", new SimpleScalar(URLEncoder.encode(requestURL)));
            try {
                fillRequest((SimpleHash) root.get("request"), pcn.getFields(), u.getAccessor());
            } catch (TemplateModelException tme) {
                getLog().error("editUser", tme);
            }
            root.put("pagename", new SimpleScalar(pageName));
            root.put("servletURL", new SimpleScalar(request.getRequestURI()));
            this.tCache.getTemplate(tBase + "/" + pcn.getTemplate()).process(root, response.getWriter());
            throw new UserException("User is being edited.");
        }
        try {
            this.adapter.validate(new ServletRequestAccessor(request), NameModifier.blank(), pcn.getFields());
            this.adapter.update(u, new ServletRequestAccessor(request), NameModifier.blank());
            Redirect.sendRedirect(response, request.getParameter("requestURL"), this.redirectType);
            throw new UserException("try just one more time");
        } catch (ComplexValidatorException cve) {
            String error = expandCVE(cve);
            root.put("error", new SimpleScalar(error));
            root.put("requestURL", new SimpleScalar(request.getParameter("requestURL")));
            root.put("requestURL_enc", new SimpleScalar(URLEncoder.encode(request.getParameter("requestURL"))));
            root.put("servletURL", new SimpleScalar(request.getParameter("servletURL")));
            root.put("pagename", new SimpleScalar(pageName));
            this.tCache.getTemplate(tBase + "/" + pcn.getTemplate()).process(root, response.getWriter());
            throw new UserException("User is being edited.");
        }
    }

    protected void logoutUser(HttpServletRequest request, HttpServletResponse response, String tBase) throws UserException, IOException {
        response.addCookie(new Cookie(this.cookieNM.getName("login"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("password"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("dir"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("url"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("server"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("port"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("real_login"), ""));
        response.addCookie(new Cookie(this.cookieNM.getName("email"), ""));
        TemplateModelRoot root = TemplateServletUtils.copyRequest(request);
        root.put("config", new ConfigModel("psoft.user"));
        root.put("servletURL", new SimpleScalar(request.getRequestURI()));
        LingualScalar ls = new LingualScalar();
        root.put("locale", new SimpleScalar(ls.getLocale().toString()));
        root.put("charset", new SimpleScalar(ls.getCharset()));
        root.put(AccountPreferences.LANGUAGE, ls);
        root.put("toolbox", Toolbox.toolbox);
        getTemplate("logouttemplate", tBase).process(root, response.getWriter());
        throw new UserException("loggedOut");
    }

    protected PageConfigNode checkData(Accessor data) {
        if (this.signupPages == null) {
            return null;
        }
        PageConfigNode pcn = (PageConfigNode) this.signupPages.get(this.firstSignupPage);
        do {
            try {
                this.adapter.checkPresence(pcn.getFields(), data, NameModifier.blank());
                pcn = (PageConfigNode) this.signupPages.get(pcn.getNext());
            } catch (ComplexValidatorException cve) {
                getLog().warn(expandCVE(cve));
                return pcn;
            }
        } while (pcn != null);
        return null;
    }

    protected void fillRequest(SimpleHash hash, Vector fields, Accessor data) {
        Enumeration en = fields.elements();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            ExtendedValidator eval = this.adapter.groupValidator(key);
            Enumeration en1 = eval.listFields(NameModifier.blank()).elements();
            while (en1.hasMoreElements()) {
                String key1 = (String) en1.nextElement();
                hash.put(key1, data.get(key1));
            }
        }
    }

    protected String expandCVE(ComplexValidatorException cve) {
        String s = "";
        Enumeration en = cve.getExceptions().keys();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            s = s + "<br>\n" + key + ":" + ((ValidationException) cve.getExceptions().get(key)).getMessage();
        }
        while (cve.hasMore()) {
            ComplexValidatorException newcve = cve.next();
            s = s + expandCVE(newcve);
        }
        return s;
    }

    protected void listAccessor(Accessor a) {
        Enumeration en = a.keys();
        while (en.hasMoreElements()) {
            String str = (String) en.nextElement();
        }
    }

    public static String getRequestURL(HttpServletRequest request) {
        String url = request.getRequestURI() + "?";
        String query = "";
        new ServletRequestAccessor(request);
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
            getLog().warn("NULL query");
        }
        return url;
    }

    protected void remind(HttpServletRequest request, HttpServletResponse response, String tBase) throws IOException, UserException {
        try {
            String email = request.getParameter("email");
            User u = (User) this.upm.get("email", email, GenericUser.class);
            this.mailer.send(u, "pass_reminder.txt");
            Redirect.sendRedirect(response, request.getParameter("redirectURL"), this.redirectType);
        } catch (PersistanceError e) {
            Redirect.sendRedirect(response, request.getParameter("failureURL"), this.redirectType);
        }
        throw new UserException("password mailed!");
    }
}
