package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import freemarker.template.TemplateScalarModel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.util.USFormat;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/EmailManager.class */
public class EmailManager implements TemplateHashModel {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    protected String status = "OK";
    protected String errorMsg = null;
    private static Category log = Category.getInstance(EmailManager.class.getName());

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel FM_isDefault(String tag, String locale) throws Exception {
        Integer id = CustomEmailMessage.getMessageId(tag, locale);
        Integer defId = CustomEmailMessage.getMessageIdLookupMap().getId(new CustomEmailMessage.MessageLookupKey(Session.getResellerId(), tag));
        if (defId == null) {
            defId = CustomEmailMessage.getMessageIdLookupMap().getId(new CustomEmailMessage.MessageLookupKey(0L, tag));
        }
        log.info("####" + id + ":" + defId);
        if (defId != null && defId.equals(id)) {
            return TemplateString.TRUE;
        }
        return null;
    }

    public CustomEmailMessage FM_getMessage(String tag, String locale) throws Exception {
        Integer id = CustomEmailMessage.getMessageIdLookupMap().getId(new CustomEmailMessage.MessageLookupKey(Session.getResellerId(), tag, locale));
        log.info("---->" + tag + ":" + locale + ":" + id);
        if (id != null) {
            return CustomEmailMessage.getMessage(id);
        }
        return null;
    }

