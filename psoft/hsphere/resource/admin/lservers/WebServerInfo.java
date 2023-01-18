package psoft.hsphere.resource.admin.lservers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.LogicalServerInfo;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/lservers/WebServerInfo.class */
public class WebServerInfo extends LogicalServerInfo {
    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getInfo(long lserverId) throws Exception {
        List list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM unix_user  WHERE hostid = ?");
            ps2.setLong(1, lserverId);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                Hashtable infos = new Hashtable();
                infos.put("name", "users");
                infos.put("value", Integer.toString(rs.getInt(1)));
                list.add(infos);
            }
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT count(*) FROM apache_vhost  WHERE host_id = ?");
            ps3.setLong(1, lserverId);
            ResultSet rs2 = ps3.executeQuery();
            if (rs2.next()) {
                Hashtable infos2 = new Hashtable();
                infos2.put("name", "hosts");
                infos2.put("value", Integer.toString(rs2.getInt(1)));
                list.add(infos2);
            }
            PreparedStatement ps4 = con.prepareStatement("SELECT count(*) FROM transfer_process  WHERE src_server = ? AND stage <> ?");
            ps4.setLong(1, lserverId);
            ps4.setInt(2, 8);
            ResultSet rs3 = ps4.executeQuery();
            if (rs3.next()) {
                Hashtable infos3 = new Hashtable();
                infos3.put("name", "src_cmove");
                infos3.put("value", Integer.toString(rs3.getInt(1)));
                list.add(infos3);
            }
            ps = con.prepareStatement("SELECT count(*) FROM transfer_process  WHERE target_server = ? AND stage <> ?");
            ps.setLong(1, lserverId);
            ps.setInt(2, 8);
            ResultSet rs4 = ps.executeQuery();
            if (rs4.next()) {
                Hashtable infos4 = new Hashtable();
                infos4.put("name", "target_cmove");
                infos4.put("value", Integer.toString(rs4.getInt(1)));
                list.add(infos4);
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public String getUsed(long lserverId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        boolean hasHosts = true;
        boolean hasCMoves = true;
        String result = "-1";
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM unix_user  WHERE hostid = ?");
                ps2.setLong(1, lserverId);
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(1) > 0) {
                        hasHosts = true;
                    } else {
                        hasHosts = false;
                    }
                }
                ps = con.prepareStatement("SELECT count(*) FROM transfer_process WHERE (src_server = ? OR target_server = ?) AND stage <> ?");
                ps.setLong(1, lserverId);
                ps.setLong(2, lserverId);
                ps.setInt(3, 8);
                ResultSet rs2 = ps.executeQuery();
                if (rs2.next()) {
                    if (rs2.getInt(1) > 0) {
                        hasCMoves = true;
                    } else {
                        hasCMoves = false;
                    }
                }
                if (hasHosts || hasCMoves) {
                    result = "1";
                } else {
                    result = "0";
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error getting LServer used", ex);
                Session.closeStatement(ps);
                con.close();
            }
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getIPTypes() throws Exception {
        List list = new ArrayList();
        list.add("0");
        list.add("2");
        list.add("4");
        return list;
    }
}
