package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import org.apache.regexp.RE;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.DNSServices;
import psoft.p000db.Database;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/DNSCreator.class */
public class DNSCreator extends C0004CP {
    static boolean isDbWise;
    static boolean delz;
    static PreparedStatement srvIdsPs = null;
    static String targetZone = "";
    static String serverIDs = "";
    static boolean toConsol = false;
    protected static final int SLAVE_ABSENT = 0;
    static final int ERROR = 1;
    static final int WARNING = 2;
    static final int INFORMATION = 3;
    static final int DEBUG = 4;
    static final String ipCheckStr = "(([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])\\.){3}([0-9][0-9]?|[0-1][0-9][0-9]|2[0-4][0-9]|25[0-5])";

    public DNSCreator() throws Exception {
        super("psoft_config.hsphere");
    }

    /* JADX WARN: Finally extract failed */
    public static void main(String[] argv) throws Exception {
        boolean configured = false;
        boolean delZones = false;
        int cMethode = 1;
        String zone = "";
        List lServers = new ArrayList();
        toConsol = true;
        int i = 0;
        while (i < argv.length) {
            if ("-m".equals(argv[i]) || "--method".equals(argv[i])) {
                if ("db".equals(argv[i + 1])) {
                    cMethode = 1;
                    configured = true;
                    i++;
                } else if ("rand".equals(argv[i + 1])) {
                    cMethode = 2;
                    configured = true;
                    i++;
                }
            } else if ("-dz".equals(argv[i]) || "--delete_zones".equals(argv[i])) {
                delZones = true;
            } else if ("-lids".equals(argv[i]) || "--logical-servers".equals(argv[i])) {
                String sId = null;
                StringTokenizer st = new StringTokenizer(argv[i + 1], ",");
                while (st.hasMoreTokens()) {
                    try {
                        sId = st.nextToken();
                        lServers.add(Long.valueOf(sId).toString());
                    } catch (NumberFormatException e) {
                        System.out.print("Unrecognized server id " + sId + ". Ingnoring...");
                    }
                }
                i++;
            } else if ("-pip".equals(argv[i]) || "--pServerIP".equals(argv[i])) {
                i++;
                String pServerIP = argv[i];
                RE ipRE = new RE(ipCheckStr);
                if (!ipRE.match(pServerIP)) {
                    System.out.println("IP '" + pServerIP + "' was specified incorrectly.");
                    System.exit(2);
                }
                ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
                Database db = Toolbox.getDB(config);
                Connection con = db.getConnection();
                PreparedStatement ps = null;
                try {
                    ps = con.prepareStatement("SELECT ls.id FROM l_server ls, l_server_groups lsg, p_server p WHERE ls.p_server_id = p.id AND ls.group_id = lsg.id AND p.ip1 = ? AND lsg.type_id IN (2,21)");
                    ps.setString(1, pServerIP);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        lServers.add(rs.getString(1));
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Throwable th) {
                    Session.closeStatement(ps);
                    con.close();
                    throw th;
                }
            } else if ("-z".equals(argv[i]) || "--zone".equals(argv[i])) {
                zone = argv[i + 1];
                i++;
            }
            i++;
        }
        try {
            if (configured) {
                try {
                    new DNSCreator();
                    m16go(cMethode, delZones, zone, lServers);
                    System.out.println("Finished at " + TimeUtils.getDate());
                } catch (Exception e2) {
                    e2.printStackTrace();
                    System.exit(-1);
                    System.out.println("Finished at " + TimeUtils.getDate());
                }
                System.out.println("Zones creation finished");
                System.exit(0);
                return;
            }
            System.out.println("Missconfiguration ");
            printHelp();
        } catch (Throwable th2) {
            System.out.println("Finished at " + TimeUtils.getDate());
            throw th2;
        }
    }

    private static void outLogInfo(String message, int messageType) {
        if (!toConsol) {
            switch (messageType) {
                case 1:
                    Session.getLog().error(message);
                    return;
                case 2:
                    Session.getLog().warn(message);
                    return;
                case 3:
                    Session.getLog().info(message);
                    return;
                default:
                    Session.getLog().debug(message);
                    return;
            }
        }
        System.out.println(message);
    }

    /* renamed from: go */
    public static void m17go(int method, boolean delzone, String zone) throws Exception {
        m16go(method, delzone, zone, null);
    }

