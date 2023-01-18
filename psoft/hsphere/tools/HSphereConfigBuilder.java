package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import psoft.hsinst.AbstractConfigurationBuilder;
import psoft.hsinst.LogicalServer;
import psoft.hsinst.ServerIP;
import psoft.hsinst.UnknownServerException;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/tools/HSphereConfigBuilder.class */
public class HSphereConfigBuilder extends AbstractConfigurationBuilder {
    protected static final HashMap ipFlagToType = new HashMap();

    static {
        ipFlagToType.put(String.valueOf(3), "system");
        ipFlagToType.put(String.valueOf(2), "shared");
        ipFlagToType.put(String.valueOf(1), "dedicated");
        ipFlagToType.put(String.valueOf(4), "service");
        ipFlagToType.put(String.valueOf(8), "resellerSSL");
        ipFlagToType.put(String.valueOf(6), "resellerDNS");
        ipFlagToType.put(String.valueOf(5), "resellerDNS");
        ipFlagToType.put(String.valueOf((int) HostEntry.TAKEN_VPS_IP), "vps");
    }

    public static String mapIPFlagToType(int flag) {
        String type = (String) ipFlagToType.get(String.valueOf(flag));
        if (type != null) {
            return type;
        }
        if (flag >= 10 && flag != 1000) {
            return "shared";
        }
        return null;
    }

    public void build() throws Exception {
        ExternalCP.initCP();
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        List webserversList = new ArrayList();
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT id, os_type, name, ip1, mask1, password FROM p_server");
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                addPhServer(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getString(4), rs.getString(5), Session.int2ext(rs.getString(4)), rs.getString(6));
            }
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("SELECT p_server_id, l.id, l.name, g.type_id  FROM l_server l, l_server_groups g WHERE l.group_id = g.id");
            ResultSet rs2 = ps3.executeQuery();
            while (rs2.next()) {
                LogicalServer ls = addLogicalServer(rs2.getInt(1), rs2.getInt(2), rs2.getString(3), HostEntry.getGroupNameByType(rs2.getInt(4)));
                if ("windows_hosting".equals(ls.getGroup()) || "unix_hosting".equals(ls.getGroup())) {
                    webserversList.add(new Long(ls.getId()));
                }
            }
            ps3.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT p.id, parent_ps_id FROM p_server p, loadbalanced_pserver l WHERE p.id = l.id AND p.id <> parent_ps_id");
            ResultSet rs3 = ps4.executeQuery();
            while (rs3.next()) {
                addPhServerSlave(rs3.getInt(1), rs3.getInt(2));
            }
            ps4.close();
            ps = con.prepareStatement("SELECT l_server_id, ip, mask, flag FROM l_server_ips ");
            ResultSet rs4 = ps.executeQuery();
            HashMap sharedIps = new HashMap();
            while (rs4.next()) {
                if (mapIPFlagToType(rs4.getInt(4)) != null) {
                    Long lServerId = new Long(rs4.getInt(1));
                    try {
                        ServerIP sIp = addServerIP(rs4.getInt(1), rs4.getString(2), rs4.getString(3), Session.int2ext(rs4.getString(2)), mapIPFlagToType(rs4.getInt(4)));
                        if (webserversList.contains(lServerId)) {
                            if ("shared".equals(mapIPFlagToType(rs4.getInt(4)))) {
                                sharedIps.put(lServerId, sIp);
                            }
                            if ("service".equals(mapIPFlagToType(rs4.getInt(4)))) {
                                sharedIps.remove(lServerId);
                            }
                        }
                    } catch (UnknownServerException e) {
                        System.err.println("Skipped ip " + rs4.getString(2) + " without corresponding logical server id:" + lServerId);
                    }
                }
            }
            for (Long lServerId2 : sharedIps.keySet()) {
                ServerIP sIp2 = (ServerIP) sharedIps.get(lServerId2);
                try {
                    addServerIP(lServerId2.intValue(), sIp2.getAddr(), sIp2.getMask(), sIp2.getIpExt(), "service");
                } catch (UnknownServerException e2) {
                    System.err.println("Skipped ip " + sIp2.getAddr() + " without corresponding logical server id:" + lServerId2);
                }
            }
            ps.close();
            ps = con.prepareStatement("select l_server_id, name, value from l_server_options;");
            ResultSet rs5 = ps.executeQuery();
            while (rs5.next()) {
                try {
                    addLogicalServerOption(rs5.getInt(1), rs5.getString(2), rs5.getString(3));
                } catch (UnknownServerException e3) {
                    System.err.println("Skipped option " + rs5.getString(2) + " without corresponding logical server id:" + rs5.getInt(1));
                }
            }
            ps.close();
            ps = con.prepareStatement("SELECT name FROM e_zones e, dns_zones d WHERE e.id = d.id AND reseller_id =1 ORDER BY e.id ");
            ResultSet rs6 = ps.executeQuery();
            if (rs6.next()) {
                getHsinst().setSystemzone(rs6.getString(1));
            }
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public static void main(String[] args) throws Exception {
        try {
            HSphereConfigBuilder hsBuilder = new HSphereConfigBuilder();
            System.out.println(hsBuilder.getConfig());
        } catch (Exception ex) {
            System.err.println("Unable to get config:" + ex.getMessage());
            ex.printStackTrace(System.err);
            System.exit(-1);
        }
    }
}
