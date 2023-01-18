package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailService.class */
public class MailService extends Resource implements HostDependentResource {
    private long hostId;

    public MailService(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mail_server  FROM mail_services WHERE id = ?");
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

    public MailService(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        HostEntry he = (HostEntry) i.next();
        this.hostId = he.getId();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO mail_services(id, mail_server) VALUES (?, ?)");
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
    public void deletePrev() throws Exception {
        if (Session.getPropertyString("MYDNS_USER").equals("")) {
            if (((getParent().get() instanceof Domain) && getParent().get().isDeletePrev()) || ((getParent().get() instanceof DomainAlias) && getParent().get().isDeletePrev())) {
                this.deletePrev = true;
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mail_services WHERE id = ?");
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
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{recursiveGet("name").toString()};
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("mail_server".equals(key)) {
            try {
                return HostManager.getHost(this.hostId);
            } catch (Exception e) {
                return null;
            }
        } else if ("mail_server_name".equals(key)) {
            try {
                return HostManager.getHost(this.hostId).get("name");
            } catch (Exception e2) {
                return null;
            }
        } else if ("domain_name".equalsIgnoreCase(key)) {
            try {
                return new TemplateString(recursiveGet("name").toString());
            } catch (Exception e3) {
                return null;
            }
        } else {
            if ("extwebmail".equals(key)) {
                try {
                    return new TemplateString(getExtWebMailHost());
                } catch (Exception e4) {
                    Session.getLog().error("Failed to get External WebMail Host for mail server" + this.hostId, e4);
                }
            }
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() {
        return this.hostId;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_services SET mail_server = ? WHERE id = ?");
            ps.setLong(1, newHostId);
            ps.setLong(2, getId().getId());
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

    public TemplateModel FM_existDomainAliasesWithoutSPF() throws Exception {
        Resource r = getParent().get();
        if (r instanceof Domain) {
            Collection<ResourceId> das = r.getId().findChildren("domain_alias");
            if (das.size() > 0) {
                for (ResourceId rid : das) {
                    ResourceId ms = rid.findChild("mail_service");
                    if (ms == null) {
                        ms = rid.findChild("mail_domain_alias");
                    }
                    if (ms != null && ms.findChild("spf") == null) {
                        return new TemplateString("true");
                    }
                }
            }
        }
        return new TemplateString("false");
    }

    public TemplateModel FM_getSPFOfDomainAliases() throws Exception {
        return new TemplateList(getSPFOfDomainAliases());
    }

    public List getSPFOfDomainAliases() throws Exception {
        ResourceId ms;
        Resource r = getParent().get();
        List l = new ArrayList();
        if (r instanceof Domain) {
            Collection<ResourceId> das = r.getId().findChildren("domain_alias");
            if (das.size() > 0) {
                for (ResourceId rid : das) {
                    ResourceId ms2 = rid.findChild("mail_service");
                    if (ms2 == null) {
                        ms2 = rid.findChild("mail_domain_alias");
                    }
                    if (ms2 != null && (ms = ms2.findChild("spf")) != null) {
                        l.add(ms);
                    }
                }
            }
        }
        return l;
    }

    public TemplateModel FM_existDomainAliasesWithSPF() throws Exception {
        Resource r = getParent().get();
        if (r instanceof Domain) {
            Collection<ResourceId> das = r.getId().findChildren("domain_alias");
            if (das.size() > 0) {
                for (ResourceId rid : das) {
                    ResourceId ms = rid.findChild("mail_service");
                    if (ms == null) {
                        ms = rid.findChild("mail_domain_alias");
                    }
                    if (ms != null && ms.findChild("spf") != null) {
                        return new TemplateString("true");
                    }
                }
            }
        }
        return new TemplateString("false");
    }

    public TemplateModel FM_addDomainAliasSPFs(String processing) throws Exception {
        Resource r = getParent().get();
        if (r instanceof Domain) {
            List spfProps = new ArrayList();
            ResourceId ri = getId().findChild("spf");
            if (!processing.equalsIgnoreCase("use_default")) {
                spfProps.add(processing);
            } else if (ri != null) {
                spfProps = ((SPFResource) ri.get()).getProperties();
            }
            Collection<ResourceId> das = r.getId().findChildren("domain_alias");
            if (das.size() > 0) {
                for (ResourceId rid : das) {
                    ResourceId ri2 = rid.findChild("mail_service");
                    if (ri2 == null) {
                        ri2 = rid.findChild("mail_domain_alias");
                    }
                    if (ri2 != null && ri2.findChild("spf") == null) {
                        ri2.get().addChild("spf", null, spfProps);
                    }
                }
            }
        }
        return this;
    }

    public TemplateModel FM_deleteDomainAliasSPFs() throws Exception {
        ResourceId ms;
        Resource r = getParent().get();
        if (r instanceof Domain) {
            Collection<ResourceId> das = r.getId().findChildren("domain_alias");
            if (das.size() > 0) {
                for (ResourceId rid : das) {
                    ResourceId ms2 = rid.findChild("mail_service");
                    if (ms2 == null) {
                        ms2 = rid.findChild("mail_domain_alias");
                    }
                    if (ms2 != null && (ms = ms2.findChild("spf")) != null) {
                        ms.get().FM_cdelete(0);
                    }
                }
            }
        }
        return this;
    }

    public TemplateModel FM_estimateNewDomainAliasSPFAmount() throws Exception {
        int count = 0;
        Resource r = getParent().get();
        if (r instanceof Domain) {
            Collection<ResourceId> das = r.getId().findChildren("domain_alias");
            if (das.size() > 0) {
                for (ResourceId rid : das) {
                    ResourceId ms = rid.findChild("mail_service");
                    if (ms == null) {
                        ms = rid.findChild("mail_domain_alias");
                    }
                    if (ms != null && ms.findChild("spf") == null) {
                        count++;
                    }
                }
            }
        }
        return new TemplateString(count);
    }

    public String getExtWebMailHost() throws Exception {
        String resultHostName;
        HostEntry he = HostManager.getHost(getHostId());
        String webId = he.getOption("ext_webmail");
        if (webId != null && !"".equals(webId)) {
            HostEntry heWeb = HostManager.getHost(webId);
            resultHostName = heWeb.getIP().getIP();
        } else {
            resultHostName = he.getIP().getIP();
        }
        return resultHostName;
    }
}
