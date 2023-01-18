package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.User;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.Salt;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ContentMoveItem.class */
public class ContentMoveItem extends SharedObject implements TemplateHashModel {
    public static final int RSYNC_MODULE_PROCESS_STARTED = 1;
    public static final int RSYNC_MODULE_PROCESS_RESTARTED = 2;
    public static final int RSYNC_MODULE_PROCESS_FINISHED = 3;
    public static final int RSYNC_MODULE_PROCESS_RUNNING = 4;
    public static final int RSYNC_MODULE_PROCESS_FAILED = -1;
    public static final int RSYNC_MODULE_MISSCONFIGURED = -2;
    public static final int RSYNC_LAUNCH_FAILED = -3;
    public static final String RSYNC_START = "start";
    public static final String RSYNC_STOP = "stop";
    public static final String RSYNC_STATUS = "status";
    public static final int RSYNC_OK = 1;
    public static final int RSYNC_FAIL = -1;

    /* renamed from: df */
    protected static final SimpleDateFormat f170df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
    protected String moduleId;
    protected Timestamp started;
    protected Timestamp finished;
    protected Timestamp suspended;
    protected HostEntry srcServer;
    protected HostEntry targetServer;
    protected String srcPath;
    protected boolean configured;
    protected List postponedResources;
    protected static Salt salt;

    static {
        salt = null;
        try {
            salt = new Salt();
        } catch (Exception e) {
        }
    }

    public ContentMoveItem(long id, String moduleId, String srcPath, long srcServerId, long targetServerId, Timestamp started, Timestamp finished, Timestamp suspended, boolean configured) throws Exception {
        super(id);
        this.postponedResources = null;
        this.moduleId = moduleId;
        this.srcPath = srcPath;
        try {
            this.srcServer = HostManager.getHost(srcServerId);
        } catch (Exception ex) {
            Session.getLog().error("ContentMoveItem " + id + " is unable to get source logical server " + srcServerId, ex);
            this.srcServer = null;
        }
        try {
            this.targetServer = HostManager.getHost(targetServerId);
        } catch (Exception ex2) {
            Session.getLog().error("ContentMoveItem " + id + " is unable to get target logical server " + targetServerId, ex2);
            this.targetServer = null;
        }
        this.started = started;
        this.finished = finished;
        this.suspended = suspended;
        this.configured = configured;
        loadPostponedResources();
    }

    public static ContentMoveItem createContentMoveItem(String srcPath, long srcServerId, long targetServerId) throws Exception {
        PreparedStatement ps = null;
        String newModuleId = salt.getNext(20);
        long newId = Session.getNewIdAsLong();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO content_move(id, module_id,src,target,src_path,configured) VALUES(?,?,?,?,?,?)");
            ps.setLong(1, newId);
            ps.setString(2, newModuleId);
            ps.setLong(3, srcServerId);
            ps.setLong(4, targetServerId);
            ps.setString(5, srcPath);
            ps.setInt(6, 0);
            ps.executeUpdate();
            ContentMoveItem result = new ContentMoveItem(newId, newModuleId, srcPath, srcServerId, targetServerId, null, null, null, false);
            result.addRsyncModule();
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v17, types: [java.lang.String[], java.lang.String[][]] */
    public boolean addRsyncModule() throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                ps = con.prepareStatement("UPDATE content_move SET configured = ? WHERE id= ?");
                if (this.targetServer instanceof WinHostEntry) {
                    ((WinHostEntry) this.targetServer).exec("rsync-addmodule.asp", (String[][]) new String[]{new String[]{"module-id", this.moduleId}, new String[]{"source-path", this.srcPath}});
                } else {
                    this.targetServer.exec("add-rsync-module.pl", new String[]{"--module-id=" + this.moduleId, "--path=" + this.srcPath});
                }
                ps.setInt(1, 1);
                ps.setLong(2, getId());
                ps.executeUpdate();
                this.configured = true;
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException sx) {
                Session.getLog().error("Unable to update configured flag for ContentModuleItem " + this.f51id + " though rsync module is configured", sx);
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().error("Unable to create rsync module for ContentModuleItem " + getId());
                Session.closeStatement(ps);
                con.close();
            }
            return this.configured;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_processRsyncModule(String force, String launchRsync) throws Exception {
        return new TemplateString(processRsyncModule(!isEmpty(force), !isEmpty(launchRsync)));
    }

