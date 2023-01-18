package psoft.hsphere.payment;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.payment.WebPayment;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PaySystems.class */
public class PaySystems extends WebPayment {
    private static final String PAYSYSTEMSURL = "https://secure.paysystems1.com/cgi-v310/payment/onlinesale-tpppro.asp";

    @Override // psoft.hsphere.payment.WebPayment
    protected WebPayment.PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest request) throws Exception {
        String orderid = request.getParameter("option1");
        String sAmount = request.getParameter("amount");
        return new WebPayment.PaymentRequestInfo(request, Double.parseDouble(sAmount), orderid);
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectUrl() {
        return PAYSYSTEMSURL;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectMethod() {
        return "POST";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected void validatePaymentRequest() throws Exception {
        checkIP(getPaymentRequestInfo().getRemoteAddr());
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected HashMap getRedirectValues(HttpServletRequest request) throws Exception {
        HashMap values = new HashMap();
        values.put("product1", getInvoiceID());
        values.put("total", getFormatedAmount());
        values.put("companyid", getValue("ID"));
        values.put("formget", "N");
        values.put("redirect", getServletPath());
        values.put("redirectfail", "http://www.paysystemsfail.com");
        values.put("delivery", "N");
        values.put("option1", getInvoiceID());
        return null;
    }
}
