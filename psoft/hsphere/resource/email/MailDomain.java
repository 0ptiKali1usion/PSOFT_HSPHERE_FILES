package psoft.hsphere.resource.email;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.billing.estimators.EstimateCreateMass;
import psoft.hsphere.billing.estimators.EstimateDeleteMass;
import psoft.hsphere.report.adv.MailReport;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/MailDomain.class */
public class MailDomain extends Resource implements HostDependentResource {
    public static String postmaster = "postmaster";
    protected String password;
    protected String catchAll;
    private int postmasterDiscardMail;
    private HashMap spamPreferences;
    private boolean isSpamPrefsLoaded;
    private HashMap virusPreferences;
    private boolean isVirusPrefsLoaded;
    protected LinkedList aliasedBy;
    protected static final long TIME_TO_LIVE = 10000;
    protected long aliasedByLastTimeLoaded;

    public MailDomain(ResourceId id) throws Exception {
        super(id);
        this.postmasterDiscardMail = 0;
        this.spamPreferences = new HashMap();
        this.isSpamPrefsLoaded = false;
        this.virusPreferences = new HashMap();
        this.isVirusPrefsLoaded = false;
        this.aliasedBy = new LinkedList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT password, catch_all FROM mail_domain WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                this.password = rs.getString(1);
                this.catchAll = rs.getString(2);
            } else {
                notFound();
            }
            ps2.close();
            ps = con.prepareStatement("SELECT discard_mail FROM mailboxes WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs2 = ps.executeQuery();
            if (rs2.next()) {
                this.postmasterDiscardMail = rs2.getInt(1);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public MailDomain(int type, Collection values) throws Exception {
        super(type, values);
        this.postmasterDiscardMail = 0;
        this.spamPreferences = new HashMap();
        this.isSpamPrefsLoaded = false;
        this.virusPreferences = new HashMap();
        this.isVirusPrefsLoaded = false;
        this.aliasedBy = new LinkedList();
        this.password = (String) values.iterator().next();
        this.catchAll = "";
        this.postmasterDiscardMail = 0;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO mail_domain (id, password, catch_all)  VALUES (?, ?, '')");
            ps2.setLong(1, getId().getId());
            ps2.setString(2, this.password);
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO mailboxes (id, email, full_email, password, discard_mail)  VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, postmaster);
            ps.setString(3, getPostmaster());
            ps.setString(4, this.password);
            ps.setInt(5, this.postmasterDiscardMail);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            MailServices.createMailDomain(recursiveGet("mail_server"), getDomainName(), this.password);
            if (C0004CP.isIrisEnabled()) {
                MailServices.createMailbox(recursiveGet("mail_server"), getPostmaster(), this.password, 0);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getDomainName() throws Exception {
        return recursiveGet("name").toString();
    }

    public String getPostmaster() throws Exception {
        return postmaster + "@" + getDomainName();
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("password".equals(key)) {
            return new TemplateString(this.password);
        }
        if ("catchAll".equals(key)) {
            return new TemplateString(this.catchAll);
        }
        try {
            if ("fullemail".equals(key)) {
                return new TemplateString(getPostmaster());
            }
            if ("name".equals(key)) {
                return new TemplateString(getDomainName());
            }
            if ("discard_incomm_mail".equals(key)) {
                if (this.postmasterDiscardMail == 1) {
                    return new TemplateString("enabled");
                }
                return new TemplateString("disabled");
            } else if ("whiteList".equals(key)) {
                if (this.spamPreferences.get("whiteList") == null) {
                    try {
                        loadAntiSpamPreferences(getId().getId());
                    } catch (Exception e) {
                        Session.getLog().debug("Can not get antispam preference whitelist:", e);
                        throw new TemplateModelException(e.getMessage());
                    }
                }
                return new TemplateString(this.spamPreferences.get("whiteList"));
            } else if ("blackList".equals(key)) {
                if (this.spamPreferences.get("blackList") == null) {
                    try {
                        loadAntiSpamPreferences(getId().getId());
                    } catch (Exception e2) {
                        Session.getLog().debug("Can not get antispam preference blacklist:", e2);
                        throw new TemplateModelException(e2.getMessage());
                    }
                }
                return new TemplateString(this.spamPreferences.get("blackList"));
            } else if ("spamLevel".equals(key)) {
                if (this.spamPreferences.get("spamLevel") == null) {
                    try {
                        loadAntiSpamPreferences(getId().getId());
                    } catch (Exception e3) {
                        Session.getLog().debug("Can not get antispam preference level", e3);
                        throw new TemplateModelException(e3.getMessage());
                    }
                }
                return new TemplateString(this.spamPreferences.get("spamLevel"));
            } else if ("spamProcessing".equals(key)) {
                if (this.spamPreferences.get("spamProcessing") == null) {
                    try {
                        loadAntiSpamPreferences(getId().getId());
                    } catch (Exception e4) {
                        Session.getLog().debug("Can not get spam processing preference", e4);
                        throw new TemplateModelException(e4.getMessage());
                    }
                }
                return new TemplateString(this.spamPreferences.get("spamProcessing"));
            } else if ("spamMaxScore".equals(key)) {
                if (this.spamPreferences.get("spamMaxScore") == null) {
                    try {
                        loadAntiSpamPreferences(getId().getId());
                    } catch (Exception e5) {
                        Session.getLog().debug("Can not get spam MaxScore preference", e5);
                        throw new TemplateModelException(e5.getMessage());
                    }
                }
                return new TemplateString(this.spamPreferences.get("spamMaxScore"));
            } else if ("virusProcessing".equals(key)) {
                if (this.virusPreferences.get("virusProcessing") == null) {
                    try {
                        loadAntiVirusPreferences(getId().getId());
                    } catch (Exception e6) {
                        Session.getLog().debug("Can not get virus processing preference", e6);
                        throw new TemplateModelException(e6.getMessage());
                    }
                }
                return new TemplateString(this.virusPreferences.get("virusProcessing"));
            } else if ("list_aliased_by".equals(key)) {
                try {
                    return new TemplateList(getAliasedBy());
                } catch (Exception e7) {
                    Session.getLog().debug("Can not get aliased_by values", e7);
                    throw new TemplateModelException(e7.getMessage());
                }
            } else {
                return super.get(key);
            }
        } catch (Exception e8) {
            Session.getLog().debug("Can not get domain name:", e8);
            throw new TemplateModelException(e8.getMessage());
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
            MailServices.deleteMailDomain(recursiveGet("mail_server"), getDomainName());
            if (C0004CP.isIrisEnabled()) {
                if (postmaster.equals(this.catchAll)) {
                    MailServices.deleteMailbox(recursiveGet("mail_server"), getPostmaster(), true);
                } else {
                    MailServices.deleteMailbox(recursiveGet("mail_server"), getPostmaster(), false);
                }
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM mail_domain WHERE id = ?");
            ps2.setLong(1, getId().getId());
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("DELETE FROM mailboxes WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            if (Session.getAccount().getPlan().isResourceAvailable("antispam") != null) {
                ps.close();
                ps = con.prepareStatement("DELETE FROM mail_preferences WHERE mobject_id = ?");
                ps.setLong(1, getId().getId());
                ps.executeUpdate();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_updatePassword(String password) throws Exception {
        changePassword(password);
        return this;
    }

    protected void changePassword(String password) throws Exception {
        accessCheck(0);
        boolean isCatchAll = false;
        if (postmaster.equals(this.catchAll)) {
            isCatchAll = true;
        }
        MailServices.setMboxPassword(recursiveGet("mail_server"), getPostmaster(), password, isCatchAll);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_domain set password = ? WHERE id = ?");
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

    public ResourceId getBoxByName(String name) throws Exception {
        Collection<ResourceId> boxes = getChildHolder().getChildrenByName("mailbox");
        synchronized (boxes) {
            for (ResourceId rid : boxes) {
                if (name.equals(rid.get("email").toString())) {
                    return rid;
                }
            }
            return null;
        }
    }

    public ResourceId getMailAliasByName(String name) throws Exception {
        Collection<ResourceId> boxes = getChildHolder().getChildrenByName("mailbox_alias");
        synchronized (boxes) {
            for (ResourceId rid : boxes) {
                if (name.equals(rid.get("local").toString())) {
                    return rid;
                }
            }
            return null;
        }
    }

    public TemplateModel FM_getMailAliasByName(String name) throws Exception {
        ResourceId rid = getMailAliasByName(name);
        if (rid == null) {
            return new TemplateString("");
        }
        return new TemplateString(rid.toString());
    }

    public TemplateModel FM_setCatchAll(String email) throws Exception {
        accessCheck(0);
        setCatchAll(email);
        return this;
    }

    protected void setCatchAll(String email) throws Exception {
        if (!this.catchAll.equals("")) {
            MailServices.unsetDefaultMailbox(recursiveGet("mail_server"), getDomainName(), this.catchAll);
        }
        MailServices.setDefaultMailbox(recursiveGet("mail_server"), email + "@" + getDomainName());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_domain set catch_all = ? WHERE id = ?");
            ps.setString(1, email);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.catchAll = email;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_disableCatchAll() throws Exception {
        accessCheck(0);
        disableCatchAll();
        return this;
    }

    protected void disableCatchAll() throws Exception {
        MailServices.unsetDefaultMailbox(recursiveGet("mail_server"), getDomainName(), this.catchAll);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_domain set catch_all = '' WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.catchAll = "";
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        super.suspend();
        MailServices.suspendMailDomain(recursiveGet("mail_server"), getDomainName(), false);
        if (!C0004CP.isIrisEnabled() && postmaster.equals(this.catchAll)) {
            MailServices.suspendMailDomain(recursiveGet("mail_server"), getPostmaster(), true);
        } else {
            MailServices.suspendMailDomain(recursiveGet("mail_server"), getPostmaster(), false);
        }
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (!this.suspended) {
            return;
        }
        super.resume();
        MailServices.resumeMailDomain(recursiveGet("mail_server"), getDomainName(), false);
        if (!C0004CP.isIrisEnabled() && postmaster.equals(this.catchAll)) {
            MailServices.resumeMailDomain(recursiveGet("mail_server"), getPostmaster(), true);
        } else {
            MailServices.resumeMailDomain(recursiveGet("mail_server"), getPostmaster(), false);
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long newHostId) throws Exception {
        try {
            MailServices.createMailDomain(HostManager.getHost(newHostId), getDomainName(), this.password);
        } catch (Exception e) {
            if (e.getMessage().indexOf("Domain already exists") == -1) {
                throw e;
            }
        }
        Hashtable mo = new Hashtable();
        Collection<ResourceId> col = getChildHolder().getChildrenByName("mailbox");
        for (ResourceId rid : col) {
            Mailbox m = (Mailbox) rid.get();
            try {
                m.realPhysicalCreate(newHostId);
            } catch (Exception e2) {
                if (e2.getMessage().indexOf("Username exists") == -1) {
                    throw e2;
                }
            }
            if (m.getDiscardMail() == 1 || m.getEmail().equalsIgnoreCase(this.catchAll)) {
                mo.put(m.getEmail(), "");
            }
        }
        Collection<ResourceId> col1 = getChildHolder().getChildrenByName("mail_forward");
        for (ResourceId rid2 : col1) {
            mo.put(((MailForward) rid2.get()).getLocal(), "");
        }
        Collection<ResourceId> col2 = getChildHolder().getChildrenByName("mailbox_alias");
        for (ResourceId rid3 : col2) {
            mo.put(((MailAlias) rid3.get()).getLocal(), "");
        }
        Collection<ResourceId> col3 = getChildHolder().getChildrenByName("responder");
        for (ResourceId rid4 : col3) {
            mo.put(((Autoresponder) rid4.get()).getLocal(), "");
        }
        Enumeration e3 = mo.keys();
        while (e3.hasMoreElements()) {
            setConfig((String) e3.nextElement(), "");
        }
        if (this.catchAll != null && !this.catchAll.equalsIgnoreCase("")) {
            MailServices.setDefaultMailbox(recursiveGet("mail_server"), this.catchAll + "@" + getDomainName());
        }
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long newHostId) throws Exception {
        MailServices.deleteMailDomain(HostManager.getHost(newHostId), getDomainName());
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return recursiveGet("mail_server").getId();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    public void setConfig(String mailobject, String exceptId) throws Exception {
        HostEntry he = recursiveGet("mail_server");
        String mdomainPath = getDomainPath();
        SimpleHash root = new SimpleHash();
        root.put("maildomain", this);
        root.put("mailobject", mailobject);
        root.put("except_id", exceptId);
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Session.getTemplate("/email/mdomain.config").process(root, out);
        out.close();
        synchronized (he) {
            ArrayList l = new ArrayList();
            String fixname = mailobject.replace('.', ':');
            l.add(mdomainPath + "/.qmail-" + fixname);
            he.exec("mbox-saveconf", l, sw.toString());
        }
    }

    public String getDomainPath() throws Exception {
        HostEntry he = recursiveGet("mail_server");
        List list = new ArrayList();
        list.add(getDomainName());
        Collection result = he.exec("mail-getdomain", list);
        if (result.size() > 0) {
            return (String) result.iterator().next();
        }
        throw new HSUserException("maildomain.path_error", new Object[]{getDomainName()});
    }

    public String getMailBoxPath(String mailbox) throws Exception {
        HostEntry he = recursiveGet("mail_server");
        ArrayList list = new ArrayList();
        list.add(MailServices.shellQuote(mailbox));
        Collection result = he.exec("mailbox-get-path", list);
        if (result.size() > 0) {
            return (String) result.iterator().next();
        }
        throw new HSUserException("mailbox.path_error", new Object[]{mailbox});
    }

    public TemplateList FM_getMailDomainObjects(String mailobject, String exceptId) throws Exception {
        return new TemplateList(getMailDomainObjects(mailobject, exceptId));
    }

    public ArrayList getMailDomainObjects(String mailobject, String exceptId) throws Exception {
        ArrayList mailList = new ArrayList();
        String mdomainPath = getDomainPath();
        String mailobjectFull = mailobject + "@" + getDomainName();
        int notCopyToBox = 0;
        if (postmaster.equalsIgnoreCase(mailobject)) {
            if (isDiscard()) {
                HashMap hmMailObject = new HashMap();
                hmMailObject.put("name", "discard");
                mailList.add(hmMailObject);
                notCopyToBox = 1;
            }
        } else {
            Collection<ResourceId> col0 = getChildHolder().getChildrenByName("mailbox");
            for (ResourceId rid : col0) {
                try {
                    if (!exceptId.equals(rid.toString()) && mailobject.equals(((Mailbox) rid.get()).email) && ((Mailbox) rid.get()).isDiscard()) {
                        HashMap hmMailObject2 = new HashMap();
                        hmMailObject2.put("name", "discard");
                        mailList.add(hmMailObject2);
                        notCopyToBox = 1;
                    }
                } catch (Exception ex) {
                    Ticket.create(ex, this, new String("Can't get mailbox with id=" + rid));
                }
            }
        }
        Collection<ResourceId> col = getChildHolder().getChildrenByName("mail_forward");
        for (ResourceId rid2 : col) {
            try {
                if (!exceptId.equals(rid2.toString())) {
                    String local = ((MailForward) rid2.get()).local;
                    if (mailobject.equals(local)) {
                        List<String> l = ((MailForward) rid2.get()).subscribers;
                        for (String foreign : l) {
                            if (foreign != null) {
                                HashMap hmMailObject3 = new HashMap();
                                hmMailObject3.put("name", "forwards");
                                hmMailObject3.put("email_foreign", "&" + foreign);
                                if ((postmaster.equalsIgnoreCase(local) || existChildMailBox(mailobject, exceptId)) && notCopyToBox == 0) {
                                    hmMailObject3.put("maildir", formDelivermailCommand(mailobjectFull));
                                    notCopyToBox = 1;
                                }
                                mailList.add(hmMailObject3);
                            }
                        }
                    }
                }
            } catch (Exception ex2) {
                Ticket.create(ex2, this, new String("Can't get mailforward with id=" + rid2));
            }
        }
        Collection<ResourceId> col1 = getChildHolder().getChildrenByName("mailbox_alias");
        for (ResourceId rid3 : col1) {
            try {
                if (!exceptId.equals(rid3.toString())) {
                    String local2 = ((MailAlias) rid3.get()).local;
                    if (mailobject.equals(local2)) {
                        if ((postmaster.equalsIgnoreCase(local2) || existChildMailBox(mailobject, exceptId)) && notCopyToBox == 0) {
                            HashMap hmMailObject4 = new HashMap();
                            hmMailObject4.put("name", "alias");
                            hmMailObject4.put("maildir", formDelivermailCommand(mailobjectFull));
                            mailList.add(hmMailObject4);
                            notCopyToBox = 1;
                        }
                        List<String> l2 = ((MailAlias) rid3.get()).subscribers;
                        for (String aliasTo : l2) {
                            if (aliasTo != null) {
                                HashMap hmMailObject5 = new HashMap();
                                hmMailObject5.put("name", "alias");
                                hmMailObject5.put("maildir", formDelivermailCommand(aliasTo + "@" + getDomainName()));
                                mailList.add(hmMailObject5);
                            }
                        }
                    }
                }
            } catch (Exception ex3) {
                Ticket.create(ex3, this, new String("Can't get mailalias with id=" + rid3));
            }
        }
        Collection<ResourceId> col2 = getChildHolder().getChildrenByName("responder");
        for (ResourceId rid4 : col2) {
            if (!exceptId.equals(rid4.toString())) {
                try {
                    Autoresponder resp = (Autoresponder) rid4.get();
                    if (mailobject.equals(resp.getLocal())) {
                        HashMap hmMailObject6 = new HashMap();
                        hmMailObject6.put("name", "autoresponder");
                        hmMailObject6.put("command", "|/hsphere/shared/bin/autorespond 300 2 " + mdomainPath + "/@" + mailobject + "/message " + mdomainPath + "/@" + mailobject + " " + resp.getIncludeIncoming());
                        if (notCopyToBox == 0 && (postmaster.equalsIgnoreCase(mailobject) || existChildMailBox(mailobject, exceptId))) {
                            hmMailObject6.put("maildir", formDelivermailCommand(mailobjectFull));
                        }
                        String foreign2 = ((Autoresponder) rid4.get()).foreign;
                        if (foreign2 != null) {
                            hmMailObject6.put("email_foreign", "&" + foreign2);
                        }
                        mailList.add(hmMailObject6);
                    }
                } catch (Exception ex4) {
                    Ticket.create(ex4, this, new String("Can't get mail autoresponder with id=" + rid4));
                }
            }
        }
        return mailList;
    }

    private String formDelivermailCommand(String mailObject) {
        return "| /hsphere/local/var/vpopmail/bin/vdelivermail '' " + mailObject + " || exit 0";
    }

    public boolean existChildMailBox(String name, String exceptId) throws Exception {
        Collection<ResourceId> col = getChildHolder().getChildrenByName("mailbox");
        for (ResourceId rid : col) {
            try {
                if (!exceptId.equals(rid.toString()) && name.equals(((Mailbox) rid.get()).email)) {
                    return true;
                }
            } catch (Exception ex) {
                Ticket.create(ex, this, new String("Can't get name of the mailbox with id=" + rid));
            }
        }
        return false;
    }

    public boolean existChildAutoresponder(String name, String exceptId) throws Exception {
        Collection<ResourceId> col = getChildHolder().getChildrenByName("responder");
        for (ResourceId rid : col) {
            try {
                if (!exceptId.equals(rid.toString()) && name.equals(((Autoresponder) rid.get()).local)) {
                    return true;
                }
            } catch (Exception ex) {
                Ticket.create(ex, this, new String("Can't get name of the autoresponder with id=" + rid));
            }
        }
        return false;
    }

    public boolean existChildMailForward(String name, String exceptId) throws Exception {
        Collection<ResourceId> col = getChildHolder().getChildrenByName("mail_forward");
        for (ResourceId rid : col) {
            try {
                if (!exceptId.equals(rid.toString()) && name.equals(((MailForward) rid.get()).local)) {
                    return true;
                }
            } catch (Exception ex) {
                Ticket.create(ex, this, new String("Can't get name of the mailforward with id=" + rid));
            }
        }
        return false;
    }

    public boolean existChildMailAlias(String name) throws Exception {
        Collection<ResourceId> col = getChildHolder().getChildrenByName("mailbox_alias");
        for (ResourceId rid : col) {
            try {
            } catch (Exception ex) {
                Ticket.create(ex, this, new String("Can't get name of the mailalias with id=" + rid));
            }
            if (name.equalsIgnoreCase(((MailAlias) rid.get()).local)) {
                return true;
            }
        }
        return false;
    }

    public TemplateModel FM_existChildMailAlias(String name) throws Exception {
        if (existChildMailAlias(name)) {
            return new TemplateString("1");
        }
        return new TemplateString("0");
    }

    public String getCatchAll() {
        return this.catchAll;
    }

    public TemplateModel FM_getReport(long moId) throws Exception {
        MailReport report = null;
        if (moId == 0) {
            report = MailReport.getMailReport(getId().getId());
        }
        if (report == null) {
            report = new MailReport();
            int num = 1;
            int numOfNewMailObjectId = 0;
            LinkedList data = new LinkedList();
            HashMap pmap = new HashMap();
            pmap.put("id", getId());
            pmap.put("name", postmaster);
            pmap.put("orderName", "0postmaster");
            pmap.put("mailType", "mailbox");
            pmap.put("orderType", "0");
            data.add(pmap);
            Collection<ResourceId> col0 = getChildHolder().getChildrenByName("mailbox");
            for (ResourceId rid : col0) {
                String name = ((Mailbox) rid.get()).getEmail();
                HashMap map = new HashMap();
                map.put("id", rid.get().getId().getAsString());
                if (moId > 0 && numOfNewMailObjectId == 0) {
                    if (moId == rid.get().getId().getId()) {
                        numOfNewMailObjectId = num;
                    } else {
                        num++;
                    }
                }
                map.put("name", name);
                map.put("orderName", "1" + name + "1");
                map.put("mailType", "mailbox");
                map.put("orderType", "1" + name);
                data.add(map);
            }
            Collection<ResourceId> col = getChildHolder().getChildrenByName("mail_forward");
            for (ResourceId rid2 : col) {
                String name2 = ((MailForward) rid2.get()).getLocal();
                HashMap map2 = new HashMap();
                map2.put("id", rid2.get().getId().getAsString());
                if (moId > 0 && numOfNewMailObjectId == 0) {
                    if (moId == rid2.get().getId().getId()) {
                        numOfNewMailObjectId = num;
                    } else {
                        num++;
                    }
                }
                map2.put("name", name2);
                map2.put("orderName", "1" + name2 + "2");
                map2.put("mailType", "mail_forward");
                map2.put("orderType", "2" + name2);
                data.add(map2);
            }
            Collection<ResourceId> col1 = getChildHolder().getChildrenByName("mailbox_alias");
            for (ResourceId rid3 : col1) {
                String name3 = ((MailAlias) rid3.get()).getLocal();
                HashMap map3 = new HashMap();
                map3.put("id", rid3.get().getId().getAsString());
                if (moId > 0 && numOfNewMailObjectId == 0) {
                    if (moId == rid3.get().getId().getId()) {
                        numOfNewMailObjectId = num;
                    } else {
                        num++;
                    }
                }
                map3.put("name", name3);
                map3.put("orderName", "1" + name3 + "3");
                map3.put("mailType", "mailbox_alias");
                map3.put("orderType", "3" + name3);
                data.add(map3);
            }
            Collection<ResourceId> col2 = getChildHolder().getChildrenByName("responder");
            for (ResourceId rid4 : col2) {
                String name4 = ((Autoresponder) rid4.get()).getLocal();
                HashMap map4 = new HashMap();
                map4.put("id", rid4.get().getId().getAsString());
                if (moId > 0 && numOfNewMailObjectId == 0) {
                    if (moId == rid4.get().getId().getId()) {
                        numOfNewMailObjectId = num;
                    } else {
                        num++;
                    }
                }
                map4.put("name", name4);
                map4.put("orderName", "1" + name4 + "4");
                map4.put("mailType", "responder");
                map4.put("orderType", "4" + name4);
                data.add(map4);
            }
            Collection<ResourceId> col3 = getChildHolder().getChildrenByName("mailing_list");
            for (ResourceId rid5 : col3) {
                String name5 = ((MailingList) rid5.get()).getEmail();
                HashMap map5 = new HashMap();
                map5.put("id", rid5.get().getId().getAsString());
                if (moId > 0 && numOfNewMailObjectId == 0) {
                    if (moId == rid5.get().getId().getId()) {
                        numOfNewMailObjectId = num;
                    } else {
                        num++;
                    }
                }
                map5.put("name", name5);
                map5.put("orderName", "1" + name5 + "5");
                map5.put("mailType", "mailing_list");
                map5.put("orderType", "5" + name5);
                data.add(map5);
            }
            LinkedList initValues = new LinkedList();
            initValues.add(new Long(getId().getId()).toString());
            initValues.add(new Integer(numOfNewMailObjectId).toString());
            initValues.add(data);
            report.init(initValues);
            report.setOrderParams("orderType", true);
        }
        return report;
    }

    public TemplateModel FM_getReportJoined(String moName) throws Exception {
        MailReport report = null;
        if (moName.equalsIgnoreCase("0")) {
            report = MailReport.getMailReport(getId().getId());
        }
        if (report == null) {
            report = new MailReport();
            LinkedList data = new LinkedList();
            HashMap mailObjects = new HashMap();
            HashMap pmap = new HashMap();
            pmap.put("mailbox", getId());
            pmap.put("name", postmaster);
            pmap.put("orderName", "*postmaster");
            pmap.put("mailType", "mail_object");
            pmap.put("orderType", "0");
            mailObjects.put(postmaster, pmap);
            Collection<ResourceId> col0 = getChildHolder().getChildrenByName("mailbox");
            for (ResourceId rid : col0) {
                String name = "";
                try {
                    name = ((Mailbox) rid.get()).getEmail();
                } catch (Exception ex) {
                    Ticket.create(ex, this, new String("Can't get name of the mailbox with id=" + rid));
                }
                if (!name.equalsIgnoreCase("")) {
                    HashMap map = new HashMap();
                    map.put("mailbox", rid.getAsString());
                    map.put("name", name);
                    map.put("orderName", "+" + name);
                    map.put("mailType", "mail_object");
                    map.put("orderType", "0");
                    mailObjects.put(name, map);
                }
            }
            Collection<ResourceId> col01 = getChildHolder().getChildrenByName("antispam");
            for (ResourceId rid2 : col01) {
                String name2 = "";
                try {
                    name2 = ((AntiSpam) rid2.get()).getLocal();
                } catch (Exception ex2) {
                    Ticket.create(ex2, this, new String("Can't get name of the antispam with id=" + rid2));
                }
                if (name2 != null && !name2.equalsIgnoreCase("")) {
                    HashMap map2 = (HashMap) mailObjects.get(name2);
                    if (map2 != null) {
                        mailObjects.remove(name2);
                        map2.put("antispam", rid2.getAsString());
                    } else {
                        map2 = new HashMap();
                        map2.put("antispam", rid2.getAsString());
                        map2.put("name", name2);
                        map2.put("orderName", "+" + name2);
                        map2.put("mailType", "mail_object");
                        map2.put("orderType", "0");
                    }
                    mailObjects.put(name2, map2);
                }
            }
            Collection<ResourceId> col02 = getChildHolder().getChildrenByName("antivirus");
            for (ResourceId rid3 : col02) {
                String name3 = "";
                try {
                    name3 = ((AntiVirus) rid3.get()).getLocal();
                } catch (Exception ex3) {
                    Ticket.create(ex3, this, new String("Can't get name of the antivirus with id=" + rid3));
                }
                if (name3 != null && !name3.equalsIgnoreCase("")) {
                    HashMap map3 = (HashMap) mailObjects.get(name3);
                    if (map3 != null) {
                        mailObjects.remove(name3);
                        map3.put("antivirus", rid3.getAsString());
                    } else {
                        map3 = new HashMap();
                        map3.put("antivirus", rid3.getAsString());
                        map3.put("name", name3);
                        map3.put("orderName", "+" + name3);
                        map3.put("mailType", "mail_object");
                        map3.put("orderType", "0");
                    }
                    mailObjects.put(name3, map3);
                }
            }
            Collection<ResourceId> col = getChildHolder().getChildrenByName("mail_forward");
            for (ResourceId rid4 : col) {
                String name4 = "";
                try {
                    name4 = ((MailForward) rid4.get()).getLocal();
                } catch (Exception ex4) {
                    Ticket.create(ex4, this, new String("Can't get name of the mailforward with id=" + rid4));
                }
                if (!name4.equalsIgnoreCase("")) {
                    HashMap map4 = (HashMap) mailObjects.get(name4);
                    if (map4 != null) {
                        mailObjects.remove(name4);
                        map4.put("forward", rid4.getAsString());
                    } else {
                        map4 = new HashMap();
                        map4.put("forward", rid4.getAsString());
                        map4.put("name", name4);
                        map4.put("orderName", "+" + name4);
                        map4.put("mailType", "mail_object");
                        map4.put("orderType", "0");
                    }
                    mailObjects.put(name4, map4);
                }
            }
            Collection<ResourceId> col1 = getChildHolder().getChildrenByName("mailbox_alias");
            for (ResourceId rid5 : col1) {
                String name5 = "";
                try {
                    name5 = ((MailAlias) rid5.get()).getLocal();
                } catch (Exception ex5) {
                    Ticket.create(ex5, this, new String("Can't get name of the mailalias with id=" + rid5));
                }
                if (!name5.equalsIgnoreCase("")) {
                    HashMap map5 = (HashMap) mailObjects.get(name5);
                    if (map5 != null) {
                        mailObjects.remove(name5);
                        map5.put("alias", rid5.getAsString());
                    } else {
                        map5 = new HashMap();
                        map5.put("alias", rid5.getAsString());
                        map5.put("name", name5);
                        map5.put("orderName", "+" + name5);
                        map5.put("mailType", "mail_object");
                        map5.put("orderType", "0");
                    }
                    mailObjects.put(name5, map5);
                }
            }
            Collection<ResourceId> col2 = getChildHolder().getChildrenByName("responder");
            for (ResourceId rid6 : col2) {
                String name6 = "";
                try {
                    name6 = ((Autoresponder) rid6.get()).getLocal();
                } catch (Exception ex6) {
                    Ticket.create(ex6, this, new String("Can't get name of the autoresponder with id=" + rid6));
                }
                if (!name6.equalsIgnoreCase("")) {
                    HashMap map6 = (HashMap) mailObjects.get(name6);
                    if (map6 != null) {
                        mailObjects.remove(name6);
                        map6.put("responder", rid6.getAsString());
                    } else {
                        map6 = new HashMap();
                        map6.put("responder", rid6.getAsString());
                        map6.put("name", name6);
                        map6.put("orderName", "+" + name6);
                        map6.put("mailType", "mail_object");
                        map6.put("orderType", "0");
                    }
                    mailObjects.put(name6, map6);
                }
            }
            Collection<ResourceId> col3 = getChildHolder().getChildrenByName("mailing_list");
            for (ResourceId rid7 : col3) {
                String name7 = "";
                try {
                    name7 = ((MailingList) rid7.get()).getEmail();
                } catch (Exception ex7) {
                    Ticket.create(ex7, this, new String("Can't get name of the mailing list with id=" + rid7));
                }
                if (!name7.equalsIgnoreCase("")) {
                    HashMap map7 = new HashMap();
                    map7.put("mailing_list", rid7.getAsString());
                    map7.put("name", name7);
                    map7.put("orderName", "+" + name7);
                    map7.put("mailType", "mailing_list");
                    map7.put("orderType", "1");
                    mailObjects.put(name7, map7);
                }
            }
            data.add(mailObjects.get(postmaster));
            mailObjects.remove(postmaster);
            Object[] keys = mailObjects.keySet().toArray();
            Arrays.sort(keys);
            int num = 1;
            int numOfNewMailObjectId = 0;
            for (int i = 0; i < keys.length; i++) {
                data.add(mailObjects.get(keys[i]));
                if (!moName.equalsIgnoreCase("") && numOfNewMailObjectId == 0 && !postmaster.equalsIgnoreCase(moName)) {
                    String name1 = (String) ((HashMap) mailObjects.get(keys[i])).get("name");
                    if (moName.equalsIgnoreCase(name1)) {
                        numOfNewMailObjectId = num;
                    } else {
                        num++;
                    }
                }
            }
            LinkedList initValues = new LinkedList();
            initValues.add(new Long(getId().getId()).toString());
            initValues.add(new Integer(numOfNewMailObjectId).toString());
            initValues.add(data);
            report.init(initValues);
            report.FM_reorder("orderName", true);
        }
        return report;
    }

    public TemplateModel FM_estimateCreateMailObjects(int mailbox, int mailForward, int mailboxAlias, int responder, int antispam, int antivirus, String aliasBy) throws Exception {
        LinkedList ll = new LinkedList();
        if (mailbox > 0) {
            ll.add("mailbox");
            ll.add("");
        }
        if (mailForward > 0) {
            ll.add("mail_forward");
            ll.add("");
        }
        if (mailboxAlias > 0) {
            ll.add("mailbox_alias");
            ll.add("");
        }
        if (responder > 0) {
            ll.add("responder");
            ll.add("");
        }
        if (antispam > 0) {
            ll.add("antispam");
            ll.add("");
        }
        if (antivirus > 0) {
            ll.add("antivirus");
            ll.add("");
        }
        if (!aliasBy.equalsIgnoreCase("")) {
            StringTokenizer st = new StringTokenizer(aliasBy, " ,;");
            while (st.hasMoreTokens()) {
                String alias = st.nextToken();
                if (alias != null && alias.trim().length() > 0) {
                    ll.add("mailbox_alias");
                    ll.add("");
                }
            }
        }
        EstimateCreateMass ec = new EstimateCreateMass(this);
        return ec.estimateCreateMass(ll);
    }

    public TemplateModel FM_estimateDeleteMailObjects(String mailbox, String mailForward, String mailboxAlias, String responder, String antispam, String antivirus, String aliasBy) throws Exception {
        EstimateDeleteMass edm = new EstimateDeleteMass(this);
        List ll = new ArrayList();
        if (mailbox != null && !"".equals(mailbox)) {
            ll.add(mailbox);
            edm.exec(ll);
            ll.clear();
        }
        if (mailForward != null && !"".equals(mailForward)) {
            ll.add(mailForward);
            edm.exec(ll);
            ll.clear();
        }
        if (mailboxAlias != null && !"".equals(mailboxAlias)) {
            ll.add(mailboxAlias);
            edm.exec(ll);
            ll.clear();
        }
        if (responder != null && !"".equals(responder)) {
            ll.add(responder);
            edm.exec(ll);
            ll.clear();
        }
        if (antispam != null && !"".equals(antispam)) {
            ll.add(antispam);
            edm.exec(ll);
            ll.clear();
        }
        if (antivirus != null && !"".equals(antivirus)) {
            ll.add(antivirus);
            edm.exec(ll);
            ll.clear();
        }
        if (aliasBy != null && !"".equals(aliasBy)) {
            StringTokenizer aliases = new StringTokenizer(aliasBy, ",");
            while (aliases.hasMoreTokens()) {
                String aliasName = aliases.nextToken();
                if (aliasName != null && aliasName.trim().length() > 0) {
                    ll.add(getMailAliasByName(aliasName).toString());
                    edm.exec(ll);
                    ll.clear();
                }
            }
        }
        return edm.exec(new ArrayList());
    }

    public HashMap loadAntiSpamPreferences(long mobjectId) throws Exception {
        HashMap newPrefs = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT preference, value FROM mail_preferences WHERE mobject_id = ? AND preference LIKE 'spam_%'");
            ps.setLong(1, mobjectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString(1).equalsIgnoreCase("spam_whitelist_from")) {
                    StringTokenizer whites = new StringTokenizer(Session.getClobValue(rs, 2), ",");
                    String whiteList = "";
                    while (whites.hasMoreTokens()) {
                        String tk = whites.nextToken();
                        if (tk != null && tk.trim().length() > 0) {
                            if (whiteList.equalsIgnoreCase("")) {
                                whiteList = tk;
                            } else {
                                whiteList = whiteList + "\n" + tk;
                            }
                        }
                    }
                    newPrefs.put("whiteList", whiteList);
                } else if (rs.getString(1).equalsIgnoreCase("spam_blacklist_from")) {
                    String blackList = "";
                    StringTokenizer blacks = new StringTokenizer(Session.getClobValue(rs, 2), ",");
                    while (blacks.hasMoreTokens()) {
                        String tk2 = blacks.nextToken();
                        if (tk2 != null && tk2.trim().length() > 0) {
                            if (blackList.equalsIgnoreCase("")) {
                                blackList = tk2;
                            } else {
                                blackList = blackList + "\n" + tk2;
                            }
                        }
                    }
                    newPrefs.put("blackList", blackList);
                } else if (rs.getString(1).equalsIgnoreCase("spam_level")) {
                    newPrefs.put("spamLevel", Session.getClobValue(rs, 2).trim());
                } else if (rs.getString(1).equalsIgnoreCase("spam_processing")) {
                    newPrefs.put("spamProcessing", Session.getClobValue(rs, 2).trim());
                } else if (rs.getString(1).equalsIgnoreCase("spam_max_score")) {
                    newPrefs.put("spamMaxScore", Session.getClobValue(rs, 2).trim());
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (newPrefs.get("spamLevel") == null) {
                newPrefs.put("spamLevel", AntiSpam.DEFAULT_LEVEL_VALUE);
            }
            if (newPrefs.get("spamProcessing") == null) {
                newPrefs.put("spamProcessing", "remove");
            }
            if (newPrefs.get("spamMaxScore") == null) {
                newPrefs.put("spamMaxScore", AntiSpam.DEFAULT_MAXSCORE_VALUE);
            }
            if (mobjectId == getId().getId()) {
                this.isSpamPrefsLoaded = true;
                this.spamPreferences.clear();
                for (String key : newPrefs.keySet()) {
                    this.spamPreferences.put(key, newPrefs.get(key));
                }
            }
            return newPrefs;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public HashMap loadAntiVirusPreferences(long mobjectId) throws Exception {
        HashMap newPrefs = new HashMap();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT preference, value FROM mail_preferences WHERE mobject_id = ? AND preference LIKE 'virus_%'");
            ps.setLong(1, mobjectId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                if (rs.getString(1).equals("virus_processing")) {
                    newPrefs.put("virusProcessing", Session.getClobValue(rs, 2).trim());
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (newPrefs.get("virusProcessing") == null) {
                newPrefs.put("virusProcessing", "remove");
            }
            if (mobjectId == getId().getId()) {
                this.isVirusPrefsLoaded = true;
                this.virusPreferences.clear();
                for (String key : newPrefs.keySet()) {
                    this.virusPreferences.put(key, newPrefs.get(key));
                }
            }
            return newPrefs;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public HashMap getSpamPreferences() throws Exception {
        return this.spamPreferences;
    }

    public HashMap getVirusPreferences() throws Exception {
        return this.virusPreferences;
    }

    public boolean getIsSpamPrefsLoaded() throws Exception {
        return this.isSpamPrefsLoaded;
    }

    public boolean getIsVirusPrefsLoaded() throws Exception {
        return this.isVirusPrefsLoaded;
    }

    public TemplateModel FM_setAntiSpamPreferences(String newWhiteString, String newBlackString, String newSpamLevel, String newSpamProcessing, String newMaxScore) throws Exception {
        setAntiSpamPreferences(newWhiteString, newBlackString, newSpamLevel, newSpamProcessing, newMaxScore);
        return this;
    }

    public void setAntiSpamPreferences(String newWhiteString, String newBlackString, String newSpamLevel, String newSpamProcessing, String newMaxScore) throws Exception {
        HashMap spamPrefs = new HashMap();
        for (String key : this.spamPreferences.keySet()) {
            spamPrefs.put(key, this.spamPreferences.get(key));
        }
        String newWhitelist = "";
        String newBlacklist = "";
        String newWhiteCommaString = "";
        String newBlackCommaString = "";
        StringTokenizer tokenizer = new StringTokenizer(newWhiteString, ",; \n\t\r");
        while (tokenizer.hasMoreTokens()) {
            String value = tokenizer.nextToken();
            if (newWhitelist.equalsIgnoreCase("")) {
                newWhitelist = value;
            } else {
                newWhitelist = newWhitelist + "\n" + value;
            }
            if (newWhiteCommaString.equalsIgnoreCase("")) {
                newWhiteCommaString = value;
            } else {
                newWhiteCommaString = newWhiteCommaString + "," + value;
            }
        }
        String value2 = spamPrefs.get("whiteList") != null ? spamPrefs.get("whiteList").toString() : "";
        saveMailPreference(getId().getId(), "spam_whitelist_from", value2, newWhitelist, newWhiteCommaString, "");
        spamPrefs.put("whiteList", newWhitelist);
        StringTokenizer tokenizer2 = new StringTokenizer(newBlackString, ",; \n\t\r");
        while (tokenizer2.hasMoreTokens()) {
            String value3 = tokenizer2.nextToken();
            if (newBlacklist.equalsIgnoreCase("")) {
                newBlacklist = value3;
            } else {
                newBlacklist = newBlacklist + "\n" + value3;
            }
            if (newBlackCommaString.equalsIgnoreCase("")) {
                newBlackCommaString = value3;
            } else {
                newBlackCommaString = newBlackCommaString + "," + value3;
            }
        }
        String value4 = spamPrefs.get("blackList") != null ? spamPrefs.get("blackList").toString() : "";
        saveMailPreference(getId().getId(), "spam_blacklist_from", value4, newBlacklist, newBlackCommaString, "");
        spamPrefs.put("blackList", newBlacklist);
        String value5 = spamPrefs.get("spamLevel") != null ? spamPrefs.get("spamLevel").toString() : AntiSpam.DEFAULT_LEVEL_VALUE;
        saveMailPreference(getId().getId(), "spam_level", value5, newSpamLevel, newSpamLevel, AntiSpam.DEFAULT_LEVEL_VALUE);
        spamPrefs.put("spamLevel", newSpamLevel);
        String value6 = spamPrefs.get("spamProcessing") != null ? spamPrefs.get("spamProcessing").toString() : "remove";
        saveMailPreference(getId().getId(), "spam_processing", value6, newSpamProcessing, newSpamProcessing, "remove");
        spamPrefs.put("spamProcessing", newSpamProcessing);
        String value7 = spamPrefs.get("spamMaxScore") != null ? spamPrefs.get("spamMaxScore").toString() : AntiSpam.DEFAULT_MAXSCORE_VALUE;
        saveMailPreference(getId().getId(), "spam_max_score", value7, newMaxScore, newMaxScore, AntiSpam.DEFAULT_MAXSCORE_VALUE);
        spamPrefs.put("spamMaxScore", newMaxScore);
        this.isSpamPrefsLoaded = true;
        for (Object rid : getId().findChildren("antispam")) {
            AntiSpam sa = (AntiSpam) ((ResourceId) rid).get();
            if (sa.getUseMdomainPrefs() == 1) {
                sa.setAntiSpamPreferences(1, newWhiteString, newBlackString, newSpamLevel, newSpamProcessing, newMaxScore);
            }
        }
        this.spamPreferences.clear();
        for (String key2 : spamPrefs.keySet()) {
            this.spamPreferences.put(key2, spamPrefs.get(key2));
        }
    }

    public TemplateModel FM_setAntiVirusPreferences(String newVirusProcessing) throws Exception {
        setAntiVirusPreferences(newVirusProcessing);
        return this;
    }

    public void setAntiVirusPreferences(String newVirusProcessing) throws Exception {
        HashMap virusPrefs = new HashMap();
        for (String key : this.virusPreferences.keySet()) {
            virusPrefs.put(key, this.virusPreferences.get(key));
        }
        String value = virusPrefs.get("virusProcessing") != null ? virusPrefs.get("virusProcessing").toString() : "remove";
        saveMailPreference(getId().getId(), "virus_processing", value, newVirusProcessing, newVirusProcessing, "remove");
        virusPrefs.put("virusProcessing", newVirusProcessing);
        this.isVirusPrefsLoaded = true;
        for (Object rid : getId().findChildren("antivirus")) {
            AntiVirus av = (AntiVirus) ((ResourceId) rid).get();
            if (av.getUseMdomainPrefs() == 1) {
                av.setAntiVirusPreferences(1, newVirusProcessing);
            }
        }
        this.virusPreferences.clear();
        for (String key2 : virusPrefs.keySet()) {
            this.virusPreferences.put(key2, virusPrefs.get(key2));
        }
    }

    public void saveMailPreference(long mobjectId, String preference, String oldValue, String newValue, String newValueSave, String defaultValue) throws Exception {
        if (!newValue.equals(oldValue)) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                if (newValue.equalsIgnoreCase(defaultValue)) {
                    ps = con.prepareStatement("DELETE FROM mail_preferences WHERE mobject_id = ? AND preference=?");
                    ps.setLong(1, mobjectId);
                    ps.setString(2, preference);
                } else if (oldValue.equalsIgnoreCase(defaultValue)) {
                    ps = con.prepareStatement("INSERT INTO mail_preferences(mobject_id, preference, value) VALUES (?, ?, ?)");
                    ps.setLong(1, mobjectId);
                    ps.setString(2, preference);
                    Session.setClobValue(ps, 3, newValueSave);
                } else {
                    ps = con.prepareStatement("UPDATE mail_preferences SET value = ? WHERE mobject_id = ? AND preference = ?");
                    Session.setClobValue(ps, 1, newValueSave);
                    ps.setLong(2, mobjectId);
                    ps.setString(3, preference);
                }
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    public TemplateModel FM_getAliasedByAsList(String aliasedBy) throws Exception {
        LinkedList listAliasedBy = new LinkedList();
        StringTokenizer st = new StringTokenizer(aliasedBy, " ,;");
        while (st.hasMoreTokens()) {
            String alias = st.nextToken();
            if (!alias.equalsIgnoreCase("")) {
                listAliasedBy.add(alias);
            }
        }
        return new TemplateList(listAliasedBy);
    }

    public LinkedList getAliasedBy() throws Exception {
        long now = TimeUtils.currentTimeMillis();
        if (now - this.aliasedByLastTimeLoaded > TIME_TO_LIVE) {
            this.aliasedBy.clear();
            Collection<ResourceId> col = getChildHolder().getChildrenByName("mailbox_alias");
            for (ResourceId resourceId : col) {
                MailAlias ma = (MailAlias) resourceId.get();
                if (ma.existSubscriber(postmaster)) {
                    this.aliasedBy.add(ma.local);
                }
            }
            this.aliasedByLastTimeLoaded = now;
        }
        return this.aliasedBy;
    }

    public String getMailPreferevceSaveAction(String oldValue, String newValue, String defaultValue) throws Exception {
        String action = "edit";
        if (newValue.equals(oldValue)) {
            action = "noedit";
        } else if (defaultValue.equalsIgnoreCase("") && newValue.equalsIgnoreCase(defaultValue)) {
            action = "delete";
        }
        return action;
    }

    public void insertMailDomainPreferenceAfterUnlink(long mid, String preference, String newValue, String newValueForSave, String defaultValue) throws Exception {
        if (!newValue.equals(defaultValue)) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("INSERT INTO mail_preferences(mobject_id, preference, value) VALUES (?, ?, ?)");
                ps.setLong(1, mid);
                ps.setString(2, preference);
                Session.setClobValue(ps, 3, newValueForSave);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
    }

    @Override // psoft.hsphere.Resource
    protected void onChildDelete(Resource curr) {
        if ((curr instanceof Mailbox) || (curr instanceof MailForward) || (curr instanceof MailAlias) || (curr instanceof Autoresponder) || (curr instanceof AntiSpam) || (curr instanceof AntiVirus)) {
            try {
                MailReport.cleanMailReport(getId().getId());
            } catch (Exception ex) {
                Session.getLog().debug("Unable to clean mail report for cache ", ex);
            }
        }
    }

    @Override // psoft.hsphere.Resource
    protected void onChildCreate(Resource curr) {
        onChildDelete(curr);
    }

    public TemplateModel FM_addAntiSpamResources(String range) throws Exception {
        ArrayList newAntiSpams = prepareNewAntiSpams(range);
        for (int i = 0; i < newAntiSpams.size(); i++) {
            String local = (String) newAntiSpams.get(i);
            ArrayList args = new ArrayList();
            args.add(local);
            addChild("antispam", "", args);
        }
        return this;
    }

    public TemplateModel FM_addAntiVirusResources(String range) throws Exception {
        ArrayList newAntiViruses = prepareNewAntiViruses(range);
        for (int i = 0; i < newAntiViruses.size(); i++) {
            String local = (String) newAntiViruses.get(i);
            ArrayList args = new ArrayList();
            args.add(local);
            addChild("antivirus", "", args);
        }
        return this;
    }

    private ArrayList loadMailObjects(Collection col, ArrayList mailObjects) throws Exception {
        Iterator i = col.iterator();
        while (i.hasNext()) {
            Resource r = ((ResourceId) i.next()).get();
            if (!mailObjects.contains(r.get("local").toString())) {
                mailObjects.add(r.get("local").toString());
            }
        }
        return mailObjects;
    }

    public TemplateModel FM_getPrepareNewAntiVirusesNumber(String range) throws Exception {
        return new TemplateString(prepareNewAntiViruses(range).size());
    }

    public TemplateModel FM_getPrepareNewAntiSpamsNumber(String range) throws Exception {
        return new TemplateString(prepareNewAntiSpams(range).size());
    }

    private ArrayList prepareNewAntiViruses(String range) throws Exception {
        ArrayList existsAntiVirusEmails = new ArrayList();
        ArrayList mailObjects = new ArrayList();
        ArrayList newAntiViruses = new ArrayList();
        mailObjects.add(postmaster);
        Collection<ResourceId> col = getChildHolder().getChildrenByName("antivirus");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            existsAntiVirusEmails.add(((AntiVirus) r).getLocal());
        }
        ArrayList mailObjects2 = loadMailObjects(getChildHolder().getChildrenByName("mailbox"), mailObjects);
        if (range == null || range.equalsIgnoreCase("all")) {
            mailObjects2 = loadMailObjects(getChildHolder().getChildrenByName("responder"), loadMailObjects(getChildHolder().getChildrenByName("mailbox_alias"), loadMailObjects(getChildHolder().getChildrenByName("mail_forward"), mailObjects2)));
        }
        Iterator i = mailObjects2.iterator();
        while (i.hasNext()) {
            String local = (String) i.next();
            if (existsAntiVirusEmails.indexOf(local) == -1) {
                newAntiViruses.add(local);
            }
        }
        return newAntiViruses;
    }

    private ArrayList prepareNewAntiSpams(String range) throws Exception {
        ArrayList existsAntiSpamEmails = new ArrayList();
        ArrayList mailObjects = new ArrayList();
        ArrayList newAntiSpams = new ArrayList();
        mailObjects.add(postmaster);
        Collection<ResourceId> col = getChildHolder().getChildrenByName("antispam");
        for (ResourceId resourceId : col) {
            Resource r = resourceId.get();
            existsAntiSpamEmails.add(((AntiSpam) r).getLocal());
        }
        ArrayList mailObjects2 = loadMailObjects(getChildHolder().getChildrenByName("mailbox"), mailObjects);
        if (range == null || range.equalsIgnoreCase("all")) {
            mailObjects2 = loadMailObjects(getChildHolder().getChildrenByName("responder"), loadMailObjects(getChildHolder().getChildrenByName("mailbox_alias"), loadMailObjects(getChildHolder().getChildrenByName("mail_forward"), mailObjects2)));
        }
        Iterator i = mailObjects2.iterator();
        while (i.hasNext()) {
            String local = (String) i.next();
            if (existsAntiSpamEmails.indexOf(local) == -1) {
                newAntiSpams.add(local);
            }
        }
        return newAntiSpams;
    }

    public TemplateModel FM_discardMail(String action) throws Exception {
        if ("enable".equals(action)) {
            this.postmasterDiscardMail = 1;
        } else {
            this.postmasterDiscardMail = 0;
        }
        if (C0004CP.isIrisEnabled()) {
            if (postmaster.equals(getCatchAll())) {
                MailServices.discardIncommingMail(getPostmaster(), String.valueOf(this.postmasterDiscardMail), new String("mailbox"), true);
            } else {
                MailServices.discardIncommingMail(getPostmaster(), String.valueOf(this.postmasterDiscardMail), new String("mailbox"), false);
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mailboxes set discard_mail = ? WHERE id = ?");
            ps.setInt(1, this.postmasterDiscardMail);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (!C0004CP.isIrisEnabled()) {
                setConfig(postmaster, "");
            }
            return new TemplateOKResult();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isDiscard() {
        if (this.postmasterDiscardMail == 1) {
            return true;
        }
        return false;
    }
}
