package psoft.hsphere.p001ds;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.cache.CacheableObject;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* renamed from: psoft.hsphere.ds.NetSwitch */
/* loaded from: hsphere.zip:psoft/hsphere/ds/NetSwitch.class */
public class NetSwitch implements CacheableObject, TemplateHashModel {
    public final String MASKED_COMMUNITY_NAME = "***";

    /* renamed from: id */
    long f90id;
    String device;
    String communityName;
    String description;
    String webURL;
    long mrtgHostId;
    long resellerId;

    public NetSwitch(long id, String device, String communityName, String description, String webURL, long hostId, long resellerId) {
        this.f90id = id;
        this.device = device;
        this.communityName = communityName;
        this.description = description;
        this.webURL = webURL;
        this.mrtgHostId = hostId;
        this.resellerId = resellerId;
    }

    public void updateData(String device, String communityName, String description, String webURL, long hostId) {
        if (!modificationGranted()) {
            return;
        }
        this.device = device;
        this.communityName = communityName;
        this.description = description;
        this.webURL = webURL;
        this.mrtgHostId = hostId;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("device".equals(key)) {
            return new TemplateString(getDevice());
        }
        if ("com_name".equals(key)) {
            return new TemplateString(getCommunityNameToShow());
        }
        if ("description".equals(key)) {
            return new TemplateString(getDescription());
        }
        if ("web_url".equals(key)) {
            return new TemplateString(getWebURL());
        }
        if ("mrtg_host_id".equals(key)) {
            return new TemplateString(getMrtgHostId());
        }
        if ("reseller_id".equals(key)) {
            return new TemplateString(getResellerId());
        }
        if ("can_be_deleted".equals(key)) {
            return new TemplateString(canBeDeleted());
        }
        if ("used_ports".equals(key)) {
            return new TemplateString(getUsedPortNumber());
        }
        if ("status".equals(key)) {
            return Resource.STATUS_OK;
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean canBeDeleted() {
        try {
            return getUsedPortNumber() == 0;
        } catch (Exception ex) {
            Session.getLog().error("Impossible to determine whether the netswitch with id #" + getId() + " can be deleted. Assume: [No].", ex);
            return false;
        }
    }

    @Override // psoft.hsphere.cache.CacheableObject
    public long getId() {
        return this.f90id;
    }

    public String getDevice() {
        return this.device;
    }

    public void setDevice(String device) {
        if (!modificationGranted()) {
            return;
        }
        this.device = device;
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

    public String getWebURL() {
        return this.webURL;
    }

    public void setWebURL(String webURL) {
        if (!modificationGranted()) {
            return;
        }
        this.webURL = webURL;
    }

    public String getCommunityName() {
        return this.communityName;
    }

    public String getCommunityNameToShow() {
        try {
            if (Session.getResellerId() == this.resellerId) {
                return this.communityName;
            }
            return "***";
        } catch (UnknownResellerException e) {
            return "***";
        }
    }

    protected void setCommunityName(String communityName) {
        if (!modificationGranted()) {
            return;
        }
        this.communityName = communityName;
    }

    public long getMrtgHostId() {
        return this.mrtgHostId;
    }

    public void setMrtgHostId(long mrtgHostId) {
        if (!modificationGranted()) {
            return;
        }
        this.mrtgHostId = mrtgHostId;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public synchronized List getUsedPorts() throws Exception {
        List usedPorts = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT DISTINCT switch_port FROM ds_netinterfaces WHERE switch_id = ? AND deleted is null");
        try {
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                usedPorts.add(new Long(rs.getLong(1)));
            }
            return usedPorts;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public synchronized int getUsedPortNumber() {
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT COUNT(DISTINCT switch_port) FROM ds_netinterfaces WHERE switch_id = ? AND deleted is null");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int i = rs.getInt(1);
                Session.closeStatement(ps);
                con.close();
                return i;
            }
            Session.closeStatement(ps);
            con.close();
            return 0;
        } catch (Exception ex) {
            Session.getLog().debug("Error in method 'NetSwitch::getUsedPortNumber()' ", ex);
            return -1;
        }
    }

    public synchronized boolean isPortInUse(int port) throws Exception {
        boolean z;
        Connection con = Session.getDb();
        PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) FROM ds_netinterfaces WHERE switch_id = ? AND switch_port = ? AND deleted is null");
        try {
            ps.setLong(1, getId());
            ps.setLong(2, port);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                if (rs.getLong(1) > 0) {
                    z = true;
                    return z;
                }
            }
            z = false;
            return z;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_getUsedPorts() throws Exception {
        return new TemplateList(getUsedPorts());
    }

    private boolean modificationGranted() {
        try {
            if (Session.getResellerId() == this.resellerId) {
                return true;
            }
            Session.getLog().debug("Detected attempt to modify the dedicated server network interface #" + this.f90id + ", created by reseller #" + this.resellerId + ", out of another reseller #" + Session.getResellerId() + ".");
            return false;
        } catch (UnknownResellerException e) {
            Session.getLog().debug("Unknown reseller. Modification of dedicated server network interface #" + this.f90id + " is disabled.");
            return false;
        }
    }
}
