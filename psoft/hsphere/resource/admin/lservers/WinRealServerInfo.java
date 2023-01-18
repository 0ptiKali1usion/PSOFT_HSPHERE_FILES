package psoft.hsphere.resource.admin.lservers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.LogicalServerInfo;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/lservers/WinRealServerInfo.class */
public class WinRealServerInfo extends LogicalServerInfo {
    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getInfo(long lserverId) throws Exception {
        List list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT sum(min_con), sum(max_con) FROM real_server WHERE host_id = ?");
            ps.setLong(1, lserverId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Hashtable infos = new Hashtable();
                infos.put("name", "min_con");
                infos.put("value", Integer.toString(rs.getInt(1)));
                list.add(infos);
                Hashtable infos2 = new Hashtable();
                infos2.put("name", "max_con");
                infos2.put("value", Integer.toString(rs.getInt(2)));
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
            ps = con.prepareStatement("SELECT count(*) FROM real_server  WHERE host_id = ?");
            ps.setLong(1, lserverId);
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
        return list;
    }
}
