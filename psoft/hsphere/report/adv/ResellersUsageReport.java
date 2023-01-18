package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/ResellersUsageReport.class */
public class ResellersUsageReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f134df = new SimpleDateFormat("MM/dd/yyyy");

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        int count;
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String planId = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String utype = (String) i.next();
        String reseller = (String) i.next();
        boolean isDetail = !isEmpty((String) i.next());
        boolean isAdmin = !isEmpty((String) i.next());
        int count2 = 1;
        StringBuffer query = new StringBuffer();
        if (!isEmpty(accountId)) {
            isDetail = true;
        }
        if (!isDetail) {
            query.append("SELECT users.id as userId, users.username, accounts.id as accountId, accounts.plan_id as planId, users.reseller_id, contact_info.email as email, accounts.created FROM users, accounts, resellers, user_account, contact_info WHERE accounts.id = user_account.account_id AND users.id = user_account.user_id AND users.id = resellers.id AND accounts.ci_id = contact_info.id");
        } else {
            query.append("SELECT users.username as username, users.reseller_id, accounts.id as accountId, accounts.plan_id as planId, contact_info.email as email, accounts.created FROM users, user_account, accounts, contact_info WHERE users.id = user_account.user_id AND user_account.account_id = accounts.id AND accounts.ci_id = contact_info.id");
        }
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
        if (!isEmpty(reseller)) {
            if (isDetail) {
                query.append(" AND accounts.reseller_id = ?");
            } else {
                query.append(" AND users.id = ?");
            }
        }
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        long resellerId = Session.getResellerId();
        try {
            try {
                ps = con.prepareStatement(query.toString());
                if (!isEmpty(accountId)) {
                    count2 = 1 + 1;
                    ps.setLong(1, Long.parseLong(accountId));
                }
                if (!isEmpty(planId)) {
                    int i2 = count2;
                    count2++;
                    ps.setInt(i2, Integer.parseInt(planId));
                }
                if (!isEmpty(email)) {
                    int i3 = count2;
                    count2++;
                    ps.setString(i3, email);
                }
                if (!isEmpty(username)) {
                    int i4 = count2;
                    count2++;
                    ps.setString(i4, "%" + username.toUpperCase() + "%");
                }
                if (!isEmpty(reseller)) {
                    int i5 = count2;
                    int i6 = count2 + 1;
                    ps.setLong(i5, Long.parseLong(reseller));
                }
                Session.getLog().debug("SQL:" + ps.toString());
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                if (!isDetail && isAdmin) {
                    int count3 = 1;
                    query.delete(0, query.length());
                    query.append("SELECT SUM(used)/COUNT(DISTINCT(cdate)) AS usage FROM usage_log WHERE EXISTS (SELECT id FROM accounts WHERE id = account_id AND reseller_id = 1)");
                    con.prepareStatement(query.toString());
                    if (!isEmpty(createdAfter)) {
                        query.append(" AND cdate >= ?");
                    }
                    if (!isEmpty(createdBefore)) {
                        query.append(" AND cdate <= ?");
                    }
                    if (!isEmpty(utype)) {
                        query.append(" AND usage_type = ?");
                    }
                    PreparedStatement ps1 = con.prepareStatement(query.toString());
                    if (!isEmpty(createdAfter)) {
                        count3 = 1 + 1;
                        ps1.setDate(1, new Date(f134df.parse(createdAfter).getTime()));
                    }
                    if (!isEmpty(createdBefore)) {
                        int i7 = count3;
                        count3++;
                        ps1.setDate(i7, new Date(f134df.parse(createdBefore).getTime()));
                    }
                    if (!isEmpty(utype)) {
                        int i8 = count3;
                        int i9 = count3 + 1;
                        ps1.setInt(i8, Integer.parseInt(utype));
                    }
                    ResultSet rs1 = ps1.executeQuery();
                    HashMap hashMap = new HashMap();
                    if (rs1.next()) {
                        hashMap.put("usage", smartLabel(rs1.getDouble("usage")));
                    }
                    hashMap.put("username", User.getUser(1L).getLogin());
                    hashMap.put("accountId", new Long(1L));
                    hashMap.put(FMACLManager.RESELLER, getResellerDescription(1L));
                    hashMap.put("email", "N/A");
                    data.add(hashMap);
                }
                long lastReseller = resellerId;
                while (rs.next()) {
                    if (rs.getLong("reseller_id") != lastReseller) {
                        lastReseller = rs.getLong("reseller_id");
                        Session.setResellerId(lastReseller);
                    }
                    query.delete(0, query.length());
                    if (!isDetail) {
                        query.append("SELECT SUM(used)/COUNT(DISTINCT(cdate)) AS usage FROM usage_log WHERE EXISTS (SELECT id FROM accounts WHERE id = account_id AND reseller_id = ?)");
                    } else {
                        query.append("SELECT SUM(used)/COUNT(DISTINCT(cdate)) AS usage FROM usage_log WHERE account_id = ?");
                    }
                    if (!isEmpty(createdAfter)) {
                        query.append(" AND cdate >= ?");
                    }
                    if (!isEmpty(createdBefore)) {
                        query.append(" AND cdate <= ?");
                    }
                    if (!isEmpty(utype)) {
                        query.append(" AND usage_type = ?");
                    }
                    PreparedStatement ps12 = con.prepareStatement(query.toString());
                    if (!isDetail) {
                        count = 1 + 1;
                        ps12.setLong(1, rs.getLong("userId"));
                    } else {
                        count = 1 + 1;
                        ps12.setLong(1, rs.getLong("accountId"));
                    }
                    if (!isEmpty(createdAfter)) {
                        Date date = new Date(f134df.parse(createdAfter).getTime());
                        if (date.compareTo((java.util.Date) rs.getDate("created")) > 0) {
                            int i10 = count;
                            count++;
                            ps12.setDate(i10, date);
                        } else {
                            int i11 = count;
                            count++;
                            ps12.setDate(i11, rs.getDate("created"));
                        }
                    }
                    if (!isEmpty(createdBefore)) {
                        int i12 = count;
                        count++;
                        ps12.setDate(i12, new Date(f134df.parse(createdBefore).getTime()));
                    }
                    if (!isEmpty(utype)) {
                        int i13 = count;
                        int i14 = count + 1;
                        ps12.setInt(i13, Integer.parseInt(utype));
                    }
                    Session.getLog().debug("SQL:" + ps12.toString());
                    ResultSet rs12 = ps12.executeQuery();
                    HashMap hashMap2 = new HashMap();
                    if (rs12.next()) {
                        hashMap2.put("usage", smartLabel(rs12.getDouble("usage")));
                        hashMap2.put("usage_sort", new Double(rs12.getDouble("usage")));
                    }
                    hashMap2.put("username", rs.getString("username"));
                    hashMap2.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap2.put(FMACLManager.RESELLER, getResellerDescription(lastReseller));
                    hashMap2.put("plan", Plan.getPlan(rs.getInt("planId")));
                    hashMap2.put("email", rs.getString("email"));
                    data.add(hashMap2);
                }
                init(new DataContainer(data));
                if (Session.getResellerId() != resellerId) {
                    Session.setResellerId(resellerId);
                }
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
                if (Session.getResellerId() != resellerId) {
                    Session.setResellerId(resellerId);
                }
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
            Session.getLog().error("Error getting UsageReport", se);
            throw se;
        }
    }

    protected String smartLabel(double mBytes) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(mBytes) + " Mb";
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

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
