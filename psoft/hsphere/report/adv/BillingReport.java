package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/BillingReport.class */
public class BillingReport extends AdvReport {
    protected StringBuffer filter = new StringBuffer();

    /* renamed from: df */
    protected static final SimpleDateFormat f122df = new SimpleDateFormat("MM/dd/yyyy");

    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        String domain;
        String user;
        Iterator i = args.iterator();
        Object account = i.next();
        Object username = i.next();
        Object plan = i.next();
        Object type = i.next();
        String createdBefore = (String) i.next();
        String createdAfter = (String) i.next();
        boolean onlyLive = "1".equals(i.next());
        boolean zero = "1".equals(i.next());
        String zeroAmount = "";
        if (!zero) {
            zeroAmount = "AND bill_entry.amount <> 0 ";
        }
        StringBuffer buf = new StringBuffer("SELECT accounts.id, bill_entry.plan_id, bill_entry.period_id, bill_entry.created, bill_entry.type, bill_entry.amount, started, ended, billing_info.email, bill_entry.description FROM bill_entry, bill, accounts, billing_info " + (username != null ? ",user_account, users " : "") + "WHERE bill_entry.bill_id = bill.id AND bill.account_id = accounts.id AND accounts.bi_id=billing_info.id AND bi_id > 0 " + zeroAmount + "AND bill_entry.canceled IS NULL AND accounts.reseller_id = ? ");
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
        if (type != null) {
            if (Integer.toString(5).equals(type.toString())) {
                this.filter.append(" AND bill_entry.type IN (").append(5).append(",").append(8).append(")");
            } else if ("-1".equals(type.toString())) {
                this.filter.append(" AND bill_entry.type IN (").append(13).append(",").append(12).append(",").append(14).append(",").append(15).append(")");
            } else {
                this.filter.append(" AND bill_entry.type = ").append(type.toString());
            }
        }
        if (onlyLive) {
            this.filter.append(" AND accounts.deleted IS NULL");
        }
        if (username != null) {
            this.filter.append(" AND accounts.id = user_account.account_id AND user_account.user_id = users.id AND UPPER(users.username) = '").append(username.toString().toUpperCase()).append("' ");
        }
        buf.append(this.filter).append(" ORDER BY bill_entry.id");
        Session.getLog().debug("BillingReport: QUERRY IS \n" + buf.toString() + "\n");
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
                    ps.setDate(count, new Date(f122df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(createdAfter)) {
                    int i2 = count;
                    int i3 = count + 1;
                    ps.setDate(i2, new Date(f122df.parse(createdAfter).getTime()));
                }
                Session.getLog().info("-->Query: " + ((Object) buf));
                ps2 = con.prepareStatement("SELECT name FROM domains, parent_child WHERE domains.id = parent_child.child_id AND parent_child.account_id = ?");
                ps3 = con.prepareStatement("SELECT username FROM users, user_account WHERE users.id = user_account.user_id AND user_account.account_id = ?");
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    ps2.setLong(1, rs.getLong(1));
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        domain = rs2.getString(1);
                        if (rs2.next()) {
                            domain = domain + " , ...";
                        }
                    } else {
                        domain = "";
                    }
                    ps3.setLong(1, rs.getLong(1));
                    ResultSet rs3 = ps3.executeQuery();
                    if (rs3.next()) {
                        user = rs3.getString(1);
                    } else {
                        user = "";
                    }
                    double amount = rs.getDouble(6);
                    Plan p = Plan.getPlan(rs.getInt(2));
                    hashMap.put("accountId", new Long(rs.getLong(1)));
                    hashMap.put("description", rs.getString("description"));
                    if (p != null) {
                        hashMap.put("plan", p.getDescription());
                        hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString(3)) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString(3)));
                    } else {
                        hashMap.put("plan", "Unknown Plan");
                    }
                    hashMap.put("domain", domain);
                    hashMap.put("username", user);
                    hashMap.put("created", rs.getTimestamp(4));
                    hashMap.put("type", getTypeById(rs.getInt(5)));
                    hashMap.put("amount", new Double(amount));
                    hashMap.put("start", rs.getTimestamp(7));
                    hashMap.put("end", rs.getTimestamp(8));
                    hashMap.put("email", rs.getString(9));
                    data.add(hashMap);
                }
                Session.getLog().debug("Field in data");
                init(new DataContainer(data));
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

    public static String getTypeById(int type) {
        String sType;
        switch (type) {
            case 1:
                sType = Localizer.translateLabel("admin.searchbill.setup");
                break;
            case 2:
                sType = Localizer.translateLabel("admin.searchbill.recur");
                break;
            case 3:
                sType = Localizer.translateLabel("admin.searchbill.usage");
                break;
            case 4:
                sType = Localizer.translateLabel("admin.searchbill.refund");
                break;
            case 5:
                sType = Localizer.translateLabel("admin.searchbill.charge");
                break;
            case 6:
                sType = Localizer.translateLabel("admin.searchbill.credit");
                break;
            case 7:
            case 9:
            default:
                sType = Localizer.translateLabel("admin.searchbill.unknown");
                break;
            case 8:
                sType = Localizer.translateLabel("admin.searchbill.charge");
                break;
            case 10:
                sType = Localizer.translateLabel("admin.searchbill.moneyback");
                break;
            case 11:
                sType = Localizer.translateLabel("admin.searchbill.debit");
                break;
            case 12:
                sType = Localizer.translateLabel("admin.searchbill.sumsetup");
                break;
            case 13:
                sType = Localizer.translateLabel("admin.searchbill.sumrecur");
                break;
            case 14:
                sType = Localizer.translateLabel("admin.searchbill.sumusage");
                break;
            case 15:
                sType = Localizer.translateLabel("admin.searchbill.sumrefund");
                break;
        }
        return sType;
    }

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
