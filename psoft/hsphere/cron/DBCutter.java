package psoft.hsphere.cron;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/cron/DBCutter.class */
public class DBCutter extends BackgroundJob {
    public DBCutter(C0004CP cp) throws Exception {
        super(cp, "DB_CUTTER");
    }

    public DBCutter(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        Calendar c = TimeUtils.getCalendar();
        c.add(2, -2);
        Date toMonthAgo = new Date(c.getTime().getTime());
        Date minUsageDate = getMinCdate(4003);
        Date minTransferDate = getMinCdate(121);
        Connection con = Session.isTransConnection() ? Session.getDb() : Session.getTransConnection();
        try {
            try {
                ps = con.prepareStatement("DELETE FROM usage_log WHERE cdate < ?");
                ps1 = con.prepareStatement("DELETE FROM trans_log WHERE cdate < ?");
                ps.setDate(1, toMonthAgo.before(minUsageDate) ? toMonthAgo : minUsageDate);
                ps1.setDate(1, toMonthAgo.before(minTransferDate) ? toMonthAgo : minTransferDate);
                ps.executeUpdate();
                ps1.executeUpdate();
                Session.commitTransConnection(con);
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
            } catch (Exception e) {
                con.rollback();
                Session.commitTransConnection(con);
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
            }
        } catch (Throwable th) {
            Session.commitTransConnection(con);
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            throw th;
        }
    }

    private Date getMinCdate(int typeId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT min(p_begin) FROM parent_child p, accounts a WHERE p.child_type = ? AND p.account_id = a.id AND a.deleted IS NULL and a.suspended IS NULL");
            ps.setInt(1, typeId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            Date res = rs.getDate(1);
            Session.closeStatement(ps);
            con.close();
            return res;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
