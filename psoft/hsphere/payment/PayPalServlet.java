package psoft.hsphere.payment;

import com.sun.net.ssl.internal.ssl.Provider;
import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Security;
import java.text.DecimalFormatSymbols;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PayPalServlet.class */
public class PayPalServlet extends ExternalPayServlet {
    private static final String GATEWAY = "PayPal";
    private static Category log = Category.getInstance(PayPalServlet.class.getName());
    static HashMap txnIds = new HashMap();

    protected String prepareRequest(Hashtable req) throws Exception {
        if (req.size() == 0) {
            return "";
        }
        Enumeration e = req.keys();
        Object key = e.nextElement();
        StringBuffer buf = new StringBuffer();
        buf.append(key).append("=").append(URLEncoder.encode((String) req.get(key)));
        while (e.hasMoreElements()) {
            Object key2 = e.nextElement();
            buf.append("&").append(key2).append("=").append(URLEncoder.encode((String) req.get(key2)));
        }
        return buf.toString();
    }

    protected HttpResponse postForm(String data) throws Exception {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        URL post = new URL("https", "www.paypal.com", 443, "/cgi-bin/webscr");
        HttpURLConnection con = (HttpURLConnection) post.openConnection();
        con.setInstanceFollowRedirects(true);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        PrintWriter out = new PrintWriter(con.getOutputStream());
        out.println(data);
        out.close();
        StringBuffer buf = new StringBuffer();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            while (true) {
                String inputLine = in.readLine();
                if (inputLine == null) {
                    break;
                }
                buf.append(inputLine);
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        HttpResponse response = new HttpResponse(con.getResponseCode(), con.getResponseMessage(), buf.toString());
        return response;
    }

    protected Hashtable createParamsTable(HttpServletRequest request) throws Exception {
        Session.getLog().debug("in PayPalServlet.createParamsTable()");
        Enumeration e = request.getParameterNames();
        Hashtable ht = new Hashtable();
        while (e.hasMoreElements()) {
            String pname = (String) e.nextElement();
            String pvalue = request.getParameter(pname);
            ht.put(pname, pvalue);
        }
        ht.put("cmd", "_notify-validate");
        return ht;
    }

    protected HttpResponse validatePayPal(HttpServletRequest request) throws Exception {
        Hashtable ht = createParamsTable(request);
        String preparedData = prepareRequest(ht);
        return postForm(preparedData);
    }

    protected HttpResponse validateTest(HttpServletRequest request) throws Exception {
        Session.getLog().debug("in PayPalServlet.validateTest()");
        URL url = new URL(Session.getProperty("CP_PROTOCOL") + Session.getProperty("CP_HOST") + ":" + Session.getProperty("CP_PORT") + "/psoft/servlet/psoft.hsphere.payment.PayPalTest");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setDoOutput(true);
        Hashtable ht = createParamsTable(request);
        String protocol = Session.getProperty("CP_PROTOCOL");
        int index = protocol.indexOf("://");
        HttpResponse result = HttpUtils.postForm(protocol.substring(0, index), Session.getProperty("CP_HOST"), Integer.parseInt(Session.getProperty("CP_PORT")), "/psoft/servlet/psoft.hsphere.payment.PayPalTest", ht);
        con.disconnect();
        return result;
    }

    @Override // psoft.hsphere.payment.ExternalPayServlet
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        C0004CP.getCP().setConfig();
        Session.setLanguage(new Language(null));
        try {
            Session.setResellerId(1L);
        } catch (UnknownResellerException ex) {
            Session.getLog().error("PayPal. Error setting default reseller ID: ", ex);
        }
        Session.getLog().debug("in PayPalServlet.service()");
        Enumeration pNames = request.getParameterNames();
        if (!pNames.hasMoreElements()) {
            processEmptyResponse(request, response, "An incorrect request has been received.");
        } else if ("redirect".equals(request.getParameter("action"))) {
            Session.getLog().debug("PayPal request has been received: ");
            while (pNames.hasMoreElements()) {
                String pName = (String) pNames.nextElement();
                Session.getLog().debug("PayPal parameter: " + pName + " value: " + request.getParameter(pName));
            }
            getProcessForm(request, response);
        } else if ("ok".equals(request.getParameter("redirectresult"))) {
            processRedirect(request, response, "", true);
        } else if ("error".equals(request.getParameter("redirectresult"))) {
            processRedirect(request, response, "", false);
        } else {
            processPayment(request, response);
        }
    }

