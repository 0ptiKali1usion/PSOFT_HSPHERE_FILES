package psoft.hsphere;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.DriverManager;
import java.text.DateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import org.apache.log4j.PropertyConfigurator;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.admin.ServiceInitializer;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.admin.signupmanager.CachedSignupRecord;
import psoft.hsphere.background.JobManager;
import psoft.hsphere.billing.estimators.ComplexEstimatorManager;
import psoft.hsphere.cache.CacheFactory;
import psoft.hsphere.cron.CronManager;
import psoft.hsphere.design.DesignProvider;
import psoft.hsphere.ipmanagement.IPCache;
import psoft.hsphere.ipmanagement.IPRangeCache;
import psoft.hsphere.ipmanagement.IPSubnetCache;
import psoft.hsphere.p001ds.DSNetInterfaceManager;
import psoft.hsphere.p001ds.NetSwitchManager;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.admin.EnterpriseManager;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.servmon.MonitoringThread;
import psoft.hsphere.tools.LicenseChecker;
import psoft.hsphere.util.PackageConfigurator;
import psoft.hsphere.util.XMLManager;
import psoft.license.License;
import psoft.p000db.Database;
import psoft.user.UserException;
import psoft.user.UserLoginException;
import psoft.user.UserSignupException;
import psoft.util.Config;
import psoft.util.DestroyRegistry;
import psoft.util.NFUCache;
import psoft.util.ServletUtils;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;
import psoft.util.freemarker.ConfigModel;
import psoft.util.freemarker.CookieMaster;
import psoft.util.freemarker.FunctionRepository;
import psoft.util.freemarker.HtmlEncodedRequest;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateError;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateSession;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.Yafv;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.util.freemarker.acl.FMACLObjects;
import psoft.validators.UtilsAccessor;
import psoft.web.utils.HTMLEncoder;
import psoft.web.utils.Redirect;

/* renamed from: psoft.hsphere.CP */
/* loaded from: hsphere.zip:psoft/hsphere/CP.class */
public class C0004CP extends HttpServlet {
    protected Category LOG;
    protected Hashtable conf;
    protected ResourceBundle config;
    protected Hashtable lang;

    /* renamed from: dr */
    DestroyRegistry f28dr;
    Hashtable other;
    public static final String INETADDRTTL = "3600";
    protected boolean redirectType;
    protected String configFileName;
    protected TemplateHash online_help;
    protected TemplateHash html_help;
    protected static String version;
    protected static String versionDescription;
    protected static String hsRelease;
    protected static License license;
    protected int langCookieAge;
    protected static FMACLObjects objects;

    /* renamed from: cp */
    protected static C0004CP f29cp;
    protected static JobManager jobManager = null;
    protected static XMLManager xmlManager = null;
    protected static MonitoringThread monitorThread = null;
    protected static Hashtable languages = new Hashtable();
    protected static Hashtable menuBundles = new Hashtable();
    protected static Hashtable usrExcBundles = new Hashtable();
    protected static String langBundle = "";
    protected static String menuBundle = "";
    protected static String userBundle = "";
    protected static boolean useCustomLangBundle = false;
    protected static boolean useCustomMenuBundle = false;
    protected static boolean useCustomUserBundle = false;
    protected static Hashtable custLanguages = new Hashtable();
    protected static Hashtable custMenuBundles = new Hashtable();
    protected static Hashtable custUsrExcBundles = new Hashtable();
    protected static String custLangBundle = "";
    protected static String custMenuBundle = "";
    protected static String custUserBundle = "";
    protected static boolean irisEnabled = false;
    protected static boolean mydnsEnabled = false;
    protected static String CP_RUNNING = "CP_RUNNING";
    static List allowedSuspTemplates = Arrays.asList("submit/billing/new_info.sbm", "submit/suspended/new_billing_info.sbm", "submit/billing/change_info.sbm", "submit/billing/turn_receive_invoice.sbm", "submit/suspended/ec_change.sbm", "submit/suspended/change_billing_info.sbm", "tt/standalone_tt_form.html", "tt/standalone_tt_close.html", "tt/user_view_tts.html", "tt/user_view.html", "tt/standalone_user_view.html", "tt/new_tt_form.html", "submit/tt/new_tt_save.sbm", "submit/tt/user_edit_save.sbm", "submit/tt/user_close.sbm", "submit/tt/user_change_email.sbm");

