package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/SignupStat.class */
public class SignupStat extends AdvReport {
    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Connection con = null;
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            try {
                con = Session.getDb("report");
                ps = con.prepareStatement("SELECT sell_aid, COUNT(*) FROM signup_log, accounts WHERE client_aid = accounts.id AND reseller_id = ? GROUP BY sell_aid ORDER by sell_aid");
                ps.setLong(1, Session.getResellerId());
                Vector data = new Vector();
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    Session.getLog().debug("Collecting info about " + rs.getLong(1));
                    long qAll = rs.getLong(2);
                    ps2 = con.prepareStatement("SELECT COUNT(*) FROM signup_log, accounts WHERE client_aid = accounts.id AND deleted IS NULL AND sell_aid = ? AND reseller_id = ?");
                    ps2.setLong(1, rs.getLong(1));
                    ps2.setLong(2, Session.getResellerId());
                    ResultSet rs2 = ps2.executeQuery();
                    long qAlive = 0;
                    if (rs2.next()) {
                        qAlive = rs2.getLong(1);
                    }
                    ps3 = con.prepareStatement("SELECT username, description FROM users, user_account, accounts WHERE accounts.id = ? AND accounts.reseller_id = ? AND accounts.id = user_account.account_id AND user_account.user_id = users.id");
                    ps3.setLong(1, rs.getLong(1));
                    ps3.setLong(2, Session.getResellerId());
                    ResultSet rs3 = ps3.executeQuery();
                    HashMap hashMap = new HashMap();
                    hashMap.put("sell_id", new Long(rs.getLong(1)));
                    if (rs3.next()) {
                        hashMap.put("seller", rs3.getString(1));
                        hashMap.put("seller_acc", rs3.getString(2));
                    } else {
                        hashMap.put("seller", "deleted");
                        hashMap.put("seller_acc", "deleted");
                    }
                    hashMap.put("alive", new Long(qAlive));
                    hashMap.put("dead", new Long(qAll - qAlive));
                    hashMap.put("total", new Long(qAll));
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                setOrderParams("sell_id", true);
                try {
                    Session.closeStatement(ps);
                    Session.closeStatement(ps2);
                    Session.closeStatement(ps3);
                    if (con != null) {
                        con.close();
                    }
                } catch (SQLException se1) {
                    se1.printStackTrace();
                }
                Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
            } catch (SQLException se) {
                Session.getLog().error("error getting the report", se);
                throw se;
            }
        } catch (Throwable th) {
            try {
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                if (con != null) {
                    con.close();
                }
            } catch (SQLException se12) {
                se12.printStackTrace();
            }
            throw th;
        }
    }
}
