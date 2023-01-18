package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.dns.MXRecord;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailDomainAlias.class */
public class MailDomainAlias extends Resource implements HostDependentResource {
    protected String domainAlias;
    protected String domainName;
    protected String parentType;

    public MailDomainAlias(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT domain_alias FROM mail_domain_aliases WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.domainAlias = rs.getString("domain_alias");
            } else {
                notFound();
            }
            this.domainName = getDomainName();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public MailDomainAlias(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.domainAlias = ((String) i.next()).toLowerCase();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT COUNT(id) FROM mail_domain_aliases WHERE domain_alias = ?");
            ps2.setString(1, this.domainAlias);
            ResultSet rs = ps2.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                throw new HSUserException("maildomainalias.exists", new Object[]{this.domainAlias});
            }
            ps = con.prepareStatement("INSERT INTO mail_domain_aliases VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.domainAlias);
            ps.executeUpdate();
            this.domainName = getDomainName();
            physicalCreate(getHostId());
            ResourceId rid = getParent().get().getParent().findChild("mail_service").findChild("mail_relay");
            if (rid != null) {
                MailRelay mr = (MailRelay) rid.get();
                if (!C0004CP.isIrisEnabled()) {
                    new ArrayList();
                    HostEntry host = HostManager.getHost(getHostId());
                    ArrayList args = new ArrayList();
                    args.add(getDomainAlias());
                    host.exec("mdomainrelay-add.pl", args);
                }
                ResourceId rid2 = getId().findChild("mx");
                if (rid2 != null) {
                    MXRecord mx = (MXRecord) rid2.get();
                    mx.addMXList(mr.getHostId());
                }
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (!C0004CP.isIrisEnabled()) {
                ResourceId rid = getParent().get().getParent().findChild("mail_service").findChild("mail_relay");
                if (rid != null) {
                    new ArrayList();
                    HostEntry host = HostManager.getHost(getHostId());
                    ArrayList args = new ArrayList();
                    args.add(getDomainAlias());
                    host.exec("mdomainrelay-del.pl", args);
                }
            }
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mail_domain_aliases WHERE id = ?");
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
        if (key.equals("id")) {
            return new TemplateString(getId().getId());
        }
        if (key.equals("domain_name")) {
            return new TemplateString(this.domainName);
        }
        if (key.equals("domain_alias")) {
            return new TemplateString(this.domainAlias);
        }
        if ("mail_server".equals(key)) {
            try {
                return recursiveGet("mail_service").get("mail_server");
            } catch (Exception e) {
                Session.getLog().error("Error getting mail_server", e);
            }
        }
        if ("mx".equals(key)) {
            try {
                return FM_getChild("mx");
            } catch (Exception e2) {
                Session.getLog().error("Error getting child mx", e2);
            }
        }
        return super.get(key);
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (!this.suspended) {
            return;
        }
        super.resume();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        MailServices.createMailDomainAlias(HostManager.getHost(targetHostId), this.domainAlias, getDomainName());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        MailServices.deleteMailDomainAlias(HostManager.getHost(targetHostId), this.domainAlias, getDomainName());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return ((MailService) getParent().get().getParent().get().FM_getChild("mail_service").get()).getHostId();
    }

    public String getDomainAlias() throws Exception {
        if (this.domainAlias != null) {
            return this.domainAlias;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT domain_alias FROM mail_domain_aliases WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.domainName = rs.getString("domain_alias");
            } else {
                notFound();
            }
            return this.domainAlias;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{getDomainAlias(), getDomainName()};
    }

    public String getDomainName() throws Exception {
        if (this.domainName != null) {
            return this.domainName;
        }
        Resource res = getParent().get();
        if (getParentType().equals("domain_alias")) {
            this.domainName = res.getParent().get("name").toString();
        }
        return this.domainName;
    }

    protected String getParentType() throws Exception {
        if (this.parentType != null) {
            return this.parentType;
        }
        Resource res = getParent().get();
        this.parentType = res.getResourceType().getType();
        return this.parentType;
    }
}
