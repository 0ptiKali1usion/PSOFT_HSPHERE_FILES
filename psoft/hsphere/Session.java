package psoft.hsphere;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateCache;
import freemarker.template.TemplateModelRoot;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.ResourceBundle;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import psoft.epayment.MerchantGateway;
import psoft.epayment.UnknownPaymentInstrumentException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.cache.CacheFactory;
import psoft.hsphere.design.SessionDesign;
import psoft.hsphere.global.Globals;
import psoft.hsphere.resource.admin.params.Params;
import psoft.hsphere.resource.email.AntiSpam;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.p000db.Database;
import psoft.p000db.DumbConnection;
import psoft.util.Config;
import psoft.util.NFUCache;
import psoft.util.freemarker.SessionBrowser;
import psoft.util.freemarker.Yafv;
import psoft.util.freemarker.YafvExecResult;

/* loaded from: hsphere.zip:psoft/hsphere/Session.class */
public class Session {
    protected static TemplateCache tCache;

    /* renamed from: rb */
    protected static ResourceBundle f49rb;
    protected static CacheFactory cacheFactory;
    protected static MerchantGatewayManager mgm;

    /* renamed from: db */
    protected static Database f50db;
    protected static Params params;
    protected static long pserverID;
    protected static Map mailer;
    private static Category cat = Category.getInstance(Session.class.getName());
    private static Category billingCat = Category.getInstance("biling");
    protected static ThreadLocal account = new ThreadLocal();
    protected static ThreadLocal user = new ThreadLocal();
    protected static ThreadLocal resellerId = new ThreadLocal();
    protected static ThreadLocal accessTable = new ThreadLocal();
    protected static ThreadLocal modelRoot = new ThreadLocal();
    protected static ThreadLocal log = new ThreadLocal();
    protected static ThreadLocal request = new ThreadLocal();
    protected static ThreadLocal response = new ThreadLocal();
    protected static ThreadLocal transConnection = new ThreadLocal();
    protected static ThreadLocal design = new ThreadLocal();
    protected static ThreadLocal browser = new ThreadLocal();
    protected static ThreadLocal billingNote = new ThreadLocal();
    protected static ThreadLocal oldState = new ThreadLocal();
    protected static ThreadLocal language = new ThreadLocal();
    protected static int USER_LOGIN_LENGTH = 8;
    protected static NFUCache downloads = new NFUCache(100, 900000);
    protected static Random rand = new Random();
    protected static HashMap ext2intIPs = null;
    protected static HashMap int2extIPs = null;
    protected static Hashtable mailServersParams = null;
    protected static Params baseMailServersParams = null;
    protected static ThreadLocal aid = new ThreadLocal();

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/Session$State.class */
    public static class State {
        protected User user;
        protected Account account;
        protected HttpServletRequest request;
        protected Language lang;

        public State(User user, Account account, HttpServletRequest request, Language lang) {
            this.user = user;
            this.account = account;
            this.request = request;
            this.lang = lang;
        }

        public Account getAccount() {
            return this.account;
        }

        public Language getLang() {
            return this.lang;
        }

        public HttpServletRequest getRequest() {
            return this.request;
        }

        public User getUser() {
            return this.user;
        }
    }

    public static void setLanguage(Language lang) {
        language.set(lang);
    }

    public static Language getLanguage() {
        Language res = (Language) language.get();
        if (res == null) {
            res = new Language(null);
            setLanguage(res);
        }
        return res;
    }

    public static Downloadable getDownload(String key) {
        return (Downloadable) downloads.get(key);
    }

    public static String setDownload(String prefix, Downloadable d) {
        String key = prefix + rand.nextLong();
        downloads.put(key, d);
        return key;
    }

    public static boolean isAccountingThread() {
        return true;
    }

    public static void save() {
        LinkedList stack = (LinkedList) oldState.get();
        if (stack == null) {
            stack = new LinkedList();
            oldState.set(stack);
        }
        stack.addFirst(new State(getUser(), getAccount(), getRequest(), (Language) language.get()));
    }

