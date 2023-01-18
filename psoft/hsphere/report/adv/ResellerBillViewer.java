package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/ResellerBillViewer.class */
public class ResellerBillViewer extends AdvReport {
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        String domain;
        String username;
        Iterator i = args.iterator();
        long billId = Long.parseLong((String) i.next());
        long billEntryId = Long.parseLong((String) i.next());
        Bill bill = new Bill(billId);
        BillEntry entry = bill.getBillEntry(billEntryId);
        if (entry == null) {
            throw new HSUserException("resellerbillviewer.billentry");
        }
        Date started = entry.getStarted();
        Date ended = entry.getEnded();
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        try {
            try {
                String types = "";
                switch (entry.getType()) {
                    case 12:
                        types = "(" + Integer.toString(1) + ")";
                        break;
                    case 13:
                        types = "(" + Integer.toString(2) + ")";
                        break;
                    case 14:
                        types = "(" + Integer.toString(3) + ")";
                        break;
                    case 15:
                        String types2 = "(" + Integer.toString(4);
                        types = (types2 + "," + Integer.toString(10)) + "," + Integer.toString(Resource.B_REFUND_ALL) + ")";
                        break;
                }
                if ("".equals(types)) {
                    throw new HSUserException("resellerbillviewer.typeentry");
                }
                ps = con.prepareStatement("SELECT accounts.id, b.plan_id, b.period_id, b.created,b.type, b.amount, r.amount, r.description,b.started, b.ended, billing_info.email FROM bill_entry b, bill_entry_res r, bill,  accounts, billing_info WHERE b.bill_id = bill.id AND bill.account_id = accounts.id AND accounts.bi_id = billing_info.id AND (r.amount <> 0 OR b.amount <> 0) AND b.canceled IS NULL AND b.id = r.id  AND r.reseller_id = ? AND b.created >= ? AND b.created < ? AND b.type IN " + types);
                ps.setLong(1, Session.getUser().getId());
                ps.setTimestamp(2, new Timestamp(started.getTime()));
                ps.setTimestamp(3, new Timestamp(ended.getTime()));
                ResultSet rs = ps.executeQuery();
                ps2 = con.prepareStatement("SELECT name FROM domains, parent_child WHERE domains.id = parent_child.child_id AND parent_child.account_id = ?");
                ps3 = con.prepareStatement("SELECT username FROM users, user_account WHERE users.id = user_account.user_id AND user_account.account_id = ?");
                Vector data = new Vector();
                long oldResellerId = Session.getResellerId();
                try {
                    Session.setResellerId(Session.getUser().getId());
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
                            username = rs3.getString(1);
                        } else {
                            username = "";
                        }
                        Plan p = Plan.getPlan(rs.getInt(2));
                        hashMap.put("account", new Long(rs.getLong(1)));
                        if (p != null) {
                            hashMap.put("plan", p.getDescription());
                            hashMap.put("period", p.getValue("_PERIOD_SIZE_" + rs.getString(3)) + " " + p.getValue("_PERIOD_TYPE_" + rs.getString(3)));
                        } else {
                            hashMap.put("plan", "Unknown Plan");
                        }
                        hashMap.put("domain", domain);
                        hashMap.put("username", username);
                        hashMap.put("created", rs.getTimestamp(4));
                        hashMap.put("type", BillingReport.getTypeById(rs.getInt(5)));
                        hashMap.put("amount", new Double(rs.getDouble(6)));
                        hashMap.put("reseller_amount", new Double(rs.getDouble(7)));
                        hashMap.put("description", rs.getString(8));
                        hashMap.put("start", rs.getTimestamp(9));
                        hashMap.put("end", rs.getTimestamp(10));
                        hashMap.put("email", rs.getString(11));
                        data.add(hashMap);
                    }
                    init(new DataContainer(data));
                    Session.setResellerId(oldResellerId);
                    Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
                } catch (Throwable th) {
                    Session.setResellerId(oldResellerId);
                    throw th;
                }
            } catch (Exception se) {
                Session.getLog().error("Error getting ResellerBillViewer", se);
                throw se;
            }
        } finally {
            Session.closeStatement(ps);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException se1) {
                    se1.printStackTrace();
                }
            }
        }
    }
}
