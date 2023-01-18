package psoft.hsphere.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.Salt;

/* loaded from: hsphere.zip:psoft/hsphere/admin/RsyncTransport.class */
public class RsyncTransport extends SharedObject implements ResourceTransport {
    public static final String RSYNC_START = "start";
    public static final String RSYNC_STOP = "stop";
    public static final String RSYNC_STATUS = "status";

    /* renamed from: id */
    private long f62id;
    private HostEntry srcServer;
    private HostEntry targetServer;
    private String srcPath;
    private String targetPath;
    private String moduleId;
    private Timestamp configured;
    private Timestamp started;
    private Timestamp finished;
    protected static Salt salt;

    static {
        salt = null;
        try {
            salt = new Salt();
        } catch (Exception e) {
        }
    }

    public RsyncTransport(long id, long srcServerId, long targetServerId, String srcPath, String targetPath, String moduleId, Timestamp started, Timestamp finished, Timestamp confugured) throws Exception {
        super(id);
        this.f62id = id;
        this.srcServer = HostManager.getHost(srcServerId);
        this.targetServer = HostManager.getHost(targetServerId);
        this.srcPath = srcPath;
        this.targetPath = targetPath;
        this.moduleId = moduleId;
        this.started = started;
        this.finished = finished;
        this.configured = confugured;
    }

    public static ResourceTransport createTransport(Collection c) throws Exception {
        Iterator i = c.iterator();
        long srcServer = ((Long) i.next()).longValue();
        long targetServer = ((Long) i.next()).longValue();
        String srcPath = (String) i.next();
        String targetPath = (String) i.next();
        return createRsyncTransport(srcServer, targetServer, srcPath, targetPath);
    }

