package psoft.hsphere.payment;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.payment.WebPayment;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ToCheckout.class */
public class ToCheckout extends WebPayment {
    private static final String AUTHNET_PAYMENT_URL = "https://www.2checkout.com/cgi-bin/Abuyers/purchase.2c";
    private static final String OLD_PAYMENT_URL = "https://www.2checkout.com/cgi-bin/buyers/cartpurchase.2c";
    private static final String NEW_PAYMENT_URL = "https://www2.2checkout.com/2co/buyer/purchase";

    @Override // psoft.hsphere.payment.WebPayment
    protected WebPayment.PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest request) throws Exception {
        String samount = request.getParameter("total");
        if (isEmpty(samount)) {
            samount = request.getParameter("x_amount");
        }
        String transid = request.getParameter("x_invoice_num");
        if (isEmpty(transid)) {
            transid = request.getParameter("cart_order_id");
        }
        return new WebPayment.PaymentRequestInfo(request, Double.parseDouble(samount), transid);
    }

    protected String getRequestId(HttpServletRequest request) {
        String requestId = request.getParameter("x_invoice_num");
        if (isEmpty(requestId)) {
            requestId = request.getParameter("cart_order_id");
        }
        return requestId;
    }

    protected double getRequestAmount(HttpServletRequest request) throws Exception {
        String samount = "";
        try {
            samount = request.getParameter("total");
            if (isEmpty(samount)) {
                samount = request.getParameter("x_amount");
            }
            double amount = Double.parseDouble(samount);
            return amount;
        } catch (Exception e) {
            throw new Exception("Unable to get amount from the request. Amount value: " + samount);
        }
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectUrl() throws Exception {
        String paymentUrl;
        int mId = Integer.parseInt(getValue("ID"));
        if (mId >= 200000 || "1".equals(getValue("v2_support"))) {
            paymentUrl = NEW_PAYMENT_URL;
        } else if ("1".equals(getValue("Use_Authorize"))) {
            paymentUrl = AUTHNET_PAYMENT_URL;
        } else {
            paymentUrl = OLD_PAYMENT_URL;
        }
        return paymentUrl;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectMethod() {
        return "POST";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected HashMap getRedirectValues(HttpServletRequest request) throws Exception {
        HashMap values = new HashMap();
        String amount = getFormatedAmount();
        if ("1".equals(getValue("Use_Authorize"))) {
            values.put("x_login", getValue("ID"));
            values.put("x_amount", amount);
            values.put("x_invoice_num", getInvoiceID());
            values.put("x_Receipt_Link_URL", getServletPath());
            values.put("x_Receipt_Link_Method", "POST");
            values.put("x_Receipt_Link_Text", "Continue");
            values.put("x_Show_Form", "PAYMENT_FORM");
        } else {
            values.put("sid", getValue("ID"));
            values.put("total", amount);
            values.put("cart_order_id", getInvoiceID());
            values.put("cart_id", getInvoiceID());
            values.put("c_prod", getInvoiceID());
        }
        return values;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected void validatePaymentRequest() throws Exception {
        String customerId = getPaymentRequestInfo().getRequestValue("x_login");
        if (isEmpty(customerId)) {
            customerId = getPaymentRequestInfo().getRequestValue("sid");
        }
        if (isEmpty(customerId)) {
            throw new Exception("Customer ID doesn't included into request");
        }
        if (!customerId.equals(getValue("ID"))) {
            throw new Exception("Incorrect customer ID is included into request");
        }
        checkIP(getPaymentRequestInfo().getRemoteAddr());
    }
}
