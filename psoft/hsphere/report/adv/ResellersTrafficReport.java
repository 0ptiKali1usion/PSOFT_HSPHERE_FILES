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
import psoft.hsphere.Account;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/ResellersTrafficReport.class */
public class ResellersTrafficReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f133df = new SimpleDateFormat("MM/dd/yyyy");
    protected Connection con;

    public ResellersTrafficReport() {
        try {
            this.con = Session.getDb("report");
        } catch (Exception e) {
            Session.getLog().error("Error open connection", e);
        }
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String accountId = (String) i.next();
        String planId = (String) i.next();
        String email = (String) i.next();
        String username = (String) i.next();
        String createdAfter = (String) i.next();
        String createdBefore = (String) i.next();
        String ttype = (String) i.next();
        String reseller = (String) i.next();
        boolean isDetail = !isEmpty((String) i.next());
        StringBuffer query = new StringBuffer();
        if (!isEmpty(accountId) || !isEmpty(username)) {
            isDetail = true;
        }
        if (!isDetail) {
            query.append("SELECT accounts.reseller_id, SUM(trans_log.xfer) as xfer FROM accounts, trans_log WHERE trans_log.account_id = accounts.id ");
        } else {
            query.append("SELECT accounts.id as accountId, accounts.reseller_id, SUM(trans_log.xfer) as xfer FROM accounts, trans_log WHERE trans_log.account_id = accounts.id ");
        }
        if (!isEmpty(accountId)) {
            query.append(" AND accounts.id = ?");
        }
        if (!isEmpty(planId)) {
            query.append(" AND accounts.plan_id = ?");
        }
        if (!isEmpty(email) && isEmpty(accountId) && isDetail) {
            query.append(" AND EXISTS (SELECT id FROM contact_info WHERE accounts.ci_id = contact_info.id AND email LIKE ?)");
        }
        if (!isEmpty(username) && isEmpty(accountId)) {
            query.append(" AND EXISTS (SELECT ua.account_id FROM users u, user_account ua WHERE accounts.id = ua.account_id AND u.id = ua.user_id AND UPPER(u.username) LIKE ?)");
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND trans_log.cdate >= ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND trans_log.cdate <= ?");
        }
        if (!isEmpty(ttype)) {
            query.append(" AND trans_log.tt_type = ?");
        }
        if (!isEmpty(reseller)) {
            query.append(" AND accounts.reseller_id = ?");
        }
        if (Session.useAccelerator() && Session.hasFlagField()) {
            query.append(" AND trans_log.flag != 0 ");
        }
        if (isDetail) {
            query.append("");
        }
        if (!isDetail) {
            query.append(" GROUP BY accounts.reseller_id");
            query.append(" HAVING SUM(xfer) > 0");
            query.append(" ORDER BY xfer DESC");
        } else {
            query.append(" GROUP BY accounts.id, accounts.reseller_id");
            query.append(" HAVING SUM(xfer) > 0");
            query.append(" ORDER BY xfer DESC");
        }
        PreparedStatement ps = null;
        long resellerId = Session.getResellerId();
        try {
            try {
                int count = 1;
                ps = this.con.prepareStatement(query.toString());
                if (!isEmpty(accountId)) {
                    count = 1 + 1;
                    ps.setLong(1, Long.parseLong(accountId));
                }
                if (!isEmpty(planId)) {
                    int i2 = count;
                    count++;
                    ps.setInt(i2, Integer.parseInt(planId));
                }
                if (!isEmpty(email) && isDetail) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, email);
                }
                if (!isEmpty(username)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, "%" + username.toUpperCase() + "%");
                }
                if (!isEmpty(createdAfter)) {
                    int i5 = count;
                    count++;
                    ps.setDate(i5, new Date(f133df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i6 = count;
                    count++;
                    ps.setDate(i6, new Date(f133df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(ttype)) {
                    int i7 = count;
                    count++;
                    ps.setInt(i7, Integer.parseInt(ttype));
                }
                if (!isEmpty(reseller)) {
                    int i8 = count;
                    int i9 = count + 1;
                    ps.setInt(i8, Integer.parseInt(reseller));
                }
                Session.getLog().debug("SQL:" + ps.toString());
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                long lastReseller = resellerId;
                while (rs.next()) {
                    try {
                        if (rs.getLong("reseller_id") != lastReseller) {
                            lastReseller = rs.getLong("reseller_id");
                            Session.setResellerId(lastReseller);
                        }
                        HashMap hashMap = new HashMap();
                        if (!isDetail) {
                            long id = rs.getLong("reseller_id");
                            hashMap.put("username", User.getUser(id).getLogin());
                            if (id == 1) {
                                hashMap.put("accountId", new Long(1L));
                            } else {
                                hashMap.put("accountId", new Long(Reseller.getReseller(rs.getLong("reseller_id")).getAccount().getId().getId()));
                            }
                            hashMap.put(FMACLManager.RESELLER, User.getUser(1L).getLogin());
                            hashMap.put("xfer", new Double(rs.getDouble("xfer")));
                        } else {
                            long id2 = rs.getLong("accountId");
                            Account account = Account.getAccount(id2);
                            User user = account.getUser();
                            if (user != null) {
                                hashMap.put("username", user.getLogin());
                            } else {
                                hashMap.put("username", Localizer.translateLabel("search.delusr"));
                            }
                            hashMap.put("accountId", new Long(id2));
                            hashMap.put(FMACLManager.RESELLER, getResellerDescription(account.getResellerId()));
                            hashMap.put("plan", account.getPlan());
                            hashMap.put("email", account.getContactInfo().getEmail());
                            hashMap.put("xfer", new Double(rs.getDouble("xfer")));
                        }
                        data.add(hashMap);
                    } catch (Exception e) {
                    }
                }
                init(new DataContainer(data));
                setOrderParams("xfer", false);
                if (Session.getResellerId() != resellerId) {
                    Session.setResellerId(resellerId);
                }
                Session.closeStatement(ps);
                try {
                    if (this.con != null) {
                        this.con.close();
                    }
                } catch (SQLException se1) {
                    se1.printStackTrace();
                }
                Session.getLog().debug("End " + TimeUtils.currentTimeMillis());
            } catch (SQLException se) {
                Session.getLog().error("Error getting ResellersTrafficReport", se);
                throw se;
            }
        } catch (Throwable th) {
            if (Session.getResellerId() != resellerId) {
                Session.setResellerId(resellerId);
            }
            Session.closeStatement(ps);
            try {
                if (this.con != null) {
                    this.con.close();
                }
            } catch (SQLException se12) {
                se12.printStackTrace();
            }
            throw th;
        }
    }

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    protected String getResellerDescription(long id) {
        try {
            Reseller res = Reseller.getReseller(id);
            return res.getUser();
        } catch (Exception e) {
            Session.getLog().warn("Can't get reseller info", e);
            return null;
        }
    }
}