    public static C0004CP getCP() {
        return f29cp;
    }

    protected String getConfigFileName() {
        return this.configFileName;
    }

    public Hashtable getConfig() {
        return this.conf;
    }

    public void setConfig() {
        Session.setLog(this.LOG);
        Config.set("CLIENT", this.conf);
        Config.set("OTHER", this.other);
        Config.set("psoft.user", this.conf);
        Config.set("LANG", this.lang);
    }

    public long getLastModified(HttpServletRequest req) {
        return TimeUtils.currentTimeMillis();
    }

    public void initLog() {
        try {
            PropertyConfigurator.configure(Toolbox.RB2Properties(this.config));
            this.LOG = Category.getInstance(getClass().getName());
            this.LOG.info("initialized");
        } catch (MissingResourceException e) {
        }
    }

    public C0004CP() {
        this("psoft_config.hsphere");
        if ("1".equals(System.getProperty(CP_RUNNING))) {
            throw new UnsupportedOperationException("The CP servlet cannot be instantinated more than once! Check config files.");
        }
        System.setProperty(CP_RUNNING, "1");
        f29cp = this;
        try {
            DesignProvider.initialize("DESIGN_SCHEME_CONFIG");
        } catch (Exception e) {
            Session.getLog().warn("Initialization Problems", e);
        }
        ServiceInitializer.init();
        if (xmlManager == null) {
            try {
                xmlManager = new XMLManager();
                xmlManager.start();
            } catch (Exception e2) {
                Session.getLog().warn("XML Manager initialization failed", e2);
            }
        }
        if (jobManager == null) {
            try {
                jobManager = new JobManager(this);
                jobManager.start();
            } catch (Exception e3) {
                Session.getLog().warn("Job Manager initialization failed", e3);
            }
        }
        if (monitorThread == null) {
            try {
                monitorThread = new MonitoringThread(this);
                monitorThread.start();
            } catch (Exception e4) {
                Session.getLog().debug("Monitoring initialization failed", e4);
            }
        }
    }

    public C0004CP(String configName) {
        this.f28dr = new DestroyRegistry();
        this.other = new Hashtable();
        this.configFileName = configName;
        initialize();
    }

    public void setNFUCache() {
        long maxMultiplier = 2;
        long multiplier = 1;
        try {
            maxMultiplier = Long.parseLong(this.config.getString("NFU_CACHE_MULTIPLIER_MAX"));
        } catch (Exception e) {
            Session.getLog().debug("NFU CACHE INITIALIZED WITH DEFAULT SIZE");
        }
        try {
            multiplier = Long.parseLong(this.config.getString("NFU_CACHE_MULTIPLIER"));
        } catch (Exception e2) {
            Session.getLog().debug("NFU CACHE INITIALIZED WITH DEFAULT SIZE");
        }
        Session.setCacheFactory(CacheFactory.getInstance());
        Session.getCacheFactory().setNfu(new NFUCache(1000 * multiplier, CronManager.MULTIPLIER, maxMultiplier));
        try {
            Session.getCacheFactory().registerCache(CachedSignupRecord.class);
            Session.getCacheFactory().registerCache(ComplexEstimatorManager.class);
            Session.getCacheFactory().registerCache(NetSwitchManager.class);
            Session.getCacheFactory().registerCache(DSNetInterfaceManager.class);
            Session.getCacheFactory().registerCache(IPCache.class);
            Session.getCacheFactory().registerCache(IPSubnetCache.class);
            Session.getCacheFactory().registerCache(IPRangeCache.class);
            Session.getCacheFactory().setDirToStore(PackageConfigurator.getDefaultValue("HSPHERE_HOME") + "/.stored/");
            Session.getCacheFactory().restore();
        } catch (Exception e3) {
            Session.getLog().error("Unable to register Signup Record Cache", e3);
        }
        Resource.setCache(new NFUCache(3000 * multiplier, CronManager.MULTIPLIER, maxMultiplier));
        SharedObject.setCache(new NFUCache(1000 * multiplier, CronManager.MULTIPLIER, maxMultiplier));
        User.setCache(new NFUCache(1000 * multiplier, CronManager.MULTIPLIER, maxMultiplier));
    }

