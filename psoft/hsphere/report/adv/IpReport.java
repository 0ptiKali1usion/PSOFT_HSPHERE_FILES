package psoft.hsphere.report.adv;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import psoft.hsphere.Session;
import psoft.hsphere.report.AdvReport;
import psoft.hsphere.report.DataContainer;
import psoft.hsphere.resource.C0015IP;
import psoft.util.TimeUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/report/adv/IpReport.class */
public class IpReport extends AdvReport {
    @Override // psoft.hsphere.report.AdvReport
    public void init(List args) throws Exception {
        Session.getLog().debug("Begin " + TimeUtils.currentTimeMillis());
        Iterator i = args.iterator();
        String ip_from = (String) i.next();
        String ip_to = (String) i.next();
        String lserver = (String) i.next();
        String pserver = (String) i.next();
        String group = (String) i.next();
        String type = (String) i.next();
        StringBuffer query = new StringBuffer("SELECT l_server.id, l_server.name, l_server_ips.ip, l_server_ips.ip_num, l_server_ips.flag, l_server_ips.r_id FROM l_server, l_server_ips WHERE l_server.id = l_server_ips.l_server_id");
        if (!isEmpty(ip_from)) {
            query.append(" AND l_server_ips.ip_num >= ?");
        }
        if (!isEmpty(ip_to)) {
            query.append(" AND l_server_ips.ip_num <= ?");
        }
        if (!isEmpty(lserver)) {
            query.append(" AND l_server.id = ?");
        }
        if (!isEmpty(pserver)) {
            query.append(" AND l_server.p_server_id = ?");
        }
        if (!isEmpty(group)) {
            query.append(" AND l_server.group_id = ?");
        }
        if (!isEmpty(type)) {
            query.append(" AND l_server_ips.flag = ?");
        }
        Connection con = Session.getDb("report");
        PreparedStatement ps = null;
        try {
            try {
                int count = 1;
                ps = con.prepareStatement(query.toString());
                if (!isEmpty(ip_from)) {
                    count = 1 + 1;
                    ps.setLong(1, C0015IP.toLong(ip_from));
                }
                if (!isEmpty(ip_to)) {
                    int i2 = count;
                    count++;
                    ps.setLong(i2, C0015IP.toLong(ip_to));
                }
                if (!isEmpty(lserver)) {
                    int i3 = count;
                    count++;
                    ps.setInt(i3, Integer.parseInt(lserver));
                }
                if (!isEmpty(pserver)) {
                    int i4 = count;
                    count++;
                    ps.setInt(i4, Integer.parseInt(pserver));
                }
                if (!isEmpty(group)) {
                    int i5 = count;
                    count++;
                    ps.setInt(i5, Integer.parseInt(group));
                }
                if (!isEmpty(type)) {
                    int i6 = count;
                    int i7 = count + 1;
                    ps.setInt(i6, Integer.parseInt(type));
                }
                Session.getLog().debug("SQL:" + ps.toString());
                ResultSet rs = ps.executeQuery();
                Vector data = new Vector();
                while (rs.next()) {
                    HashMap hashMap = new HashMap();
                    hashMap.put("id", rs.getObject("id"));
                    hashMap.put("name", rs.getString("name"));
                    hashMap.put("ip", rs.getString("ip"));
                    hashMap.put("ip_num", rs.getObject("ip_num"));
                    hashMap.put("flag", rs.getString("flag"));
                    if (rs.getInt("flag") == 1) {
                        PreparedStatement ps1 = con.prepareStatement("SELECT domains.name as domain, type_name.description as type FROM domains, type_name, parent_child WHERE domains.id = parent_child.parent_id AND type_name.id = parent_child.parent_type AND parent_child.child_id = ?");
                        int i8 = 1 + 1;
                        ps1.setLong(1, rs.getLong("r_id"));
                        Session.getLog().debug("SQL:" + ps1.toString());
                        ResultSet rs1 = ps1.executeQuery();
                        if (rs1.next()) {
                            hashMap.put("domain", rs1.getString("domain"));
                            hashMap.put("type", rs1.getString("type"));
                        }
                    }
                    if (rs.getInt("flag") == 6) {
                        PreparedStatement ps12 = con.prepareStatement("SELECT dns_records.name as dns_record, users.username as reseller FROM dns_records, l_server_aliases, e_zones, users WHERE dns_records.id = ? AND l_server_aliases.l_server_id = ? AND l_server_aliases.e_dns_rec_id = dns_records.id AND e_zones.id = l_server_aliases.e_zone_id AND users.id = e_zones.reseller_id");
                        int count2 = 1 + 1;
                        ps12.setLong(1, rs.getLong("r_id"));
                        int i9 = count2 + 1;
                        ps12.setLong(count2, rs.getLong("id"));
                        Session.getLog().debug("SQL:" + ps12.toString());
                        ResultSet rs12 = ps12.executeQuery();
                        if (rs12.next()) {
                            hashMap.put("dns_record", rs12.getString("dns_record"));
                            hashMap.put(FMACLManager.RESELLER, rs12.getString(FMACLManager.RESELLER));
                        }
                    }
                    if (rs.getInt("flag") == 1001) {
                        PreparedStatement ps13 = con.prepareStatement("SELECT name FROM vps v, parent_child p WHERE p.child_id = ? AND p.parent_id = v.id;");
                        ps13.setLong(1, rs.getLong("r_id"));
                        ResultSet rs13 = ps13.executeQuery();
                        if (rs13.next()) {
                            hashMap.put("vps_name", rs13.getString(1));
                        }
                    }
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
            } catch (SQLException se) {
                Session.getLog().error("Error getting IpReport", se);
                throw se;
            }
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
    }

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0 || obj.equals("%");
    }
}
