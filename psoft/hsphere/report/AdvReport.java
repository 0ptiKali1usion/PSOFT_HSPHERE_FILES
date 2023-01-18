package psoft.hsphere.report;

import freemarker.template.SimpleHash;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import psoft.hsphere.Account;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.Bill;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.HSUserException;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Language;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.User;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.util.XMLHashtableTranslator;
import psoft.util.NFUCache;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.MapAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMethodWrapper;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/AdvReport.class */
public abstract class AdvReport implements TemplateHashModel, TemplateMethodModel {

    /* renamed from: id */
    protected int f116id;
    protected int size;
    protected int step = 10;
    protected int startPos;
    protected int endPos;
    protected int currentPage;
    protected int lastPage;
    protected DataContainer data;
    protected TemplateModel field;
    protected TemplateModel direction;
    protected TemplateList pages;
    protected static NFUCache cache = new NFUCache(20);
    protected static int idseq = 0;
    protected static Hashtable classHolder = null;
    public static final TemplateString STATUS_OK = new TemplateString("OK");

    public abstract void init(List list) throws Exception;

    protected static synchronized int getNewId() {
        int i = idseq + 1;
        idseq = i;
        return i;
    }

    protected static String getCacheKey(int id) {
        try {
            return Integer.toString(id) + "|" + Session.getAccountId();
        } catch (Exception e) {
            Session.getLog().error("Unable to retrieve reseller ID:", e);
            return "";
        }
    }

    public static AdvReport getReport(int id) throws Exception {
        AdvReport rep = (AdvReport) cache.get(getCacheKey(id));
        if (rep == null) {
            throw new HSUserException("advreport.expired");
        }
        return (AdvReport) cache.get(getCacheKey(id));
    }

    protected void init() {
    }

