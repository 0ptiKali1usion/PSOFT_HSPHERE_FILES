package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/TemplateErrorResult.class */
public class TemplateErrorResult implements TemplateHashModel {
    protected TemplateString msg;
    protected TemplateString code;
    protected static final TemplateString error = new TemplateString("ERROR");

    public TemplateErrorResult(Throwable t) {
        this(t.getMessage(), "");
    }

    public TemplateErrorResult(Throwable t, String code) {
        this(t.getMessage(), code);
    }

    public TemplateErrorResult(String msg) {
        this(msg, "");
    }

    public TemplateErrorResult(String msg, String code) {
        this.msg = new TemplateString(msg);
        this.code = new TemplateString(code);
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return key.equals("status") ? error : ("msg".equals(key) || "msgs".equals(key)) ? this.msg : key.equals("code") ? this.code : this;
    }
}