    protected void getProcessForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            try {
                response.setContentType("text/html");
                log.info("Begin process PayPal");
                Session.save();
                String cpUrl = request.getParameter("cp_url");
                if (cpUrl == null || "".equals(cpUrl)) {
                    throw new Exception("Empty cp_url parameter");
                }
                String trDesc = request.getParameter("trans_id");
                if (trDesc == null || "".equals(trDesc)) {
                    throw new Exception("Empty transaction ID");
                }
                String amount = request.getParameter("amount");
                if (amount == null || "".equals(amount)) {
                    throw new Exception("Empty amount for trans ID:" + trDesc);
                }
                String amount2 = formatAmount(amount);
                String description = request.getParameter("description");
                setResellerId(trDesc);
                HashMap values = MerchantManager.getProcessorSettings(GATEWAY);
                if (!"1".equals(values.get("enabled"))) {
                    throw new Exception("PayPal is not available");
                }
                Session.getLog().debug("Amount:" + amount2 + " Transaction Description:" + trDesc);
                Template template = Session.getTemplate("submit/billing/paypal.sbm");
                if (null == template) {
                    processEmptyResponse(request, response, "Template submit/billing/paypal.sbm is not exist");
                    try {
                        Session.restore();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                String enableservlet = Session.getPropertyString("ENABLE_PAYMENT_SERVLET");
                SimpleHash root = new SimpleHash();
                root.put("trDesc", new TemplateString(trDesc));
                root.put("values", new TemplateMap(values));
                root.put("amount", new TemplateString(amount2));
                root.put("servlet_url", new TemplateString(cpUrl + ExternalPayServlet.SERVLETPATH + values.get("servlet")));
                root.put("test_url", new TemplateString(cpUrl + ExternalPayServlet.SERVLETPATH + "PayPalTest"));
                root.put("description", new TemplateString(description));
                root.put("currency_code", new TemplateString(getInternationalCurrrencySymbol()));
                if (enableservlet != null && !"".equals(enableservlet)) {
                    root.put("enable_payment_servlet", enableservlet);
                }
                root.put("invoice", new TemplateString(String.valueOf(Session.getNewIdAsLong()) + "-" + trDesc));
                template.process(root, response.getWriter());
                try {
                    Session.restore();
                } catch (Exception e2) {
                }
            } catch (Exception e3) {
                log.warn("Unable to process transaction", e3);
                Ticket.create(e3, null);
                processEmptyResponse(request, response, "The payment can't be processed: " + e3.getMessage());
                try {
                    Session.restore();
                } catch (Exception e4) {
                }
            }
        } catch (Throwable th) {
            try {
                Session.restore();
            } catch (Exception e5) {
            }
            throw th;
        }
    }

