package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/UserDetails.class */
public class UserDetails extends AdvReport {
    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        long sellerId = Long.parseLong((String) i.next());
        Session.getLog().debug("SellerId=" + sellerId);
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            try {
                con = Session.getDb("report");
                ps = con.prepareStatement("SELECT client_aid FROM signup_log s, accounts a WHERE s.sell_aid = ? AND a.id = sell_aid");
                ps.setLong(1, sellerId);
                Vector data = new Vector();
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    ps1 = con.prepareStatement("SELECT users.username, accounts.description, accounts.plan_id,accounts.period_id, accounts.created,contact_info.email, billing_info.type as ptype, balance_credit.balance, balance_credit.credit FROM users, user_account, contact_info, accounts, balance_credit, billing_info WHERE  users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id AND balance_credit.id=accounts.id AND accounts.ci_id = contact_info.id AND accounts.id = ? AND accounts.reseller_id=?");
                    ps1.setLong(1, rs.getLong("client_aid"));
                    ps1.setLong(2, Session.getResellerId());
                    Session.getLog().debug("Client_aid=" + rs.getLong("client_aid"));
                    ResultSet rs1 = ps1.executeQuery();
                    if (rs1.next()) {
                        HashMap hashMap = new HashMap();
                        Plan p = Plan.getPlan(rs1.getInt("plan_id"));
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs1.getString("period_id")) + " " + p.getValue("_PERIOD_TYPE_" + rs1.getString("period_id")));
                        hashMap.put("username", rs1.getString("username"));
                        hashMap.put("acc_id", new Long(rs.getLong("client_aid")));
                        hashMap.put("description", rs1.getString("description"));
                        hashMap.put("created", rs1.getTimestamp("created"));
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("email", rs1.getString("email"));
                        hashMap.put("ptype", rs1.getString("ptype"));
                        hashMap.put("balance", new Double(rs1.getDouble("balance")));
                        hashMap.put("credit", new Double(rs1.getDouble("credit")));
                        data.add(hashMap);
                    }
                    ps1.close();
                }
                init(new DataContainer(data));
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se1) {
                        se1.printStackTrace();
                    }
                }
                Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
            } catch (SQLException se) {
                Session.getLog().error("error getting the report", se);
                throw se;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
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
