package psoft.hsphere.resource.mssql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mssql/MSSQLLogin.class */
public class MSSQLLogin extends Resource implements HostDependentResource {
    protected String login;
    protected String password;

    public MSSQLLogin(int type, Collection initValues) throws Exception {
        super(type, initValues);
        String prefix = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.login = prefix + ((String) i.next());
        this.password = (String) i.next();
    }

    public MSSQLLogin(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password FROM mssql_logins WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.login = rs.getString(1);
                this.password = rs.getString(2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        if (WinService.isSOAPSupport()) {
            try {
                he.invokeMethod("create", new String[]{new String[]{"resourcename", "mssqllogin"}, new String[]{"login", this.login}, new String[]{"password", this.password}});
                return;
            } catch (Exception e) {
                return;
            }
        }
        he.exec("mssql-createlogin.asp", (String[][]) new String[]{new String[]{"login", this.login}, new String[]{"password", this.password}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        List users = getAllUsers();
        int size = users.size();
        for (int i = 0; i < size; i++) {
            ResourceId oneUserId = (ResourceId) users.get(i);
            MSSQLUser oneUser = (MSSQLUser) oneUserId.get();
            oneUser.physicalDelete(targetHostId);
        }
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "mssqllogin"}, new String[]{"login", this.login}, new String[]{"password", this.password}});
        } else {
            he.exec("mssql-removelogin.asp", (String[][]) new String[]{new String[]{"login", this.login}});
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM mssql_logins WHERE login = ?");
                ps.setString(1, this.login);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("MSSQL Login exists: " + this.login);
                }
                PreparedStatement ps2 = con.prepareStatement("INSERT into mssql_logins (id, login, password) VALUES (?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.login);
                ps2.setString(3, this.password);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting MS SQL login", e);
                throw new HSUserException("mssql.exists");
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
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
            ps = con.prepareStatement("DELETE FROM mssql_logins WHERE id = ?");
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
        return key.equals("name") ? new TemplateString(this.login) : key.equals("password") ? new TemplateString(this.password) : super.get(key);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v12, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v14, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_changeLoginPassword(String newpassword) throws Exception {
        PreparedStatement ps;
        Session.getLog().info("MSSQLResource: process to change password " + this.login + " password");
        boolean mylogin = false;
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            ps2 = con.prepareStatement("SELECT login FROM mssql_logins WHERE login = ?");
            ps2.setString(1, this.login);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                WinHostEntry whe = (WinHostEntry) recursiveGet("host");
                if (WinService.isSOAPSupport()) {
                    whe.invokeMethod("update", new String[]{new String[]{"resourcename", "mssqllogin"}, new String[]{"login", this.login}, new String[]{"password", newpassword}});
                } else {
                    whe.exec("mssql-changeloginpassword.asp", (String[][]) new String[]{new String[]{"login", this.login}, new String[]{"password", newpassword}});
                }
                mylogin = true;
            }
            if (mylogin) {
                Connection con1 = Session.getDb();
                try {
                    ps = con1.prepareStatement("UPDATE mssql_logins SET password = ? WHERE id = ?");
                    ps.setString(1, newpassword);
                    ps.setLong(2, getId().getId());
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                    con1.close();
                    this.password = newpassword;
                } catch (Throwable th) {
                    Session.closeStatement(ps);
                    con1.close();
                    throw th;
                }
            }
            return this;
        } finally {
            Session.closeStatement(ps2);
            con.close();
        }
    }

    public String getName() {
        return this.login;
    }

    public List getAllUsers() throws Exception {
        List result = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM mssql_users WHERE mssql_login_id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long user_id = rs.getLong(1);
                int userType = new Integer(Integer.parseInt(TypeRegistry.getTypeId("MSSQLUser"))).intValue();
                Session.getAccount();
                ResourceId resId = new ResourceId(user_id, userType);
                result.add(resId);
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

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLLogin.refund", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.MSSQLLogin.setup", new Object[]{getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLLogin.recurrent", new Object[]{getPeriodInWords(), getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.MSSQLLogin.refundall", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }
}
