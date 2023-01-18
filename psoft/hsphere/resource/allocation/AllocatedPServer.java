package psoft.hsphere.resource.allocation;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import psoft.hsphere.ResourceId;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/allocation/AllocatedPServer.class */
public class AllocatedPServer implements TemplateHashModel {
    public static final int AVAILABLE = 1;
    public static final int TAKEN = 2;

    /* renamed from: id */
    private long f179id;
    private int planId;
    private long resellerId;
    private ResourceId usedBy;

    public AllocatedPServer(long _id, int _planId, long _resellerId, ResourceId _usedBy) {
        this.f179id = _id;
        this.planId = _planId;
        this.resellerId = _resellerId;
        this.usedBy = _usedBy;
    }

    public long getId() {
        return this.f179id;
    }

    public void setId(long id) {
        this.f179id = id;
    }

    public int getPlanId() {
        return this.planId;
    }

    public void setPlanId(int _planId) {
        this.planId = _planId;
    }

    public long getResellerId() {
        return this.resellerId;
    }

    public void setResellerId(long resellerId) {
        this.resellerId = resellerId;
    }

    public ResourceId getUsedBy() {
        return this.usedBy;
    }

    public void setUsedBy(ResourceId _usedBy) {
        this.usedBy = _usedBy;
    }

    public int getState() {
        if (getPlanId() <= 0 || getResellerId() <= 0) {
            return getPlanId() > 0 ? 1 : -1;
        }
        return 2;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("state".equals(key)) {
            return new TemplateString(getState());
        }
        if ("plan_id".equals(key)) {
            return new TemplateString(getPlanId());
        }
        if ("reseller_id".equals(key)) {
            return new TemplateString(getResellerId());
        }
        if ("rid".equals(key)) {
            return getUsedBy();
        }
        return null;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