    public static void restore() throws UnknownResellerException {
        LinkedList stack = (LinkedList) oldState.get();
        if (stack != null) {
            State st = null;
            try {
                st = (State) stack.removeFirst();
            } catch (NoSuchElementException e) {
            }
            if (st != null) {
                setUser(st.getUser());
                setAccount(st.getAccount());
                setRequest(st.getRequest());
                setLanguage(st.getLang());
            }
        }
    }

    public static void setBillingNote(String note) {
        billingNote.set(note);
    }

    public static void resetBillingNote() {
        billingNote.set(null);
    }

    public static String getBillingNote() {
        return (String) billingNote.get();
    }

    public static String getPropertyString(String key) {
        try {
            return f49rb.getString(key).trim();
        } catch (NullPointerException | MissingResourceException e) {
            return "";
        }
    }

    public static void setResourceBundle(ResourceBundle rb) {
        f49rb = rb;
        tCache = new HSTemplateCache(rb);
    }

    public static ResourceBundle getResourceBundle() {
        return f49rb;
    }

    public static Locale getCurrentLocale() {
        return getLanguage().getLocale();
    }

    public static String getCurrentCharset() {
        return getLanguage().getCharset();
    }

    public static SessionBrowser getBrowser() {
        SessionBrowser br = (SessionBrowser) browser.get();
        if (br == null) {
            try {
                br = new SessionBrowser(getRequest().getHeader("user-agent"));
            } catch (Exception e) {
                br = new SessionBrowser("");
            }
            browser.set(br);
        }
        return br;
    }

    public static void resetDesign() {
        SessionDesign sd = (SessionDesign) design.get();
        if (sd != null) {
            sd.reset();
        }
    }

    public static SessionDesign getDesign() {
        SessionDesign sd = (SessionDesign) design.get();
        if (sd == null) {
            sd = new SessionDesign();
            design.set(sd);
        }
        return sd;
    }

    public static String getDesignTemplateDir() {
        return getDesign().designTemplateDir();
    }

    public static String getReplacementTemplateDir() {
        return getDesign().replacementTemplateDir();
    }

    public static Template getTemplate(String name) {
        return tCache.getTemplate(name);
    }

    public static void setLog(Category l) {
        log.set(l);
    }

    public static TemplateModelRoot getModelRoot() {
        TemplateModelRoot root = (TemplateModelRoot) modelRoot.get();
        if (null == root) {
            root = new SimpleHash();
            root.put("obj", C0004CP.objects);
            modelRoot.set(root);
        }
        return root;
    }

    public static void setMerchantGatewayManager(MerchantGatewayManager newMGM) {
        mgm = newMGM;
    }

    public static MerchantGatewayManager getMerchantGatewayManager() {
        return mgm;
    }

    public static MerchantGateway getMerchantGateway(String type) throws UnknownPaymentInstrumentException, Exception {
        return mgm.getMerchantGateway(type);
    }

    public static void addMessage(String key) {
        try {
            getModelRoot().get("session").addMessage(key);
        } catch (Exception e) {
        }
    }

    public static AccessTable getAccessTable() {
        try {
            return AccessTable.get(getUser(), getAccount());
        } catch (Exception e) {
            getLog().error("Access Table", e);
            return null;
        }
    }

    public static void validateAccount(long id) throws Exception {
        if (getAccount().getId().getId() != id) {
            throw new Exception("Invalid Account Id -->" + id);
        }
    }

    public static Account getAccount() {
        return (Account) account.get();
    }

    public static User getUser() {
        return (User) user.get();
    }

    public static HttpServletRequest getRequest() {
        return (HttpServletRequest) request.get();
    }

    public static HttpServletResponse getResponse() {
        return (HttpServletResponse) response.get();
    }

    public static int getNewId(String name) throws SQLException {
        return f50db.getNewId(name);
    }

