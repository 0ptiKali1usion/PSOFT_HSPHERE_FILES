package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.Config;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/ServletResource.class */
public class ServletResource extends Resource {
    private static int portMin = Integer.parseInt(Config.getProperty("CLIENT", "MIN_JSERV_PORT"));
    private static int portMax = Integer.parseInt(Config.getProperty("CLIENT", "MAX_JSERV_PORT"));
    private long physicalServID;
    private int port;
    private String mount;

    public ServletResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.port = portMin;
        this.physicalServID = 0L;
        this.mount = "servlets";
    }

    public ServletResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, physicalServID, port, mount, portIsBusy FROM jserv_ports WHERE ((id=?) AND (portIsBusy=TRUE)) ORDER BY id;");
            ps.setLong(1, rId.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.port = rs.getInt("port");
                this.physicalServID = rs.getLong("physicalServID");
                this.mount = rs.getString("mount");
            }
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void process() throws Exception {
        super.process();
        HostEntry he = HostManager.getHost(recursiveGet("host_id"));
        this.physicalServID = he.getPServer().getId();
        this.port = getNewPort();
    }

    private int getNewPort() throws Exception {
        PreparedStatement ps;
        int newport;
        int i = portMin;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT port, physicalServID, portIsBusy FROM jserv_ports WHERE ((physicalServID=?) AND (portIsBusy=FALSE)) ORDER BY port;");
            ps2.setLong(1, this.physicalServID);
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                int newport2 = rs.getInt("port");
                ps2.close();
                PreparedStatement ps3 = con.prepareStatement("UPDATE jserv_ports SET id=?, mount=?, portIsBusy = TRUE WHERE ((physicalServID=?) AND (port=?));");
                ps3.setLong(1, getId().getId());
                ps3.setString(2, this.mount);
                ps3.setLong(3, this.physicalServID);
                ps3.setInt(4, newport2);
                ps3.executeUpdate();
                Session.closeStatement(ps3);
                if (con != null) {
                    con.close();
                }
                return newport2;
            }
            ps2.close();
            PreparedStatement ps4 = con.prepareStatement("SELECT physicalServID, port FROM jserv_ports WHERE ((physicalServID=?)) ORDER BY port DESC;");
            ps4.setLong(1, this.physicalServID);
            ResultSet rs2 = ps4.executeQuery();
            if (rs2.next()) {
                newport = rs2.getInt("port") + 1;
                if (newport <= portMax) {
                    ps4.close();
                    ps = con.prepareStatement("INSERT INTO jserv_ports (id, port, physicalServID, mount, portIsBusy ) VALUES (?,?,?,?,TRUE);");
                    ps.setLong(1, getId().getId());
                    ps.setInt(2, newport);
                    ps.setLong(3, this.physicalServID);
                    ps.setString(4, this.mount);
                    ps.executeUpdate();
                } else {
                    con.close();
                    throw new Exception("ADD_NEW_PORT: All possible ports are used");
                }
            } else {
                ps4.close();
                ps = con.prepareStatement("INSERT INTO jserv_ports (id, port, physicalServID, mount, portIsBusy ) VALUES (?,?,?,?,TRUE);");
                ps.setLong(1, getId().getId());
                ps.setInt(2, portMin);
                ps.setLong(3, this.physicalServID);
                ps.setString(4, this.mount);
                ps.executeUpdate();
                newport = portMin;
            }
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
            return newport;
        } catch (Throwable th) {
            Session.closeStatement(null);
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("config")) {
                return new TemplateString(getServerConfig());
            }
            if (key.equals("mount")) {
                return new TemplateString(this.mount);
            }
            return super.get(key);
        } catch (Exception e) {
            getLog().warn("geting config", e);
            return null;
        }
    }

    public String getServerConfig() throws Exception {
        try {
            String domainName = recursiveGet("real_name").toString();
            String portStr = String.valueOf(this.port);
            return "ApJServMount /" + this.mount + "  ajpv12:" + domainName + ":" + portStr + "/servlets\n";
        } catch (Exception ex) {
            throw new Exception("GetServerConfig:" + ex);
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        reConfig();
    }

    private void reConfig() throws Exception {
        Session.getLog().debug("*** call jserv reConfig method");
        List l = new ArrayList();
        String login = recursiveGet("login").toString();
        String domainName = recursiveGet("real_name").toString();
        String portStr = String.valueOf(this.port);
        String group = recursiveGet("group").toString();
        l.add(login);
        l.add(domainName);
        l.add(portStr);
        l.add("servlets");
        l.add(group);
        HostEntry he = HostManager.getHost(recursiveGet("host_id"));
        he.exec("jserv-conf", l);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            List l = new ArrayList();
            String login = recursiveGet("login").toString();
            l.add(login);
            l.add("stop");
            HostEntry he = HostManager.getHost(recursiveGet("host_id"));
            he.exec("jserv-restart", l);
            l.clear();
            l.add(login);
            he.exec("jserv-delconf", l);
            ps = con.prepareStatement("UPDATE jserv_ports SET portIsBusy = FALSE WHERE id=?;");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            if (con != null) {
                con.close();
            }
            throw th;
        }
    }

    public TemplateModel FM_setMount(String newmount) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            this.mount = newmount;
            ps = con.prepareStatement("UPDATE jserv_ports SET mount = ? WHERE id = ?");
            ps.setString(1, this.mount);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_reStart() throws Exception {
        try {
            List l = new ArrayList();
            String login = recursiveGet("login").toString();
            l.add(login);
            l.add("restart");
            HostEntry he = HostManager.getHost(recursiveGet("host_id"));
            he.exec("jserv-restart", l);
            return this;
        } catch (Exception e) {
            return null;
        }
    }
}
