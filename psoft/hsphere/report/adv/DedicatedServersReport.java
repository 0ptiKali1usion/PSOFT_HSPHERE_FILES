package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.p001ds.DSNetInterface;
import psoft.hsphere.p001ds.DSNetInterfaceManager;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/DedicatedServersReport.class */
public class DedicatedServersReport extends AdvReport {

    /* renamed from: df */
    protected static final SimpleDateFormat f127df = new SimpleDateFormat("MM/dd/yyyy");

    public boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }

    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
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
        String takenByUser = (String) i.next();
        String netSwitchId = (String) i.next();
        boolean niIncluded = !isEmpty(netSwitchId);
        DSNetInterfaceManager netInterfaceMan = null;
        StringBuffer query = new StringBuffer("SELECT d.id, d.internal_id, d.ip, d.name, d.template_id, d.state, d.created, d.taken, d.cancellation, p.account_id, u.username, u.reseller_id FROM (dedicated_servers as d LEFT JOIN parent_child as p ON (d.rid=p.child_id)) LEFT JOIN (user_account as ua JOIN users as u on (ua.user_id=u.id)) ON (ua.account_id=p.account_id) WHERE d.reseller_id = ?");
        if (!isEmpty(dsStatus)) {
            query.append(" AND d.state = ?");
        }
        if (!isEmpty(dsTemplate)) {
            query.append(" AND d.template_id = ?");
        }
        if (!isEmpty(dsOS)) {
            query.append(" AND UPPER(d.os) LIKE ?");
        }
        if (!isEmpty(dsCPU)) {
            query.append(" AND UPPER(d.cpu) LIKE ?");
        }
        if (!isEmpty(dsIP)) {
            query.append(" AND UPPER(d.ip) LIKE ?");
        }
        if (!isEmpty(dsID)) {
            query.append(" AND UPPER(d.internal_id) LIKE ?");
        }
        if (!isEmpty(createdAfter)) {
            query.append(" AND d.created > ?");
        }
        if (!isEmpty(createdBefore)) {
            query.append(" AND d.created < ? ");
        }
        if (!isEmpty(takenAfter)) {
            query.append(" AND d.taken > ?");
        }
        if (!isEmpty(takenBefore)) {
            query.append(" AND d.taken < ? ");
        }
        if (!isEmpty(takenByUser)) {
            query.append(" AND UPPER(u.username) LIKE ?");
        }
        if (niIncluded) {
            query.append(" AND EXISTS (SELECT * from ds_netinterfaces WHERE ds_id = d.id AND switch_id = ? AND deleted is null)");
        }
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                String tmpQ = query.toString();
                Session.getLog().debug("query is --> " + tmpQ);
                ps = con.prepareStatement(tmpQ);
                long sessionResellerId = Session.getResellerId();
                int count = 1 + 1;
                ps.setLong(1, sessionResellerId);
                if (!isEmpty(dsStatus)) {
                    count++;
                    ps.setInt(count, DedicatedServer.getIntState(dsStatus));
                }
                if (!isEmpty(dsTemplate)) {
                    int i2 = count;
                    count++;
                    ps.setLong(i2, Long.parseLong(dsTemplate));
                }
                if (!isEmpty(dsOS)) {
                    int i3 = count;
                    count++;
                    ps.setString(i3, dsOS.toUpperCase());
                }
                if (!isEmpty(dsCPU)) {
                    int i4 = count;
                    count++;
                    ps.setString(i4, dsCPU.toUpperCase());
                }
                if (!isEmpty(dsIP)) {
                    int i5 = count;
                    count++;
                    ps.setString(i5, dsIP.toUpperCase());
                }
                if (!isEmpty(dsID)) {
                    int i6 = count;
                    count++;
                    ps.setString(i6, dsID.toUpperCase());
                }
                if (!isEmpty(createdAfter)) {
                    int i7 = count;
                    count++;
                    ps.setDate(i7, new Date(f127df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(createdBefore)) {
                    int i8 = count;
                    count++;
                    ps.setDate(i8, new Date(f127df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(takenAfter)) {
                    int i9 = count;
                    count++;
                    ps.setDate(i9, new Date(f127df.parse(createdAfter).getTime()));
                }
                if (!isEmpty(takenBefore)) {
                    int i10 = count;
                    count++;
                    ps.setDate(i10, new Date(f127df.parse(createdBefore).getTime()));
                }
                if (!isEmpty(takenByUser)) {
                    int i11 = count;
                    count++;
                    ps.setString(i11, takenByUser.toUpperCase());
                }
                if (niIncluded) {
                    int i12 = count;
                    int i13 = count + 1;
                    ps.setLong(i12, Long.parseLong(netSwitchId));
                    netInterfaceMan = (DSNetInterfaceManager) Session.getCacheFactory().getCache(DSNetInterface.class);
                }
                ResultSet rs = ps.executeQuery();
                List data = new ArrayList();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    long dsId = rs.getLong("id");
                    hashMap.put("ds_id", new Long(dsId));
                    hashMap.put("ds_internal_id", rs.getString("internal_id"));
                    hashMap.put("ds_ip", rs.getString("ip"));
                    hashMap.put("ds_name", rs.getString("name"));
                    hashMap.put("ds_template", Integer.valueOf(rs.getInt("template_id")));
                    hashMap.put("ds_state", DedicatedServer.getStrState(rs.getInt("state")));
                    hashMap.put("ds_added", rs.getTimestamp("created"));
                    hashMap.put("ds_taken", rs.getTimestamp("taken"));
                    Timestamp cancellation = rs.getTimestamp("cancellation");
                    if (cancellation != null) {
                        hashMap.put("ds_cancel", cancellation);
                    }
                    long accountId = rs.getLong("account_id");
                    if (accountId > 0) {
                        long userResellerId = rs.getLong("reseller_id");
                        if (userResellerId != sessionResellerId && sessionResellerId == 1 && userResellerId > 0) {
                            try {
                                hashMap.put(FMACLManager.RESELLER, Reseller.getReseller(userResellerId).getUser());
                            } catch (UnknownResellerException e) {
                                Session.getLog().debug("Unable to get reseller #" + userResellerId);
                            }
                        }
                        hashMap.put("ds_account_id", String.valueOf(accountId));
                    }
                    hashMap.put("ds_username", rs.getString("username"));
                    if (niIncluded) {
                        hashMap.put("ds_netinterfaces", new TemplateList(netInterfaceMan.getInterfacesByDS(dsId)));
                    }
                    data.add(hashMap);
                }
                init(new DataContainer(data));
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
