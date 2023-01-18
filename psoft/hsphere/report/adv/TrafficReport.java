package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Account;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/TrafficReport.class */
public class TrafficReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f137df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String planId = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String ttype = (String) i.next();
        StringBuffer query = new StringBuffer("SELECT accounts.id as accountId, SUM(trans_log.xfer) as xfer FROM accounts, trans_log WHERE trans_log.account_id = accounts.id AND accounts.reseller_id = ?");
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            query.append(" AND accounts.plan_id = ?");
        }
        if (!isEmpty(email) && isEmpty(accountId)) {
            query.append(" AND EXISTS (SELECT id FROM contact_info WHERE accounts.ci_id = contact_info.id AND email LIKE ?)");
        }
        if (!isEmpty(username) && isEmpty(accountId)) {
            query.append(" AND EXISTS (SELECT ua.account_id FROM users u, user_account ua WHERE accounts.id = ua.account_id AND u.id = ua.user_id AND UPPER(u.username) LIKE ?)");
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND trans_log.cdate >= ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND trans_log.cdate <= ?");
        }
        if (!isEmpty(ttype)) {
            query.append(" AND trans_log.tt_type = ?");
        }
        if (Session.useAccelerator() && Session.hasFlagField()) {
            query.append(" AND trans_log.flag != 0 ");
        }
        query.append(" GROUP BY accounts.id");
        query.append(" HAVING SUM(xfer) > 0");
        query.append(" ORDER BY xfer DESC");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query.toString());
                int count = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(accountId)) {
                    count++;
                    ps.setLong(count, Long.parseLong(accountId));
                }
                if (!isEmpty(planId)) {
                    int i2 = count;
                    count++;
                    ps.setInt(i2, Integer.parseInt(planId));
                }
                if (!isEmpty(email)) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, email);
                }
                if (!isEmpty(username)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, username.toUpperCase());
                }
                if (!isEmpty(createdAfter)) {
                    int i5 = count;
                    count++;
                    ps.setDate(i5, new Date(f137df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i6 = count;
                    count++;
                    ps.setDate(i6, new Date(f137df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(ttype)) {
                    int i7 = count;
                    int i8 = count + 1;
                    ps.setInt(i7, Integer.parseInt(ttype));
                }
                Session.getLog().debug("SQL:" + ps.toString());
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    long id = rs.getLong("accountId");
                    Account account = Account.getAccount(id);
                    User user = account.getUser();
                    if (user != null) {
                        hashMap.put("username", user.getLogin());
                    } else {
                        hashMap.put("username", Localizer.translateLabel("search.delusr"));
                    }
                    hashMap.put("accountId", new Long(id));
                    hashMap.put("plan", account.getPlan());
                    hashMap.put("email", account.getContactInfo().getEmail());
                    hashMap.put("xfer", new Double(rs.getDouble("xfer")));
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                setOrderParams("xfer", false);
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
                Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
            } catch (Throwable th) {
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se12) {
                        se12.printStackTrace();
                        throw th;
                    }
                }
                throw th;
            }
        } catch (SQLException se) {
            Session.getLog().error("Error getting TrafficReport", se);
            throw se;
        }
    }
}
