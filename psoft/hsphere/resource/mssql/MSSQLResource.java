package psoft.hsphere.resource.mssql;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import org.apache.axis.AxisFault;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mssql/MSSQLResource.class */
public class MSSQLResource extends Resource implements HostDependentResource {
    protected long hostId;

    public MSSQLResource(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mssql_host_id  FROM mssqlres WHERE id = ?");
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

    public MSSQLResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        WinHostEntry he = (WinHostEntry) i.next();
        this.hostId = he.getId();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry winHostEntry = (WinHostEntry) HostManager.getHost(targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        WinHostEntry winHostEntry = (WinHostEntry) HostManager.getHost(targetHostId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mssqlres set mssql_host_id = ? WHERE id = >");
            ps.setLong(2, getId().getId());
            ps.setLong(1, newHostId);
            ps.executeUpdate();
            this.hostId = newHostId;
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

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO mssqlres(id, mssql_host_id) VALUES (?, ?)");
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
            ps = con.prepareStatement("DELETE FROM mssqlres WHERE id = ?");
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
            return super.get(key);
        } catch (Exception e) {
            Session.getLog().warn("no host entry for MSSQL resource", e);
            return null;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v7, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_isLoginExist(String login) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("Checking existance of MSSQL login: " + prefix + login);
        WinHostEntry whe = (WinHostEntry) HostManager.getHost(this.hostId);
        if (WinHostEntry.getEmulationMode()) {
            return null;
        }
        int tmp = 0;
        if (WinService.isSOAPSupport()) {
            try {
                whe.invokeMethod("get", new String[]{new String[]{"resourcename", "mssqllogin"}, new String[]{"login", login}});
            } catch (AxisFault fault) {
                if (WinService.getFaultDetailValue(fault, "subcode").equals("1404")) {
                    tmp = 0;
                } else {
                    throw AxisFault.makeFault(fault);
                }
            }
        } else {
            Collection res = whe.exec("mssql-checklogin.asp", (String[][]) new String[]{new String[]{"login", prefix + login}});
            Object[] resArr = res.toArray();
            tmp = Integer.parseInt(((String) resArr[0]).toString().trim());
        }
        if (tmp == 1) {
            return this;
        }
        return null;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v7, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_isDatabaseExist(String database) throws Exception {
        String prefix = Session.getUser().getUserPrefix();
        Session.getLog().debug("Checking existance of MSSQL database: " + prefix + database);
        WinHostEntry whe = (WinHostEntry) HostManager.getHost(this.hostId);
        int tmp = 0;
        if (WinService.isSOAPSupport()) {
            try {
                whe.invokeMethod("get", new String[]{new String[]{"resourcename", "mssqldatabase"}, new String[]{"database", database}});
            } catch (AxisFault fault) {
                if (WinService.getFaultDetailValue(fault, "subcode").equals("1404")) {
                    tmp = 0;
                } else {
                    throw AxisFault.makeFault(fault);
                }
            }
        } else {
            Collection res = whe.exec("mssql-checkdatabase.asp", (String[][]) new String[]{new String[]{"database", prefix + database}});
            Object[] resArr = res.toArray();
            tmp = Integer.parseInt(((String) resArr[0]).toString().trim());
        }
        if (tmp == 1) {
            return this;
        }
        return null;
    }
}
