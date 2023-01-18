package psoft.hsphere.resource.pgsql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/pgsql/PGSQLResource.class */
public class PGSQLResource extends Resource {
    protected String postgresPassword;
    protected long hostId;
    protected static final long TIME_TO_LIVE = 300000;
    public Map rlogin;

    public PGSQLResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.postgresPassword = null;
        this.rlogin = new HashMap();
        Iterator i = initValues.iterator();
        HostEntry he = (HostEntry) i.next();
        this.hostId = he.getId();
        Session.getLog().info("PGSQLResource:Resource init by first const.");
    }

    public PGSQLResource(ResourceId rId) throws Exception {
        super(rId);
        this.postgresPassword = null;
        this.rlogin = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT pgsql_host_id FROM pgsqlres WHERE id = ?");
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
            ps = con.prepareStatement("INSERT INTO pgsqlres (id, pgsql_host_id) VALUES (?, ?)");
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
            ps = con.prepareStatement("DELETE FROM pgsqlres WHERE id = ?");
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
                } catch (Exception e) {
                    throw new TemplateModelException("Unable to get prefix for user " + Session.getUser().getLogin());
                }
            }
            if ("phppgadminhost".equals(key)) {
                try {
                    return new TemplateString(getPHPPgAdminHost());
                } catch (Exception e2) {
                    Session.getLog().error("Failed to get PHPPgAdmin Host for PgSQL server id " + this.hostId, e2);
                }
            }
            return super.get(key);
        } catch (Exception e3) {
            getLog().warn("no host entry for PGSQL resource", e3);
            return null;
        }
    }

    public TemplateModel FM_isUserExist(String userName) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("Checking PSQL user existance: " + prefix + userName);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT login FROM pgsqlres, pgsql_users WHERE pgsqlres.id = parent_id AND pgsql_host_id = ? AND login = ?");
            ps2.setLong(1, this.hostId);
            ps2.setString(2, prefix + userName);
            ResultSet rs = ps2.executeQuery();
            return rs.next() ? this : null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_isDatabaseExist(String dbName) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("Checking PGSQL database existance: " + prefix + dbName);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT db_name FROM pgsqlres, pgsqldb WHERE pgsqlres.id = parent_id AND pgsql_host_id = ? AND db_name = ?");
            ps2.setLong(1, this.hostId);
            ps2.setString(2, prefix + dbName);
            ResultSet rs = ps2.executeQuery();
            return rs.next() ? this : null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void RootLogin() throws Exception {
        ArrayList args = new ArrayList();
        new LinkedList();
        HostEntry host = get("host");
        getLog().info("PGSQLResource.RootLogin() : host name is: " + host.getName());
        LinkedList result = (LinkedList) host.exec("pgsql-get-login", args);
        if (result.size() != 1) {
            getLog().info("PGSQLResource:unable to get password from physical host. See README");
            throw new HSUserException("pgsqlresource.paswnotfound");
        } else {
            this.postgresPassword = (String) result.get(0);
        }
    }

    public String getRootLogin() throws Exception {
        Long timeToLive = (Long) this.rlogin.get("time");
        Object o = this.rlogin.get("login");
        if (timeToLive != null && o != null && TimeUtils.currentTimeMillis() - timeToLive.longValue() < 300000) {
            return (String) o;
        }
        RootLogin();
        this.rlogin.put("login", this.postgresPassword);
        this.rlogin.put("time", new Long(TimeUtils.currentTimeMillis()));
        return this.postgresPassword;
    }

    protected String getPHPPgAdminHost() throws Exception {
        String resultHostName;
        HostEntry he = HostManager.getHost(this.hostId);
        String webId = he.getOption("phppgadmin");
        if (webId != null && !"".equals(webId)) {
            HostEntry heWeb = HostManager.getHost(webId);
            resultHostName = heWeb.getName();
        } else {
            resultHostName = he.getName();
        }
        return resultHostName;
    }
}
