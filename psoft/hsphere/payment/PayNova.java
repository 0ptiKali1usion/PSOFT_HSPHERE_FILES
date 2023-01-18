package psoft.hsphere.payment;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.encryption.MD5;
import psoft.epayment.ISOCodes;
import psoft.hsphere.Session;
import psoft.hsphere.payment.WebPayment;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PayNova.class */
public class PayNova extends WebPayment {
    private static final String TESTPOSTURL = "test.paynova.com";
    private static final String POSTURL = "paynova.com";

    @Override // psoft.hsphere.payment.WebPayment
    protected WebPayment.PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest request) throws Exception {
        String amount = request.getParameter("paymentdata");
        String transid = request.getParameter("orderid");
        WebPayment.PaymentRequestInfo info = new WebPayment.PaymentRequestInfo(request, Double.parseDouble(amount), transid);
        return info;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectUrl() {
        String server = POSTURL;
        if ("TRUE".equals(getValue("testmode"))) {
            server = TESTPOSTURL;
        }
        return "https://" + server + "/wallet/default.asp";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectMethod() {
        return "POST";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected void validatePaymentRequest() throws Exception {
        String data = getPaymentRequestInfo().getRequestValue("paymentstatus") + getPaymentRequestInfo().getRequestValue("orderid") + getPaymentRequestInfo().getRequestValue("paymentdata") + getPaymentRequestInfo().getRequestValue("transid");
        String chksum = calculateChkSum(data + getValue("secretkey"));
        if (!chksum.equals(getPaymentRequestInfo().getRequestValue("checksum"))) {
            throw new Exception("The response which was received from  paynova contains the incorrect check sum.");
        }
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected HashMap getRedirectValues(HttpServletRequest request) throws Exception {
        HashMap info = new HashMap();
        String sessionKey = getSessionKey(request);
        info.put("sessionkey", sessionKey);
        return info;
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

    protected String calculateChkSum(String data) throws Exception {
        MD5 md5 = new MD5();
        byte[] encoded = data.getBytes("ISO-8859-15");
        md5.Update(encoded);
        byte[] result = md5.Final();
        String res = asHex(result);
        return res;
    }

    private String getSessionKey(HttpServletRequest request) throws Exception {
        String message;
        String icpaccountid = getValue("icpaccountid");
        String currency = getValue("currency");
        double amount = getAmount();
        String sAmount = formatAmount(amount, currency);
        String notifypage = getServletPath();
        String secretkey = getValue("secretkey");
        String orderId = getInvoiceID();
        String description = request.getParameter("description");
        String dataToBeEncrypted = icpaccountid + sAmount + currency + notifypage + "" + orderId + getFormatedAmount() + description + secretkey;
        String res = calculateChkSum(dataToBeEncrypted);
        Hashtable reqdata = new Hashtable();
        reqdata.put("icpaccountid", getValue("icpaccountid"));
        reqdata.put("amount", sAmount);
        reqdata.put("currency", currency);
        reqdata.put("notifypage", notifypage);
        reqdata.put("redirecturlok", "");
        reqdata.put("redirecturlcancel", "");
        reqdata.put("orderid", orderId);
        reqdata.put("paymentdata", getFormatedAmount());
        reqdata.put("contracttext", description);
        reqdata.put("checksum", res);
        String server = POSTURL;
        if ("TRUE".equals(getValue("testmode"))) {
            server = TESTPOSTURL;
        }
        HttpResponse response = HttpUtils.postForm("https", server, 443, "/payment/startpayment.asp", reqdata);
        String body = response.getBody();
        StringTokenizer st = new StringTokenizer(body, "\n");
        String str = "";
        while (true) {
            message = str;
            if (!st.hasMoreTokens()) {
                break;
            }
            str = message + st.nextToken();
        }
        Session.getLog().debug("Response: " + message);
        if (message.length() != 37) {
            throw new Exception("Problem getting session key. An Incorect response has been received from server");
        }
        return message;
    }

    @Override // psoft.hsphere.payment.WebPayment
    public void processResponse(HttpServletResponse response, short transResult, String errorMessage) throws Exception {
        if (getRequestType() == 1) {
            super.processResponse(response, transResult, errorMessage);
            return;
        }
        response.setContentType("text/html");
        PrintWriter out1 = new PrintWriter(response.getWriter());
        out1.println(transResult == 0 ? "OK" : "ERROR");
        out1.close();
    }

    protected String formatAmount(double amount, String currency) {
        int precision = ISOCodes.getPrecisionByISOShortName(currency);
        return Long.toString(Math.round(amount * Math.pow(10.0d, precision)));
    }
}
