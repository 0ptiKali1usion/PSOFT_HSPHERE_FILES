package psoft.hsphere;

import com.psoft.ip2country.IP2Country;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Category;
import psoft.hsphere.admin.signupmanager.SignupRecord;
import psoft.hsphere.admin.signupmanager.SignupRecordHolder;
import psoft.hsphere.payment.ExternalPayment;
import psoft.hsphere.resource.moderate_signup.RequestRecord;
import psoft.util.FakeRequest;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/SignupManager.class */
public class SignupManager {
    private static Category log = Category.getInstance(SignupManager.class.getName());
    private static IP2Country ip2country;
    public static final long SIGNUP_LENGTH = 1200000;
    private static HashSet lokedRequestRecords;

    static {
        try {
            ip2country = new IP2Country(SignupManager.class.getResource("/psoft_config/ip.dat").getPath());
        } catch (Throwable t) {
            log.error("Problem getting base", t);
        }
        lokedRequestRecords = new HashSet();
    }

    public static String getCountryCode(String ip) {
        try {
            return ip2country.lookupCountryByIp(ip);
        } catch (Exception e) {
            return "";
        }
    }

    public static SignupRecord getSafeSignupRecord(long signupRecordId) throws SQLException, UnknownResellerException {
        SignupRecord sr = getRecord(signupRecordId);
        if (Session.getResellerId() == 1 || sr.getResellerId() == Session.getResellerId()) {
            return sr;
        }
        return null;
    }

    protected static SignupRecord getRecord(long signupRecordId) throws SQLException {
        return SignupRecordHolder.getInstance().get(signupRecordId);
    }

    public static SignupRecord getRecordBySignupId(long id) throws UnknownResellerException, SQLException {
        return getSafeSignupRecord(id);
    }

    public static String getValueByUserId(long userId, String name) throws SQLException, UnknownResellerException {
        SignupRecord sr = getRecordByUserId(userId);
        String value = (String) sr.getValues().get(name);
        if (value != null) {
            return value;
        }
        return "";
    }

    public static SignupRecord getRecordByUserId(long userId) throws SQLException, UnknownResellerException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT signup_id FROM signup_record WHERE user_id = ?");
            ps.setLong(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                SignupRecord safeSignupRecord = getSafeSignupRecord(rs.getLong(1));
                Session.closeStatement(ps);
                con.close();
                return safeSignupRecord;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /*  JADX ERROR: NullPointerException in pass: AttachTryCatchVisitor
        java.lang.NullPointerException: Cannot invoke "String.charAt(int)" because "obj" is null
        	at jadx.core.utils.Utils.cleanObjectName(Utils.java:35)
        	at jadx.core.dex.instructions.args.ArgType.object(ArgType.java:84)
        	at jadx.core.dex.info.ClassInfo.fromName(ClassInfo.java:41)
        	at jadx.core.dex.visitors.AttachTryCatchVisitor.convertToHandlers(AttachTryCatchVisitor.java:118)
        	at jadx.core.dex.visitors.AttachTryCatchVisitor.initTryCatches(AttachTryCatchVisitor.java:59)
        	at jadx.core.dex.visitors.AttachTryCatchVisitor.visit(AttachTryCatchVisitor.java:47)
        */
    public static long saveRequest(javax.servlet.http.HttpServletRequest r7) {
        /*
            Method dump skipped, instructions count: 301
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.SignupManager.saveRequest(javax.servlet.http.HttpServletRequest):long");
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    /* JADX WARN: Finally extract failed */
    public static long checkConcurrentSignup(HttpServletRequest request) throws SQLException, HSUserException {
        long signupId = getRequestSignupId(request);
        SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, true);
        if (sr != null) {
            try {
                synchronized (sr) {
                    switch (sr.getState()) {
                        case 0:
                        case 2:
                            sr.setState(3);
                            sr.setUpdated(TimeUtils.getDate());
                            SignupRecordHolder.getInstance().save(sr);
                            break;
                        case 1:
                            throw new HSUserException("signupresource.usercreationdone");
                        case 3:
                            throw new HSUserException("signupresource.userundercreation");
                    }
                }
                SignupRecordHolder.getInstance().unlock(sr);
            } catch (Throwable th) {
                SignupRecordHolder.getInstance().unlock(sr);
                throw th;
            }
        }
        return signupId;
    }

    public static int getRequestSignupId(HttpServletRequest request) {
        String tmpSignupId = request.getParameter("signup_id");
        int signupId = -1;
        if (tmpSignupId != null) {
            try {
                signupId = Integer.parseInt(tmpSignupId);
            } catch (Exception e) {
            }
        }
        return signupId;
    }

    public static void done(long signupId, long userId, long accountId) throws SQLException {
        SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, true);
        if (sr != null) {
            try {
                synchronized (sr) {
                    sr.setState(1);
                    sr.setAccountId(accountId);
                    sr.setUserId(userId);
                    SignupRecordHolder.getInstance().save(sr);
                }
                SignupRecordHolder.getInstance().unlock(sr);
            } catch (Throwable th) {
                SignupRecordHolder.getInstance().unlock(sr);
                throw th;
            }
        }
    }