    public TemplateModel FM_update(String tag, String subject, String body, String contentType, String locale, boolean def, int group_id, boolean no_cc) throws Exception {
        try {
            CustomEmailMessage.update(Session.getResellerId(), tag, subject, body, contentType, locale, def, group_id, no_cc);
            this.status = "OK";
        } catch (Exception e) {
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public TemplateModel FM_restoreTemplate(String tag, String locale, boolean def, boolean no_cc) throws Exception {
        try {
            CustomEmailMessage.updateFromTemplate(Session.getResellerId(), tag, locale, def, no_cc);
            this.status = "OK";
        } catch (Exception e) {
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public CustomEmailMessage FM_getDefaultMessage(String tag, String locale) throws Exception {
        return CustomEmailMessage.getMessage(tag, locale);
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("status".equals(key)) {
            return new TemplateString(this.status);
        }
        if ("msg".equals(key)) {
            return new TemplateString(this.errorMsg);
        }
        if ("groups".equals(key)) {
            HashMap grps = CustomEmailMessage.getGroups();
            List groups = new ArrayList();
            TreeSet gr_set = new TreeSet(grps.keySet());
            Iterator iterator = gr_set.iterator();
            while (iterator.hasNext()) {
                TemplateHash th = new TemplateHash();
                Integer id = (Integer) iterator.next();
                String name = (String) grps.get(id);
                th.put("id", String.valueOf(id));
                th.put("name", Localizer.translateLabel(name));
                groups.add(th);
            }
            return new TemplateList(groups);
        } else if ("templates".equals(key)) {
            HashMap tmplts = CustomEmailMessage.getDefaultTemplates();
            List templates = new ArrayList();
            TreeSet t_set = new TreeSet(tmplts.keySet());
            Iterator iterator2 = t_set.iterator();
            while (iterator2.hasNext()) {
                TemplateHash th2 = new TemplateHash();
                String tag = (String) iterator2.next();
                CustomEmailMessage.DefaultEmailTemplate template = (CustomEmailMessage.DefaultEmailTemplate) tmplts.get(tag);
                Integer group_id = new Integer(template.getGroupId());
                th2.put("tag", new TemplateString(tag));
                th2.put("group_id", new TemplateString(group_id));
                th2.put("no_cc", new TemplateString(String.valueOf(template.isNoCC())));
                templates.add(th2);
            }
            return new TemplateList(templates);
        } else if ("massmail_templates".equals(key)) {
            List mmTemplates = new ArrayList();
            HashMap templateMap = CustomEmailMessage.getDefaultTemplates();
            Set<String> templateSet = templateMap.keySet();
            for (String tag2 : templateSet) {
                CustomEmailMessage.DefaultEmailTemplate template2 = (CustomEmailMessage.DefaultEmailTemplate) templateMap.get(tag2);
                if (template2.isMassMailApplicable()) {
                    Integer group_id2 = new Integer(template2.getGroupId());
                    TemplateHash th3 = new TemplateHash();
                    th3.put("tag", new TemplateString(tag2));
                    th3.put("group_id", new TemplateString(group_id2));
                    mmTemplates.add(th3);
                }
            }
            return new TemplateList(mmTemplates);
        } else {
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public TemplateModel FM_previewEmailSubject(String messageTag, String locale) throws Exception {
        String subject = null;
        try {
            CustomEmailMessage message = CustomEmailMessage.getMessage(messageTag, locale);
            subject = message.getSubject(new TestModelRoot());
        } catch (Throwable se) {
            Session.getLog().warn("Unable to get the message subject.", se);
        }
        return new TemplateString(subject);
    }

    public TemplateModel FM_previewEmailBody(String messageTag, String locale) throws Exception {
        String body = null;
        try {
            CustomEmailMessage message = CustomEmailMessage.getMessage(messageTag, locale);
            body = message.getBody(new TestModelRoot());
        } catch (Throwable se) {
            Session.getLog().warn("Unable to get the message body.", se);
        }
        return new TemplateString(body);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/EmailManager$TestModelRoot.class */
    public class TestModelRoot implements TemplateModelRoot {
        TemplateModel toolbox = null;
        TemplateModel config = null;
        TemplateModel settings = null;
        TemplateModel date = null;
        TemplateModel lang = null;

        public TestModelRoot() {
            EmailManager.this = r4;
        }

        public void put(String s, TemplateModel templateModel) {
        }

        public void remove(String s) {
        }

        public TemplateModel get(String key) throws TemplateModelException {
            try {
            } catch (Exception e) {
                Session.getLog().warn("Error", e);
            } catch (Throwable t) {
                Session.getLog().warn("Error", t);
            }
            if ("toolbox".equals(key)) {
                if (this.toolbox == null) {
                    this.toolbox = new TestToolbox();
                }
                return this.toolbox;
            } else if ("settings".equals(key)) {
                if (this.settings == null) {
                    this.settings = new MapAdapter(Settings.get().getMap());
                }
                return this.settings;
            } else if ("date".equals(key)) {
                if (this.date == null) {
                    this.date = new TemplateString(new Date().toLocaleString());
                }
                return this.date;
            } else if (AccountPreferences.LANGUAGE.equals(key)) {
                if (this.lang == null) {
                    this.lang = new HSLingualScalar();
                }
                return this.lang;
            } else {
                if (key.equals("root")) {
                    return this;
                }
                return new TestObject(key);
            }
        }

        public boolean isEmpty() throws TemplateModelException {
            return false;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/EmailManager$TestToolbox.class */
    public class TestToolbox extends HsphereToolbox {
        List supportedSuperKeys = Arrays.asList("now", "random", "multiplier", "current_lang", "current_country", "reseller_id", "compose", "strlit_encode", "counter", "startsWith", "endsWith", "consists", "substring", "tokenizer", "shrink_string", "date", "numberToCurLocale");

        public TestToolbox() {
            EmailManager.this = r7;
        }

        @Override // psoft.hsphere.HsphereToolbox, psoft.util.freemarker.Toolbox
        public TemplateModel get(String key) {
            if (key == null || "".equals(key)) {
                return null;
            }
            if (key.equals("currency")) {
                return new HSCurrencyDumb();
            }
            if (this.supportedSuperKeys.contains(key)) {
                try {
                    return super.get(key);
                } catch (Exception e) {
                }
            }
            return new TestObject(key.toUpperCase());
        }

        @Override // psoft.hsphere.HsphereToolbox
        public TemplateModel FM_numberToCurLocale(String str, String useGrouping) throws Exception {
            double tmpValue;
            try {
                try {
                    tmpValue = USFormat.parseDouble(str);
                } catch (Exception e) {
                    return null;
                }
            } catch (Exception e2) {
                tmpValue = 0.0d;
            }
            DecimalFormat df = getLocalizedDecimalFormat();
            String tmp = df.format(tmpValue);
            return new TemplateString(tmp);
        }

        /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/EmailManager$TestToolbox$HSCurrencyDumb.class */
        class HSCurrencyDumb implements TemplateMethodModel {
            HSCurrencyDumb() {
                TestToolbox.this = r4;
            }

            public boolean isEmpty() {
                return false;
            }

            public TemplateModel exec(List l) throws TemplateModelException {
                double value;
                try {
                    try {
                        value = USFormat.parseDouble((String) HTMLEncoder.decode(l).get(0));
                    } catch (Exception e) {
                        value = 0.0d;
                    }
                    return new TemplateString(HsphereToolbox.translateCurrency(value));
                } catch (Throwable th) {
                    return new TemplateString("0");
                }
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/EmailManager$TestObject.class */
    public class TestObject implements TemplateHashModel, TemplateMethodModel, TemplateScalarModel, TemplateListModel {
        int listIndex;
        String key;

        public TestObject() {
            EmailManager.this = r4;
            this.listIndex = 0;
            this.key = null;
        }

        public TestObject(String key) {
            EmailManager.this = r4;
            this.listIndex = 0;
            this.key = null;
            this.key = key;
        }

        public boolean isEmpty() throws TemplateModelException {
            return false;
        }

        public String getAsString() {
            if (this.key == null || "".equals(this.key)) {
                return "TEST";
            }
            if (this.key.indexOf("amount") >= 0 || this.key.indexOf("sum") >= 0 || this.key.indexOf("balance") >= 0 || this.key.indexOf("total") >= 0 || this.key.indexOf("subtotal") >= 0 || this.key.indexOf("size") >= 0) {
                return "3";
            }
            if (this.key.indexOf("name") >= 0) {
                if (this.key.indexOf("first") >= 0) {
                    return "FirstName";
                }
                if (this.key.indexOf("last") >= 0) {
                    return "LastName";
                }
            }
            return this.key.toUpperCase();
        }

        public TemplateModel get(String key) throws TemplateModelException {
            return new TestObject(this.key != null ? this.key + "." + key : key);
        }

        public TemplateModel exec(List list) throws TemplateModelException {
            return new TestObject(this.key);
        }

        public void rewind() throws TemplateModelException {
            this.listIndex = 0;
        }

        public boolean isRewound() throws TemplateModelException {
            return this.listIndex == 0;
        }

        public boolean hasNext() throws TemplateModelException {
            return this.listIndex < 3;
        }

        public TemplateModel next() throws TemplateModelException {
            this.listIndex++;
            return this;
        }

        public TemplateModel get(int i) throws TemplateModelException {
            return this;
        }
    }

    public TemplateList FM_getTemplates(int groupId) throws Exception {
        HashMap tmplts = CustomEmailMessage.getDefaultTemplates();
        List templates = new ArrayList();
        TreeSet t_set = new TreeSet(tmplts.keySet());
        Iterator iterator = t_set.iterator();
        while (iterator.hasNext()) {
            String tag = (String) iterator.next();
            CustomEmailMessage template = CustomEmailMessage.getMessage(tag);
            if (template.getGroupId() == groupId && (Session.getResellerId() == 1 || !CustomEmailMessage.DefaultEmailTemplate.isAdminOnly(tag))) {
                TemplateHash th = new TemplateHash();
                Integer group_id = new Integer(template.getGroupId());
                th.put("tag", new TemplateString(tag));
                th.put("group_id", new TemplateString(group_id));
                th.put("no_cc", new TemplateString(String.valueOf(template.isNoCC())));
                templates.add(th);
            }
        }
        return new TemplateList(templates);
    }

    public TemplateModel FM_setNoCC(String tag, boolean noCC) throws Exception {
        CustomEmailMessage message = CustomEmailMessage.getMessage(tag);
        message.setNoCC(noCC);
        return this;
    }

    public TemplateModel FM_getGroup(int groupId) throws Exception {
        HashMap groups = CustomEmailMessage.getGroups();
        return new TemplateString(groups.get(new Integer(groupId)));
    }
}