    public static AdvReport newInstance(String name) {
        if (classHolder == null) {
            try {
                XMLHashtableTranslator translator = new XMLHashtableTranslator("ADV_REPORTS");
                classHolder = translator.translate("report", "name", "class");
            } catch (Exception e) {
                Session.getLog().error("Impossible to get 'function' from 'functions.xml'", e);
                classHolder = new Hashtable();
            }
        }
        Class c = null;
        try {
            c = Class.forName((String) classHolder.get(name));
        } catch (Exception e2) {
        }
        Session.getLog().info("getting new instance for :" + name + "-->" + c);
        if (c == null) {
            return null;
        }
        try {
            AdvReport report = (AdvReport) c.newInstance();
            return report;
        } catch (Exception e3) {
            e3.printStackTrace();
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) {
        try {
            init(HTMLEncoder.decode(l));
            return this;
        } catch (Exception e) {
            Session.getLog().debug("Error getting report: ", e);
            return new TemplateErrorResult(e);
        }
    }

    public void init(DataContainer data) {
        Session.getLog().info("inside init");
        this.f116id = getNewId();
        this.data = data;
        this.size = data.size();
        initPageList();
        setPosition(0);
        cache.put(getCacheKey(this.f116id), this);
        Session.getLog().info("out of init");
    }

    public TemplateModel FM_setStep(int step) {
        setStep(step);
        return this;
    }

    protected void setStep(int step) {
        this.step = step;
        initPageList();
        setPosition(this.startPos);
    }

    public TemplateModel FM_reorder(String fieldName, boolean asc) {
        try {
            Session.getLog().info("--reorder->" + fieldName + ":" + asc);
            this.data.reorder(fieldName, asc);
            setPosition(0);
            this.field = new TemplateString(fieldName);
            this.direction = new TemplateString(asc);
            Session.getLog().info("--reorder->" + fieldName + ":" + asc + " -done");
            return this;
        } catch (Throwable t) {
            Session.getLog().error("reorder error:" + fieldName, t);
            return null;
        }
    }

    public void setOrderParams(String fieldName, boolean asc) {
        this.field = new TemplateString(fieldName);
        this.direction = new TemplateString(asc);
    }

    public TemplateModel FM_setPosition(int pos) {
        setPosition(pos);
        return this;
    }

    public TemplateModel FM_setPage(int page) {
        setPosition(((page < this.lastPage ? page : this.lastPage) - 1) * this.step);
        return null;
    }

    public void setPosition(int pos) {
        this.startPos = pos;
        if (this.startPos < 0) {
            this.startPos = 0;
        }
        this.endPos = this.startPos + this.step;
        if (this.endPos > this.size) {
            this.endPos = this.size;
        }
        this.currentPage = (this.startPos / this.step) + 1;
    }

    protected TemplateModel sublist() {
        if (this.startPos == this.endPos) {
            return null;
        }
        try {
            List l = this.data.subList(this.startPos, this.endPos);
            return new ListAdapter(l);
        } catch (IndexOutOfBoundsException ioofbe) {
            Session.getLog().error("index out of bounds", ioofbe);
            return null;
        }
    }

    public TemplateModel FM_all() {
        return new ListAdapter(this.data.all());
    }

    public TemplateModel FM_page() {
        return sublist();
    }

    public TemplateModel FM_next() {
        setPosition(this.endPos);
        return sublist();
    }

    public TemplateModel FM_prev() {
        setPosition(this.startPos - this.step);
        return sublist();
    }

    public void initPageList() {
        TemplateList tl = new TemplateList();
        int count = 0;
        int i = 0;
        while (true) {
            int i2 = i;
            if (i2 < this.data.size()) {
                count++;
                tl.add((TemplateModel) new TemplateString(count));
                i = i2 + this.step;
            } else {
                this.pages = tl;
                this.lastPage = count;
                return;
            }
        }
    }

    public TemplateModel get(String key) {
        if ("status".equals(key)) {
            return STATUS_OK;
        }
        if ("id".equals(key)) {
            return new TemplateString(this.f116id);
        }
        if ("size".equals(key)) {
            return new TemplateString(this.data.size());
        }
        if ("isNext".equals(key)) {
            return new TemplateString(this.endPos < this.data.size());
        } else if ("isPrev".equals(key)) {
            return new TemplateString(this.startPos > 0);
        } else if ("pages".equals(key)) {
            return this.pages;
        } else {
            if ("currentPage".equals(key)) {
                return new TemplateString(this.currentPage);
            }
            if ("field".equals(key)) {
                return this.field;
            }
            if ("direction".equals(key)) {
                return this.direction;
            }
            try {
                return TemplateMethodWrapper.getMethod(this, key);
            } catch (IllegalArgumentException iae) {
                Session.getLog().info("GET ERROR", iae);
                return null;
            }
        }
    }

    public TemplateModel massMail(String from, String sbj, String msg, String tmplt, String useSettings, boolean oepa, String contentType) throws Exception {
        Template messageTemplate;
        Language oldLang = Session.getLanguage();
        Template subjectTemplate = new Template(new StringReader(sbj));
        if (tmplt != null && !"".equals(tmplt)) {
            CustomEmailMessage tmp_msg = CustomEmailMessage.getMessage(tmplt);
            messageTemplate = tmp_msg.getBodyTemplate();
        } else {
            messageTemplate = new Template(new StringReader(msg));
        }
        Set sentSet = new HashSet();
        int empty = 0;
        int duplicate = 0;
        int sent = 0;
        int errors = 0;
        for (Object obj : this.data.all()) {
            SimpleHash root = new SimpleHash();
            if (obj instanceof Map) {
                Map map = (Map) obj;
                String address = (String) map.get("email");
                long account_id = ((Long) map.get("accountId")).longValue();
                if (address == null || address.length() == 0 || address.equals("N/A")) {
                    empty++;
                } else if (oepa && sentSet.contains(address)) {
                    duplicate++;
                } else {
                    if (oepa) {
                        sentSet.add(address);
                    }
                    root.put("entry", new MapAdapter(map));
                    root.put("toolbox", ReportToolbox.toolbox);
                    try {
                        Session.getLog().info("Sending message to: " + address);
                        Session.getMailer().sendMessage(from, address, getString(root, subjectTemplate, account_id), getString(root, messageTemplate, account_id), Session.getCurrentCharset(), contentType);
                        sent++;
                    } catch (Exception e) {
                        errors++;
                        Session.getLog().warn("Error sending message to: " + address, e);
                    }
                }
            } else {
                throw new Exception("Invalid AdvReport, Mass Mail not supported");
            }
        }
        SimpleHash result = new SimpleHash();
        result.put("sent", new TemplateString(sent));
        result.put("errors", new TemplateString(errors));
        result.put("empty", new TemplateString(empty));
        if (oepa) {
            result.put("duplicate", new TemplateString(duplicate));
        }
        Session.setLanguage(oldLang);
        return result;
    }

    private String getString(TemplateModelRoot root, Template t, long accountId) throws Exception {
        DateFormat dateFormat = DateFormat.getDateInstance();
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Account oldAccount = Session.getAccount();
        User oldUser = Session.getUser();
        try {
            Account a = (Account) Account.get(new ResourceId(accountId, 0));
            User user = a.getUser();
            Session.setUser(user);
            Session.setAccount(a);
            root.put(FMACLManager.USER, user);
            root.put("account", a);
            root.put("hstoolbox", HsphereToolbox.toolbox);
            root.put("toolbox", HsphereToolbox.toolbox);
            root.put("settings", new MapAdapter(Settings.get().getMap()));
            root.put("current_date", new TemplateString(dateFormat.format(TimeUtils.getDate())));
            root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
            Bill bill = a.getBill();
            if (bill != null) {
                root.put("bill", bill);
            }
            t.process(root, out);
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            out.flush();
            out.close();
            return sw.toString();
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public int getId() {
        return this.f116id;
    }

    public boolean isParameterEmpty(String param) {
        return param == null || "null".equals(param);
    }
}
