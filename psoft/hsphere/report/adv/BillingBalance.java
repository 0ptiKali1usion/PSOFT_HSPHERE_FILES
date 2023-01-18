package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/BillingBalance.class */
public class BillingBalance extends AdvReport {
    public final long MILLIS_IN_DAY = 86400000;
    protected final String shortDateFormat = "MM/dd/yyyy";
    protected StringBuffer where = new StringBuffer();

    protected void daterange(String varname, int days) {
        Calendar cal = TimeUtils.getCalendar();
        cal.add(5, (-1) * days);
        Object before = cal.getTime();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        if (before != null) {
            this.where.append(" AND ").append(varname).append(" <= to_date('").append(dateFormat.format(before)).append("', 'MM/DD/YYYY')");
        }
        this.where.append(" AND ").append(varname).append(" IS NOT NULL");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        Object plan = i.next();
        Object type = i.next();
        Object mul = i.next();
        Object val = i.next();
        Object days = i.next();
        StringBuffer buf = new StringBuffer("SELECT a.id, a.plan_id, u.username, b.balance, a.period_id, a.suspended, bi.type, b.negative_date, bi.email, bi.company, bi.name, bi.last_name FROM users u, user_account ua, accounts a, balance_credit b, billing_info bi WHERE a.id = b.id AND a.bi_id = bi.id AND (?*b.balance) > ? AND a.deleted IS NULL AND ua.account_id = a.id AND u.id = ua.user_id AND a.bi_id <> 0 AND a.reseller_id = ?");
        try {
            if (Integer.parseInt((String) mul) < 0) {
                daterange("b.negative_date", Integer.parseInt(days.toString()));
            }
        } catch (Exception e) {
        }
        if (plan != null) {
            this.where.append(" AND a.plan_id = ").append(plan.toString());
        }
        if (type != null && !"".equals(type.toString())) {
            this.where.append(" AND bi.type = '").append(type.toString()).append("'");
        }
        buf.append(this.where);
        Connection con = null;
        PreparedStatement ps = null;
        try {
            try {
                con = Session.getDb("report");
                ps = con.prepareStatement(buf.toString());
                ps.setInt(1, Integer.parseInt((String) mul));
                try {
                    ps.setDouble(2, Double.parseDouble((String) val));
                } catch (Exception e2) {
                    ps.setDouble(2, 0.0d);
                }
                ps.setLong(3, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    Plan p = Plan.getPlan(rs.getInt(2));
                    hashMap.put("accountId", new Long(rs.getLong(1)));
                    hashMap.put("type", rs.getString(7));
                    if (p != null) {
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString(5)) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString(5)));
                    } else {
                        hashMap.put("plan", "Unknown Plan");
                    }
                    hashMap.put("username", rs.getString(3));
                    hashMap.put("balance", new Double(rs.getDouble(4)));
                    hashMap.put("email", rs.getString(9));
                    hashMap.put("company", HTMLEncoder.encode(rs.getString(10)));
                    String name = rs.getString(11) + " " + rs.getString(12);
                    hashMap.put("name", HTMLEncoder.encode(name));
                    try {
                        long negDays = Math.round((float) ((TimeUtils.dropMinutes(TimeUtils.getDate()).getTime() - TimeUtils.dropMinutes(rs.getTimestamp(8)).getTime()) / 86400000));
                        hashMap.put("days", new Long(negDays));
                    } catch (Exception e3) {
                        hashMap.put("days", "");
                    }
                    if (rs.getTimestamp(6) != null) {
                        hashMap.put("suspended", rs.getTimestamp(6));
                    }
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
            } catch (SQLException se) {
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
}
