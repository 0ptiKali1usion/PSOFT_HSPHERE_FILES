package psoft.hsphere.resource.mysql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.UniqueNameGenerator;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mysql/MySQLResource.class */
public class MySQLResource extends Resource implements HostDependentResource {
    private long hostId;
    public static final TemplateString STATUS_0 = new TemplateString("0");
    public static final TemplateString STATUS_1 = new TemplateString("1");
    public static final int MAX_DB_NAME = 50;
    public static final int MAX_USER_NAME = 16;

    public MySQLResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        HostEntry he = (HostEntry) i.next();
        this.hostId = he.getId();
    }

    public MySQLResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mysql_host_id FROM mysqlres WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.hostId = rs.getLong(1);
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
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO mysqlres (id, mysql_host_id) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, this.hostId);
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
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mysqlres WHERE id = ?");
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
        if (key.equals("host_id")) {
            return new TemplateString(this.hostId);
        }
        try {
            if (key.equals("host")) {
                return HostManager.getHost(this.hostId);
            }
            if ("prefix".equals(key)) {
                try {
                    return new TemplateString(Session.getUser().getUserPrefix());
                } catch (Exception ex) {
                    Session.getLog().error("Error getting user prefix ", ex);
                    throw new TemplateModelException("Error getting user prefix for user " + Session.getUser().getLogin());
                }
            }
            if ("phpmyadminhost".equals(key)) {
                try {
                    return new TemplateString(getPHPMyAdminHost());
                } catch (Exception e) {
                    Session.getLog().error("Failed to get PHPMyAdmin Host for MySQL server id " + this.hostId, e);
                }
            }
            return super.get(key);
        } catch (Exception e2) {
            getLog().warn("no host entry for MySQL resource", e2);
            return null;
        }
    }

    public String getUniqueUsername(String userName) throws Exception {
        String name;
        int maxSize = (16 - Session.getUser().getUserPrefix().length()) - 1;
        UniqueNameGenerator ung = new UniqueNameGenerator(userName, maxSize, 99);
        do {
            name = ung.next();
            if (name == null) {
                return null;
            }
        } while (isUserExist(name));
        return name;
    }

    public boolean isUserExist(String userName) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("Checing existance of MySQL user: " + prefix + userName);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login FROM mysqlres, mysql_users WHERE mysqlres.id = parent_id AND mysql_host_id = ? AND login = ?");
            ps.setLong(1, this.hostId);
            ps.setString(2, prefix + userName);
            ResultSet rs = ps.executeQuery();
            boolean next = rs.next();
            Session.closeStatement(ps);
            con.close();
            return next;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_isUserExist(String userName) throws Exception {
        return isUserExist(userName) ? STATUS_1 : STATUS_0;
    }

    public String getUniqueDatabaseName(String dbName) throws Exception {
        String name;
        int length = (50 - Session.getUser().getUserPrefix().length()) - 1;
        UniqueNameGenerator ung = new UniqueNameGenerator(dbName, 50, 99);
        do {
            name = ung.next();
            if (name == null) {
                return null;
            }
        } while (isDatabaseExist(name));
        return name;
    }

    public boolean isDatabaseExist(String dbName) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("Checking existance of MySQL database: " + prefix + dbName);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT db_name FROM mysqlres, mysqldb WHERE mysqlres.id = parent_id AND mysql_host_id = ? AND db_name = ?");
            ps.setLong(1, this.hostId);
            ps.setString(2, prefix + dbName);
            ResultSet rs = ps.executeQuery();
            boolean next = rs.next();
            Session.closeStatement(ps);
            con.close();
            return next;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_isDatabaseExist(String dbName) throws Exception {
        return isDatabaseExist(dbName) ? STATUS_1 : STATUS_0;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE mysqlres SET mysql_host_id = ? WHERE id = ?");
            ps.setLong(1, newHostId);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return this.hostId;
    }

    protected String getPHPMyAdminHost() throws Exception {
        String resultHostName;
        HostEntry he = HostManager.getHost(getHostId());
        String webId = he.getOption("phpmyadmin");
        if (webId != null && !"".equals(webId)) {
            HostEntry heWeb = HostManager.getHost(webId);
            resultHostName = heWeb.getName();
        } else {
            resultHostName = he.getName();
        }
        return resultHostName;
    }
}
