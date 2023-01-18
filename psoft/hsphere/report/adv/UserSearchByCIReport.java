package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/UserSearchByCIReport.class */
public class UserSearchByCIReport extends AdvReport {
    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String firstName = (String) i.next();
        String lastName = (String) i.next();
        String company = (String) i.next();
        String city = (String) i.next();
        String state = (String) i.next();
        String postalCode = (String) i.next();
        String country = (String) i.next();
        StringBuffer query = new StringBuffer("SELECT users.username as username,accounts.id as accountId, accounts.created as created, accounts.suspended as suspended, accounts.plan_id as planId, plans.description as planDescription, accounts.description as accountDescription, accounts.p_end as pEnd, billing_info.type as billingType, billing_info.email as email, balance_credit.balance as balance, balance_credit.credit as credit FROM users, user_account, accounts, balance_credit, billing_info, plans , contact_info ci WHERE users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id AND balance_credit.id=accounts.id AND plans.id = accounts.plan_id AND users.reseller_id = ? AND ci.id = accounts.ci_id ");
        if (!isEmpty(firstName)) {
            query.append(" AND UPPER(ci.name) LIKE ?");
        }
        if (!isEmpty(lastName)) {
            query.append(" AND UPPER(ci.last_name) LIKE ?");
        }
        if (!isEmpty(company)) {
            query.append(" AND UPPER(ci.company) LIKE ?");
        }
        if (!isEmpty(city)) {
            query.append(" AND UPPER(ci.city) LIKE ?");
        }
        if (!isEmpty(state)) {
            query.append(" AND ci.state LIKE ?");
        }
        if (!isEmpty(postalCode)) {
            query.append(" AND ci.postal_code LIKE ?");
        }
        if (!isEmpty(country)) {
            query.append(" AND ci.country LIKE ?");
        }
        query.append(" ORDER BY accounts.id");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query.toString());
                int count = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(firstName)) {
                    count++;
                    ps.setString(count, firstName.toUpperCase());
                }
                if (!isEmpty(lastName)) {
                    int i2 = count;
                    count++;
                    ps.setString(i2, lastName.toUpperCase());
                }
                if (!isEmpty(company)) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, company.toUpperCase());
                }
                if (!isEmpty(city)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, city.toUpperCase());
                }
                if (!isEmpty(state)) {
                    int i5 = count;
                    count++;
                    ps.setString(i5, state);
                }
                if (!isEmpty(postalCode)) {
                    int i6 = count;
                    count++;
                    ps.setString(i6, postalCode);
                }
                if (!isEmpty(country)) {
                    int i7 = count;
                    int i8 = count + 1;
                    ps.setString(i7, country);
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
                        hashMap.put("createdBy", p.getValue("_CREATED_BY_"));
                        if (p.getBilling() == 0 && !"7".equals(p.getValue("_CREATED_BY_")) && p.getValue("_CREATED_BY_") != null) {
                            hashMap.put("without_bill", Localizer.translateMessage("advreport.withoutbilling"));
                        }
                    } else {
                        hashMap.put("plan", Localizer.translateLabel("label.unknown_plan"));
                    }
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    hashMap.put("billingType", rs.getString("billingType"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("balance", rs.getString("balance"));
                    hashMap.put("credit", rs.getString("credit"));
                    data.add(hashMap);
                    setOrderParams("accountId", true);
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
                Session.getLog().error("Error getting UserSearchByCIReport", se);
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
