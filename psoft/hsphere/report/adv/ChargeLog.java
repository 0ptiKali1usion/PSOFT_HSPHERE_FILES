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
import psoft.epayment.GenericMerchantGateway;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/ChargeLog.class */
public class ChargeLog extends AdvReport {
    protected StringBuffer filter = new StringBuffer();

    /* renamed from: df */
    protected static final SimpleDateFormat f124df = new SimpleDateFormat("MM/dd/yyyy");

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        boolean result;
        String user;
        Iterator i = args.iterator();
        Object account = i.next();
        Object username = i.next();
        Object plan = i.next();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        boolean onlyLive = "1".equals(i.next());
        Object type = i.next();
        StringBuffer buf = new StringBuffer("SELECT a.id, a.plan_id, a.period_id, c.created, c.amount, c.trtype, c.result, error_message, d.email, c.id, c.mgid  FROM charge_log c, accounts a, billing_info d " + (username != null ? ",user_account, users " : "") + " WHERE c.account_id = a.id AND a.bi_id = d.id AND a.reseller_id = ?");
        if (!isEmpty(createdBefore)) {
            this.filter.append(" AND c.created <= ?");
        }
        if (!isEmpty(createdAfter)) {
            this.filter.append(" AND c.created >= ?");
        }
        if (account != null) {
            this.filter.append(" AND a.id = ").append(account.toString());
        }
        if (plan != null) {
            this.filter.append(" AND a.plan_id= ").append(plan.toString());
        }
        if (onlyLive) {
            this.filter.append(" AND a.deleted IS NULL");
        }
        if (type != null) {
            if ("1".equals(type.toString())) {
                this.filter.append(" AND c.result=1");
            }
            if ("2".equals(type.toString())) {
                this.filter.append(" AND c.result=0");
            }
        }
        if (username != null) {
            this.filter.append(" AND a.id = user_account.account_id AND user_account.user_id = users.id AND UPPER(users.username) = '").append(username.toString().toUpperCase()).append("' ");
        }
        buf.append(this.filter);
        Session.getLog().debug("ChargeLog query is\n" + buf.toString() + "\n");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        try {
            try {
                Session.getLog().info("-->Query: " + ((Object) buf));
                ps = con.prepareStatement(buf.toString());
                int count = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(createdBefore)) {
                    count++;
                    ps.setDate(count, new Date(f124df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(createdAfter)) {
                    int i2 = count;
                    int i3 = count + 1;
                    ps.setDate(i2, new Date(f124df.parse(createdAfter).getTime()));
                }
                ps2 = con.prepareStatement("SELECT username FROM users,user_account WHERE users.id = user_account.user_id AND user_account.account_id = ?");
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    String errMess = Session.getClobValue(rs, 8);
                    int trtype = rs.getInt(6);
                    if (rs.wasNull()) {
                        trtype = 100;
                    }
                    String amount = "";
                    if (trtype == 100) {
                        hashMap.put("amount", "");
                        result = errMess.length() == 0;
                    } else {
                        amount = Session.getClobValue(rs, 5);
                        result = rs.getBoolean(7);
                    }
                    String strResult = result ? "Successful" : "Failed";
                    hashMap.put("result", strResult);
                    hashMap.put("amount", amount);
                    hashMap.put("trtype", GenericMerchantGateway.getTrDescription(trtype));
                    hashMap.put("error_message", errMess);
                    hashMap.put("email", rs.getString(9));
                    ps2.setLong(1, rs.getLong(1));
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        user = rs2.getString(1);
                    } else {
                        user = "";
                    }
                    Plan p = Plan.getPlan(rs.getInt(2));
                    hashMap.put("accountId", new Long(rs.getLong(1)));
                    if (p != null) {
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString(3)) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString(3)));
                    } else {
                        hashMap.put("plan", "Unknown Plan");
                    }
                    hashMap.put("username", user);
                    hashMap.put("created", rs.getTimestamp(4));
                    hashMap.put("reqid", Session.getClobValue(rs, 10));
                    hashMap.put("mgid", Session.getClobValue(rs, 11));
                    data.add(hashMap);
                }
                Session.getLog().info("Field in data");
                init(new DataContainer(data));
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
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
