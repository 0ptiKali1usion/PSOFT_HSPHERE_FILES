package psoft.hsphere.functions;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import psoft.util.freemarker.TemplateString;

/* compiled from: DiskUsageDetailed.java */
/* loaded from: hsphere.zip:psoft/hsphere/functions/UsageInfo.class */
class UsageInfo implements TemplateHashModel {
    private String serverName = null;
    private String resourceDescription;
    private float used;

    public UsageInfo(String resourceDescription, float used) {
        this.resourceDescription = null;
        this.used = 0.0f;
        this.resourceDescription = resourceDescription;
        this.used = used;
    }

    public TemplateModel get(String key) {
        if (key.equals("description")) {
            return new TemplateString(this.resourceDescription);
        }
        if (key.equals("usage")) {
            return new TemplateString(Float.toString(this.used));
        }
        return null;
    }

    public boolean isEmpty() {
        return false;
    }
}
