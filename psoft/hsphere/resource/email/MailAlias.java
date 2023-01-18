package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
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
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailAlias.class */
public class MailAlias extends Resource implements ComparableResource {
    protected String local;
    protected String foreign;
    private String description;
    protected List subscribers;
    protected String fullEmail;

    public MailAlias(ResourceId id) throws Exception {
        super(id);
        this.subscribers = new ArrayList();
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT email_local, email_foreign FROM mail_aliases WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String foreignClob = Session.getClobValue(rs, 2);
                this.local = rs.getString(1);
                this.foreign = foreignClob.toString().trim();
                StringTokenizer foreigns = new StringTokenizer(foreignClob, ";");
                while (foreigns.hasMoreTokens()) {
                    String tk = foreigns.nextToken();
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
                ps2 = con.prepareStatement("INSERT INTO mailobject VALUES (?, ?)");
                ps2.setLong(1, getId().getId());
                ps2.setString(2, getFullEmail());
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

    public MailAlias(int type, Collection values) throws Exception {
        super(type, values);
        this.subscribers = new ArrayList();
        Iterator i = values.iterator();
        this.local = ((String) i.next()).toLowerCase();
        this.foreign = (String) i.next();
        if (!C0004CP.isIrisEnabled()) {
            StringTokenizer tokenizer = new StringTokenizer(this.foreign, ";");
            while (tokenizer.hasMoreTokens()) {
                String email = tokenizer.nextToken();
                this.subscribers.add(email);
            }
            return;
        }
        this.description = (String) i.next();
        this.subscribers.add(this.foreign);
    }

    public String getLocal() {
        return this.local;
    }

    public String getForeign() {
        return this.foreign;
    }

    @Override // psoft.hsphere.Resource
    protected Object[] getDescriptionParams() throws Exception {
        return new Object[]{getFullEmail()};
    }

    protected String getFullEmail() throws TemplateModelException {
        if (this.fullEmail != null) {
            return this.fullEmail;
        }
        this.fullEmail = this.local + "@" + getParent().get("name");
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
    public void initDone() throws Exception {
        super.initDone();
        if (AntiSpam.DEFAULT_LEVEL_VALUE.equals(this.local)) {
            throw new HSUserException("mailresource.name_disallowed", new String[]{this.local});
        }
        if (C0004CP.isIrisEnabled() && this.local.equals(this.foreign)) {
            throw new HSUserException("autoresponder.mail");
        }
        Collection<ResourceId> aliases = getParent().getChildHolder().getChildrenByName("mailbox_alias");
        synchronized (aliases) {
            for (ResourceId rid : aliases) {
                try {
                } catch (NullPointerException e) {
                }
                if (!rid.equals(getId()) && this.local.equals(rid.get("local").toString())) {
                    System.err.println("ME: " + getId() + ":" + this.local);
                    System.err.println("THEM: " + rid + ":" + rid.get("local").toString());
                    throw new HSUserException("mailalias.exist");
                    break;
                }
            }
        }
        if (C0004CP.isIrisEnabled()) {
            Collection<ResourceId> boxes = new ArrayList();
            boxes.addAll(getParent().getChildHolder().getChildrenByName("mailbox"));
            boxes.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
            for (ResourceId rid2 : boxes) {
                if (!rid2.equals(getId()) && this.local.equals(rid2.get("email").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
            Collection<ResourceId> forwards = new ArrayList(getParent().getChildHolder().getChildrenByName("mail_forward"));
            for (ResourceId rid3 : forwards) {
                if (!rid3.equals(getId()) && this.local.equals(rid3.get("local").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        } else {
            Collection<ResourceId> mobjects = new ArrayList();
            mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
            for (ResourceId rid4 : mobjects) {
                if (!rid4.equals(getId()) && this.local.equals(rid4.get("email").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO mail_aliases (id, email_local, email_foreign) VALUES (?, ?, ?)");
            ps2.setLong(1, getId().getId());
            ps2.setString(2, this.local);
            Session.setClobValue(ps2, 3, this.foreign);
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO mailobject (id, full_email)  VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, getFullEmail());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (C0004CP.isIrisEnabled()) {
                setDescription(this.description);
                MailServices.createForward(getFullEmail(), this.foreign + "@" + getParent().get("name"), false);
            } else {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
            }
            StringTokenizer foreigns = new StringTokenizer(this.foreign, ";");
            this.subscribers.clear();
            while (foreigns.hasMoreTokens()) {
                String tk = foreigns.nextToken();
                if (tk != null && tk.trim().length() > 0) {
                    this.subscribers.add(tk);
                }
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
        if (this.initialized) {
            if (C0004CP.isIrisEnabled()) {
                MailServices.removeForward(getFullEmail(), this.foreign + "@" + getParent().get("name"), false);
            } else if (!getParent().get().isDeletePrev()) {
                ((MailDomain) getParent().get()).setConfig(this.local, getId().toString());
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM mail_aliases WHERE id = ?");
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

    public TemplateModel FM_changeDescription(String description) {
        try {
            setDescription(description);
            return this;
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("local".equals(key)) {
            return new TemplateString(this.local);
        }
        if (!"fulllocal".equals(key) && !"fullemail".equals(key)) {
            return "foreign".equals(key) ? new TemplateString(this.foreign) : "subscribers".equals(key) ? new TemplateList(this.subscribers) : "subs_cnt".equals(key) ? new TemplateString(this.subscribers.size()) : super.get(key);
        }
        return new TemplateString(getFullEmail());
    }

    @Override // psoft.hsphere.ComparableResource
    public String comparableString() throws Exception {
        return getFullEmail();
    }

    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public TemplateModel FM_changeForeign(String newForeign) throws Exception {
        StringTokenizer foreigns = new StringTokenizer(newForeign, ";");
        if (foreigns.countTokens() == 0) {
            throw new HSUserException("mailalias.target");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_aliases SET email_foreign = ? WHERE id = ?");
            ps.setString(1, newForeign);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.foreign = newForeign;
            this.subscribers.clear();
            while (foreigns.hasMoreTokens()) {
                String tk = foreigns.nextToken();
                this.subscribers.add(tk);
            }
            if (!C0004CP.isIrisEnabled()) {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
            }
            return new TemplateOKResult();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_existSubscriber(String mbox_email) throws Exception {
        String exists = "0";
        if (existSubscriber(mbox_email)) {
            exists = "1";
        }
        return new TemplateString(exists);
    }

    public boolean existSubscriber(String mbox_email) throws Exception {
        for (int i = 0; i < this.subscribers.size(); i++) {
            if (this.subscribers.get(i).toString().trim().equalsIgnoreCase(mbox_email.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox_alias.refund", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mailbox_alias.setup", new Object[]{_getFullEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailbox_alias.recurrent", new Object[]{getPeriodInWords(), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mailbox_alias.refundall", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    public TemplateModel FM_changeOneForeign(String operation, String email) throws Exception {
        String str;
        if (!operation.equalsIgnoreCase("add") && !operation.equalsIgnoreCase("del")) {
            throw new Exception("Wrong operation parameter. It should be 'add' or 'del'");
        }
        if (operation.equalsIgnoreCase("del")) {
            if (this.subscribers.contains(email)) {
                if (this.subscribers.size() == 1) {
                    throw new Exception("MailAlias should contain target mailbox(es)");
                }
                this.subscribers.remove(email);
            }
        } else {
            this.subscribers.add(email);
        }
        String newForeigns = "";
        for (int i = 0; i < this.subscribers.size(); i++) {
            if (newForeigns.equalsIgnoreCase("")) {
                str = (String) this.subscribers.get(i);
            } else {
                str = newForeigns + ";" + ((String) this.subscribers.get(i));
            }
            newForeigns = str;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_aliases SET email_foreign = ? WHERE id = ?");
            ps.setString(1, newForeigns);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.foreign = newForeigns;
            if (!C0004CP.isIrisEnabled()) {
                ((MailDomain) getParent().get()).setConfig(this.local, "");
            }
            return new TemplateOKResult();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
