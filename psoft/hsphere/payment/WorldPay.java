package psoft.hsphere.payment;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.payment.WebPayment;

/* loaded from: hsphere.zip:psoft/hsphere/payment/WorldPay.class */
public class WorldPay extends WebPayment {
    @Override // psoft.hsphere.payment.WebPayment
    protected WebPayment.PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest request) throws Exception {
        String orderid = request.getParameter("cartId");
        String sAmount = request.getParameter("amount");
        return new WebPayment.PaymentRequestInfo(request, Double.parseDouble(sAmount), orderid);
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectUrl() {
        return "https://select.worldpay.com/wcc/purchase";
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
        HashMap info = new HashMap();
        info.put("instId", getValue("INSTALLATION_ID"));
        info.put("accId1", "ACCOUNT_ID");
        info.put("desc", request.getParameter("description"));
        info.put("cartId", getInvoiceID());
        info.put("testMode", getValue("TEST_MODE"));
        info.put("currency", getValue("CURRENCY"));
        info.put("MC_return", getServletPath());
        if ("1".equals(getValue("USE_CUSTOM_PAGE"))) {
            info.put("resultFile", getValue("CUSTOM_PAGE_NAME"));
        }
        info.put("amount", getFormatedAmount());
        return info;
    }
}
