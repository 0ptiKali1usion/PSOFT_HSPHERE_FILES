package psoft.hsphere.resource.p003ds;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.p001ds.DedicatedServerState;

/* renamed from: psoft.hsphere.resource.ds.DedicatedServerResource */
/* loaded from: hsphere.zip:psoft/hsphere/resource/ds/DedicatedServerResource.class */
public class DedicatedServerResource extends Resource {
    private long serverOrTemplateId;
    private String initState;

    /* renamed from: ds */
    private DedicatedServer f190ds;

    @Override // psoft.hsphere.Resource
    public Collection getCurrentInitValues() {
        List values = new ArrayList();
        values.add(Long.toString(this.serverOrTemplateId));
        return values;
    }

    public DedicatedServerResource(int id, Collection values) throws Exception {
        super(id, values);
        this.serverOrTemplateId = 0L;
        this.initState = null;
        this.f190ds = null;
        Iterator i = values.iterator();
        this.serverOrTemplateId = Long.parseLong((String) i.next());
        if (i.hasNext()) {
            this.initState = (String) i.next();
            if ("".equals(this.initState)) {
                this.initState = null;
            }
        }
        this.f190ds = DSHolder.getDedicatedServerObject(this.serverOrTemplateId);
    }

    public DedicatedServerResource(ResourceId rid) throws Exception {
        super(rid);
        this.serverOrTemplateId = 0L;
        this.initState = null;
        this.f190ds = null;
        this.f190ds = DSHolder.getServerByRid(rid.getId());
        if (this.f190ds != null) {
            if (this.f190ds.isTemplatedServer()) {
                this.serverOrTemplateId = this.f190ds.getParent().getId();
            } else {
                this.serverOrTemplateId = this.f190ds.getId();
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        DedicatedServerState state = this.initState != null ? DedicatedServerState.get(this.initState) : DedicatedServerState.IN_USE;
        Session.getLog().debug("initDoine(). State will be set to '" + state.toString() + "'.");
        this.f190ds = DSHolder.lockDedicatedServer(this.serverOrTemplateId, getId(), state);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (this.f190ds.get(key) != null) {
            return this.f190ds.get(key);
        }
        return super.get(key);
    }

    public DedicatedServer getDSObject() {
        return this.f190ds;
    }

    @Override // psoft.hsphere.Resource
    public void suspend() throws Exception {
        if (this.suspended) {
            return;
        }
        super.suspend();
        this.f190ds.setState(DedicatedServerState.ON_HOLD);
    }

    @Override // psoft.hsphere.Resource
    public void resume() throws Exception {
        if (this.suspended) {
            super.resume();
            this.f190ds.setState(DedicatedServerState.IN_USE);
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        if (this.f190ds != null) {
            DSHolder.unlockDedicatedServer(this.f190ds.getId(), DedicatedServerState.CLEAN_UP);
        }
    }

    public static String getDescription(InitToken token) throws Exception {
        long _id = Long.parseLong((String) token.getValues().iterator().next());
        DedicatedServer _ds = DSHolder.getDedicatedServerObject(_id);
        if (_ds == null) {
            return Resource.getDescription(token);
        }
        return _ds.getName();
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_recurrent", new Object[]{getPeriodInWords(), getDescription(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        String[] strArr = new String[1];
        strArr[0] = this.f190ds != null ? this.f190ds.getName() : "";
        return Localizer.translateMessage("ds.description", strArr);
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.b_setup", new Object[]{getDescription()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_refund", new Object[]{getDescription(), f42df.format(begin), f42df.format(end)});
    }
}