    protected void initialize() {
        try {
            System.setOut(System.err);
            System.setProperty("sun.net.inetaddr.ttl", INETADDRTTL);
            this.config = PropertyResourceBundle.getBundle(getConfigFileName());
            Session.setResourceBundle(this.config);
            try {
                String language = "";
                String country = "";
                String variant = "";
                StringTokenizer tkz = new StringTokenizer(this.config.getString("LOCALE"), "_");
                if (tkz.hasMoreTokens()) {
                    language = tkz.nextToken();
                }
                if (tkz.hasMoreTokens()) {
                    country = tkz.nextToken();
                }
                if (tkz.hasMoreTokens()) {
                    variant = LanguageManager.STANDARD_CHARSET;
                }
                Locale.setDefault(new Locale(language, country, variant));
            } catch (MissingResourceException e) {
            }
            initLog();
            setNFUCache();
            setVersion();
            try {
                HostEntry.setEmulationMode(this.config.getString("EMULATION_MODE").equals("TRUE"));
            } catch (Exception e2) {
            }
            try {
                Session.setUserLoginLength(this.config.getString("USER_LOGIN_LENGTH"));
                Session.getLog().debug("User login length set to:" + Session.getUserLoginLength());
            } catch (Exception e3) {
            }
            Session.setMerchantGatewayManager(new MerchantGatewayManager());
            this.lang = new Hashtable();
            this.lang.put(Category.class, this.LOG);
            this.other.put(Category.class, this.LOG);
            this.other.put(DestroyRegistry.class, this.f28dr);
            this.conf = new Hashtable();
            Database db = Toolbox.getDB(this.config);
            Session.setDb(db);
            this.conf.put(Database.class, db);
            this.conf.put("config", this.config);
            this.conf.put(Category.class, this.LOG);
            this.conf.put("functions", new FunctionRepository(new UtilsAccessor()));
            if ("TRUE".equals(this.config.getString("LOG_JDBC"))) {
                DriverManager.setLogWriter(new PrintWriter(System.err));
            }
            try {
                String irisUser = this.config.getString("IRIS_USER");
                if (irisUser != null && !"".equals(irisUser)) {
                    irisEnabled = true;
                }
            } catch (Exception e4) {
            }
            try {
                String mydnsUser = Session.getPropertyString("MYDNS_USER");
                if (mydnsUser != null && !"".equals(mydnsUser)) {
                    mydnsEnabled = true;
                }
            } catch (Exception e5) {
            }
            setConfig();
            try {
                this.redirectType = this.config.getString("REDIRECT_TYPE").toUpperCase().equals("LOCATION");
            } catch (Exception e6) {
                this.redirectType = true;
            }
            this.online_help = HelpBuilder.getOnlineHelp("ONLINE_HELP_CONFIG");
            this.html_help = HelpBuilder.getHelp("HELP_CONFIG");
            Session.setResellerId(1L);
            license = new License(Settings.get().getValue("license"));
            Session.setResellerId(0L);
            String langBundlesDirectory = Session.getPropertyString("INT_LANGBUNDLE_DIRECTORY");
            if ("".equals(langBundlesDirectory)) {
                langBundlesDirectory = LangBundlesCompiler.INT_DIRECTORY;
            }
            langBundle = Session.getPropertyString("INT_TEMPLATE_BUNDLE");
            if ("".equals(langBundle)) {
                langBundle = langBundlesDirectory + "." + LangBundlesCompiler.INT_TEMPLATE_BUNDLE;
            }
            menuBundle = Session.getPropertyString("INT_MENU_BUNDLE");
            if ("".equals(menuBundle)) {
                menuBundle = langBundlesDirectory + "." + LangBundlesCompiler.INT_MENU_BUNDLE;
            }
            userBundle = Session.getPropertyString("INT_USER_BUNDLE");
            if ("".equals(userBundle)) {
                userBundle = langBundlesDirectory + "." + LangBundlesCompiler.INT_USER_BUNDLE;
            }
            boolean recreateBundles = false;
            try {
                ResourceBundle.getBundle(langBundle);
            } catch (MissingResourceException e7) {
                recreateBundles = true;
            }
            try {
                ResourceBundle.getBundle(menuBundle);
            } catch (MissingResourceException e8) {
                recreateBundles = true;
            }
            try {
                ResourceBundle.getBundle(userBundle);
            } catch (MissingResourceException e9) {
                recreateBundles = true;
            }
            if (recreateBundles) {
                LangBundlesCompiler.compile();
            }
            this.langCookieAge = Integer.parseInt(this.config.getString("COOKIE_AGE"), 10);
            Session.initIps();
            try {
                Session.initMailer();
            } catch (Exception e10) {
                Session.getLog().warn("Mailer initialization failed", e10);
            }
            objects = new FMACLObjects(XMLManager.getXML("ACL_OBJECTS"));
            try {
                this.f28dr.register((Database) this.conf.get(Database.class));
            } catch (NullPointerException e11) {
            }
        } catch (Exception e12) {
            e12.printStackTrace();
            throw new ExceptionInInitializerError(e12);
        }
    }

