package psoft.hsphere.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/admin/LanguageToken.class */
public class LanguageToken implements TemplateHashModel {
    String code;
    String desc;
    String charset;
    String mailCharset;
    String country;
    String language;
    String locale;

    public String getMailCharset() {
        if (this.mailCharset == null) {
            return this.charset;
        }
        if (this.mailCharset.length() == 0) {
            return this.charset;
        }
        return this.mailCharset;
    }

    public LanguageToken(String str) {
        int pos = str.indexOf(58);
        this.code = str.substring(0, pos);
        this.desc = str.substring(pos + 1);
        this.charset = Localizer.getCharsetFromString(this.code);
        this.mailCharset = Localizer.getMailCharsetFromString(this.code);
        this.country = Localizer.getCountryFromString(this.code);
        this.language = Localizer.getLanguageFromString(this.code);
        this.locale = Localizer.getLocaleFromString(this.code).toString();
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("code".equals(key)) {
            return new TemplateString(this.code);
        }
        if ("desc".equals(key)) {
            return new TemplateString(this.desc);
        }
        if ("charset".equals(key)) {
            return new TemplateString(this.charset);
        }
        if ("mailCharset".equals(key)) {
            return new TemplateString(this.mailCharset);
        }
        if ("country".equals(key)) {
            return new TemplateString(this.country);
        }
        if ("language".equals(key)) {
            return new TemplateString(this.language);
        }
        if ("locale".equals(key)) {
            return new TemplateString(this.locale);
        }
        if ("isDefault".equals(key)) {
            return new TemplateString(this.code.indexOf(Session.getCurrentLocale().toString()) != -1);
        }
        return null;
    }
}
