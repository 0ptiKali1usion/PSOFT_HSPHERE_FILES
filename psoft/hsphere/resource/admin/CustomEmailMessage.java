package psoft.hsphere.resource.admin;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.Account;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.util.XMLManager;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.Template2String;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CustomEmailMessage.class */
public class CustomEmailMessage extends SharedObject implements TemplateHashModel {
    protected String subjectStr;
    protected Template subject;
    protected String contentType;
    protected String bodyStr;
    protected Template body;
    protected String locale;
    protected String defaultTemplate;
    protected int groupId;
    protected boolean noCC;
    public static final String DEFAULT_LOCALE = "en_US_ISO8859_1";
    private static Category log = Category.getInstance(CustomEmailMessage.class.getName());
    protected static MessageIdLookupMap messageLookupMap = new MessageIdLookupMap();

    static {
        try {
            Document custom_emails = XMLManager.getXML("USER_EMAILS");
            Element root = custom_emails.getDocumentElement();
            NodeList groups = root.getElementsByTagName("group");
            for (int i = 0; i < groups.getLength(); i++) {
                Element group = (Element) groups.item(i);
                String group_id = group.getAttribute("id");
                String group_name = group.getAttribute("name");
                DefaultEmailTemplate.addGroup(group_id, group_name);
            }
            NodeList emails = root.getElementsByTagName("email");
            for (int i2 = 0; i2 < emails.getLength(); i2++) {
                Element email = (Element) emails.item(i2);
                String subject = email.getElementsByTagName("subject").item(0).getFirstChild().getNodeValue();
                boolean noCC = Boolean.valueOf(email.getAttribute("no_cc")).booleanValue();
                boolean adminOnly = Boolean.valueOf(email.getAttribute("admin_only")).booleanValue();
                DefaultEmailTemplate.add(email.getAttribute("tag"), email.getAttribute("template"), subject, email.getAttribute("group_id"), email.getAttribute("massmail_applicable"), noCC, adminOnly);
            }
        } catch (Exception e) {
            log.error("Unable to read custom email", e);
        }
    }

    public static Set getTagSet() {
        return DefaultEmailTemplate.tags();
    }

    public static HashMap getGroups() {
        return DefaultEmailTemplate.groups();
    }

