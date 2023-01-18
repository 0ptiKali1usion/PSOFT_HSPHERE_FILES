package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.hsphere.resource.email.SPFResource;
import psoft.hsphere.resource.registrar.LoggableRegistrar;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/RegistrarLogInfo.class */
public class RegistrarLogInfo extends AdvReport {
    protected StringBuffer filter = new StringBuffer();

    /* renamed from: df */
    protected static final SimpleDateFormat f132df = new SimpleDateFormat("MM/dd/yyyy");

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        String domain = (String) i.next();
        Object account = i.next();
        String username = (String) i.next();
        Object plan = i.next();
        Object trresult = i.next();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        boolean onlyLive = "1".equals(i.next());
        long resellerId = Session.getResellerId();
        StringBuffer buf = new StringBuffer("SELECT r.id, r.domain, a.id, u.username, a.plan_id, r.registrar, r.signature, r.tt_type, r.created, r.period, r.result, r.error_message, r.account_id, r.username, r.planid FROM ((registrar_log as r LEFT OUTER JOIN domains AS d ON (r.domain=d.name)) LEFT OUTER JOIN parent_child AS p ON (p.child_id=d.id)) LEFT OUTER JOIN user_account AS ua ON (ua.account_id=p.account_id) LEFT OUTER JOIN users AS u ON (ua.user_id=u.id) LEFT OUTER JOIN accounts AS a ON (a.id=ua.account_id) ");
        if (resellerId != 1) {
            buf.append("WHERE r.reseller_id=? ");
        } else {
            buf.append("WHERE 1=1 ");
        }
        if (!isEmpty(createdBefore)) {
            this.filter.append(" AND r.created <= ?");
        }
        if (!isEmpty(createdAfter)) {
            this.filter.append(" AND r.created >= ?");
        }
        if (account != null) {
            this.filter.append(" AND (a.id = ").append(account.toString());
            this.filter.append(" OR r.account_id= ").append(account.toString());
            this.filter.append(") ");
        }
        if (plan != null) {
            this.filter.append(" AND (a.plan_id= ").append(plan.toString());
            this.filter.append(" OR r.planid= ").append(plan.toString());
            this.filter.append(") ");
        }
        if (onlyLive) {
            this.filter.append(" AND a.deleted IS NULL");
        }
        if (domain != null) {
            this.filter.append(" AND r.domain=? ");
        }
        if (username != null) {
            this.filter.append(" AND (u.username= ? OR r.username=?) ");
        }
        if (trresult != null) {
            if ("1".equals(trresult.toString())) {
                this.filter.append(" AND r.result=1");
            }
            if ("2".equals(trresult.toString())) {
                this.filter.append(" AND r.result=0");
            }
        }
        buf.append(this.filter);
        Session.getLog().debug("ChargeLog query is\n" + buf.toString() + "\n");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        int count = 1;
        try {
            try {
                Session.getLog().info("-->Query: " + ((Object) buf));
                ps = con.prepareStatement(buf.toString());
                if (resellerId != 1) {
                    count = 1 + 1;
                    ps.setLong(1, Session.getResellerId());
                }
                if (!isEmpty(createdBefore)) {
                    int i2 = count;
                    count++;
                    ps.setDate(i2, new Date(f132df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(createdAfter)) {
                    int i3 = count;
                    count++;
                    ps.setDate(i3, new Date(f132df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(domain)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, domain);
                }
                if (!isEmpty(username)) {
                    int i5 = count;
                    int count2 = count + 1;
                    ps.setString(i5, username);
                    int i6 = count2 + 1;
                    ps.setString(count2, username);
                }
                ResultSet rs = ps.executeQuery();
                ArrayList data = new ArrayList();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("id", Long.toString(rs.getLong(1)));
                    hashMap.put("domain", rs.getString(2));
                    String userName = rs.getString(4);
                    if (userName != null && !"".equals(userName)) {
                        hashMap.put("account_id", Long.toString(rs.getLong(3)));
                        hashMap.put("usermane", userName);
                    } else {
                        hashMap.put("account_id", Long.toString(rs.getLong(13)));
                        hashMap.put("username", rs.getString(14));
                    }
                    hashMap.put("registrar_id", Long.toString(rs.getLong(6)));
                    hashMap.put("signature", rs.getString(7));
                    hashMap.put("tt_type", LoggableRegistrar.getTtType(rs.getInt(8)));
                    hashMap.put("created", rs.getTimestamp(9));
                    int period = rs.getInt(10);
                    hashMap.put("period", period > 0 ? Integer.toString(period) : "");
                    hashMap.put("result", rs.getInt(11) == 1 ? "success" : SPFResource.DEFAULT_PROCESSING_VALUE);
                    String errMess = Session.getClobValue(rs, 12);
                    hashMap.put("error", errMess);
                    data.add(hashMap);
                }
                Session.getLog().info("Field in data");
                init(new DataContainer(data));
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
            } catch (Exception se) {
                Session.getLog().error("error getting the report", se);
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

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
