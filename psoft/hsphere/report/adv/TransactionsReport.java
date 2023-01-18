package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/TransactionsReport.class */
public class TransactionsReport extends AdvReport {
    protected StringBuffer filter = new StringBuffer();

    /* renamed from: df */
    protected static final SimpleDateFormat f138df = new SimpleDateFormat("MM/dd/yyyy");

    protected Map accountInit(PreparedStatement userPs, PreparedStatement domainPs, long accountId, int planId, String periodId) throws Exception {
        String domain;
        String user;
        Map map = new HashMap();
        domainPs.setLong(1, accountId);
        ResultSet rs2 = domainPs.executeQuery();
        if (rs2.next()) {
            domain = rs2.getString(1);
            if (rs2.next()) {
                domain = domain + " , ...";
            }
        } else {
            domain = "";
        }
        userPs.setLong(1, accountId);
        ResultSet rs3 = userPs.executeQuery();
        if (rs3.next()) {
            user = rs3.getString(1);
        } else {
            user = "";
        }
        map.put("domain", domain);
        map.put("username", user);
        map.put("accountId", new Long(accountId));
        Plan p = Plan.getPlan(planId);
        if (p != null) {
            map.put("plan", p.getDescription());
            map.put("period", p.getValue("_PERIOD_SIZE_" + periodId) + " " + p.getValue("_PERIOD_TYPE_" + periodId));
        } else {
            map.put("plan", "Unknown Plan");
        }
        return map;
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Iterator i = args.iterator();
        Object account = i.next();
        Object username = i.next();
        Object plan = i.next();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        boolean onlyLive = "1".equals(i.next());
        StringBuffer buf = new StringBuffer("SELECT accounts.id, bill_entry.plan_id, bill_entry.period_id, bill_entry.type, bill_entry.amount, bill_entry.rid, bill_entry.created, billing_info.email FROM bill_entry, bill, accounts, billing_info" + (username != null ? ",user_account, users " : "") + " WHERE bill_entry.bill_id=bill.id AND accounts.bi_id=billing_info.id AND bill.account_id=accounts.id AND bi_id > 0 AND bill_entry.amount <> 0 AND bill_entry.canceled IS NULL AND accounts.reseller_id = ?");
        if (!isEmpty(createdBefore)) {
            this.filter.append(" AND bill_entry.created <= ?");
        }
        if (!isEmpty(createdAfter)) {
            this.filter.append(" AND bill_entry.created >= ?");
        }
        if (account != null) {
            this.filter.append(" AND accounts.id = ").append(account.toString());
        }
        if (plan != null) {
            this.filter.append(" AND bill_entry.plan_id= ").append(plan.toString());
        }
        if (onlyLive) {
            this.filter.append(" AND accounts.deleted IS NULL");
        }
        if (username != null) {
            this.filter.append(" AND accounts.id = user_account.account_id AND user_account.user_id = users.id AND UPPER(users.username) = '").append(username.toString().toUpperCase()).append("' ");
        }
        this.filter.append(" ORDER BY bill.account_id, bill_entry.created");
        buf.append(this.filter);
        Session.getLog().debug("TransactionsReport: query is\n" + buf.toString() + "\n");
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            try {
                ps = con.prepareStatement(buf.toString());
                int count = 1 + 1;
                ps.setLong(1, Session.getResellerId());
                if (!isEmpty(createdBefore)) {
                    count++;
                    ps.setDate(count, new Date(f138df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(createdAfter)) {
                    int i2 = count;
                    int i3 = count + 1;
                    ps.setDate(i2, new Date(f138df.parse(createdAfter).getTime()));
                }
                Session.getLog().info("-->Query: " + ((Object) buf));
                ps2 = con.prepareStatement("SELECT name FROM domains, parent_child WHERE domains.id = parent_child.child_id AND parent_child.account_id = ?");
                ps3 = con.prepareStatement("SELECT username FROM users, user_account WHERE users.id = user_account.user_id AND user_account.account_id = ?");
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                Map map = null;
                long accountId = 0;
                double setup = 0.0d;
                double regDomain = 0.0d;
                double usage = 0.0d;
                double recurrent = 0.0d;
                double refund = 0.0d;
                double charge = 0.0d;
                boolean z = "";
                while (rs.next()) {
                    if (accountId != rs.getLong(1)) {
                        if (map != null && (setup != 0.0d || regDomain != 0.0d || usage != 0.0d || recurrent != 0.0d || refund != 0.0d)) {
                            map.put("setup", new Double(setup));
                            map.put("reg_domain", new Double(regDomain));
                            map.put("recurrent", new Double(recurrent));
                            map.put("usage", new Double(usage));
                            map.put("refund", new Double(refund));
                            map.put("charge", new Double(charge));
                            setup = 0.0d;
                            regDomain = 0.0d;
                            usage = 0.0d;
                            recurrent = 0.0d;
                            refund = 0.0d;
                            charge = 0.0d;
                            z = "";
                        }
                        accountId = rs.getLong(1);
                        map = accountInit(ps3, ps2, accountId, rs.getInt(2), rs.getString(3));
                    }
                    double amount = rs.getDouble(5);
                    double amount2 = amount >= 0.0d ? amount : -amount;
                    switch (rs.getInt(4)) {
                        case 1:
                            if (rs.getLong(6) == 14) {
                                setup += amount2;
                                break;
                            } else {
                                regDomain += amount2;
                                break;
                            }
                        case 2:
                            recurrent += amount2;
                            break;
                        case 3:
                            usage += amount2;
                            break;
                        case 4:
                        case Resource.B_REFUND_ALL /* 104 */:
                            refund += amount2;
                            break;
                        case 5:
                        case 8:
                            z = "CC";
                            charge += amount2;
                            break;
                        case 6:
                        case 10:
                            z = "Check";
                            charge += amount2;
                            break;
                    }
                    if (rs.getInt(4) == 5 || rs.getInt(4) == 8 || rs.getInt(4) == 6 || rs.getInt(4) == 10) {
                        map.put("setup", new Double(setup));
                        map.put("reg_domain", new Double(regDomain));
                        map.put("recurrent", new Double(recurrent));
                        map.put("usage", new Double(usage));
                        map.put("refund", new Double(refund));
                        map.put("charge", new Double(charge));
                        map.put("type", z);
                        map.put("created", rs.getTimestamp(7));
                        map.put("email", rs.getString("email"));
                        data.add(map);
                        map = accountInit(ps3, ps2, accountId, rs.getInt(2), rs.getString(3));
                        setup = 0.0d;
                        regDomain = 0.0d;
                        usage = 0.0d;
                        recurrent = 0.0d;
                        refund = 0.0d;
                        charge = 0.0d;
                        z = "";
                    }
                }
                init(new DataContainer(data));
                this.data.reorder("created", true);
                Session.closeStatement(ps);
                Session.closeStatement(ps2);
                Session.closeStatement(ps3);
                con.close();
            } catch (Exception se) {
                Session.getLog().error("error getting the report", se);
                throw se;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            con.close();
            throw th;
        }
    }

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