    public static HashMap getDefaultTemplates() {
        return DefaultEmailTemplate.defaultTemplates();
    }

    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("subject".equals(key)) {
            return new TemplateString(this.subjectStr);
        }
        if ("body".equals(key)) {
            return new TemplateString(this.bodyStr);
        }
        if ("contentType".equals(key)) {
            return new TemplateString(this.contentType);
        }
        if ("locale".equals(key)) {
            return new TemplateString(this.locale);
        }
        if ("group_id".equals(key)) {
            return new TemplateString(this.groupId);
        }
        if ("no_cc".equals(key)) {
            return new TemplateString(String.valueOf(this.noCC));
        }
        return null;
    }

    public static CustomEmailMessage getMessage(Integer cid) throws Exception {
        synchronized (SharedObject.getLock(cid.intValue())) {
            CustomEmailMessage cem = (CustomEmailMessage) get(cid.intValue(), CustomEmailMessage.class);
            if (cem != null) {
                return cem;
            }
            return load(cid);
        }
    }

    protected static CustomEmailMessage load(Integer cid) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT subject, body, content_type, locale, def, tag, no_cc FROM custom_emails WHERE id = ?");
            ps.setInt(1, cid.intValue());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                CustomEmailMessage customEmailMessage = new CustomEmailMessage(cid, Session.getClobValue(rs, "subject"), Session.getClobValue(rs, "body"), rs.getString("content_type"), rs.getString("locale"), DefaultEmailTemplate.getGroupId(rs.getString("tag")), rs.getBoolean("no_cc"));
                Session.closeStatement(ps);
                con.close();
                return customEmailMessage;
            }
            throw new Exception("No such custom message: " + cid);
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static MessageIdLookupMap getMessageIdLookupMap() {
        return messageLookupMap;
    }

    public static Integer getMessageId(String tag, String locale) throws Exception {
        long resId;
        if (DefaultEmailTemplate.getGroupId(tag) == 2) {
            resId = 1;
        } else {
            resId = Session.getResellerId();
        }
        Integer cid = pickMessageId(resId, tag, locale);
        if (cid == null) {
            cid = pickMessageId(0L, tag, locale);
        }
        if (cid == null && DefaultEmailTemplate.getTemplate(tag) != null) {
            updateFromTemplate(0L, tag, DEFAULT_LOCALE, true, DefaultEmailTemplate.isNoCC(tag));
            cid = pickMessageId(0L, tag, locale);
        }
        return cid;
    }

    private static Integer pickMessageId(long resId, String tag, String locale) {
        Integer cid = messageLookupMap.getId(new MessageLookupKey(resId, tag, locale));
        if (cid != null) {
            return cid;
        }
        return messageLookupMap.getId(new MessageLookupKey(resId, tag));
    }

    public static CustomEmailMessage getMessage(String tag) throws Exception {
        return getMessage(tag, Session.getCurrentLocale().toString());
    }

    public static CustomEmailMessage getMessage(String tag, String locale) throws Exception {
        Integer cid = getMessageId(tag, locale);
        if (cid != null) {
            return getMessage(cid);
        }
        throw new Exception("No custom email message for tag '" + tag + "'.");
    }

    public static void delete(int resId, String tag, String locale) throws Exception {
        Integer cid = messageLookupMap.getId(new MessageLookupKey(resId, tag, locale));
        if (cid == null) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM custom_emails WHERE id = ?");
            ps.setInt(1, cid.intValue());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            CustomEmailMessage message = (CustomEmailMessage) getCache().get(cid);
            if (message != null) {
                getCache().remove(message);
            }
            messageLookupMap.resetId(new MessageLookupKey(resId, tag, locale));
            MessageLookupKey key2 = new MessageLookupKey(resId, tag);
            if (cid.equals(messageLookupMap.getId(key2))) {
                messageLookupMap.resetId(key2);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static String checkUpdateMessage(String mes) throws IOException {
        TemplateWrapper t = new TemplateWrapper(new StringReader(HTMLEncoder.decode(mes)));
        if (!t.isStatusOK()) {
            return t.getErrorMessage();
        }
        return null;
    }

    public static void update(long resId, String tag, String subject, String body, String contentType, String locale, boolean def, boolean no_cc) throws Exception {
        update(resId, tag, subject, body, contentType, locale, def, 0, no_cc);
    }

    public static void update(long resId, String tag, String subject, String body, String contentType, String locale, boolean isDefault, int group_id, boolean no_cc) throws Exception {
        String newLocale;
        MessageLookupKey lookupKey;
        String subject2 = HTMLEncoder.encode(subject);
        String body2 = HTMLEncoder.encode(body);
        String checkErrMessage = checkUpdateMessage(subject2);
        if (checkErrMessage == null) {
            checkErrMessage = checkUpdateMessage(body2);
        }
        if (checkErrMessage != null) {
            throw new Exception(checkErrMessage);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        if (isDefault) {
            newLocale = "";
            lookupKey = new MessageLookupKey(resId, tag);
        } else {
            newLocale = locale;
            lookupKey = new MessageLookupKey(resId, tag, locale);
        }
        Integer cid = messageLookupMap.getId(lookupKey);
        try {
            if (cid == null) {
                cid = new Integer(Session.getNewId("custom_email_seq"));
                ps = con.prepareStatement("INSERT INTO custom_emails (id, reseller_id, subject, body, tag, locale, content_type, def, no_cc) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setInt(1, cid.intValue());
                ps.setLong(2, resId);
                Session.setClobValue(ps, 3, subject2);
                Session.setClobValue(ps, 4, body2);
                ps.setString(5, tag);
                ps.setString(6, newLocale);
                ps.setString(7, contentType);
                ps.setInt(8, isDefault ? 1 : 0);
                ps.setBoolean(9, no_cc);
                ps.executeUpdate();
                messageLookupMap.setId(lookupKey, cid);
            } else {
                ps = con.prepareStatement("UPDATE custom_emails SET subject = ?, body = ?, locale=?, content_type = ?, def = ?, no_cc = ? WHERE id = ?");
                Session.setClobValue(ps, 1, subject2);
                Session.setClobValue(ps, 2, body2);
                ps.setString(3, newLocale);
                ps.setString(4, contentType);
                ps.setInt(5, isDefault ? 1 : 0);
                ps.setBoolean(6, no_cc);
                ps.setInt(7, cid.intValue());
                ps.executeUpdate();
            }
            if (isDefault && resId != 0) {
                ps = con.prepareStatement("DELETE FROM custom_emails WHERE id <> ? AND reseller_id = ? AND tag = ? AND (def = ? OR locale = ?)");
                ps.setInt(1, cid.intValue());
                ps.setLong(2, resId);
                ps.setString(3, tag);
                ps.setInt(4, 1);
                ps.setString(5, locale != null ? locale : "");
                ps.executeUpdate();
                MessageLookupKey baseKey = new MessageLookupKey(resId, tag, locale);
                if (messageLookupMap.getId(baseKey) != null) {
                    messageLookupMap.resetId(baseKey);
                }
            }
            new CustomEmailMessage(cid, subject2, body2, contentType, locale, group_id, no_cc);
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public static void updateFromTemplate(long resId, String tag, String locale, boolean def, boolean no_cc) throws Exception {
        String templateName = DefaultEmailTemplate.getTemplate(tag);
        String subject = DefaultEmailTemplate.getSubject(tag);
        int groupId = DefaultEmailTemplate.getGroupId(tag);
        File file = new File(new File(new File(Session.getPropertyString("USER_TEMPLATE_PATH")), "common"), templateName);
        if (!file.exists()) {
            file = new File(new File(new File(Session.getPropertyString("TEMPLATE_PATH")), "common"), templateName);
        }
        log.info("Importing: " + file.getAbsoluteFile());
        FileInputStream in = new FileInputStream(file);
        byte[] b = new byte[(int) file.length()];
        int length = in.read(b);
        if (length != file.length()) {
            log.error("Error reading file: " + file.getAbsoluteFile() + " read: " + length + " :should have: " + file.length());
        }
        in.close();
        if (groupId != 0) {
            update(resId, tag, subject, new String(b, LanguageManager.STANDARD_CHARSET), "text/plain", locale, def, groupId, no_cc);
        } else {
            update(resId, tag, subject, new String(b, LanguageManager.STANDARD_CHARSET), "text/plain", locale, def, no_cc);
        }
    }

    public CustomEmailMessage(Integer cid, String s, String b, String contentType, String locale, int group_id, boolean no_cc) throws IOException {
        this(cid.intValue(), s, b, contentType, locale, group_id, no_cc);
    }

    public CustomEmailMessage(long id, String s, String b, String contentType, String locale, boolean no_cc) throws IOException {
        this(id, s, b, contentType, locale, 0, no_cc);
    }

    public CustomEmailMessage(long id, String s, String b, String contentType, String locale, int group_id, boolean no_cc) throws IOException {
        super(id);
        this.defaultTemplate = null;
        this.noCC = false;
        this.subjectStr = s;
        this.subject = new Template(new StringReader(HTMLEncoder.decode(s)));
        this.bodyStr = b;
        this.body = new Template(new StringReader(HTMLEncoder.decode(b)));
        this.contentType = contentType;
        this.locale = locale;
        this.groupId = group_id;
        this.noCC = no_cc;
    }

    public CustomEmailMessage(long id) {
        super(id);
        this.defaultTemplate = null;
        this.noCC = false;
    }

    public String getSubject(TemplateModelRoot root) {
        return Template2String.process(this.subject, root);
    }

    public String getBody(TemplateModelRoot root) {
        String _body = Template2String.process(this.body, root);
        log.debug("Message text: " + _body);
        return _body;
    }

    public Template getBodyTemplate() {
        return this.body;
    }

    public String getCharset() {
        return LanguageManager.STANDARD_CHARSET;
    }

    public void send(String address, int type, TemplateModelRoot root) throws Exception {
        Session.getMailer().sendMessage(address, type, getSubject(root), getBody(root), (String) null, getCharset(), this.contentType);
    }

    public void send(String address, TemplateModelRoot root, String replyTo, List cc) throws Exception {
        Session.getMailer().sendMessage(address, (List) null, cc, getSubject(root), getBody(root), getCharset(), this.contentType);
    }

    public void send(String address, int type, TemplateModelRoot root, String replyTo) throws Exception {
        Session.getMailer().sendMessage(address, type, getSubject(root), getBody(root), replyTo, getCharset(), this.contentType);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CustomEmailMessage$TemplateWrapper.class */
    public static class TemplateWrapper extends Template {
        public TemplateWrapper() {
        }

        public TemplateWrapper(String filePath) throws IOException {
            super(filePath);
        }

        public TemplateWrapper(File file) throws IOException {
            super(file);
        }

        public TemplateWrapper(InputStream stream) throws IOException {
            super(stream);
        }

        public TemplateWrapper(Reader stream) throws IOException {
            super(stream);
        }

        public boolean isStatusOK() {
            return this.buildError == null;
        }

        public String getErrorMessage() {
            if (this.buildError != null) {
                return this.buildError.getMessage();
            }
            return null;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CustomEmailMessage$DefaultEmailTemplate.class */
    public static class DefaultEmailTemplate {
        private static HashMap messages = new HashMap();
        private static HashMap groups = new HashMap();
        private String template;
        private String subject;
        private int groupId;
        private boolean massmailApplicable;
        private boolean noCC;
        private boolean adminOnly;

        public static void add(String tag, String template, String subject, String groupId, String massMailApplicable, boolean noCC, boolean adminOnly) {
            DefaultEmailTemplate obj = new DefaultEmailTemplate();
            obj.template = template;
            obj.subject = subject;
            obj.groupId = Integer.parseInt(groupId);
            obj.massmailApplicable = (massMailApplicable == null || massMailApplicable.length() == 0) ? false : true;
            obj.noCC = noCC;
            obj.adminOnly = adminOnly;
            messages.put(tag, obj);
        }

        public static void addGroup(String groupId, String groupName) {
            groups.put(new Integer(Integer.parseInt(groupId)), groupName);
        }

        public static String getTemplate(String tag) {
            DefaultEmailTemplate obj = (DefaultEmailTemplate) messages.get(tag);
            if (obj != null) {
                return obj.template;
            }
            return null;
        }

        public static String getSubject(String tag) {
            DefaultEmailTemplate obj = (DefaultEmailTemplate) messages.get(tag);
            if (obj != null) {
                return obj.subject;
            }
            return null;
        }

        public static int getGroupId(String tag) {
            DefaultEmailTemplate obj = (DefaultEmailTemplate) messages.get(tag);
            if (obj != null) {
                return obj.groupId;
            }
            return 0;
        }

        public static boolean isNoCC(String tag) {
            DefaultEmailTemplate obj = (DefaultEmailTemplate) messages.get(tag);
            return obj != null && obj.noCC;
        }

        public static boolean isAdminOnly(String tag) {
            DefaultEmailTemplate obj = (DefaultEmailTemplate) messages.get(tag);
            return obj != null && obj.adminOnly;
        }

        public static Set tags() {
            return messages.keySet();
        }

        public static HashMap groups() {
            return groups;
        }

        public static HashMap defaultTemplates() {
            return messages;
        }

        public int getGroupId() {
            return this.groupId;
        }

        public boolean isNoCC() {
            return this.noCC;
        }

        public boolean isMassMailApplicable() {
            return this.massmailApplicable;
        }

        public boolean isAdminOnly() {
            return this.adminOnly;
        }
    }

    public static CustomEmailMessage send(String tag, String email, TemplateModelRoot root) throws Exception {
        return send(tag, email, root, (List) null);
    }

    public static CustomEmailMessage send(String tag, String email, TemplateModelRoot root, List cc) throws Exception {
        CustomEmailMessage message = getMessage(tag, Session.getCurrentLocale().toString());
        List ccList = message.getCCList();
        if (cc != null) {
            ccList.addAll(cc);
        }
        Session.getMailer().sendMessage(email, (List) null, ccList, message.getSubject(root), message.getBody(root), message.getCharset(), message.contentType);
        return message;
    }

    public int getGroupId() {
        return this.groupId;
    }

    public List getCCList() throws Exception {
        List ccList = new ArrayList();
        if (!this.noCC) {
            List<HashMap> recipients = MailMan.getMailMan().getRecipients(this.groupId);
            if (recipients == null) {
                return ccList;
            }
            for (HashMap mail : recipients) {
                ccList.add(mail.get("email"));
            }
        }
        return ccList;
    }

    public boolean isNoCC() {
        return this.noCC;
    }

    public static CustomEmailMessage send(String tag, TemplateModelRoot root) throws Exception {
        return send(tag, (String) null, root, (List) null);
    }

    public static SimpleHash getDefaultRoot(Account account) throws Exception {
        SimpleHash root = new SimpleHash();
        root.put("account", account);
        root.put(FMACLManager.USER, account.getUser());
        root.put("plan", account.getPlan());
        root.put("toolbox", HsphereToolbox.toolbox);
        root.put("settings", new MapAdapter(Settings.get().getMap()));
        root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
        return root;
    }

    public void setNoCC(boolean noCC) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE custom_emails SET no_cc = ? WHERE id = ?");
            ps.setBoolean(1, noCC);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.noCC = noCC;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CustomEmailMessage$MessageIdLookupMap.class */
    public static class MessageIdLookupMap {
        private Map obj = new HashMap();

        MessageIdLookupMap() {
            load();
        }

        public Integer getId(MessageLookupKey key) {
            return (Integer) this.obj.get(key);
        }

        public synchronized void setId(MessageLookupKey key, Integer messageId) {
            this.obj.put(key, messageId);
        }

        public synchronized void resetId(MessageLookupKey key) {
            this.obj.remove(key);
        }

        private void load() {
            try {
                Connection con = Session.getDb();
                PreparedStatement ps = con.prepareStatement("SELECT id, reseller_id, tag, locale, def FROM custom_emails");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    MessageLookupKey key = rs.getInt("def") == 1 ? new MessageLookupKey(rs.getLong("reseller_id"), rs.getString("tag")) : new MessageLookupKey(rs.getLong("reseller_id"), rs.getString("tag"), rs.getString("locale"));
                    setId(key, new Integer(rs.getInt("id")));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Problem detected when loading 'MessageIdLookupMap' for Custom Emails: ", ex);
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/CustomEmailMessage$MessageLookupKey.class */
    public static class MessageLookupKey implements Serializable {
        long resellerId;
        String tag;
        String locale;

        public MessageLookupKey(long resellerId, String tag, String locale) {
            this.resellerId = resellerId;
            this.tag = tag;
            this.locale = "".equals(locale) ? null : locale;
        }

        public MessageLookupKey(long resellerId, String tag) {
            this(resellerId, tag, null);
        }

        public boolean equals(Object o) {
            if (o instanceof MessageLookupKey) {
                MessageLookupKey key = (MessageLookupKey) o;
                return this.resellerId == key.resellerId && (this.tag != null ? this.tag.equals(key.tag) : key.tag == null) && (this.locale != null ? this.locale.equals(key.locale) : key.locale == null);
            }
            return false;
        }

        public int hashCode() {
            return 0;
        }

        public String toString() {
            return "reseller_id #" + this.resellerId + "; tag '" + this.tag + "'; locale " + (this.locale == null ? "null" : "'" + this.locale + "'");
        }
    }
}
