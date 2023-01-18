package psoft.hsphere.resource.mysql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mysql/MySQLUser.class */
public class MySQLUser extends Resource implements HostDependentResource {
    protected String userName;
    protected String userPasswd;
    protected UserPrivileges userPriv;
    protected ResourceId lockedBy;
    public static final TemplateString STATUS_0 = new TemplateString("0");
    public static final TemplateString STATUS_1 = new TemplateString("1");

    public MySQLUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.lockedBy = null;
        String pref = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.userName = pref + ((String) i.next());
        this.userPasswd = (String) i.next();
    }

    public MySQLUser(ResourceId rId) throws Exception {
        super(rId);
        this.lockedBy = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password, locked_by FROM mysql_users WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.userName = rs.getString(1);
                this.userPasswd = rs.getString(2);
                if (rs.getString(3) != null) {
                    this.lockedBy = new ResourceId(rs.getString(3));
                }
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO mysql_users (id, login, password, parent_id) VALUES (?, ?, ?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.userName);
                ps.setString(3, this.userPasswd);
                ps.setLong(4, getParent().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                throw new HSUserException("mysqluser.exists", new Object[]{this.userName});
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mysql_users WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("name") ? new TemplateString(this.userName) : key.equals("password") ? new TemplateString(this.userPasswd) : key.equals("locked_by") ? new TemplateString(this.lockedBy) : super.get(key);
    }

    private int createMySQLUser(long hostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(hostId);
        if (HostEntry.getEmulationMode()) {
            return 0;
        }
        args.add(this.userName);
        args.add(MailServices.shellQuote(this.userPasswd));
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        return Integer.parseInt(host.exec("mysql-create-user", args).iterator().next().toString());
    }

    public TemplateList FM_getDatabasePrivileges() {
        TemplateList tlist = new TemplateList();
        Hashtable dp = getUserPrivileges().getDatabasePrivileges();
        Enumeration e = dp.keys();
        while (e.hasMoreElements()) {
            TemplateHash th = new TemplateHash();
            String name = (String) e.nextElement();
            String value = (String) dp.get(name);
            th.put("name_priv", name);
            if (value.equals("Y")) {
                th.put("value", "CHECKED");
            } else {
                th.put("value", "");
            }
            tlist.add((TemplateModel) th);
        }
        return tlist;
    }

    public TemplateModel FM_revokeAllDatabasePrivileges() throws Exception {
        Session.getLog().info("MySQLUser: revoking all database privileges for user=" + this.userName);
        if (getUserPrivileges().revokeAllDatabasePrivileges()) {
            return STATUS_1;
        }
        return STATUS_0;
    }

    public TemplateModel FM_setDatabasePrivileges(String privileges) throws Exception {
        getUserPrivileges().setDatabasePrivileges(privileges);
        return this;
    }

    public TemplateModel FM_changeUserPassword(String newpassword) throws Exception {
        HostEntry host = recursiveGet("host");
        List args = new ArrayList();
        args.add(this.userName);
        args.add(MailServices.shellQuote(newpassword));
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        host.exec("mysql-change-user-password", args);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mysql_users SET password = ? WHERE id = ?");
            ps.setString(1, newpassword);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.userPasswd = newpassword;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_loadUserPrivilegesOn(String db_name) throws Exception {
        this.userPriv = new UserPrivileges(this.userName, db_name, Long.parseLong(recursiveGet("host_id").toString()));
        getLog().info("MySQLResource: privileges successfully loaded");
        return this;
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        HostEntry host = recursiveGet("host");
        List args = new ArrayList();
        args.add(this.userName);
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        host.exec("mysql-suspend-user", args);
        super.suspend();
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            HostEntry host = recursiveGet("host");
            List args = new ArrayList();
            args.add(this.userName);
            args.add(MailServices.shellQuote(this.userPasswd));
            if ("1".equals(host.getOption("mysql_clustering"))) {
                args.add(host.getIP().toString());
            }
            host.exec("mysql-resume-user", args);
            super.resume();
        }
    }

    public boolean isNeedToBeDeleted() throws Exception {
        ArrayList args = new ArrayList();
        args.add(this.userName);
        HostEntry host = recursiveGet("host");
        if (HostEntry.getEmulationMode()) {
            return true;
        }
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        LinkedList result = (LinkedList) host.exec("mysql-user-dbs", args);
        return result.size() <= 1;
    }

    public TemplateModel FM_isNeedToBeDeleted() throws Exception {
        return isNeedToBeDeleted() ? STATUS_1 : STATUS_0;
    }

    public UserPrivileges getUserPrivileges() {
        return this.userPriv;
    }

    public String getName() {
        return this.userName;
    }

    public void lock(ResourceId rid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mysql_users SET locked_by = ? WHERE id = ?");
            ps.setString(1, rid.toString());
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.lockedBy = rid;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void unlock() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mysql_users SET locked_by = ? WHERE id = ?");
            ps.setNull(1, 12);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.lockedBy = null;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ResourceId lockedBy() {
        return this.lockedBy;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MySQLUser.refund", new Object[]{this.userName, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.MySQLUser.setup", new Object[]{this.userName});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MySQLUser.recurrent", new Object[]{getPeriodInWords(), this.userName, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.MySQLUser.refundall", new Object[]{this.userName, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        if (createMySQLUser(targetHostId) < 0) {
            throw new HSUserException("mysqluser.exists", new Object[]{this.userName});
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        args.add(this.userName);
        HostEntry host = HostManager.getHost(targetHostId);
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        host.exec("mysql-delete-user", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
