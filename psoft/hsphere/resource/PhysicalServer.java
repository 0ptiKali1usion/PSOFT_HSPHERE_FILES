package psoft.hsphere.resource;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsinst.boxes.SSHUtil;
import psoft.hsinst.boxes.SshAuthorization;
import psoft.hsp.PackageFile;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.resource.sshpool.SSHConnection;
import psoft.hsphere.resource.sshpool.SSHPool;
import psoft.hsphere.resource.sshpool.SSHResult;
import psoft.hsphere.servmon.ServerInfo;
import psoft.hsphere.util.PackageConfigurator;
import psoft.util.Config;
import psoft.util.IOUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/PhysicalServer.class */
public class PhysicalServer {
    protected String fqdn;
    protected String ip1;
    protected String ip2;
    protected String mask1;
    protected String mask2;
    protected String login;
    protected String password;

    /* renamed from: id */
    protected final long f157id;
    protected int status;
    protected boolean authorized;
    protected boolean installed;
    protected static boolean POOLED_CONNECTION;
    protected static boolean stopServerInfo;
    protected SshAuthorization auth;
    protected Collection services;
    public static final int UNIX_OS = 1;
    public static final int WINDOWS_OS = 2;
    public static final int NEWLY_INSTALLED = 1;
    public static final int WORKED = 0;
    public String HSRELEASE;
    public static final String PREFIX = "/hsphere/shared/scripts/";
    protected static String HS_ADMIN = "root";
    protected static List noServerInfoServers = new ArrayList();
    protected static final HashMap lServerToPserver = new HashMap();
    protected static final HashMap pservers = new HashMap();

    static {
        POOLED_CONNECTION = true;
        stopServerInfo = false;
        try {
            String stopServerInfoBoxes = Config.getProperty("CLIENT", "STOP_SERVER_INFO_SERVERS");
            StringTokenizer st = new StringTokenizer(stopServerInfoBoxes, ",", false);
            while (st.hasMoreTokens()) {
                String serverId = st.nextToken();
                Session.getLog().info("Stopping serverinfo from pserver " + serverId);
                noServerInfoServers.add(new Long(serverId));
            }
        } catch (Exception ex) {
            Session.getLog().warn("Error while filling STOP_SERVER_INFO_SERVERS", ex);
        } catch (Exception e) {
        }
        try {
            POOLED_CONNECTION = !"FALSE".equals(Session.getProperty("POOLED_CONNECTION"));
        } catch (Exception e2) {
        }
        try {
            stopServerInfo = "STOP".equals(Session.getProperty("SERVER_INFO_MONITOR"));
        } catch (Exception e3) {
        }
    }

    public String getName() {
        return this.fqdn;
    }

    public int getOsType() {
        return 1;
    }

    public boolean isAuthorized() {
        return this.authorized;
    }

    public boolean isInstalled() {
        return this.installed;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    protected PhysicalServer(long id, String fqdn, String ip1, String ip2, String mask1, String mask2, int status, String login) {
        this(id, fqdn, ip1, ip2, mask1, mask2, status, login, null);
    }

    protected PhysicalServer(long id, String fqdn, String ip1, String ip2, String mask1, String mask2, int status, String login, String password) {
        this.auth = null;
        this.services = null;
        this.HSRELEASE = "/hsphere/shared/bin/hspackages";
        this.fqdn = fqdn;
        this.ip1 = ip1;
        this.ip2 = ip2;
        this.mask1 = mask1;
        this.mask2 = mask2;
        this.status = status;
        this.f157id = id;
        if (login == null || "".equals(login)) {
            this.login = HS_ADMIN;
        } else {
            this.login = login;
        }
        this.password = password;
        this.authorized = true;
    }

    public PhysicalServer(long id, String fqdn, String ip1, String ip2, String mask1, String mask2, int status) {
        this(id, fqdn, ip1, ip2, mask1, mask2, status, HS_ADMIN);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PhysicalServer) {
            PhysicalServer physicalServer = (PhysicalServer) o;
            return this.ip1 == null ? physicalServer.ip1 == null : this.ip1.equals(physicalServer.ip1);
        }
        return false;
    }

