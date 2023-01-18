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
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/BalanceExhaustionDateReport.class */
public class BalanceExhaustionDateReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f121df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        String pType = null;
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String planId = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String str = (String) i.next();
        if (i.hasNext()) {
            pType = (String) i.next();
        }
        StringBuffer query = new StringBuffer("SELECT users.username as username, accounts.id as accountId,  accounts.suspended as suspended, accounts.plan_id as planId,  accounts.created as created,  accounts.period_id as periodId, accounts.description as accountDescription,  p.p_begin as p_begin,  contact_info.email as email, balance_credit.balance as balance, balance_credit.negative_date as negative_date, billing_info.type as billingType,  b.runout_date as runout_date  FROM users, user_account, accounts, contact_info,  balance_runout_date b, balance_credit, billing_info,  parent_child p  WHERE users.id = user_account.user_id  AND user_account.account_id = accounts.id  AND billing_info.id = accounts.bi_id  AND b.id = accounts.id AND balance_credit.id = accounts.id  AND p.child_id = accounts.id  AND accounts.ci_id = contact_info.id AND users.reseller_id = ?");
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            query.append(" AND accounts.plan_id = ?");
        }
        if (!isEmpty(email)) {
            query.append(" AND contact_info.email LIKE ?");
        }
        if (!isEmpty(username)) {
            query.append(" AND UPPER(users.username) LIKE ?");
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND b.runout_date >= ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND b.runout_date <= ?");
        }
        if (!isEmpty(pType)) {
            query.append(" AND billing_info.type = ?");
        }
        Session.getLog().debug("SQL:" + query.toString());
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
                    ps.setDate(i5, new Date(f121df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i6 = count;
                    count++;
                    ps.setDate(i6, new Date(f121df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(pType)) {
                    int i7 = count;
                    int i8 = count + 1;
                    ps.setString(i7, pType);
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap.put("suspended", rs.getTimestamp("suspended"));
                    Plan p = Plan.getPlan(rs.getInt("planId"));
                    if (p != null) {
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString("periodId")) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString("periodId")));
                        if (p.getBilling() == 0 && !FMACLManager.ADMIN.equals(p.getValue("_CREATED_BY_")) && p.getValue("_CREATED_BY_") != null) {
                            hashMap.put("without_bill", Localizer.translateMessage("advreport.withoutbilling"));
                        }
                        java.util.Date nextPaymentDate = p.getNextPaymentDate(rs.getDate("p_begin"), rs.getInt("periodId"));
                        hashMap.put("next_payment_date", nextPaymentDate);
                        hashMap.put("next_bill_date", rs.getDouble("balance") == 0.0d ? nextPaymentDate : rs.getDate("runout_date"));
                    } else {
                        hashMap.put("plan", "Unknown Plan");
                    }
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("balance", new Double(rs.getDouble("balance")));
                    hashMap.put("exhaustion_date", rs.getDate("runout_date"));
                    hashMap.put("created", rs.getDate("created"));
                    hashMap.put("billingType", rs.getString("billingType"));
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
                Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
            } catch (SQLException se) {
                Session.getLog().error("Error getting BalanceExhaustionDateReport", se);
                throw se;
            }
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
    }
}