    public static RsyncTransport createRsyncTransport(long srcServerId, long targetServerId, String srcPath, String targtePath) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            long newId = Session.getNewIdAsLong("rsync_job");
            String moduleId = salt.getNext(20);
            ps = con.prepareStatement("INSERT INTO rsync_jobs(id, src_server, target_server, src_path, target_path, module_id, configured)VALUES (?,?,?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setLong(2, srcServerId);
            ps.setLong(3, targetServerId);
            ps.setString(4, srcPath);
            ps.setString(5, targtePath);
            ps.setString(6, moduleId);
            ps.setNull(7, 93);
            ps.executeUpdate();
            RsyncTransport rt = new RsyncTransport(newId, srcServerId, targetServerId, srcPath, targtePath, moduleId, null, null, null);
            Session.closeStatement(ps);
            con.close();
            return rt;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static ResourceTransport accuireTransport(Long id) throws Exception {
        return get(id.longValue());
    }

    public static RsyncTransport get(long id) throws Exception {
        PreparedStatement ps = null;
        RsyncTransport tmp = (RsyncTransport) get(id, RsyncTransport.class);
        if (tmp != null) {
            return tmp;
        }
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT src_server, target_server, src_path, target_path, module_id, started, finished, configured FROM rsync_jobs WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                RsyncTransport rsyncTransport = new RsyncTransport(id, rs.getLong("src_server"), rs.getLong("target_server"), rs.getString("src_path"), rs.getString("target_path"), rs.getString("module_id"), rs.getTimestamp("started"), rs.getTimestamp("finished"), rs.getTimestamp("configured"));
                Session.closeStatement(ps);
                con.close();
                return rsyncTransport;
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

    public String getSrcPath() {
        return this.srcPath;
    }

    public String getTargetPath() {
        return this.targetPath;
    }

    public HostEntry getSrcServer() {
        return this.srcServer;
    }

    public HostEntry getTargetServer() {
        return this.targetServer;
    }

    public String getModuleId() {
        return this.moduleId;
    }

    public boolean isConfigured() {
        return this.configured != null;
    }

    @Override // psoft.hsphere.admin.ResourceTransport
    public Timestamp getStarted() {
        return this.started;
    }

    @Override // psoft.hsphere.admin.ResourceTransport
    public Timestamp getFinished() {
        return this.finished;
    }

    @Override // psoft.hsphere.admin.ResourceTransport
    public boolean isFinished() {
        return this.finished != null;
    }

    @Override // psoft.hsphere.admin.ResourceTransport
    public boolean configure() throws Exception {
        return configure(getTargetServer());
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v19, types: [java.lang.String[], java.lang.String[][]] */
    public boolean configure(HostEntry he) throws Exception {
        Session.getLog().debug("Inside RsyncTransport::configure()");
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                ps = con.prepareStatement("UPDATE rsync_jobs SET configured = ? WHERE id= ?");
                if (he instanceof WinHostEntry) {
                    WinHostEntry winHostEntry = (WinHostEntry) he;
                    ?? r2 = new String[2];
                    String[] strArr = new String[2];
                    strArr[0] = "module-id";
                    strArr[1] = getModuleId();
                    r2[0] = strArr;
                    String[] strArr2 = new String[2];
                    strArr2[0] = "source-path";
                    strArr2[1] = he.equals(getTargetServer()) ? getSrcPath() : getTargetPath();
                    r2[1] = strArr2;
                    winHostEntry.exec("rsync-addmodule.asp", (String[][]) r2);
                } else {
                    HostEntry hostEntry = this.targetServer;
                    String[] strArr3 = new String[2];
                    strArr3[0] = "--module-id=" + getModuleId();
                    strArr3[1] = "--path=" + (he.equals(getTargetServer()) ? getSrcPath() : getTargetPath());
                    hostEntry.exec("add-rsync-module.pl", strArr3);
                }
                Timestamp now = new Timestamp(System.currentTimeMillis());
                ps.setTimestamp(1, now);
                ps.setLong(2, getId());
                ps.executeUpdate();
                this.configured = now;
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException sx) {
                Session.getLog().error("Unable to update configured flag for RsyncJob " + getId() + " though rsync module is configured", sx);
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Unable to configure RsyncJob " + getId(), ex);
                Session.closeStatement(ps);
                con.close();
            }
            return this.configured != null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.admin.ResourceTransport
    public int executeTransfer(boolean force) throws Exception {
        return processRsyncModule(force, true);
    }

    @Override // psoft.hsphere.admin.ResourceTransport
    public int executeTransfer() throws Exception {
        return processRsyncModule(false, true);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v26, types: [java.lang.String[], java.lang.String[][]] */
    public int processRsyncModule(boolean force, boolean launchRsync) throws Exception {
        Session.getLog().debug("Inside RsyncTransport::processRsyncModule()");
        Connection con = Session.getDb();
        if (!isFinished() || force) {
            if (isConfigured() || configure(getTargetServer())) {
                if (!launchRsync || touchDaemon(getTargetServer(), "start")) {
                    try {
                        Iterator output = getSrcServer() instanceof WinHostEntry ? ((WinHostEntry) getSrcServer()).exec("rsync-processmodule.asp", (String[][]) new String[]{new String[]{"source-path", getSrcPath()}, new String[]{"module-id", getModuleId()}, new String[]{"target-ip", getTargetServer().getPFirstIP().toString()}}).iterator() : getSrcServer().exec("process-rsync-module.pl", new String[]{"--source-path=" + getSrcPath(), "--module-id=" + getModuleId(), "--target-ip=" + getTargetServer().getPFirstIP().toString()}).iterator();
                        if (output.hasNext()) {
                            String result = output.next().toString();
                            Session.getLog().debug("rsync job " + getId() + " processing result = " + result);
                            try {
                                try {
                                    PreparedStatement ps1 = con.prepareStatement("UPDATE rsync_jobs SET started = ? WHERE id = ?");
                                    PreparedStatement ps2 = con.prepareStatement("UPDATE rsync_jobs SET finished = ? WHERE id = ?");
                                    new Date();
                                    Timestamp now = new Timestamp(System.currentTimeMillis());
                                    if ("STARTED".equals(result) || "RE-STARTED".equals(result) || "RUNNING".equals(result)) {
                                        if (getStarted() == null) {
                                            ps1.setTimestamp(1, now);
                                            ps1.setLong(2, getId());
                                            ps1.executeUpdate();
                                            this.started = now;
                                        }
                                    } else if ("FINISHED".equals(result)) {
                                        ps2.setTimestamp(1, now);
                                        ps2.setLong(2, getId());
                                        ps2.executeUpdate();
                                        this.finished = now;
                                    }
                                    Session.closeStatement(ps1);
                                    Session.closeStatement(ps2);
                                    con.close();
                                    if ("STARTED".equals(result)) {
                                        return 1;
                                    }
                                    if ("RE-STARTED".equals(result)) {
                                        return 2;
                                    }
                                    if ("RUNNING".equals(result)) {
                                        return 4;
                                    }
                                    return "FINISHED".equals(result) ? 3 : -1;
                                } catch (SQLException ex) {
                                    Session.getLog().error("Unable to update database during processing rsync job " + getId(), ex);
                                    Session.closeStatement(null);
                                    Session.closeStatement(null);
                                    con.close();
                                    return -1;
                                }
                            } catch (Throwable th) {
                                Session.closeStatement(null);
                                Session.closeStatement(null);
                                con.close();
                                throw th;
                            }
                        }
                        return -1;
                    } catch (Exception ex2) {
                        Session.getLog().error("Failed to process rsync job " + getId(), ex2);
                        return -1;
                    }
                }
                return -1;
            }
            return -2;
        }
        return 3;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    public boolean touchDaemon(HostEntry he, String action) {
        String result;
        try {
            if (he instanceof WinHostEntry) {
                Iterator output = ((WinHostEntry) he).exec("rsync.asp", (String[][]) new String[]{new String[]{"command", action}}).iterator();
                if (output.hasNext()) {
                    result = output.next().toString();
                } else {
                    return false;
                }
            } else {
                Iterator output2 = he.exec("rsync.sh", new String[]{action}).iterator();
                if (output2.hasNext()) {
                    result = output2.next().toString();
                } else {
                    return false;
                }
            }
            return "OK".equals(result.trim());
        } catch (Exception ex) {
            Session.getLog().error("An error has occured during " + action + " rsync", ex);
            return false;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v10, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.admin.ResourceTransport
    public void delete() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM rsync_jobs WHERE id = ?");
            ps.setLong(1, this.f62id);
            if (this.targetServer != null) {
                try {
                    if (this.targetServer instanceof WinHostEntry) {
                        ((WinHostEntry) this.targetServer).exec("rsync-delmodule.asp", (String[][]) new String[]{new String[]{"module-id", this.moduleId}});
                    } else {
                        this.targetServer.exec("del-rsync-module.pl", new String[]{"--module-id=" + this.moduleId});
                    }
                    ps.executeUpdate();
                } catch (Exception ex) {
                    Session.getLog().error("Error deletion rsync job " + this.f62id, ex);
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
}
