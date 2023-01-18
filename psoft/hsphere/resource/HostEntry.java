package psoft.hsphere.resource;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.Set;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Plan;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.global.Globals;
import psoft.hsphere.resource.allocation.AllocatedPServer;
import psoft.hsphere.resource.allocation.AllocatedPServerHolder;
import psoft.hsphere.resource.allocation.AllocatedServerManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/HostEntry.class */
public class HostEntry implements TemplateHashModel, Comparable {
    public static final int WEB_SERVER = 1;
    public static final int DNS_SERVER = 2;
    public static final int MAIL_SERVER = 3;
    public static final int MYSQL_SERVER = 4;
    public static final int WIN_SERVER = 5;
    public static final int REAL_SERVER = 6;
    public static final int WIN_REAL_SERVER = 7;
    public static final int CONTROL_PANEL_SERVER = 10;
    public static final int VP_SERVER = 12;
    public static final int MSSQL_SERVER = 15;
    public static final int PGSQL_SERVER = 18;
    public static final int MYDNS_SERVER = 21;
    public static final int IRIS_SERVER = 23;
    public static final int MRTG_SERVER = 25;
    protected static final HashMap groupTypeToId = new HashMap();
    protected static HashMap groupNameToId = null;
    protected static final int TIMEOUT = 50000;
    public static final int INVALID_IP = -1;
    public static final int UNUSED_IP = 0;
    public static final int EXCLUSIVE_IP = 1;
    public static final int SHARED_IP = 2;
    public static final int SYSTEM_MANAGEMENT_IP = 3;
    public static final int SERVICE_IP = 4;
    public static final int RESELLER_DNS_IP = 5;
    public static final int TAKEN_RESELLER_DNS_IP = 6;
    public static final int RESELLER_SSL_IP = 7;
    public static final int TAKEN_RESELLER_SSL_IP = 8;
    public static final int VPS_IP = 1000;
    public static final int TAKEN_VPS_IP = 1001;
    public static final String UnixUserHomePrefix = "/hsphere/local/";
    public static final String UnixUserDefaultHomeDir = "home";
    public static final String UnixUserDefaultHome = "/hsphere/local/home";
    public static final String WinUserDefaultHome = "D:\\home";
    protected String fqdn;

    /* renamed from: id */
    protected long f145id;
    protected int status;
    protected int group;
    protected int groupType;
    protected String filerPath;
    protected String fileServer;
    protected int signup;
    protected Set sharedSSL;
    protected Set sharedSSLca;
    private static boolean emulationMode;
    private static Hashtable heSyncObject;
    protected Hashtable options = new Hashtable();
    protected Hashtable aliases = new Hashtable();
    protected Hashtable resellerIPs = new Hashtable();

    public static TemplateMap getGroupTypeToId() {
        return new TemplateMap(groupTypeToId);
    }

    public static HashMap getGroupTypeToIdHash() {
        return groupTypeToId;
    }

    static {
        groupTypeToId.put("unix_hosting", String.valueOf(1));
        groupTypeToId.put("windows_hosting", String.valueOf(5));
        groupTypeToId.put("mail", String.valueOf(3));
        groupTypeToId.put("mysql", String.valueOf(4));
        groupTypeToId.put("mssql", String.valueOf(15));
        groupTypeToId.put("pgsql", String.valueOf(18));
        groupTypeToId.put("unix_real", String.valueOf(6));
        groupTypeToId.put("windows_real", String.valueOf(7));
        groupTypeToId.put("dns", String.valueOf(2));
        groupTypeToId.put("cp", String.valueOf(10));
        groupTypeToId.put("vps", String.valueOf(12));
        groupTypeToId.put("mrtg", String.valueOf(25));
        heSyncObject = new Hashtable();
    }

    public static boolean getEmulationMode() {
        Account a = Session.getAccount();
        if (a != null) {
            try {
                String emulationValue = a.getPlan().getValue("_EMULATION_MODE");
                if (emulationValue != null) {
                    if ("1".equals(emulationValue)) {
                        return true;
                    }
                }
            } catch (Exception e) {
            }
        }
        return emulationMode;
    }

    public static boolean getSystemEmulationMode() {
        return emulationMode;
    }

