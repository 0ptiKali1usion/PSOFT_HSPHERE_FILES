package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/TPReport.class */
public class TPReport extends AdvReport {

    /* renamed from: df */
    protected SimpleDateFormat f136df = null;

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List arguments) throws Exception {
        Session.getLog().debug("Inside TPReport::init()");
        int count = 1;
        Iterator i = arguments.iterator();
        String tpAccId = (String) i.next();
        String tpUsername = (String) i.next();
        String startedAfter = (String) i.next();
        String startedBefore = (String) i.next();
        String finishedAfter = (String) i.next();
        String finishedBefore = (String) i.next();
        String tpSource = (String) i.next();
        String tpTarget = (String) i.next();
        StringBuffer query = new StringBuffer("SELECT tp.id, tp.started, tp.finished, tp.suspended, tp.account_id, tp.src_server, tp.target_server, tp.stage, u.username FROM transfer_process tp, user_account ua, users u WHERE tp.account_id = ua.account_id AND ua.user_id = u.id");
        if (!isEmpty(tpAccId)) {
            query.append(" AND tp.account_id = ?");
        }
        if (!isEmpty(tpUsername)) {
            query.append(" AND u.username like ?");
        }
        if (!isEmpty(startedAfter)) {
            query.append(" AND tp.started >= ?");
        }
        if (!isEmpty(startedBefore)) {
            query.append(" AND tp.started <= ?");
        }
        if (!isEmpty(finishedAfter)) {
            query.append(" AND tp.finished >= ?");
        }
        if (!isEmpty(finishedBefore)) {
            query.append(" AND tp.finished <= ?");
        }
        if (!isEmpty(finishedAfter) || !isEmpty(finishedBefore)) {
            query.append(" AND tp.stage = 8");
        }
        if (!isEmpty(tpSource)) {
            query.append(" AND tp.src_server = ?");
        }
        if (!isEmpty(tpTarget)) {
            query.append(" AND tp.target_server = ?");
        }
        this.f136df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                ps = con.prepareStatement(query.toString());
                if (!isEmpty(tpAccId)) {
                    count = 1 + 1;
                    ps.setLong(1, Long.parseLong(tpAccId));
                }
                if (!isEmpty(tpUsername)) {
                    int i2 = count;
                    count++;
                    ps.setString(i2, tpUsername);
                }
                if (!isEmpty(startedAfter)) {
                    int i3 = count;
                    count++;
                    ps.setDate(i3, new Date(this.f136df.parse(startedAfter + " 00:00:00").getTime()));
                }
                if (!isEmpty(startedBefore)) {
                    int i4 = count;
                    count++;
                    ps.setTimestamp(i4, new Timestamp(this.f136df.parse(startedBefore + " 23:59:59").getTime()));
                }
                if (!isEmpty(finishedAfter)) {
                    int i5 = count;
                    count++;
                    ps.setDate(i5, new Date(this.f136df.parse(finishedAfter + " 00:00:00").getTime()));
                }
                if (!isEmpty(finishedBefore)) {
                    int i6 = count;
                    count++;
                    ps.setTimestamp(i6, new Timestamp(this.f136df.parse(finishedBefore + " 23:59:59").getTime()));
                }
                if (!isEmpty(tpSource)) {
                    int i7 = count;
                    count++;
                    ps.setLong(i7, Long.parseLong(tpSource));
                }
                if (!isEmpty(tpTarget)) {
                    int i8 = count;
                    int i9 = count + 1;
                    ps.setLong(i8, Long.parseLong(tpTarget));
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("id", new Long(rs.getLong("id")));
                    hashMap.put("started", rs.getTimestamp("started") == null ? Localizer.translateMessage("eeman.tp.notstarted") : this.f136df.format((java.util.Date) rs.getTimestamp("started")));
                    hashMap.put("finished", rs.getInt("stage") == 8 ? this.f136df.format((java.util.Date) rs.getTimestamp("finished")) : Localizer.translateMessage("eeman.tp.unfinished"));
                    hashMap.put("suspended", rs.getTimestamp("suspended") == null ? "N/A" : this.f136df.format((java.util.Date) rs.getTimestamp("suspended")));
                    hashMap.put("accountId", new Long(rs.getLong("account_id")));
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("srcServerId", new Long(rs.getLong("src_server")));
                    hashMap.put("targetServerId", new Long(rs.getLong("target_server")));
                    hashMap.put("stage", new Long(rs.getLong("stage")));
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error getting TrafficReport", ex);
                throw ex;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