    public int processRsyncModule() throws SQLException {
        return processRsyncModule(false, false);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v27, types: [java.lang.String[], java.lang.String[][]] */
    public int processRsyncModule(boolean force, boolean launchRsync) throws SQLException {
        Connection con = Session.getDb();
        if (this.finished == null || force) {
            if (this.configured || addRsyncModule()) {
                if (!launchRsync || turnRsyncDaemon("start")) {
                    try {
                        Iterator output = this.srcServer instanceof WinHostEntry ? ((WinHostEntry) this.srcServer).exec("rsync-processmodule.asp", (String[][]) new String[]{new String[]{"source-path", this.srcPath}, new String[]{"module-id", this.moduleId}, new String[]{"target-ip", this.targetServer.getPFirstIP().toString()}}).iterator() : this.srcServer.exec("process-rsync-module.pl", new String[]{"--source-path=" + this.srcPath, "--module-id=" + this.moduleId, "--target-ip=" + this.targetServer.getPFirstIP().toString()}).iterator();
                        if (output.hasNext()) {
                            String result = output.next().toString();
                            Session.getLog().debug("ContentItem " + this.f51id + " module processing result = " + result);
                            try {
                                try {
                                    PreparedStatement ps1 = con.prepareStatement("UPDATE content_move SET started = ? WHERE id = ?");
                                    PreparedStatement ps2 = con.prepareStatement("UPDATE content_move SET finished = ? WHERE id = ?");
                                    Date d = new Date();
                                    Timestamp now = new Timestamp(d.getTime());
                                    if ("STARTED".equals(result) || "RE-STARTED".equals(result) || "RUNNING".equals(result)) {
                                        if (this.started == null) {
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
                                    Session.getLog().error("Unable to update database during processing rsync module for ContentMoveItem " + this.f51id, ex);
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
                        Session.getLog().error("Failed to process ContentModuleItem " + this.f51id, ex2);
                        return -1;
                    }
                }
                return -3;
            }
            return -2;
        }
        return 3;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    public boolean turnRsyncDaemon(String action) {
        try {
            if (this.targetServer instanceof WinHostEntry) {
                ((WinHostEntry) this.targetServer).exec("rsync.asp", (String[][]) new String[]{new String[]{"command", action}}).iterator();
            } else {
                Iterator output = this.targetServer.exec("rsync.sh", new String[]{action}).iterator();
                if (output.hasNext()) {
                    output.next().toString();
                } else {
                    return false;
                }
            }
            if ("OK".equals("OK")) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            Session.getLog().error("An error has occured ruring " + action + " rsync", ex);
            return false;
        }
    }

    public boolean isDeleted() {
        boolean result = true;
        for (int i = 0; i < this.postponedResources.size(); i++) {
            PostponedResource p = (PostponedResource) this.postponedResources.get(i);
            result = result && p.isDeleted();
        }
        return result;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v16, types: [java.lang.String[], java.lang.String[][]] */
    public boolean delRsyncModule() throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                try {
                    ps = con.prepareStatement("UPDATE content_move SET configured = ? WHERE id = ?");
                    ps.setInt(1, 0);
                    ps.setLong(2, getId());
                    if (this.targetServer != null) {
                        if (this.targetServer instanceof WinHostEntry) {
                            ((WinHostEntry) this.targetServer).exec("rsync-delmodule.asp", (String[][]) new String[]{new String[]{"module-id", this.moduleId}});
                        } else {
                            this.targetServer.exec("del-rsync-module.pl", new String[]{"--module-id=" + this.moduleId});
                        }
                    }
                    ps.executeUpdate();
                    this.configured = false;
                    Session.closeStatement(ps);
                    con.close();
                    return true;
                } catch (Exception ex) {
                    Session.getLog().debug("An error occured during deletion of rsync module for ContentMoveItem " + this.f51id, ex);
                    Session.closeStatement(ps);
                    con.close();
                    return false;
                }
            } catch (SQLException sx) {
                Session.getLog().error("Unable to update database during deletion of rsync module for ContentMoveItem " + this.f51id, sx);
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

    public TemplateModel FM_delete() throws Exception {
        deleteExpiredResource(true);
        delete(true);
        return new TemplateOKResult();
    }

    public void delete() throws Exception {
        delete(false);
    }

    public void delete(boolean force) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM content_move WHERE id = ?");
            ps.setLong(1, getId());
            PreparedStatement ps1 = con.prepareStatement("DELETE FROM expired_resources WHERE cm_id = ?");
            ps1.setLong(1, getId());
            if ((!this.configured || delRsyncModule()) && (!isDeleted() || force)) {
                ps.executeUpdate();
                ps1.executeUpdate();
            } else {
                Session.getLog().error("Unable to delete ContentMoveItem " + this.f51id + " : unable to delete rsync module from target server");
            }
            remove(this.f51id, ContentMoveItem.class);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("srcServer".equals(key)) {
            return this.srcServer;
        }
        if ("targetServer".equals(key)) {
            return this.targetServer;
        }
        if ("started".equals(key)) {
            return new TemplateString(getStartedDescr());
        }
        if ("finished".equals(key)) {
            return new TemplateString(getFinishedDescr());
        }
        if ("srcPath".equals(key)) {
            return new TemplateString(this.srcPath);
        }
        if ("isStarted".equals(key)) {
            return new TemplateString(this.started != null);
        } else if ("isFinished".equals(key)) {
            return new TemplateString(this.finished != null && isDeleted());
        } else if ("isSuspended".equals(key)) {
            return new TemplateString(this.suspended != null);
        } else if ("content_type".equals(key)) {
            return new TemplateString(getContentDescription());
        } else {
            if ("module_status".equals(key)) {
                return new TemplateString(getStatusDescr());
            }
            if ("posponed_resources".equals(key)) {
                return new TemplateList(getPostponedResources());
            }
            return super.get(key);
        }
    }

    public static ContentMoveItem get(long id) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT module_id, started, finished, src, target, src_path,  suspended, configured FROM content_move WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ContentMoveItem contentMoveItem = new ContentMoveItem(id, rs.getString("module_id"), rs.getString("src_path"), rs.getLong("src"), rs.getLong("target"), rs.getTimestamp("started"), rs.getTimestamp("finished"), rs.getTimestamp("suspended"), rs.getInt("configured") == 1);
                Session.closeStatement(ps);
                con.close();
                return contentMoveItem;
            }
            throw new Exception("ContentMoveItem with id " + id + " not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_deleteExpiredResource() throws Exception {
        deleteExpiredResource(true);
        return this;
    }

    public void deleteExpiredResource() throws Exception {
        deleteExpiredResource(false);
    }

    public void deleteExpiredResource(boolean force) throws Exception {
        Session.getLog().debug("Inside deleteExpiredResource postponedResources size=" + this.postponedResources.size() + " force=" + force);
        TimeUtils.getSQLTimestamp();
        if (this.finished != null || force) {
            for (int i = 0; i < this.postponedResources.size(); i++) {
                PostponedResource pr = (PostponedResource) this.postponedResources.get(i);
                pr.removeResource(this.srcServer.getId(), force);
            }
        }
    }

    public static List getAllMoveItems() throws Exception {
        PreparedStatement ps = null;
        ArrayList result = new ArrayList();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id FROM content_move");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                result.add(get(rs.getLong(1)));
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected boolean isEmpty(String obj) {
        return obj == null || obj.length() == 0;
    }

    public String getContentDescription() {
        StringBuffer t = new StringBuffer();
        String delim = "";
        for (int i = 0; i < this.postponedResources.size(); i++) {
            PostponedResource p = (PostponedResource) this.postponedResources.get(i);
            t.append(delim + p.getDescription());
            delim = ",";
        }
        return t.toString();
    }

    public String getStartedDescr() {
        return this.started == null ? "Not started" : f170df.format((Date) this.started);
    }

    public String getFinishedDescr() {
        return this.finished == null ? "Not finihed" : f170df.format((Date) this.finished);
    }

    public String getDeletedDescr() {
        return isDeleted() ? "Not deleted" : "Deleted";
    }

    public String getStatusDescr() {
        return this.suspended == null ? this.finished == null ? "Active" : "Finished " + f170df.format((Date) this.finished) : "Suspended" + f170df.format((Date) this.suspended);
    }

    public TemplateModel FM_suspendMove() throws Exception {
        suspendMove();
        return this;
    }

    public void suspendMove() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        Timestamp now = TimeUtils.getSQLTimestamp();
        try {
            ps = con.prepareStatement("UPDATE content_move SET suspended = ? WHERE id = ?");
            ps.setTimestamp(1, now);
            ps.setLong(2, getId());
            delRsyncModule();
            ps.executeUpdate();
            this.suspended = now;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_resumeMove() throws Exception {
        resumeMove();
        return this;
    }

    public void resumeMove() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        TimeUtils.getSQLTimestamp();
        try {
            ps = con.prepareStatement("UPDATE content_move SET suspended = ? WHERE id = ?");
            ps.setNull(1, 93);
            ps.setLong(2, getId());
            addRsyncModule();
            ps.executeUpdate();
            this.suspended = null;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isSuspended() {
        return this.suspended != null;
    }

    public void addPostponedResource(ResourceId rid) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            long ttl = TypeRegistry.getTTL(Integer.toString(rid.getType()));
            ps = con.prepareStatement("INSERT INTO expired_resources(cm_id, rid, rtype, expires) VALUES (?, ?, ?, ?)");
            ps.setLong(1, getId());
            ps.setLong(2, rid.getId());
            ps.setLong(3, rid.getType());
            Timestamp now = new Timestamp(TimeUtils.currentTimeMillis() + (ttl * 60000));
            ps.setTimestamp(4, now);
            ps.executeUpdate();
            this.postponedResources.add(new PostponedResource(rid, now, null));
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void loadPostponedResources() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT rid, rtype, expires, deleted FROM expired_resources WHERE cm_id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            ArrayList p = new ArrayList();
            while (rs.next()) {
                p.add(new PostponedResource(new ResourceId(rs.getString("rid") + "_" + rs.getString("rtype")), rs.getTimestamp("expires"), rs.getTimestamp("deleted")));
            }
            Session.closeStatement(ps);
            con.close();
            this.postponedResources = p;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List getPostponedResources() {
        return this.postponedResources;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ContentMoveItem$PostponedResource.class */
    public class PostponedResource implements TemplateHashModel {
        private ResourceId rid;
        private Timestamp expires;
        private Timestamp deleted;

        public PostponedResource(ResourceId rid, Timestamp expires, Timestamp deleted) {
            ContentMoveItem.this = r4;
            this.rid = rid;
            this.expires = expires;
            this.deleted = deleted;
        }

        public Timestamp getExpiredDate() {
            return this.expires;
        }

        public Timestamp geteDeleDate() {
            return this.deleted;
        }

        public boolean isDeleted() {
            return this.deleted != null;
        }

        public TemplateModel get(String key) throws TemplateModelException {
            if (key.equals("expires")) {
                return new TemplateString(ContentMoveItem.f170df.format((Date) this.expires));
            }
            if (key.equals("deleted")) {
                return new TemplateString(isDeleted() ? ContentMoveItem.f170df.format((Date) this.deleted) : "Not deleted");
            } else if (key.equals("description")) {
                return new TemplateString(getDescription());
            } else {
                return null;
            }
        }

        public boolean isEmpty() {
            return false;
        }

        public String getDescription() {
            try {
                return TypeRegistry.getDescription(this.rid.getType());
            } catch (Exception ex) {
                Session.getLog().error("Error getting description of the postponed resource", ex);
                return "Unavailable";
            }
        }

        public void removeResource(long serverId) throws Exception {
            removeResource(serverId, false);
        }

        public void removeResource(long serverId, boolean force) throws Exception {
            Timestamp now = TimeUtils.getSQLTimestamp();
            if (force || (this.deleted == null && !now.before(this.expires))) {
                PreparedStatement ps = null;
                Connection con = Session.getDb();
                Account oldAcc = Session.getAccount();
                User oldUser = Session.getUser();
                try {
                    ps = con.prepareStatement("UPDATE expired_resources SET deleted = ? WHERE rid = ? AND rtype = ?");
                    ps.setLong(2, this.rid.getId());
                    ps.setLong(3, this.rid.getType());
                    Account a = this.rid.getAccount();
                    User u = a.getUser();
                    Session.setUser(u);
                    Session.setAccount(a);
                    HostDependentResource r = (HostDependentResource) this.rid.get();
                    try {
                        r.physicalDelete(serverId);
                    } catch (Exception ex) {
                        Session.getLog().debug("Unable to perform physical deletion of resource for ContentMoveItem rid=" + this.rid.toString(), ex);
                    }
                    ps.setTimestamp(1, now);
                    ps.executeUpdate();
                    this.deleted = now;
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                    Session.closeStatement(ps);
                    con.close();
                } catch (Throwable th) {
                    Session.setUser(oldUser);
                    Session.setAccount(oldAcc);
                    Session.closeStatement(ps);
                    con.close();
                    throw th;
                }
            }
        }
    }
}
