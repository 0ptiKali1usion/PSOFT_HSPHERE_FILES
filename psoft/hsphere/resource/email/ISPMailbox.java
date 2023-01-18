package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/ISPMailbox.class */
public class ISPMailbox extends Resource implements HostDependentResource {
    protected String email;
    protected String fullEmail;
    protected String password;

    public ISPMailbox(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT email, password, full_email FROM ispmailboxes WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.email = rs.getString(1);
                this.password = rs.getString(2);
                this.fullEmail = rs.getString(3);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public ISPMailbox(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        this.email = ((String) i.next()).toLowerCase();
        this.password = (String) i.next();
    }

    protected HostEntry getMailServer() throws Exception {
        return HostManager.getHost(getMailServerId(getPlanValue("ISP_MAIL_DOMAIN")));
    }

    protected long getMailServerId(String domain) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT mail_server FROM mail_services, domains, parent_child WHERE domains.name = ? AND parent_child.parent_id = domains.id AND parent_child.child_id = mail_services.id");
            ps.setString(1, domain);
            ResultSet rs = ps.executeQuery();
            rs.next();
            long j = rs.getLong(1);
            Session.closeStatement(ps);
            return j;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        this.fullEmail = this.email + "@" + getPlanValue("ISP_MAIL_DOMAIN");
        if (AntiSpam.DEFAULT_LEVEL_VALUE.equals(this.email)) {
            throw new HSUserException("mailresource.name_disallowed", new String[]{this.email});
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ispmailboxes (id, email, password, full_email)  VALUES (?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.email);
            ps.setString(3, this.password);
            ps.setString(4, getFullEmail());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            MailServices.createMailbox(getMailServer(), getFullEmail(), this.password, 0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{getFullEmail()};
    }

    protected String getFullEmail() {
        return this.fullEmail;
    }

    public String getPassword() throws TemplateModelException {
        return this.password;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            MailServices.deleteMailbox(getMailServer(), getFullEmail(), false);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM ispmailboxes WHERE id = ?");
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

    public TemplateModel FM_changePassword(String password) throws Exception {
        changePassword(password);
        return this;
    }

    public TemplateModel FM_changeDescription(String description) throws Exception {
        setDescription(description);
        return this;
    }

    public void changePassword(String password) throws Exception {
        accessCheck(0);
        MailServices.setMboxPassword(getMailServer(), getFullEmail(), password);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE ispmailboxes SET password = ? WHERE id = ?");
            ps.setString(1, password);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.password = password;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("password".equals(key)) {
            return new TemplateString(this.password);
        }
        if ("email".equals(key)) {
            return new TemplateString(this.email);
        }
        if ("fullemail".equals(key)) {
            return new TemplateString(getFullEmail());
        }
        if ("mail_server".equals(key)) {
            try {
                return getMailServer();
            } catch (Exception e) {
                return null;
            }
        } else if ("mail_server_name".equals(key)) {
            try {
                return getMailServer().get("name");
            } catch (Exception e2) {
                return null;
            }
        } else {
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        super.suspend();
        MailServices.suspendMailDomain(getMailServer(), getFullEmail());
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            super.resume();
            MailServices.resumeMailDomain(getMailServer(), getFullEmail());
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        MailServices.createMailbox(HostManager.getHost(targetHostId), getFullEmail(), this.password, 0);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        MailServices.deleteMailbox(HostManager.getHost(targetHostId), getFullEmail());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return getMailServerId("ISP_MAIL_DOMAIN");
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox.refund", new Object[]{getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mailbox.setup", new Object[]{getFullEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox.recurrent", new Object[]{getPeriodInWords(), getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mailbox.refundall", new Object[]{getFullEmail(), f42df.format(begin), f42df.format(end)});
    }
}
