package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.MissingResourceException;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.global.Globals;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.allocation.AllocatedPServer;
import psoft.hsphere.resource.allocation.AllocatedServerManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/ResellerEnterpriseManager.class */
public class ResellerEnterpriseManager extends Resource {
    protected static final String ttl = "86400";
    private List lserverFree;
    private List lserverFreeDNS;
    private List lserverAliases;
    private List lserverAliasesDNS;

    public ResellerEnterpriseManager(int type, Collection init) throws Exception {
        super(type, init);
        this.lserverFree = new ArrayList();
        this.lserverFreeDNS = new ArrayList();
        this.lserverAliases = new ArrayList();
        this.lserverAliasesDNS = new ArrayList();
        loadLServers();
    }

    public ResellerEnterpriseManager(ResourceId rid) throws Exception {
        super(rid);
        this.lserverFree = new ArrayList();
        this.lserverFreeDNS = new ArrayList();
        this.lserverAliases = new ArrayList();
        this.lserverAliasesDNS = new ArrayList();
        loadLServers();
    }

    public synchronized TemplateModel FM_addAlias(long lServerId, long zoneId, String prefix) throws Exception {
        for (AdmServiceDNSRecord rec : this.lserverAliases) {
            if (lServerId == rec.getLServerId()) {
                throw new HSUserException("eeman.aliases.lserverexists");
            }
        }
        for (AdmServiceDNSRecord rec2 : this.lserverAliasesDNS) {
            if (lServerId == rec2.getLServerId()) {
                throw new HSUserException("eeman.aliases.lserverexists");
            }
        }
        AdmDNSZone zone = AdmDNSZone.get(zoneId);
        HostEntry he = HostManager.getHost(lServerId);
        zone.addServiceRecord(he.getBaseName(), prefix, lServerId, false);
        loadLServers();
        return this;
    }

    public synchronized TemplateModel FM_delAlias(long zoneId, long recId) throws Exception {
        loadLServers();
        AdmServiceDNSRecord rec = AdmServiceDNSRecord.get(recId);
        if (!this.lserverAliases.contains(rec) && !this.lserverAliasesDNS.contains(rec)) {
            throw new HSUserException("eeman.aliases.aliasdontbelong");
        }
        AdmDNSZone zone = AdmDNSZone.get(zoneId);
        zone.delServiceRecord(recId);
        loadLServers();
        return this;
    }

