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
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/DSUserSearchReport.class */
public class DSUserSearchReport extends AdvReport {
    final String DS_PLAN_TYPE_NAME = "_CREATED_BY_";
    final String DS_PLAN_TYPE_VALUE = "dedicated_server";
    final int DS_RES_TYPE_ID = 7100;
    boolean isNewQuery;

    /* renamed from: df */
    protected static final SimpleDateFormat f126df = new SimpleDateFormat("MM/dd/yyyy");

    boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    void setNewQuery() {
        this.isNewQuery = true;
    }

    void appendQueryIfParam(String param, StringBuffer query, String expression) throws Exception {
        if (!isEmpty(param)) {
            if (this.isNewQuery) {
                query.append(" WHERE ");
                this.isNewQuery = false;
            } else {
                query.append(" AND ");
            }
            query.append(expression);
        }
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        String dsStatus = (String) i.next();
        String dsTemplate = (String) i.next();
        String dsOS = (String) i.next();
        String dsCPU = (String) i.next();
        String dsIP = (String) i.next();
        String dsID = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String takenAfter = (String) i.next();
        String takenBefore = (String) i.next();
        String userLogin = (String) i.next();
        String accountId = (String) i.next();
        String planId = (String) i.next();
        String selectedResellerId = (String) i.next();
        setNewQuery();
        StringBuffer query = new StringBuffer("SELECT u.username, a.id as account_id, a.created as account_created, a.suspended as account_suspended, a.p_end as account_p_end, p.id as plan_id, p.description as plan_description, bi.type as b_type, ci.email as ci_email, bc.balance, bc.credit, ds.name as ds_name, ds.id as ds_id, ds.state as ds_state, ds.internal_id as ds_internal_id, ds.ip as ds_ip, ds.created as ds_created, ds.taken as ds_taken FROM (((((((( accounts a JOIN plans p ON (a.reseller_id = ? AND a.deleted IS null AND a.plan_id = p.id) ) JOIN plan_value pv ON (p.id = pv.plan_id AND pv.name = ? AND pv.value = ?) ) JOIN user_account ua ON a.id = ua.account_id ) JOIN users u ON ua.user_id = u.id ) LEFT JOIN billing_info bi ON a.bi_id = bi.id ) LEFT JOIN contact_info ci ON a.ci_id = ci.id ) LEFT JOIN balance_credit bc ON a.id = bc.id ) LEFT JOIN parent_child pc ON (a.id = pc.account_id AND pc.child_type = ?) ) LEFT JOIN dedicated_servers ds ON pc.child_id = ds.rid");
        appendQueryIfParam(dsStatus, query, "ds.state = ?");
        appendQueryIfParam(dsTemplate, query, "ds.template_id = ?");
        appendQueryIfParam(dsOS, query, "UPPER(ds.os) LIKE ?");
        appendQueryIfParam(dsCPU, query, "UPPER(ds.cpu) LIKE ?");
        appendQueryIfParam(dsIP, query, "UPPER(ds.ip) LIKE ?");
        appendQueryIfParam(dsID, query, "UPPER(ds.internal_id) LIKE ?");
        appendQueryIfParam(createdAfter, query, "ds.created > ?");
        appendQueryIfParam(createdBefore, query, "ds.created < ?");
        appendQueryIfParam(takenAfter, query, "ds.taken > ?");
        appendQueryIfParam(takenBefore, query, "ds.taken < ?");
        appendQueryIfParam(userLogin, query, "UPPER(u.username) LIKE ?");
        appendQueryIfParam(accountId, query, "a.id = ?");
        appendQueryIfParam(planId, query, "p.id = ?");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                String tmpQ = query.toString();
                Session.getLog().debug("query is --> " + tmpQ);
                ps = con.prepareStatement(tmpQ);
                long resellerId = Session.getResellerId();
                if (resellerId == 1 && selectedResellerId != null && !"".equals(selectedResellerId)) {
                    try {
                        resellerId = Long.parseLong(selectedResellerId);
                    } catch (Exception ex) {
                        Session.getLog().debug("Unable to parse the reseller_id field: [" + selectedResellerId + "]", ex);
                    }
                }
                int count = 1 + 1;
                ps.setLong(1, resellerId);
                int count2 = count + 1;
                ps.setString(count, "_CREATED_BY_");
                int count3 = count2 + 1;
                ps.setString(count2, "dedicated_server");
                int count4 = count3 + 1;
                ps.setInt(count3, 7100);
                if (!isEmpty(dsStatus)) {
                    count4++;
                    ps.setInt(count4, DedicatedServer.getIntState(dsStatus));
                }
                if (!isEmpty(dsTemplate)) {
                    int i2 = count4;
                    count4++;
                    ps.setLong(i2, Long.parseLong(dsTemplate));
                }
                if (!isEmpty(dsOS)) {
                    int i3 = count4;
                    count4++;
                    ps.setString(i3, dsOS.toUpperCase());
                }
                if (!isEmpty(dsCPU)) {
                    int i4 = count4;
                    count4++;
                    ps.setString(i4, dsCPU.toUpperCase());
                }
                if (!isEmpty(dsIP)) {
                    int i5 = count4;
                    count4++;
                    ps.setString(i5, dsIP.toUpperCase());
                }
                if (!isEmpty(dsID)) {
                    int i6 = count4;
                    count4++;
                    ps.setString(i6, dsID.toUpperCase());
                }
                if (!isEmpty(createdAfter)) {
                    int i7 = count4;
                    count4++;
                    ps.setDate(i7, new Date(f126df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i8 = count4;
                    count4++;
                    ps.setDate(i8, new Date(f126df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(takenAfter)) {
                    int i9 = count4;
                    count4++;
                    ps.setDate(i9, new Date(f126df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(takenBefore)) {
                    int i10 = count4;
                    count4++;
                    ps.setDate(i10, new Date(f126df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(userLogin)) {
                    int i11 = count4;
                    count4++;
                    ps.setString(i11, userLogin.toUpperCase());
                }
                if (!isEmpty(accountId)) {
                    int i12 = count4;
                    count4++;
                    ps.setLong(i12, Long.parseLong(accountId));
                }
                if (!isEmpty(planId)) {
                    int i13 = count4;
                    int i14 = count4 + 1;
                    ps.setLong(i13, Integer.parseInt(planId));
                }
                ResultSet rs = ps.executeQuery();
                List data = new ArrayList();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("username", rs.getString("username"));
                    hashMap.put("account_id", rs.getString("account_id"));
                    hashMap.put("account_created", rs.getTimestamp("account_created"));
                    hashMap.put("account_suspended", rs.getString("account_suspended"));
                    hashMap.put("account_p_end", rs.getString("account_p_end"));
                    hashMap.put("plan_id", rs.getString("plan_id"));
                    hashMap.put("plan_description", rs.getString("plan_description"));
                    hashMap.put("b_type", rs.getString("b_type"));
                    hashMap.put("ci_email", rs.getString("ci_email"));
                    hashMap.put("balance", rs.getString("balance"));
                    hashMap.put("credit", rs.getString("credit"));
                    long dsId = rs.getLong("ds_id");
                    if (dsId > 0) {
                        hashMap.put("ds_name", rs.getString("ds_name"));
                        hashMap.put("ds_id", new Long(dsId));
                        hashMap.put("ds_state", DedicatedServer.getStrState(rs.getInt("ds_state")));
                        hashMap.put("ds_internal_id", rs.getString("ds_internal_id"));
                        hashMap.put("ds_ip", rs.getString("ds_ip"));
                        hashMap.put("ds_created", rs.getTimestamp("ds_created"));
                        hashMap.put("ds_taken", rs.getTimestamp("ds_taken"));
                    } else {
                        hashMap.put("ds_name", "");
                        hashMap.put("ds_id", null);
                        hashMap.put("ds_state", "");
                        hashMap.put("ds_internal_id", "");
                        hashMap.put("ds_ip", "");
                        hashMap.put("ds_created", null);
                        hashMap.put("ds_taken", null);
                    }
                    data.add(hashMap);
                }
                init(new DataContainer(data));
                FM_reorder("account_id", true);
                FM_reorder("ds_id", true);
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
            Session.getLog().error("Error getting DedicatedServersReport", se);
            throw se;
        }
    }
}
