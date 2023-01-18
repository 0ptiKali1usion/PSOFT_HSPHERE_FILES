package psoft.hsphere.resource.mysql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.LineNumberReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mysql/MySQLDatabase.class */
public class MySQLDatabase extends Resource implements HostDependentResource {
    protected String db_name;
    protected String db_description;
    protected ResourceId lockedBy;

    public String getName() {
        return this.db_name;
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{this.db_name};
    }

    public MySQLDatabase(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.lockedBy = null;
        String pref = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.db_name = pref + ((String) i.next());
        this.db_description = (String) i.next();
    }

    public MySQLDatabase(ResourceId rId) throws Exception {
        super(rId);
        this.lockedBy = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT db_name, db_description, locked_by FROM mysqldb WHERE id = ?");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.db_name = rs.getString(1);
                this.db_description = rs.getString(2);
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
    public void delete() throws Exception {
        super.delete();
        deleteDatabaseUsers();
        Connection con = Session.getDb();
        if (this.initialized) {
            ArrayList args = new ArrayList();
            HostEntry host = recursiveGet("host");
            args.add(this.db_name);
            if ("1".equals(host.getOption("mysql_clustering"))) {
                args.add(host.getIP().toString());
            }
            host.exec("mysql-drop-database", args);
        }
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mysqldb WHERE id = ?");
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
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO mysqldb(id, db_name, db_description, parent_id) VALUES (? ,? ,?, ?)");
                ps.setLong(1, getId().getId());
                ps.setString(2, this.db_name);
                ps.setString(3, this.db_description);
                ps.setLong(4, getParent().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                physicalCreate(getHostId());
            } catch (SQLException e) {
                throw new HSUserException("mysqldatabase.exists", new Object[]{this.db_name});
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("db_name")) {
            return new TemplateString(this.db_name);
        }
        if (key.equals("db_description")) {
            return new TemplateString(this.db_description);
        }
        return key.equals("locked_by") ? this.lockedBy : super.get(key);
    }

    private String createDatabase(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        if (HostEntry.getEmulationMode()) {
            return "0";
        }
        args.add(this.db_name);
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        String result = "";
        Iterator output = host.exec("mysql-create-db", args).iterator();
        if (output.hasNext()) {
            result = output.next().toString();
        }
        return result;
    }

    private LinkedList getUserNames() throws Exception {
        ArrayList args = new ArrayList();
        args.add(this.db_name);
        HostEntry host = recursiveGet("host");
        if (HostEntry.getEmulationMode()) {
            return new LinkedList();
        }
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        return (LinkedList) host.exec("mysql-db-users", args);
    }

    public List getUsers() throws Exception {
        LinkedList userNames = getUserNames();
        ArrayList users = new ArrayList();
        Connection con = Session.getDb();
        for (int i = 0; i < userNames.size(); i++) {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM mysql_users WHERE login = ?");
                ps.setString(1, (String) userNames.get(i));
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    users.add(new ResourceId(rs.getLong("id"), Integer.parseInt(TypeRegistry.getTypeId("MySQLUser"))));
                }
                Session.closeStatement(ps);
            } catch (Exception e) {
                getLog().info("MySQLDatabase: " + e.toString());
                throw e;
            }
        }
        return users;
    }

    public TemplateModel FM_getUsers() throws Exception {
        return new TemplateList(getUsers());
    }

    private void deleteDatabaseUsers() throws Exception {
        ArrayList users = (ArrayList) getUsers();
        Iterator i = users.iterator();
        while (i.hasNext()) {
            MySQLUser user = (MySQLUser) ((ResourceId) i.next()).get();
            UserPrivileges userPriv = new UserPrivileges(user.getName(), this.db_name, Long.parseLong(recursiveGet("host_id").toString()));
            if (user.isNeedToBeDeleted()) {
                user.delete(false);
            } else {
                userPriv.revokeAllDatabasePrivileges();
            }
        }
    }

    public void batchSQL(ResourceId userId, LineNumberReader listComand) throws Exception {
        String mysqlBox;
        Class.forName("org.gjt.mm.mysql.Driver");
        HostEntry host = recursiveGet("host");
        if ("1".equals(host.getOption("mysql_clustering"))) {
            mysqlBox = host.getIP().toString();
        } else {
            mysqlBox = host.getPFirstIP();
        }
        String db_url = "jdbc:mysql://" + mysqlBox + "/" + this.db_name + "?jdbcCompliantTruncation=no";
        Session.getLog().debug("UserPrivileges " + db_url);
        Connection con = null;
        try {
            con = DriverManager.getConnection(db_url, String.valueOf(userId.get("name")), String.valueOf(userId.get("password")));
            Statement stm = con.createStatement();
            StringBuffer buffer = new StringBuffer();
            while (true) {
                String line = listComand.readLine();
                if (line == null) {
                    break;
                }
                String line2 = line.trim();
                if (!"".equals(line2) && !line2.trim().startsWith("#")) {
                    if (!line2.endsWith(";")) {
                        buffer.append(line2).append("\n");
                    } else {
                        buffer.append(line2.substring(0, line2.lastIndexOf(59)));
                        stm.executeUpdate(buffer.toString());
                        buffer = new StringBuffer("");
                    }
                }
            }
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public void lock(ResourceId rid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mysqldb SET locked_by = ? WHERE id = ?");
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
            ps = con.prepareStatement("UPDATE mysqldb SET locked_by = ? WHERE id = ?");
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
        return Localizer.translateMessage("bill.MySQLDatabase.refund", new Object[]{this.db_name, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.MySQLDatabase.setup", new Object[]{this.db_name});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.MySQLDatabase.recurrent", new Object[]{getPeriodInWords(), this.db_name, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.MySQLDatabase.refundall", new Object[]{this.db_name, f42df.format(begin), f42df.format(end)});
    }

    public static Hashtable getMySQLInfoByName(String dbName) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, account_id FROM mysqldb, parent_child WHERE db_name = ? AND mysqldb.id + 0 = parent_child.child_id ");
            ps.setString(1, dbName);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Hashtable db = new Hashtable(2);
                db.put("resource_id", new Long(rs.getLong(1)));
                db.put("account_id", new Long(rs.getLong(2)));
                Session.getLog().debug("MySQLResource:" + dbName + " account_id:" + rs.getLong(2) + " resourceId:" + rs.getLong(1));
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

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        if ("-1".equals(createDatabase(targetHostId))) {
            throw new HSUserException("mysqldatabase.exists", new Object[]{this.db_name});
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        args.add(this.db_name);
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        host.exec("mysql-drop-database", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    public String getDatabasePrefix() {
        return Session.getUser().getLogin() + "-";
    }

    public String getUserPrefix() {
        return "";
    }
}
