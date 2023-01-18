package psoft.hsphere.resource.vps;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.ThirdPartyResource;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.VPSDependentResource;
import psoft.hsphere.resource.VPSDependentResourceIterator;
import psoft.hsphere.resource.VPSIPResource;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/vps/VPSResource.class */
public class VPSResource extends Resource implements HostDependentResource, ThirdPartyResource, VPSDependentResource {
    protected String serverName;
    protected String rootPassword;
    protected long hostId;
    protected long securityContext;
    public static final long TIME_TO_LIVE = 180000;
    protected boolean physicallyInitialized;
    private Map report;
    StringBuffer cronLogger;

    public VPSResource(int type, Collection init) throws Exception {
        super(type, init);
        this.securityContext = -1L;
        this.physicallyInitialized = false;
        this.report = new HashMap();
        this.cronLogger = new StringBuffer("");
        Iterator i = init.iterator();
        HostEntry he = (HostEntry) i.next();
        this.hostId = he.getId();
        this.serverName = (String) i.next();
        this.rootPassword = (String) i.next();
    }

    public VPSResource(ResourceId rid) throws Exception {
        super(rid);
        this.securityContext = -1L;
        this.physicallyInitialized = false;
        this.report = new HashMap();
        this.cronLogger = new StringBuffer("");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name,password,hostid,context,initialized FROM vps, vps_resources WHERE vps.id = vps_resources.vps_id AND vps_resources.rid = ? AND vps.id = ?");
            ps.setLong(1, getId().getId());
            ps.setLong(2, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.serverName = rs.getString(1);
                this.rootPassword = rs.getString(2);
                this.hostId = rs.getLong(3);
                this.securityContext = rs.getLong(4);
                this.physicallyInitialized = rs.getTimestamp(5) != null;
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public void checkAutoRenewal() throws Exception {
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        try {
            try {
                PreparedStatement ps = con.prepareStatement("SELECT id FROM vps WHERE name = ?");
                ps.setString(1, this.serverName);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    throw new SQLException("VPS Hostname exists: " + this.serverName);
                }
                ps.close();
                PreparedStatement ps2 = con.prepareStatement("INSERT INTO vps (id, name, password, hostid) VALUES (?, ?, ?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.serverName);
                ps2.setString(3, this.rootPassword);
                ps2.setLong(4, this.hostId);
                ps2.executeUpdate();
                Session.closeStatement(ps2);
                con.close();
                setPsInitialized(false);
            } catch (SQLException e) {
                Session.getLog().debug("Error inserting VPS Hostname", e);
                throw new HSUserException("vps.hostname.exists", new Object[]{this.serverName.toString()});
            }
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public String getVPSHostName() {
        return this.serverName;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (!key.equals("vpsHostName") && !key.equals("name")) {
            if (key.equals("rootPassword")) {
                return new TemplateString(this.rootPassword);
            }
            if (!key.equals("hostid") && !key.equals("host_id")) {
                if (key.equals("host")) {
                    try {
                        return HostManager.getHost(this.hostId);
                    } catch (Exception e) {
                        getLog().warn("unable to get host entry for VPS with id=" + getId(), e);
                        return null;
                    }
                }
                if ("dns_zone".equals(key)) {
                    try {
                        ResourceId rid = FM_findChild("dns_zone");
                        if (rid != null) {
                            return rid;
                        }
                    } catch (Exception e2) {
                        Session.getLog().error("Error getting domain child ", e2);
                    }
                }
                if ("ip".equals(key)) {
                    try {
                        ResourceId rid2 = FM_getChild("vps_ip");
                        if (rid2 != null) {
                            return rid2;
                        }
                    } catch (Exception e3) {
                        Session.getLog().error("Error getting domain child ip", e3);
                    }
                }
                if (key.equals("securityContext")) {
                    return new TemplateString(this.securityContext);
                }
                if (key.equals("server_status_label")) {
                    return getServerStatusLabel();
                }
                if (key.equals("server_status")) {
                    return new TemplateString(((Integer) getServerStatusReport(1)).intValue());
                }
                return key.equals("vps") ? this : super.get(key);
            }
            return new TemplateString(this.hostId);
        }
        return new TemplateString(this.serverName);
    }

    public TemplateModel getServerStatusLabel() {
        int status = ((Integer) getServerStatusReport(1)).intValue();
        switch (status) {
            case 0:
                return new TemplateString(Localizer.translateMessage("vps.status.initializing"));
            case 1:
                return new TemplateString(Localizer.translateMessage("vps.status.running"));
            case 2:
                return new TemplateString(Localizer.translateMessage("vps.status.stopped"));
            default:
                return new TemplateString(Localizer.translateMessage("vps.status.unable_to_get"));
        }
    }

    public Object getServerStatusReport(int paramIndex) {
        Session.getLog().debug("Inside getServerStatusReport");
        Long timeToLive = (Long) this.report.get("time");
        Object o = this.report.get("report");
        if (timeToLive != null && o != null && TimeUtils.currentTimeMillis() - timeToLive.longValue() < TIME_TO_LIVE) {
            Session.getLog().debug("getServerStatusReport: Information found ");
            return ((Object[]) o)[paramIndex];
        }
        Session.getLog().debug("getServerStatusReport: Info is outdated of not found");
        Object[] rep = new Object[2];
        try {
            rep[0] = new Boolean(isPsInitialized());
        } catch (Exception ex) {
            Session.getLog().debug("Ubable to get isPsInitialized ", ex);
            rep[0] = new Boolean(false);
        }
        if (((Boolean) rep[0]).booleanValue()) {
            try {
                HostEntry he = HostManager.getHost(getHostId());
                ArrayList args = new ArrayList();
                args.add(this.serverName);
                Collection retv = he.exec("vps-status.pl", args);
                if (retv.size() == 1) {
                    Session.getLog().debug("getServerStatusReport: Got VPS server status");
                    Iterator i = retv.iterator();
                    rep[1] = new Integer((String) i.next());
                } else {
                    Session.getLog().debug("getServerStatusReport: Server returned irrelevant status response");
                    rep[1] = new Integer(-1);
                }
            } catch (Exception ex2) {
                Session.getLog().error("Unable to get " + this.serverName + " status", ex2);
                rep[1] = new Integer(-1);
            }
        } else {
            rep[1] = new Integer(0);
        }
        this.report.put("report", rep);
        this.report.put("time", new Long(TimeUtils.currentTimeMillis()));
        return rep[paramIndex];
    }

    public TemplateModel FM_isVPSInitialized() throws Exception {
        return new TemplateString(((Boolean) getServerStatusReport(0)).booleanValue());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        if (this.initialized) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            PreparedStatement ps1 = null;
            try {
                ps = con.prepareStatement("DELETE FROM vps WHERE id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                ps1 = con.prepareStatement("DELETE FROM vps_resources WHERE rid = ?");
                ps1.setLong(1, getId().getId());
                ps1.executeUpdate();
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                con.close();
                physicalDelete(this.hostId);
            } catch (Throwable th) {
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                con.close();
                throw th;
            }
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        ArrayList args = new ArrayList();
        args.add(this.serverName);
        Session.getLog().debug("VPS CREATION :" + this.serverName + " CONFIG=\n" + getConfig());
        setPsInitialized(false);
        String reply = "";
        Iterator output = he.exec("vps-post-config.pl", args, MailServices.shellQuote(getConfig())).iterator();
        if (output.hasNext()) {
            reply = output.next().toString();
        }
        if (!"ACCEPTED".equals(reply)) {
            Ticket.create("Error during VPS host installation", 1, Localizer.translateMessage("vps.creation_failure", new String[]{this.serverName, reply}), null, 0, 1, 0, 0, 0, 0);
            throw new HSUserException(Localizer.translateMessage("vps.creation_failure", new String[]{this.serverName, reply}));
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        HostEntry he = HostManager.getHost(targetHostId);
        ArrayList args = new ArrayList();
        args.add(this.serverName);
        he.exec("vps-delete.pl", args);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE vps SET hostid = ? WHERE id = ?");
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

    public void setSecurityContext(long newSecurityContext) throws SQLException {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE vps SET context = ? WHERE id = ?");
            ps.setLong(1, newSecurityContext);
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

    public void setRootPassword(String rootPassword) throws Exception {
        HostEntry he = HostManager.getHost(getHostId());
        ArrayList args = new ArrayList();
        args.add(this.serverName);
        he.exec("vps-rootpwd.pl", args, rootPassword);
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("UPDATE vps SET password = ? WHERE id = ?");
            ps.setString(1, rootPassword);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.rootPassword = rootPassword;
        } catch (Exception ex) {
            Session.getLog().error("Unable to set" + rootPassword, ex);
        }
    }

    public TemplateModel FM_setRootPassword(String rootPassword) throws Exception {
        setRootPassword(rootPassword);
        return this;
    }

    @Override // psoft.hsphere.ThirdPartyResource
    public void thirdPartyAction() throws Exception {
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public boolean isPsInitialized() throws Exception {
        if (this.physicallyInitialized) {
            return true;
        }
        try {
            VPSDependentResourceIterator vpsIt = new VPSDependentResourceIterator(Session.getAccount().getChildManager().getAllResources());
            Session.getLog().debug("Inside VPSResource:isPsInitialized. Size=" + vpsIt.getValues().size());
            while (vpsIt.hasNext()) {
                ResourceId rId = (ResourceId) vpsIt.next();
                if (rId.equals(getId())) {
                    if (vpsIt.getValues().size() == 1) {
                        return false;
                    }
                } else {
                    try {
                        VPSDependentResource r = (VPSDependentResource) rId.get();
                        Session.getLog().debug("GOT " + rId.toString() + "psInitialized = " + r.isPsInitialized());
                        if (!r.isPsInitialized()) {
                            return false;
                        }
                    } catch (Exception ex) {
                        Session.getLog().warn("Error getting resource " + rId.toString(), ex);
                        return false;
                    }
                }
            }
            setPsInitialized(true);
            return true;
        } catch (Exception ex2) {
            Session.getLog().warn("Error getting VPSDependent iterator: ", ex2);
            return false;
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void setPsInitialized(boolean init) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("UPDATE vps_resources SET initialized = ? WHERE rid = ?");
                Timestamp now = TimeUtils.getSQLTimestamp();
                if (init) {
                    ps.setTimestamp(1, now);
                } else {
                    ps.setNull(1, 93);
                }
                ps.setLong(2, getId().getId());
                if (ps.executeUpdate() == 0) {
                    ps = con.prepareStatement("INSERT INTO vps_resources(rid, vps_id, initialized) VALUES (?, ?, ?)");
                    ps.setLong(1, getId().getId());
                    ps.setLong(2, getId().getId());
                    if (init) {
                        ps.setTimestamp(3, now);
                    } else {
                        ps.setNull(3, 93);
                    }
                    ps.executeUpdate();
                }
                this.physicallyInitialized = init;
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Unable to set physicallyInitialized", ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void parseConfig(List config) throws Exception {
        boolean allResourcesPsInitialized = true;
        List notInitResources = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT rid FROM vps_resources WHERE vps_id = ? AND initialized IS NULL");
                ps.setLong(1, getId().getId());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    notInitResources.add(rs.getString(1));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                Session.getLog().error("Failed to get vps resources for parent VPS id " + getId().getId(), e);
                Session.closeStatement(ps);
                con.close();
            }
            VPSDependentResourceIterator vpsIt = new VPSDependentResourceIterator(Session.getAccount().getChildManager().getAllResources());
            while (vpsIt.hasNext()) {
                ResourceId rId = (ResourceId) vpsIt.next();
                if (!rId.equals(getId())) {
                    VPSDependentResource r = (VPSDependentResource) rId.get();
                    if (notInitResources.contains(rId.get("id").toString())) {
                        r.parseConfig(config);
                        allResourcesPsInitialized = allResourcesPsInitialized && r.isPsInitialized();
                        if (!r.getCronLogger().equals("")) {
                            this.cronLogger.append("\n" + r.getCronLogger());
                            r.dropCronLogger();
                        }
                    }
                }
            }
            if (!allResourcesPsInitialized) {
                return;
            }
            for (int i = 0; i < config.size(); i++) {
                String line = (String) config.get(i);
                if (line.startsWith("FILES=OK")) {
                    setPsInitialized(true);
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public String getConfig() throws Exception {
        StringBuffer ipCfg = new StringBuffer("IP=");
        StringBuffer config = new StringBuffer();
        List<ResourceId> ipIds = (List) getId().findChildren("vps_ip");
        for (ResourceId vpsIpId : ipIds) {
            VPSIPResource vpsIp = (VPSIPResource) vpsIpId.get();
            ipCfg.append(vpsIp.getIP().toString()).append(":").append(vpsIp.getIP().getMask()).append(",");
        }
        config.append('\n').append("ROOTPWD=").append(this.rootPassword).append('\n');
        config.append(ipCfg.toString()).append('\n');
        VPSDependentResourceIterator vpsIt = new VPSDependentResourceIterator(Session.getAccount().getChildManager().getAllResources());
        while (vpsIt.hasNext()) {
            ResourceId rId = (ResourceId) vpsIt.next();
            if (!rId.equals(getId())) {
                VPSDependentResource r = (VPSDependentResource) rId.get();
                config.append(r.getConfig()).append('\n');
            }
        }
        return config.toString();
    }

    public List getActualConfig() throws Exception {
        HostEntry he = HostManager.getHost(getHostId());
        ArrayList args = new ArrayList();
        args.add(this.serverName);
        return (List) he.exec("vps-get-config.pl", args);
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        HostEntry he = HostManager.getHost(getHostId());
        ArrayList args = new ArrayList();
        args.add(this.serverName);
        he.exec("vps-suspend.pl", args);
        super.suspend();
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            HostEntry he = HostManager.getHost(getHostId());
            ArrayList args = new ArrayList();
            args.add(this.serverName);
            he.exec("vps-resume.pl", args);
            super.resume();
        }
    }

    public TemplateModel FM_startServer() throws Exception {
        turnServer(true);
        this.report.remove("time");
        return this;
    }

    public TemplateModel FM_stopServer() throws Exception {
        turnServer(false);
        this.report.remove("time");
        return this;
    }

    protected void turnServer(boolean running) throws Exception {
        Collection retv;
        if (this.suspended) {
            throw new Exception("Unable to start/stop suspended " + this.serverName + " server");
        }
        HostEntry he = HostManager.getHost(getHostId());
        ArrayList args = new ArrayList();
        args.add(this.serverName);
        if (running) {
            retv = he.exec("vps-start.pl", args);
        } else {
            retv = he.exec("vps-stop.pl", args);
        }
        if (retv.size() != 1) {
            throw new Exception("Unexpected response from script during " + (running ? "starting " : "stopping ") + this.serverName);
        }
        Iterator i = retv.iterator();
        String res = (String) i.next();
        if (!"OK".equals(res)) {
            String translateMessage = Localizer.translateMessage("vps.turn_server.failure");
            String[] strArr = new String[1];
            strArr[0] = running ? Localizer.translateMessage("vps.turn_server.start.label") : Localizer.translateMessage("vps.turn_server.stop.label");
            throw new HSUserException(translateMessage, strArr);
        }
    }

    public void sendInitMail() {
        try {
            String eml = Session.getAccount().getContactInfo().getEmail();
            SimpleHash root = CustomEmailMessage.getDefaultRoot(Session.getAccount());
            root.put("vpsname", this.serverName);
            root.put("ips", new TemplateList(getId().findChildren("vps_ip")));
            root.put("ci", Session.getAccount().getContactInfo());
            CustomEmailMessage.send("VPS_INIT", eml, (TemplateModelRoot) root);
        } catch (Throwable ex) {
            Session.getLog().error("Error sending vps init mail", ex);
        }
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public String getCronLogger() {
        return this.cronLogger.toString();
    }

    @Override // psoft.hsphere.resource.VPSDependentResource
    public void dropCronLogger() {
        this.cronLogger = new StringBuffer("");
    }
}
