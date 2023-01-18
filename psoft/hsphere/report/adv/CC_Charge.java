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
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/CC_Charge.class */
public class CC_Charge extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f123df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        boolean result;
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String username = (String) i.next();
        String cc_type = (String) i.next();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        boolean onlyLive = "1".equals(i.next());
        String tr_result = (String) i.next();
        StringBuffer query = new StringBuffer("SELECT a.id as accountId, u.username as username, cc.hidden_cc_number as ccNumber, cc.type as ccType, cl.id as chargeId, cl.amount as amount, cl.created as created, cl.result as result, cl.trtype as transactionType, cl.error_message as error_message FROM charge_log cl JOIN accounts a ON a.id = cl.account_id LEFT JOIN user_account ua ON ua.account_id = a.id LEFT JOIN users u ON u.id = ua.user_id JOIN credit_card cc ON cc.id = a.bi_id WHERE a.reseller_id = ?");
        if (!isEmpty(accountId)) {
            query.append(" AND a.id = ?");
        }
        if (!isEmpty(username)) {
            query.append(" AND UPPER(u.username) LIKE ?");
        }
        if (!isEmpty(cc_type)) {
            query.append(" AND cc.type LIKE ?");
        }
        if (!isEmpty(tr_result)) {
            query.append(" AND cl.result = ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND cl.created <= ?");
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND cl.created >= ?");
        }
        if (onlyLive) {
            query.append(" AND a.deleted IS NULL");
        }
        query.append(" ORDER BY created");
        Session.getLog().debug("SQL:" + query.toString());
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
                if (!isEmpty(username)) {
                    int i2 = count;
                    count++;
                    ps.setString(i2, username.toUpperCase());
                }
                if (!isEmpty(cc_type)) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, cc_type);
                }
                if (!isEmpty(tr_result)) {
                    int res = 1;
                    if ("2".equals(tr_result)) {
                        res = 0;
                    }
                    int i4 = count;
                    count++;
                    ps.setInt(i4, res);
                }
                if (!isEmpty(createdBefore)) {
                    int i5 = count;
                    count++;
                    ps.setDate(i5, new Date(f123df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(createdAfter)) {
                    int i6 = count;
                    int i7 = count + 1;
                    ps.setDate(i6, new Date(f123df.parse(createdAfter).getTime()));
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                double total = 0.0d;
                double totalSum = 0.0d;
                boolean first = true;
                Date curentDate = new Date(0L);
                while (rs.next()) {
                    double amount = 0.0d;
                    int trtype = rs.getInt("transactionType");
                    if (rs.wasNull()) {
                        trtype = 100;
                    }
                    if (trtype != 100) {
                        amount = rs.getDouble("amount");
                        result = rs.getBoolean("result");
                    } else {
                        result = rs.getString("error_message").length() == 0;
                    }
                    String strResult = result ? "Successful" : "Failed";
                    if (curentDate.compareTo((java.util.Date) rs.getDate("created")) != 0) {
                        if (!first) {
                            HashMap hashMap = new HashMap();
                            hashMap.put("tabId", "1");
                            hashMap.put("date", curentDate);
                            hashMap.put("total", new Double(total));
                            data.add(hashMap);
                        } else {
                            first = false;
                        }
                        curentDate = rs.getDate("created");
                        switch (trtype) {
                            case 0:
                            case 4:
                                totalSum += total;
                                break;
                            case 3:
                                totalSum -= total;
                                break;
                        }
                        total = 0.0d;
                    }
                    total += amount;
                    HashMap hashMap2 = new HashMap();
                    hashMap2.put("tabId", "0");
                    hashMap2.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap2.put("username", rs.getString("username"));
                    hashMap2.put("ccNumber", rs.getString("ccNumber"));
                    hashMap2.put("ccType", rs.getString("ccType"));
                    hashMap2.put("chargeId", new Long(rs.getLong("chargeId")));
                    hashMap2.put("created", rs.getTimestamp("created"));
                    hashMap2.put("amount", new Double(amount));
                    hashMap2.put("result", strResult);
                    data.add(hashMap2);
                }
                if (!first) {
                    HashMap hashMap3 = new HashMap();
                    hashMap3.put("tabId", "1");
                    hashMap3.put("date", curentDate);
                    hashMap3.put("total", new Double(total));
                    data.add(hashMap3);
                    totalSum += total;
                }
                HashMap hashMap4 = new HashMap();
                hashMap4.put("tabId", "2");
                hashMap4.put("total", new Double(totalSum));
                data.add(hashMap4);
                init(new DataContainer(data));
                setOrderParams("created", true);
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
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
        } catch (Exception se) {
            Session.getLog().error("error getting the CC_Charge report", se);
            throw se;
        }
    }
}
