package psoft.hsphere.resource.plan_wizard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import javax.servlet.ServletRequest;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.plan.InitModifier;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.plan.InitValue;
import psoft.hsphere.plan.ResourceType;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/resource/plan_wizard/PlanEditor.class */
public class PlanEditor implements PlanValueSetter {
    private PreparedStatement svps;
    private PreparedStatement dvps;
    private PreparedStatement dvps1;
    private Plan plan;

    /* renamed from: rq */
    private ServletRequest f206rq;
    private int billingType = 0;
    private boolean isBiTypeSet = false;
    private Connection con = Session.getDb();
    private PreparedStatement tps = this.con.prepareStatement("INSERT INTO plan_resource (plan_id, type_id, class_name, disabled, showable) VALUES (?, ?, ?, ?, ?)");

    private int getBillingType() {
        if (!this.isBiTypeSet) {
            String strBiType = this.f206rq.getParameter("trial");
            if (strBiType != null) {
                this.billingType = Integer.parseInt(strBiType);
            } else {
                this.billingType = -1;
            }
            this.isBiTypeSet = true;
        }
        return this.billingType;
    }

    public PlanEditor(ServletRequest rq) throws Exception {
        this.plan = Plan.getPlan(rq.getParameter("plan_id"));
        this.tps.setInt(1, this.plan.getId());
        this.tps.setInt(4, 0);
        this.tps.setInt(5, 1);
        this.f206rq = rq;
    }

    public void disableResource(String type) throws Exception {
        if (this.plan.getResourceType(TypeRegistry.getTypeId(type)) != null) {
            Session.getLog().debug(" disabling " + type);
            this.plan.FM_disableResource(type);
        }
    }

    public void addResource(String type, String className) throws Exception {
        ResourceType rs = this.plan.getResourceType(TypeRegistry.getTypeId(type));
        if (rs != null) {
            this.plan.FM_enableResource(type);
            setResourcePrice(type);
            return;
        }
        try {
            this.tps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
            this.tps.setString(3, className);
            this.tps.executeUpdate();
            Session.getLog().debug("Resource with type=" + type + " className=" + className + " has been added.Going to set price");
            setResourcePrice(type);
        } catch (NoSuchTypeException nstp) {
            Session.getLog().warn("Type: " + type + " is unknown", nstp);
        }
        this.plan.addResourceType(TypeRegistry.getTypeId(type), className, 0, 1);
    }

    public void eraseResourcePrice(String type) throws Exception {
        int periods = 0;
        try {
            periods = Integer.parseInt(this.plan.getValue("_PERIOD_TYPES"));
        } catch (Exception e) {
        }
        int rtype = Integer.parseInt(TypeRegistry.getTypeId(type));
        deleteValue(rtype, "_SETUP_PRICE_");
        deleteValue(rtype, "_UNIT_PRICE_");
        deleteValue(rtype, "_USAGE_PRICE_");
        if (isEmpty(this.f206rq.getParameter("leave_prices"))) {
            deleteValue(rtype, "_FREE_UNITS_");
        }
        deleteValue(rtype, "_REFUND_PRICE_");
        for (int i = 0; i < periods; i++) {
            deleteValue(rtype, "_SETUP_PRICE_" + i);
            deleteValue(rtype, "_UNIT_PRICE_" + i);
            deleteValue(rtype, "_USAGE_PRICE_" + i);
            deleteValue(rtype, "_FREE_UNITS_" + i);
            deleteValue(rtype, "_REFUND_PRICE_" + i);
        }
        deleteValue(rtype, "_RECURRENT_CALC");
        deleteValue(rtype, "_USAGE_CALC");
        deleteValue(rtype, "_REFUND_CALC");
        deleteValue(rtype, "_SETUP_CALC");
    }

