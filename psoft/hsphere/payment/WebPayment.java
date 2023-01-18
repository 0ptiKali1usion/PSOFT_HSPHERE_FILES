package psoft.hsphere.payment;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Language;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.freemarker.ConfigModel;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateSession;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.Yafv;

/* loaded from: hsphere.zip:psoft/hsphere/payment/WebPayment.class */
public abstract class WebPayment {
    public static final int REDIRECT = 1;
    public static final int PAYMENT = 2;
    public static final int OTHER = 3;
    public static final int DEMOACCOUNTREDIRECT = 4;
    protected String processorName;
    protected int requestId;
    protected long accountId;
    protected long resellerId;
    protected long transactionId;
    protected String transInfo;
    protected double amount;
    private PaymentRequestInfo paymentRequestInfo;
    private boolean initialized;
    public NumberFormat engFormat = NumberFormat.getNumberInstance(new Locale("en", "US"));
    protected int requestType;
    protected HashMap values;
    protected static Category log = Category.getInstance(WebPayment.class.getName());

    protected abstract PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest httpServletRequest) throws Exception;

    protected abstract String getRedirectUrl() throws Exception;

    protected abstract String getRedirectMethod();

    protected abstract void validatePaymentRequest() throws Exception;

    protected abstract HashMap getRedirectValues(HttpServletRequest httpServletRequest) throws Exception;

    public void init(String name, HttpServletRequest request) throws Exception {
        this.initialized = false;
        String action = request.getParameter("action");
        this.processorName = name;
        this.requestId = 0;
        this.accountId = 0L;
        this.resellerId = 0L;
        this.transactionId = 0L;
        this.amount = 0.0d;
        this.requestType = 0;
        this.engFormat.setMinimumFractionDigits(2);
        this.engFormat.setMinimumIntegerDigits(1);
        this.engFormat.setMaximumFractionDigits(2);
        this.engFormat.setGroupingUsed(false);
        if ("redirect".equals(action)) {
            this.requestType = 1;
            initRedirect(request);
            if (this.accountId != 0) {
                Account a = (Account) Account.get(new ResourceId(this.accountId, 0));
                if (a.isDemoAccount()) {
                    this.requestType = 4;
                }
            } else if (this.requestId != 0) {
                Connection con = Session.getDb();
                PreparedStatement ps = null;
                try {
                    try {
                        ps = con.prepareStatement("SELECT user_id,bid,cid,plan_id,created,username,deleted FROM request_record, users WHERE request_id = ? AND users.id = user_id");
                        ps.setInt(1, this.requestId);
                        ResultSet rs = ps.executeQuery();
                        if (rs.next()) {
                            int planId = rs.getInt(4);
                            ps = con.prepareStatement("SELECT value FROM plan_value WHERE plan_id = ? AND name = '_EMULATION_MODE' AND value = 1");
                            ps.setInt(1, planId);
                            if (ps.executeQuery().next()) {
                                this.requestType = 4;
                            }
                        }
                        Session.closeStatement(ps);
                        con.close();
                    } catch (Exception ex) {
                        Session.getLog().error("Unable to get plan using signup request data ", ex);
                        Session.closeStatement(ps);
                        con.close();
                    }
                } catch (Throwable th) {
                    Session.closeStatement(ps);
                    con.close();
                    throw th;
                }
            }
        } else if (isEmpty(action)) {
            this.requestType = 2;
            initPayment(request);
        } else {
            this.requestType = 3;
            initOtherNotificationRequest(action, request);
        }
        this.initialized = true;
    }

    public boolean isInitialized() {
        return this.initialized;
    }

    protected void initRedirect(HttpServletRequest request) throws Exception {
        this.transInfo = request.getParameter("trans_id");
        if (isEmpty(this.transInfo)) {
            throw new Exception("Request info is empty. An incorrect redirect request has been received");
        }
        setTransactInfo(this.transInfo);
        this.values = MerchantManager.getProcessorSettings(this.processorName);
        String strAmount = request.getParameter("amount");
        if (isEmpty(strAmount)) {
            throw new Exception("An incorrect amount value is included for the :" + this.transInfo + " request");
        }
        this.amount = HsphereToolbox.parseLocalizedNumber(strAmount);
        if (this.accountId != 0) {
            this.transactionId = ExternalPaymentManager.requestAccountPayment(this.amount, this.accountId);
        } else if (this.requestId != 0) {
            this.transactionId = ExternalPaymentManager.requestSignupPayment(this.amount, this.requestId);
        } else {
            throw new Exception(" Problem getting account or request IDfor the :" + this.transInfo + " request");
        }
    }

    protected void initPayment(HttpServletRequest request) throws Exception {
        String invoiceId = "";
        this.paymentRequestInfo = initPaymentRequestInfo(request);
        try {
            invoiceId = this.paymentRequestInfo.getTransId();
            int index = invoiceId.indexOf("-");
            this.transactionId = Long.parseLong(invoiceId.substring(0, index));
            this.transInfo = invoiceId.substring(index + 1);
        } catch (Exception ex) {
            Session.getLog().error("Unable to get transaction ID from the payment notification request: " + invoiceId, ex);
            Session.getLog().debug("paymentRequestInfo: " + this.paymentRequestInfo.toString());
        }
        if (this.transactionId == 0) {
            throw new Exception("Unable to get transaction ID from the payment notification request: " + this.requestId);
        }
        setTransactInfo(this.transInfo);
        try {
            this.amount = this.paymentRequestInfo.getAmount();
            this.values = MerchantManager.getProcessorSettings(this.processorName);
        } catch (Exception e) {
            throw new Exception("Unable to get amount fro  the request: amount: ");
        }
    }

    protected void setTransactInfo(String info) throws Exception {
        if (info.startsWith("acc-")) {
            String fullAccId = info.substring(4);
            int acc_suff = fullAccId.indexOf("_0");
            String accId = fullAccId.substring(0, acc_suff);
            this.accountId = Long.parseLong(accId);
            Account a = (Account) Account.get(new ResourceId(this.accountId, 0));
            this.resellerId = a.getResellerId();
            Session.getLog().debug("Web Payment. resellerId: " + this.resellerId);
            Session.setResellerId(this.resellerId);
        } else if (!info.startsWith("sig-")) {
            if (info.startsWith("mail-")) {
                this.requestId = Integer.parseInt(info.substring(5));
                setResellerBySignup(this.requestId);
            }
        } else {
            String accId2 = info.substring(4);
            Connection con_db = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con_db.prepareStatement("SELECT id FROM request WHERE name = 'signup_id' and value= ?");
                ps.setString(1, accId2);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.requestId = rs.getInt("id");
                }
                Session.closeStatement(ps);
                con_db.close();
                setResellerBySignup(this.requestId);
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con_db.close();
                throw th;
            }
        }
    }

    public void processRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (this.requestType == 1) {
            processRedirectRequest(request, response);
        } else if (this.requestType == 2) {
            confirmPayment(request);
        } else if (this.requestType != 4) {
            processOtherNotificationRequest(request);
        }
    }

    protected void processRedirectRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String url = getRedirectUrl();
        response.setContentType("text/html");
        String method = "GET".equals(getRedirectMethod()) ? "GET" : "POST";
        HashMap values = getRedirectValues(request);
        Template template = Session.getTemplate("submit/billing/redirect.sbm");
        SimpleHash root = new SimpleHash();
        ArrayList data = new ArrayList();
        for (String key : values.keySet()) {
            HashMap map = new HashMap();
            map.put(MerchantGatewayManager.MG_KEY_PREFIX, key);
            map.put("data", values.get(key));
            data.add(map);
        }
        root.put("paymenturl", new TemplateString(url));
        root.put("paymentmethod", new TemplateString(method));
        root.put("values", new ListAdapter(data));
        template.process(root, response.getWriter());
    }

    protected void confirmPayment(HttpServletRequest request) throws Exception {
        try {
            validatePaymentRequest();
            String descr = this.transactionId + "-" + this.transInfo;
            ExternalPaymentManager.confirmPayment(this.amount, this.transactionId, this.processorName, descr);
        } catch (DuplicatePaymentException e) {
            Session.getLog().warn(this.processorName + ": A duplicate payment has been received, possibly a client has been redirected back to hsphere.");
        }
    }

    protected void setResellerBySignup(long signup_id) {
        try {
            Connection con_db = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con_db.prepareStatement("SELECT u.reseller_id FROM users AS u JOIN request_record AS rr ON (u.id=rr.user_id) WHERE rr.request_id=?");
                ps.setLong(1, signup_id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    this.resellerId = rs.getInt(1);
                }
                Session.setResellerId(this.resellerId);
                Session.getLog().debug("Web Payment. resellerId: " + this.resellerId);
                Session.closeStatement(ps);
                con_db.close();
            } catch (Exception ex) {
                Session.getLog().error("Problem setting reseller ID", ex);
                Session.closeStatement(ps);
                con_db.close();
            }
        } catch (Exception ex2) {
            Session.getLog().error("Can`t get id from request", ex2);
        }
    }

    public static boolean isEmpty(String value) {
        return value == null || "".equals(value);
    }

    public String getValue(String name) {
        return (String) this.values.get(name);
    }

    public double getAmount() {
        return this.amount;
    }

    public String getInvoiceID() {
        return this.transactionId + "-" + this.transInfo;
    }

    public String getFormatedAmount() {
        return this.engFormat.format(getAmount());
    }

    public long getTransactionId() {
        return this.transactionId;
    }

    public int getRequestId() {
        return this.requestId;
    }

    public long getAccountId() {
        return this.accountId;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public HashMap getTransactInfo() {
        HashMap info = new HashMap();
        if (this.accountId > 0) {
            info.put("accountId", Long.toString(this.accountId));
        }
        if (this.requestId > 0) {
            info.put("requestId", Integer.toString(this.requestId));
        }
        if (this.transactionId > 0) {
            info.put("transactionId", Long.toString(this.transactionId));
        }
        if (!isEmpty(this.transInfo)) {
            info.put("transInfo", this.transInfo);
        }
        if (this.amount > 0.0d) {
            info.put("amount", getFormatedAmount());
        }
        return info;
    }

    public String getServletPath() throws Exception {
        Reseller resel = Reseller.getReseller(this.resellerId);
        String protocol = resel.getProtocol();
        String port = resel.getPort().trim();
        String url = protocol + resel.getURL() + ((port == null || "".equals(port)) ? "" : ":" + port);
        String uri = Session.getPropertyString("CP_URI");
        String uri2 = uri == null ? "" : uri;
        int pos = uri2.lastIndexOf(47);
        if (pos > 0) {
            uri2 = uri2.substring(0, pos + 1);
        }
        return url + uri2 + getValue("servlet");
    }

    public int getRequestType() {
        return this.requestType;
    }

    public PaymentRequestInfo getPaymentRequestInfo() throws Exception {
        return this.paymentRequestInfo;
    }

    public void checkIP(String addr) throws Exception {
        Session.getLog().debug("Remoute IP is: " + addr);
        String ips = getValue("IPs");
        Session.getLog().debug("Allowed IP list for reseler: " + Session.getResellerId() + ". Gateway:  " + this.processorName + " gateway is : " + ips);
        if (ips != null && !"".equals(ips)) {
            StringTokenizer st = new StringTokenizer(ips, ";");
            while (st.hasMoreTokens()) {
                String ip = st.nextToken();
                StringTokenizer st1 = new StringTokenizer(ip, ".");
                StringTokenizer st2 = new StringTokenizer(addr, ".");
                int i = 0;
                while (i < 4) {
                    String part1 = st1.nextToken();
                    String part2 = st2.nextToken();
                    if (!"*".equals(part1) && !part1.equals(part2)) {
                        break;
                    }
                    i++;
                }
                if (i == 4) {
                    return;
                }
            }
            throw new Exception("Remote IP (" + addr + ") is not included into " + this.processorName + " allowed IP list.");
        }
        log.warn("Allowed IP list for " + this.processorName + " gateway is empty");
    }

    public void processResponse(HttpServletResponse response, short transResult, String errorMessage) throws Exception {
        TemplateModelRoot root = Session.getModelRoot();
        response.setContentType("text/html");
        Session.setResellerId(this.resellerId);
        Template template = Session.getTemplate("billing/paymentresult.html");
        Language lang = Session.getLanguage();
        root.put("locale", lang.getLocaleWrapper());
        root.put("settings", new MapAdapter(Settings.get().getMap()));
        root.put("reseller_id", new TemplateString(Session.getResellerId()));
        root.put("processorName", new TemplateString(this.processorName));
        root.put("transResult", new TemplateString(transResult == 0 ? "success" : "error"));
        root.put("charset", new TemplateString(LanguageManager.STANDARD_CHARSET));
        root.put("config", new ConfigModel("CLIENT"));
        root.put("yafv", new Yafv("yafv_html.hsphere"));
        root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
        root.put("toolbox", HsphereToolbox.toolbox);
        root.put("session", new TemplateSession());
        root.put("design", Session.getDesign());
        root.put("browser", Session.getBrowser());
        if (transResult != 0) {
            root.put("errorMessage", new TemplateString(errorMessage != null ? errorMessage : ""));
        }
        HashMap info = getTransactInfo();
        if (info != null && !info.isEmpty()) {
            for (String key : info.keySet()) {
                root.put(key, new TemplateString((String) info.get(key)));
            }
        }
        template.process(root, response.getWriter());
    }

    public void displayDemoRedirectPage(HttpServletResponse response) {
        try {
            TemplateModelRoot root = Session.getModelRoot();
            response.setContentType("text/html");
            Session.setResellerId(this.resellerId);
            Template template = Session.getTemplate("billing/demoredirect.html");
            Language lang = Session.getLanguage();
            root.put("locale", lang.getLocaleWrapper());
            root.put("settings", new MapAdapter(Settings.get().getMap()));
            root.put("reseller_id", new TemplateString(Session.getResellerId()));
            root.put("processorName", new TemplateString(this.processorName));
            root.put("charset", new TemplateString(LanguageManager.STANDARD_CHARSET));
            root.put("config", new ConfigModel("CLIENT"));
            root.put("yafv", new Yafv("yafv_html.hsphere"));
            root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
            root.put("toolbox", HsphereToolbox.toolbox);
            root.put("session", new TemplateSession());
            root.put("design", Session.getDesign());
            root.put("browser", Session.getBrowser());
            Session.getLog().debug("Processing template" + template.toString());
            template.process(root, response.getWriter());
        } catch (Exception ex) {
            Session.getLog().error("Problem displaying the demo redirect result.", ex);
        }
    }

    protected void initOtherNotificationRequest(String action, HttpServletRequest request) throws Exception {
        Session.setResellerId(request);
        this.resellerId = Session.getResellerId();
    }

    protected void processOtherNotificationRequest(HttpServletRequest request) throws Exception {
        String action = request.getParameter("action");
        if (!"success".equals(action)) {
            throw new Exception("The payment can't be processed");
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/payment/WebPayment$PaymentRequestInfo.class */
    public class PaymentRequestInfo {
        private double amount;
        private String transId;
        private HashMap requestData = new HashMap();
        private String remoteAddr;

        public PaymentRequestInfo(HttpServletRequest request, double amount, String transId) {
            WebPayment.this = r5;
            this.amount = amount;
            this.transId = transId;
            this.remoteAddr = request.getRemoteAddr();
            Enumeration e = request.getParameterNames();
            while (e.hasMoreElements()) {
                String pname = (String) e.nextElement();
                String pvalue = request.getParameter(pname);
                this.requestData.put(pname, pvalue);
            }
        }

        public double getAmount() {
            return this.amount;
        }

        public String getTransId() {
            return this.transId;
        }

        public String getRemoteAddr() {
            return this.remoteAddr;
        }

        public String getRequestValue(String key) {
            return (String) this.requestData.get(key);
        }

        public HashMap getRequestValues() {
            return this.requestData;
        }
    }

    public static String getRequestTypeDescription(int type) {
        String description = "";
        switch (type) {
            case 1:
                description = Localizer.translateMessage("webpayment.redirect");
                break;
            case 2:
                description = Localizer.translateMessage("webpayment.payment");
                break;
            case 3:
                description = Localizer.translateMessage("webpayment.other");
                break;
        }
        return description;
    }
}
