package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.resource.allocation.AllocatedPServerHolder;
import psoft.hsphere.resource.allocation.AllocatedServerManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/AllocatedServerManagerAccessor.class */
public class AllocatedServerManagerAccessor implements TemplateHashModel {
    public TemplateModel get(String key) throws TemplateModelException {
        return "status".equals(key) ? new TemplateString("OK") : AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateModel FM_scan(int unassigned, int available, int taken) throws Exception {
        List list = new ArrayList();
        list.add(new Integer(unassigned));
        list.add(new Integer(available));
        list.add(new Integer(taken));
        return AllocatedServerManager.getInstance().scan(list);
    }

    public TemplateModel FM_assignToPlan(long pServerId, int planId) throws Exception {
        AllocatedServerManager.getInstance().assignToPlan(pServerId, planId);
        return this;
    }

    public TemplateModel FM_getAllocatedPServer(long apsId) {
        return AllocatedPServerHolder.getInstance().get(apsId);
    }

    public TemplateModel FM_unassignPServer(long apsId) throws SQLException {
        AllocatedServerManager.getInstance().unassignPServer(apsId);
        return this;
    }

    public TemplateModel FM_getscanReport(int arid) throws Exception {
        return AllocatedServerManager.getInstance().getscanReport(arid);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }
}
