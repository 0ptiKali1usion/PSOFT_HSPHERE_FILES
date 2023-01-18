package psoft.hsphere.payment;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PaySystemsServlet.class */
public class PaySystemsServlet extends ExternalPayServlet {
    private static Category log = Category.getInstance(PaySystemsServlet.class.getName());
    private static final String PAYSYSTEMSURL = "https://secure.paysystems1.com/cgi-v310/payment/onlinesale-tpppro.asp";
    private static final String SERVLETURL = "/psoft/servlet/psoft.hsphere.payment.PaySystemsServlet";

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
        String cc_status = request.getParameter("cc_status");
        if ("redirect".equals(action)) {
            getProcessForm(request, response);
        } else if ("pass".equals(cc_status)) {
            processPayment(request, response);
        } else {
            Session.getLog().debug("An incorrect request has been received.");
        }
    }

    protected void getProcessForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            try {
                C0004CP.getCP().setConfig();
                response.setContentType("text/html");
                log.warn("Begin process PaySystems");
                System.out.println("Begin process PaySystems");
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
                setResellerId(trDesc);
                String trId = paymentPrepare(Double.parseDouble(amount2), trDesc);
                String enable = Settings.get().getValue("PaySystems_enabled");
                if (!"1".equals(enable)) {
                    throw new Exception("PaySystems is not available");
                }
                String accountId = Settings.get().getValue("PaySystems_ID");
                Session.getLog().debug("Amount:" + amount2 + " Transaction Description:" + trDesc);
                Template template = Session.getTemplate("submit/billing/paysystems.sbm");
                if (null == template) {
                    processEmptyResponse(request, response, "Template submit/billing/paysystems.sbm is not exist");
                    try {
                        Session.restore();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                String descr = trId + "-" + trDesc;
                SimpleHash root = new SimpleHash();
                root.put("url", PAYSYSTEMSURL);
                root.put("ok_url", new TemplateString(cpUrl + SERVLETURL));
                root.put("fail_url", new TemplateString(cpUrl + SERVLETURL));
                root.put("accountID", new TemplateString(accountId));
                root.put("amount", new TemplateString(amount2));
                root.put("option1", new TemplateString(descr));
                root.put("descripton", new TemplateString(trDesc));
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

    protected void getOKForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void getFailForm(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    protected void processPayment(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String orderid = request.getParameter("orderid");
        String sAmount = request.getParameter("amount");
        String trId = request.getParameter("option1");
        try {
            try {
                try {
                    String ip = request.getRemoteAddr();
                    if (trId.indexOf("-") <= 0) {
                        throw new Exception(" Unable to parce result transId " + trId);
                    }
                    String accInfo = trId.substring(trId.indexOf("-") + 1);
                    setResellerId(accInfo);
                    double amount = USFormat.parseDouble(sAmount);
                    checkIP("PaySystems", ip);
                    Session.getLog().info("PaySystems. paymentSet. OrderId=" + orderid + " Amount=" + sAmount + " Info: " + trId);
                    paymentSet(amount, trId, "PaySystems");
                    processRedirect(request, response, "Your payment has been added. PaySystems TransactionId:" + orderid, true);
                    try {
                        Session.restore();
                    } catch (Exception e) {
                    }
                } catch (Exception e2) {
                    log.warn(" Error: ", e2);
                    Ticket.create(e2, null);
                    processEmptyResponse(request, response, e2.getMessage());
                    try {
                        Session.restore();
                    } catch (Exception e3) {
                    }
                }
            } catch (UnknownPaymentException ex) {
                log.warn(" Error: ", ex);
                Ticket.create(ex, null);
                processEmptyResponse(request, response, "Failed to add the payment. Probably, it has already been added.");
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
}
