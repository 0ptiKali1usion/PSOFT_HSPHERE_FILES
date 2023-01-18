package psoft.hsphere.resource.admin.lservers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.LogicalServerInfo;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/lservers/NameServerInfo.class */
public class NameServerInfo extends LogicalServerInfo {
    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getInfo(long lserverId) throws Exception {
        List list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM dns_zones WHERE master = ?");
            ps2.setLong(1, lserverId);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                Hashtable infos = new Hashtable();
                infos.put("name", "masters");
                infos.put("value", Integer.toString(rs.getInt(1)));
                list.add(infos);
            }
            ps2.close();
            ps = con.prepareStatement("SELECT count(*) FROM  dns_zones  WHERE slave1 = ? OR slave2 = ?");
            ps.setLong(1, lserverId);
            ps.setLong(2, lserverId);
            ResultSet rs2 = ps.executeQuery();
            if (rs2.next()) {
                Hashtable infos2 = new Hashtable();
                infos2.put("name", "slaves");
                infos2.put("value", Integer.toString(rs2.getInt(1)));
                list.add(infos2);
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
        try {
            ps = con.prepareStatement("SELECT count(*) FROM dns_zones WHERE master = ? OR slave1 = ? OR slave2 = ?");
            ps.setLong(1, lserverId);
            ps.setLong(2, lserverId);
            ps.setLong(3, lserverId);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                Session.closeStatement(ps);
                con.close();
                return "-1";
            } else if (rs.getInt(1) > 0) {
                Session.closeStatement(ps);
                con.close();
                return "1";
            } else {
                Session.closeStatement(ps);
                con.close();
                return "0";
            }
        } catch (Exception e) {
            Session.closeStatement(ps);
            con.close();
            return "-1";
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getIPTypes() throws Exception {
        List list = new ArrayList();
        list.add("4");
        list.add("5");
        return list;
    }
}
