package psoft.hsphere.payment;

import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.payment.WebPayment;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PayPal.class */
public class PayPal extends WebPayment {
    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectUrl() throws Exception {
        String server = getValue("SERVER");
        String port = getValue("PORT");
        String path = getValue("PATH");
        return "https://" + server + ":" + port + "/" + path;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected String getRedirectMethod() {
        return "POST";
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected void validatePaymentRequest() throws Exception {
        HashMap parameters = getPaymentRequestInfo().getRequestValues();
        parameters.put("cmd", "_notify-validate");
        HttpResponse response = HttpUtils.postForm("https", getValue("SERVER"), Integer.parseInt(getValue("PORT")), getValue("PATH"), parameters);
        String body = response.getBody().trim();
        if (body.indexOf("VERIFIED") == -1) {
            throw new Exception("The payment is not verified");
        }
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected HashMap getRedirectValues(HttpServletRequest request) throws Exception {
        HashMap result = new HashMap();
        String description = request.getParameter("description");
        result.put("cmd", "_xclick");
        result.put("business", getValue("ID"));
        result.put("receiver_id", getValue("ID"));
        result.put("item_name", description == null ? "" : description);
        result.put("item_number", getInvoiceID());
        result.put("image_url", getValue("image_url"));
        result.put("no_shipping", "1");
        result.put("return", getServletPath() + "?redirectresult=ok");
        result.put("cancel_return", getServletPath() + "?redirectresult=error");
        result.put("no_note", "1");
        result.put("custom", getValue("CUSTOM"));
        result.put("invoice", getInvoiceID());
        result.put("currency_code", getInternationalCurrrencySymbol());
        result.put("amount", getFormatedAmount());
        return result;
    }

    @Override // psoft.hsphere.payment.WebPayment
    protected WebPayment.PaymentRequestInfo initPaymentRequestInfo(HttpServletRequest request) throws Exception {
        String amount;
        String status = request.getParameter("payment_status");
        if (status != null && status.equals("Completed")) {
            String custom = request.getParameter("custom");
            String invoice = request.getParameter("item_number");
            if (invoice == null || "".equals(invoice) || custom == null || "".equals(custom)) {
                throw new Exception("Invoice parameter is incorrect. The payment can't be processed");
            }
            String mcGross = request.getParameter("mc_gross");
            String mcCurrency = request.getParameter("mc_currency");
            String sAmount = request.getParameter("settle_amount");
            DecimalFormatSymbols dfs = new DecimalFormatSymbols(Session.getCurrentLocale());
            String resCurSymbol = dfs.getInternationalCurrencySymbol();
            if (!isEmpty(mcCurrency)) {
                if (mcCurrency.equals(resCurSymbol)) {
                    amount = mcGross;
                } else if (!isEmpty(sAmount)) {
                    amount = sAmount;
                } else {
                    amount = mcGross;
                }
            } else {
                amount = mcGross;
            }
            return new WebPayment.PaymentRequestInfo(request, Double.parseDouble(amount), invoice);
        }
        return null;
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
