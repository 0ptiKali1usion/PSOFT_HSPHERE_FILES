package psoft.hsphere.p001ds;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;
import psoft.hsphere.cache.CacheableObject;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.p003ds.BandwidthResource;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ConfigModel;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.ds.DSNetInterface */
/* loaded from: hsphere.zip:psoft/hsphere/ds/DSNetInterface.class */
public class DSNetInterface implements CacheableObject, TemplateHashModel {

    /* renamed from: id */
    long f86id;
    long dsId;
    long netswitchId;
    int netswitchPort;
    String mrtgTargetName;
    String description;
    Date created;
    long resellerId;
    static final String GRAPH_URL_PREFIX = "http://";
    static final String GRAPH_URL_PORT = "";
    static final String GRAPH_URL_PATH_D = "/rrd/d/";
    static final String GRAPH_URL_PATH_W = "/rrd/w/";
    static final String GRAPH_URL_PATH_M = "/rrd/m/";
    static final String GRAPH_URL_PATH_Y = "/rrd/y/";
    static final String GRAPH_URL_SUFFIX_D = "_d.png";
    static final String GRAPH_URL_SUFFIX_W = "_w.png";
    static final String GRAPH_URL_SUFFIX_M = "_m.png";
    static final String GRAPH_URL_SUFFIX_Y = "_y.png";
    BandwidthResource bandwidthRes = null;
    Date deleted = null;

    public DSNetInterface(long id, long dsId, long netswitchId, int netswitchPort, String targetName, String description, Date created, long resellerId) {
        this.f86id = id;
        this.dsId = dsId;
        this.netswitchId = netswitchId;
        this.netswitchPort = netswitchPort;
        this.mrtgTargetName = targetName;
        this.description = description;
        this.created = created;
        this.resellerId = resellerId;
    }

