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
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.encryption.MD5;
import psoft.epayment.ISOCodes;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PayNovaServlet.class */
public class PayNovaServlet extends ExternalPayServlet {
    private static final String GATEWAY = "PayNova";
    private static final String TESTPOSTURL = "test.paynova.com";
    private static final String POSTURL = "paynova.com";

    @Override // psoft.hsphere.payment.ExternalPayServlet
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        C0004CP.getCP().setConfig();
        Session.setLanguage(new Language(null));
        Enumeration pNames = request.getParameterNames();
        if (!pNames.hasMoreElements()) {
            Session.getLog().debug("PayNova request has been received: ");
            while (pNames.hasMoreElements()) {
                String pName = (String) pNames.nextElement();
                Session.getLog().debug("PayNova parameter: " + pName + " value: " + request.getParameter(pName));
            }
        }
        if ("redirect".equals(request.getParameter("action"))) {
            getProcessForm(request, response);
        } else if ("1".equals(request.getParameter("paymentstatus"))) {
            processPayment(request, response);
        }
    }

    protected void getProcessForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            try {
                response.setContentType("text/html");
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
                    throw new Exception("ExampleWebPayment is not available");
                }
                String transKey = paymentPrepare(Double.parseDouble(amount2), trDesc);
                String sessionkey = obtainTransactionId(values, amount2, transKey + "-" + trDesc, description, cpUrl);
                Template template = Session.getTemplate("submit/billing/PayNova.sbm");
                if (null == template) {
                    processEmptyResponse(request, response, "Template submit/billing/PayNova.sbm is not exist");
                    try {
                        Session.restore();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                SimpleHash root = new SimpleHash();
                root.put("sessionkey", new TemplateString(sessionkey));
                root.put("values", new TemplateMap(values));
                String server = POSTURL;
                if ("TRUE".equals(values.get("testmode"))) {
                    server = TESTPOSTURL;
                }
                root.put("redirecturl", "https://" + server + "/wallet/default.asp");
                template.process(root, response.getWriter());
                try {
                    Session.restore();
                } catch (Exception e2) {
                }
            } catch (Exception e3) {
                log.warn("Unable to process transaction", e3);
                Ticket.create(e3, null);
                processEmptyResponse(request, response, "The payment can't be processed: " + e3);
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

    protected void processPayment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String chksum;
        try {
            String paymentstatus = request.getParameter("paymentstatus");
            String invoice = request.getParameter("orderid");
            String samount = request.getParameter("paymentdata");
            String transid = request.getParameter("transid");
            String checksum = request.getParameter("checksum");
            try {
                String data = paymentstatus + invoice + samount + transid;
                Session.save();
                StringTokenizer tokenizer = new StringTokenizer(invoice, "-");
                String transID = tokenizer.nextToken().trim();
                String accInfo = invoice.substring(transID.length() + 1);
                setResellerId(accInfo);
                HashMap values = MerchantManager.getProcessorSettings(GATEWAY);
                chksum = calculateChkSum(data + ((String) values.get("secretkey")));
            } catch (Exception e) {
                log.warn("Unable to add payment ", e);
                confirmStatus(request, response, (short) 0);
                Session.restore();
            }
            if (!chksum.equals(checksum)) {
                throw new Exception("The response which was received from  paynova contains the incorrect check sum.");
            }
            double amount = Double.parseDouble(samount);
            paymentSet(amount, invoice, GATEWAY);
            confirmStatus(request, response, (short) 1);
            Session.restore();
        } catch (Exception e2) {
            log.warn("Unable to process transaction", e2);
        }
    }

    private void confirmStatus(HttpServletRequest request, HttpServletResponse response, short success) {
        try {
            response.setContentType("text/html");
            PrintWriter out1 = new PrintWriter(response.getWriter());
            out1.println(success == 1 ? "OK" : "ERROR");
            out1.close();
        } catch (IOException ex) {
            Session.getLog().error("Unable to confirm payment.", ex);
        }
    }

    private String obtainTransactionId(HashMap values, String samount, String orderid, String description, String cpUrl) throws Exception {
        Security.addProvider(new Provider());
        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
        String icpaccountid = (String) values.get("icpaccountid");
        String currency = (String) values.get("currency");
        String amount = formatAmount(Double.parseDouble(samount), currency);
        String notifypage = cpUrl + ExternalPayServlet.SERVLETPATH + values.get("servlet");
        String secretkey = (String) values.get("secretkey");
        String dataToBeEncrypted = icpaccountid + amount + currency + notifypage + "" + orderid + samount + description + secretkey;
        String res = calculateChkSum(dataToBeEncrypted);
        Hashtable reqdata = new Hashtable();
        reqdata.put("icpaccountid", icpaccountid);
        reqdata.put("amount", amount);
        reqdata.put("currency", currency);
        reqdata.put("notifypage", notifypage);
        reqdata.put("redirecturlok", "");
        reqdata.put("redirecturlcancel", "");
        reqdata.put("orderid", orderid);
        reqdata.put("paymentdata", samount);
        reqdata.put("contracttext", description);
        reqdata.put("checksum", res);
        Enumeration e = reqdata.keys();
        Object key = e.nextElement();
        StringBuffer request = new StringBuffer();
        request.append(key).append("=").append(URLEncoder.encode((String) reqdata.get(key)));
        while (e.hasMoreElements()) {
            Object key2 = e.nextElement();
            request.append("&").append(key2).append("=").append(URLEncoder.encode((String) reqdata.get(key2)));
        }
        String server = POSTURL;
        if ("TRUE".equals(values.get("testmode"))) {
            server = TESTPOSTURL;
        }
        URL post = new URL("https", server, 443, "/payment/startpayment.asp");
        HttpURLConnection con = (HttpURLConnection) post.openConnection();
        con.setInstanceFollowRedirects(true);
        con.setRequestMethod("POST");
        con.setDoOutput(true);
        PrintWriter out = new PrintWriter(con.getOutputStream());
        out.println(request.toString());
        out.close();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuffer buf = new StringBuffer();
        while (true) {
            String inputLine = in.readLine();
            if (inputLine == null) {
                break;
            }
            buf.append(inputLine);
        }
        in.close();
        String response = buf.toString();
        Session.getLog().debug("PayNova server to server request result:" + response);
        if (response.length() != 37) {
            throw new Exception(response);
        }
        return response;
    }

    public static String asHex(byte[] hash) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            if ((hash[i] & 255) < 16) {
                buf.append("0");
            }
            buf.append(Long.toString(hash[i] & 255, 16));
        }
        return buf.toString();
    }

    protected String formatAmount(double amount, String currency) {
        int precision = ISOCodes.getPrecisionByISOShortName(currency);
        return Long.toString(Math.round(amount * Math.pow(10.0d, precision)));
    }

    protected String calculateChkSum(String data) throws Exception {
        MD5 md5 = new MD5();
        byte[] encoded = data.getBytes("ISO-8859-15");
        md5.Update(encoded);
        byte[] result = md5.Final();
        String res = asHex(result);
        return res;
    }
}
