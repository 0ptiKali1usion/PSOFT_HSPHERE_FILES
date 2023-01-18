package psoft.hsphere.report.adv;

import java.sql.Connection;
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

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/DomainSearchReport.class */
public class DomainSearchReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f129df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        String emailSelect;
        String emailAnd;
        String emailFrom;
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String domain = (String) i.next();
        String emailType = (String) i.next();
        if ("bi".equals(emailType)) {
            emailSelect = "billing_info.email as email, ";
            emailAnd = "";
            emailFrom = "";
        } else {
            emailSelect = "contact_info.email as email, ";
            emailAnd = "AND contact_info.id = accounts.ci_id ";
            emailFrom = ", contact_info ";
        }
        StringBuffer query = new StringBuffer("SELECT users.username, accounts.id as accountId, accounts.created, accounts.suspended, accounts.plan_id as planId, plans.description as planDescription, accounts.description as accountDescription, accounts.p_end as pEnd, billing_info.type as billingType, " + emailSelect + "balance_credit.balance, balance_credit.credit,domains.name as domain,type_name.description as domainType, 't' as f_domain FROM users, user_account, accounts, balance_credit, billing_info, plans, domains, parent_child, type_name " + emailFrom + "WHERE  parent_child.child_id = domains.id AND accounts.id = parent_child.account_id AND users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id " + emailAnd + "AND balance_credit.id=accounts.id AND plans.id = accounts.plan_id AND accounts.reseller_id = ? AND parent_child.child_type = type_name.id");
        if (!isEmpty(domain)) {
            query.append(" AND UPPER(domains.name) LIKE ? ");
        }
        query.append(" UNION ALL SELECT null, 0, null, null, 0, null, null, null, 'none', null, 0, 0, dns_zones.name, 'Service DNS Zone' , 'f' FROM e_zones, dns_zones WHERE e_zones.reseller_id = " + Session.getResellerId() + " AND e_zones.id = dns_zones.id AND e_zones.r_id IS NULL");
        if (!isEmpty(domain)) {
            query.append(" AND UPPER(dns_zones.name) LIKE ? ");
        }
        query.append(" ORDER BY f_domain, domain");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query.toString());
                int count = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(domain)) {
                    int count2 = count + 1;
                    ps.setString(count, domain.toUpperCase());
                    int i2 = count2 + 1;
                    ps.setString(count2, domain.toUpperCase());
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
                        if (p.getBilling() == 0 && !"7".equals(p.getValue("_CREATED_BY_")) && p.getValue("_CREATED_BY_") != null) {
                            hashMap.put("without_bill", Localizer.translateMessage("advreport.withoutbilling"));
                        }
                        hashMap.put("planDescription", rs.getString("planDescription"));
                        hashMap.put("createdBy", p.getValue("_CREATED_BY_"));
                    } else {
                        hashMap.put("planDescription", Localizer.translateLabel("label.unknown_plan"));
                    }
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    hashMap.put("billingType", rs.getString("billingType"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("balance", new Double(rs.getDouble("balance")));
                    hashMap.put("credit", new Double(rs.getDouble("credit")));
                    hashMap.put("domain", rs.getString("domain"));
                    hashMap.put("domainType", rs.getString("domainType"));
                    if (rs.getString("f_domain").equals("t")) {
                        hashMap.put("f_domain", rs.getString("f_domain") + rs.getString("domainType"));
                    } else {
                        hashMap.put("f_domain", rs.getString("f_domain"));
                    }
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                setOrderParams("f_domain", true);
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
            Session.getLog().error("Error getting DomainSearchReport", se);
            throw se;
        }
    }
}
