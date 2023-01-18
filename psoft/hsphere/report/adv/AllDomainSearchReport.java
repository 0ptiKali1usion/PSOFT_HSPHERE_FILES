package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/AllDomainSearchReport.class */
public class AllDomainSearchReport extends AdvReport {
    private static final List allowedPlans = Arrays.asList(FMACLManager.ADMIN, "mysql", "unix", "unixreal", "windows", "windowsreal", "mailonly");

    /* renamed from: df */
    protected static final SimpleDateFormat f119df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    /* JADX WARN: Finally extract failed */
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
        StringBuffer query = new StringBuffer("SELECT domains.name as domain, users.username, accounts.id as accountId, accounts.created, accounts.plan_id as plan_id, accounts.suspended, users.id as userid, " + emailSelect + "accounts.p_end as pEnd, users.reseller_id as resellerId, type_name.description as domainType, 't' as f_domain FROM users, user_account, accounts, billing_info, domains, parent_child, type_name " + emailFrom + "WHERE parent_child.child_id = domains.id AND users.id = user_account.user_id AND user_account.account_id = accounts.id AND accounts.id = parent_child.account_id AND billing_info.id = accounts.bi_id AND parent_child.child_type = type_name.id " + emailAnd);
        if (!isEmpty(domain)) {
            query.append(" AND UPPER(domains.name) LIKE ? ");
        }
        query.append(" UNION ALL SELECT dns_zones.name, null, 0 ,null, 0, null, 0, null, null,e_zones.reseller_id, 'Service DNS Zone', 'f' FROM e_zones, dns_zones WHERE e_zones.id = dns_zones.id AND e_zones.r_id IS NULL");
        if (!isEmpty(domain)) {
            query.append(" AND UPPER(dns_zones.name) LIKE ? ");
        }
        query.append(" ORDER BY f_domain, domain");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query.toString());
                if (!isEmpty(domain)) {
                    int count = 1 + 1;
                    ps.setString(1, domain.toUpperCase());
                    int i2 = count + 1;
                    ps.setString(count, domain.toUpperCase());
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("domain", rs.getString("domain"));
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("userId", rs.getString("userid"));
                    hashMap.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap.put("created", rs.getTimestamp("created"));
                    hashMap.put("suspended", rs.getTimestamp("suspended"));
                    long resId = rs.getLong("resellerId");
                    hashMap.put("resellerId", new Long(resId));
                    hashMap.put("resellerDescription", getResellerDescription(resId));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("domainType", rs.getString("domainType"));
                    if (rs.getString("f_domain").equals("t")) {
                        hashMap.put("f_domain", rs.getString("f_domain") + rs.getString("domainType"));
                    } else {
                        hashMap.put("f_domain", rs.getString("f_domain"));
                    }
                    PreparedStatement ps1 = con.prepareStatement("SELECT value FROM plan_value WHERE plan_id = ? AND name = ?");
                    ps1.setInt(1, rs.getInt("plan_id"));
                    ps1.setString(2, "_CREATED_BY_");
                    ResultSet rs1 = ps1.executeQuery();
                    if (rs1.next()) {
                        if (allowedPlans.contains(rs1.getString("value"))) {
                            hashMap.put("moveable", "1");
                        } else {
                            hashMap.put("moveable", "0");
                        }
                    } else {
                        hashMap.put("moveable", "0");
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
            } catch (SQLException se) {
                Session.getLog().error("Error getting AllDomainSearchReport", se);
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

    protected String getResellerDescription(long id) {
        try {
            Reseller res = Reseller.getReseller(id);
            return res.getUser();
        } catch (Exception e) {
            Session.getLog().warn("Can't get reseller info", e);
            return null;
        }
    }
}
