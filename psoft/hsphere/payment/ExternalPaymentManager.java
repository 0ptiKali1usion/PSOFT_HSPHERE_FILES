package psoft.hsphere.payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import psoft.hsphere.Account;
import psoft.hsphere.Language;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SignupGuard;
import psoft.hsphere.SignupManager;
import psoft.hsphere.User;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ExternalPaymentManager.class */
public class ExternalPaymentManager {
    public static final Object PAYMENT_REGISTRATION = new Object();

    public static void cancelPayment(long extId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE extern_pm SET status = ? WHERE id = ?");
            ps.setInt(1, 2);
            ps.setLong(2, extId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void confirmPayment(double amount, long extId, String text, String refId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Timestamp now = TimeUtils.getSQLTimestamp();
        try {
            synchronized (PAYMENT_REGISTRATION) {
                PreparedStatement ps2 = con.prepareStatement("SELECT status FROM extern_pm WHERE id = ?");
                ps2.setLong(1, extId);
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) {
                    int status = rs.getInt(1);
                    if (status == 1) {
                        throw new DuplicatePaymentException(extId);
                    }
                } else {
                    throw new UnknownPaymentException(extId);
                }
            }
            ps = con.prepareStatement("UPDATE extern_pm SET rec_amount = ?, status = ?, status_date = ?, descr = ?, ref_id = ? WHERE id = ? and status=?");
            ps.setDouble(1, amount);
            ps.setInt(2, 1);
            ps.setTimestamp(3, now);
            if (text == null) {
                ps.setNull(4, 12);
            } else {
                ps.setString(4, text);
            }
            if (refId == null) {
                ps.setNull(5, 12);
            } else {
                ps.setString(5, refId);
            }
            ps.setLong(6, extId);
            ps.setInt(7, 0);
            if (ps.executeUpdate() == 0) {
                throw new UnknownPaymentException(extId);
            }
            ExternalPayment ep = ExternalPayment.getPayment(extId);
            if (ep.getType() == 1) {
                chargeAccount(ep);
            } else if (ep.getType() == 2 && ep.getRecAmount() >= ep.getReqAmount() && SignupGuard.getWarnings((int) ep.getRequestId()) == null) {
                Session.getLog().debug("Request id ---> " + ep.getRequestId());
                SignupManager.createAccount((int) ep.getRequestId());
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected static void chargeAccount(ExternalPayment ep) throws Exception {
        Session.save();
        Account a = (Account) Account.get(new ResourceId(ep.getRequestId(), 0));
        User u = a.getUser();
        Session.setAccount(a);
        Session.setUser(u);
        Session.setResellerId(u.getResellerId());
        Session.setLanguage(new Language(null));
        ep.addCharge();
        Session.restore();
    }

    public static long requestSignupPayment(double amount, int reqId) throws Exception {
        return requestPayment(amount, reqId, 2);
    }

    public static long requestAccountPayment(double amount, long reqId) throws Exception {
        return requestPayment(amount, reqId, 1);
    }

    public static long requestPayment(double amount, long reqId, int type) throws Exception {
        return requestPayment(amount, reqId, type, Session.getResellerId());
    }

    public static long requestPayment(double amount, long reqId, int type, long resellerId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO extern_pm (id, req_amount, created, req_id, req_type, status, reseller_id) VALUES (?, ?, ?, ?, ?, ?, ?)");
            long id = Session.getNewId("extern_pm_seq");
            ps.setLong(1, id);
            ps.setDouble(2, amount);
            ps.setTimestamp(3, TimeUtils.getSQLTimestamp());
            ps.setLong(4, reqId);
            ps.setInt(5, type);
            ps.setInt(6, 0);
            ps.setLong(7, resellerId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return id;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
