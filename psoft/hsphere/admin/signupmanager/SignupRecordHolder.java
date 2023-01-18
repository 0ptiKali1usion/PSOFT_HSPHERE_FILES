package psoft.hsphere.admin.signupmanager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Hashtable;
import java.util.Map;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.hsphere.cache.LockableCache;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/admin/signupmanager/SignupRecordHolder.class */
public class SignupRecordHolder {
    private static SignupRecordHolder ourInstance = new SignupRecordHolder();

    public static SignupRecordHolder getInstance() {
        return ourInstance;
    }

    public SignupRecord get(long signupRecordId) throws SQLException {
        return get(signupRecordId, false);
    }

    public SignupRecord get(long signupRecordId, boolean locked) throws SQLException {
        SignupRecord signupRecord = (SignupRecord) Session.getCacheFactory().getCache(SignupRecord.class).get(signupRecordId);
        if (signupRecord != null) {
            return signupRecord;
        }
        SignupRecord signupRecord2 = getSignupRecordFromDB(signupRecordId);
        if (signupRecord2 != null) {
            Session.getCacheFactory().getCache(SignupRecord.class).put(signupRecord2);
        }
        if (locked) {
            Session.getCacheFactory().getLockableCache(SignupRecord.class).lock(signupRecordId);
        }
        return signupRecord2;
    }

    public void save(SignupRecord sr) throws SQLException {
        try {
            if (!saveSignupRecord(sr)) {
                createSignupRecordInDB(sr);
            }
            saveValues(sr);
        } catch (SQLException ex) {
            Session.getLog().error("Unable to save Signup Record", ex);
            throw ex;
        }
    }

    public synchronized SignupRecord update(SignupRecord sr) throws SQLException {
        SignupRecord srTmp = get(sr.getId(), true);
        try {
            if (srTmp == null) {
                Session.getCacheFactory().getCache(SignupRecord.class).put(sr);
                save(sr);
                unlock(sr);
                return sr;
            }
            srTmp.setValues(sr.getValues());
            srTmp.setUpdated(TimeUtils.getDate());
            unlock(sr);
            return srTmp;
        } catch (Throwable th) {
            unlock(sr);
            throw th;
        }
    }

    public void unlock(SignupRecord sr) {
        LockableCache cache = Session.getCacheFactory().getLockableCache(SignupRecord.class);
        cache.unlock(sr.getId());
    }

    public void lock(SignupRecord sr) {
        LockableCache cache = Session.getCacheFactory().getLockableCache(SignupRecord.class);
        cache.lock(sr.getId());
    }

    protected SignupRecord getSignupRecordFromDB(long signupRecordId) throws SQLException {
        SignupRecord signupRecord = null;
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT signup_id, created, ip, updated, state, user_id, reseller_id, msg, account_id FROM signup_record WHERE signup_id = ?");
            ps.setLong(1, signupRecordId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                signupRecord = new SimpleSignupRecord(rs.getLong(1), rs.getLong(7));
                signupRecord.setCreated(rs.getTimestamp(2));
                signupRecord.setIp(rs.getString(3));
                signupRecord.setUpdated(rs.getTimestamp(4));
                signupRecord.setState(rs.getInt(5));
                signupRecord.setUserId(rs.getInt(6));
                signupRecord.setMsg(rs.getString(8));
                signupRecord.setCountryCode(SignupManager.getCountryCode(rs.getString(3)));
                signupRecord.setAccountId(rs.getLong(9));
                Map values = new Hashtable();
                PreparedStatement ps1 = con.prepareStatement("SELECT name, value FROM signup_values WHERE signup_id = ?");
                ps1.setLong(1, signupRecord.getId());
                ResultSet rs1 = ps1.executeQuery();
                while (rs1.next()) {
                    values.put(rs1.getString(1), rs1.getString(2));
                }
                signupRecord.setValues(values);
                Session.closeStatement(ps1);
            }
            Session.closeStatement(ps);
            con.close();
            return signupRecord;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void createSignupRecordInDB(SignupRecord sr) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO signup_record (signup_id, created, ip, updated, state, reseller_id, request_complete, user_id, account_id)  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, sr.getId());
            ps.setTimestamp(2, new Timestamp(sr.getCreated().getTime()));
            ps.setString(3, sr.getIp());
            ps.setTimestamp(4, new Timestamp(sr.getUpdated().getTime()));
            ps.setInt(5, sr.getState());
            ps.setLong(6, sr.getResellerId());
            ps.setInt(7, 0);
            ps.setLong(8, sr.getUserId());
            ps.setLong(9, sr.getAccountId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean saveSignupRecord(SignupRecord sr) throws SQLException {
        boolean updated = null;
        Connection con = Session.getDb();
        String msg = sr.getMsg();
        if (msg != null && msg.length() > 255) {
            msg = msg.substring(0, 255);
        }
        try {
            boolean updated2 = con.prepareStatement("UPDATE signup_record SET created = ?,  ip = ?, updated = ?, state = ?, reseller_id = ?,  user_id = ?, account_id = ?, msg = ? WHERE signup_id = ?");
            updated2.setTimestamp(1, new Timestamp(sr.getCreated().getTime()));
            updated2.setString(2, sr.getIp());
            updated2.setTimestamp(3, new Timestamp(sr.getUpdated().getTime()));
            updated2.setInt(4, sr.getState());
            updated2.setLong(5, sr.getResellerId());
            updated2.setLong(6, sr.getUserId());
            updated2.setLong(7, sr.getAccountId());
            updated2.setString(8, msg);
            updated2.setLong(9, sr.getId());
            return updated2.executeUpdate() > 0;
        } finally {
            Session.closeStatement(updated);
            con.close();
        }
    }

    protected void saveValues(SignupRecord sr) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM signup_values WHERE signup_id = ?");
            ps2.setLong(1, sr.getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO signup_values (signup_id, name, value) VALUES (?, ?, ?)");
            ps.setLong(1, sr.getId());
            for (String name : sr.getValues().keySet()) {
                ps.setString(2, name);
                String val = (String) sr.getValues().get(name);
                if (val == null) {
                    ps.setNull(3, 12);
                } else {
                    if ("_bi_cc_number".equals(name)) {
                        val = GenericCreditCard.getHiddenNumber(val);
                    } else if ("_bi_cc_cvv".equals(name)) {
                        val = GenericCreditCard.getHiddenCVV(val);
                    }
                    if (val.length() > 255) {
                        val = val.substring(0, 255);
                    }
                    ps.setString(3, val);
                }
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
