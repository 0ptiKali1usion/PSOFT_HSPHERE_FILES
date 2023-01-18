package psoft.hsphere.resource.mssql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
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
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mssql/MSSQLUser.class */
public class MSSQLUser extends Resource implements HostDependentResource {
    protected String name;
    protected long login;
    protected long database;

    public MSSQLUser(int type, Collection initValues) throws Exception {
        super(type, initValues);
        String prefix = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.name = prefix + ((String) i.next());
        ResourceId resId = new ResourceId((String) i.next());
        this.login = resId.getId();
        ResourceId tmpdatabase = new ResourceId((String) i.next());
        this.database = tmpdatabase.getId();
    }

    public MSSQLUser(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT user_name, mssql_login_id, mssql_db_id FROM mssql_users WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.login = rs.getLong(2);
                this.database = rs.getLong(3);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public String getName() {
        return this.name;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        int loginId = Integer.parseInt(TypeRegistry.getTypeId("MSSQLLogin"));
        MSSQLLogin mssql_login = new MSSQLLogin(new ResourceId(this.login, loginId));
        String user_login = mssql_login.getName();
        int dbId = Integer.parseInt(TypeRegistry.getTypeId("MSSQLDatabase"));
        MSSQLDatabase mssql_db = new MSSQLDatabase(new ResourceId(this.database, dbId));
        String db_name = mssql_db.getName();
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("create", new String[]{new String[]{"resourcename", "mssqluser"}, new String[]{"database", db_name}, new String[]{FMACLManager.USER, this.name}, new String[]{"login", user_login}});
        } else {
            he.exec("mssql-adduser.asp", (String[][]) new String[]{new String[]{"database", db_name}, new String[]{FMACLManager.USER, this.name}, new String[]{"login", user_login}});
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        int dbId = Integer.parseInt(TypeRegistry.getTypeId("MSSQLDatabase"));
        MSSQLDatabase mssql_db = new MSSQLDatabase(new ResourceId(this.database, dbId));
        String db_name = mssql_db.getName();
        if (WinService.isSOAPSupport()) {
            int loginId = Integer.parseInt(TypeRegistry.getTypeId("MSSQLLogin"));
            MSSQLLogin mssql_login = new MSSQLLogin(new ResourceId(this.login, loginId));
            String user_login = mssql_login.getName();
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "mssqluser"}, new String[]{"database", db_name}, new String[]{FMACLManager.USER, this.name}, new String[]{"login", user_login}});
            return;
        }
        he.exec("mssql-deluser.asp", (String[][]) new String[]{new String[]{"database", db_name}, new String[]{FMACLManager.USER, this.name}});
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
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT into mssql_users (id, user_name, mssql_login_id, mssql_db_id) VALUES (?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.name);
            ps.setLong(3, this.login);
            ps.setLong(4, this.database);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            physicalCreate(getHostId());
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
            ps = con.prepareStatement("DELETE FROM mssql_users WHERE id = ?");
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
            return new TemplateString(this.name);
        }
        if (!key.equals("login")) {
            return key.equals("database") ? new TemplateString(this.database) : super.get(key);
        }
        try {
            int loginId = Integer.parseInt(TypeRegistry.getTypeId("MSSQLLogin"));
            return new TemplateString(new ResourceId(this.login, loginId));
        } catch (Exception e) {
            Session.getLog().error("Login not found ", e);
            return null;
        }
    }

    protected String _getName() {
        try {
            int dbId = Integer.parseInt(TypeRegistry.getTypeId("MSSQLDatabase"));
            MSSQLDatabase mssql_db = new MSSQLDatabase(new ResourceId(this.database, dbId));
            return mssql_db.getName();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLUser.refund", new Object[]{this.name, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.MSSQLUser.setup", new Object[]{this.name, _getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLUser.recurrent", new Object[]{getPeriodInWords(), this.name, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.MSSQLUser.refundall", new Object[]{this.name, _getName(), f42df.format(begin), f42df.format(end)});
    }
}
