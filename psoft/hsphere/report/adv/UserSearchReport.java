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

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/UserSearchReport.class */
public class UserSearchReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f140df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    /* JADX WARN: Finally extract failed */
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
        String billingType = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
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
        StringBuffer query = new StringBuffer("SELECT users.username as username,accounts.id as accountId, accounts.created as created, accounts.suspended as suspended, accounts.plan_id as planId, plans.description as planDescription, accounts.description as accountDescription, accounts.p_end as pEnd, billing_info.type as billingType, " + emailSelect + "balance_credit.balance as balance, balance_credit.credit as credit FROM users, user_account, accounts, balance_credit, billing_info, plans " + emailFrom + "WHERE users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id " + emailAnd + "AND balance_credit.id=accounts.id AND plans.id = accounts.plan_id AND users.reseller_id=?");
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            if (!"resellers".equals(planId)) {
                query.append(" AND accounts.plan_id = ?");
            } else {
                query.append(" AND accounts.plan_id IN (select id from plans p where exists(select * from plan_resource pr where pr.plan_id=p.id and pr.type_id in (select id from type_name where name='reseller')))");
            }
        }
        if (!isEmpty(billingType)) {
            query.append(" AND billing_info.type = ?");
        }
        if (!isEmpty(email)) {
            query.append(emailLike);
        }
        if (!isEmpty(username)) {
            query.append(" AND UPPER(users.username) LIKE ?");
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND accounts.created > ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND accounts.created < ? ");
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
                if (!isEmpty(planId) && !"resellers".equals(planId)) {
                    int i2 = count;
                    count++;
                    ps.setInt(i2, Integer.parseInt(planId));
                }
                if (!isEmpty(billingType)) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, billingType);
                }
                if (!isEmpty(email)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, email);
                }
                if (!isEmpty(username)) {
                    int i5 = count;
                    count++;
                    ps.setString(i5, username.toUpperCase());
                }
                if (!isEmpty(createdAfter)) {
                    int i6 = count;
                    count++;
                    ps.setDate(i6, new Date(f140df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i7 = count;
                    int i8 = count + 1;
                    ps.setDate(i7, new Date(f140df.parse(createdBefore).getTime()));
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
                        if (p.isDemoPlan()) {
                            hashMap.put("demo", "1");
                        } else {
                            hashMap.put("demo", "0");
                        }
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString(3)) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString(3)));
                        hashMap.put("createdBy", p.getValue("_CREATED_BY_"));
                    } else {
                        hashMap.put("plan", Localizer.translateLabel("label.unknown_plan"));
                        hashMap.put("demo", "1");
                    }
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    rs.getString("billingType");
                    if (p != null && p.getBilling() == 0 && !"7".equals(p.getValue("_CREATED_BY_")) && p.getValue("_CREATED_BY_") != null) {
                        hashMap.put("without_bill", Localizer.translateMessage("advreport.withoutbilling"));
                    }
                    hashMap.put("billingType", rs.getString("billingType"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("balance", new Double(rs.getDouble("balance")));
                    hashMap.put("credit", new Double(rs.getDouble("credit")));
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
            } catch (SQLException se) {
                Session.getLog().error("Error getting UserSearchReport", se);
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