    public void updateData(String description) {
        if (!modificationGranted()) {
            return;
        }
        this.description = description;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("ds_id".equals(key)) {
            return new TemplateString(getDsId());
        }
        if ("netswitch_id".equals(key)) {
            return new TemplateString(getNetswitchId());
        }
        if ("netswitch_port".equals(key)) {
            return new TemplateString(getNetswitchPort());
        }
        if ("target".equals(key)) {
            return new TemplateString(getMrtgTargetName());
        }
        if ("description".equals(key)) {
            return new TemplateString(getDescription());
        }
        if ("created".equals(key)) {
            return new TemplateString(this.created);
        }
        if ("deleted".equals(key)) {
            if (this.deleted != null) {
                return new TemplateString(this.deleted);
            }
            return null;
        } else if ("reseller_id".equals(key)) {
            return new TemplateString(getResellerId());
        } else {
            if ("status".equals(key)) {
                return Resource.STATUS_OK;
            }
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.f86id;
    }

    public long getDsId() {
        return this.dsId;
    }

    public void setDsId(long dsId) {
        if (!modificationGranted()) {
            return;
        }
        this.dsId = dsId;
    }

    public long getNetswitchId() {
        return this.netswitchId;
    }

    public void setNetswitchId(long netswitchId) {
        if (!modificationGranted()) {
            return;
        }
        this.netswitchId = netswitchId;
    }

    public int getNetswitchPort() {
        return this.netswitchPort;
    }

    public void setNetswitchPort(int netswitchPort) {
        if (!modificationGranted()) {
            return;
        }
        this.netswitchPort = netswitchPort;
    }

    public String getMrtgTargetName() {
        return this.mrtgTargetName;
    }

    public void setMrtgTargetName(String mrtgTargetName) {
        if (!modificationGranted()) {
            return;
        }
        this.mrtgTargetName = mrtgTargetName;
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        if (!modificationGranted()) {
            return;
        }
        this.description = description;
    }

    public Date getCreated() {
        return this.created;
    }

    public void setCreated(Date created) {
        if (!modificationGranted()) {
            return;
        }
        this.created = created;
    }

    public Date getDeleted() {
        return this.deleted;
    }

    public void setDeleted(Date deleted) {
        if (!modificationGranted()) {
            return;
        }
        this.deleted = deleted;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public TemplateModel FM_getGraphURL(String period) throws Exception {
        NetSwitch ns = ((NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class)).getNetSwitch(getNetswitchId());
        if (ns == null) {
            throw new Exception(Localizer.translateMessage("ds.netinterface.incor_specified_ns_mes", new String[]{String.valueOf(getId()), String.valueOf(getNetswitchId())}));
        }
        HostEntry he = HostManager.getHost(ns.getMrtgHostId());
        if (he == null) {
            throw new Exception(Localizer.translateMessage("ds.netinterface.incor_specified_mrtg_host_mes", new String[]{String.valueOf(getId()), String.valueOf(ns.getMrtgHostId())}));
        }
        if ("month".equals(period)) {
            return new TemplateString(GRAPH_URL_PREFIX + he.getName() + GRAPH_URL_PORT + GRAPH_URL_PATH_M + getMrtgTargetName() + GRAPH_URL_SUFFIX_M);
        }
        if ("year".equals(period)) {
            return new TemplateString(GRAPH_URL_PREFIX + he.getName() + GRAPH_URL_PORT + GRAPH_URL_PATH_Y + getMrtgTargetName() + GRAPH_URL_SUFFIX_Y);
        }
        if ("day".equals(period)) {
            return new TemplateString(GRAPH_URL_PREFIX + he.getName() + GRAPH_URL_PORT + GRAPH_URL_PATH_D + getMrtgTargetName() + GRAPH_URL_SUFFIX_D);
        }
        if ("week".equals(period)) {
            return new TemplateString(GRAPH_URL_PREFIX + he.getName() + GRAPH_URL_PORT + GRAPH_URL_PATH_W + getMrtgTargetName() + GRAPH_URL_SUFFIX_W);
        }
        return null;
    }

    public synchronized void postConfig() throws Exception {
        NetSwitch ns = getNetSwitch();
        HostEntry he = HostManager.getHost(ns.getMrtgHostId());
        String cfgEntry = getConfigEntry(ns);
        List l = new ArrayList();
        l.add(getMrtgTargetName() + ".conf");
        he.exec("mrtg-rrd-saveconf", l, cfgEntry);
        l.clear();
        he.exec("mrtg-rrd-indexconf", l);
    }

    public synchronized void delConfig() throws Exception {
        NetSwitch ns = getNetSwitch();
        HostEntry he = HostManager.getHost(ns.getMrtgHostId());
        List l = new ArrayList();
        l.add(getMrtgTargetName() + ".conf");
        he.exec("mrtg-rrd-delconf", l);
        l.clear();
        he.exec("mrtg-rrd-indexconf", l);
    }

    public void setStartDate() throws Exception {
        long bandwidthStartDateInSeconds = getBandwidthStartDateInSeconds();
        if (bandwidthStartDateInSeconds != -1) {
            HostEntry he = HostManager.getHost(getMrtgHostId());
            List l = new ArrayList();
            l.add(getMrtgTargetName());
            l.add(String.valueOf(bandwidthStartDateInSeconds));
            he.exec("mrtg-rrd-startdate-set", l);
        }
    }

    public void resetStartDate() throws Exception {
        long bandwidthStartDateInSeconds = getBandwidthStartDateInSeconds();
        if (bandwidthStartDateInSeconds != -1) {
            HostEntry he = HostManager.getHost(getMrtgHostId());
            List l = new ArrayList();
            l.add(getMrtgTargetName());
            he.exec("mrtg-rrd-startdate-reset", l);
        }
    }

    private String getConfigEntry(NetSwitch netSwitch) throws Exception {
        SimpleHash root = new SimpleHash();
        root.put("target", getMrtgTargetName());
        root.put("description", getDescription());
        root.put("port", String.valueOf(getNetswitchPort()));
        root.put("com_name", netSwitch.getCommunityName());
        root.put("device", netSwitch.getDevice());
        root.put("toolbox", HsphereToolbox.toolbox);
        root.put("config", new ConfigModel("CLIENT"));
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Session.getTemplate("ds/mrtg_target.config").process(root, out);
        out.close();
        return sw.toString();
    }

    private NetSwitch getNetSwitch() throws Exception {
        NetSwitch netswitch = ((NetSwitchManager) Session.getCacheFactory().getCache(NetSwitch.class)).getNetSwitch(getNetswitchId());
        if (netswitch == null) {
            throw new Exception("ds.netinterface.incor_specified_ns");
        }
        return netswitch;
    }

    public long getMrtgHostId() throws Exception {
        return getNetSwitch().getMrtgHostId();
    }

    protected long getBandwidthStartDateInSeconds() throws Exception {
        if (this.dsId != 0) {
            DedicatedServer ds = DSHolder.getAcessibleDedicatedServer(this.dsId);
            NetSwitch ns = getNetSwitch();
            if (ds == null || ns == null) {
                return -1L;
            }
            long rid = ds.getRid();
            if (rid != 0) {
                Session.save();
                try {
                    Account a = new ResourceId(rid, TypeRegistry.getIntTypeId("ds")).getAccount();
                    User u = a.getUser();
                    Session.setUser(u);
                    Session.setAccount(a);
                    ResourceId dsBandwidthResId = a.FM_findChild("ds_bandwidth");
                    if (dsBandwidthResId != null) {
                        long dateTimeInSeconds = TimeUtils.getDateTimeInSeconds(((BandwidthResource) dsBandwidthResId.get()).getStartDate());
                        Session.restore();
                        return dateTimeInSeconds;
                    }
                    return -1L;
                } catch (Exception ex) {
                    Session.getLog().error("Unable to get the 'ds_bandwidth' resource.", ex);
                    return -1L;
                } finally {
                    Session.restore();
                }
            }
            return -1L;
        }
        return -1L;
    }

    private boolean modificationGranted() {
        try {
            if (Session.getResellerId() == this.resellerId) {
                return true;
            }
            Session.getLog().debug("Detected attempt to modify the dedicated server network interface #" + this.f86id + ", created by reseller #" + this.resellerId + ", out of another reseller #" + Session.getResellerId() + ".");
            return false;
        } catch (UnknownResellerException e) {
            Session.getLog().debug("Unknown reseller. Modification of dedicated server network interface #" + this.f86id + " is disabled.");
            return false;
        }
    }
}
