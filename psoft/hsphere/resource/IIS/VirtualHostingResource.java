package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.VHostSettings;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/VirtualHostingResource.class */
public class VirtualHostingResource extends Resource implements IPDependentResource, HostDependentResource, VHostSettings {
    protected int index;
    protected int iis_status;
    protected int hostnum;
    protected String dir;
    protected Hashtable hostnums;
    protected String[] indexValues;
    protected String[] iisStatusValues;
    protected String INDEX;
    protected String IIS_STATUS;

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        boolean isSOAP = WinService.isSOAPSupport();
        String login = recursiveGet("login").toString();
        String name = recursiveGet("real_name").toString();
        String ip = getIp().toString();
        String systemUserId = Long.toString(getParent().getId());
        String idomain = "";
        Resource domain = getParent().get();
        if (this.dir == null) {
            this.dir = ((Domain) domain).getName();
        }
        ResourceId ipId = domain.FM_getChild("ip");
        if (ipId.get("shared").toString().equals("1")) {
            try {
                idomain = FM_getChild("idomain_alias").get("alias").toString();
            } catch (NullPointerException e) {
            }
        } else {
            idomain = ipId.get("ip").toString();
        }
        if (isSOAP) {
            try {
                ?? r2 = new String[9];
                String[] strArr = new String[2];
                strArr[0] = "resourcename";
                strArr[1] = "hosting";
                r2[0] = strArr;
                String[] strArr2 = new String[2];
                strArr2[0] = "username";
                strArr2[1] = login;
                r2[1] = strArr2;
                String[] strArr3 = new String[2];
                strArr3[0] = "hostname";
                strArr3[1] = name;
                r2[2] = strArr3;
                String[] strArr4 = new String[2];
                strArr4[0] = "ip";
                strArr4[1] = ip;
                r2[3] = strArr4;
                String[] strArr5 = new String[2];
                strArr5[0] = "index";
                strArr5[1] = this.index == 0 ? "false" : "true";
                r2[4] = strArr5;
                String[] strArr6 = new String[2];
                strArr6[0] = "dedicatedip";
                strArr6[1] = getIp().get("shared").toString().equals("0") ? "true" : "false";
                r2[5] = strArr6;
                String[] strArr7 = new String[2];
                strArr7[0] = "idomain";
                strArr7[1] = idomain;
                r2[6] = strArr7;
                String[] strArr8 = new String[2];
                strArr8[0] = "poolname";
                strArr8[1] = "SharedAppPool";
                r2[7] = strArr8;
                String[] strArr9 = new String[2];
                strArr9[0] = "appcount";
                strArr9[1] = "50";
                r2[8] = strArr9;
                he.invokeMethod("create", r2);
                he.invokeMethod("create", new String[]{new String[]{"resourcename", "webcontent"}, new String[]{"hostname", name}, new String[]{"lid", systemUserId}, new String[]{"cpurl", Session.getUser().get("CP_URL").toString()}, new String[]{"ssurl", Session.getProperty("PATH_SITE_STUDIO").toString()}, new String[]{"ssclass", Session.getProperty("SITE_STUDIO_CLASS").toString()}});
            } catch (Exception ex) {
                physicalDelete(targetHostId);
                throw ex;
            }
        } else {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                getLog().debug("Inside IIS.VirtualHostingResource::physicalCreate()");
                Collection c = he.exec("web-create.asp", (String[][]) new String[]{new String[]{"user-name", login}, new String[]{"hostname", name}, new String[]{"dir", this.dir}, new String[]{"ip", ip}, new String[]{"index", Integer.toString(this.index)}, new String[]{"status", Integer.toString(this.iis_status)}, new String[]{"shared", getIp().get("shared").toString()}, new String[]{"idomain", idomain}, new String[]{"cp_url", Session.getUser().get("CP_URL").toString()}, new String[]{"ss_path", Session.getProperty("PATH_SITE_STUDIO").toString()}, new String[]{"ss_class", Session.getProperty("SITE_STUDIO_CLASS").toString()}, new String[]{"lid", systemUserId}});
                this.hostnum = Integer.parseInt((String) c.iterator().next());
                ps = con.prepareStatement("UPDATE iis_vhost SET hostnum = ? WHERE id = ?");
                ps.setInt(1, this.hostnum);
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
        Collection<ResourceId> domAliases = domain.getChildHolder().getChildrenByName("domain_alias");
        StringBuffer warnMessages = new StringBuffer("");
        for (ResourceId daResId : domAliases) {
            try {
                DomainAliasResource da = (DomainAliasResource) daResId.get();
                da.physicalCreate(targetHostId);
            } catch (Exception e2) {
                String warnMessage = "Domain alias " + daResId.toString() + " failed to create physically";
                Session.getLog().warn(warnMessage);
                warnMessages.append(warnMessage).append("\n");
            }
        }
        if (!"".equals(warnMessages.toString())) {
            Session.addMessage(warnMessages.toString());
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        boolean isSOAP = WinService.isSOAPSupport();
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            hostnum = Integer.toString(getActualHostNum(targetHostId));
        } else {
            hostnum = Integer.toString(this.hostnum);
        }
        if (isSOAP) {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "hosting"}, new String[]{"hostname", name}});
        } else {
            he.exec("web-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}});
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        PreparedStatement ps = null;
        Session.getLog().debug("Inside IIS.VitrualHostingResource::setHostId");
        int hn = getActualHostNum(newHostId);
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE iis_vhost SET hostnum = ?, host_id = ? WHERE id = ?");
            ps.setInt(1, hn);
            ps.setLong(2, newHostId);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.getLog().debug("Setting new hostnum: old=" + this.hostnum + " new hostnum = " + hn);
            this.hostnums.put(new Long(newHostId), new Integer(hn));
            this.hostnum = hn;
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
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    public VirtualHostingResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.hostnums = new Hashtable();
        this.indexValues = new String[]{"disabled", "enabled"};
        this.iisStatusValues = new String[]{"stopped", "running"};
        this.INDEX = "index";
        this.IIS_STATUS = "iis_status";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.dir = (String) i.next();
            if (i.hasNext()) {
                this.index = Integer.parseInt((String) i.next());
                if (i.hasNext()) {
                    this.iis_status = Integer.parseInt((String) i.next());
                    return;
                }
                return;
            }
            initDirParam();
            return;
        }
        this.dir = null;
        initDirParam();
    }

    public VirtualHostingResource(ResourceId rId) throws Exception {
        super(rId);
        this.hostnums = new Hashtable();
        this.indexValues = new String[]{"disabled", "enabled"};
        this.iisStatusValues = new String[]{"stopped", "running"};
        this.INDEX = "index";
        this.IIS_STATUS = "iis_status";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT dir, indx, status, hostnum FROM iis_vhost WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.dir = rs.getString(1);
                this.index = rs.getInt(2);
                this.iis_status = rs.getInt(3);
                this.hostnum = rs.getInt(4);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_getIP() throws Exception {
        return getIp().get("ip");
    }

    public Resource getIp() throws Exception {
        Resource r = getParent().get();
        ResourceId ipId = r.FM_getChild("ip");
        return ipId.get();
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        getLog().debug("Win VirtualHostingResource.delete");
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM iis_vhost WHERE id = ?");
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

    public boolean hasDedicatedIp() throws Exception {
        return !"1".equals(getIp().get("shared").toString());
    }

    public String getPath() throws Exception {
        return recursiveGet("dir") + "\\" + this.dir;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("server")) {
            return new TemplateString("IIS");
        }
        if (key.equals("index")) {
            return new TemplateString(this.index);
        }
        if (key.equals("iis_status")) {
            return new TemplateString(this.iis_status);
        }
        if (key.equals("hostnum")) {
            return new TemplateString(this.hostnum);
        }
        if (key.equals("local_dir")) {
            return new TemplateString(this.dir);
        }
        if (key.equals("vhostingResource")) {
            return this;
        }
        try {
            return key.equals("ip") ? FM_getIP() : key.equals("path") ? new TemplateString(getPath()) : super.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    public void initDirParam() throws Exception {
        try {
            this.index = Integer.parseInt(getPlanValue("INDEX"));
        } catch (NullPointerException e) {
        }
        try {
            this.iis_status = Integer.parseInt(getPlanValue("STATUS"));
        } catch (NullPointerException e2) {
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Resource domain = getParent().get();
        if (this.dir == null) {
            this.dir = ((Domain) domain).getName();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO iis_vhost (id, host_id, dir, indx, status) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, getHostId());
            ps.setString(3, this.dir);
            ps.setInt(4, this.index);
            ps.setInt(5, this.iis_status);
            ps.executeUpdate();
            physicalCreate(getHostId());
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private String getSettingStringValue(String[] values, int index) {
        try {
            return values[index];
        } catch (IndexOutOfBoundsException e) {
            return String.valueOf(index);
        }
    }

    private boolean checkSettingIndValue(int index, String[] values) {
        return index >= 0 && index < values.length;
    }

    private int getSettingIndex(String name, String[] values) throws IndexOutOfBoundsException {
        if (name != null) {
            int index = values.length;
            do {
                index--;
                if (index < 0) {
                    try {
                        int index2 = Integer.getInteger(name).intValue();
                        if (checkSettingIndValue(index2, values)) {
                            return index2;
                        }
                    } catch (Exception e) {
                    }
                }
            } while (!name.equals(values[index]));
            return index;
        }
        throw new IndexOutOfBoundsException();
    }

    @Override // psoft.hsphere.resource.VHostSettings
    public HashMap getSettings() {
        HashMap result = new HashMap();
        result.put(this.INDEX, getSettingStringValue(this.indexValues, this.index));
        result.put(this.IIS_STATUS, getSettingStringValue(this.iisStatusValues, this.iis_status));
        return result;
    }

    @Override // psoft.hsphere.resource.VHostSettings
    public void setSettings(HashMap settings) throws Exception {
        int index = -1;
        int iis_status = -1;
        for (Object obj : settings.keySet()) {
            String key = obj.toString();
            String value = settings.get(key).toString();
            try {
                if (this.INDEX.equals(key)) {
                    index = getSettingIndex(value, this.indexValues);
                } else if (this.IIS_STATUS.equals(key)) {
                    iis_status = getSettingIndex(value, this.iisStatusValues);
                } else {
                    throw new HSUserException("An unknown option " + key + ".");
                }
            } catch (IndexOutOfBoundsException e) {
                throw new HSUserException("Unknown value [" + value + "] for the " + key + " option.");
            }
        }
        updateSettings(index, iis_status);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    public synchronized void updateSettings(int index, int iis_status) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        boolean optAdded = false;
        StringBuffer psStr = new StringBuffer("UPDATE iis_vhost SET ");
        if (checkSettingIndValue(index, this.indexValues)) {
            psStr.append("indx = ").append(String.valueOf(index));
            optAdded = true;
        } else {
            index = -1;
        }
        if (checkSettingIndValue(iis_status, this.iisStatusValues)) {
            if (optAdded) {
                psStr.append(", ");
            } else {
                optAdded = true;
            }
            psStr.append("status = ").append(String.valueOf(iis_status));
        } else {
            iis_status = -1;
        }
        if (optAdded) {
            getLog().debug("Win VirtualHostingResource, updating");
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            String name = recursiveGet("real_name").toString();
            ?? r2 = new String[6];
            String[] strArr = new String[2];
            strArr[0] = "hostnum";
            strArr[1] = Integer.toString(this.hostnum);
            r2[0] = strArr;
            String[] strArr2 = new String[2];
            strArr2[0] = "hostname";
            strArr2[1] = name;
            r2[1] = strArr2;
            String[] strArr3 = new String[2];
            strArr3[0] = "index";
            strArr3[1] = Integer.toString(index >= 0 ? index : this.index);
            r2[2] = strArr3;
            String[] strArr4 = new String[2];
            strArr4[0] = "status";
            strArr4[1] = Integer.toString(iis_status >= 0 ? iis_status : this.iis_status);
            r2[3] = strArr4;
            String[] strArr5 = new String[2];
            strArr5[0] = "ip";
            strArr5[1] = FM_getIP().toString();
            r2[4] = strArr5;
            String[] strArr6 = new String[2];
            strArr6[0] = "shared";
            strArr6[1] = getIp().get("shared").toString();
            r2[5] = strArr6;
            he.exec("web-update.asp", (String[][]) r2);
            try {
                psStr.append(" WHERE id = ?");
                ps = con.prepareStatement(psStr.toString());
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                if (index >= 0) {
                    this.index = index;
                }
                if (iis_status >= 0) {
                    this.iis_status = iis_status;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    public TemplateModel FM_updateSettings(int index, int iis_status) throws Exception {
        updateSettings(index, iis_status);
        return this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        String name = recursiveGet("real_name").toString();
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("suspend", new String[]{new String[]{"resourcename", "hosting"}, new String[]{"hostname", name}});
        } else {
            he.exec("web-suspend.asp", (String[][]) new String[]{new String[]{"hostnum", Integer.toString(this.hostnum)}, new String[]{"hostname", name}, new String[]{"user-name", recursiveGet("login").toString()}});
        }
        super.suspend();
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            String name = recursiveGet("real_name").toString();
            if (WinService.isSOAPSupport()) {
                he.invokeMethod("resume", new String[]{new String[]{"resourcename", "hosting"}, new String[]{"hostname", name}});
            } else {
                he.exec("web-resume.asp", (String[][]) new String[]{new String[]{"hostnum", Integer.toString(this.hostnum)}, new String[]{"hostname", name}, new String[]{"user-name", recursiveGet("login").toString()}});
            }
            super.resume();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        HostEntry he = HostManager.getHost(Long.parseLong(recursiveGet("host_id").toString()));
        Session.getLog().debug("IIS was restarted by IP");
        FM_updateSettings(this.index, this.iis_status);
        ((WinHostEntry) he).exec("net/changeIP-web.aspx", (String[][]) new String[]{new String[]{"hostname", recursiveGet("name").toString()}, new String[]{"newIP", getIp().toString()}});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("hosting.desc", new Object[]{recursiveGet("real_name").toString()});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    public int getActualHostNum(long hostId) throws Exception {
        if (this.hostnums.keySet().contains(new Long(hostId))) {
            return ((Integer) this.hostnums.get(new Long(hostId))).intValue();
        }
        WinHostEntry he = (WinHostEntry) HostManager.getHost(hostId);
        Collection c = he.exec("web-gethostnum.asp", (String[][]) new String[]{new String[]{"hostname", recursiveGet("real_name").toString()}});
        int hn = Integer.parseInt((String) c.iterator().next());
        this.hostnums.put(new Long(hostId), new Integer(hn));
        return hn;
    }
}