    public static void doneAccount(long signupId, long accountId) throws SQLException {
        SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, true);
        if (sr != null) {
            try {
                synchronized (sr) {
                    sr.setState(1);
                    sr.setAccountId(accountId);
                    SignupRecordHolder.getInstance().save(sr);
                }
                SignupRecordHolder.getInstance().unlock(sr);
            } catch (Throwable th) {
                SignupRecordHolder.getInstance().unlock(sr);
                throw th;
            }
        }
    }

    public static void error(long signupId, String errorMessage) {
        try {
            SignupRecord sr = SignupRecordHolder.getInstance().get(signupId, true);
            if (sr != null) {
                synchronized (sr) {
                    sr.setState(2);
                    sr.setMsg(errorMessage);
                    SignupRecordHolder.getInstance().save(sr);
                }
                SignupRecordHolder.getInstance().unlock(sr);
            }
        } catch (SQLException se) {
            Session.getLog().warn("Error during error save", se);
        }
    }

    public static Account createAccount(int rid) throws Exception {
        int bp = 0;
        User oldUser = Session.getUser();
        Account oldAcc = Session.getAccount();
        RequestRecord record = new RequestRecord(rid);
        Session.getLog().debug("Got request record id = " + record.getId() + " isDeleted=" + record.isDeleted());
        if (lokedRequestRecords.contains(record) || record.isDeleted()) {
            throw new HSUserException("signupresource.request_locked");
        }
        Session.getLog().debug("Request record with id " + record.getId() + " locked isDeleted=" + record.isDeleted());
        lokedRequestRecords.add(record);
        FakeRequest newRequest = record.getFakeRequest();
        HttpServletRequest oldRequest = Session.getRequest();
        try {
            Session.setUser(record.getUser());
            Session.setRequest(newRequest);
            try {
                bp = Integer.parseInt(newRequest.getParameter("_bp"));
            } catch (NumberFormatException e) {
            }
            long signupId = getRequestSignupId(newRequest);
            Account a = record.getUser().addAccount(record.getPlanId(), record.getBillingInfo(), record.getContactInfo(), "Account #" + record.getPlanId(), newRequest.getParameter("_mod"), bp);
            doneAccount(signupId, a.getId().getId());
            try {
                int sellerId = Integer.parseInt(newRequest.getParameter("seller_id"));
                if (a != null && sellerId > 0) {
                    writeLog(sellerId, a.getId().getId());
                }
            } catch (Exception e2) {
            }
            record.dump();
            log.info("ADDING PAYMENT: " + record.getBillingInfo().getBillingType());
            if (record.getBillingInfo().getBillingType() == 3) {
                ExternalPayment pm = record.getPayment();
                log.info("ADDING charge: " + record.getBillingInfo().getBillingType());
                pm.addCharge(a.getId().getId());
            }
            return a;
        } finally {
            Session.setUser(oldUser);
            Session.setAccount(oldAcc);
            Session.setRequest(oldRequest);
            lokedRequestRecords.remove(record);
            Session.getLog().debug("Request record id=" + record.getId() + " unlocked");
        }
    }

    public static void writeLog(long seller_id, long client_id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO signup_log (id, sell_aid, client_aid, created) VALUES (?, ?, ?, ?)");
            ps.setLong(1, Session.getNewIdAsLong());
            ps.setLong(2, seller_id);
            ps.setLong(3, client_id);
            ps.setTimestamp(4, TimeUtils.getSQLTimestamp());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
