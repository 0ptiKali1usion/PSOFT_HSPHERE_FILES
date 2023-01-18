package psoft.hsphere.report.adv;

import java.sql.Connection;
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
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/ContentMoving.class */
public class ContentMoving extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f125df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String username = (String) i.next();
        String accId = (String) i.next();
        String sServerId = (String) i.next();
        String tServerId = (String) i.next();
        String unCompletedOnly = (String) i.next();
        PreparedStatement ps = null;
        StringBuffer query = new StringBuffer("SELECT DISTINCT cm.id,cm.src, cm.target, u.username, p.account_id, ci.email AS email  FROM content_move cm, parent_child p, user_account ua, users u, expired_resources er, accounts a, contact_info ci  WHERE cm.id=er.cm_id AND er.rid=p.child_id AND p.account_id = ua.account_id AND ua.user_id = u.id and ua.account_id = a.id AND a.ci_id = ci.id");
        Connection con = Session.getDb("report");
        try {
            try {
                if (!isEmpty(username)) {
                    query.append(" AND UPPER(u.username) LIKE ?");
                }
                if (!isEmpty(accId)) {
                    query.append(" AND p.account_id = ?");
                }
                if (!isEmpty(sServerId)) {
                    query.append(" AND cm.src = ?");
                }
                if (!isEmpty(tServerId)) {
                    query.append(" AND cm.target = ?");
                }
                if (!isEmpty(unCompletedOnly)) {
                    query.append(" AND cm.finished IS NULL");
                }
                Session.getLog().debug("ContentMoving query is:\n" + query.toString());
                ps = con.prepareStatement(query.toString());
                int count = 1;
                if (!isEmpty(username)) {
                    count = 1 + 1;
                    ps.setString(1, username.toUpperCase());
                }
                if (!isEmpty(accId)) {
                    int i2 = count;
                    count++;
                    ps.setLong(i2, Long.parseLong(accId));
                }
                if (!isEmpty(sServerId)) {
                    int i3 = count;
                    count++;
                    ps.setLong(i3, Long.parseLong(sServerId));
                }
                if (!isEmpty(tServerId)) {
                    int i4 = count;
                    int i5 = count + 1;
                    ps.setLong(i4, Long.parseLong(tServerId));
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap map = new HashMap();
                    map.put("id", rs.getString("id"));
                    map.put("accId", rs.getString("account_id"));
                    map.put("accountId", new Long(rs.getLong("account_id")));
                    map.put("username", rs.getString("username"));
                    map.put("src", rs.getString("src"));
                    map.put("target", rs.getString("target"));
                    map.put("email", rs.getString("email"));
                    data.add(map);
                }
                init(new DataContainer(data));
                Session.closeStatement(ps);
                con.close();
                Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
            } catch (SQLException se) {
                Session.getLog().error("Error getting ContentMoving", se);
                throw se;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
