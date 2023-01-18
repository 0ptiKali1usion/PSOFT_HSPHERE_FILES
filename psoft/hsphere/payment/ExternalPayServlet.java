package psoft.hsphere.payment;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.Locale;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ExternalPayServlet.class */
public abstract class ExternalPayServlet extends HttpServlet {
    protected static Category log = Category.getInstance(PayPalServlet.class.getName());
    protected static final String PAYPAL_NOTIFY_URL = "https://www.paypal.com/cgi-bin/websrc";
    public static final String SERVLETPATH = "psoft.hsphere.payment.";
    protected static final int PREPAREANDSET = 0;
    protected static final int PREPARE = 1;
    protected static final int SET = 2;

    public abstract void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException;

    protected long getRecurrentAccId(String invoice) throws Exception {
        String fullAccId = invoice.substring(4);
        int acc_suff = fullAccId.indexOf("_0");
        String accId = fullAccId.substring(0, acc_suff);
        return Long.parseLong(accId);
    }

    protected int getSignupAccId(String invoice) throws Exception {
        String accId = invoice.substring(4);
        int signup_id = 0;
        Connection con_db = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con_db.prepareStatement("SELECT id FROM request WHERE name = 'signup_id' and value= ?");
            ps.setString(1, accId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                signup_id = rs.getInt("id");
            }
            Session.closeStatement(ps);
            con_db.close();
            return signup_id;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con_db.close();
            throw th;
        }
    }

    protected int getMailSignupAccId(String invoice) throws Exception {
        return Integer.parseInt(invoice.substring(5));
    }

    protected void setResellerBySignup(int signup_id) {
        try {
            Connection con_db = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con_db.prepareStatement("select reseller_id from users where id = (select user_id from request_record where request_id = ?)");
                ps.setInt(1, signup_id);
                ResultSet rs = ps.executeQuery();
                int sig_res_id = 0;
                while (rs.next()) {
                    sig_res_id = rs.getInt("reseller_id");
                }
                Session.setResellerId(sig_res_id);
                Session.closeStatement(ps);
                con_db.close();
            } catch (Exception ex) {
                Session.getLog().error("Problem setting reseller ID", ex);
                Session.closeStatement(ps);
                con_db.close();
            }
        } catch (Exception ex2) {
            Session.getLog().error("Can`t get id from request", ex2);
        }
    }

    public void setResellerId(String invoice) throws Exception {
        Session.getLog().debug("Invoice name = " + invoice);
        if (invoice.startsWith("acc-")) {
            Account a = (Account) Account.get(new ResourceId(getRecurrentAccId(invoice), 0));
            Session.getLog().debug("setResellerId : " + a.getResellerId());
            Session.setResellerId(a.getResellerId());
        } else if (invoice.startsWith("sig-")) {
            int signup_id = getSignupAccId(invoice);
            setResellerBySignup(signup_id);
        } else if (invoice.startsWith("mail-")) {
            int signup_id2 = getMailSignupAccId(invoice);
            setResellerBySignup(signup_id2);
        }
    }

    public String paymentPrepare(double amount, String invoice) throws Exception {
        long reqId;
        if (invoice.startsWith("acc-")) {
            String fullAccId = invoice.substring(4);
            int acc_suff = fullAccId.indexOf("_0");
            String accId = fullAccId.substring(0, acc_suff);
            reqId = ExternalPaymentManager.requestAccountPayment(amount, Long.parseLong(accId));
        } else {
            int signup_id = 0;
            if (invoice.startsWith("sig-")) {
                signup_id = getSignupAccId(invoice);
            } else if (invoice.startsWith("mail-")) {
                signup_id = getMailSignupAccId(invoice);
            }
            Session.getLog().debug("Signup account id ---> " + new Integer(signup_id));
            ExternalPayment ep = ExternalPayment.getPaymentBySignup(signup_id);
            reqId = ep.getId();
        }
        return Long.toString(reqId);
    }

    public void paymentSet(double amount, String txnId, String paymentName) throws Exception {
        int index = txnId.indexOf("-");
        if (index > 0) {
            long reqId = Long.parseLong(txnId.substring(0, index));
            ExternalPaymentManager.confirmPayment(amount, reqId, paymentName, txnId);
            return;
        }
        throw new Exception("Unnable to get transaction ID from response");
    }

    public void setPayment(double amount, String txnId, String invoice, String paymentName) throws Exception {
        long reqId;
        if (invoice.startsWith("acc-")) {
            String fullAccId = invoice.substring(4);
            int acc_suff = fullAccId.indexOf("_0");
            String accId = fullAccId.substring(0, acc_suff);
            reqId = ExternalPaymentManager.requestAccountPayment(amount, Long.parseLong(accId));
        } else {
            int signup_id = 0;
            if (invoice.startsWith("sig-")) {
                signup_id = getSignupAccId(invoice);
            } else if (invoice.startsWith("mail-")) {
                signup_id = getMailSignupAccId(invoice);
            }
            Session.getLog().debug("Signup account id ---> " + new Integer(signup_id));
            ExternalPayment ep = ExternalPayment.getPaymentBySignup(signup_id);
            reqId = ep.getId();
        }
        ExternalPaymentManager.confirmPayment(amount, reqId, paymentName, txnId);
    }

    public void checkIP(String gatewayName, String addr) throws Exception {
        Session.getLog().debug("Remoute IP is: " + addr);
        String ips = Settings.get().getValue(gatewayName + "_IPs");
        Session.getLog().debug("Allowed IP list for reseler: " + Session.getResellerId() + ". Gateway:  " + gatewayName + " gateway is : " + ips);
        if (ips != null && !"".equals(ips)) {
            if (ips.indexOf(addr) < 0) {
                throw new Exception("Remote IP (" + addr + ") is not included into " + gatewayName + " allowed IP list.");
            }
            return;
        }
        throw new Exception("Allowed IP list for " + gatewayName + " gateway is empty");
    }

    public boolean isTxnIdUnique(String txnId) throws Exception {
        boolean result = false;
        Connection con_db = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con_db.prepareStatement("select id from extern_pm where ref_id = ?");
            ps.setString(1, txnId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                result = true;
            }
            Session.closeStatement(ps);
            con_db.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con_db.close();
            throw th;
        }
    }

    public void processEmptyResponse(HttpServletRequest request, HttpServletResponse response, String msg) throws IOException {
        response.setContentType("text/html");
        PrintWriter out1 = new PrintWriter(response.getWriter());
        out1.println("<html><body>");
        out1.println("The folowing error occured when the transaction had been processed: <br>");
        out1.println(msg);
        out1.println("</body></html>");
        out1.close();
    }

    public void processRedirect(HttpServletRequest request, HttpServletResponse response, String msg, boolean result) throws IOException {
        response.setContentType("text/html");
        PrintWriter out1 = new PrintWriter(response.getWriter());
        out1.println("<html><body>");
        String res = result ? "Your payment has been processed successfully" : "Your payment hasn't been processed";
        out1.println(res + "<br>");
        out1.println(msg);
        out1.println("</body></html>");
        out1.close();
    }

    public String formatAmount(String samount) throws Exception {
        NumberFormat engFormat = NumberFormat.getNumberInstance(new Locale("en", "US"));
        engFormat.setMinimumFractionDigits(2);
        engFormat.setMinimumIntegerDigits(1);
        engFormat.setMaximumFractionDigits(2);
        engFormat.setGroupingUsed(false);
        double amount = USFormat.parseDouble(samount);
        return engFormat.format(amount);
    }
}