    public static void setLicense(String text) {
        license.update(text);
    }

    public static License getLicense() {
        return license;
    }

    public void destroy() {
        Session.getLog().debug("Stopping services");
        Mailer.stop();
        xmlManager.interrupt();
        jobManager.interrupt();
        monitorThread.die();
        this.f28dr.destroy();
        Session.getCacheFactory().store();
        try {
            monitorThread.join();
        } catch (Exception e) {
        }
    }

    public Account getAccount(ResourceId id, HttpServletResponse response) throws Exception {
        Account a = null;
        User u = Session.getUser();
        Session.getLog().info("ID--------->" + id);
        if (id != null) {
            try {
                a = u.getAccount(id);
                if (a != null) {
                    Session.setAccount(a);
                    Session.getModelRoot().put("account", a);
                }
            } catch (StringIndexOutOfBoundsException e) {
                Session.getLog().debug("Account not found, ID :" + id);
            }
            return a;
        }
        Collection col = u.getAccountIds();
        Session.getLog().debug("Number of accounts: " + (col != null ? col.size() : 0) + " RedirectType:" + this.redirectType);
        if (col != null && col.size() == 1) {
            String uri = Session.getUser().getURI() + col.iterator().next() + '/';
            Redirect.sendRedirect(response, uri, this.redirectType);
            throw new UserLoginException("try just one more time");
        }
        return null;
    }

    public boolean setAccount(HttpServletRequest req, HttpServletResponse res, User u) throws Exception {
        Account a;
        String accountId = req.getParameter("account_id");
        Session.getLog().debug("CP,getAccountId " + accountId);
        if (null == accountId || "".equals(accountId)) {
            String accountId2 = ServletUtils.getCookieValue(req.getCookies(), "hsphere-account-id");
            if (accountId2 != null && null != (a = u.getAccount(new ResourceId(accountId2)))) {
                Session.setAccount(a);
                Session.getModelRoot().put("account", a);
            }
            Collection col = u.getAccountIds();
            if (col.size() == 1) {
                setCookie((ResourceId) col.iterator().next(), u, res);
                return true;
            }
            return false;
        }
        setCookie(new ResourceId(accountId), u, res);
        return true;
    }

    protected void setCookie(ResourceId aid, User u, HttpServletResponse res) throws Exception {
        Session.setAccount(u.getAccount(aid));
        Cookie c = new Cookie("hsphere-account-id", aid.toString());
        res.addCookie(c);
    }

    protected void setVersion() {
        try {
            LineNumberReader vers = new LineNumberReader(new InputStreamReader(getClass().getResourceAsStream("/psoft_config/HS_VERSION")));
            while (true) {
                String line = vers.readLine();
                if (line == null) {
                    break;
                } else if (version == null) {
                    version = line;
                } else if (versionDescription == null) {
                    versionDescription = line;
                } else {
                    return;
                }
            }
        } catch (Exception ex) {
            Session.getLog().debug("Unable to read HS_VERSION file", ex);
        }
        if (version == null) {
            try {
                version = this.config.getString("HS_VERSION");
            } catch (Exception e) {
            }
        }
    }

    public static String getVersion() {
        return version;
    }

