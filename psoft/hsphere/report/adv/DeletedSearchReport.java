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

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/DeletedSearchReport.class */
public class DeletedSearchReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f128df = new SimpleDateFormat("MM/dd/yyyy");

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
        String billingType = (String) i.next();
        String email = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String deletedAfter = (String) i.next();
        String deletedBefore = (String) i.next();
        String emailType = (String) i.next();
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
        StringBuffer query = new StringBuffer("SELECT accounts.id as accountId, accounts.created as created, accounts.suspended as suspended, accounts.plan_id as planId, plans.description as planDescription, accounts.description as accountDescription, accounts.p_end as pEnd, billing_info.type as billingType, " + emailSelect + "balance_credit.balance as balance, balance_credit.credit as credit, accounts.deleted as deleted FROM accounts, balance_credit, billing_info, plans " + emailFrom + "WHERE billing_info.id = accounts.bi_id " + emailAnd + "AND balance_credit.id=accounts.id AND plans.id = accounts.plan_id AND accounts.reseller_id = ? AND accounts.deleted IS NOT NULL AND (accounts.failed <>1 OR accounts.failed IS NULL) ");
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            query.append(" AND accounts.plan_id = ?");
        }
        if (!isEmpty(billingType)) {
            query.append(" AND billing_info.type = ?");
        }
        if (!isEmpty(email)) {
            query.append(emailLike);
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND accounts.created > ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND accounts.created < ? ");
        }
        if (!isEmpty(deletedAfter)) {
            query.append(" AND accounts.deleted > ?");
        }
        if (!isEmpty(deletedBefore)) {
            query.append(" AND accounts.deleted < ? ");
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
                if (!isEmpty(createdAfter)) {
                    int i5 = count;
                    count++;
                    ps.setDate(i5, new Date(f128df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i6 = count;
                    count++;
                    ps.setDate(i6, new Date(f128df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(deletedAfter)) {
                    int i7 = count;
                    count++;
                    ps.setDate(i7, new Date(f128df.parse(deletedAfter).getTime()));
                }
                if (!isEmpty(deletedBefore)) {
                    int i8 = count;
                    int i9 = count + 1;
                    ps.setDate(i8, new Date(f128df.parse(deletedBefore).getTime()));
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap.put("created", rs.getTimestamp("created"));
                    hashMap.put("deleted", rs.getTimestamp("deleted"));
                    hashMap.put("suspended", rs.getTimestamp("suspended"));
                    hashMap.put("planId", new Integer(rs.getInt("planId")));
                    Plan p = Plan.getPlan(rs.getInt("planId"));
                    if (p != null && p.getBilling() == 0 && !"7".equals(p.getValue("_CREATED_BY_")) && p.getValue("_CREATED_BY_") != null) {
                        hashMap.put("without_bill", Localizer.translateMessage("advreport.withoutbilling"));
                    }
                    hashMap.put("planDescription", rs.getString("planDescription"));
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    hashMap.put("billingType", rs.getString("billingType"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("balance", rs.getString("balance"));
                    hashMap.put("credit", rs.getString("credit"));
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
            Session.getLog().error("Error getting DeletedSearchReport", se);
            throw se;
        }
    }
}
