package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ComparableResource;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailForward.class */
public class MailForward extends Resource implements ComparableResource {
    protected String local;
    protected List subscribers;
    protected String description;
    protected String fullEmail;

    public List getSubscribers() {
        return this.subscribers;
    }

    public String getLocal() {
        return this.local;
    }

    public MailForward(ResourceId id) throws Exception {
        super(id);
        this.subscribers = Collections.synchronizedList(new ArrayList());
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT email_local, email_foreign FROM mail_forwards WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String foreignClob = Session.getClobValue(rs, 2);
                this.local = rs.getString(1);
                StringTokenizer foreign = new StringTokenizer(foreignClob, ";");
                while (foreign.hasMoreTokens()) {
                    String tk = foreign.nextToken();
                    if (tk != null && tk.trim().length() > 0) {
                        this.subscribers.add(tk);
                    }
                }
            } else {
                notFound();
            }
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("SELECT full_email FROM mailobject WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ResultSet rs2 = ps2.executeQuery();
            if (rs2.next()) {
                this.fullEmail = rs2.getString(1);
            } else {
                ps2.close();
                this.fullEmail = this.local + "@" + getParent().get("name");
                ps2 = con.prepareStatement("INSERT INTO mailobject VALUES (?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, this.fullEmail);
                ps2.executeUpdate();
            }
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public MailForward(int type, Collection values) throws Exception {
        super(type, values);
        this.subscribers = Collections.synchronizedList(new ArrayList());
        Iterator i = values.iterator();
        this.local = ((String) i.next()).toLowerCase();
        if (!C0004CP.isIrisEnabled()) {
            String emails = (String) i.next();
            StringTokenizer tokenizer = new StringTokenizer(emails, ",; \n\t\r");
            while (tokenizer.hasMoreTokens()) {
                String email = tokenizer.nextToken();
                this.subscribers.add(email);
            }
            return;
        }
        this.subscribers.add((String) i.next());
        this.description = (String) i.next();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (AntiSpam.DEFAULT_LEVEL_VALUE.equals(this.local)) {
            throw new HSUserException("mailresource.name_disallowed", new String[]{this.local});
        }
        this.fullEmail = this.local + "@" + getParent().get("name");
        if (C0004CP.isIrisEnabled()) {
            Collection<ResourceId> boxes = new ArrayList();
            boxes.addAll(getParent().getChildHolder().getChildrenByName("mailbox"));
            boxes.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
            for (ResourceId rid : boxes) {
                if (!rid.equals(getId()) && this.local.equals(rid.get("email").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
            Collection<ResourceId> aliases = new ArrayList();
            aliases.addAll(getParent().getChildHolder().getChildrenByName("mailbox_alias"));
            for (ResourceId rid2 : aliases) {
                if (!rid2.equals(getId()) && this.local.equals(rid2.get("local").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        } else {
            Collection<ResourceId> mobjects = new ArrayList();
            mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
            for (ResourceId rid3 : mobjects) {
                if (this.local.equals(rid3.get("email").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        }
        for (String str : this.subscribers) {
            if (getFullEmail().equals(str)) {
                throw new HSUserException("autoresponder.mail");
            }
        }
        Collection<ResourceId> forwards = getParent().getChildHolder().getChildrenByName("mail_forward");
        synchronized (forwards) {
            for (ResourceId rid4 : forwards) {
                if (!rid4.equals(getId()) && this.local.equals(rid4.get("local").toString())) {
                    throw new HSUserException("mailforward.exist");
                    break;
                }
            }
        }
        Collection<ResourceId> mobjects2 = new ArrayList();
        mobjects2.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
        for (ResourceId rid5 : mobjects2) {
            if (this.local.equals(rid5.get("email").toString())) {
                throw new HSUserException("mailobject.exist");
                break;
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO mail_forwards(id, email_local, email_foreign) VALUES(?, ?, ?)");
            ps2.setLong(1, getId().getId());
            ps2.setString(2, this.local);
            StringBuffer tmp = new StringBuffer();
            for (String str2 : this.subscribers) {
                tmp.append(str2).append(";");
            }
            Session.setClobValue(ps2, 3, tmp.toString());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO mailobject (id, full_email)  VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, this.fullEmail);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (C0004CP.isIrisEnabled()) {
                setDescription(this.description);
                for (String str3 : this.subscribers) {
                    MailServices.createForward(getFullEmail(), str3, false);
                }
                return;
            }
            ((MailDomain) getParent().get()).setConfig(this.local, "");
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

    protected String _getFullEmail() {
        try {
            return getFullEmail();
        } catch (Exception e) {
            return this.local;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (C0004CP.isIrisEnabled()) {
                boolean is_catch_all = false;
                if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                    is_catch_all = true;
                }
                for (String str : this.subscribers) {
                    MailServices.removeForward(getFullEmail(), str, is_catch_all);
                }
            } else if (!getParent().get().isDeletePrev()) {
                ((MailDomain) getParent().get()).setConfig(this.local, getId().toString());
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM mail_forwards WHERE id = ?");
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

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "local".equals(key) ? new TemplateString(this.local) : "subscribers".equals(key) ? new TemplateList(this.subscribers) : "subs_cnt".equals(key) ? new TemplateString(this.subscribers.size()) : "fullemail".equals(key) ? new TemplateString(getFullEmail()) : super.get(key);
    }

    @Override // psoft.hsphere.ComparableResource
    public String comparableString() throws Exception {
        return getFullEmail();
    }

    public void removeSubscriber(String foreign) throws Exception {
        if (this.subscribers.size() == 1) {
            throw new HSUserException("mailforward.last");
        }
        if (this.subscribers.contains(foreign)) {
            if (C0004CP.isIrisEnabled()) {
                if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                    MailServices.removeForward(getFullEmail(), foreign, true);
                } else {
                    MailServices.removeForward(getFullEmail(), foreign, false);
                }
                this.subscribers.remove(foreign);
                updateSubscribers();
                return;
            }
            this.subscribers.remove(foreign);
            updateSubscribers();
            try {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
                return;
            } catch (Exception e) {
                this.subscribers.add(foreign);
                updateSubscribers();
                throw e;
            }
        }
        throw new HSUserException("mailforward.nonexist");
    }

    public void removeSubscribers(String foreigns) throws Exception {
        List subscribers_old = this.subscribers;
        StringTokenizer tokenizer = new StringTokenizer(foreigns, ";");
        if (this.subscribers.size() - tokenizer.countTokens() < 1) {
            throw new HSUserException("mailforward.last");
        }
        while (tokenizer.hasMoreTokens()) {
            String foreign = tokenizer.nextToken();
            if (this.subscribers.contains(foreign)) {
                if (C0004CP.isIrisEnabled()) {
                    if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                        MailServices.removeForward(getFullEmail(), foreign, true);
                    } else {
                        MailServices.removeForward(getFullEmail(), foreign, false);
                    }
                    this.subscribers.remove(foreign);
                    updateSubscribers();
                } else {
                    this.subscribers.remove(foreign);
                }
            }
        }
        if (!C0004CP.isIrisEnabled()) {
            updateSubscribers();
            try {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
            } catch (Exception e) {
                this.subscribers = subscribers_old;
                updateSubscribers();
                throw e;
            }
        }
    }

    public void addSubscriber(String foreign) throws Exception {
        if (getFullEmail().equals(foreign)) {
            throw new HSUserException("autoresponder.mail");
        }
        if (!this.subscribers.contains(foreign)) {
            if (C0004CP.isIrisEnabled()) {
                if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                    MailServices.createForward(getFullEmail(), foreign, true);
                } else {
                    MailServices.createForward(getFullEmail(), foreign, false);
                }
                this.subscribers.add(foreign);
                updateSubscribers();
                return;
            }
            this.subscribers.add(foreign);
            updateSubscribers();
            try {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
                return;
            } catch (Exception e) {
                this.subscribers.remove(foreign);
                updateSubscribers();
                throw e;
            }
        }
        throw new HSUserException("mailforward.existsubsc");
    }

    private void updateSubscribers() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        StringBuffer tmp = new StringBuffer();
        for (String str : this.subscribers) {
            tmp.append(str).append(";");
        }
        try {
            ps = con.prepareStatement("UPDATE mail_forwards SET email_foreign = ? WHERE id = ?");
            Session.setClobValue(ps, 1, tmp.toString());
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

    public TemplateModel FM_addSubscriber(String foreign) throws Exception {
        addSubscriber(foreign);
        return this;
    }

    public TemplateModel FM_batchSubscribe(String emails) throws Exception {
        List subscribers_old = this.subscribers;
        StringTokenizer tokenizer = new StringTokenizer(emails, ",; \n\t\r");
        while (tokenizer.hasMoreTokens()) {
            String email = tokenizer.nextToken();
            if (!this.subscribers.contains(email)) {
                if (C0004CP.isIrisEnabled()) {
                    if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                        MailServices.createForward(getFullEmail(), email, true);
                    } else {
                        MailServices.createForward(getFullEmail(), email, false);
                    }
                    this.subscribers.add(email);
                    updateSubscribers();
                } else {
                    this.subscribers.add(email);
                }
            }
        }
        updateSubscribers();
        if (!C0004CP.isIrisEnabled()) {
            try {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
            } catch (Exception e) {
                this.subscribers = subscribers_old;
                updateSubscribers();
                throw e;
            }
        }
        return this;
    }

    public TemplateModel FM_changeDescription(String description) throws Exception {
        setDescription(description);
        return this;
    }

    public TemplateModel FM_removeSubscriber(String foreign) throws Exception {
        removeSubscriber(foreign);
        return this;
    }

    public TemplateModel FM_removeSubscribers(String foreigns) throws Exception {
        removeSubscribers(foreigns);
        return this;
    }

    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox_forward.refund", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mailbox_forward.setup", new Object[]{_getFullEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox_forward.recurrent", new Object[]{getPeriodInWords(), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mailbox_forward.refundall", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }
}