    /* renamed from: go */
    public static void m16go(int method, boolean delzone, String zone, List lServers) throws Exception {
        PreparedStatement ps1 = null;
        isDbWise = method == 1;
        delz = delzone;
        targetZone = zone;
        if (lServers != null && !lServers.isEmpty()) {
            Iterator iter = lServers.iterator();
            serverIDs = (String) iter.next();
            while (iter.hasNext()) {
                serverIDs += "," + ((String) iter.next());
            }
        } else {
            serverIDs = "";
        }
        String messageToOut = new String("Working on " + (zone.length() > 0 ? " " + zone + " dns zone; " : " all DNS zones; ") + (delz ? " will try to delete zone before create it; " : " ") + (isDbWise ? "will pick NS from db" : " will pick NS randomly;") + (serverIDs.length() > 0 ? "\nProcessing zones on the logical servers with IDs " + serverIDs : ""));
        outLogInfo(messageToOut, 3);
        String messageToOut2 = new String("Started at " + TimeUtils.getDate());
        outLogInfo(messageToOut2, 3);
        Connection con = Session.getDb();
        try {
            ps1 = con.prepareStatement("SELECT pv.value FROM parent_child p, accounts a, plan_value pv WHERE p.child_id = ? AND p.account_id = a.id AND a.plan_id = pv.plan_id AND pv.name=?");
            ps1.setString(2, "_EMULATION_MODE");
            outLogInfo("Starting :", 3);
            srvIdsPs = con.prepareStatement("SELECT master, slave1, slave2 FROM dns_zones WHERE id = ?");
            StringBuffer q = new StringBuffer("SELECT id,name,email FROM dns_zones");
            if (serverIDs.length() > 0 || targetZone.length() > 0) {
                q.append(" WHERE ");
            }
            if (serverIDs.length() > 0) {
                q.append(" master IN (").append(serverIDs).append(") OR slave1 IN (").append(serverIDs).append(") OR slave2 IN (").append(serverIDs).append(")");
            } else if (targetZone.length() > 0) {
                q.append(" name=? ");
            }
            q.append(" ORDER BY name");
            System.out.print("The querry is " + q.toString());
            PreparedStatement ps = con.prepareStatement(q.toString());
            if (targetZone.length() > 0 && serverIDs.length() <= 0) {
                ps.setString(1, targetZone);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    ps1.setLong(1, rs.getLong("id"));
                    ResultSet rs1 = ps1.executeQuery();
                    if (rs1.next() && "1".equals(rs1.getString(1))) {
                        outLogInfo("DNS zone " + rs.getString("name") + " has been created under a demo plan. Skipping ...", 3);
                        Session.addMessage("DNS zone " + rs.getString("name") + " has been created under a demo plan. Skipping ...");
                    } else {
                        try {
                            createDNSZone(rs.getLong("id"), rs.getString("name"), rs.getString("email"), getResellerId(rs.getLong("id")));
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                } catch (SQLException e) {
                    outLogInfo("An SQL error occured while checking dns zone " + rs.getString("name") + ".DNS zone will not be created. More details can be found in the H-Sphere log file", 3);
                    Session.addMessage("An SQL error occured while checking dns zone " + rs.getString("name") + ".DNS zone will not be created.");
                }
            }
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    private static void createDNSZone(long zoneId, String name, String email, long resellerId) throws Exception {
        long master;
        long slave1;
        long slave2;
        long dmaster = 0;
        long dslave1 = 0;
        long dslave2 = 0;
        if (delz) {
            srvIdsPs.setLong(1, zoneId);
            ResultSet rs = srvIdsPs.executeQuery();
            rs.next();
            dmaster = rs.getLong(1);
            dslave1 = rs.getLong(2);
            dslave2 = rs.getLong(3);
            try {
                outLogInfo("DNS zone " + name + " deletion", 3);
                DNSServices.deleteZone(HostManager.getHost(dmaster), name);
                if (dslave1 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(dslave1), name);
                }
                if (dslave2 != 0) {
                    DNSServices.deleteZone(HostManager.getHost(dslave2), name);
                }
                outLogInfo("[   OK   ]", 3);
            } catch (Exception e) {
                outLogInfo("[   FAIL   ]", 2);
                outLogInfo(e.toString(), 2);
            }
        }
        if (isDbWise) {
            if (delz) {
                master = dmaster;
                slave1 = dslave1;
                slave2 = dslave2;
            } else {
                srvIdsPs.setLong(1, zoneId);
                ResultSet rs2 = srvIdsPs.executeQuery();
                rs2.next();
                master = rs2.getLong(1);
                slave1 = rs2.getLong(2);
                slave2 = rs2.getLong(3);
            }
        } else {
            Iterator i = HostManager.getRandomHostsList(2).iterator();
            master = ((HostEntry) i.next()).getId();
            slave1 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
            slave2 = i.hasNext() ? ((HostEntry) i.next()).getId() : 0L;
        }
        HostEntry m = HostManager.getHost(master);
        HostEntry s1 = null;
        String ip1 = null;
        HostEntry s2 = null;
        String ip2 = null;
        if (slave1 != 0) {
            s1 = HostManager.getHost(slave1);
            ip1 = s1.getIPs().iterator().next().toString();
        }
        if (slave2 != 0) {
            s2 = HostManager.getHost(slave2);
            ip2 = s2.getIPs().iterator().next().toString();
        }
        try {
            Session.setResellerId(resellerId);
            outLogInfo("Creating DNS zone " + name + " for resellerId " + resellerId + " MASTER " + m.getName() + " SLAVE1 " + (s1 != null ? s1.getName() : "UNASSIGNED") + " SLAVE2 " + (s2 != null ? s2.getName() : "UNASSIGNED"), 3);
            DNSServices.createZone(m, name, m.getName(), email, m.getName(), s1 != null ? s1.getName() : null, ip1, s2 != null ? s2.getName() : null, ip2, "10800", C0004CP.INETADDRTTL, "604800", "86400");
            String ipm = m.getPServer().getPFirstIP();
            if (slave1 != 0) {
                DNSServices.createSlaveZone(s1, name, ipm);
            }
            if (slave2 != 0) {
                DNSServices.createSlaveZone(s2, name, ipm);
            }
            outLogInfo("[   OK   ]", 3);
            if (!isDbWise) {
                updateDNSZone(zoneId, master, slave1, slave2);
            }
            createDNSRecords(zoneId, name, master);
        } catch (Exception ex) {
            outLogInfo("[   FAIL   ]", 2);
            outLogInfo(ex.toString(), 2);
        }
    }

    private static void createDNSRecords(long zoneId, String zoneName, long masterId) throws Exception {
        PreparedStatement ps1 = null;
        Connection con = Session.getDb();
        try {
            ps1 = con.prepareStatement("SELECT a.name, a.type, a.data, a.ttl, a.pref FROM dns_records a WHERE a.name LIKE ? OR a.name = ?");
            ps1.setString(1, "%." + zoneName);
            ps1.setString(2, zoneName);
            ResultSet rs1 = ps1.executeQuery();
            while (rs1.next()) {
                try {
                    HostEntry host = HostManager.getHost(masterId);
                    outLogInfo("New DNS record:" + zoneName + " name:" + rs1.getString("name") + " type:" + rs1.getString("type") + " ttl:" + rs1.getString("ttl") + " data:" + Session.int2ext(rs1.getString("data")) + " pref:" + rs1.getString("pref"), 3);
                    DNSServices.addToZone(host, zoneName, rs1.getString("name"), rs1.getString("type"), rs1.getString("ttl"), rs1.getString("data"), rs1.getString("pref"), false);
                    outLogInfo("[   OK   ]", 3);
                } catch (Exception ex) {
                    outLogInfo("[   FAIL   ]", 2);
                    outLogInfo(ex.toString(), 2);
                }
            }
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    private static void updateDNSZone(long zoneId, long master, long slave1, long slave2) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE dns_zones set master=?,slave1=?,slave2=? WHERE id=?");
            ps.setLong(1, master);
            ps.setLong(2, slave1);
            ps.setLong(3, slave2);
            ps.setLong(4, zoneId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static long getResellerId(long zoneId) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT c.reseller_id FROM parent_child b, accounts c WHERE b.child_id=? AND b.account_id = c.id");
            ps.setLong(1, zoneId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long j = rs.getLong(1);
                Session.closeStatement(ps);
                con.close();
                return j;
            }
            PreparedStatement ps2 = con.prepareStatement("SELECT b.reseller_id FROM e_zones b WHERE b.id = ?");
            ps2.setLong(1, zoneId);
            ResultSet rs2 = ps2.executeQuery();
            if (!rs2.next()) {
                Session.closeStatement(ps2);
                con.close();
                return 1L;
            }
            long j2 = rs2.getLong(1);
            Session.closeStatement(ps2);
            con.close();
            return j2;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static void printHelp() {
        System.out.println("H-Sphere DNS zones recreator.");
        System.out.println("Usage java psoft.hsphere.tools.DNSCreator -m creation_method [-dz] -z zonename");
        System.out.println("   -m|--methode possible values db or rand;");
        System.out.println("        db - pick NS servers as they defined in the H-Sphere database;");
        System.out.println("        rand - pick NS servers randomly;");
        System.out.println("   -dz|--delete_zones when this option utility will try to delete each DNS zone before recreate it.");
        System.out.println("       Be aware this will take at least twice more time;");
        System.out.println("   -lids|--logical-servers process zones which are on the logical servers with the specified IDs.");
        System.out.println("   -pip|--pServerIP \tspecifies a physical server by its primary IP. All the necessary logical server ids are chosen automatically. It is used as an alternative to the \"--logical-servers\" parameter.");
        System.out.println("   -z|--zone zonename Recreate only give zone.");
        System.out.println("    If both -lids|--logical-servers and -z|--zone parameters are specified\n    the -z|--zone parameter will be ignored");
    }
}
