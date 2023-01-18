package psoft.hsphere.resource.epayment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import psoft.encryption.Crypt;
import psoft.epayment.MerchantGatewayLog;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.admin.CCEncryption;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/epayment/DbLog.class */
public class DbLog implements MerchantGatewayLog {
    @Override // psoft.epayment.MerchantGatewayLog
    public void transaction(int id, double amount, Date date, int type) {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO merchant_log (id, amount, created, type) VALUES (?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setDouble(2, amount);
            ps.setTimestamp(3, new Timestamp(date.getTime()));
            ps.setInt(4, type);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Exception e) {
            Session.getLog().warn("Store transaction in charge_log failed", e);
        }
    }

    @Override // psoft.epayment.MerchantGatewayLog
    public long write(long id, long accid, double amount, int trtype, String dataOut, String dataIn, String error, boolean success, int mgid) {
        boolean encryptionIsOn;
        PreparedStatement ps;
        long resId = id;
        int result = success ? 1 : 0;
        try {
            Connection con = Session.getDb();
            if (resId != 0) {
                PreparedStatement ps2 = con.prepareStatement("SELECT id FROM charge_log WHERE id=?");
                ps2.setLong(1, resId);
                ResultSet rs = ps2.executeQuery();
                if (!rs.next()) {
                    resId = 0;
                }
            }
            String encryptedMessOut = null;
            synchronized (CCEncryption.ENCRYPTION_LOCK) {
                encryptionIsOn = CCEncryption.get().isOn();
                if (encryptionIsOn) {
                    encryptedMessOut = Crypt.lencrypt(CCEncryption.get().getPublicEncryptionKey(), dataOut);
                }
            }
            if (resId != 0) {
                ps = con.prepareStatement("UPDATE charge_log SET account_id=?, amount=?, trtype=?, created=?, message_out=?,message_in=?,error_message=?, result=?, mgid=?, message_out_enc=? WHERE id=?");
                ps.setLong(1, accid);
                ps.setDouble(2, amount);
                ps.setInt(3, trtype);
                ps.setTimestamp(4, TimeUtils.getSQLTimestamp());
                if (encryptionIsOn) {
                    ps.setNull(5, 2005);
                    Session.setClobValue(ps, 10, encryptedMessOut);
                } else {
                    Session.setClobValue(ps, 5, dataOut);
                    ps.setNull(10, 2005);
                }
                Session.setClobValue(ps, 6, dataIn);
                Session.setClobValue(ps, 7, error);
                ps.setInt(8, result);
                ps.setInt(9, mgid);
                ps.setLong(11, resId);
            } else {
                ps = con.prepareStatement("INSERT INTO charge_log(id, account_id, amount, trtype, created, message_out,message_in,error_message, result, mgid, message_out_enc) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                resId = Resource.getNewId();
                ps.setLong(1, resId);
                ps.setLong(2, accid);
                ps.setDouble(3, amount);
                ps.setInt(4, trtype);
                ps.setTimestamp(5, TimeUtils.getSQLTimestamp());
                if (encryptionIsOn) {
                    ps.setNull(6, 2005);
                    Session.setClobValue(ps, 11, encryptedMessOut);
                } else {
                    Session.setClobValue(ps, 6, dataOut);
                    ps.setNull(11, 2005);
                }
                Session.setClobValue(ps, 7, dataIn);
                Session.setClobValue(ps, 8, error);
                ps.setInt(9, result);
                ps.setInt(10, mgid);
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable e) {
            Session.getLog().warn("Store transaction in charge_log failed", e);
        }
        return resId;
    }
}
