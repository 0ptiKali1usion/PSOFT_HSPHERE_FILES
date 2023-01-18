package psoft.hsphere.payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/payment/ExternalPayment.class */
public class ExternalPayment {
    private static Category log = Category.getInstance(ExternalPayment.class.getName());
    public static final int ACCOUNT = 1;
    public static final int SIGNUP = 2;
    public static final int PENDING = 0;
    public static final int PAID = 1;
    public static final int CANCELED = 2;

    /* renamed from: id */
    long f103id;
    long resellerId;
    double reqAmount;
    double recAmount;
    Timestamp created;
    long reqId;
    int reqType;
    int status;
    Timestamp statusDate;
    String text;
    String refId;

    public long getId() {
        return this.f103id;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public double getReqAmount() {
        return this.reqAmount;
    }

    public double getRecAmount() {
        return this.recAmount;
    }

    public Timestamp getCreated() {
        return this.created;
    }

    public long getRequestId() {
        return this.reqId;
    }

    public int getType() {
        return this.reqType;
    }

    public int getStatus() {
        return this.status;
    }

    public Timestamp getStatusDate() {
        return this.statusDate;
    }

    public String getText() {
        return this.text;
    }

    public String getRefId() {
        return this.refId;
    }

    public void addCharge(long accountId) throws Exception {
        Session.save();
        Account a = (Account) Account.get(new ResourceId(accountId, 0));
        User u = a.getUser();
        Session.setAccount(a);
        Session.setUser(u);
        Session.setResellerId(u.getResellerId());
        log.info("ADDING PAYMENT FOR ACCOUNT# " + accountId + " where status# " + getStatus());
        if (getStatus() == 1) {
            Account.addCharge(accountId, getRecAmount(), Localizer.translateMessage("billing.b_ext_charge", new Object[]{this.text}), TimeUtils.getSQLTimestamp());
        }
        Session.restore();
    }

    public void addCharge() throws Exception {
        addCharge(getRequestId());
    }

    protected static ExternalPayment getPayment(long reqId, int type) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, req_amount, created, rec_amount, req_id, req_type, status, status_date, descr, ref_id, reseller_id FROM extern_pm WHERE req_id = ? AND req_type = ? ORDER BY status_date");
            ps.setLong(1, reqId);
            ps.setInt(2, type);
            ResultSet rs = ps.executeQuery();
            long id = 0;
            double reqAmount = 0.0d;
            Timestamp created = null;
            double recAmount = 0.0d;
            long requestId = 0;
            int reqType = 0;
            int status = 0;
            Timestamp statusDate = null;
            String text = "";
            String refId = "";
            long resellerId = 0;
            int resultSize = 0;
            while (rs.next()) {
                id = rs.getLong(1);
                reqAmount = rs.getDouble(2);
                created = rs.getTimestamp(3);
                recAmount = rs.getDouble(4);
                requestId = rs.getLong(5);
                reqType = rs.getInt(6);
                status = rs.getInt(7);
                statusDate = rs.getTimestamp(8);
                text = rs.getString(9);
                refId = rs.getString(10);
                resellerId = rs.getLong(11);
                if (status == 1) {
                    ExternalPayment externalPayment = new ExternalPayment(id, reqAmount, created, recAmount, requestId, reqType, status, statusDate, text, refId, resellerId);
                    Session.closeStatement(ps);
                    con.close();
                    return externalPayment;
                }
                resultSize++;
            }
            if (resultSize > 0) {
                ExternalPayment externalPayment2 = new ExternalPayment(id, reqAmount, created, recAmount, requestId, reqType, status, statusDate, text, refId, resellerId);
                Session.closeStatement(ps);
                con.close();
                return externalPayment2;
            }
            throw new UnknownPaymentException(reqId, type);
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static ExternalPayment getPaymentBySignup(int requestId) throws Exception {
        return getPayment(requestId, 2);
    }

    public static ExternalPayment getPaymentByAccount(int requestId) throws Exception {
        return getPayment(requestId, 1);
    }

    public static ExternalPayment getPayment(long extId) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT req_amount, created, rec_amount, req_id, req_type, status, status_date, descr, ref_id, reseller_id FROM extern_pm WHERE id = ?");
            ps.setLong(1, extId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ExternalPayment externalPayment = new ExternalPayment(extId, rs.getDouble(1), rs.getTimestamp(2), rs.getDouble(3), rs.getLong(4), rs.getInt(5), rs.getInt(6), rs.getTimestamp(7), rs.getString(8), rs.getString(9), rs.getLong(10));
                Session.closeStatement(ps);
                con.close();
                return externalPayment;
            }
            throw new UnknownPaymentException(extId);
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public ExternalPayment(long id, double reqAmount, Timestamp created, double recAmount, long reqId, int reqType, int status, Timestamp statusDate, String text, String refId, long resellerId) {
        this.f103id = id;
        this.reqAmount = reqAmount;
        this.recAmount = recAmount;
        this.created = created;
        this.reqId = reqId;
        this.reqType = reqType;
        this.status = status;
        this.statusDate = statusDate;
        this.text = text;
        this.refId = refId;
        this.resellerId = resellerId;
    }
}
