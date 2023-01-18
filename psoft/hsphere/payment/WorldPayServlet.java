package psoft.hsphere.payment;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;
import java.util.HashMap;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Language;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/WorldPayServlet.class */
public class WorldPayServlet extends ExternalPayServlet {
    private static Category log = Category.getInstance(WorldPayServlet.class.getName());
    private static final String GATEWAY = "WorldPay";

    protected void emulateValidation(HttpServletRequest request, HttpServletResponse response) {
        Session.getLog().debug("Emulate validation from WorldPay server");
        try {
            response.setContentType("text/html");
            PrintWriter out = new PrintWriter(response.getWriter());
            out.print("VERIFIED");
            out.close();
        } catch (Exception e) {
            Session.getLog().error("Some errors occured", e);
        }
    }

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
                log.info("Begin process WorldPay");
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
                    throw new Exception("WorldPay is not available");
                }
                Session.getLog().debug("Amount:" + amount2 + " Transaction Description:" + trDesc);
                Template template = Session.getTemplate("submit/billing/worldpay.sbm");
                if (null == template) {
                    processEmptyResponse(request, response, "Template submit/billing/worldpay.sbm is not exist");
                    try {
                        Session.restore();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                SimpleHash root = new SimpleHash();
                root.put("cartId", new TemplateString(trDesc));
                root.put("values", new TemplateMap(values));
                root.put("amount", new TemplateString(amount2));
                root.put("ok_url", new TemplateString(cpUrl + ExternalPayServlet.SERVLETPATH + values.get("servlet")));
                root.put("description", new TemplateString(description));
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
        String installation = request.getParameter("installation");
        String invoice = request.getParameter("cartId");
        double amount = 0.0d;
        try {
            amount = Double.parseDouble(request.getParameter("amount"));
            Session.getLog().debug("amount = " + String.valueOf(amount));
        } catch (Exception e) {
            Session.getLog().debug("amount = " + String.valueOf(amount));
            Session.getLog().error("Failed to get amount from request", e);
        }
        String txnId = request.getParameter("transId");
        String transStatus = request.getParameter("transStatus");
        Session.getLog().debug("Transaction status = " + transStatus);
        String testMode = "";
        Enumeration en = request.getParameterNames();
        while (true) {
            if (!en.hasMoreElements()) {
                break;
            }
            String parameterName = (String) en.nextElement();
            if (parameterName.equalsIgnoreCase("testMode")) {
                testMode = request.getParameter(parameterName);
                break;
            }
        }
        Session.getLog().debug("Test Mode from request = |" + testMode + "|");
        try {
            Session.save();
            setResellerId(invoice);
            String test_mode = Settings.get().getValue("WorldPay_TEST_MODE");
            test_mode = (test_mode == null || "".equals(test_mode)) ? "0" : "0";
            int settingsTestMode = 0;
            try {
                settingsTestMode = Integer.parseInt(test_mode);
                Session.getLog().debug("settingsTestMode = " + String.valueOf(settingsTestMode));
            } catch (Exception e2) {
                Session.getLog().error("Failed to get Test Mode from settings", e2);
            }
            if (settingsTestMode == 0 && "100".equals(testMode)) {
                Session.getLog().info("This is test transaction. Payment was cancelled.");
            } else if (settingsTestMode == 101) {
                Session.getLog().info("This is test transaction. Payment was declined.");
            } else {
                if (installation.equals(Settings.get().getValue("WorldPay_INSTALLATION_ID")) && amount != 0.0d) {
                    if ("Y".equals(transStatus)) {
                        setPayment(amount, txnId, invoice, GATEWAY);
                    } else if ("C".equals(transStatus)) {
                        log.info("Transaction was cancelled");
                    } else {
                        Session.getLog().warn("Transaction was declined due to the invalid transaction status: " + transStatus);
                    }
                }
                Session.restore();
            }
        } catch (Exception e3) {
            log.warn("Unable to process transaction", e3);
            Ticket.create(e3, null);
        }
    }
}
