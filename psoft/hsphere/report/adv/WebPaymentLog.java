package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.payment.WebPayment;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/WebPaymentLog.class */
public class WebPaymentLog extends AdvReport {
    protected StringBuffer filter = new StringBuffer();

    /* renamed from: df */
    protected static final SimpleDateFormat f142df = new SimpleDateFormat("MM/dd/yyyy");

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        Object account = i.next();
        Object username = i.next();
        Object plan = i.next();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        boolean onlyLive = "1".equals(i.next());
        Object type = i.next();
        StringBuffer buf = new StringBuffer("SELECT ex.id, ex.account_id, ex.request_id, ex.reseller_id, ex.created, ex.gateway_name, ex.request_type, ex.requested_amount, ex.result_amount, ex.error_message, ex.result, pl.description, p2.description, bi.email, bi2.email, ex.reseller_id, u.username FROM extern_pm_log AS ex LEFT JOIN accounts AS acc ON (ex.account_id=acc.id) LEFT JOIN user_account AS ua ON (ex.account_id=ua.account_id) LEFT JOIN users AS u ON (ua.user_id=u.id) LEFT JOIN plans as pl ON (acc.plan_id=pl.id) LEFT JOIN request_record AS rr ON (ex.request_id=rr.request_id) LEFT JOIN plans AS p2 ON (rr.plan_id=p2.id) LEFT JOIN billing_info AS bi ON (acc.bi_id=bi.id) LEFT JOIN billing_info AS bi2 ON (rr.bid=bi2.id) WHERE ex.reseller_id=? ");
        if (!isEmpty(createdBefore)) {
            this.filter.append(" AND ex.created <= ?");
        }
        if (!isEmpty(createdAfter)) {
            this.filter.append(" AND ex.created >= ?");
        }
        if (account != null) {
            this.filter.append(" AND acc.id = ").append(account.toString());
        }
        if (plan != null) {
            this.filter.append(" AND acc.plan_id= ").append(plan.toString());
        }
        if (onlyLive) {
            this.filter.append(" AND acc.id > 0 AND acc.deleted IS NULL");
        }
        if (type != null) {
            if ("success".equals(type.toString())) {
                this.filter.append(" AND ex.result=0");
            }
            if ("error".equals(type.toString())) {
                this.filter.append(" AND ex.result>0");
            }
        }
        if (username != null) {
            this.filter.append(" AND u.username=? ");
        }
        buf.append(this.filter);
        Session.getLog().debug("Web Payment Log query is:\n" + buf.toString() + "\n");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                Session.getLog().info("-->Query: " + ((Object) buf));
                ps = con.prepareStatement(buf.toString());
                int count = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(createdBefore)) {
                    count++;
                    ps.setDate(count, new Date(f142df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(createdAfter)) {
                    int i2 = count;
                    count++;
                    ps.setDate(i2, new Date(f142df.parse(createdAfter).getTime()));
                }
                if (username != null) {
                    int i3 = count;
                    int i4 = count + 1;
                    ps.setString(i3, (String) username);
                }
                ResultSet rs = ps.executeQuery();
                ArrayList data = new ArrayList();
                while (rs.next()) {
                    Hashtable map = new Hashtable();
                    long transId = rs.getLong(1);
                    long accountId = rs.getLong(2);
                    long requestId = rs.getLong(3);
                    long resellerId = rs.getLong(4);
                    Timestamp created = rs.getTimestamp(5);
                    String gatewayName = rs.getString(6);
                    String paymentDescr = WebPayment.getRequestTypeDescription(rs.getInt(7));
                    double amount = 1 == rs.getInt(7) ? rs.getDouble(8) : rs.getDouble(9);
                    String errorMessage = rs.getString(10);
                    String result = rs.getInt(11) == 0 ? "OK" : "ERROR";
                    String planDescription = accountId > 0 ? rs.getString(12) : rs.getString(13);
                    String email = accountId > 0 ? rs.getString(14) : rs.getString(15);
                    String userName = accountId > 0 ? rs.getString(17) : "";
                    map.put("transId", new Long(transId));
                    map.put("username", userName);
                    map.put("accountId", new Long(accountId));
                    map.put("requestId", new Long(requestId));
                    map.put("resellerId", new Long(resellerId));
                    map.put("created", created);
                    map.put("gatewayName", gatewayName);
                    map.put("paymentDescr", paymentDescr);
                    map.put("amount", new Double(amount));
                    map.put("errorMessage", errorMessage);
                    map.put("result", result);
                    map.put("planDescription", WebPayment.isEmpty(planDescription) ? "" : planDescription);
                    map.put("email", WebPayment.isEmpty(email) ? "" : email);
                    data.add(map);
                }
                Session.getLog().info("Field in data");
                init(new DataContainer(data));
                Session.closeStatement(ps);
                Session.closeStatement(null);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                Session.closeStatement(null);
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
        } catch (Exception se) {
            Session.getLog().error("error getting the report", se);
            throw se;
        }
    }

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
