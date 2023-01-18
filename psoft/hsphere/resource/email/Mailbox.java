package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ComparableResource;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/Mailbox.class */
public class Mailbox extends Resource implements HostDependentResource, ComparableResource {
    protected String email;
    protected String fullEmail;
    protected String password;
    private String description;
    private int discardMail;
    protected LinkedList aliasedBy;
    protected static final long TIME_TO_LIVE = 10000;
    protected long aliasedByLastTimeLoaded;
    private static MailboxComparator COMPARATOR = new MailboxComparator();

    public Mailbox(ResourceId id) throws Exception {
        super(id);
        this.aliasedBy = new LinkedList();
        this.aliasedByLastTimeLoaded = 0L;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT email, password, full_email, discard_mail FROM mailboxes WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.email = rs.getString(1);
                this.password = rs.getString(2);
                this.fullEmail = rs.getString(3);
                this.discardMail = rs.getInt(4);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Mailbox(int type, Collection values) throws Exception {
        super(type, values);
        this.aliasedBy = new LinkedList();
        this.aliasedByLastTimeLoaded = 0L;
        Iterator i = values.iterator();
        this.email = ((String) i.next()).toLowerCase();
        this.password = (String) i.next();
        if (i.hasNext()) {
            this.description = (String) i.next();
        }
        this.discardMail = 0;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (AntiSpam.DEFAULT_LEVEL_VALUE.equals(this.email) || "postmaster".equals(this.email)) {
            throw new HSUserException("mailresource.name_disallowed", new String[]{this.email});
        }
        Collection<ResourceId> boxes = getParent().getChildHolder().getChildrenByName("mailbox");
        synchronized (boxes) {
            for (ResourceId rid : boxes) {
                if (!rid.equals(getId()) && this.email.equals(rid.get("email").toString())) {
                    throw new HSUserException("mailbox.exist");
                    break;
                }
            }
        }
        if (C0004CP.isIrisEnabled()) {
            Collection<ResourceId> mlist = new ArrayList();
            mlist.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
            for (ResourceId rid2 : mlist) {
                if (!rid2.equals(getId()) && this.email.equals(rid2.get("email").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
            Collection<ResourceId> alias_forw = new ArrayList();
            alias_forw.addAll(getParent().getChildHolder().getChildrenByName("mailbox_alias"));
            alias_forw.addAll(getParent().getChildHolder().getChildrenByName("mail_forward"));
            for (ResourceId rid3 : alias_forw) {
                if (!rid3.equals(getId()) && this.email.equals(rid3.get("local").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        } else {
            Collection<ResourceId> mobjects = new ArrayList();
            mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
            for (ResourceId rid4 : mobjects) {
                if (this.email.equals(rid4.get("email").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO mailboxes (id, email, password, full_email, discard_mail)  VALUES (?, ?, ?, ?, ?)");
            ps2.setLong(1, getId().getId());
            ps2.setString(2, this.email);
            ps2.setString(3, this.password);
            ps2.setString(4, getFullEmail());
            ps2.setInt(5, this.discardMail);
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO mailobject (id, full_email)  VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, getFullEmail());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            setDescription(this.description);
            MailServices.createMailbox(recursiveGet("mail_server"), getFullEmail(), this.password, 0);
            if (!C0004CP.isIrisEnabled()) {
                if (((MailDomain) getParent().get()).existChildAutoresponder(this.email, "") || ((MailDomain) getParent().get()).existChildMailForward(this.email, "") || ((MailDomain) getParent().get()).existChildMailAlias(this.email)) {
                    ((MailDomain) getParent().get()).setConfig(this.email, "");
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getEmail() {
        return this.email;
    }

    public int getDiscardMail() {
        return this.discardMail;
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{getFullEmail()};
    }

    protected String getFullEmail() throws TemplateModelException {
        if (this.fullEmail != null) {
            return this.fullEmail;
        }
        this.fullEmail = this.email + "@" + getParent().get("name");
        return this.fullEmail;
    }

    public String getPassword() throws TemplateModelException {
        return this.password;
    }

    protected String _getFullEmail() {
        try {
            return getFullEmail();
        } catch (Exception e) {
            return this.email;
        }
    }

    @Override // psoft.hsphere.Resource
    public void deletePrev() {
        if (!C0004CP.isIrisEnabled()) {
            this.deletePrev = true;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (C0004CP.isIrisEnabled()) {
                if (((MailDomain) getParent().get()).getCatchAll().equals(this.email)) {
                    MailServices.deleteMailbox(recursiveGet("mail_server"), getFullEmail(), true);
                } else {
                    MailServices.deleteMailbox(recursiveGet("mail_server"), getFullEmail(), false);
                }
            } else {
                MailServices.deleteMailbox(recursiveGet("mail_server"), getFullEmail(), false);
                if (!getParent().get().isDeletePrev()) {
                    ((MailDomain) getParent().get()).setConfig(this.email, getId().toString());
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM mailboxes WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM mailobject WHERE id = ?");
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

    public TemplateModel FM_updatePassword(String password) throws Exception {
        changePassword(password);
        return this;
    }

    public TemplateModel FM_changeDescription(String description) throws Exception {
        setDescription(description);
        return this;
    }

    public TemplateModel FM_discardMail(String action) throws Exception {
        if ("enable".equals(action)) {
            this.discardMail = 1;
        } else {
            this.discardMail = 0;
        }
        if (C0004CP.isIrisEnabled()) {
            if (((MailDomain) getParent().get()).getCatchAll().equals(this.email)) {
                MailServices.discardIncommingMail(this.fullEmail, String.valueOf(this.discardMail), new String("mailbox"), true);
            } else {
                MailServices.discardIncommingMail(this.fullEmail, String.valueOf(this.discardMail), new String("mailbox"), false);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mailboxes set discard_mail = ? WHERE id = ?");
            ps.setInt(1, this.discardMail);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (!C0004CP.isIrisEnabled()) {
                ((MailDomain) getParent().get()).setConfig(this.email, "");
            }
            return new TemplateOKResult();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void changePassword(String password) throws Exception {
        accessCheck(0);
        if (C0004CP.isIrisEnabled()) {
            boolean is_catch_all = false;
            if (((MailDomain) getParent().get()).getCatchAll().equals(this.email)) {
                is_catch_all = true;
            }
            MailServices.setMboxPassword(recursiveGet("mail_server"), getFullEmail(), password, is_catch_all);
        } else {
            MailServices.setMboxPassword(recursiveGet("mail_server"), getFullEmail(), password, false);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mailboxes set password = ? WHERE id = ?");
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
        if (!"email".equals(key) && !"local".equals(key)) {
            if ("fullemail".equals(key)) {
                return new TemplateString(getFullEmail());
            }
            if ("discard_incomm_mail".equals(key)) {
                if (this.discardMail == 1) {
                    return new TemplateString("enabled");
                }
                return new TemplateString("disabled");
            } else if ("list_aliased_by".equals(key)) {
                try {
                    return new TemplateList(getAliasedBy());
                } catch (Exception e) {
                    Session.getLog().debug("Can not get aliased_by values", e);
                    throw new TemplateModelException(e.getMessage());
                }
            } else {
                return super.get(key);
            }
        }
        return new TemplateString(this.email);
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        super.suspend();
        if (((MailDomain) getParent().get()).getCatchAll().equals(this.email)) {
            MailServices.suspendMailDomain(recursiveGet("mail_server"), getFullEmail(), true);
        } else {
            MailServices.suspendMailDomain(recursiveGet("mail_server"), getFullEmail(), false);
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            super.resume();
            if (((MailDomain) getParent().get()).getCatchAll().equals(this.email)) {
                MailServices.resumeMailDomain(recursiveGet("mail_server"), getFullEmail(), true);
            } else {
                MailServices.resumeMailDomain(recursiveGet("mail_server"), getFullEmail(), false);
            }
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    public void realPhysicalCreate(long targetHostId) throws Exception {
        MailServices.createMailbox(HostManager.getHost(targetHostId), getFullEmail(), this.password, 0);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        if (((MailDomain) getParent().get()).getCatchAll().equals(this.email)) {
            MailServices.deleteMailbox(HostManager.getHost(targetHostId), getFullEmail(), true);
        } else {
            MailServices.deleteMailbox(HostManager.getHost(targetHostId), getFullEmail(), false);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return recursiveGet("mail_server").getId();
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox.refund", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mailbox.setup", new Object[]{_getFullEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox.recurrent", new Object[]{getPeriodInWords(), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mailbox.refundall", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    public static Hashtable getMailboxInfoByName(String fullEmail) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, account_id FROM mailboxes, parent_child WHERE full_email = ? AND mailboxes.id + 0 = parent_child.child_id ");
            ps.setString(1, fullEmail.trim());
            ps.executeQuery();
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Hashtable mailbox = new Hashtable(2);
                mailbox.put("resource_id", new Long(rs.getLong(1)));
                mailbox.put("account_id", new Long(rs.getLong(2)));
                Session.closeStatement(ps);
                con.close();
                return mailbox;
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

    public boolean isDiscard() {
        if (this.discardMail == 1) {
            return true;
        }
        return false;
    }

    @Override // psoft.hsphere.ComparableResource
    public String comparableString() throws Exception {
        return getFullEmail();
    }

    public static Comparator getComparator() {
        return COMPARATOR;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/email/Mailbox$MailboxComparator.class */
    static class MailboxComparator implements Comparator {
        MailboxComparator() {
        }

        @Override // java.util.Comparator
        public int compare(Object o1, Object o2) {
            try {
                Mailbox m1 = (Mailbox) o1;
                Mailbox m2 = (Mailbox) o2;
                return m1.getFullEmail().compareTo(m2.getFullEmail());
            } catch (Exception e) {
                Session.getLog().warn("Unable to compare mailboxes", e);
                return 0;
            }
        }
    }

    public LinkedList getAliasedBy() throws Exception {
        long now = TimeUtils.currentTimeMillis();
        if (now - this.aliasedByLastTimeLoaded > TIME_TO_LIVE) {
            this.aliasedBy.clear();
            Collection<ResourceId> col = getParent().get().getChildHolder().getChildrenByName("mailbox_alias");
            for (ResourceId resourceId : col) {
                MailAlias ma = (MailAlias) resourceId.get();
                if (ma.existSubscriber(this.email)) {
                    this.aliasedBy.add(ma.local);
                }
            }
            this.aliasedByLastTimeLoaded = now;
        }
        return this.aliasedBy;
    }
}
