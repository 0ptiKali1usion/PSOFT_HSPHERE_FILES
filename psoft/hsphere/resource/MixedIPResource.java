package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.dns.DNSRecord;
import psoft.util.freemarker.StringShrink;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/MixedIPResource.class */
public class MixedIPResource extends IPResource implements HostDependentResource {
    protected int typeIp;
    protected String strIp;

    public String getStrIp() {
        return this.strIp;
    }

    public void setStrIp(String strIp) {
        this.strIp = strIp;
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return this.typeIp == 1 ? 1.0d : 0.0d;
    }

    @Override // psoft.hsphere.Resource
    public Collection getCurrentInitValues() {
        List values = new ArrayList();
        values.add(Integer.toString(this.typeIp));
        return values;
    }

    public static double getAmount(InitToken token) {
        int typeIp = 2;
        Iterator i = token.getValues().iterator();
        if (i.hasNext()) {
            typeIp = Integer.parseInt((String) i.next());
        }
        return typeIp == 1 ? 1.0d : 0.0d;
    }

    public MixedIPResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.typeIp = 2;
        this.strIp = "";
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.typeIp = Integer.parseInt((String) i.next());
        }
        if (i.hasNext()) {
            this.strIp = (String) i.next();
            Session.getLog().debug("Trying to get " + this.strIp + " IP");
        }
        Session.getLog().debug("Created typeIP " + getId() + "");
    }

    protected C0015IP getSharedIP() throws Exception {
        int tag = getSharedIPTag();
        return HostManager.getHost(recursiveGet("host_id")).getSharedIP(tag);
    }

    public int getSharedIPTag() {
        int tag = 2;
        try {
            tag = Integer.parseInt(getResourcePlanValue("SHARED_IP"));
        } catch (Throwable th) {
            Session.getLog().info("No shared_ip valued defined for " + this);
        }
        return tag;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        C0015IP tmpIp;
        super.initDone();
        if (isDedicated()) {
            try {
                HostEntry entry = HostManager.getHost(recursiveGet("host_id"));
                if (this.strIp != null && !"".equals(this.strIp)) {
                    tmpIp = entry.getIPbyIP(this.strIp, getId());
                } else {
                    tmpIp = entry.getExclusiveIP(getId());
                }
                setIP(tmpIp);
                getLog().info("Inside dedicated initDone");
            } catch (Exception e) {
                throw new HSUserException("mixedipresource.ip");
            }
        } else if (this.typeIp != 1000) {
            setIP(getSharedIP());
            getLog().info("Inside shared initDone");
        }
        getLog().info("Got ip");
    }

    public MixedIPResource(ResourceId rId) throws Exception {
        super(rId);
        this.typeIp = 2;
        this.strIp = "";
        try {
            setIP(HostManager.getHost(recursiveGet("host_id")).getIPbyRid(getId()));
            getLog().info("Inside dedicated " + getId());
            this.typeIp = 1;
        } catch (NotFoundException e) {
            setIP(getSharedIP());
            getLog().info("Inside shared " + getId());
            this.typeIp = 2;
        }
    }

    @Override // psoft.hsphere.Resource
    public void deletePrev() throws Exception {
        if (!C0004CP.isMyDNSEnabled() && ((Domain) getParent().get()).isDeletePrev()) {
            this.deletePrev = true;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (getIP() != null && isDedicated()) {
            HostManager.getHost(recursiveGet("host_id")).releaseIP(getIP());
        }
    }

    public synchronized TemplateModel FM_reconfig() throws Exception {
        Session.getLog().debug("Resource " + this + " reconfig");
        IPDependentResource hosting = null;
        Iterator ri = new IPDependentIterator(getParent());
        while (ri.hasNext()) {
            IPDependentResource res = (IPDependentResource) ri.next();
            if (((Resource) res).getId().getType() == 9) {
                hosting = res;
            } else {
                try {
                    res.ipRestart();
                    Session.getLog().debug("Resource is " + res + " restarted");
                } catch (Exception ex) {
                    Session.getLog().debug("Resource is not " + res + " restarted", ex);
                }
            }
        }
        if (hosting != null) {
            try {
                hosting.ipRestart();
                Session.getLog().debug("Resource is " + hosting + " restarted");
            } catch (Exception ex2) {
                Session.getLog().debug("Resource is not " + hosting + " restarted", ex2);
            }
        }
        return this;
    }

    public synchronized TemplateModel FM_ipdelete() throws Exception {
        Session.getLog().debug("Resource " + this + " delete");
        Iterator ri = new IPDeletedIterator(getParent());
        while (ri.hasNext()) {
            IPDeletedResource res = (IPDeletedResource) ri.next();
            try {
                res.ipDelete();
                Session.getLog().debug("Resource is " + res + " Deleted");
            } catch (Exception ex) {
                Session.getLog().debug("Resource is not " + res + " deleted", ex);
            }
        }
        return this;
    }

    public static Hashtable getIpInfoByName(String ip) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip, r_id, account_id FROM l_server_ips, parent_child WHERE l_server_ips.r_id = parent_child.child_id AND ip = ?");
            ps.setString(1, ip.trim());
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            Session.getLog().debug("Ip :" + ip);
            if (rs.next()) {
                Hashtable resource = new Hashtable(2);
                resource.put("resource_id", new Long(rs.getLong(2)));
                resource.put("account_id", new Long(rs.getLong(3)));
                Session.getLog().debug("Dedicated IP:" + ip + " account_id:" + rs.getLong(3) + " resourceId:" + rs.getLong(2));
                Session.closeStatement(ps);
                con.close();
                return resource;
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

    @Override // psoft.hsphere.resource.IPResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (!key.equals("shared")) {
            return key.equals("tag") ? new TemplateString(getSharedIPTag()) : super.get(key);
        } else if (isDedicated()) {
            return new TemplateString("0");
        } else {
            return new TemplateString("1");
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        if (isDedicated()) {
            if (!HostManager.getHost(newHostId).hasFreeDedicatedIP()) {
                throw new Exception("Target host has no free dedicated IPs");
            }
            return true;
        }
        try {
            HostManager.getHost(newHostId).getSharedIP(getSharedIPTag());
            return true;
        } catch (Exception ex) {
            if (ex instanceof HSUserException) {
                throw new Exception("Target host nas no shared IP with " + getSharedIPTag() + " tag");
            }
            throw ex;
        }
    }

    protected String _getIP() {
        try {
            return getIP().toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            try {
                domainName = StringShrink.doShrink(domainName2, 64);
            } catch (Exception e) {
                domainName = "";
            }
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.refund", new Object[]{_getIP(), f42df.format(begin), f42df.format(end), domainName});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            domainName = StringShrink.doShrink(domainName2, 64);
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.setup", new Object[]{_getIP(), domainName});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            domainName = StringShrink.doShrink(domainName2, 64);
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.recurrent", new Object[]{getPeriodInWords(), _getIP(), f42df.format(begin), f42df.format(end), domainName});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        String domainName;
        String domainName2 = getDomainName();
        if (domainName2 != null) {
            try {
                domainName = StringShrink.doShrink(domainName2, 64);
            } catch (Exception e) {
                domainName = "";
            }
        } else {
            domainName = "";
        }
        return Localizer.translateMessage("bill.ip.refundall", new Object[]{_getIP(), f42df.format(begin), f42df.format(end), domainName});
    }

    @Override // psoft.hsphere.Resource
    public long getTTL() {
        if (isDedicated()) {
            return TypeRegistry.getTTL(Integer.toString(getId().getType()));
        }
        return 0L;
    }

    public boolean isDedicated() {
        return this.typeIp == 1;
    }

    public void changeDNS(String oldIp, String newIp) throws Exception {
        Session.getLog().debug("Inside MixedIPResource::changeDNS() for resource " + getId());
        Iterator i = new IPPrunedResourceIterator(getParent(), Arrays.asList(new Integer(3002), new Integer(6401)));
        while (i.hasNext()) {
            ResourceId rid = (ResourceId) i.next();
            Session.getLog().debug("Got and processing resource " + rid.toString());
            DNSRecord r = (DNSRecord) rid.get();
            if (oldIp.equals(r.getData())) {
                try {
                    r.physicalDelete(oldIp.toString());
                    try {
                        r.physicalCreate(newIp);
                    } catch (Exception ex) {
                        Session.getLog().error("Error creating " + r.get("type").toString() + " DNS record " + newIp);
                        throw ex;
                    }
                } catch (Exception ex2) {
                    Session.getLog().error("Error deleting " + r.get("type").toString() + " DNS record " + oldIp.toString());
                    throw ex2;
                }
            }
        }
        if (isDedicated()) {
            setIP(HostManager.getHost(recursiveGet("host_id")).getIPbyRid(getId()));
        } else {
            setIP(HostManager.getHost(recursiveGet("host_id")).getSharedIP(getSharedIPTag()));
        }
        Session.getLog().debug("Inside MixedIPResource::changeDNS(): new IP=" + getIP());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        C0015IP newIP;
        Session.getLog().debug("Inside MixedIPResource::physicalCreate() for resource " + getId());
        if (isDedicated()) {
            newIP = HostManager.getHost(targetHostId).getExclusiveIP(getId());
        } else {
            newIP = HostManager.getHost(targetHostId).getSharedIP(getSharedIPTag());
        }
        setIP(newIP);
    }

    public void addIP(long targetHostId) throws Exception {
    }
}
