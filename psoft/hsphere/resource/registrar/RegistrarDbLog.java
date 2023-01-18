package psoft.hsphere.resource.registrar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/RegistrarDbLog.class */
public class RegistrarDbLog implements RegistrarLog {
    @Override // psoft.hsphere.resource.registrar.RegistrarLog
    public long write(String domain, String tld, int period, int trtype, int registrarId, String signature) {
        long id = 0;
        int planId = 0;
        long accountId = 0;
        String username = "";
        try {
            long resellerId = Session.getResellerId();
            id = Session.getNewIdAsLong("registrar_log_seq");
            try {
                accountId = Session.getAccountId();
                planId = Session.getAccount().getPlan().getId();
                username = Session.getUser().getLogin();
            } catch (Exception e) {
            }
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO registrar_log (id, reseller_id, domain, registrar, signature, created, period, tt_type, account_id, username, planid) VALUES (?,?,?,?,?,?,?,?,?,?,?)");
            ps.setLong(1, id);
            ps.setLong(2, resellerId);
            ps.setString(3, domain + "." + tld);
            ps.setInt(4, registrarId);
            ps.setString(5, signature);
            ps.setTimestamp(6, TimeUtils.getSQLTimestamp());
            ps.setInt(7, period);
            ps.setInt(8, trtype);
            ps.setLong(9, accountId);
            ps.setString(10, username);
            ps.setLong(11, planId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable tr) {
            Session.getLog().error("Problem getting connection: ", tr);
        }
        return id;
    }

    @Override // psoft.hsphere.resource.registrar.RegistrarLog
    public void write(long id, RegistrarTransactionData data, boolean success) {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            int result = success ? 1 : 0;
            try {
                ps = con.prepareStatement("UPDATE registrar_log SET request = ?, response = ?, error_message = ?, result = ? WHERE id = ?");
                Session.setClobValue(ps, 1, data.getRequest());
                Session.setClobValue(ps, 2, data.getResponse());
                Session.setClobValue(ps, 3, data.getError());
                ps.setInt(4, result);
                ps.setLong(5, id);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Problem updating registrar_log", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable tr) {
            Session.getLog().error("Problem getting connection: ", tr);
        }
    }
}
