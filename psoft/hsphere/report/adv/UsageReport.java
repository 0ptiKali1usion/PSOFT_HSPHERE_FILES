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
import psoft.hsphere.Account;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/UsageReport.class */
public class UsageReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f139df = new SimpleDateFormat("MM/dd/yyyy");

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
        String utype = (String) i.next();
        StringBuffer query = new StringBuffer("SELECT id as accountId, accounts.created FROM accounts WHERE accounts.reseller_id = ?");
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
                    int i5 = count + 1;
                    ps.setString(i4, username.toUpperCase());
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    query.delete(0, query.length());
                    query.append("SELECT SUM(used)/COUNT(DISTINCT(cdate)) AS usage FROM usage_log WHERE account_id = ?");
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
                    int count2 = 1 + 1;
                    ps1.setLong(1, rs.getLong("accountId"));
                    if (!isEmpty(createdAfter)) {
                        Date date = new Date(f139df.parse(createdAfter).getTime());
                        if (date.compareTo((java.util.Date) rs.getDate("created")) > 0) {
                            count2++;
                            ps1.setDate(count2, date);
                        } else {
                            count2++;
                            ps1.setDate(count2, rs.getDate("created"));
                        }
                    }
                    if (!isEmpty(createdBefore)) {
                        int i6 = count2;
                        count2++;
                        ps1.setDate(i6, new Date(f139df.parse(createdBefore).getTime()));
                    }
                    if (!isEmpty(utype)) {
                        int i7 = count2;
                        int i8 = count2 + 1;
                        ps1.setInt(i7, Integer.parseInt(utype));
                    }
                    ResultSet rs1 = ps1.executeQuery();
                    if (rs1.next()) {
                        double usage = rs1.getDouble("usage");
                        if (usage > 0.0d) {
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
                            hashMap.put("usage", smartLabel(usage));
                            hashMap.put("usage_sort", new Double(usage));
                            data.add(hashMap);
                        }
                    }
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
                Session.getLog().error("Error getting UsageReport", se);
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

    public String smartLabel(double mBytes) {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(2);
        return numberFormat.format(mBytes) + " Mb";
    }
}