    public static long getNewIdAsLong() throws SQLException {
        return getNewIdAsLong("newid");
    }

    public static long getNewIdAsLong(String name) throws SQLException {
        return f50db.getNewIdAsLong(name);
    }

    public static void setDb(Database db) {
        f50db = db;
    }

    public static void setAllPhysicalServersParams(Params params2) {
        params = params2;
    }

    public static Params getAllPhysicalServersParams() {
        return params;
    }

    public static void setPserverID(long ID) {
        pserverID = ID;
    }

    public static long getPserverID() {
        return pserverID;
    }

    public static void setMailServerParams(long physicalServerId, Params params2) {
        if (mailServersParams == null) {
            mailServersParams = new Hashtable();
        }
        mailServersParams.put(String.valueOf(physicalServerId), params2);
    }

    public static Params getMailServerParams(long physicalServerId) {
        Object params2;
        if (mailServersParams == null || (params2 = mailServersParams.get(String.valueOf(physicalServerId))) == null) {
            return null;
        }
        return (Params) params2;
    }

    public static void setBaseMailServerParams(Params baseParams) {
        baseMailServersParams = baseParams;
    }

    public static Params getBaseMailServerParams() {
        if (baseMailServersParams != null) {
            return baseMailServersParams.copy();
        }
        return baseMailServersParams;
    }

    public static String getClobValue(ResultSet rs, int index) throws SQLException {
        return f50db.getClobValue(rs, index);
    }

    public static String getClobValue(ResultSet rs, String name) throws SQLException {
        return f50db.getClobValue(rs, name);
    }

    public static void setClobValue(PreparedStatement ps, int index, String value) throws SQLException {
        f50db.setClobValue(ps, index, value);
    }

    public static void setClobValue(PreparedStatement ps, int index, String value, String encoding) throws SQLException {
        f50db.setClobValue(ps, index, value, encoding);
    }

    public static Connection getDb() throws SQLException {
        return getDb(AntiSpam.DEFAULT_LEVEL_VALUE);
    }

    public static Connection getDb(String type) throws SQLException {
        Connection con = (Connection) transConnection.get();
        return con == null ? f50db.getConnection(type) : new DumbConnection(con);
    }

    public static Connection getTransConnection() throws SQLException {
        getLog().debug("Getting exclusive connection");
        if (((Connection) transConnection.get()) != null) {
            throw new SQLException("Already in transaction");
        }
        Connection con = f50db.getTransConnection();
        transConnection.set(con);
        return con;
    }

    public static void commitTransConnection(Connection con) throws SQLException {
        getLog().debug("Releasing exclusive connection");
        transConnection.set(null);
        f50db.commitTransConnection(con);
    }

    public static boolean isTransConnection() throws SQLException {
        return transConnection.get() != null;
    }

    public static void initMailer() throws Exception {
        long oldResellerId;
        mailer = new HashMap();
        try {
            oldResellerId = getResellerId();
        } catch (UnknownResellerException e) {
            oldResellerId = 0;
        }
        try {
            for (Reseller resel : Reseller.getResellerList()) {
                setResellerId(resel.getId());
                String smtp = getPropertyString("SMTP_HOST");
                String email = Settings.get().getValue("email");
                if (email == null) {
                    email = getPropertyString("FROM_ADDRESS");
                }
                mailer.put(new Long(resel.getId()), new Mailer(smtp, email));
            }
        } finally {
            setResellerId(oldResellerId);
        }
    }

    public static Mailer getMailer() throws Exception {
        long reselId = getResellerId();
        Mailer ml = (Mailer) mailer.get(new Long(reselId));
        if (ml != null) {
            return ml;
        }
        String email = Settings.get().getValue("email");
        if (email == null) {
            email = getPropertyString("FROM_ADDRESS");
        }
        String smtp = getPropertyString("SMTP_HOST").trim();
        Mailer ml2 = new Mailer(smtp, email);
        mailer.put(new Long(reselId), ml2);
        return ml2;
    }

