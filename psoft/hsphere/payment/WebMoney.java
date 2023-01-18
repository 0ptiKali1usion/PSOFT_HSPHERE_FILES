package psoft.hsphere.payment;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import psoft.encryption.MD5;
import psoft.epayment.AuthorizeNetSIM;
import psoft.hsphere.Session;
import psoft.hsphere.payment.WebPayment;

/* loaded from: hsphere.zip:psoft/hsphere/payment/WebMoney.class */
public class WebMoney extends WebPayment {
    @Override // psoft.hsphere.payment.WebPayment
    protected WebPayment.PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest request) throws Exception {
        String sAmount = request.getParameter("LMI_PAYMENT_AMOUNT");
        String transId = request.getParameter("TRANSACTIONID");
        Session.getLog().debug("info: Amount: " + sAmount + " transId: " + transId);
        WebPayment.PaymentRequestInfo info = new WebPayment.PaymentRequestInfo(request, Double.parseDouble(sAmount), transId);
        return info;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectUrl() throws Exception {
        return "https://merchant.webmoney.ru/lmi/payment.asp";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectMethod() {
        return "POST";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected void validatePaymentRequest() throws Exception {
        WebPayment.PaymentRequestInfo info = getPaymentRequestInfo();
        String payeePurse = info.getRequestValue("LMI_PAYEE_PURSE");
        String paymentAmount = info.getRequestValue("LMI_PAYMENT_AMOUNT");
        String paymentNo = info.getRequestValue("LMI_PAYMENT_NO");
        String mode = info.getRequestValue("LMI_MODE");
        String sysInvsNo = info.getRequestValue("LMI_SYS_INVS_NO");
        String sysTransNo = info.getRequestValue("LMI_SYS_TRANS_NO");
        String sysTransDate = info.getRequestValue("LMI_SYS_TRANS_DATE");
        String secretKey = getValue("secretkey");
        String payerPurse = info.getRequestValue("LMI_PAYER_PURSE");
        String payerWM = info.getRequestValue("LMI_PAYER_WM");
        String md5Hash = info.getRequestValue("LMI_HASH");
        String data = payeePurse + paymentAmount + paymentNo + mode + sysInvsNo + sysTransNo + sysTransDate + secretKey + payerPurse + payerWM;
        MD5 md5 = new MD5();
        md5.Update(data.getBytes());
        String result = AuthorizeNetSIM.asHex(md5.Final()).toUpperCase();
        Session.getLog().debug(" result hash: " + result);
        Session.getLog().debug(" input  hash: " + md5Hash);
        if (!result.equals(md5Hash)) {
            throw new Exception("Payment request verification failed.");
        }
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected HashMap getRedirectValues(HttpServletRequest request) throws Exception {
        boolean testMode = "1".equals(getValue("testmode"));
        HashMap values = new HashMap();
        values.put("LMI_PAYEE_PURSE", getValue("wmid"));
        values.put("LMI_PAYMENT_AMOUNT", getFormatedAmount());
        values.put("LMI_PAYMENT_NO", Long.toString(getTransactionId()));
        values.put("LMI_PAYMENT_DESC", request.getParameter("description"));
        if (testMode) {
            values.put("LMI_SIM_MODE", "0");
        }
        values.put("LMI_RESULT_URL", getServletPath());
        values.put("TRANSACTIONID", getInvoiceID());
        values.put("LMI_SUCCESS_URL", getServletPath() + "?action=success");
        values.put("LMI_SUCCESS_METHOD", "2");
        values.put("LMI_FAIL_URL", getServletPath() + "?action=error");
        values.put("LMI_FAIL_METHOD", "2");
        return values;
    }
}
