package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/TemplateOKResult.class */
public class TemplateOKResult implements TemplateHashModel {
    protected TemplateString msg;

    /* renamed from: ok */
    protected static final TemplateString f55ok = new TemplateString("OK");

    public TemplateOKResult(String msg) {
        this.msg = new TemplateString(msg);
    }

    public TemplateOKResult() {
        this("");
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return key.equals("status") ? f55ok : key.equals("msg") ? this.msg : this;
    }
}
