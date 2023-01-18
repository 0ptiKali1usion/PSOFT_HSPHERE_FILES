package psoft.hsphere;

import freemarker.template.TemplateScalarModel;
import java.util.Locale;
import org.apache.log4j.Category;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.admin.Settings;

/* loaded from: hsphere.zip:psoft/hsphere/Language.class */
public class Language {
    private static Category log = Category.getInstance(Language.class.getName());
    static String charset = LanguageManager.STANDARD_CHARSET;
    Locale locale;
    boolean parsed;

    public Charset getCharsetWrapper() {
        return new Charset();
    }

    public LocaleStr getLocaleWrapper() {
        return new LocaleStr();
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Language$Charset.class */
    public class Charset implements TemplateScalarModel {
        public Charset() {
            Language.this = r4;
        }

        public String getAsString() {
            return Language.this.getCharset();
        }

        public boolean isEmpty() {
            return false;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/Language$LocaleStr.class */
    public class LocaleStr implements TemplateScalarModel {
        public LocaleStr() {
            Language.this = r4;
        }

        public String getAsString() {
            return Language.this.getLocale().toString();
        }

        public boolean isEmpty() {
            return false;
        }
    }

    public Language(String token) {
        parse(token);
    }

    public String getCharset() {
        init();
        return charset;
    }

    public Locale getLocale() {
        init();
        return this.locale;
    }

    public synchronized void init() {
        if (this.parsed) {
            return;
        }
        Account a = Session.getAccount();
        if (a != null) {
            parse(a.getPreferences().getValueSafe(AccountPreferences.LANGUAGE));
        }
        if (this.parsed) {
            return;
        }
        parse(Settings.get().getValue(AccountPreferences.LANGUAGE));
        if (this.parsed) {
            return;
        }
        parseLocale(Session.getPropertyString("LOCALE"));
        if (this.parsed) {
            return;
        }
        parseDefault();
    }

    private void parse(String curlang) {
        if (curlang != null && curlang.length() > 0) {
            this.locale = Localizer.getLocaleFromString(curlang);
            this.parsed = true;
        }
    }

    private void parseLocale(String tmpLocale) {
        if (tmpLocale != null && tmpLocale.length() > 0) {
            this.locale = new Locale(tmpLocale.substring(0, 2), tmpLocale.substring(3, 5));
            this.parsed = true;
        }
    }

    private void parseDefault() {
        this.locale = Locale.getDefault();
    }
}