    public static String getVersionDescription() {
        return versionDescription;
    }

    protected String getTemplateName(Account a, HttpServletRequest request) {
        String template_name = request.getParameter("template_name");
        if (HsphereToolbox.isOnlineHelpTemplate(template_name)) {
            return template_name;
        }
        if (a != null) {
            if (!a.isSuspended()) {
                if (null == template_name) {
                    template_name = a.getPlan().getValue("_template");
                }
                if (null == template_name) {
                    template_name = "default.html";
                }
            } else if (template_name == null || (!template_name.startsWith("suspended/") && !allowedSuspTemplates.contains(template_name))) {
                template_name = "suspended/suspended.html";
            }
        } else {
            template_name = "design/choose_account.html";
        }
        return template_name;
    }

    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        setConfig();
        request.setCharacterEncoding("utf-8");
        Session.setRequest(request);
        Session.setResponse(response);
        try {
            try {
                try {
                    try {
                        try {
                            try {
                                setModelRoot(request, response);
                                Session.getLog().info("--------------->" + request.getPathInfo());
                                Session.getLog().info("--------------->" + request.getRequestURI());
                                try {
                                    User u = User.process(request, response);
                                    Session.setUser(u);
                                    Session.getModelRoot().put(FMACLManager.USER, u);
                                    String userName = null;
                                    ResourceId accId = null;
                                    Session.getLog().info("PATCH_INFO:" + request.getPathInfo());
                                    if (request.getPathInfo() != null) {
                                        StringTokenizer st = new StringTokenizer(request.getPathInfo(), "/");
                                        if (st.hasMoreTokens()) {
                                            userName = st.nextToken();
                                        }
                                        if (st.hasMoreTokens()) {
                                            String accIdStr = st.nextToken();
                                            try {
                                                accId = new ResourceId(accIdStr);
                                            } catch (Exception e) {
                                            }
                                        }
                                    }
                                    Session.getLog().info("USERNAME:" + userName + " AccountID:" + accId);
                                    Account a = getAccount(accId, response);
                                    String templateName = getTemplateName(a, request);
                                    Template template = Session.getTemplate(templateName);
                                    if (null == template) {
                                        response.getWriter().write("Unknown template : '" + HsphereToolbox.toolbox.FM_formatForHTML(templateName) + "'");
                                        try {
                                            Session.resetSession();
                                            return;
                                        } catch (Exception e2) {
                                            return;
                                        }
                                    }
                                    template.process(Session.getModelRoot(), response.getWriter());
                                    try {
                                        Session.resetSession();
                                    } catch (Exception e3) {
                                    }
                                } catch (HSUserException e4) {
                                    throw new UserException(e4.getMessage());
                                }
                            } catch (NoSuchMethodError error) {
                                Session.getLog().warn("Error", error);
                                try {
                                    Session.resetSession();
                                } catch (Exception e5) {
                                }
                            } catch (UnknownResellerException e6) {
                                TemplateModelRoot root = Session.getModelRoot();
                                String action = request.getParameter("action");
                                String uri = Session.getPropertyString("CP_URI");
                                root.put("cpuri", new TemplateString(uri));
                                if ("cp_login".equals(action)) {
                                    String ulogin = request.getParameter("login");
                                    String upassword = request.getParameter("password");
                                    if (ulogin != null && upassword != null) {
                                        try {
                                            User user = User.getUser(ulogin);
                                            if (upassword.equals(user.getPassword())) {
                                                root.put(FMACLManager.USER, user);
                                            } else {
                                                root.put("errmessage", new TemplateString(Localizer.translateMessage("user.incorrect_login")));
                                            }
                                        } catch (Exception e7) {
                                            root.put("errmessage", new TemplateString(Localizer.translateMessage("user.incorrect_login")));
                                            Session.getLog().debug("Can't login: ", e7);
                                        }
                                    }
                                }
                                if ("change_mbox_password".equals(action)) {
                                    try {
                                        User.changeMboxPassword(request, response);
                                    } catch (UserException e8) {
                                    }
                                    try {
                                        return;
                                    } catch (Exception e9) {
                                        return;
                                    }
                                }
                                Session.getTemplate("/errors/InvalidCpLocation.html").process(root, response.getWriter());
                                try {
                                    Session.resetSession();
                                } catch (Exception e10) {
                                }
                            }
                        } catch (UserSignupException e11) {
                            Session.getLog().debug("Signup ...");
                            try {
                                Session.resetSession();
                            } catch (Exception e12) {
                            }
                        }
                    } catch (TemplateError te) {
                        TemplateModelRoot root2 = Session.getModelRoot();
                        String encodedMessage = HTMLEncoder.encode(te.getMessage());
                        root2.put("message", new TemplateString(encodedMessage));
                        Session.addMessage(encodedMessage);
                        Session.getTemplate("/errors/GenericError").process(root2, response.getWriter());
                        Session.getLog().warn("template error", te);
                        try {
                            Session.resetSession();
                        } catch (Exception e13) {
                        }
                    }
                } catch (UserException ue) {
                    Session.getLog().debug(ue.getMessage());
                    TemplateModelRoot root3 = Session.getModelRoot();
                    String encodedMessage2 = HTMLEncoder.encode(ue.getMessage());
                    Session.addMessage(encodedMessage2);
                    root3.put("message", new TemplateString(encodedMessage2));
                    Session.getTemplate(Config.getProperty("psoft.user", "logintemplate")).process(root3, response.getWriter());
                    try {
                        Session.resetSession();
                    } catch (Exception e14) {
                    }
                }
            } catch (UserLoginException e15) {
                Session.getLog().debug("Logging ...");
                try {
                    Session.resetSession();
                } catch (Exception e16) {
                }
            }
        } finally {
            try {
                Session.resetSession();
            } catch (Exception e17) {
            }
        }
    }

    public void setModelRoot(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String type;
        TemplateModelRoot root = Session.getModelRoot();
        try {
            Session.setResellerId(request);
            root.put("reseller_id", new TemplateString(Session.getResellerId()));
            String curlang = request.getParameter("_language_");
            if (curlang != null) {
                setLanguageCookie(AccountPreferences.LANGUAGE, curlang, response);
            } else {
                curlang = ServletUtils.getCookieValue(request.getCookies(), AccountPreferences.LANGUAGE);
            }
            Language lang = new Language(curlang);
            Session.setLanguage(lang);
            root.put(FMACLManager.USER, Session.getUser());
            root.put("settings", new MapAdapter(Settings.get().getMap()));
            root.put("plans", new MapAdapter(Plan.getPlans()));
            root.put("locale", lang.getLocaleWrapper());
            root.put("charset", new TemplateString(LanguageManager.STANDARD_CHARSET));
        } catch (UnknownResellerException e) {
            Session.getLog().warn("Unknown reseller");
        }
        root.put("request", new HtmlEncodedRequest(request));
        root.put("config", new ConfigModel("CLIENT"));
        root.put("cookies", new CookieMaster(request, response));
        root.put("utils", new SimpleDynamicMethod("utils"));
        root.put("yafv", new Yafv("yafv_html.hsphere"));
        root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
        root.put("toolbox", HsphereToolbox.toolbox);
        root.put("session", new TemplateSession());
        root.put("new_user", new CreateUser(request, response));
        root.put("online_help", this.online_help);
        root.put("design", Session.getDesign());
        root.put("browser", Session.getBrowser());
        root.put("html_help", this.html_help);
        root.put("license", license);
        root.put("version", new TemplateString(getVersionDescription()));
        root.put("versionDescription", new TemplateString(getVersionDescription()));
        root.put("versionNumber", new TemplateString(getVersion()));
        TemplateList l = new TemplateList();
        Enumeration e2 = request.getParameterNames();
        while (e2.hasMoreElements()) {
            TemplateModel templateHash = new TemplateHash();
            String name = (String) e2.nextElement();
            String value = request.getParameter(name);
            templateHash.put("name", name);
            templateHash.put("value", value);
            l.add(templateHash);
        }
        root.put("params", l);
        String type2 = request.getParameter("_content_type");
        if (type2 != null) {
            type = "CONTENT_TYPE_" + type2;
        } else {
            type = "text/html";
        }
        response.setContentType(type + "; charset=UTF-8");
        Session.getLog().debug("Content type:" + response.getCharacterEncoding());
    }

    protected void setLanguageCookie(String name, String value, HttpServletResponse response) {
        Cookie cookie = new Cookie(name, value);
        cookie.setMaxAge(this.langCookieAge);
        cookie.setPath(Session.getProperty("CP_URI"));
        response.addCookie(cookie);
    }

    public static ResourceBundle getLabelBundle(Locale key) {
        return PropertyResourceBundle.getBundle(langBundle, key);
    }

    public static ResourceBundle getMenuBundle(Locale key) {
        return PropertyResourceBundle.getBundle(menuBundle, key);
    }

    public static ResourceBundle getMessageBundle(Locale key) {
        return PropertyResourceBundle.getBundle(userBundle, key);
    }

    public static void sendCPInfo() {
        String header;
        try {
            Session.setLanguage(new Language(null));
            Session.initMailer();
            Session.setResellerId(1L);
            Session.setUser(User.getUser(FMACLManager.ADMIN));
            Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
            TemplateModel templateModel = null;
            try {
                templateModel = (EnterpriseManager) Session.getAccount().FM_findChild("eeman").get();
            } catch (Exception e) {
            }
            ConfigModel conf = new ConfigModel("CLIENT");
            String header2 = "E-Mail:" + Session.getProperty("FROM_ADDRESS") + "\n====================================\nURL:" + Session.getProperty("CP_HOST") + "\n====================================\nVersion = " + Session.getProperty("HS_VERSION") + "\n====================================\nLICENSE\n-------\nCompany:" + getLicense().get("COMPANY").toString() + "\nName:" + getLicense().get("NAME").toString() + "\nIssued:" + getLicense().get("ISSUED").toString() + "\nIssued By:" + getLicense().get("ISSUER").toString() + "\nProduct:" + getLicense().get("PRODUCT").toString() + "\nLicense:" + getLicense().get("LICENSE").toString() + "\nAccounts:" + getLicense().get("ACCOUNTS").toString() + "\nLicense Key:" + getLicense().get("LICENSE_KEY").toString() + "\n";
            LicenseChecker lc = new LicenseChecker();
            if (lc.getLicenseStatus() < 0) {
                header = header2 + "------------------------------------\n";
            } else {
                header = header2 + "====================================\n";
            }
            String servers = "";
            try {
                Template t = Session.getTemplate("misc/email_info.txt");
                StringWriter out = new StringWriter();
                PrintWriter writer = new PrintWriter(out);
                SimpleHash root = new SimpleHash();
                root.put("eeman", templateModel);
                root.put("toolbox", HsphereToolbox.toolbox);
                root.put("config", conf);
                root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
                root.put("settings", new MapAdapter(Settings.get().getMap()));
                t.process(root, writer);
                writer.flush();
                writer.close();
                servers = out.toString();
            } catch (Exception e2) {
            }
            Session.getMailer().sendMessage("updates@psoft.net", "CP INFO:" + Session.getProperty("CP_HOST"), header + servers + "\n" + DateFormat.getDateInstance().format(TimeUtils.getDate()), Session.getCurrentCharset());
            TimeUtils.sleep(2000L);
        } catch (Throwable se) {
            Session.getLog().warn("Error sending message", se);
        }
    }

    public static void main(String[] args) throws Exception {
        new C0004CP("psoft_config.hsphere");
        sendCPInfo();
        System.exit(0);
    }

    public static boolean isIrisEnabled() {
        return irisEnabled;
    }

    public static boolean isMyDNSEnabled() {
        return mydnsEnabled;
    }

    public static JobManager getJobManager() {
        return jobManager;
    }

    public static synchronized String getHSRelease() {
        if (hsRelease != null) {
            return hsRelease;
        }
        try {
            LineNumberReader vers = new LineNumberReader(new InputStreamReader(C0004CP.class.getResourceAsStream("/psoft_config/HSRELEASE")));
            while (true) {
                String line = vers.readLine();
                if (line == null) {
                    break;
                } else if (hsRelease == null) {
                    hsRelease = line;
                }
            }
        } catch (Exception ex) {
            Session.getLog().debug("Unable to read /psoft_config/HSRELEASE file", ex);
        }
        return hsRelease;
    }
}