    public void setResourcePrice(String type) throws Exception {
        if (getBillingType() == 0) {
            eraseResourcePrice(type);
            int rtype = Integer.parseInt(TypeRegistry.getTypeId(type));
            if (!isEmpty(this.f206rq.getParameter("f_" + type))) {
                setValue(rtype, "_FREE_UNITS_", this.f206rq.getParameter("f_" + type));
            }
        } else if (isEmpty(this.f206rq.getParameter("leave_prices")) && this.plan.getValue("_PERIOD_TYPES") != null) {
            int periods = Integer.parseInt(this.plan.getValue("_PERIOD_TYPES"));
            int rtype2 = Integer.parseInt(TypeRegistry.getTypeId(type));
            boolean delSetupCalc = true;
            boolean delRefundCalc = true;
            boolean delUsageCalc = true;
            boolean delRecurrentCalc = true;
            Session.getLog().debug("Periods " + periods);
            for (int i = 0; i < periods; i++) {
                if (!isEmpty(this.f206rq.getParameter("s_" + type + "_" + i))) {
                    setValue(rtype2, "_SETUP_PRICE_" + i, this.f206rq.getParameter("s_" + type + "_" + i));
                    delSetupCalc = false;
                } else {
                    deleteValue(rtype2, "_SETUP_PRICE_" + i);
                }
                if (!isEmpty(this.f206rq.getParameter("m_" + type + "_" + i))) {
                    setValue(rtype2, "_UNIT_PRICE_" + i, this.f206rq.getParameter("m_" + type + "_" + i));
                    delRefundCalc = false;
                    delRecurrentCalc = false;
                } else {
                    deleteValue(rtype2, "_UNIT_PRICE_" + i);
                }
                if (!isEmpty(this.f206rq.getParameter("u_" + type + "_" + i))) {
                    setValue(rtype2, "_USAGE_PRICE_" + i, this.f206rq.getParameter("u_" + type + "_" + i));
                    delUsageCalc = false;
                } else {
                    deleteValue(rtype2, "_USAGE_PRICE_" + i);
                }
                if (!isEmpty(this.f206rq.getParameter("f_" + type + "_" + i))) {
                    setValue(rtype2, "_FREE_UNITS_" + i, this.f206rq.getParameter("f_" + type + "_" + i));
                } else {
                    deleteValue(rtype2, "_FREE_UNITS_" + i);
                }
                if (!isEmpty(this.f206rq.getParameter("ref_" + type + "_" + i))) {
                    delRefundCalc = false;
                }
            }
            if (!isEmpty(this.f206rq.getParameter("s_" + type))) {
                setValue(rtype2, "_SETUP_PRICE_", this.f206rq.getParameter("s_" + type));
            } else {
                deleteValue(rtype2, "_SETUP_PRICE_");
            }
            if (!isEmpty(this.f206rq.getParameter("m_" + type))) {
                setValue(rtype2, "_UNIT_PRICE_", this.f206rq.getParameter("m_" + type));
            } else {
                deleteValue(rtype2, "_UNIT_PRICE_");
            }
            if (!isEmpty(this.f206rq.getParameter("u_" + type))) {
                setValue(rtype2, "_USAGE_PRICE_", this.f206rq.getParameter("u_" + type));
            } else {
                deleteValue(rtype2, "_USAGE_PRICE_");
            }
            if (!isEmpty(this.f206rq.getParameter("f_" + type))) {
                setValue(rtype2, "_FREE_UNITS_", this.f206rq.getParameter("f_" + type));
            } else {
                deleteValue(rtype2, "_FREE_UNITS_");
            }
            if (delSetupCalc && isEmpty(this.f206rq.getParameter("s_" + type))) {
                deleteValue(rtype2, "_SETUP_CALC");
            } else {
                setValue(rtype2, "_SETUP_CALC", "psoft.hsphere.calc.StandardSetupCalc");
            }
            if (delRefundCalc && isEmpty(this.f206rq.getParameter("m_" + type))) {
                deleteValue(rtype2, "_REFUND_CALC");
            } else {
                setValue(rtype2, "_REFUND_CALC", "psoft.hsphere.calc.StandardRefundCalc");
            }
            if (delUsageCalc && isEmpty(this.f206rq.getParameter("u_" + type))) {
                deleteValue(rtype2, "_USAGE_CALC");
            } else {
                setValue(rtype2, "_USAGE_CALC", "psoft.hsphere.calc.StandardUsageCalc");
            }
            if (delRecurrentCalc && isEmpty(this.f206rq.getParameter("m_" + type))) {
                deleteValue(rtype2, "_RECURRENT_CALC");
                return;
            }
            setValue(rtype2, "_RECURRENT_CALC", "psoft.hsphere.calc.StandardCalc");
            setValue(rtype2, "_REFUND_CALC", "psoft.hsphere.calc.StandardRefundCalc");
        }
    }

    public void setValue(String type, String name, String value) throws Exception {
        try {
            setValue(Integer.parseInt(TypeRegistry.getTypeId(type)), name, value);
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such type " + type, nste);
        }
    }

