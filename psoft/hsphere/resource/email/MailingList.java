package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ComparableResource;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.report.DataComparator;
import psoft.hsphere.report.GenericMailReport;
import psoft.hsphere.report.adv.MailReport;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.YafvExecParams;
import psoft.util.freemarker.YafvExecResult;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailingList.class */
public class MailingList extends Resource implements HostDependentResource, ComparableResource {
    protected String email;
    protected boolean trailer;
    protected boolean archiveWebAccess;
    private String description;
    public static final long TIME_TO_LIVE = 300000;
    protected long lastTimeLoaded;
    protected Collection cachedSubscribers;
    protected String fullEmail;
    public static final int ML_SUBSCRIBE = 0;
    public static final int ML_UNSUBSCRIBE = 0;

    public MailingList(ResourceId id) throws Exception {
        super(id);
        this.trailer = false;
        this.archiveWebAccess = false;
        this.lastTimeLoaded = 0L;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT email, trailer, archive_web_access  FROM mailing_lists WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.email = rs.getString(1);
                this.trailer = "1".equals(rs.getString(2));
                this.archiveWebAccess = "1".equals(rs.getString(3));
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
            this.description = getDescription(getId());
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public MailingList(int type, Collection values) throws Exception {
        super(type, values);
        this.trailer = false;
        this.archiveWebAccess = false;
        this.lastTimeLoaded = 0L;
        Iterator i = values.iterator();
        this.email = ((String) i.next()).toLowerCase();
        this.description = (String) i.next();
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (AntiSpam.DEFAULT_LEVEL_VALUE.equals(this.email)) {
            throw new HSUserException("mailresource.name_disallowed", new String[]{this.email});
        }
        Collection<ResourceId> mobjects = new ArrayList();
        mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailbox"));
        mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
        for (ResourceId rid : mobjects) {
            if (!rid.equals(getId()) && this.email.equals(rid.get("email").toString())) {
                throw new HSUserException("maillist.exist");
                break;
            }
        }
        if (C0004CP.isIrisEnabled()) {
            Collection<ResourceId> alias_forw = new ArrayList();
            alias_forw.addAll(getParent().getChildHolder().getChildrenByName("mailbox_alias"));
            alias_forw.addAll(getParent().getChildHolder().getChildrenByName("mail_forward"));
            for (ResourceId rid2 : alias_forw) {
                if (!rid2.equals(getId()) && this.email.equals(rid2.get("local").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        } else {
            mobjects.clear();
            mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailbox_alias"));
            mobjects.addAll(getParent().getChildHolder().getChildrenByName("mail_forward"));
            mobjects.addAll(getParent().getChildHolder().getChildrenByName("responder"));
            for (ResourceId rid3 : mobjects) {
                if (!rid3.equals(getId()) && this.email.equals(rid3.get("local").toString())) {
                    throw new HSUserException("mailobject.exist");
                    break;
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO mailing_lists (id, email) VALUES (?, ?)");
            ps2.setLong(1, getId().getId());
            ps2.setString(2, this.email);
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO mailobject (id, full_email)  VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, getFullEmail());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            setDescription(this.description);
            MailServices.createMailingList(recursiveGet("mail_server"), getFullEmail(), this.description);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected String getDomainName() throws TemplateModelException {
        return getParent().get("name").toString();
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

    @Override // psoft.hsphere.ComparableResource
    public String comparableString() throws Exception {
        return getFullEmail();
    }

    protected String _getFullEmail() {
        try {
            return getFullEmail();
        } catch (Exception e) {
            return this.email;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized && !getParent().get().isDeletePrev()) {
            MailServices.deleteMailingList(recursiveGet("mail_server"), getFullEmail());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM mailing_lists WHERE id = ?");
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

    protected Collection list() throws Exception {
        long now = TimeUtils.currentTimeMillis();
        if (this.cachedSubscribers != null && now - this.lastTimeLoaded < 300000) {
            return this.cachedSubscribers;
        }
        this.cachedSubscribers = list(false);
        this.lastTimeLoaded = now;
        return this.cachedSubscribers;
    }

    protected Collection list(boolean isModerate) throws Exception {
        return MailServices.listMailingList(recursiveGet("mail_server"), getFullEmail(), isModerate);
    }

    public TemplateModel FM_list() throws Exception {
        return new TemplateList(list());
    }

    public TemplateModel FM_getReport(boolean regenerate) throws Exception {
        MailReport report;
        if (!regenerate && (report = MailReport.getMailReport(getId().getId())) != null) {
            return report;
        }
        Vector v = new Vector();
        int j = 1;
        for (Object obj : list()) {
            HashMap map = new HashMap();
            int i = j;
            j++;
            map.put("id", new Integer(i));
            map.put("email", obj);
            v.add(map);
        }
        MailReport report2 = new GenericMailReport(getId().getId(), v, new DataComparator());
        report2.setOrderParams("id", true);
        return report2;
    }

    public TemplateModel FM_listMod() throws Exception {
        return new TemplateList(list(true));
    }

    public TemplateModel FM_subscribe(String email) throws Exception {
        accessCheck(0);
        subscribe(email);
        return this;
    }

    public TemplateModel FM_subscribeMod(String email) throws Exception {
        accessCheck(0);
        subscribe(email, true);
        return this;
    }

    public TemplateModel FM_batchSubscribe(String emails) throws Exception {
        accessCheck(0);
        LineNumberReader in = new LineNumberReader(new StringReader(emails));
        while (true) {
            String email = in.readLine();
            if (email != null) {
                YafvExecParams params = new YafvExecParams("Email format", email);
                YafvExecResult val = Session.yafvExec("common.s_email.vEmail", params);
                if (!val.isStatusOK()) {
                    throw new HSUserException("mailinglist.validation", new Object[]{email, val.getMessage()});
                }
                subscribe(email);
            } else {
                return this;
            }
        }
    }

    public void subscribe(String email) throws Exception {
        subscribe(email, false);
        this.cachedSubscribers = null;
    }

    public void subscribe(String email, boolean isModerate) throws Exception {
        MailServices.subscribeToMailingList(recursiveGet("mail_server"), getFullEmail(), email, isModerate);
    }

    public TemplateModel FM_unsubscribe(String email) throws Exception {
        accessCheck(0);
        unsubscribe(email);
        return this;
    }

    public TemplateModel FM_unsubscribeMod(String email) throws Exception {
        accessCheck(0);
        unsubscribe(email, true);
        return this;
    }

    public void unsubscribe(String email) throws Exception {
        unsubscribe(email, false);
        this.cachedSubscribers = null;
    }

    public void unsubscribe(String email, boolean isModerate) throws Exception {
        MailServices.unsubscribeFromMailingList(recursiveGet("mail_server"), getFullEmail(), email, isModerate);
    }

    public TemplateModel FM_unsubscribeAll() throws Exception {
        accessCheck(0);
        unsubscribeAll();
        return this;
    }

    public void unsubscribeAll() throws Exception {
        MailServices.unsubscribeAllFromMailingList(recursiveGet("mail_server"), getFullEmail());
        this.cachedSubscribers = null;
    }

    public TemplateModel FM_update(String flag, String owner, String newArchiveWebAccess, String newDescription) throws Exception {
        boolean newArchiveWebAccessBoolean = "1".equals(newArchiveWebAccess);
        MailServices.updateMailingList(recursiveGet("mail_server"), getFullEmail(), flag, owner, newArchiveWebAccessBoolean, newDescription);
        if (newArchiveWebAccessBoolean != this.archiveWebAccess) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE mailing_lists SET archive_web_access=? WHERE id=?");
                ps.setString(1, newArchiveWebAccessBoolean ? "1" : "0");
                ps.setLong(2, getId().getId());
                ps.executeUpdate();
                this.archiveWebAccess = newArchiveWebAccessBoolean;
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        setDescription(newDescription);
        this.description = newDescription;
        return this;
    }

    public TemplateModel FM_stat() throws Exception {
        Iterator i = MailServices.statMailingList(recursiveGet("mail_server"), getFullEmail()).iterator();
        TemplateHash stat = new TemplateHash();
        stat.put("flag", i.next());
        String owner = (String) i.next();
        if ("false".equals(owner)) {
            owner = "";
        }
        stat.put("owner", owner);
        return stat;
    }

    public TemplateModel FM_changeDescription(String description) {
        try {
            setDescription(description);
            return this;
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    public TemplateModel FM_put_trailer(String trailerMsg) {
        try {
            MailServices.putMailListTrailer(recursiveGet("mail_server"), getDomainName(), this.email, trailerMsg);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE mailing_lists SET trailer=? WHERE id=?");
            ps.setString(1, "1");
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.trailer = true;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    public TemplateModel FM_get_trailer() {
        try {
            return new TemplateString(MailServices.getMailListTrailer(recursiveGet("mail_server"), getDomainName(), this.email));
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    public TemplateModel FM_del_trailer() {
        try {
            MailServices.delMailListTrailer(recursiveGet("mail_server"), getDomainName(), this.email);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE mailing_lists SET trailer=? WHERE id=?");
            ps.setString(1, "0");
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.trailer = false;
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    public TemplateModel FM_getIrisMessages() {
        try {
            return new TemplateMap(MailServices.getIrisMailListMessages(getDomainName(), this.email));
        } catch (Exception e) {
            return new TemplateErrorResult(e);
        }
    }

    public TemplateModel FM_updateIrisMessage(String mailDomain, String localEmail, String text_name, String text_value, String mode) throws Exception {
        MailServices.updateIrisMailListMessage(mailDomain, localEmail, text_name, text_value, mode);
        return this;
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("email".equals(key)) {
            return new TemplateString(this.email);
        }
        if ("fullemail".equals(key)) {
            return new TemplateString(getFullEmail());
        }
        if ("trailer".equals(key)) {
            return new TemplateString(this.trailer ? "1" : "");
        } else if ("archive_web_access".equals(key)) {
            return new TemplateString(this.archiveWebAccess ? "1" : "");
        } else {
            return "description".equals(key) ? new TemplateString(this.description) : super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        MailServices.createMailingList(HostManager.getHost(targetHostId), getFullEmail(), this.description);
        Collection<String> moderators = list(true);
        Collection<String> subscribers = list(false);
        for (String email : moderators) {
            MailServices.subscribeToMailingList(HostManager.getHost(targetHostId), getFullEmail(), email, true);
        }
        for (String email2 : subscribers) {
            MailServices.subscribeToMailingList(HostManager.getHost(targetHostId), getFullEmail(), email2, false);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        MailServices.deleteMailingList(HostManager.getHost(targetHostId), getFullEmail());
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
        return Localizer.translateMessage("bill.mailing_list.refund", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.mailing_list.setup", new Object[]{_getFullEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.mailing_list.recurrent", new Object[]{getPeriodInWords(), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.mailing_list.refundall", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    public String getEmail() {
        return this.email;
    }

    public boolean isTrailer() {
        return this.trailer;
    }

    public TemplateString FM_existArchive() throws Exception {
        String result = "0";
        HostEntry he = recursiveGet("mail_server");
        String[] cmd = {getParent().get("name").toString(), this.email};
        Iterator output = he.exec("mlist-contain-archive", cmd).iterator();
        if (output.hasNext()) {
            result = output.next().toString();
        }
        return new TemplateString(result);
    }
}