    public static void setEmulationMode(boolean em) {
        emulationMode = em;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("name")) {
            return new TemplateString(getName());
        }
        if (key.equals("base_name")) {
            return new TemplateString(getBaseName());
        }
        if (key.equals("id")) {
            return new TemplateString(this.f145id);
        }
        if (key.equals("platform")) {
            return new TemplateString("Unix (Linux/FreeBSD)");
        }
        if (key.equals("platform_type")) {
            return new TemplateString(platformType());
        }
        if (key.equals("sharedSSLCaZones")) {
            return new TemplateList(this.sharedSSLca);
        }
        if ("allocation_descr".equals(key)) {
            return new TemplateString(getAllocationDescription());
        }
        if (key.equals("ip")) {
            try {
                return new TemplateString(getIP().toString());
            } catch (Exception e) {
            }
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public HostEntry(long id, String fqdn, int group, int groupType, String fileServer, String filerPath, int status, int signup) throws Exception {
        this.sharedSSL = null;
        this.sharedSSLca = null;
        this.fqdn = fqdn;
        this.f145id = id;
        this.status = status;
        this.signup = signup;
        this.group = group;
        this.groupType = groupType;
        this.filerPath = filerPath;
        this.fileServer = fileServer;
        loadAllAliases();
        loadResellerIPs();
        loadAllOptions();
        this.sharedSSL = Collections.synchronizedSet(new HashSet());
        this.sharedSSLca = Collections.synchronizedSet(new HashSet());
        loadSharedSSL();
    }

    public Collection getIPs() throws SQLException {
        LinkedList ll = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip, mask FROM l_server_ips WHERE l_server_id = ? AND flag IN (?,?) ORDER BY flag DESC ");
            ps.setLong(1, this.f145id);
            ps.setInt(2, 4);
            ps.setInt(3, 2);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ll.add(C0015IP.createIP(rs.getString(1), rs.getString(2)));
            }
            Session.closeStatement(ps);
            con.close();
            return ll;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void dumpIP(C0015IP ip) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE l_server_ips SET flag = ? WHERE ip = ? AND l_server_id = ?");
            ps.setInt(1, -1);
            ps.setString(2, ip.getIP());
            ps.setLong(3, this.f145id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected static Object getSyncObject(long id) {
        Long l = new Long(id);
        Object obj = heSyncObject.get(l);
        if (obj != null) {
            return obj;
        }
        heSyncObject.put(l, l);
        return l;
    }

    public C0015IP getExclusiveIP(ResourceId rid, long recId, int free_ip, int taken_ip) throws Exception {
        PreparedStatement ps;
        C0015IP result = null;
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            ps2 = con.prepareStatement("SELECT ip, mask, flag, ip_num FROM l_server_ips WHERE l_server_id = ? AND l_server_ips.flag = ? ORDER BY ip_num");
            synchronized (getSyncObject(this.f145id)) {
                ps2.setLong(1, this.f145id);
                ps2.setInt(2, free_ip);
                ResultSet rs = ps2.executeQuery();
                if (rs.next()) {
                    C0015IP ip = C0015IP.createIP(rs.getString(1), rs.getString(2));
                    Session.getLog().debug("---------> " + ip);
                    ps2.close();
                    if (free_ip == 0) {
                        ps = con.prepareStatement("UPDATE l_server_ips SET flag = ?, r_id = ?, r_type = ? WHERE ip = ? AND l_server_id = ?");
                        ps.setInt(1, taken_ip);
                        ps.setLong(2, rid.getId());
                        ps.setInt(3, rid.getType());
                        ps.setString(4, ip.toString());
                        ps.setLong(5, this.f145id);
                        if (1 == ps.executeUpdate()) {
                            result = ip;
                        }
                    } else if (free_ip == 5 || free_ip == 7 || free_ip == 1000) {
                        ps = con.prepareStatement("UPDATE l_server_ips SET flag = ?, r_id = ? WHERE ip = ? AND l_server_id = ?");
                        ps.setInt(1, taken_ip);
                        ps.setLong(2, recId);
                        ps.setString(3, ip.toString());
                        ps.setLong(4, this.f145id);
                        if (1 == ps.executeUpdate()) {
                            result = ip;
                        }
                    }
                    C0015IP c0015ip = result;
                    Session.closeStatement(ps);
                    con.close();
                    return c0015ip;
                }
                throw new HSUserException("hostentry.exclusive", new Object[]{String.valueOf(this.f145id)});
            }
        } finally {
            Session.closeStatement(ps2);
            con.close();
        }
    }

    public synchronized C0015IP getExclusiveIP(ResourceId rid) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getExclusiveIP(" + rid + ")");
        try {
            return getExclusiveIP(rid, -1L, 0, 1);
        } catch (Exception e) {
            throw new HSUserException("hostentry.exclusive", new Object[]{String.valueOf(this.f145id)});
        }
    }

