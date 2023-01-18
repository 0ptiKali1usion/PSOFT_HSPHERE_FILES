package psoft.hsphere.resource.admin;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/SpamAssassinManager.class */
public class SpamAssassinManager extends Resource {
    private static HashMap RULES_MAP = new HashMap();

    public SpamAssassinManager(int type, Collection init) throws Exception {
        super(type, init);
    }

    public SpamAssassinManager(ResourceId rid) throws Exception {
        super(rid);
    }

    public TemplateModel FM_mail_servers() throws Exception {
        List l = new ArrayList();
        l.addAll(HostManager.getHostsByGroupType(3));
        return new ListAdapter(l);
    }

    public TemplateModel FM_getPrefs(long mserver) throws Exception {
        double fileSizeVal = 0.0d;
        TemplateMap spamassassin_prefs = new TemplateMap();
        HostEntry he = HostManager.getHost(mserver);
        Collection colres = he.exec("spamassassin-get-config.pl", new ArrayList());
        String res = (String) colres.iterator().next();
        StringTokenizer st = new StringTokenizer(res, "|");
        if (st.hasMoreTokens()) {
            spamassassin_prefs.put("required_score", new TemplateString(st.nextToken("|")));
        }
        if (st.hasMoreTokens()) {
            spamassassin_prefs.put("rewrite_header_subject", new TemplateString(st.nextToken("|")));
        }
        if (st.hasMoreTokens()) {
            spamassassin_prefs.put("report_safe", new TemplateString(st.nextToken("|")));
        }
        if (st.hasMoreTokens()) {
            spamassassin_prefs.put("clear_headers", new TemplateString(st.nextToken("|")));
        }
        if (st.hasMoreTokens()) {
            spamassassin_prefs.put("use_bayes", new TemplateString(st.nextToken("|")));
        }
        if (st.hasMoreTokens()) {
            spamassassin_prefs.put("use_auto_whitelist", new TemplateString(st.nextToken("|")));
        }
        if (st.hasMoreTokens()) {
            String fileSizeStr = st.nextToken("|");
            try {
                fileSizeVal = new Double(fileSizeStr).doubleValue();
            } catch (Exception e) {
                new HSUserException("spamassassin.param.samsgsize.get_error");
            }
            spamassassin_prefs.put("file_size", new TemplateString(new Double(Math.round((100.0d * fileSizeVal) / 1024.0d)).doubleValue() / 100.0d));
        }
        return spamassassin_prefs;
    }

    public TemplateModel FM_setPrefs(long mserverId, String requiredScore, String rewriteHeaderSubject, String reportSafe, String clearHeaders, String useBayes, String useAutoWhitelist, String fileSize) throws Exception {
        HostEntry he = HostManager.getHost(mserverId);
        List list = new ArrayList();
        list.add("--required_score");
        list.add(requiredScore);
        list.add("--rewrite_header_subject");
        list.add('\"' + rewriteHeaderSubject + '\"');
        list.add("--report_safe");
        list.add(reportSafe);
        clearHeaders = (clearHeaders == null || clearHeaders.equals("")) ? "0" : "0";
        list.add("--clear_headers");
        list.add(clearHeaders);
        useBayes = (useBayes == null || useBayes.equals("")) ? "0" : "0";
        list.add("--use_bayes");
        list.add(useBayes);
        useAutoWhitelist = (useAutoWhitelist == null || useAutoWhitelist.equals("")) ? "0" : "0";
        list.add("--use_auto_whitelist");
        list.add(useAutoWhitelist);
        double fileSizeValKb = 0.0d;
        try {
            fileSizeValKb = new Double(fileSize).doubleValue();
        } catch (Exception e) {
            new HSUserException("spamassassin.param.samsgsize.incorrect");
        }
        long fileSizeValByte = new Double(fileSizeValKb * 1024.0d).longValue();
        list.add("--file_size");
        list.add(new Long(fileSizeValByte).toString());
        he.exec("spamassassin-set-config.pl", list);
        return this;
    }

    public synchronized TemplateModel FM_addRulesCron(long mserverId, String trustedRulesets, String email) throws Exception {
        HostEntry he = HostManager.getHost(mserverId);
        List l = new ArrayList();
        l.add(trustedRulesets);
        l.add(email);
        he.exec("spamassassin-rules-set", l);
        addToRulesMap(mserverId, "status", "1");
        StringTokenizer st = new StringTokenizer(trustedRulesets, ",");
        String str = "";
        while (true) {
            String trustedRulesetsWithSpaces = str;
            if (st.hasMoreTokens()) {
                str = st.nextToken() + " " + trustedRulesetsWithSpaces;
            } else {
                addToRulesMap(mserverId, "sets", trustedRulesetsWithSpaces);
                addToRulesMap(mserverId, "email", email);
                return this;
            }
        }
    }

    public TemplateModel FM_delRulesCron(long mserverId) throws Exception {
        HostEntry he = HostManager.getHost(mserverId);
        he.exec("spamassassin-rules-del", new ArrayList());
        addToRulesMap(mserverId, "status", "0");
        addToRulesMap(mserverId, "sets", "");
        addToRulesMap(mserverId, "email", "");
        return this;
    }

    public TemplateModel FM_getRulesCronParam(long mserverId, String paramName) throws Exception {
        HashMap hm;
        Long mId = new Long(mserverId);
        Object o = RULES_MAP.get(mId);
        if (o == null) {
            initRulesMap(mserverId);
            hm = (HashMap) RULES_MAP.get(mId);
        } else {
            hm = (HashMap) o;
        }
        return new TemplateString(hm.get(paramName));
    }

    private void initRulesMap(long mserverId) throws Exception {
        HostEntry he = HostManager.getHost(mserverId);
        Iterator i = he.exec("spamassassin-rules-get.pl", new ArrayList()).iterator();
        if (i.hasNext()) {
            String str = (String) i.next();
            StringTokenizer st = new StringTokenizer(str, "|");
            if (st.hasMoreTokens()) {
                addToRulesMap(mserverId, "status", st.nextToken());
            }
            if (st.hasMoreTokens()) {
                addToRulesMap(mserverId, "sets", st.nextToken());
            }
            if (st.hasMoreTokens()) {
                addToRulesMap(mserverId, "email", st.nextToken());
            }
        }
    }

    private HashMap addToRulesMap(long mserverId, String paramName, String paramValue) throws Exception {
        HashMap hm;
        Long mId = new Long(mserverId);
        Object o = RULES_MAP.get(mId);
        if (o != null) {
            hm = (HashMap) o;
        } else {
            hm = new HashMap();
        }
        hm.put(paramName, paramValue);
        RULES_MAP.put(mId, hm);
        return RULES_MAP;
    }
}
