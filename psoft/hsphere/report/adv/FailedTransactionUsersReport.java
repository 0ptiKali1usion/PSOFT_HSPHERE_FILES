package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/FailedTransactionUsersReport.class */
public class FailedTransactionUsersReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f130df = new SimpleDateFormat("MM/dd/yyyy H:m:s");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        String emailSelect;
        String emailAnd;
        String emailLike;
        String emailFrom;
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String planId = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String emailType = (String) i.next();
        String suspended = (String) i.next();
        if ("bi".equals(emailType)) {
            emailSelect = "billing_info.email as email, ";
            emailAnd = "";
            emailLike = " AND billing_info.email LIKE ? ";
            emailFrom = "";
        } else {
            emailSelect = "contact_info.email as email, ";
            emailAnd = "AND contact_info.id = accounts.ci_id ";
            emailLike = " AND contact_info.email LIKE ? ";
            emailFrom = ", contact_info ";
        }
        StringBuffer query = new StringBuffer("SELECT users.username as username,accounts.id as accountId, accounts.created as created, accounts.suspended as suspended, accounts.plan_id as planId, plans.description as planDescription, accounts.description as accountDescription, accounts.p_end as pEnd, billing_info.type as billingType, billing_info.id as bid, credit_card.fatts as fatts, credit_card.last_fatt as last_fatt, " + emailSelect + "balance_credit.balance as balance, balance_credit.credit as credit FROM users, user_account, accounts, balance_credit, billing_info, plans, credit_card " + emailFrom + "WHERE users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id AND billing_info.id=credit_card.id AND credit_card.fatts > 0 " + emailAnd + "AND balance_credit.id=accounts.id AND plans.id = accounts.plan_id AND users.reseller_id=?");
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            query.append(" AND accounts.plan_id = ?");
        }
        if (!isEmpty("CC")) {
            query.append(" AND billing_info.type = ?");
        }
        if (!isEmpty(email)) {
            query.append(emailLike);
        }
        if (!isEmpty(username)) {
            query.append(" AND UPPER(users.username) LIKE ?");
        }
        if (!isEmpty(suspended)) {
            if ("active".equals(suspended)) {
                query.append(" AND accounts.suspended IS NULL ");
            } else {
                query.append(" AND accounts.suspended IS NOT NULL ");
            }
        }
        query.append(" ORDER BY accounts.id");
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
                if (!isEmpty("CC")) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, "CC");
                }
                if (!isEmpty(email)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, email);
                }
                if (!isEmpty(username)) {
                    int i5 = count;
                    int i6 = count + 1;
                    ps.setString(i5, username.toUpperCase());
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap.put("created", rs.getTimestamp("created"));
                    hashMap.put("suspended", rs.getTimestamp("suspended"));
                    hashMap.put("planId", new Integer(rs.getInt("planId")));
                    Plan p = Plan.getPlan(rs.getInt("planId"));
                    if (p != null) {
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString(3)) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString(3)));
                    } else {
                        hashMap.put("plan", "Unknown Plan");
                    }
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    hashMap.put("billingType", rs.getString("billingType"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("balance", new Double(rs.getDouble("balance")));
                    hashMap.put("credit", new Double(rs.getDouble("credit")));
                    hashMap.put("fatts", rs.getString("fatts"));
                    hashMap.put("last_fatt", f130df.format((Date) rs.getTimestamp("last_fatt")));
                    hashMap.put("bid", rs.getString("bid"));
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                setOrderParams("accountId", true);
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
            Session.getLog().error("Error getting UserSearchReport", se);
            throw se;
        }
    }
}