    public static String getProperty(String key) {
        return Config.getProperty("CLIENT", key);
    }

    public static void setAccount(Account a) {
        account.set(a);
        resetDesign();
    }

    public static void setUser(User u) throws UnknownResellerException {
        user.set(u);
        if (u == null) {
            resellerId.set(null);
        } else {
            setResellerId(u.getResellerId());
        }
    }

    public static void setRequest(HttpServletRequest r) {
        request.set(r);
    }

    public static void setResponse(HttpServletResponse r) {
        response.set(r);
    }

    public static long getAccountId() {
        Account a = (Account) account.get();
        if (a != null) {
            return a.getId().getId();
        }
        Long l = (Long) aid.get();
        if (l == null) {
            return 0L;
        }
        return l.longValue();
    }

    public static void setAccountId(long aid2) {
        aid.set(new Long(aid2));
    }

    public static long getResellerId() throws UnknownResellerException {
        Long l = (Long) resellerId.get();
        if (l == null) {
            throw new UnknownResellerException("Unknown reseller Id");
        }
        return l.longValue();
    }

    public static Reseller getReseller() throws UnknownResellerException {
        return Reseller.getReseller(getResellerId());
    }

    public static void setResellerId(Long resellerId2) throws UnknownResellerException {
        setResellerId(resellerId2.longValue());
    }

    public static void setResellerId(long resellerId2) throws UnknownResellerException {
        if (resellerId2 == 0) {
            resellerId.set(null);
            return;
        }
        Reseller reseller = Reseller.getReseller(resellerId2);
        if (reseller == null) {
            throw new UnknownResellerException("Unknown reseller Id");
        }
        resellerId.set(new Long(resellerId2));
    }

    public static void setResellerId(HttpServletRequest request2) throws UnknownResellerException {
        String resellerURL = request2.getHeader("host");
        try {
            Reseller reseller = Reseller.getReseller(resellerURL);
            resellerId.set(new Long(reseller.getId()));
        } catch (Exception ex) {
            throw new UnknownResellerException(ex);
        }
    }

    public static Category getLog() {
        Category cat1 = (Category) log.get();
        return cat1 == null ? cat : cat1;
    }

    public static Category getBillingLog() {
        return billingCat;
    }

    public static void billingLog(double amount, String descr, double resAmount, String resDescr, String type) {
        String userDescr;
        String IP = "";
        try {
            IP = getRequest().getRemoteAddr();
        } catch (Exception e) {
        }
        try {
            userDescr = "ACCOUNT:" + getAccount().getId().getId() + " " + type + ":" + descr + " AMOUNT:" + amount + " NOTE:" + getBillingNote() + " BILL#:" + getAccount().getBill().getId() + " BALANCE:" + getAccount().getBill().getBalance() + ("".equals(IP) ? "" : " REMOTE IP:" + IP);
        } catch (Exception e2) {
            userDescr = "UNKNOWN ACCOUNT " + type + ":" + descr + " AMOUNT:" + amount + " NOTE:" + getBillingNote();
        }
        getBillingLog().info(userDescr);
        String resellerDescr = null;
        try {
            if (getResellerId() != 1) {
                resellerDescr = "ACCOUNT:" + getAccount().getId().getId() + " RESELLER:" + getResellerId() + " " + type + ":" + resDescr + " AMOUNT:" + resAmount;
            }
            if (resellerDescr != null) {
                getBillingLog().info(resellerDescr);
            }
        } catch (UnknownResellerException e3) {
        }
    }

    public static void resetSession() throws UnknownResellerException {
        setUser(null);
        setAccount(null);
        setAccountId(0L);
        accessTable.set(null);
        log.set(null);
        request.set(null);
        response.set(null);
        modelRoot.set(null);
        resellerId.set(null);
        setLanguage(null);
        browser.set(null);
        resetBillingNote();
    }

