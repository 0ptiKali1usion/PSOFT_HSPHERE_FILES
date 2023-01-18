package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.DomainAlias;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.dns.MXRecord;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailRelay.class */
public class MailRelay extends Resource implements HostDependentResource {
    private long hostId;

    public MailRelay(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mserver_id  FROM mail_relay WHERE id = ?");
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

    public MailRelay(int type, Collection values) throws Exception {
        super(type, values);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        HostEntry mserver = null;
        HostEntry main_mserver = recursiveGet("mail_server");
        if (Session.getPropertyString("IRIS_USER").equals("")) {
            LinkedList mservers = HostManager.getMailRelayHosts(new Integer(Session.getAccount().getPlanValue("_HOST_mail")).intValue());
            int i = 0;
            while (true) {
                if (i < mservers.size()) {
                    if (mservers.get(i).equals(main_mserver)) {
                        i++;
                    } else {
                        mserver = (HostEntry) mservers.get(i);
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        if (mserver == null) {
            mserver = main_mserver;
        }
        this.hostId = mserver.getId();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO mail_relay (id, mserver_id ) VALUES (?, ?)");
                ps.setLong(1, getId().getId());
                ps.setLong(2, this.hostId);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                if (mserver != main_mserver || !Session.getPropertyString("IRIS_USER").equals("")) {
                    if (Session.getPropertyString("IRIS_USER").equals("")) {
                        physicalCreate(this.hostId);
                    }
                    ((MXRecord) getParent().get().FM_getChild("mx").get()).addMXList(this.hostId);
                    new LinkedList();
                    Resource r = getParent().get().getParent().get();
                    if (r instanceof Domain) {
                        Collection<ResourceId> mdas = r.getId().findChildren("mail_domain_alias");
                        if (mdas.size() > 0) {
                            for (ResourceId resourceId : mdas) {
                                MailDomainAlias mda = (MailDomainAlias) get(resourceId);
                                ResourceId rid = mda.getId().findChild("mx");
                                if (rid != null) {
                                    MXRecord mx = (MXRecord) rid.get();
                                    mx.addMXList(this.hostId);
                                }
                            }
                        }
                    }
                }
            } catch (SQLException se) {
                getLog().warn("Error inserting new MailRelay ", se);
                throw new Exception("Unable to add MailRealy " + mserver.get("name").toString());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized && Session.getPropertyString("IRIS_USER").equals("") && getHostId() != recursiveGet("mail_server").getId()) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mail_relay WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (this.initialized) {
                ((MXRecord) getParent().get().FM_getChild("mx").get()).deleteMXList();
                new LinkedList();
                Resource r = getParent().get().getParent().get();
                if (r instanceof Domain) {
                    Collection<ResourceId> mdas = r.getId().findChildren("mail_domain_alias");
                    if (mdas.size() > 0) {
                        for (ResourceId resourceId : mdas) {
                            MailDomainAlias mda = new MailDomainAlias(resourceId);
                            ResourceId rid = mda.getId().findChild("mx");
                            if (rid != null) {
                                MXRecord mx = (MXRecord) rid.get();
                                mx.deleteMXList();
                            }
                        }
                    }
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
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
    public void setHostId(long newHostId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_relay SET mail_server = ? WHERE id = ?");
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
        } else {
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        if (HostEntry.getEmulationMode()) {
            return;
        }
        if (getParent().get().getParent().get() instanceof Domain) {
            args.add(((Domain) getParent().get().getParent().get()).getName());
        } else if (getParent().get().getParent().get() instanceof DomainAlias) {
            args.add(((DomainAlias) getParent().get().getParent().get()).getName());
        } else {
            throw new Exception("Can't find the domain name");
        }
        host.exec("mdomainrelay-add.pl", args);
        Resource r = getParent().get().getParent().get();
        new LinkedList();
        if (r instanceof Domain) {
            Collection<ResourceId> mdas = r.getId().findChildren("mail_domain_alias");
            if (mdas.size() > 0) {
                for (ResourceId resourceId : mdas) {
                    MailDomainAlias mda = new MailDomainAlias(resourceId);
                    ArrayList args2 = new ArrayList();
                    args2.add(mda.getDomainAlias());
                    host.exec("mdomainrelay-add.pl", args2);
                }
            }
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ArrayList args = new ArrayList();
        HostEntry host = HostManager.getHost(targetHostId);
        if (getParent().get().getParent().get() instanceof Domain) {
            args.add(((Domain) getParent().get().getParent().get()).getName());
        } else if (getParent().get().getParent().get() instanceof DomainAlias) {
            args.add(((DomainAlias) getParent().get().getParent().get()).getName());
        } else {
            throw new Exception("Can't find the domain name");
        }
        host.exec("mdomainrelay-del.pl", args);
        Resource r = getParent().get().getParent().get();
        new LinkedList();
        if (r instanceof Domain) {
            Collection<ResourceId> mdas = r.getId().findChildren("mail_domain_alias");
            if (mdas.size() > 0) {
                for (ResourceId resourceId : mdas) {
                    MailDomainAlias mda = new MailDomainAlias(resourceId);
                    ArrayList args2 = new ArrayList();
                    args2.add(mda.getDomainAlias());
                    host.exec("mdomainrelay-del.pl", args2);
                }
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mail_relay.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mail_relay.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mail_relay.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mail_relay.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    public void reconfigure() throws Exception {
        long newHostId = recursiveGet("mail_server").getId();
        String newMX = recursiveGet("mail_server").get("name").toString();
        physicalDelete(this.hostId);
        ((MXRecord) getParent().get().FM_getChild("mx").get()).physicalDeleteMXList();
        physicalCreate(newHostId);
        ((MXRecord) getParent().get().FM_getChild("mx").get()).physicalCreateMXList(newMX);
        this.hostId = newHostId;
    }
}
