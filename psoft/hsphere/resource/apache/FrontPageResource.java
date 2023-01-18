package psoft.hsphere.resource.apache;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.IPDependentResource;
import psoft.hsphere.resource.email.MailDomain;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/FrontPageResource.class */
public class FrontPageResource extends Resource implements IPDependentResource, IPDeletedResource, HostDependentResource {
    protected String login;
    protected String password;

    public FrontPageResource(int type, Collection init) throws Exception {
        super(type, init);
        Iterator i = init.iterator();
        this.login = (String) i.next();
        this.password = (String) i.next();
    }

    public FrontPageResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT login, password FROM frontpage WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.login = rs.getString(1);
                this.password = rs.getString(2);
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
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO frontpage (id, login, password) VALUES (?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.login);
            ps.setString(3, this.password);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            update(this.login, this.password);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_update(String login, String password) throws Exception {
        update(login, password);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE frontpage SET login = ?, password = ? WHERE id = ?");
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.login = login;
            this.password = password;
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("login") ? new TemplateString(this.login) : key.equals("password") ? new TemplateString(this.password) : key.equals("getProperties") ? new GetProperties() : key.equals("setProperties") ? new SetProperties() : super.get(key);
    }

    public void setProperties(String mailSender, String mailReplyTo) throws Exception {
        SetProperties sp = new SetProperties();
        List l = new ArrayList();
        l.add("MailSender");
        l.add(mailSender);
        l.add("MailReplyTo");
        l.add(mailReplyTo);
        sp.exec(l);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        delInternal();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM frontpage WHERE id = ?");
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

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        HostEntry he = recursiveGet("host");
        List l = new ArrayList();
        l.add(recursiveGet("path").toString());
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + recursiveGet("real_name").toString());
        }
        String port = getPort();
        l.add(port);
        l.add(recursiveGet("login").toString());
        l.add(recursiveGet("group").toString());
        l.add(this.login);
        l.add(MailServices.shellQuote(this.password));
        he.exec("frontpage-install", l);
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        HostEntry he = recursiveGet("host");
        List l = new ArrayList();
        l.add(recursiveGet("path").toString());
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + recursiveGet("real_name").toString());
        }
        String port = getPort();
        l.add(port);
        he.exec("frontpage-uninstall", l);
    }

    public TemplateModel FM_fix() throws Exception {
        Map properties = null;
        try {
            properties = getProperties(Arrays.asList("MailSender", "MailReplyTo"));
        } catch (Exception e) {
        }
        delInternal();
        update(this.login, this.password);
        if (properties != null) {
            try {
                setProperties(properties);
            } catch (Exception ex) {
                Session.getLog().debug("Unable to set old properties. ", ex);
            }
        }
        return this;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/apache/FrontPageResource$GetProperties.class */
    public class GetProperties implements TemplateMethodModel {
        GetProperties() {
            FrontPageResource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            try {
                List<Object> l2 = HTMLEncoder.decode(l);
                List list = new ArrayList();
                for (Object obj : l2) {
                    list.add(obj);
                }
                return new TemplateMap(FrontPageResource.this.getProperties(list));
            } catch (Exception e) {
                Session.getLog().error("Error getting FP props", e);
                return new TemplateErrorResult(e.getMessage());
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/apache/FrontPageResource$SetProperties.class */
    public class SetProperties implements TemplateMethodModel {
        SetProperties() {
            FrontPageResource.this = r4;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) {
            ResourceId domain;
            try {
                HashMap map = new HashMap();
                Iterator i = HTMLEncoder.decode(l).iterator();
                while (i.hasNext()) {
                    map.put(i.next(), i.next());
                }
                ResourceId mdid = null;
                try {
                    domain = FrontPageResource.this.getParent().get().getParent();
                } catch (Exception e) {
                }
                if (!"nodomain".equals(domain.getNamedType())) {
                    mdid = domain.findChild("mail_domain");
                    if (mdid == null) {
                        mdid = Session.getAccount().getId().findChild("mail_domain");
                    }
                    long hostid = ((MailDomain) mdid.get()).getHostId();
                    String smtp = HostManager.getHost(hostid).getPFirstIP();
                    map.put("smtphost", smtp);
                    FrontPageResource.this.setProperties(map);
                    return new TemplateOKResult();
                }
                FrontPageResource.this.setProperties(map);
                return new TemplateOKResult();
            } catch (Exception e2) {
                Session.getLog().error("Error setting FP props", e2);
                return new TemplateErrorResult(e2.getMessage());
            }
        }
    }

    protected void setProperties(Map map, HostEntry he) throws Exception {
        List l = new ArrayList();
        l.add("--site");
        l.add(recursiveGet("real_name").toString());
        l.add("--action set");
        for (String key : map.keySet()) {
            l.add("--param");
            l.add(key);
            l.add("--value");
            l.add(MailServices.shellQuote((String) map.get(key)));
        }
        he.exec("frontpage-prop", l);
    }

    protected void setProperties(Map map) throws Exception {
        setProperties(map, (HostEntry) recursiveGet("host"));
    }

    protected Map getProperties(List params) throws Exception {
        HostEntry he = recursiveGet("host");
        List l = new ArrayList();
        l.add("--site");
        l.add(recursiveGet("real_name").toString());
        l.add("--action get");
        Iterator i = params.iterator();
        while (i.hasNext()) {
            l.add("--param");
            l.add((String) i.next());
        }
        Collection col = he.exec("frontpage-prop", l);
        Map res = new HashMap();
        Iterator j = col.iterator();
        while (j.hasNext()) {
            res.put(j.next(), j.next());
        }
        return res;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    private boolean isSSLEnabled() throws Exception {
        ResourceId res = getParent();
        return (res.FM_getChild("ssl") == null && res.FM_getChild("sharedssl") == null) ? false : true;
    }

    private String getSSLPort() {
        return "443";
    }

    private String getNonSSLPort() {
        return "80";
    }

    private String getPort() throws Exception {
        return isSSLEnabled() ? getSSLPort() : getNonSSLPort();
    }

    public void physicalCreate(long targetHostId, String port) throws Exception {
        physicalCreate(HostManager.getHost(targetHostId), port);
    }

    protected void physicalCreate(HostEntry he, String port) throws Exception {
        VirtualHostingResource hosting = (VirtualHostingResource) getParent().get();
        List<Object> l = new ArrayList();
        l.add(hosting.getPath());
        l.add(recursiveGet("real_name").toString());
        l.add(port);
        l.add(hosting.recursiveGet("login").toString());
        l.add(hosting.recursiveGet("group").toString());
        l.add(this.login);
        l.add(this.password);
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + recursiveGet("real_name").toString());
        }
        StringBuffer str = new StringBuffer();
        for (Object obj : l) {
            str.append(obj).append(":");
        }
        getLog().info("--->" + ((Object) str));
        he.exec("frontpage-install", l);
        he.exec("apache-reconfig", new ArrayList());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        physicalCreate(HostManager.getHost(targetHostId));
    }

    protected void physicalCreate(HostEntry he) throws Exception {
        if (isSSLEnabled()) {
            physicalCreate(he, getNonSSLPort());
            physicalCreate(he, getSSLPort());
        } else {
            physicalCreate(he, getNonSSLPort());
        }
        try {
            Map fpProperties = getProperties(Arrays.asList("MailSender", "MailReplyTo"));
            setProperties(fpProperties, he);
        } catch (Exception ex) {
            Session.getLog().error("Error getting/setting frontpage properties ", ex);
        }
    }

    protected void update(String login, String password) throws Exception {
        HostEntry he = (HostEntry) recursiveGet("host");
        if (isSSLEnabled()) {
            update(he, getNonSSLPort(), login, password);
            update(he, getSSLPort(), login, password);
            return;
        }
        update(he, getNonSSLPort(), login, password);
    }

    protected void update(HostEntry he, String port, String login, String password) throws Exception {
        List l = new ArrayList();
        l.add(recursiveGet("path").toString());
        l.add(recursiveGet("real_name").toString());
        l.add(port);
        l.add(recursiveGet("login").toString());
        l.add(recursiveGet("group").toString());
        l.add(login);
        l.add(MailServices.shellQuote(password));
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + recursiveGet("real_name").toString());
        }
        he.exec("frontpage-install", l);
    }

    public void physicalDelete(long targetHostId, String port) throws Exception {
        physicalDelete(HostManager.getHost(targetHostId), port);
    }

    protected void physicalDelete(HostEntry he, String port) throws Exception {
        List l = new ArrayList();
        l.add(recursiveGet("path").toString());
        l.add(recursiveGet("real_name").toString());
        l.add(port);
        if (((VirtualHostingResource) getParent().get()).hasDedicatedIp()) {
            l.add(getParent().get("ip").toString());
        } else {
            l.add("www." + recursiveGet("real_name").toString());
        }
        he.exec("frontpage-uninstall", l);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        physicalDelete(HostManager.getHost(targetHostId));
    }

    protected void physicalDelete(HostEntry he) throws Exception {
        if (isSSLEnabled()) {
            physicalDelete(he, getSSLPort());
            physicalDelete(he, getNonSSLPort());
            return;
        }
        physicalDelete(he, getNonSSLPort());
    }

    protected void delInternal() throws Exception {
        if (this.initialized) {
            physicalDelete((HostEntry) recursiveGet("host"));
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.frontpage.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.frontpage.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.frontpage.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.frontpage.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("frontpage.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
