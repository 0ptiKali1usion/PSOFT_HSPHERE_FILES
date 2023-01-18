package psoft.hsphere.monitoring;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/NetSwitchManager.class */
public class NetSwitchManager {
    public static void portUp(String ip) throws Exception {
        PortInfo pi = getPortInfo(ip);
        exec(pi, "enable");
    }

    public static void portDown(String ip) throws Exception {
        PortInfo pi = getPortInfo(ip);
        exec(pi, "disable");
    }

    protected static void exec(PortInfo pi, String command) throws Exception {
        Session.getLog().info(Session.getProperty("SCRIPTS_SYSTEM_DIR"));
        Process p = Runtime.getRuntime().exec(Session.getProperty("SCRIPTS_SYSTEM_DIR") + "foundry-manage-interface.pl", new String[]{"--ip=" + pi.getIP(), "--iface=" + pi.getBlade() + "/" + pi.getPort(), "--command=" + command});
        PrintStream ps = new PrintStream(p.getOutputStream());
        ps.println(Session.getProperty("SWITCH_PASSWORD"));
        ps.flush();
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new Exception("Error contacting switch: " + exitCode);
        }
    }

    protected static PortInfo getPortInfo(String ip) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT management_ip, blade, port FROM net_switch WHERE ip = ?");
            ps.setString(1, ip);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                PortInfo portInfo = new PortInfo(rs.getString(1), rs.getInt(2), rs.getInt(3));
                Session.closeStatement(ps);
                con.close();
                return portInfo;
            }
            throw new Exception("IP " + ip + " does not belong to any switch");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }
}