    protected void loadLServers() throws Exception {
        Session.getLog().debug("In ResellerEnterpriseManager.loadLServers() ");
        this.lserverAliases.clear();
        this.lserverAliasesDNS.clear();
        this.lserverFree.clear();
        this.lserverFreeDNS.clear();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT e_dns_rec_id FROM l_server_aliases WHERE e_zone_id IN (SELECT id FROM e_zones WHERE reseller_id = ?)");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long recId = rs.getLong(1);
                AdmServiceDNSRecord rec = AdmServiceDNSRecord.get(recId);
                try {
                    if (HostManager.getHost(rec.getLServerId()).getGroupType() != 2) {
                        this.lserverAliases.add(rec);
                    } else {
                        this.lserverAliasesDNS.add(rec);
                    }
                } catch (Exception ex) {
                    Session.getLog().error("Can`t get LogicalSever id:", ex);
                }
            }
            ps.close();
            Reseller res = Session.getReseller();
            ps = con.prepareStatement("SELECT id FROM l_server WHERE id NOT IN (SELECT l_server_id FROM l_server_aliases WHERE e_zone_id IN (SELECT id FROM e_zones WHERE reseller_id = ?))");
            ps.setLong(1, res.getId());
            ResultSet rs2 = ps.executeQuery();
            AllocatedServerManager asm = AllocatedServerManager.getInstance();
            AllocatedPServer resellerServer = Session.getReseller().getAllocatedPServer();
            while (rs2.next()) {
                try {
                    HostEntry he = HostManager.getHost(rs2.getLong(1));
                    if (he.getGroupType() == 10 || Globals.isSetKeyDisabled("server_groups", String.valueOf(he.getGroup())) == 0) {
                        if (he.getGroupType() != 2) {
                            LogicalServer lserver = LogicalServer.get(rs2.getLong(1));
                            if (AllocatedServerManager.supportedServices.contains(new Integer(lserver.getGroup()))) {
                                if (asm.isAllocated(lserver.getPServerId())) {
                                    if (resellerServer != null && resellerServer.getId() == lserver.getPServerId()) {
                                        this.lserverFree.add(lserver);
                                    }
                                } else if (resellerServer == null) {
                                    this.lserverFree.add(lserver);
                                }
                            } else {
                                this.lserverFree.add(lserver);
                            }
                        } else {
                            String access = he.getOption("named_access");
                            if (access != null && !"".equals(access)) {
                                if (res.getId() != 1 || access.equals("main_only")) {
                                    if (Session.getResellerId() != 1 && !access.equals("reseller_only")) {
                                    }
                                }
                            }
                            this.lserverFreeDNS.add(LogicalServer.get(rs2.getLong(1)));
                        }
                    }
                } catch (Exception e) {
                }
            }
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
        if (key.equals("aliases")) {
            return new TemplateList(this.lserverAliases);
        }
        if (key.equals("aliasesDNS")) {
            return new TemplateList(this.lserverAliasesDNS);
        }
        if (key.equals("free_lserver")) {
            return new TemplateList(this.lserverFree);
        }
        if (key.equals("free_lserverDNS")) {
            return new TemplateList(this.lserverFreeDNS);
        }
        try {
            if (key.equals("zones")) {
                return new TemplateList(AdmDNSZone.getZones());
            }
            try {
                if (key.equals("reload")) {
                    loadLServers();
                    return null;
                } else if (key.equals("isSSLEnabled")) {
                    try {
                        return isSSLEnabled();
                    } catch (Exception e) {
                        return null;
                    }
                } else {
                    return super.get(key);
                }
            } catch (Exception ex) {
                Session.getLog().error("Error getting zones", ex);
                return null;
            }
        } catch (Exception ex2) {
            Session.getLog().error("Error getting zones", ex2);
            return null;
        }
    }

    public TemplateModel FM_reloadHosts() throws Exception {
        HostManager.loadHosts(true);
        return this;
    }

    public TemplateModel isSSLEnabled() throws Exception {
        String result = "0";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM reseller_ssl WHERE reseller_id = ?");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long tmp = rs.getLong(1);
                result = String.valueOf(tmp);
            }
            Session.closeStatement(ps);
            con.close();
            return new TemplateString(result);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_initResellerSSL(long lServerId, long zoneId, String prefix) throws Exception {
        AdmResellerSSL result = AdmResellerSSL.create();
        Session.getLog().debug("prefix in ResellerEnterpriseManager.initResellerSSL = " + prefix);
        addCPSSLARecord(result.getId(), lServerId, zoneId, prefix);
        return result;
    }

    public TemplateModel FM_initResellerSSL(long lServerId, long zoneId, String prefix, int sslType) throws Exception {
        try {
            Session.getProperty("RESELLER_SSL_PORT_RANGE");
            AdmResellerSSL result = AdmResellerSSL.create(sslType);
            return result;
        } catch (MissingResourceException e) {
            throw new HSUserException("Failed to create Reseller SSL. No free ports available.");
        }
    }

    public TemplateModel FM_checkServerTypeForResellerSSL() throws Exception {
        AdmResellerSSL res_ssl = AdmResellerSSL.create();
        String result = res_ssl.getServerType();
        return new TemplateString(result);
    }

    public TemplateModel FM_deleteResellerSSL(long id) throws Exception {
        AdmResellerSSL tmp = AdmResellerSSL.get(id);
        tmp.delete();
        tmp.deleteKeys();
        return this;
    }

    public TemplateModel FM_getResellerSSL(long id) throws Exception {
        return AdmResellerSSL.get(id);
    }

    public TemplateModel FM_changeURL(String URL, String protocol, String port) throws Exception {
        try {
            Reseller reseller = Reseller.getReseller(Session.getResellerId());
            reseller.setURL(URL, protocol, port);
        } catch (Exception e) {
            Session.getLog().debug("Some errors", e);
        }
        return this;
    }

    protected synchronized void addCPSSLARecord(long newId, long lServerId, long zoneId, String prefix) throws Exception {
        Session.getLog().debug("in ResellerEnterpriseManager.addCPSSLARecord()");
        Session.getLog().debug("lServerId = " + String.valueOf(lServerId));
        Session.getLog().debug("zoneId = " + String.valueOf(zoneId));
        Session.getLog().debug("prefix = " + String.valueOf(prefix));
        for (AdmServiceDNSRecord rec : this.lserverAliases) {
            if (lServerId == rec.getLServerId()) {
                throw new HSUserException("eeman.aliases.lserverexists");
            }
        }
        for (AdmServiceDNSRecord rec2 : this.lserverAliasesDNS) {
            if (lServerId == rec2.getLServerId()) {
                throw new HSUserException("eeman.aliases.lserverexists");
            }
        }
        AdmDNSZone zone = AdmDNSZone.get(zoneId);
        HostManager.getHost(lServerId);
        zone.addServiceRecord(prefix, lServerId, newId, true);
        loadLServers();
    }
}
