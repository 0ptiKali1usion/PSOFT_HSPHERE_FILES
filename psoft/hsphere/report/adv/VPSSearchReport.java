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
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/VPSSearchReport.class */
public class VPSSearchReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f141df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String vpshostname = (String) i.next();
        String vpsIp = (String) i.next();
        String resId = (String) i.next();
        String userName = (String) i.next();
        String accId = (String) i.next();
        String planId = (String) i.next();
        String vpsIPAnd = "";
        String vpsIPFrom = "";
        if (Session.getResellerId() != 1) {
            resId = Long.toString(Session.getResellerId());
        }
        if (!isEmpty(vpsIp)) {
            vpsIPAnd = " AND pc1.parent_id = parent_child.child_id AND pc1.child_type = 7001 AND lsi.ip = ? AND lsi.r_id = pc1.child_id ";
            vpsIPFrom = ", parent_child pc1, l_server_ips lsi";
        }
        int count = 1;
        StringBuffer query = new StringBuffer("SELECT users.username, accounts.id as accountId, accounts.created, accounts.suspended, accounts.plan_id as planId, plans.description as planDescription, accounts.description as accountDescription, accounts.reseller_id as accResId, contact_info.email as email, vps.name as vpshostname FROM users, user_account, accounts, billing_info, plans, vps, parent_child, type_name" + vpsIPFrom + ", contact_info WHERE  parent_child.child_id = vps.id AND accounts.id = parent_child.account_id AND users.id = user_account.user_id AND user_account.account_id = accounts.id AND billing_info.id = accounts.bi_id " + vpsIPAnd + "AND contact_info.id = accounts.ci_id AND plans.id = accounts.plan_id AND parent_child.child_type = type_name.id");
        if (!isEmpty(vpshostname)) {
            query.append(" AND UPPER(vps.name) LIKE ? ");
        }
        if (!isEmpty(resId)) {
            query.append(" AND accounts.reseller_id = ?");
        }
        if (!isEmpty(userName)) {
            query.append(" AND users.username LIKE ?");
        }
        if (!isEmpty(accId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            query.append(" AND accounts.plan_id = ?");
        }
        query.append(" ORDER BY vpshostname");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query.toString());
                if (!isEmpty(vpshostname)) {
                    count = 1 + 1;
                    ps.setString(1, vpshostname.toUpperCase());
                }
                if (!isEmpty(vpsIp)) {
                    int i2 = count;
                    count++;
                    ps.setString(i2, vpsIp);
                }
                if (!isEmpty(resId)) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, resId);
                }
                if (!isEmpty(userName)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, userName);
                }
                if (!isEmpty(accId)) {
                    int i5 = count;
                    count++;
                    ps.setString(i5, accId);
                }
                if (!isEmpty(planId)) {
                    int i6 = count;
                    int i7 = count + 1;
                    ps.setString(i6, planId);
                }
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("accountId", new Long(rs.getLong("accountId")));
                    hashMap.put("created", rs.getTimestamp("created"));
                    hashMap.put("suspended", rs.getTimestamp("suspended"));
                    hashMap.put("planId", new Integer(rs.getInt("planId")));
                    long curResId = rs.getLong("accResId");
                    if (curResId == Session.getResellerId()) {
                        Plan p = Plan.getPlan(rs.getInt("planId"));
                        if (p != null) {
                            if (p.getBilling() == 0 && !"7".equals(p.getValue("_CREATED_BY_")) && p.getValue("_CREATED_BY_") != null) {
                                hashMap.put("without_bill", Localizer.translateMessage("advreport.withoutbilling"));
                            }
                            hashMap.put("planDescription", rs.getString("planDescription"));
                            hashMap.put("createdBy", p.getValue("_CREATED_BY_"));
                        } else {
                            hashMap.put("planDescription", Localizer.translateLabel("label.unknown_plan"));
                        }
                    } else {
                        hashMap.put("planDescription", "");
                    }
                    hashMap.put("accountDescription", rs.getString("accountDescription"));
                    hashMap.put("resellerId", new Long(curResId));
                    hashMap.put("resellerDescription", getResellerDescription(curResId));
                    hashMap.put("email", rs.getString("email"));
                    hashMap.put("vpshostname", rs.getString("vpshostname"));
                    data.add(hashMap);
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
        } catch (SQLException se) {
            Session.getLog().error("Error getting DomainSearchReport", se);
            throw se;
        }
    }

    protected String getResellerDescription(long id) {
        try {
            Reseller res = Reseller.getReseller(id);
            return res.getUser();
        } catch (Exception e) {
            Session.getLog().warn("Can't get info for reseller id " + Long.toString(id), e);
            return null;
        }
    }
}
