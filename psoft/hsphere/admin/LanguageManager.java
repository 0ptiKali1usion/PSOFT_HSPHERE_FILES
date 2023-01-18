package psoft.hsphere.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.hsphere.util.PackageConfigurator;

/* loaded from: hsphere.zip:psoft/hsphere/admin/LanguageManager.class */
public class LanguageManager {
    protected static Collection langList;
    protected static Map langMap;
    public static final String STANDARD_CHARSET = "UTF-8";

    public static String getMailCharset(String locale) {
        initList();
        LanguageToken token = (LanguageToken) langMap.get(locale);
        if (token != null) {
            return token.getMailCharset();
        }
        Session.getLog().debug("Unable to get a charset for locale '" + locale + "'. The standard encoding '" + STANDARD_CHARSET + "' will be used instead.");
        return STANDARD_CHARSET;
    }

    public static Collection getLanguageList() {
        initList();
        return langList;
    }

    protected static void initList() {
        synchronized (LanguageManager.class) {
            if (langList == null) {
                List<String> tmpLangList = PackageConfigurator.getPackageProperties("LANG_LIST");
                StringBuffer availLocales = new StringBuffer(Session.getProperty("LANG_LIST"));
                if (tmpLangList != null) {
                    for (String o : tmpLangList) {
                        Session.getLog().debug("Locale from package is -> " + o);
                        availLocales.append(" ");
                        availLocales.append(o);
                    }
                }
                initList(availLocales.toString());
            }
        }
    }

    protected static void initList(String str) {
        StringTokenizer st = new StringTokenizer(str);
        langList = new ArrayList();
        langMap = new HashMap();
        while (st.hasMoreTokens()) {
            LanguageToken token = new LanguageToken(st.nextToken());
            langList.add(token);
            langMap.put(token.locale.toString(), token);
        }
    }
}