    public synchronized C0015IP getExclusiveDNSIP(long recId) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getExclusiveDNSIP(" + recId + ")");
        try {
            if (Globals.isObjectDisabled(Plan.RESELLER_DNS_IPS_PARAM) == 0) {
                return getExclusiveIP(null, recId, 5, 6);
            }
            return getIP();
        } catch (Exception e) {
            throw new HSUserException("hostentry.exclusivedns", new Object[]{String.valueOf(this.f145id)});
        }
    }

    public synchronized C0015IP getExclusiveCPSSLIP(long recId) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getExclusiveCPSSLIP(" + recId + ")");
        try {
            return getExclusiveIP(null, recId, 7, 8);
        } catch (Exception e) {
            throw new HSUserException("hostentry.exclusivessl", new Object[]{String.valueOf(this.f145id)});
        }
    }

    public synchronized C0015IP getExclusiveVPSIP(long recId) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getExclusiveVPSIP(" + recId + ")");
        try {
            return getExclusiveIP(null, recId, VPS_IP, TAKEN_VPS_IP);
        } catch (Exception e) {
            throw new HSUserException("hostentry.exclusivevps", new Object[]{String.valueOf(this.f145id)});
        }
    }

    public C0015IP getSharedIP() throws Exception {
        return getSharedIP(2);
    }

    public synchronized C0015IP getSharedIP(int tag) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getSharedIP");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip,mask,flag FROM l_server_ips WHERE l_server_id = ? AND l_server_ips.flag = ?");
            ps.setLong(1, this.f145id);
            ps.setInt(2, tag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                C0015IP ip = C0015IP.createIP(rs.getString(1), rs.getString(2));
                Session.getLog().debug("---------> " + ip);
                return ip;
            }
            throw new HSUserException("hostentry.shared", new Object[]{String.valueOf(this.f145id)});
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public C0015IP getIPbyRid(ResourceId rid) throws Exception {
        return getIPbyRid(rid, 1);
    }

    public C0015IP getIPbyRid(ResourceId rid, int flag) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip,mask FROM l_server_ips WHERE l_server_id = ? AND r_id = ? AND flag =?");
            ps.setLong(1, getId());
            ps.setLong(2, rid.getId());
            ps.setInt(3, flag);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                C0015IP createIP = C0015IP.createIP(rs.getString(1), rs.getString(2));
                Session.closeStatement(ps);
                con.close();
                return createIP;
            }
            Session.closeStatement(ps);
            con.close();
            throw new NotFoundException("no IP is available for " + rid + " resource");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public C0015IP getIPbyIP(String strIp, ResourceId rid, long recId, int free_ip, int taken_ip) throws Exception {
        C0015IP result = null;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT ip, mask, flag, ip_num FROM l_server_ips WHERE l_server_id = ? AND l_server_ips.flag = ? AND ip = ? ORDER BY ip_num");
            ps.setLong(1, this.f145id);
            ps.setInt(2, free_ip);
            ps.setString(3, strIp);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                C0015IP ip = C0015IP.createIP(rs.getString(1), rs.getString(2));
                Session.getLog().debug("---------> " + ip);
                ps.close();
                if (free_ip == 0) {
                    ps = con.prepareStatement("UPDATE l_server_ips SET flag = ?, r_id = ?, r_type = ? WHERE ip = ? AND l_server_id = ?");
                    ps.setInt(1, taken_ip);
                    ps.setLong(2, rid.getId());
                    ps.setInt(3, rid.getType());
                    ps.setString(4, ip.toString());
                    ps.setLong(5, this.f145id);
                    if (1 == ps.executeUpdate()) {
                        result = ip;
                    }
                } else if (free_ip == 5 || free_ip == 7) {
                    ps = con.prepareStatement("UPDATE l_server_ips SET flag = ?, r_id = ? WHERE ip = ? AND l_server_id = ?");
                    ps.setInt(1, taken_ip);
                    ps.setLong(2, recId);
                    ps.setString(3, ip.toString());
                    ps.setLong(4, this.f145id);
                    if (1 == ps.executeUpdate()) {
                        result = ip;
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public C0015IP getIPbyIP(String strIp, ResourceId rid) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getIPbyIP(" + strIp + ")");
        try {
            return getIPbyIP(strIp, rid, -1L, 0, 1);
        } catch (Exception e) {
            throw new HSUserException("hostentry.exclips", new Object[]{strIp, String.valueOf(this.f145id)});
        }
    }

    public synchronized C0015IP getDNSIPbyIP(String strIp, long recId) throws Exception {
        Session.getLog().debug("HostEntry " + this.f145id + " getExclusiveDNSIP(" + recId + ")");
        try {
            return getIPbyIP(strIp, null, recId, 5, 6);
        } catch (Exception e) {
            throw new HSUserException("hostentry.exclusivedns", new Object[]{String.valueOf(this.f145id)});
        }
    }

    protected String getFreeCPSSLIP() throws Exception {
        Session.getLog().debug("HostEntry.getFreeCPSSLIP()");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        String ip = new String();
        try {
            ps = con.prepareStatement("SELECT ip FROM l_server_ips WHERE flag = ?");
            ps.setInt(1, 7);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ip = rs.getString(1);
            }
            rs.close();
            Session.closeStatement(ps);
            con.close();
            return ip;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Code restructure failed: missing block: B:21:0x0038, code lost:
        if ("".equals(r10) != false) goto L4;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    public synchronized psoft.hsphere.resource.C0015IP getCPSSLIPbyIP(java.lang.String r10, long r11) throws java.lang.Exception {
        /*
            r9 = this;
            org.apache.log4j.Category r0 = psoft.hsphere.Session.getLog()
            java.lang.StringBuilder r1 = new java.lang.StringBuilder
            r2 = r1
            r2.<init>()
            java.lang.String r2 = "HostEntry "
            java.lang.StringBuilder r1 = r1.append(r2)
            r2 = r9
            long r2 = r2.f145id
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r2 = " getCPSSLIPbyIP("
            java.lang.StringBuilder r1 = r1.append(r2)
            r2 = r11
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r2 = ") strIP = "
            java.lang.StringBuilder r1 = r1.append(r2)
            r2 = r10
            java.lang.StringBuilder r1 = r1.append(r2)
            java.lang.String r1 = r1.toString()
            r0.debug(r1)
            r0 = r10
            if (r0 == 0) goto L3b
            java.lang.String r0 = ""
            r1 = r10
            boolean r0 = r0.equals(r1)     // Catch: java.lang.Exception -> L4c
            if (r0 == 0) goto L40
        L3b:
            r0 = r9
            java.lang.String r0 = r0.getFreeCPSSLIP()     // Catch: java.lang.Exception -> L4c
            r10 = r0
        L40:
            r0 = r9
            r1 = r10
            r2 = 0
            r3 = r11
            r4 = 7
            r5 = 8
            psoft.hsphere.resource.IP r0 = r0.getIPbyIP(r1, r2, r3, r4, r5)     // Catch: java.lang.Exception -> L4c
            return r0
        L4c:
            r13 = move-exception
            psoft.hsphere.HSUserException r0 = new psoft.hsphere.HSUserException
            r1 = r0
            java.lang.String r2 = "hostentry.exclusivessl"
            r3 = 1
            java.lang.Object[] r3 = new java.lang.Object[r3]
            r4 = r3
            r5 = 0
            r6 = r9
            long r6 = r6.f145id
            java.lang.String r6 = java.lang.String.valueOf(r6)
            r4[r5] = r6
            r1.<init>(r2, r3)
            throw r0
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.resource.HostEntry.getCPSSLIPbyIP(java.lang.String, long):psoft.hsphere.resource.IP");
    }

    public static int checkAllIP(String strIp) throws Exception {
        Connection con = Session.getDb();
        int ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT ip, mask, flag, ip_num,r_id FROM l_server_ips WHERE ip = ? ORDER BY ip_num");
            ps2.setString(1, strIp);
            ResultSet rs = ps2.executeQuery();
            return rs.next() ? rs.getInt("flag") : -1;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public int checkIP(String strIp) throws Exception {
        Connection con = Session.getDb();
        int ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT ip, mask, flag, ip_num,r_id FROM l_server_ips WHERE ip = ? AND L_Server_Id = ? ORDER BY ip_num");
            ps2.setString(1, strIp);
            ps2.setLong(2, this.f145id);
            ResultSet rs = ps2.executeQuery();
            return rs.next() ? rs.getInt("flag") : -1;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void releaseIP(String ip, int free_ip, int busyIp) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE l_server_ips set flag = ?, r_id = ?, r_type = ? WHERE ip = ?  AND l_server_id = ? AND flag = ?");
            ps.setInt(1, free_ip);
            ps.setNull(2, 4);
            ps.setNull(3, 4);
            ps.setString(4, ip);
            ps.setLong(5, this.f145id);
            ps.setInt(6, busyIp);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void releaseIP(C0015IP ip) throws Exception {
        releaseIP(ip.toString(), 0, 1);
    }

    public void releaseDNSIP(String ip) throws Exception {
        releaseIP(ip.toString(), 5, 6);
    }

    public void releaseVPSIP(String ip) throws Exception {
        releaseIP(ip.toString(), VPS_IP, TAKEN_VPS_IP);
    }

    public void releaseCPSSLIP(String ip) throws Exception {
        releaseIP(ip.toString(), 7, 8);
    }

    public String getName() {
        try {
            String name = (String) this.aliases.get(new Long(Session.getResellerId()));
            if (name == null) {
                return (String) this.aliases.get(new Long(1L));
            }
            return name;
        } catch (Exception e) {
            return (String) this.aliases.get(new Long(1L));
        }
    }

    public C0015IP getIP() {
        try {
            C0015IP ip = (C0015IP) this.resellerIPs.get(new Long(Session.getResellerId()));
            if (ip == null) {
                return (C0015IP) getIPs().iterator().next();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }

    public String getBaseName() {
        return this.fqdn;
    }

    public String getFilerPath() {
        return this.filerPath;
    }

    public String getFiler() {
        return this.fileServer.substring(0, this.fileServer.indexOf(58));
    }

    public int getGroup() {
        return this.group;
    }

    public int getGroupType() {
        return this.groupType;
    }

    public long getId() {
        return this.f145id;
    }

    public int getStatus() {
        return this.status;
    }

    public void setSignup(int signup) {
        this.signup = signup;
    }

    public boolean isInstalled() {
        try {
            if (getPServer().getStatus() == 1) {
                return false;
            }
            String groupTypeName = getGroupNameByType(getGroupType());
            if (!getPServer().getServices().contains(groupTypeName)) {
                return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean availableForSignup() {
        return this.signup == 1;
    }

    public Collection exec(String command, String[] args) throws Exception {
        return exec(command, args, (String) null);
    }

    public Collection exec(String command, String[] args, String input) throws Exception {
        return exec(command, Arrays.asList(args), input);
    }

    public Collection exec(String command, Collection args) throws Exception {
        return exec(command, args, (byte[]) null);
    }

    public Collection exec(String command, Collection args, String input) throws Exception {
        return exec(command, args, input != null ? input.getBytes(LanguageManager.STANDARD_CHARSET) : null);
    }

    public Collection exec(String command, Collection args, byte[] input) throws Exception {
        PhysicalServer ps = getPServer();
        if (getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, command to execute: " + command + " arguments " + args);
            return Arrays.asList("0");
        }
        return ps.exec("root", command, args, input);
    }

    public Collection execSpecial(String command, String[] args, InputStream is) throws Exception {
        PhysicalServer ps = getPServer();
        if (getEmulationMode()) {
            Session.getLog().warn("EMULATION MODE, command to execute: " + command + " arguments " + Arrays.asList(args));
            return Arrays.asList("0");
        }
        return ps.execSpecial("root", command, Arrays.asList(args), is);
    }

    public PhysicalServer getPServer() throws Exception {
        for (int i = 5; i > 0; i--) {
            PhysicalServer ps = PhysicalServer.getPServerForLServer(this.f145id);
            if (null == ps) {
                Session.getLog().debug("HostEntry.getPServer sleeping .....");
                TimeUtils.sleep(10000L);
            } else {
                return ps;
            }
        }
        throw new Exception("Unavailable PhysicalServer for LogicalServer " + this.f145id);
    }

    public String getPFirstIP() throws Exception {
        PhysicalServer ps = getPServer();
        if (ps != null) {
            return ps.getPFirstIP();
        }
        throw new Exception("No Physical Server Found");
    }

    public String getPSecondIP() throws Exception {
        PhysicalServer ps = getPServer();
        if (ps != null) {
            return ps.getPSecondIP();
        }
        throw new Exception("No Physical Server Found");
    }

    public String getLogicalIP() throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT ip, mask FROM l_server_ips WHERE l_server_id = ?");
            ps.setLong(1, this.f145id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String string = rs.getString(1);
                Session.closeStatement(ps);
                con.close();
                return string;
            }
            throw new Exception("No logical IP found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public synchronized void loadAllAliases() throws Exception {
        this.aliases.clear();
        this.aliases.put(new Long(1L), this.fqdn);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT reseller_id, prefix FROM l_server_aliases, e_zones WHERE l_server_id = ? AND id = e_zone_id");
            ps.setLong(1, this.f145id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.aliases.put(new Long(rs.getLong(1)), rs.getString(2));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void loadAllOptions() throws Exception {
        this.options.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT name, value FROM l_server_options WHERE l_server_id = ?");
        ps.setLong(1, this.f145id);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            this.options.put(rs.getString(1), rs.getString(2));
        }
        Session.closeStatement(ps);
        con.close();
    }

    public String getOption(String name) {
        String str;
        synchronized (this) {
            str = (String) this.options.get(name);
        }
        return str;
    }

    public void setOption(String name, String value) throws Exception {
        synchronized (this) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("DELETE FROM l_server_options WHERE l_server_id = ? AND name = ?");
            ps.setLong(1, getId());
            ps.setString(2, name);
            ps.executeUpdate();
            ps.close();
            if (value != null || !"".equals(value.trim())) {
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO l_server_options(l_server_id, name, value) VALUES(? ,?, ?)");
                ps2.setLong(1, getId());
                ps2.setString(2, name);
                ps2.setString(3, value);
                ps2.executeUpdate();
                this.options.put(name, value.trim());
                Session.closeStatement(ps2);
                con.close();
                return;
            }
            Session.closeStatement(ps);
            con.close();
        }
    }

    public synchronized void loadResellerIPs() throws Exception {
        this.resellerIPs.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT reseller_id, ip, mask FROM l_server_aliases l, e_zones e, l_server_ips i WHERE l.l_server_id = ? AND e.id = e_zone_id AND i.r_id = l.e_dns_rec_id ");
            ps.setLong(1, this.f145id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.resellerIPs.put(new Long(rs.getLong(1)), C0015IP.createIP(rs.getString(2), rs.getString(3)));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void loadSharedSSL() throws Exception {
        this.sharedSSL.clear();
        this.sharedSSLca.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT zone_id, cert FROM shared_ssl_hosts WHERE l_server_id = ?");
            ps.setLong(1, this.f145id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.sharedSSL.add(new Long(rs.getLong(1)));
                if (rs.getInt(2) > 0) {
                    this.sharedSSLca.add(new Long(rs.getLong(1)));
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void setSharedSSL(long zoneId, boolean state) throws Exception {
        Connection con;
        if (state) {
            if (hasSharedSSL(zoneId)) {
                return;
            }
            con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("INSERT INTO shared_ssl_hosts (zone_id, l_server_id, cert) VALUES(? ,?, 0)");
                ps.setLong(1, zoneId);
                ps.setLong(2, this.f145id);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                this.sharedSSL.add(new Long(zoneId));
            } finally {
            }
        } else if (hasSharedSSL(zoneId)) {
            con = Session.getDb();
            PreparedStatement ps2 = null;
            try {
                ps2 = con.prepareStatement("DELETE FROM shared_ssl_hosts WHERE zone_id = ? AND l_server_id = ?");
                ps2.setLong(1, zoneId);
                ps2.setLong(2, this.f145id);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                this.sharedSSL.remove(new Long(zoneId));
                this.sharedSSLca.remove(new Long(zoneId));
            } finally {
            }
        }
    }

    public boolean hasSharedSSL(long zoneId) throws Exception {
        return this.sharedSSL.contains(new Long(zoneId));
    }

    public synchronized void setSharedSSLca(long zoneId, boolean state) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE shared_ssl_hosts SET cert = ? WHERE l_server_id = ? AND zone_id = ?");
            ps.setInt(1, state ? 1 : 0);
            ps.setLong(2, this.f145id);
            ps.setLong(3, zoneId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (state) {
                this.sharedSSLca.add(new Long(zoneId));
            } else {
                this.sharedSSLca.remove(new Long(zoneId));
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean hasSharedSSLca(long zoneId) throws Exception {
        return this.sharedSSLca.contains(new Long(zoneId));
    }

    public boolean hasFreeDedicatedIP() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                ps = con.prepareStatement("SELECT ip, mask, flag, ip_num FROM l_server_ips WHERE l_server_id = ? AND l_server_ips.flag = ?");
                ps.setLong(1, this.f145id);
                ps.setInt(2, 0);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Session.closeStatement(ps);
                    con.close();
                    return true;
                }
                Session.closeStatement(ps);
                con.close();
                return false;
            } catch (Exception ex) {
                Session.getLog().error("Error during execute hasFreeDedicatedIP method", ex);
                Session.closeStatement(ps);
                con.close();
                return false;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized boolean hasFreeDedicatedIP(int ipType) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT count(*) as count FROM l_server_ips WHERE l_server_id = ? AND flag = ?  AND r_id IS NULL AND r_type IS NULL");
            ps.setLong(1, getId());
            ps.setInt(2, ipType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getInt(1) > 0) {
                    return true;
                }
                return false;
            }
            return false;
        } catch (Exception ex) {
            Session.getLog().error("Error in method hasFreeDedicatedIP for logical server " + getId(), ex);
            return false;
        }
    }

    public synchronized boolean hasFreeVPSIP() {
        try {
            return hasFreeDedicatedIP(VPS_IP);
        } catch (Exception e) {
            return false;
        }
    }

    public TemplateModel FM_hasFreeVPSIP() {
        Session.getLog().debug("Inside FM_hasFreeVPSIP");
        return new TemplateString(hasFreeVPSIP());
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        HostEntry pl = (HostEntry) o;
        if (getId() == pl.getId()) {
            return 0;
        }
        return (int) (getId() - pl.getId());
    }

    public String getUserHome() {
        switch (this.groupType) {
            case 1:
            case 6:
                String vhomedir = getOption("user_homedir");
                if (vhomedir == null || "".equals(vhomedir)) {
                    return UnixUserDefaultHome;
                }
                return UnixUserHomePrefix + vhomedir;
            case 2:
            case 3:
            case 4:
            default:
                return "";
            case 5:
            case 7:
                return WinUserDefaultHome;
        }
    }

    public String platformType() {
        return "unix";
    }

    public String getAllocationDescription() {
        AllocatedPServer aps;
        String result = "";
        if (AllocatedServerManager.supportedServices.contains(new Integer(getGroup()))) {
            try {
                long pserverId = getPServer().getId();
                if (AllocatedPServerHolder.getInstance().isAllocated(pserverId) && (aps = AllocatedPServerHolder.getInstance().get(pserverId)) != null) {
                    if (aps.getState() == 1) {
                        result = "(Allocated)";
                    } else if (aps.getState() == 2) {
                        result = "(Allocated and Taken by account #" + aps.getUsedBy().getAccountId() + ")";
                    }
                }
            } catch (Exception ex) {
                Session.getLog().debug("Unable to get pserver ", ex);
                return "";
            }
        }
        return result;
    }

    public static String getGroupNameByType(int type) {
        synchronized (HostEntry.class) {
            if (groupNameToId == null) {
                groupNameToId = new HashMap();
                for (String key : getGroupTypeToIdHash().keySet()) {
                    String value = (String) getGroupTypeToIdHash().get(key);
                    groupNameToId.put(value, key);
                }
            }
        }
        return (String) groupNameToId.get(String.valueOf(type));
    }
}
