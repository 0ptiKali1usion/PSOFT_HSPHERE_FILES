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
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ExampleWebPaymentServlet.class */
public class ExampleWebPaymentServlet extends ExternalPayServlet {
    private static final String GATEWAY = "ExampleWebPayment";

    @Override // psoft.hsphere.payment.ExternalPayServlet
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        C0004CP.getCP().setConfig();
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
                String description = request.getParameter("description");
                setResellerId(trDesc);
                HashMap values = MerchantManager.getProcessorSettings(GATEWAY);
                if (!"1".equals(values.get("enabled"))) {
                    throw new Exception("ExampleWebPayment is not available");
                }
                Template template = Session.getTemplate("submit/billing/example.sbm");
                if (null == template) {
                    processEmptyResponse(request, response, "Template submit/billing/example.sbm is not exist");
                    try {
                        Session.restore();
                        return;
                    } catch (Exception e) {
                        return;
                    }
                }
                SimpleHash root = new SimpleHash();
                root.put("trDesc", new TemplateString(trDesc));
                root.put("values", new TemplateMap(values));
                root.put("amount", new TemplateString(amount));
                root.put("servlet_url", new TemplateString(cpUrl + ExternalPayServlet.SERVLETPATH + values.get("servlet")));
                root.put("description", new TemplateString(description));
                root.put("invoice", new TemplateString(String.valueOf(Session.getNewIdAsLong()) + "-" + trDesc));
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
        String invoice = request.getParameter("parameter_name_in_the_payment_system_request");
        double amount = Double.parseDouble(request.getParameter("parameter_name_in_the_payment_system_request"));
        new StringTokenizer(invoice, "-");
        short success = 0;
        try {
            try {
                Session.save();
                StringTokenizer tokenizer = new StringTokenizer(invoice, "-");
                String transID = tokenizer.nextToken().trim();
                String accInfo = invoice.substring(transID.length() + 1);
                setResellerId(accInfo);
                setPayment(amount, transID, accInfo, GATEWAY);
                success = 1;
                Session.restore();
            } catch (Exception e) {
                log.warn("Unable to process transaction", e);
            }
        } catch (Exception e2) {
            log.warn("Unable to add payment to the ", e2);
            Session.restore();
        }
        printResultPage(response, success);
    }

    private void printResultPage(HttpServletResponse response, int status) throws IOException {
        response.setContentType("text/html");
        PrintWriter out = new PrintWriter(response.getWriter());
        out.println("<html><body>");
        switch (status) {
            case 0:
                out.println("Unable to process transaction");
                break;
            case 1:
                out.println("Transaction has been completed successfully.");
                break;
            default:
                out.println("Unable to process transaction");
                break;
        }
        out.println("</body></html>");
        out.close();
    }
}
