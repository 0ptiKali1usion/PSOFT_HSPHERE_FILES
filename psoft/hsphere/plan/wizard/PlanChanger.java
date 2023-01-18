package psoft.hsphere.plan.wizard;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashSet;
import java.util.Set;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.plan.InitModifier;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.plan.ResourceType;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/plan/wizard/PlanChanger.class */
public class PlanChanger implements TemplateHashModel {

    /* renamed from: p */
    Plan f111p;
    Set enabledResources = new HashSet();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public PlanChanger(Plan p) throws Exception {
        this.f111p = p;
        loadInitResources();
    }

    protected void loadInitResources() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT distinct new_type_id FROM plan_iresource, plan_imod WHERE plan_iresource.disabled = 0 AND plan_iresource.plan_id = ? AND plan_imod.plan_id = ? AND plan_imod.disabled = 0 AND plan_imod.type_id = plan_iresource.type_id AND ((plan_imod.mod_id = plan_iresource.mod_id) OR ((plan_imod.mod_id IS NULL OR plan_imod.mod_id='') AND (plan_iresource.mod_id IS NULL OR plan_iresource.mod_id='')))");
            ps.setInt(1, this.f111p.getId());
            ps.setInt(2, this.f111p.getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.enabledResources.add(new Integer(rs.getInt(1)));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isResourceEnabled(String resourceType) throws Exception {
        return this.enabledResources.contains(new Integer(TypeRegistry.getTypeId(resourceType)));
    }

    public TemplateModel FM_isResourceEnabled(String resourceType) throws Exception {
        return new TemplateString(isResourceEnabled(resourceType));
    }

    public TemplateModel FM_getLogicalServerGroup(String name) {
        return new TemplateString(this.f111p.getValue("_HOST_" + name));
    }

    public TemplateModel FM_getIPType() throws Exception {
        InitResource ir = getChildInitResource("domain", "", "ip");
        if (ir == null) {
            ir = getChildInitResource("nodomain", "", "ip");
            if (ir == null) {
                ir = getChildInitResource("3ldomain", "", "ip");
                if (ir == null) {
                    ir = getChildInitResource("service_domain", "", "ip");
                    if (ir == null) {
                        ir = getChildInitResource("subdomain", "", "ip");
                        if (ir == null) {
                            return null;
                        }
                    }
                }
            }
        }
        return new TemplateString(ir.getMod());
    }

    private InitResource getChildInitResource(String parentType, String mod, String childType) throws Exception {
        InitModifier im;
        ResourceType rt = this.f111p.getResourceType(TypeRegistry.getTypeId(parentType));
        if (rt == null || (im = rt.getInitModifier(mod)) == null) {
            return null;
        }
        return im.FM_getInitResource(childType);
    }

    public TemplateModel FM_getSharedIPTag() throws Exception {
        String tag = null;
        try {
            tag = this.f111p.getResourceType(TypeRegistry.getTypeId("ip")).getValue("SHARED_IP");
        } catch (NullPointerException e) {
        }
        tag = (tag == null || tag.length() == 0) ? "2" : "2";
        return new TemplateString(tag);
    }

    public TemplateModel FM_getWizard() throws Exception {
        return PlanWizardXML.getWizard(this.f111p.getValue("_CREATED_BY_"));
    }
}
