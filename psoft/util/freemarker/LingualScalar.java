package psoft.util.freemarker;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.ResourceBundle;
import psoft.util.Config;

/* loaded from: hsphere.zip:psoft/util/freemarker/LingualScalar.class */
public class LingualScalar implements TemplateHashModel, TemplateScalarModel {
    protected StringBuffer buf;

    public boolean isEmpty() {
        return false;
    }

    public LingualScalar() {
        this.buf = null;
    }

    public LingualScalar(String key) {
        this.buf = new StringBuffer(key == null ? "" : key);
    }

    public String getAsString() {
        ResourceBundle rb = getLangBundle();
        String key = "";
        try {
            key = this.buf.toString();
            String s = rb.getString(key);
            if (null == s) {
                return null;
            }
            return fixEncoding(s);
        } catch (Exception e) {
            Config.getLog("LANG").warn("Was trying to get " + key + " from " + rb, e);
            return key;
        }
    }

    protected ResourceBundle getLangBundle() {
        return (ResourceBundle) Config.get("LANG").get("TEXT");
    }

    public TemplateModel get(String key) {
        if (this.buf == null) {
            return new LingualScalar(key);
        }
        this.buf.append(".").append(key);
        return this;
    }

    protected String fixEncoding(String raw) {
        String encoding = (String) Config.get("LANG").get("ENCODING");
        if (null == encoding) {
            return raw;
        }
        try {
            String s = new String(raw.getBytes(), encoding);
            return s;
        } catch (UnsupportedEncodingException e) {
            return raw;
        }
    }

    public String getCharset() {
        String charset = (String) Config.get("LANG").get("CHARSET");
        return null != charset ? charset : "ISO-8859-1";
    }

    public Locale getLocale() {
        Locale locale = (Locale) Config.get("LANG").get("LOCALE");
        return null != locale ? locale : Locale.US;
    }
}
