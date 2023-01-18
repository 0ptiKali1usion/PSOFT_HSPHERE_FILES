package psoft.hsphere.payment;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.StringTokenizer;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ToCheckOutServlet.class */
public class ToCheckOutServlet extends ExternalPayServlet {
    private static final String GATEWAY = "2CheckOut";
    private static final String AUTHNET_PAYMENT_URL = "https://www.2checkout.com/cgi-bin/Abuyers/purchase.2c";
    private static final String OLD_PAYMENT_URL = "https://www.2checkout.com/cgi-bin/buyers/cartpurchase.2c";
    private static final String NEW_PAYMENT_URL = "https://www2.2checkout.com/2co/buyer/purchase";

    @Override // psoft.hsphere.payment.ExternalPayServlet
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        C0004CP.getCP().setConfig();
        Session.setLanguage(new Language(null));
        try {
            Session.setResellerId(1L);
        } catch (UnknownResellerException ex) {
            Session.getLog().error("Error setting default reseller ID: ", ex);
        }
        String action = request.getParameter("action");
        if ("redirect".equals(action)) {
            getProcessForm(request, response);
        } else {
            processPayment(request, response);
        }
    }

    protected void getProcessForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            try {
                response.setContentType("text/html");
                log.info("Begin process ToCheckOut");
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
                String trId = paymentPrepare(Double.parseDouble(amount2), trDesc);
                String description = request.getParameter("description");
                setResellerId(trDesc);
                HashMap values = MerchantManager.getProcessorSettings(GATEWAY);
                if (!"1".equals(values.get("enabled"))) {
                    throw new Exception("ToCheckOut is not available");
                }
                int mId = Integer.parseInt((String) values.get("ID"));
                Session.getLog().debug("Amount:" + amount2 + " Transaction Description:" + trDesc);
                Template template = Session.getTemplate("submit/billing/2checkout.sbm");
                if (null == template) {
                    processEmptyResponse(request, response, "Template submit/billing/2checkout.sbm is not exist");
                    try {
                        Session.restore();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                String paymentUrl = (mId >= 200000 || "1".equals(values.get("v2_support"))) ? NEW_PAYMENT_URL : "1".equals((String) values.get("Use_Authorize")) ? AUTHNET_PAYMENT_URL : OLD_PAYMENT_URL;
                String isdemo = Session.getPropertyString("TO_CHECK_OUT_DEMO");
                String descr = trId + "-" + trDesc;
                SimpleHash root = new SimpleHash();
                root.put("paymenturl", new TemplateString(paymentUrl));
                root.put("trDesc", new TemplateString(descr));
                root.put("values", new TemplateMap(values));
                root.put("amount", new TemplateString(amount2));
                root.put("servlet_url", new TemplateString(cpUrl + ExternalPayServlet.SERVLETPATH + values.get("servlet")));
                root.put("description", new TemplateString(description));
                if (isdemo != null && !"".equals(isdemo)) {
                    root.put("is_demo", isdemo);
                }
                root.put("invoice", new TemplateString(descr));
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

    protected void processPayment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String invoice;
        String order_number;
        String txnId;
        int status;
        String demo = request.getParameter("demo");
        if (demo != null && "Y".equalsIgnoreCase(demo)) {
            Session.getLog().info("This is test transaction. Payment was cancelled");
            return;
        }
        String url = request.getRemoteHost();
        log.debug("Paymen request was received from the following adderss: " + url);
        String x_tmp = request.getParameter("x_invoice_num");
        if (x_tmp != null && !"".equals(x_tmp)) {
            invoice = x_tmp;
            order_number = request.getParameter("x_login");
            txnId = x_tmp;
        } else {
            invoice = request.getParameter("cart_order_id");
            order_number = request.getParameter("sid");
            txnId = invoice;
        }
        if (txnId == null || "".equals(txnId)) {
            processEmptyResponse(request, response, "The payment can't be processed. The payment request is invalid.");
            return;
        }
        String samount = request.getParameter("total");
        if (samount == null || "".equals(samount)) {
            samount = request.getParameter("x_amount");
        }
        double amount = Double.parseDouble(samount);
        StringTokenizer tokenizer = new StringTokenizer(invoice, "-");
        String tmp = tokenizer.nextToken().trim();
        String invoice2 = invoice.substring(tmp.length() + 1);
        try {
            Session.save();
            setResellerId(invoice2);
            Session.getLog().debug("invoice(cart_order_id) = " + invoice2);
            Session.getLog().debug("amount = " + String.valueOf(amount));
            Session.getLog().debug("txn id(cart_order_id) = " + txnId);
            Session.getLog().debug("ToCheckOut_ID = " + Settings.get().getValue("ToCheckOut_ID"));
            StringTokenizer tokenizer2 = new StringTokenizer(order_number, "-");
            String sid = tokenizer2.nextToken().trim();
            Session.getLog().debug("sid = " + sid);
            if (sid.equals(Settings.get().getValue("ToCheckOut_ID"))) {
                paymentSet(amount, txnId, GATEWAY);
            }
            if (invoice2.startsWith("sig")) {
                status = 1;
            } else if (invoice2.startsWith("mail")) {
                status = 2;
            } else if (invoice2.startsWith("acc")) {
                status = 3;
            } else {
                status = 0;
            }
            Session.restore();
        } catch (Exception e) {
            status = -1;
            log.warn("Unable to process transaction", e);
            Ticket.create(e, null);
        }
        printResultPage(response, status);
    }

    private void printResultPage(HttpServletResponse response, int status) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = new PrintWriter(response.getWriter());
        out.println("<html><body>");
        switch (status) {
            case -1:
                out.println("Unable to process transaction");
                break;
            case 0:
                out.println("Weird transaction");
                break;
            default:
                out.println("Transaction has been completed successfully.");
                break;
        }
        out.println("</body></html>");
        out.close();
    }
}
