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
import java.util.Hashtable;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/pgsql/PGSQLDatabase.class */
public class PGSQLDatabase extends Resource implements HostDependentResource {
    protected String db_name;
    protected String db_description;
    protected long db_ownerid;
    protected String db_owner;

    public String getName() {
        return this.db_name;
    }

    public long getOwnerId() {
        return this.db_ownerid;
    }

    public String getOwner() {
        return this.db_owner;
    }

    public PGSQLDatabase(int type, Collection initValues) throws Exception {
        super(type, initValues);
        String pref = Session.getUser().getUserPrefix();
        Iterator i = initValues.iterator();
        this.db_name = pref + ((String) i.next());
        this.db_description = (String) i.next();
        this.db_owner = (String) i.next();
    }

    public PGSQLDatabase(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT b.db_name, b.db_description,u.login, b.db_ownerid FROM pgsqldb b, pgsql_users u WHERE b.id = ? AND b.db_ownerid = u.id");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.db_name = rs.getString(1);
                this.db_description = rs.getString(2);
                this.db_owner = rs.getString(3);
                this.db_ownerid = rs.getLong(4);
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
            PreparedStatement ps2 = con.prepareStatement("SELECT id FROM pgsql_users WHERE login = ?");
            ps2.setString(1, this.db_owner);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                this.db_ownerid = rs.getLong(1);
            } else {
                notFound();
            }
            ps2.close();
            ps = con.prepareStatement("INSERT INTO pgsqldb(id, db_name, db_description, parent_id, db_ownerid) VALUES (? ,? ,?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.db_name);
            ps.setString(3, this.db_description);
            ps.setLong(4, getParent().getId());
            ps.setLong(5, this.db_ownerid);
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
            ps = con.prepareStatement("DELETE FROM pgsqldb WHERE id = ?");
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
            return new TemplateString(this.db_name);
        }
        if (key.equals("db_description")) {
            return new TemplateString(this.db_description);
        }
        return key.equals("db_owner") ? new TemplateString(this.db_owner) : key.equals("db_owner_r") ? new ResourceId(this.db_ownerid, 6902) : super.get(key);
    }

    protected void createDatabase() throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = recursiveGet("host");
        if (HostEntry.getEmulationMode()) {
            return;
        }
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.db_name);
        args.add(this.db_owner);
        host.exec("pgsql-create-db", args);
    }

    public TemplateModel FM_changeDatabaseDescription(String newdscr) throws Exception {
        Session.getLog().info("PGSQLDatabase:change database description " + this.db_name);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE pgsqldb SET db_description = ? WHERE id = ?");
            ps.setString(1, newdscr);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.db_description = newdscr;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.pgsqldatabase.refund", new Object[]{this.db_name, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.pgsqldatabase.setup", new Object[]{this.db_name});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.pgsqldatabase.recurrent", new Object[]{getPeriodInWords(), this.db_name, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.pgsqldatabase.refundall", new Object[]{this.db_name, f42df.format(begin), f42df.format(end)});
    }

    public static Hashtable getPGSQLInfoByName(String dbName) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, account_id FROM pgsqldb, parent_child WHERE db_name = ? AND pgsqldb.id + 0 = parent_child.child_id ");
            ps.setString(1, dbName);
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Hashtable db = new Hashtable(2);
                db.put("resource_id", new Long(rs.getLong(1)));
                db.put("account_id", new Long(rs.getLong(2)));
                Session.getLog().debug("PGSQLResource:" + dbName + " account_id:" + rs.getLong(2) + " resourceId:" + rs.getLong(1));
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
    public void physicalCreate(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        if (HostEntry.getEmulationMode()) {
            return;
        }
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.db_name);
        args.add(this.db_owner);
        host.exec("pgsql-create-db", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        args.add(((PGSQLResource) getParent().get()).getRootLogin());
        args.add(this.db_name);
        host.exec("pgsql-drop-database", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }
}