    public void closeAutorization() {
        if (this.auth != null) {
            try {
                if (this.auth.getSession() != null) {
                    this.auth.getSession().disconnect();
                    this.auth = null;
                }
            } catch (Exception e) {
                this.auth.getLogger().debug("Unable to close session", e);
            }
        }
    }

    public SshAuthorization getAuthorization() {
        if (this.auth == null) {
            checkIfAuthorized(false);
        }
        return this.auth;
    }

    public boolean checkIfAuthorized() {
        return checkIfAuthorized(true);
    }

    public boolean checkIfAuthorized(boolean closeSession) {
        if (HostEntry.getSystemEmulationMode()) {
            this.authorized = true;
            return true;
        } else if (!isAuthorized() && this.password == null) {
            return false;
        } else {
            this.auth = SshAuthorization.getInstance(this.ip1, this.login, this.password);
            try {
                this.auth.getSession();
                this.authorized = true;
            } catch (Exception e1) {
                Session.getLog().error("Unable to authorize to server:" + this.ip1, e1);
                this.authorized = false;
            }
            if (closeSession) {
                try {
                    this.auth.getSession().disconnect();
                    this.auth = null;
                } catch (Exception e) {
                }
            }
            return this.authorized;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    public static void checkServer() {
        PhysicalServer ph;
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, name, ip1, ip2, mask1, mask2, status, login, password, os_type FROM p_server");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getInt(10) == 1) {
                    ph = new PhysicalServer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7), rs.getString(8), rs.getString(9));
                } else {
                    ph = new WinPhysicalServer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(8), rs.getString(9), rs.getInt(7));
                }
                synchronized (pservers) {
                    PhysicalServer phInCache = (PhysicalServer) pservers.get(new Long(ph.getId()));
                    if (phInCache != null) {
                        phInCache.set(ph);
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (SQLException ex) {
            Session.getLog().error("Unable to check physical servers", ex);
        }
    }

    public void set(PhysicalServer ph) {
        if (ph.getId() != getId()) {
            return;
        }
        setIp1(ph.getPFirstIP());
        setIp2(ph.getPSecondIP());
        setMask1(ph.getMask1());
        setMask2(ph.getMask2());
        setLogin(getLogin());
        if ((this instanceof WinPhysicalServer) && (ph instanceof WinPhysicalServer)) {
            setPassword(ph.getPassword());
        }
    }

    public int hashCode() {
        if (this.ip1 != null) {
            return this.ip1.hashCode();
        }
        return 0;
    }

    public static PhysicalServer getPServer(long id) throws SQLException {
        synchronized (pservers) {
            PhysicalServer ph = (PhysicalServer) pservers.get(new Long(id));
            if (ph != null) {
                Session.getLog().debug("got pserver:" + ph.getPFirstIP() + " from cache");
                return ph;
            }
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT id, name, ip1, ip2, mask1, mask2, status, login, password, os_type FROM p_server WHERE id = ?");
                ps.setLong(1, id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getInt(10) == 1) {
                        ph = new PhysicalServer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7), rs.getString(8));
                    } else {
                        ph = new WinPhysicalServer(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(8), rs.getString(9), rs.getInt(7));
                    }
                    synchronized (pservers) {
                        pservers.put(new Long(id), ph);
                    }
                    ph.checkStatus();
                    Session.getLog().debug("load pserver:" + ph.getPFirstIP() + " from DB");
                }
                Session.closeStatement(ps);
                con.close();
                return ph;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    private boolean checkStatus() {
        try {
            if (checkIfAuthorized(false)) {
                checkInstalledServices();
            }
            return isAuthorized();
        } finally {
            closeAutorization();
        }
    }

    protected synchronized void checkInstalledServices() {
        if (HostEntry.getSystemEmulationMode()) {
            setServices(HostEntry.getGroupTypeToIdHash().keySet());
            setStatus(0);
            return;
        }
        String command = this.HSRELEASE + " -V " + C0004CP.getHSRelease();
        try {
            try {
                setServices(SSHUtil.exec(getAuthorization(), command, (String) null));
                setStatus(0);
                closeAutorization();
            } catch (Exception e) {
                Session.getLog().error("Unable to get services", e);
                setStatus(1);
                closeAutorization();
            }
        } catch (Throwable th) {
            closeAutorization();
            throw th;
        }
    }

    public void setServices(Collection services) {
        this.services = services;
    }

    public synchronized Collection getServices() {
        if (this.services == null) {
            checkInstalledServices();
        }
        return this.services;
    }

    public void reconfigServices() {
        String command = "/hsphere/pkg/scripts/upackages -i -j -v " + C0004CP.getHSRelease();
        try {
            try {
                SSHUtil.exec(getAuthorization(), command, (String) null);
                closeAutorization();
            } catch (Exception e) {
                Session.getLog().error("Failed to repost config files on box " + getId(), e);
                closeAutorization();
            }
        } catch (Throwable th) {
            closeAutorization();
            throw th;
        }
    }

    public void monitor(ServerInfo serverInfo) {
        if (HostEntry.getEmulationMode() || noServerInfoServers.contains(new Long(this.f157id))) {
            serverInfo.update(ServerInfo.getStaticInfo("psoft_config/UnixServerInfo.xml"));
            return;
        }
        try {
            if (checkStatus()) {
                Collection<String> col = exec("server_info", new String[0], null);
                StringBuffer buf = new StringBuffer();
                for (String str : col) {
                    buf.append(str).append("\n");
                }
                serverInfo.update(buf.toString());
                this.installed = true;
            }
        } catch (Exception e) {
            this.installed = false;
            Session.getLog().warn("Error while querying the p_server " + getId(), e);
        }
    }

    public static void reformLServerToPServer() {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, p_server_id FROM l_server");
            ResultSet rs = ps.executeQuery();
            synchronized (lServerToPserver) {
                while (rs.next()) {
                    Long pId = (Long) lServerToPserver.get(new Long(rs.getLong(1)));
                    if (pId != null && rs.getLong(2) != pId.longValue()) {
                        lServerToPserver.remove(new Long(rs.getLong(1)));
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (SQLException ex) {
            Session.getLog().error("Unable to reformat lServerToPserver cache", ex);
        }
    }

    public static PhysicalServer getPServerForLServer(long lServerId) throws SQLException {
        PhysicalServer ph;
        synchronized (lServerToPserver) {
            Long pId = (Long) lServerToPserver.get(new Long(lServerId));
            if (pId != null) {
                return getPServer(pId.longValue());
            }
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT p_server_id FROM l_server WHERE l_server.id = ?");
                ps.setLong(1, lServerId);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    synchronized (lServerToPserver) {
                        ph = getPServer(rs.getLong(1));
                        lServerToPserver.put(new Long(lServerId), new Long(rs.getLong(1)));
                    }
                    Session.closeStatement(ps);
                    con.close();
                    return ph;
                }
                Session.closeStatement(ps);
                con.close();
                return null;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    public void delete() {
        synchronized (pservers) {
            pservers.remove(this);
        }
    }

    public Collection exec(String command, String[] args, String input) throws Exception {
        return exec(this.login, command, Arrays.asList(args), input != null ? input.getBytes(LanguageManager.STANDARD_CHARSET) : null);
    }

    public Collection exec(String login, String command, Collection args, byte[] input) throws Exception {
        if (POOLED_CONNECTION) {
            return pooledExec(login, getFullPath(command), args, input);
        }
        return usualExec(login, getFullPath(command), args, input);
    }

    protected String getFullPath(String command) throws SQLException, IOException, ClassNotFoundException {
        if (PackageConfigurator.arePackagesInstalled()) {
            try {
                PackageFile pFile = PackageFile.getFile(command);
                if (pFile != null && pFile.getType() == 4) {
                    Session.getLog().debug("EXEC:" + command);
                    return pFile.getFullPath();
                }
            } catch (IOException e) {
            }
        }
        return PREFIX + command;
    }

    public Collection execSpecial(String login, String command, Collection args, InputStream is) throws Exception {
        return usualExec(login, getFullPath(command), args, is, null);
    }

    protected Collection usualExec(String login, String command, Collection args, byte[] input) throws Exception {
        if (input == null) {
            return usualExec(login, command, args, null, null);
        }
        return usualExec(login, command, args, new ByteArrayInputStream(input), input);
    }

    protected Collection usualExec(String login, String command, Collection args, InputStream is, byte[] input) throws Exception, InterruptedException {
        int exit;
        Session.getLog().debug("PhysicalServer.exec (U) " + command);
        StringBuffer cmd = new StringBuffer("ssh  -a -x ");
        cmd.append(login).append("@").append(this.ip1).append(" ");
        cmd.append(command).append(" ");
        StreamReadingThread t = new StreamReadingThread();
        for (Object obj : args) {
            cmd.append(obj.toString()).append(" ");
        }
        Session.getLog().debug(cmd.toString());
        Process p = Runtime.getRuntime().exec(cmd.toString());
        t.addStream(p.getInputStream());
        t.addStream(p.getErrorStream());
        if (!t.isAlive()) {
            t.start();
        }
        Collection col = new LinkedList();
        if (is != null) {
            try {
                IOUtils.copy(is, p.getOutputStream());
                p.getOutputStream().close();
            } catch (Exception ex) {
                Session.getLog().debug("usualExec exception " + ((Object) cmd) + ":", ex);
                exit = -1;
            }
        }
        exit = p.waitFor();
        t.done();
        col = t.retrieveResult(p.getInputStream()).getResultAsCollection();
        Session.getLog().debug("PhysicalServer.exec before wait " + command);
        Session.getLog().debug(cmd);
        Session.getLog().debug("PhysicalServer.exec after wait " + command + " exitcode:" + exit);
        if (0 != exit) {
            String error = t.retrieveResult(p.getErrorStream()).getResult();
            String errMessage = error + "Command: " + cmd.toString();
            if (input != null) {
                errMessage = errMessage + "\nSTDIN: " + new String(input);
            }
            throw new Exception(errMessage);
        }
        return col;
    }

    protected Collection pooledExec(String login, String command, Collection args, byte[] input) throws Exception {
        Session.getLog().debug("PhysicalServer.exec " + command);
        StringBuffer cmd = new StringBuffer("ssh -x -a ");
        cmd.append(login).append("@").append(this.ip1).append(" ");
        cmd.append(command).append(" ");
        for (Object obj : args) {
            cmd.append(obj.toString()).append(" ");
        }
        Session.getLog().debug(cmd.toString());
        SSHConnection con = SSHPool.get(this.ip1, login);
        try {
            SSHResult result = con.execute(command, args, input);
            SSHPool.release(con);
            Collection col = new LinkedList();
            BufferedReader br = new BufferedReader(new StringReader(result.getOutput()));
            while (true) {
                String tmp = br.readLine();
                if (null == tmp) {
                    break;
                }
                col.add(tmp);
            }
            if (0 != result.getExitCode()) {
                Session.getLog().debug("PhysicalServer some errors: " + result.getExitCode());
                String errMessage = result.getError() + " Command: " + cmd.toString();
                if (input != null) {
                    errMessage = errMessage + "\nSTDIN: " + new String(input);
                }
                throw new Exception(errMessage);
            }
            return col;
        } catch (Throwable th) {
            SSHPool.release(con);
            throw th;
        }
    }

    public String getPFirstIP() {
        return this.ip1;
    }

    public String getPSecondIP() {
        return this.ip2;
    }

    public long getId() {
        return this.f157id;
    }

    public String getLogin() {
        return this.login;
    }

    public void setIp1(String ip1) {
        this.ip1 = ip1;
    }

    public void setIp2(String ip2) {
        this.ip2 = ip2;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public void setMask1(String mask1) {
        this.mask1 = mask1;
    }

    public void setMask2(String mask2) {
        this.mask2 = mask2;
    }

    public String getMask1() {
        return this.mask1;
    }

    public String getMask2() {
        return this.mask2;
    }

    public int getStatus() {
        return this.status;
    }
}
