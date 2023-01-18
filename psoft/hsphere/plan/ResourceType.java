package psoft.hsphere.plan;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.global.Globals;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/plan/ResourceType.class */
public class ResourceType implements TemplateHashModel, Comparable {
    protected static final TemplateString TS_1 = new TemplateString("1");
    protected static final TemplateString TS_0 = new TemplateString("0");

    /* renamed from: OK */
    protected static final TemplateString f108OK = new TemplateString("OK");

    /* renamed from: id */
    protected int f109id;
    protected int planId;
    protected String type;
    protected String description;
    protected boolean disabled;
    protected boolean showable;

    /* renamed from: c */
    protected Class f110c;
    protected HashMap initMod;
    protected HashMap values;

    public ResourceType(int planId, int id, String className, int disabled, int showable) {
        this.planId = planId;
        this.f109id = id;
        this.disabled = disabled == 1;
        this.showable = showable == 1;
        try {
            this.type = TypeRegistry.getType(id);
            this.description = TypeRegistry.getDescription(id);
            this.f110c = Class.forName(className);
        } catch (Exception e) {
            Session.getLog().warn("Class unknow while initializing resource :" + id + ":" + className, e);
        }
        this.initMod = new HashMap();
        this.values = new HashMap();
    }

    protected ResourceType(int planId, ResourceType rt) throws Exception {
        this.planId = planId;
        this.f109id = rt.f109id;
        this.disabled = rt.disabled;
        this.f110c = rt.f110c;
        this.type = rt.type;
        this.description = rt.description;
        this.showable = rt.showable;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO plan_resource (plan_id, type_id, class_name, disabled, showable) VALUES (?, ?, ?, ?, ?)");
            int type_id = this.f109id;
            ps2.setInt(1, planId);
            ps2.setInt(2, type_id);
            ps2.setObject(3, this.f110c.getName());
            ps2.setInt(4, this.disabled ? 1 : 0);
            ps2.setInt(5, this.showable ? 1 : 0);
            ps2.executeUpdate();
            this.initMod = new HashMap();
            this.values = new HashMap();
            ps2.close();
            ps = con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (?, ?, ?, ?)");
            ps.setInt(1, planId);
            ps.setInt(3, type_id);
            for (Object key : rt.values.keySet()) {
                Object value = rt.values.get(key);
                ps.setObject(2, key);
                ps.setObject(4, value);
                this.values.put(key, value);
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
            for (Object key2 : rt.initMod.keySet()) {
                this.initMod.put(key2, ((InitModifier) rt.initMod.get(key2)).copy(this));
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void enable() {
        this.disabled = false;
    }

    public void disable() {
        this.disabled = true;
    }

    public void makeShown() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plan_resource SET showable=1 WHERE plan_id = ? AND type_id = ?");
            ps.setInt(1, this.planId);
            ps.setInt(2, this.f109id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.showable = true;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void makeHidden() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE plan_resource SET showable=0 WHERE plan_id = ? AND type_id = ?");
            ps.setInt(1, this.planId);
            ps.setInt(2, this.f109id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.showable = false;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isDisabled() {
        if (this.disabled) {
            return true;
        }
        try {
            return Globals.isObjectDisabled(this.type) != 0;
        } catch (UnknownResellerException ure) {
            Session.getLog().error("Unknown reseller", ure);
            return false;
        } catch (Exception ex) {
            Session.getLog().error("Unable to get to know whether the resource " + this.type + " was disabled or not.", ex);
            return false;
        }
    }

    public boolean isResourceShowable() {
        return this.showable;
    }

    public TemplateModel FM_isResourceShowable() {
        if (this.showable) {
            return new TemplateString("1");
        }
        return null;
    }

    public int getId() {
        return this.f109id;
    }

    public Class getResourceClass() {
        return this.f110c;
    }

    public String getType() {
        return this.type;
    }

    public int getPlanId() {
        return this.planId;
    }

    public HashMap getValues() {
        return this.values;
    }

    public Set getValueKeys() {
        return this.values.keySet();
    }

    public void putValue(String key, String value) {
        this.values.put(key, value);
    }

    public String getValue(String key) {
        return (String) this.values.get(key);
    }

    public boolean hasRecurrent(int periodId) {
        if (getValue("_RECURRENT_CALC") != null) {
            return true;
        }
        try {
            if (Session.getResellerId() == 1) {
                return false;
            }
            return !Double.isNaN(Session.getReseller().getPrices(this.f109id).getRecurrentPrice());
        } catch (Exception e) {
            Session.getLog().warn("Calc problem ", e);
            return true;
        }
    }

    public boolean hasRefund(int periodId) {
        if (getValue("_REFUND_CALC" + periodId) != null || getValue("_REFUND_CALC_") != null) {
            return true;
        }
        try {
            if (Session.getResellerId() == 1) {
                return false;
            }
            return Session.getReseller().getPrices(this.f109id).getRefundCalc() != null;
        } catch (Exception e) {
            Session.getLog().warn("Calc problem ", e);
            return true;
        }
    }

    public boolean hasUsage(int periodId) {
        if (getValue("_USAGE_PRICE_" + periodId) != null || getValue("_USAGE_PRICE_") != null) {
            return true;
        }
        try {
            if (Session.getResellerId() == 1) {
                return false;
            }
            return !Double.isNaN(Session.getReseller().getPrices(this.f109id).getUsagePrice());
        } catch (Exception e) {
            Session.getLog().warn("Calc problem ", e);
            return true;
        }
    }

    public void addInitModifier(String key, int disabled) {
        if (key == null) {
            key = "";
        }
        this.initMod.put(key, new InitModifier(this, key, disabled));
    }

    public InitModifier getInitModifier(String key) {
        if (key == null) {
            key = "";
        }
        return (InitModifier) this.initMod.get(key);
    }

    public ResourceType copy(int planId) throws Exception {
        return new ResourceType(planId, this);
    }

    public boolean isResourceInitializes(String mod, String type) {
        InitModifier im = getInitModifier(mod);
        if (im != null) {
            ArrayList irs = (ArrayList) im.getInitResources();
            Iterator i = irs.iterator();
            while (i.hasNext()) {
                InitResource ir = (InitResource) i.next();
                try {
                    if (type.equals(TypeRegistry.getType(ir.getType()))) {
                        return !ir.isDisabled();
                    }
                } catch (NoSuchTypeException e) {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        ResourceType rt = (ResourceType) o;
        if (getId() == rt.getId()) {
            return 0;
        }
        return getId() - rt.getId();
    }

    public boolean compatible(ResourceType rt) {
        if (rt == null) {
            return false;
        }
        return this.f110c.equals(rt.f110c);
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("status")) {
            return f108OK;
        }
        if (key.equals("description")) {
            return new TemplateString(this.description);
        }
        if (key.equals("id")) {
            return new TemplateString(this.f109id);
        }
        if (key.equals("type")) {
            return new TemplateString(this.type);
        }
        if (key.equals("disabled")) {
            return isDisabled() ? TS_1 : TS_0;
        } else if (key.equals("values")) {
            return new TemplateMap(this.values);
        } else {
            if (key.equals("valueKeys")) {
                return new TemplateList(this.values.keySet());
            }
            if (key.equals("mods")) {
                return new TemplateMap(this.initMod);
            }
            if (key.equals("modKeys")) {
                return new TemplateList(this.initMod.keySet());
            }
            if (key.equals("modDefault")) {
                return (InitModifier) this.initMod.get("");
            }
            if (key.equals("isMonthly")) {
                if (isMonthly()) {
                    return new TemplateString("1");
                }
                return null;
            }
            try {
                if (key.equals("required")) {
                    return TypeRegistry.isRequired(this.f109id) ? TS_1 : TS_0;
                }
            } catch (Exception e) {
                Session.getLog().warn("ResourceType.get problem ", e);
            }
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public TemplateModel FM_putValue(String key, String value) throws Exception {
        if (value.equals(this.values.get(key))) {
            return this;
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("DELETE FROM plan_value WHERE plan_id = ? AND type_id = ? AND name = ?");
            ps.setInt(1, this.planId);
            ps.setInt(2, this.f109id);
            ps.setString(3, key);
            ps.executeUpdate();
            if (value.length() > 0) {
                ps.close();
                ps = con.prepareStatement("INSERT INTO plan_value (plan_id, type_id, name, value) VALUES (?, ?, ?, ?)");
                ps.setInt(1, this.planId);
                ps.setInt(2, this.f109id);
                ps.setString(3, key);
                ps.setString(4, value);
                ps.executeUpdate();
                putValue(key, value);
            } else {
                this.values.remove(key);
            }
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public boolean isChangeable() {
        return TypeRegistry.isChangeable(this.f109id);
    }

    public boolean isMonthly() {
        return TypeRegistry.isMonthly(this.f109id);
    }

    public boolean isDummy() {
        return TypeRegistry.isDummy(this.f109id);
    }

    public String _getValue(String key) {
        return (String) this.values.get(key);
    }

    public TemplateModel FM_getValue(String key) throws Exception {
        return new TemplateString(_getValue(key));
    }
}