    public static YafvExecResult yafvExec(String vMethod, List params2) throws Exception {
        Yafv yafv = getModelRoot().get("yafv");
        return yafv.internalCall(vMethod, params2);
    }

    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (Exception e) {
                getLog().error("Error closing statement", e);
            }
        }
    }

    public static String initDefaultValue(String param, String defaultValue) {
        return (param == null || "".equals(param)) ? defaultValue : param;
    }

    public static void initIps() throws Exception {
        try {
            String ipsXMLFileName = getProperty("IPS-XML-FILENAME");
            try {
                InputSource dataFile = new InputSource(new FileInputStream(ipsXMLFileName));
                try {
                    DOMParser parser = new DOMParser();
                    parser.parse(dataFile);
                    Document doc = parser.getDocument();
                    Element root = doc.getDocumentElement();
                    NodeList list = root.getElementsByTagName("ip");
                    int2extIPs = new HashMap();
                    ext2intIPs = new HashMap();
                    for (int i = 0; i < list.getLength(); i++) {
                        Element ipEl = (Element) list.item(i);
                        String external = ipEl.getAttributes().getNamedItem("ext").getNodeValue();
                        String internal = ipEl.getAttributes().getNamedItem("int").getNodeValue();
                        ext2intIPs.put(external, internal);
                        int2extIPs.put(internal, external);
                    }
                } catch (Exception ex) {
                    getLog().error("Error parsing " + ipsXMLFileName + " file.", ex);
                }
            } catch (FileNotFoundException ex2) {
                getLog().error("File set in IPS-XML-FILENAME property not found", ex2);
            }
        } catch (MissingResourceException e) {
            getLog().error("IPS-XML-FILENAME property not set");
        }
    }

    public static String int2ext(String internal) {
        if (int2extIPs == null) {
            return internal;
        }
        String external = (String) int2extIPs.get(internal);
        if (external == null) {
            return internal;
        }
        return external;
    }

    public static String ext2int(String external) {
        if (ext2intIPs == null) {
            return external;
        }
        String internal = (String) ext2intIPs.get(external);
        if (internal == null) {
            return external;
        }
        return internal;
    }

    public static void setUserLoginLength(String length) {
        try {
            USER_LOGIN_LENGTH = Integer.parseInt(length);
            if (USER_LOGIN_LENGTH < 8) {
                USER_LOGIN_LENGTH = 8;
            }
        } catch (Exception e) {
            USER_LOGIN_LENGTH = 8;
        }
    }

    public static int getUserLoginLength() {
        return USER_LOGIN_LENGTH;
    }

    public static boolean useAccelerator() throws Exception {
        boolean result = false;
        try {
            String fast_traffic = getPropertyString("FAST_TRAFFIC");
            if (fast_traffic != null && !"".equals(fast_traffic) && "TRUE".equals(fast_traffic)) {
                result = true;
                getLog().debug("Use accelerator");
            }
        } catch (MissingResourceException e) {
        }
        return result;
    }

    public static boolean hasFlagField() throws Exception {
        Connection con = getDb();
        DatabaseMetaData dbMetaData = con.getMetaData();
        ResultSet columns = dbMetaData.getColumns(null, "", "trans_log", "flag");
        while (columns.next()) {
            String tableName = columns.getString(3);
            String columnName = columns.getString(4);
            String columnJavaType = columns.getString(5);
            if ("trans_log".equals(tableName) && "flag".equals(columnName) && Integer.parseInt(columnJavaType) == 4) {
                return true;
            }
        }
        return false;
    }

    public static boolean isResourceDisabled(String key) throws Exception {
        int res = Globals.isObjectDisabled(key);
        return res != 0;
    }

    public static CacheFactory getCacheFactory() {
        return cacheFactory;
    }

    public static void setCacheFactory(CacheFactory cacheFactory2) {
        cacheFactory = cacheFactory2;
    }
}
