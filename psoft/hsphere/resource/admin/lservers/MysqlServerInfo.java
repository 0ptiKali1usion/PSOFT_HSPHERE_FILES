package psoft.hsphere.resource.admin.lservers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.LogicalServerInfo;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/lservers/MysqlServerInfo.class */
public class MysqlServerInfo extends LogicalServerInfo {
    @Override // psoft.hsphere.resource.admin.LogicalServerInfo
    public List getInfo(long lserverId) throws Exception {
        List list = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM mysqlres WHERE mysql_host_id = ?");
            ps2.setLong(1, lserverId);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                Hashtable infos = new Hashtable();
                infos.put("name", "mysql_res");
                infos.put("value", Integer.toString(rs.getInt(1)));
                list.add(infos);
            }
            ps = con.prepareStatement("select count(*) from parent_child p, mysqlres m where p.parent_id = m.id and m.mysql_host_id = ?");
            ps.setLong(1, lserverId);
            ResultSet rs2 = ps.executeQuery();
            if (rs2.next()) {
                int tmp = rs2.getInt(1);
                Hashtable infos2 = new Hashtable();
                if (tmp > 0) {
                    infos2.put("name", "mysql_dbs");
                    infos2.put("value", Integer.toString(tmp));
                    list.add(infos2);
                }
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
            try {
                ps = con.prepareStatement("SELECT count(*) FROM mysqlres WHERE mysql_host_id = ?");
                ps.setLong(1, lserverId);
                ResultSet rs = ps.executeQuery();
                if (!rs.next()) {
                    Session.closeStatement(ps);
                    con.close();
                    return "-1";
                }
                String str = rs.getInt(1) > 0 ? "1" : "0";
                Session.closeStatement(ps);
                con.close();
                return str;
            } catch (Exception ex) {
                Session.getLog().debug("Error in method 'MysqlServerInfo::getUsed(" + lserverId + ")' ", ex);
                Session.closeStatement(ps);
                con.close();
                return "-1";
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getFixed(long lserverId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        int usedServices = 0;
        int usedDBs = 0;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM mysqlres WHERE mysql_host_id = ?");
            ps2.setLong(1, lserverId);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                usedServices = rs.getInt(1);
            }
            ps2.close();
            ps = con.prepareStatement("select count(*) from parent_child p, mysqlres m where p.parent_id = m.id and m.mysql_host_id = ?");
            ps.setLong(1, lserverId);
            ResultSet rs2 = ps.executeQuery();
            if (rs2.next()) {
                usedDBs = rs2.getInt(1);
            }
            Session.closeStatement(ps);
            con.close();
            if (usedDBs == 0 && usedServices > 0) {
                return "1";
            }
            return "0";
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
