package psoft.hsphere.resource.mssql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
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

/* loaded from: hsphere.zip:psoft/hsphere/resource/mssql/MSSQLDatabase.class */
public class MSSQLDatabase extends Resource implements HostDependentResource {
    protected String name;
    protected long login_id;
    protected int quota_perc;

    public MSSQLDatabase(int type, Collection initValues) throws Exception {
        super(type, initValues);
        String prefix = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.name = prefix + ((String) i.next());
        this.login_id = new ResourceId((String) i.next()).getId();
        Integer tmpInt = new Integer(Integer.parseInt((String) i.next()));
        this.quota_perc = tmpInt.intValue();
    }

    public MSSQLDatabase(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mssql_db_name, mssql_login_id, mssql_quota_perc FROM mssql_dbs WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.name = rs.getString(1);
                this.login_id = rs.getLong(2);
                this.quota_perc = rs.getInt(3);
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
            he.invokeMethod("create", new String[]{new String[]{"resourcename", "mssqldatabase"}, new String[]{"database", this.name}, new String[]{"login", getLoginName()}});
        } else {
            he.exec("mssql-createdatabase.asp", (String[][]) new String[]{new String[]{"database", this.name}, new String[]{"login", getLoginName()}});
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "mssqldatabase"}, new String[]{"database", this.name}, new String[]{"login", getLoginName()}});
        } else {
            he.exec("mssql-dropdatabase.asp", (String[][]) new String[]{new String[]{"database", this.name}});
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
                PreparedStatement ps = con.prepareStatement("SELECT mssql_db_name FROM mssql_dbs WHERE mssql_db_name = ?");
                ps.setString(1, this.name);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("Database exists: " + this.name);
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT into mssql_dbs (id, mssql_db_name, mssql_login_id, mssql_quota_perc) VALUES (?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.name);
                ps2.setLong(3, this.login_id);
                ps2.setInt(4, this.quota_perc);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                throw new HSUserException("mssqldatabase.exists", new Object[]{this.name});
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
            ps = con.prepareStatement("DELETE FROM mssql_dbs WHERE id = ?");
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
        if (key.equals("db_name")) {
            return new TemplateString(this.name);
        }
        if (key.equals("login_id")) {
            return new TemplateString(String.valueOf(this.login_id));
        }
        if (key.equals("quota_perc")) {
            return new TemplateString(String.valueOf(this.quota_perc));
        }
        if (key.equals("login_name")) {
            return new TemplateString(getLoginName());
        }
        return super.get(key);
    }

    public TemplateModel FM_isUserExist(String name, String database) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("MSSQLDatabase::Checking existance of MSSQL user: " + prefix + name);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM  mssql_users WHERE user_name = ? AND mssql_db_id = ?");
            ResourceId dbId = new ResourceId(database);
            ps.setString(1, prefix + name);
            ps.setLong(2, dbId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int id = rs.getInt(1);
                ResourceId usrId = new ResourceId(id, new Integer(Integer.parseInt(TypeRegistry.getTypeId("MSSQLUser"))).intValue());
                MSSQLUser result = (MSSQLUser) usrId.get();
                Session.closeStatement(ps);
                con.close();
                return result;
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

    public String getLoginName() {
        String result = new String();
        try {
            Session.getAccount();
            int typeId = new Integer(Integer.parseInt(TypeRegistry.getTypeId("MSSQLLogin"))).intValue();
            ResourceId resId = new ResourceId(this.login_id, typeId);
            MSSQLLogin login = (MSSQLLogin) resId.get();
            result = login.getName();
        } catch (Exception e) {
            Session.getLog().error(e.toString());
        }
        return result;
    }

    public TemplateModel FM_getLoginName() throws Exception {
        return new TemplateString(getLoginName());
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_setDatabaseStatus(String dbstatus) throws Exception {
        WinHostEntry whe = (WinHostEntry) recursiveGet("host");
        whe.exec("mssql-databasestatus.asp", (String[][]) new String[]{new String[]{"database", this.name}, new String[]{"status", dbstatus}});
        return this;
    }

    public TemplateModel FM_changeQuotaPerc(String newquotaperc) throws Exception {
        int qperc = new Integer(Integer.parseInt(newquotaperc.trim(), 10)).intValue();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mssql_dbs SET mssql_quota_perc = ? WHERE id = ?");
            ps.setInt(1, qperc);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.quota_perc = qperc;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getName() {
        return this.name;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLDatabase.refund", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.MSSQLDatabase.setup", new Object[]{getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MSSQLDatabase.recurrent", new Object[]{getPeriodInWords(), getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.MSSQLDatabase.refundall", new Object[]{getName(), f42df.format(begin), f42df.format(end)});
    }

    public static Hashtable getMSSQLInfoByName(String dbName) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, account_id FROM mssql_dbs, parent_child WHERE mssql_db_name = ? AND mssql_dbs.id + 0 = parent_child.child_id ");
            ps.setString(1, dbName);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Hashtable db = new Hashtable(2);
                db.put("resource_id", new Long(rs.getLong(1)));
                db.put("account_id", new Long(rs.getLong(2)));
                Session.getLog().debug("MSSQLResource:" + dbName + " account_id:" + rs.getLong(2) + " resourceId:" + rs.getLong(1));
                Session.closeStatement(ps);
                con.close();
                return db;
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
