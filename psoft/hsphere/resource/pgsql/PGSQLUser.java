package psoft.hsphere.resource.pgsql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
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
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/pgsql/PGSQLUser.class */
public class PGSQLUser extends Resource implements HostDependentResource {
    protected String userName;
    protected String userPasswd;
    protected String rootLogin;

    public PGSQLUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
        String pref = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.userName = pref + ((String) i.next());
        this.userPasswd = (String) i.next();
    }

    public PGSQLUser(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password FROM pgsql_users WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.userName = rs.getString(1);
                this.userPasswd = rs.getString(2);
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
                ps = con.prepareStatement("INSERT INTO pgsql_users (id, login, password, parent_id) VALUES (?, ?, ?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.userName);
                ps.setString(3, this.userPasswd);
                ps.setLong(4, getParent().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                throw new HSUserException("pgsqluser.exists", new Object[]{this.userName});
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
            ps = con.prepareStatement("DELETE FROM pgsql_users WHERE id = ?");
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
        if (key.equals("name")) {
            return new TemplateString(this.userName);
        }
        if (key.equals("password")) {
            return new TemplateString(this.userPasswd);
        }
        if (key.equals("host_id")) {
            try {
                return new TemplateString(getHostId());
            } catch (Exception e) {
                Session.getLog().error("Exception during retrieving of host id occured", e);
                return null;
            }
        }
        return super.get(key);
    }

    public TemplateModel FM_changeUserPassword(String newpassword) throws Exception {
        Session.getLog().info("PGSQLResource: process to change user " + this.userName + " password");
        HostEntry host = recursiveGet("host");
        List args = new ArrayList();
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.userName);
        args.add(MailServices.shellQuote(newpassword));
        host.exec("pgsql-change-user-password", args);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE pgsql_users SET password = ? WHERE id = ?");
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

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        HostEntry host = recursiveGet("host");
        List args = new ArrayList();
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.userName);
        host.exec("pgsql-suspend-user", args);
        super.suspend();
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            HostEntry host = recursiveGet("host");
            List args = new ArrayList();
            args.add(((PGSQLResource) getParent().get()).getRootLogin());
            args.add(this.userName);
            host.exec("pgsql-resume-user", args);
            super.resume();
        }
    }

    public TemplateModel FM_isDBOwner() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) FROM pgsqldb WHERE db_ownerid = (SELECT id FROM pgsql_users WHERE login = ?)");
            ps2.setString(1, this.userName);
            ResultSet rs = ps2.executeQuery();
            rs.next();
            return rs.getInt(1) > 0 ? this : null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public String getName() {
        return this.userName;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.pgsqluser.refund", new Object[]{this.userName, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.pgsqluser.setup", new Object[]{this.userName});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.pgsqluser.recurrent", new Object[]{getPeriodInWords(), this.userName, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.pgsqluser.refundall", new Object[]{this.userName, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        if (HostEntry.getEmulationMode()) {
            return;
        }
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.userName);
        args.add(MailServices.shellQuote(this.userPasswd));
        host.exec("pgsql-create-user", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.userName);
        HostEntry host = HostManager.getHost(targetHostId);
        host.exec("pgsql-delete-user", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
