package psoft.hsphere.report.adv;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.hsphere.resource.HostEntry;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/UsersReport.class */
public class UsersReport extends AdvReport {

    /* loaded from: hsphere.zip:psoft/hsphere/report/adv/UsersReport$lServer.class */
    private class lServer {
        public int hostId;
        public int groupId;

        public lServer(int hostId, int groupId) {
            UsersReport.this = r4;
            this.hostId = hostId;
            this.groupId = groupId;
        }
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String group = (String) i.next();
        String pserver = (String) i.next();
        String lserver = (String) i.next();
        String user = null;
        String account = null;
        if (i.hasNext()) {
            user = (String) i.next();
        }
        if (i.hasNext()) {
            account = (String) i.next();
        }
        PreparedStatement ps = null;
        StringBuffer query = new StringBuffer();
        List hosts = new ArrayList();
        List data = new ArrayList();
        Connection con = Session.getDb("report");
        try {
            try {
                if (isEmpty(group)) {
                    if (!isEmpty(lserver)) {
                        int hostId = Integer.parseInt(lserver);
                        PreparedStatement ps2 = con.prepareStatement("SELECT l_server_groups.type_id FROM l_server  JOIN l_server_groups ON  l_server.group_id = l_server_groups.id  WHERE l_server.id=?");
                        ps2.setInt(1, hostId);
                        ResultSet rs = ps2.executeQuery();
                        if (rs.next()) {
                            hosts.add(new lServer(hostId, rs.getInt(1)));
                        }
                    } else if (!isEmpty(pserver)) {
                        int pserverId = Integer.parseInt(pserver);
                        PreparedStatement ps3 = con.prepareStatement("SELECT l_server.id, l_server_groups.type_id FROM l_server  JOIN l_server_groups ON  l_server.group_id = l_server_groups.id  WHERE p_server_id=?");
                        ps3.setInt(1, pserverId);
                        ResultSet rs2 = ps3.executeQuery();
                        while (rs2.next()) {
                            hosts.add(new lServer(rs2.getInt(1), rs2.getInt(2)));
                        }
                    }
                } else {
                    int groupId = Integer.parseInt(group);
                    PreparedStatement ps4 = con.prepareStatement("SELECT l_server.id, l_server_groups.type_id FROM l_server  JOIN l_server_groups ON l_server.group_id = l_server_groups.id  WHERE l_server.group_id=?");
                    ps4.setInt(1, groupId);
                    ResultSet rs3 = ps4.executeQuery();
                    while (rs3.next()) {
                        hosts.add(new lServer(rs3.getInt(1), rs3.getInt(2)));
                    }
                }
                query.append("SELECT users.username as username, accounts.id as accountId,  accounts.reseller_id as resellerId,  accounts.created as created,  accounts.suspended as suspended,  accounts.plan_id as planId,  plans.description as planDescription,  accounts.description as accountDescription,  accounts.p_end as pEnd,  billing_info.type as billingType,  contact_info.email as email,  balance_credit.balance as balance,  balance_credit.credit as credit  FROM users, user_account, accounts, balance_credit,  billing_info, plans, contact_info, parent_child p  WHERE users.id = user_account.user_id  AND user_account.account_id = accounts.id  AND billing_info.id = accounts.bi_id  AND contact_info.id = accounts.ci_id  AND balance_credit.id=accounts.id  AND plans.id = accounts.plan_id ");
                if (account != null && account.length() > 0) {
                    query.append(" AND accounts.id = ?");
                }
                if (user != null && user.length() > 0) {
                    query.append(" AND username = ?");
                }
                query.append(" AND accounts.id = p.account_id ");
                String hostingIds = "";
                String dnsIds = "";
                String mailIds = "";
                String mysqlIds = "";
                String realHostingIds = "";
                String mssqlIds = "";
                String pgsqlIds = "";
                String vpsIds = "";
                for (int index = 0; index < hosts.size(); index++) {
                    lServer host = (lServer) hosts.get(index);
                    int hostId2 = host.hostId;
                    int groupId2 = host.groupId;
                    switch (groupId2) {
                        case 1:
                        case 5:
                            hostingIds = hostingIds + hostId2 + ",";
                            break;
                        case 2:
                        case 21:
                            dnsIds = dnsIds + hostId2 + ",";
                            break;
                        case 3:
                        case 23:
                            mailIds = mailIds + hostId2 + ",";
                            break;
                        case 4:
                            mysqlIds = mysqlIds + hostId2 + ",";
                            break;
                        case 6:
                        case 7:
                            realHostingIds = realHostingIds + hostId2 + ",";
                            break;
                        case 12:
                            vpsIds = vpsIds + hostId2 + ",";
                            break;
                        case 15:
                            mssqlIds = mssqlIds + hostId2 + ",";
                            break;
                        case 18:
                            pgsqlIds = pgsqlIds + hostId2 + ",";
                            break;
                    }
                }
                StringBuffer theQuery = new StringBuffer("");
                if (hostingIds.length() > 0) {
                    Session.getLog().debug("hostingIds=" + hostingIds);
                    String toSet = hostingIds.substring(0, hostingIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = unix_user.id AND unix_user.hostid IN (" + toSet + ")");
                }
                if (dnsIds.length() > 0) {
                    String toSet2 = dnsIds.substring(0, dnsIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = dns_zones.id AND (dns_zones.master IN (" + toSet2 + ") OR dns_zones.slave1 IN (" + toSet2 + ") OR dns_zones.slave2 IN (" + toSet2 + "))");
                }
                if (mailIds.length() > 0) {
                    String toSet3 = mailIds.substring(0, mailIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = mail_services.id AND mail_services.mail_server IN (" + toSet3 + ")");
                }
                if (mysqlIds.length() > 0) {
                    String toSet4 = mysqlIds.substring(0, mysqlIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = mysqlres.id AND mysqlres.mysql_host_id IN (" + toSet4 + ")");
                }
                if (realHostingIds.length() > 0) {
                    String toSet5 = realHostingIds.substring(0, realHostingIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = real_server.id AND real_server.host_id IN (" + toSet5 + ")");
                }
                if (mssqlIds.length() > 0) {
                    String toSet6 = mssqlIds.substring(0, mssqlIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = mssqlres.id AND mssqlres.mssql_host_id IN (" + toSet6 + ")");
                }
                if (pgsqlIds.length() > 0) {
                    String toSet7 = pgsqlIds.substring(0, pgsqlIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = pgsqlres.id AND pgsqlres.pgsql_host_id IN (" + toSet7 + ")");
                }
                if (vpsIds.length() > 0) {
                    String toSet8 = vpsIds.substring(0, vpsIds.length() - 1);
                    theQuery.append(theQuery.length() > 0 ? " UNION " : "").append(query).append(" AND p.child_type = ? AND p.child_id = vps.id AND vps.hostid IN (" + toSet8 + ")");
                }
                Session.getLog().debug("Query is:  " + theQuery.toString());
                if (theQuery.length() <= 0) {
                    theQuery.append("SELECT users.username as username, accounts.id as accountId,  accounts.reseller_id as resellerId,  accounts.created as created,  accounts.suspended as suspended,  accounts.plan_id as planId,  plans.description as planDescription,  accounts.description as accountDescription,  accounts.p_end as pEnd,  billing_info.type as billingType,  contact_info.email as email,  balance_credit.balance as balance,  balance_credit.credit as credit  FROM users, user_account, accounts, balance_credit,  billing_info, plans, contact_info  WHERE users.id = user_account.user_id  AND user_account.account_id = accounts.id  AND billing_info.id = accounts.bi_id  AND contact_info.id = accounts.ci_id  AND balance_credit.id=accounts.id  AND plans.id = accounts.plan_id ");
                }
                ps = con.prepareStatement(theQuery.toString());
                int count = 1;
                if (hostingIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        count = 1 + 1;
                        ps.setLong(1, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i2 = count;
                        count++;
                        ps.setString(i2, user);
                    }
                    int i3 = count;
                    count++;
                    ps.setInt(i3, 7);
                }
                if (dnsIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i4 = count;
                        count++;
                        ps.setLong(i4, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i5 = count;
                        count++;
                        ps.setString(i5, user);
                    }
                    int i6 = count;
                    count++;
                    ps.setInt(i6, 3001);
                }
                if (mailIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i7 = count;
                        count++;
                        ps.setLong(i7, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i8 = count;
                        count++;
                        ps.setString(i8, user);
                    }
                    int i9 = count;
                    count++;
                    ps.setInt(i9, HostEntry.VPS_IP);
                }
                if (mysqlIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i10 = count;
                        count++;
                        ps.setLong(i10, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i11 = count;
                        count++;
                        ps.setString(i11, user);
                    }
                    int i12 = count;
                    count++;
                    ps.setInt(i12, 6000);
                }
                if (realHostingIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i13 = count;
                        count++;
                        ps.setLong(i13, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i14 = count;
                        count++;
                        ps.setString(i14, user);
                    }
                    int i15 = count;
                    count++;
                    ps.setInt(i15, 6102);
                }
                if (mssqlIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i16 = count;
                        count++;
                        ps.setLong(i16, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i17 = count;
                        count++;
                        ps.setString(i17, user);
                    }
                    int i18 = count;
                    count++;
                    ps.setInt(i18, 6800);
                }
                if (pgsqlIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i19 = count;
                        count++;
                        ps.setLong(i19, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i20 = count;
                        count++;
                        ps.setString(i20, user);
                    }
                    int i21 = count;
                    count++;
                    ps.setInt(i21, 6900);
                }
                if (vpsIds.length() > 0) {
                    if (account != null && account.length() > 0) {
                        int i22 = count;
                        count++;
                        ps.setLong(i22, Long.parseLong(account));
                    }
                    if (user != null && user.length() > 0) {
                        int i23 = count;
                        count++;
                        ps.setString(i23, user);
                    }
                    int i24 = count;
                    int i25 = count + 1;
                    ps.setInt(i24, 7000);
                }
                ResultSet rs4 = ps.executeQuery();
                while (rs4.next()) {
                    HashMap hashMap = new HashMap();
                    ps.clearParameters();
                    ps = con.prepareStatement("SELECT id FROM transfer_process WHERE account_id=" + rs4.getLong("accountId"));
                    if (ps.executeQuery().next()) {
                        hashMap.put("istransfer", "1");
                    } else {
                        hashMap.put("istransfer", "0");
                    }
                    hashMap.put("username", rs4.getString("username"));
                    hashMap.put("accountId", new Long(rs4.getLong("accountId")));
                    hashMap.put("resellerId", new Long(rs4.getLong("resellerId")));
                    hashMap.put("created", rs4.getTimestamp("created"));
                    hashMap.put("suspended", rs4.getTimestamp("suspended"));
                    hashMap.put("planId", new Integer(rs4.getInt("planId")));
                    Session.setResellerId(rs4.getLong("resellerId"));
                    Plan p = Plan.getPlan(rs4.getInt("planId"));
                    if (p != null) {
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs4.getString(3)) + " " + p.getValue("_PERIOD_TYPE_" + rs4.getString(3)));
                    } else {
                        hashMap.put("plan", Localizer.translateLabel("label.unknown_plan"));
                    }
                    hashMap.put("accountDescription", rs4.getString("accountDescription"));
                    hashMap.put("pEnd", rs4.getDate("pEnd"));
                    hashMap.put("billingType", rs4.getString("billingType"));
                    hashMap.put("email", rs4.getString("email"));
                    hashMap.put("balance", new Double(rs4.getDouble("balance")));
                    hashMap.put("credit", new Double(rs4.getDouble("credit")));
                    data.add(hashMap);
                }
                Session.setResellerId(1L);
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
                Session.getLog().error("Error getting UsersReport", se);
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

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    public TemplateModel FM_isTransfered(long accounId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT id FROM transfer_process WHERE account_id=" + accounId);
        if (ps.executeQuery().next()) {
            return new TemplateString("1");
        }
        return new TemplateString("0");
    }
}
