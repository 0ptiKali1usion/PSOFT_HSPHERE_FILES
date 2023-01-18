package psoft.hsphere.payment;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/payment/PaymentServlet.class */
public class PaymentServlet extends HttpServlet {
    public void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        C0004CP.getCP().setConfig();
        if (!Session.getPropertyString("ENABLE_PAYMENT_SERVLET").equals("1")) {
            return;
        }
        response.setContentType("text/html");
        PrintWriter out = new PrintWriter(response.getWriter());
        if (request.getParameter("id") == null || request.getParameter("amount") == null) {
            out.println("<html><body>Syntax: ?id=PAYMENT_ID&amount=AMOUNT&text=TEXT&ref=REFERECE</body></html>");
            return;
        }
        try {
            ExternalPaymentManager.confirmPayment(Double.parseDouble(request.getParameter("amount")), Long.parseLong(request.getParameter("id")), request.getParameter("text"), request.getParameter("ref"));
            out.println("<html><body>Success</body></html>");
        } catch (Throwable t) {
            out.println("<html><body>");
            t.printStackTrace(out);
            out.println("</body></html>");
        }
    }
}