    protected void processPayment(HttpServletRequest request, HttpServletResponse response) {
        double amount;
        HttpResponse verifyResponse;
        String status = request.getParameter("payment_status");
        Session.getLog().info("PayPal. Process Payment. Status:  " + status);
        if (status != null && status.equals("Completed")) {
            String custom = request.getParameter("custom");
            String txnId = request.getParameter("txn_id");
            String invoice = request.getParameter("item_number");
            Session.getLog().info("PayPal. Parameters were received: custom:" + custom + " txnId:" + txnId + " invoice" + invoice);
            if (invoice == null || "".equals(invoice) || custom == null || "".equals(custom)) {
                Session.getLog().warn("Invoice parameter is incorrect. The payment can't be processed");
                return;
            }
            try {
                Session.save();
                setResellerId(invoice);
                if (custom != null && custom.equals(Settings.get().getValue("PayPal_CUSTOM"))) {
                    String mcGross = request.getParameter("mc_gross");
                    String mcCurrency = request.getParameter("mc_currency");
                    String sAmount = request.getParameter("settle_amount");
                    DecimalFormatSymbols dfs = new DecimalFormatSymbols(Session.getCurrentLocale());
                    String resCurSymbol = dfs.getInternationalCurrencySymbol();
                    if (mcCurrency != null) {
                        if (mcCurrency.equals(resCurSymbol)) {
                            amount = Double.parseDouble(mcGross);
                        } else if (sAmount != null) {
                            try {
                                amount = Double.parseDouble(sAmount);
                            } catch (Exception ex) {
                                Session.getLog().error(ex, (Throwable) null);
                                amount = Double.parseDouble(mcGross);
                            }
                        } else {
                            amount = Double.parseDouble(mcGross);
                        }
                    } else {
                        amount = Double.parseDouble(mcGross);
                    }
                    new String();
                    if (!Session.getPropertyString("ENABLE_PAYMENT_SERVLET").equals("1")) {
                        verifyResponse = validatePayPal(request);
                    } else {
                        verifyResponse = validateTest(request);
                    }
                    String vResBody = verifyResponse.getBody();
                    vResBody.trim();
                    Session.getLog().debug("Response from PayPal (Verification) is: " + vResBody);
                    if (vResBody.indexOf("VERIFIED") != -1 && isTxnIdUnique(txnId)) {
                        synchronized (txnIds) {
                            if (txnIds.containsKey(txnId)) {
                                Session.getLog().debug("Transaction ID isn't unique");
                                return;
                            }
                            txnIds.put(txnId, txnId);
                            setPayment(amount, txnId, invoice, GATEWAY);
                            txnIds.remove(txnId);
                        }
                    } else {
                        Session.getLog().debug("Payment is not verified or is doubled.");
                    }
                    if (Session.getPropertyString("ENABLE_PAYMENT_SERVLET").equals("1")) {
                        String next_url = request.getParameter("return");
                        response.setContentType("text/html");
                        PrintWriter out = new PrintWriter(response.getWriter());
                        out.println("<html><body>");
                        out.print("<br><a href=\"" + next_url + "\"><b>Here you can return to H-Sphere</b></a>");
                        out.println("</body></html>");
                        out.close();
                    }
                }
                Session.restore();
                return;
            } catch (Exception e) {
                log.warn("Unable to process transaction", e);
                Ticket.create(e, null);
                return;
            }
        }
        String pending_reason = request.getParameter("pending_reason");
        Session.getLog().info("PayPal. Analyze pending reason - " + pending_reason);
        try {
            analyzePending(pending_reason);
        } catch (Exception e2) {
            log.warn("Unable to analyze pending reason for PayPal Payment", e2);
        }
    }

    private void analyzePending(String pending_reason) throws Exception {
        String message;
        if ("echeck".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_echeck");
        } else if ("multi_currency".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_multi_currency");
        } else if ("intl".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_intl");
        } else if ("verify".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_verify");
        } else if ("address".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_address");
        } else if ("upgrade".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_upgrade");
        } else if ("unilateral".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_unilateral");
        } else if ("other".equals(pending_reason)) {
            message = Localizer.translateMessage("paypal.pending_other");
        } else {
            message = Localizer.translateMessage("paypal.pending_unknown");
        }
        String title = Localizer.translateMessage("paypal.pending_title");
        Ticket.create(title, 75, message, null, 1, 1, 0, 0, 0, 0);
    }

    private String getInternationalCurrrencySymbol() throws Exception {
        String result;
        String adm_curcode = Settings.get().getValue("curcode");
        if (adm_curcode != null) {
            return adm_curcode;
        }
        String locale = Settings.get().getValue(AccountPreferences.LANGUAGE);
        if (locale != null) {
            String[] tmpLocale = Localizer.getArrayLocale(locale);
            result = new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1])).getInternationalCurrencySymbol();
        } else {
            result = new DecimalFormatSymbols().getInternationalCurrencySymbol();
        }
        return result;
    }
}
