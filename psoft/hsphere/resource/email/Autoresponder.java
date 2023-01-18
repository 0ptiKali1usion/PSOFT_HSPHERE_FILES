package psoft.hsphere.resource.email;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
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
import psoft.hsphere.Uploader;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.Base64;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/email/Autoresponder.class */
public class Autoresponder extends Resource implements HostDependentResource, ComparableResource {
    protected String local;
    protected String foreign;
    protected String subject;
    protected String message;
    protected String description;
    protected int includeIncoming;
    protected ArrayList attachments;
    public static final long TIME_TO_LIVE = 300000;
    private long attachmentsLastTimeLoaded;
    protected String fullEmail;

    public Autoresponder(ResourceId id) throws Exception {
        super(id);
        this.attachments = new ArrayList();
        this.attachmentsLastTimeLoaded = 0L;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT local_email, foreign_email, subject, message, include_incoming  FROM responders WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.local = rs.getString(1);
                this.foreign = rs.getString(2);
                this.subject = rs.getString(3);
                this.message = Session.getClobValue(rs, 4);
                try {
                    this.includeIncoming = rs.getInt(5);
                } catch (Exception e) {
                    this.includeIncoming = 0;
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
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public Autoresponder(int type, Collection values) throws Exception {
        super(type, values);
        String atts;
        this.attachments = new ArrayList();
        this.attachmentsLastTimeLoaded = 0L;
        Iterator i = values.iterator();
        this.local = ((String) i.next()).toLowerCase();
        this.foreign = (String) i.next();
        this.subject = (String) i.next();
        this.message = (String) i.next();
        if (this.message == null) {
            this.message = "";
        }
        this.description = (String) i.next();
        String tmpIncludeIncoming = null;
        tmpIncludeIncoming = i.hasNext() ? (String) i.next() : tmpIncludeIncoming;
        if (tmpIncludeIncoming == null || tmpIncludeIncoming.equals("")) {
            this.includeIncoming = 0;
        } else {
            this.includeIncoming = Integer.parseInt(tmpIncludeIncoming);
        }
        if (i.hasNext() && (atts = (String) i.next()) != null) {
            StringTokenizer atTkz = new StringTokenizer(atts, "/");
            while (atTkz.hasMoreTokens()) {
                HashMap file = new HashMap();
                String str = atTkz.nextToken();
                file.put("name", str.trim());
                file.put("size", "unknown");
                this.attachments.add(file);
            }
        }
    }

    /* JADX WARN: Finally extract failed */
    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        if (getFullEmail().equals(this.foreign)) {
            throw new HSUserException("autoresponder.mail");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO responders (id, local_email, foreign_email, subject, message, include_incoming) VALUES (?, ?, ?, ?, ?, ?)");
            ps2.setLong(1, getId().getId());
            ps2.setString(2, this.local);
            ps2.setString(3, this.foreign);
            if (this.subject == null) {
                this.subject = "";
            }
            ps2.setString(4, this.subject);
            Session.setClobValue(ps2, 5, this.message);
            ps2.setInt(6, this.includeIncoming);
            ps2.executeUpdate();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO mailobject (id, full_email)  VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setString(2, getFullEmail());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            setDescription(this.description);
            Collection<ResourceId> responders = getParent().getChildHolder().getChildrenByName("responder");
            synchronized (responders) {
                for (ResourceId rid : responders) {
                    if (!rid.equals(getId()) && this.local.equals(rid.get("local").toString())) {
                        throw new HSUserException("autoresponder.exist");
                        break;
                    }
                }
            }
            if (C0004CP.isIrisEnabled()) {
                String is_exist_mob = "0";
                Collection<ResourceId> boxes = getParent().getChildHolder().getChildrenByName("mailbox");
                for (ResourceId rid2 : boxes) {
                    if (this.local.equals(rid2.get("email").toString())) {
                        is_exist_mob = "1";
                        if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                            is_exist_mob = "2";
                        }
                    }
                }
                if (is_exist_mob.equals("0")) {
                    Collection<ResourceId> fwds = getParent().getChildHolder().getChildrenByName("mail_forward");
                    for (ResourceId rid3 : fwds) {
                        if (this.local.equals(rid3.get("local").toString())) {
                            is_exist_mob = "1";
                            if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                                is_exist_mob = "2";
                            }
                        }
                    }
                }
                if (is_exist_mob.equals("0")) {
                    Collection<ResourceId> als = getParent().getChildHolder().getChildrenByName("mailbox_alias");
                    for (ResourceId rid4 : als) {
                        if (this.local.equals(rid4.get("local").toString())) {
                            is_exist_mob = "1";
                        }
                    }
                }
                if (is_exist_mob.equals("0")) {
                    Collection<ResourceId> mlists = getParent().getChildHolder().getChildrenByName("mailing_list");
                    for (ResourceId rid5 : mlists) {
                        if (this.local.equals(rid5.get("email").toString())) {
                            is_exist_mob = "1";
                        }
                    }
                }
                MailServices.createResponder(getFullEmail(), this.subject, this.message, is_exist_mob);
            } else {
                Collection<ResourceId> mobjects = new ArrayList();
                mobjects.addAll(getParent().getChildHolder().getChildrenByName("mailing_list"));
                for (ResourceId rid6 : mobjects) {
                    if (this.local.equals(rid6.get("email").toString())) {
                        throw new HSUserException("mailobject.exist");
                        break;
                    }
                }
                ((MailDomain) getParent().get()).setConfig(this.local, "");
                physicalCreate(getHostId());
            }
            if (this.attachments.size() > 0) {
                addAttachments(this.attachments);
            }
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

    public TemplateModel FM_updateResponder(String foreign, String subject, String message, int includeIncoming) throws Exception {
        updateResponder(foreign, subject, message, includeIncoming);
        return this;
    }

    protected void updateResponder(String foreign, String subject, String message, int includeIncoming) throws Exception {
        accessCheck(0);
        if (C0004CP.isIrisEnabled()) {
            String isExistsMob = "0";
            Collection<ResourceId> boxes = getParent().getChildHolder().getChildrenByName("mailbox");
            for (ResourceId rid : boxes) {
                if (this.local.equals(rid.get("email").toString())) {
                    isExistsMob = "1";
                    if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                        isExistsMob = "2";
                    }
                }
            }
            if (isExistsMob.equals("0")) {
                Collection<ResourceId> fwds = getParent().getChildHolder().getChildrenByName("mail_forward");
                for (ResourceId rid2 : fwds) {
                    if (this.local.equals(rid2.get("local").toString())) {
                        isExistsMob = "1";
                        if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                            isExistsMob = "2";
                        }
                    }
                }
            }
            if (isExistsMob.equals("0")) {
                Collection<ResourceId> als = getParent().getChildHolder().getChildrenByName("mailbox_alias");
                for (ResourceId rid3 : als) {
                    if (this.local.equals(rid3.get("local").toString())) {
                        isExistsMob = "1";
                    }
                }
            }
            if (isExistsMob.equals("0")) {
                Collection<ResourceId> mls = getParent().getChildHolder().getChildrenByName("mailing_list");
                for (ResourceId rid4 : mls) {
                    if (this.local.equals(rid4.get("email").toString())) {
                        isExistsMob = "1";
                    }
                }
            }
            MailServices.modifyResponder(getFullEmail(), subject, message, isExistsMob);
            this.foreign = foreign;
            this.subject = subject;
            this.message = message;
            this.includeIncoming = includeIncoming;
        } else {
            String oldForeign = this.foreign;
            String oldSubject = this.subject;
            String oldMessage = this.message;
            int oldIncludeIncoming = this.includeIncoming;
            this.foreign = foreign;
            this.subject = subject;
            this.message = message;
            this.includeIncoming = includeIncoming;
            int physicalCreationPart = 0;
            try {
                if (!"".equals(oldForeign)) {
                    ((MailDomain) getParent().get()).setConfig(this.local, "");
                }
                physicalCreationPart = 1;
                physicalCreate(getHostId());
            } catch (Exception e) {
                this.foreign = oldForeign;
                this.subject = oldSubject;
                this.message = oldMessage;
                this.includeIncoming = oldIncludeIncoming;
                if (!"".equals(oldForeign) && physicalCreationPart == 1) {
                    ((MailDomain) getParent().get()).setConfig(this.local, "");
                }
                throw e;
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE responders SET foreign_email = ?, subject = ?,  message = ?, include_incoming = ? WHERE id = ?");
            ps.setString(1, foreign);
            ps.setString(2, subject);
            Session.setClobValue(ps, 3, message);
            ps.setInt(4, includeIncoming);
            ps.setLong(5, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addAttachments(String atts) throws Exception {
        addAttachments(atts);
        return this;
    }

    public void addAttachments(String atts) throws Exception {
        ArrayList l = new ArrayList();
        StringTokenizer atTkz = new StringTokenizer(atts, "/");
        while (atTkz.hasMoreTokens()) {
            HashMap file = new HashMap();
            String str = atTkz.nextToken();
            file.put("name", str.trim());
            file.put("size", "unknown");
            l.add(file);
        }
        addAttachments(l);
    }

    public void addAttachments(ArrayList atts) throws Exception {
        boolean reloadAttachments = false;
        int needMessageEncode = isNeedEncode(this.message) ? 1 : 0;
        Iterator i = atts.iterator();
        while (i.hasNext()) {
            String fileName = (String) ((HashMap) i.next()).get("name");
            String fName = "";
            if (fileName.indexOf("|") > 0) {
                fName = fileName.substring(0, fileName.indexOf("|"));
            }
            try {
                byte[] fileData = Uploader.getData(fileName);
                if (fileData.length != 0) {
                    reloadAttachments = true;
                    ArrayList l = new ArrayList();
                    l.add(getFullEmail());
                    l.add('\"' + fName + '\"');
                    l.add(Integer.toString(this.includeIncoming));
                    l.add(Integer.toString(needMessageEncode));
                    HostEntry he = recursiveGet("mail_server");
                    he.exec("autoresponder-attachment-add.pl", l, fileData);
                }
            } catch (Exception e) {
                throw new HSUserException("autoresponder.upload_failure", new String[]{fName});
            }
        }
        if (reloadAttachments) {
            initAttachments(true);
            return;
        }
        throw new HSUserException("autoresponder.attachment_empty");
    }

    public TemplateModel FM_removeAttachments(String atts) throws Exception {
        removeAttachments(atts);
        return this;
    }

    public void removeAttachments(String atts) throws Exception {
        initAttachments(false);
        StringTokenizer atTkz = new StringTokenizer(atts, "/");
        String attsPipeSeparated = "";
        while (atTkz.hasMoreTokens()) {
            String fileName = atTkz.nextToken().trim();
            if (attsPipeSeparated.equalsIgnoreCase("")) {
                attsPipeSeparated = fileName;
            } else {
                attsPipeSeparated = attsPipeSeparated + "|" + fileName;
            }
            int count = 0;
            Iterator j = this.attachments.iterator();
            while (true) {
                if (j.hasNext()) {
                    HashMap h = (HashMap) j.next();
                    if (((String) h.get("name")).equalsIgnoreCase(fileName)) {
                        this.attachments.remove(count);
                        break;
                    }
                    count++;
                }
            }
        }
        List l = new ArrayList();
        l.add(getFullEmail());
        l.add('\"' + attsPipeSeparated + '\"');
        l.add(Integer.toString(this.includeIncoming));
        l.add(Integer.toString(isNeedEncode(this.message) ? 1 : 0));
        HostEntry he = recursiveGet("mail_server");
        he.exec("autoresponder-attachments-del", l);
        this.attachmentsLastTimeLoaded = TimeUtils.currentTimeMillis();
    }

    public TemplateModel FM_getAttachments() throws Exception {
        initAttachments(false);
        return new TemplateList(this.attachments);
    }

    public ArrayList getAttachments() throws Exception {
        initAttachments(false);
        return this.attachments;
    }

    private void loadAttachments() throws Exception {
        HostEntry he = recursiveGet("mail_server");
        ArrayList l = new ArrayList();
        l.add(getFullEmail());
        Collection<String> listRes = he.exec("autoresponder-attachments-list.pl", l);
        this.attachments.clear();
        for (String str : listRes) {
            HashMap file = new HashMap();
            StringTokenizer st = new StringTokenizer(str, "|");
            if (st.hasMoreTokens()) {
                file.put("name", st.nextToken());
                if (st.hasMoreTokens()) {
                    file.put("size", st.nextToken());
                }
                this.attachments.add(file);
            }
        }
        this.attachmentsLastTimeLoaded = TimeUtils.currentTimeMillis();
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            if (C0004CP.isIrisEnabled()) {
                String isExistsMob = "0";
                Collection<ResourceId> boxes = getParent().getChildHolder().getChildrenByName("mailbox");
                for (ResourceId rid : boxes) {
                    if (this.local.equals(rid.get("email").toString())) {
                        isExistsMob = "1";
                        if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                            isExistsMob = "2";
                        }
                    }
                }
                if (isExistsMob.equals("0")) {
                    Collection<ResourceId> fwds = getParent().getChildHolder().getChildrenByName("mail_forward");
                    for (ResourceId rid2 : fwds) {
                        if (this.local.equals(rid2.get("local").toString())) {
                            isExistsMob = "1";
                            if (((MailDomain) getParent().get()).getCatchAll().equals(this.local)) {
                                isExistsMob = "2";
                            }
                        }
                    }
                }
                if (isExistsMob.equals("0")) {
                    Collection<ResourceId> als = getParent().getChildHolder().getChildrenByName("mailbox_alias");
                    for (ResourceId rid3 : als) {
                        if (this.local.equals(rid3.get("local").toString())) {
                            isExistsMob = "1";
                        }
                    }
                }
                MailServices.removeResponder(getFullEmail(), isExistsMob);
            } else if (!getParent().get().isDeletePrev()) {
                physicalDelete(getHostId());
                ((MailDomain) getParent().get()).setConfig(this.local, getId().toString());
            }
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("DELETE FROM responders WHERE id = ?");
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
        if ("local".equals(key)) {
            return new TemplateString(this.local);
        }
        if ("foreign".equals(key)) {
            return new TemplateString(this.foreign);
        }
        if ("subject".equals(key)) {
            return new TemplateString(this.subject);
        }
        if ("message".equals(key)) {
            return new TemplateString(this.message);
        }
        if ("fullemail".equals(key)) {
            return new TemplateString(getFullEmail());
        }
        if ("include_incoming".equals(key)) {
            return new TemplateString(this.includeIncoming);
        }
        if ("attachments_count".equals(key)) {
            try {
                initAttachments(false);
                return new TemplateString(this.attachments.size());
            } catch (Exception e) {
                Session.getLog().error("Can't get autoresponder attachments");
            }
        }
        return super.get(key);
    }

    private void initAttachments(boolean force) throws Exception {
        if (force || TimeUtils.currentTimeMillis() - this.attachmentsLastTimeLoaded > 300000) {
            loadAttachments();
        }
    }

    public String getLocal() {
        return this.local;
    }

    public String getForeign() {
        return this.foreign;
    }

    public String getSubject() {
        return this.subject;
    }

    public String getMessage() {
        return this.message;
    }

    public int getIncludeIncoming() {
        return this.includeIncoming;
    }

    @Override // psoft.hsphere.ComparableResource
    public String comparableString() throws Exception {
        return getFullEmail();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.responder.refund", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.responder.setup", new Object[]{_getFullEmail()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.responder.recurrent", new Object[]{getPeriodInWords(), _getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.responder.refundall", new Object[]{_getFullEmail(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        String subjectVal;
        String subjectVal2;
        List l = new ArrayList();
        l.add(getFullEmail());
        int needEncodeSubj = isNeedEncode(this.subject) ? 1 : 0;
        if (needEncodeSubj == 1) {
            subjectVal = Base64.encode(this.subject);
        } else {
            subjectVal = this.subject;
        }
        if (!"".equals(this.subject)) {
            StringBuffer sb = new StringBuffer(subjectVal);
            StringBuffer res = new StringBuffer();
            for (int i = 0; i < sb.length(); i++) {
                if (!Character.isLetterOrDigit(sb.charAt(i))) {
                    res.append("\\");
                }
                res.append(sb.charAt(i));
            }
            subjectVal2 = res.toString();
        } else {
            subjectVal2 = "\"\"";
        }
        l.add(subjectVal2);
        l.add(Integer.toString(this.includeIncoming));
        String messageToScript = this.message;
        int needEncodeMsg = isNeedEncode(this.message) ? 1 : 0;
        if (needEncodeMsg == 1) {
            messageToScript = Base64.encode(this.message);
        }
        l.add(Integer.toString(needEncodeSubj));
        l.add(Integer.toString(needEncodeMsg));
        HostEntry he = HostManager.getHost(targetHostId);
        he.exec("autoresponder-message-add", l, messageToScript);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        ArrayList l = new ArrayList();
        l.add(getFullEmail());
        HostEntry he = HostManager.getHost(targetHostId);
        he.exec("autoresponder-message-del", l);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return recursiveGet("mail_server").getId();
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    public boolean isNeedEncode(String s) throws Exception {
        if (s != null && !s.equalsIgnoreCase("")) {
            char[] c = s.toCharArray();
            for (char c2 : c) {
                if (c2 > 128) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
