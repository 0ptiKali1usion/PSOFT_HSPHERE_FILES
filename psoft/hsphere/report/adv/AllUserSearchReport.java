package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/AllUserSearchReport.class */
public class AllUserSearchReport extends AdvReport {
    private static final List allowedPlans = Arrays.asList(FMACLManager.ADMIN, "mysql", "unix", "unixreal", "windows", "windowsreal", "mailonly");

    /* renamed from: df */
    protected static final SimpleDateFormat f120df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        String emailSelect;
        String emailAnd;
        String emailLike;
        String emailFrom;
        String formatedCreated;
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String resellerId = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String emailType = (String) i.next();
        int count = 1;
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
        StringBuffer query = new StringBuffer("SELECT users.username as username,users.id as userid, accounts.id as accountId, accounts.created as created, accounts.suspended as suspended, accounts.plan_id as plan_id, " + emailSelect + "accounts.p_end as pEnd, users.reseller_id as resellerId FROM users, user_account, accounts, billing_info " + emailFrom + "WHERE users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id " + emailAnd);
        if (!isEmpty(resellerId)) {
            query.append(" AND users.reseller_id LIKE ?");
        }
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
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
        query.append(" ORDER BY accounts.id");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query.toString());
                if (!isEmpty(resellerId)) {
                    count = 1 + 1;
                    ps.setLong(1, Long.parseLong(resellerId));
                }
                if (!isEmpty(accountId)) {
                    int i2 = count;
                    count++;
                    ps.setLong(i2, Long.parseLong(accountId));
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
                    ps.setDate(i5, new Date(f120df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i6 = count;
                    int i7 = count + 1;
                    ps.setDate(i6, new Date(f120df.parse(createdBefore).getTime()));
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("userId", rs.getString("userid"));
                    hashMap.put("accountId", new Long(rs.getLong("accountId")));
                    Timestamp created = rs.getTimestamp("created");
                    SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance(3, Session.getCurrentLocale());
                    String shortDatePattern = Settings.get().getValue("shortdate");
                    if (shortDatePattern != null) {
                        df.applyPattern(shortDatePattern);
                        formatedCreated = df.format((java.util.Date) created);
                    } else {
                        formatedCreated = df.format((java.util.Date) created);
                    }
                    hashMap.put("created", formatedCreated);
                    hashMap.put("suspended", rs.getTimestamp("suspended"));
                    long resId = rs.getLong("resellerId");
                    hashMap.put("resellerId", new Long(resId));
                    hashMap.put("resellerDescription", getResellerDescription(resId));
                    hashMap.put("pEnd", rs.getDate("pEnd"));
                    hashMap.put("email", rs.getString("email"));
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
            Session.getLog().error("Error getting AllUserSearchReport", se);
            throw se;
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