    public void setValue(int type, String name, String value) throws Exception {
        String newValue = value;
        try {
            Session.getLog().debug("Value before formatting: " + value);
            newValue = USFormat.format(HsphereToolbox.parseLocalizedNumber(value));
            Session.getLog().debug("Value after formatting: " + newValue);
        } catch (ParseException e) {
        }
        if (this.svps == null) {
            this.svps = this.con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (?, ?, ?, ?)");
            this.svps.setInt(1, this.plan.getId());
            Session.getLog().debug("Prepeared statement svps");
        }
        if (this.dvps == null) {
            this.dvps = this.con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND type_id = ? AND name = ?");
            this.dvps.setInt(1, this.plan.getId());
        }
        this.dvps.setInt(2, type);
        this.dvps.setString(3, name);
        this.dvps.executeUpdate();
        this.svps.setInt(3, type);
        this.svps.setString(2, name);
        this.svps.setString(4, newValue);
        this.svps.executeUpdate();
        Session.getLog().debug("Value with name=" + name + " an value=" + newValue + " has been added");
    }

    public String getValue(String rtype, String name) throws Exception {
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(rtype));
        return rt.getValue(name);
    }

    public boolean areLiveAccounts() throws Exception {
        return this.plan.qntySignupedUsers() > 0;
    }

    public void addMod(String type, String mod) throws Exception {
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(type));
        Session.getLog().debug("RT=" + rt + " for type=" + type);
        InitModifier im = rt.getInitModifier(mod);
        if (im != null) {
            if (im.isDisabled()) {
                im.setDisabled(0);
                return;
            }
            return;
        }
        PreparedStatement ps = null;
        try {
            try {
                try {
                    ps = this.con.prepareStatement("INSERT INTO plan_imod (plan_id, type_id, mod_id, disabled) VALUES (?, ?, ?, 0)");
                    ps.setInt(1, this.plan.getId());
                    ps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
                    ps.setString(3, mod);
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                } catch (NoSuchTypeException nste) {
                    Session.getLog().warn("No Such Type " + type, nste);
                    Session.closeStatement(ps);
                }
            } catch (SQLException sqlex) {
                Session.getLog().error("Error adding mod, type=" + type + " modId=" + mod, sqlex);
                Session.closeStatement(ps);
            }
            rt.addInitModifier(mod, 0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }

    public void disableMod(String type, String mod) throws Exception {
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(type));
        if (rt == null) {
            return;
        }
        InitModifier im = rt.getInitModifier(mod);
        if (im != null) {
            im.setDisabled(1);
        } else {
            Session.getLog().warn("Mod for modId=" + mod + " and type=" + type + " not found");
        }
    }

    public void addIResource(String type, String mod, String newType, String newMod) throws Exception {
        addIResource(type, mod, newType, newMod, false);
    }

    public void addIResource(String type, String mod, String newType, String newMod, boolean unique) throws Exception {
        InitModifier im;
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(type));
        if (rt == null || (im = rt.getInitModifier(mod)) == null) {
            return;
        }
        InitResource ir = unique ? im.getInitResource(newType) : im.getInitResource(newType, newMod);
        Session.getLog().debug("Inside PlanEditor::addIResource: type=" + type + " mod=" + mod + " newType=" + newType + " newMod=" + newMod + " unique=" + unique + " ir=" + ir);
        if (ir != null) {
            if (unique) {
                im.changeInitResource(im.getInitResources().indexOf(ir), Integer.parseInt(TypeRegistry.getTypeId(newType)), newMod, 0);
                return;
            } else if (ir.isDisabled()) {
                im.setIRDisabled(im.getInitResources().indexOf(ir), 0);
                return;
            } else {
                return;
            }
        }
        PreparedStatement getOrder = null;
        PreparedStatement ps = null;
        try {
            try {
                try {
                    if (mod.length() == 0) {
                        getOrder = this.con.prepareStatement("SELECT COUNT(*) as new_order_id FROM plan_iresource WHERE plan_id = ? AND type_id= ? AND mod_id IS NULL");
                        getOrder.setInt(1, this.plan.getId());
                        getOrder.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
                    } else {
                        getOrder = this.con.prepareStatement("SELECT COUNT(*) as new_order_id FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND mod_id = ?");
                        getOrder.setInt(1, this.plan.getId());
                        getOrder.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
                        getOrder.setString(3, mod);
                    }
                    ResultSet newOrder = getOrder.executeQuery();
                    newOrder.next();
                    ps = this.con.prepareStatement("INSERT INTO plan_iresource (plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (? , ?, ?, ?, ?, ?, 0)");
                    ps.setInt(1, this.plan.getId());
                    ps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
                    if (mod.length() == 0) {
                        ps.setNull(3, 12);
                    } else {
                        ps.setString(3, mod);
                    }
                    ps.setInt(4, Integer.parseInt(TypeRegistry.getTypeId(newType)));
                    if (newMod.length() == 0) {
                        ps.setNull(5, 12);
                    } else {
                        ps.setString(5, newMod);
                    }
                    ps.setInt(6, newOrder.getInt(1));
                    ps.executeUpdate();
                    im.addInitResource(Integer.parseInt(TypeRegistry.getTypeId(newType)), newMod, 0);
                    Session.closeStatement(ps);
                    Session.closeStatement(getOrder);
                } catch (NoSuchTypeException nste) {
                    Session.getLog().warn("No Such Type ", nste);
                    Session.closeStatement(ps);
                    Session.closeStatement(getOrder);
                }
            } catch (SQLException sqlex) {
                Session.getLog().error("Error adding iresource type=" + type + " mod=" + mod, sqlex);
                Session.closeStatement(ps);
                Session.closeStatement(getOrder);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(getOrder);
            throw th;
        }
    }

    public void changeIResource(String type, String mod, String newType, String newMod, int order) throws Exception {
        InitModifier im;
        InitResource ir;
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(type));
        if (rt == null || (im = rt.getInitModifier(mod)) == null) {
            return;
        }
        try {
            ir = im.getInitResource(order);
        } catch (IndexOutOfBoundsException e) {
            ir = null;
        }
        if (ir == null) {
            addIResource(type, mod, newType, newMod);
        } else {
            im.changeInitResource(order, Integer.parseInt(TypeRegistry.getTypeId(newType)), newMod, 0);
        }
    }

    public void disableIResource(String type, String mod, String newType, String newMod) throws Exception {
        InitModifier im;
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(type));
        if (rt == null || (im = rt.getInitModifier(mod)) == null) {
            return;
        }
        InitResource ir = im.getInitResource(newType, newMod);
        if (ir != null) {
            im.setIRDisabled(im.getInitResources().indexOf(ir), 1);
            ir.disable();
            return;
        }
        Session.getLog().warn("IResource for type=" + newType + " with mod=" + newMod + " not found in mod=" + mod + " of type=" + type);
    }

    public void setBInfo(int biInfo) throws Exception {
        this.plan.FM_setBInfo(biInfo);
    }

    @Override // psoft.hsphere.resource.plan_wizard.PlanValueSetter
    public void setValue(String name, String value) throws Exception {
        String newValue = value;
        try {
            newValue = USFormat.format(HsphereToolbox.parseLocalizedNumber(value));
        } catch (ParseException e) {
        }
        if (this.svps == null) {
            this.svps = this.con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (?, ?, ?, ?)");
            this.svps.setInt(1, this.plan.getId());
            Session.getLog().debug("Prepeared statement svps");
        }
        if (this.dvps1 == null) {
            this.dvps1 = this.con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND type_id IS NULL AND name = ?");
            this.dvps1.setInt(1, this.plan.getId());
        }
        this.dvps1.setString(2, name);
        this.dvps1.executeUpdate();
        this.svps.setNull(3, 4);
        this.svps.setString(2, name);
        this.svps.setString(4, newValue);
        this.svps.executeUpdate();
        Session.getLog().debug("Value with name=" + name + " an value=" + value + " has been added");
    }

    public void setIValue(String type, String modId, String label, String value, int order, int vType) {
        InitModifier im;
        try {
            ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(type));
            if (rt != null && (im = rt.getInitModifier(modId)) != null && order < im.getInitValues().size()) {
                im.FM_changeInitValue(order, label, value, vType);
            }
        } catch (Exception ex) {
            Session.getLog().error("Failed to set an Init Value. Resource: [" + type + "], Mod: [" + modId + "], Label: [" + label + "], Value: [" + value + "].", ex);
        }
    }

    public void addIValue(String rtype, String mod, int type, String value, String label, int order) throws Exception {
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(rtype));
        InitModifier im = rt.getInitModifier(mod);
        if (order < im.getInitValues().size()) {
            im.FM_changeInitValue(order, label, value, type);
            return;
        }
        PreparedStatement ps = null;
        try {
            try {
                try {
                    ps = this.con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value, label, order_id, disabled) VALUES(?, ?, ?, ?, ?, ?, ?, 0)");
                    ps.setInt(1, this.plan.getId());
                    ps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(rtype)));
                    ps.setInt(3, type);
                    if (mod.length() == 0) {
                        ps.setNull(4, 12);
                    } else {
                        ps.setString(4, mod);
                    }
                    if (value.length() == 0) {
                        ps.setNull(5, 12);
                    } else if (type != 30 || type != 32) {
                        try {
                            ps.setString(5, USFormat.format(HsphereToolbox.parseLocalizedNumber(value)));
                        } catch (Exception e) {
                            Session.getLog().debug("addIValue  value=" + value);
                            ps.setString(5, value);
                        }
                    }
                    if (label.length() == 0) {
                        ps.setNull(6, 12);
                    } else {
                        ps.setString(6, label);
                    }
                    ps.setInt(7, order);
                    ps.executeUpdate();
                    Session.closeStatement(ps);
                } catch (NoSuchTypeException nste) {
                    Session.getLog().warn("No Such Type " + rtype, nste);
                    Session.closeStatement(ps);
                }
                ((ArrayList) im.getInitValues()).ensureCapacity(order + 1);
                im.getInitValues().add(order, new InitValue(value, type, label));
            } catch (SQLException sqlex) {
                Session.getLog().warn("SQL exception during adding init value", sqlex);
                Session.closeStatement(ps);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }

    public String getIValue(String rtype, String mod, int order) throws Exception {
        InitValue iv;
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(rtype));
        InitModifier im = rt.getInitModifier(mod);
        if (order < im.getInitValues().size() && (iv = im.FM_getInitValue(order)) != null) {
            return iv.getValue();
        }
        return null;
    }

    public void addMissingIValue(String rtype, String mod, int type, String value, String label, int order) {
        try {
            String oldValue = getIValue(rtype, mod, order);
            if (oldValue == null || "".equals(oldValue)) {
                addIValue(rtype, mod, type, value, label, order);
            }
        } catch (Exception ex) {
            Session.getLog().error("Failed to add a new Init Value. Resource: [" + rtype + "], Mod: [" + mod + "], Value: [" + value + "].", ex);
        }
    }

    public void deleteValue(String name) throws Exception {
        PreparedStatement dv = null;
        try {
            dv = this.con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND type_id IS NULL AND name = ?");
            dv.setInt(1, this.plan.getId());
            dv.setString(2, name);
            dv.executeUpdate();
            Session.closeStatement(dv);
        } catch (Throwable th) {
            Session.closeStatement(dv);
            throw th;
        }
    }

    public void deleteValue(int typeId, String name) throws Exception {
        PreparedStatement dv = null;
        try {
            dv = this.con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND type_id = ? AND name = ?");
            dv.setInt(1, this.plan.getId());
            dv.setInt(2, typeId);
            dv.setString(3, name);
            dv.executeUpdate();
            Session.closeStatement(dv);
        } catch (Throwable th) {
            Session.closeStatement(dv);
            throw th;
        }
    }

    public void deleteValue(String typeName, String name) throws Exception {
        deleteValue(Integer.parseInt(TypeRegistry.getTypeId(typeName)), name);
    }

    protected boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public Plan done() throws Exception {
        try {
            Session.closeStatement(this.tps);
            Session.closeStatement(this.svps);
            Session.closeStatement(this.dvps);
            Session.closeStatement(this.dvps1);
            this.con.close();
        } catch (Exception ex) {
            Session.getLog().error("Errors during closing connections ", ex);
        }
        return Plan.load(this.plan.getId());
    }

    public void deleteRValuesStartsWith(String rtype, String st) throws Exception {
        ResourceType rt = this.plan.getResourceType(TypeRegistry.getTypeId(rtype));
        if (rt != null) {
            for (String key : rt.getValueKeys()) {
                if (key.startsWith(st)) {
                    Session.getLog().debug("Deleting value " + key);
                    deleteValue(rtype, key);
                }
            }
        }
    }

    public Plan getPlan() {
        return this.plan;
    }
}
