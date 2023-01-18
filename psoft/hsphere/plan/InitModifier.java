package psoft.hsphere.plan;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateScalarModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.InitToken;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.HostManager;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/plan/InitModifier.class */
public class InitModifier implements TemplateHashModel {

    /* renamed from: id */
    protected String f105id;
    protected ResourceType parent;
    protected boolean disabled;
    protected List initValues;
    protected List initResources;
    protected static final TemplateString TS_1 = new TemplateString("1");
    protected static final TemplateString TS_0 = new TemplateString("0");

    /* renamed from: OK */
    protected static final TemplateString f106OK = new TemplateString("OK");

    public ResourceType getParent() {
        return this.parent;
    }

    public String toString() {
        return "InitModifier -> " + this.f105id + " " + this.initValues.toString() + this.initResources.toString();
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("status")) {
            return f106OK;
        }
        if (key.equals("id")) {
            return new TemplateString(this.f105id);
        }
        if (!key.equals("disabled")) {
            return key.equals("iresources") ? new TemplateList(this.initResources) : key.equals("ivalues") ? new TemplateList(this.initValues) : key.equals("get_ls_group") ? getValueByType(6) : AccessTemplateMethodWrapper.getMethod(this, key);
        } else if (this.disabled) {
            return TS_1;
        } else {
            return TS_0;
        }
    }

    public InitValue getValueByType(int type) {
        for (InitValue value : this.initValues) {
            if (value.getType() == type) {
                return value;
            }
        }
        return null;
    }

    public void signUpVar(List s, Plan p) throws Exception {
        String tmp;
        if (!isDisabled()) {
            for (InitValue tmpi : this.initValues) {
                if (tmpi.getType() == 1 && !s.contains(tmpi)) {
                    s.add(tmpi);
                }
            }
            for (InitResource ir : this.initResources) {
                if (!ir.isDisabled()) {
                    ResourceType r = p.getResourceType(ir.getType());
                    if (ir.getMod() == null) {
                        tmp = "";
                    } else {
                        tmp = ir.getMod();
                    }
                    InitModifier im = (InitModifier) r.initMod.get(tmp);
                    if (im == null) {
                        im = (InitModifier) r.initMod.get("");
                    }
                    if (im != null) {
                        im.signUpVar(s, p);
                    }
                }
            }
        }
    }

    public TemplateModel FM_signUpVar() throws Exception {
        Plan p = Plan.getPlan(this.parent.planId);
        List s = new ArrayList();
        signUpVar(s, p);
        return new TemplateList(s);
    }

    public TemplateModel FM_enable() throws Exception {
        if (this.disabled) {
            setDisabled(0);
            this.disabled = false;
        }
        return this;
    }

    public TemplateModel FM_disable() throws Exception {
        if (!this.disabled) {
            setDisabled(1);
            this.disabled = true;
        }
        return this;
    }

    public InitValue FM_getInitValue(int count) throws Exception {
        return (InitValue) this.initValues.get(count);
    }

    public InitResource FM_getInitResource(String type) throws Exception {
        for (InitResource ir : this.initResources) {
            if (ir.getType() == Integer.parseInt(TypeRegistry.getTypeId(type)) && !ir.isDisabled()) {
                return ir;
            }
        }
        return null;
    }

    public TemplateModel FM_changeInitValue(int count, String label, String value, int type) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        Session.getLog().info("inside change initval label=" + label + " value=" + value + " type=" + type);
        try {
            if (this.f105id.length() == 0) {
                ps = con.prepareStatement("UPDATE plan_ivalue SET value = ?, label = ?, type = ? WHERE plan_id = ? AND type_id = ? AND order_id = ? AND mod_id IS NULL");
            } else {
                ps = con.prepareStatement("UPDATE plan_ivalue SET value = ?, label = ?, type = ? WHERE plan_id = ? AND type_id = ? AND order_id = ? AND mod_id = ?");
                ps.setString(7, this.f105id);
            }
            try {
                Session.getLog().debug("FM_changeInitValue (try)  value=" + value);
                String newValue = value;
                try {
                    if (this.parent.getId() != 30 && this.parent.getId() != 32) {
                        newValue = USFormat.format(HsphereToolbox.parseLocalizedNumber(value));
                    }
                } catch (ParseException e) {
                }
                ps.setString(1, newValue);
                Session.getLog().debug("FM_changeInitValue (try in USFormat) value=" + USFormat.parseString(newValue));
            } catch (Exception e2) {
                Session.getLog().debug("FM_changeInitValue (catch)  value=" + value);
                ps.setString(1, value);
            }
            if (label != null) {
                ps.setString(2, label);
            } else {
                ps.setNull(2, 12);
            }
            ps.setInt(3, type);
            ps.setInt(4, this.parent.planId);
            ps.setInt(5, this.parent.getId());
            ps.setInt(6, count);
            ps.executeUpdate();
            Session.getLog().info(value + ":" + label + ":" + type + ":" + this.parent.planId + ":" + this.parent.getId() + ":" + this.f105id + ":" + count);
            Session.closeStatement(ps);
            con.close();
            InitValue tmp = new InitValue(value, type, label);
            this.initValues.set(count, tmp);
            return this;
        } catch (Throwable t) {
            try {
                Session.getLog().info("----->CHANGE INITVAL", t);
                Session.closeStatement(ps);
                con.close();
                return null;
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    public InitResource changeInitResource(int order, int typeId, String modId, int disabled) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if (this.f105id.length() == 0) {
                ps = con.prepareStatement("UPDATE plan_iresource SET new_type_id = ?, new_mod_id=?, disabled=? WHERE plan_id=? and type_id=? and mod_id IS NULL and order_id=?");
                ps.setInt(1, typeId);
                ps.setString(2, modId);
                ps.setInt(3, 0 == disabled ? disabled : 1);
                ps.setInt(4, this.parent.planId);
                ps.setInt(5, this.parent.getId());
                ps.setInt(6, order);
            } else {
                ps = con.prepareStatement("UPDATE plan_iresource SET new_type_id = ?, new_mod_id=?, disabled=? WHERE plan_id=? and type_id=? and mod_id = ? and order_id=?");
                ps.setInt(1, typeId);
                ps.setString(2, modId);
                ps.setInt(3, 0 == disabled ? disabled : 1);
                ps.setInt(4, this.parent.planId);
                ps.setInt(5, this.parent.getId());
                ps.setString(6, this.f105id);
                ps.setInt(7, order);
            }
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            InitResource tmp = new InitResource(typeId, modId, disabled);
            this.initResources.set(order, tmp);
            return tmp;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public InitResource getInitResource(int count) {
        return (InitResource) this.initResources.get(count);
    }

    public TemplateModel FM_irEnable(int count) throws Exception {
        InitResource ir = getInitResource(count);
        if (ir.isDisabled()) {
            setIRDisabled(count, 0);
            ir.enable();
        }
        return this;
    }

    public TemplateModel FM_irDisable(int count) throws Exception {
        InitResource ir = getInitResource(count);
        if (!ir.isDisabled()) {
            setIRDisabled(count, 1);
            ir.disable();
        }
        return this;
    }

    public void setIRDisabled(int count, int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if (this.f105id.length() == 0) {
                ps = con.prepareStatement("UPDATE plan_iresource SET disabled = ? WHERE plan_id = ? AND type_id = ? AND order_id = ? AND mod_id IS NULL");
            } else {
                ps = con.prepareStatement("UPDATE plan_iresource SET disabled = ? WHERE plan_id = ? AND type_id = ? AND order_id = ? AND mod_id = ?");
                ps.setString(5, this.f105id);
            }
            ps.setInt(1, value);
            ps.setInt(2, this.parent.planId);
            ps.setInt(3, this.parent.getId());
            ps.setInt(4, count);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setDisabled(int value) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            if (this.f105id.length() == 0) {
                ps = con.prepareStatement("UPDATE plan_imod SET disabled = ? WHERE plan_id = ? AND type_id = ? AND mod_id IS NULL");
            } else {
                ps = con.prepareStatement("UPDATE plan_imod SET disabled = ? WHERE plan_id = ? AND type_id = ? AND mod_id = ?");
                ps.setString(4, this.f105id);
            }
            ps.setInt(1, value);
            ps.setInt(2, this.parent.planId);
            ps.setInt(3, this.parent.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean isDisabled() {
        return this.disabled;
    }

    public InitModifier copy(ResourceType rt) throws Exception {
        return new InitModifier(rt, this);
    }

    protected InitModifier(ResourceType parent, InitModifier im) throws Exception {
        this.parent = parent;
        this.f105id = im.f105id;
        this.disabled = im.disabled;
        this.initValues = new ArrayList();
        this.initResources = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("INSERT INTO plan_imod (plan_id, type_id, mod_id, disabled) VALUES (?, ?, ?, ?)");
            int plan_id = parent.planId;
            int type_id = parent.getId();
            ps2.setInt(1, plan_id);
            ps2.setInt(2, type_id);
            if (this.f105id.length() > 0) {
                ps2.setString(3, this.f105id);
            } else {
                ps2.setNull(3, 12);
            }
            ps2.setInt(4, this.disabled ? 1 : 0);
            ps2.executeUpdate();
            ps2.close();
            PreparedStatement ps3 = con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, mod_id, value, order_id, disabled, type, label) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
            ps3.setInt(1, plan_id);
            ps3.setInt(2, type_id);
            if (this.f105id.length() == 0) {
                ps3.setNull(3, 12);
            } else {
                ps3.setString(3, this.f105id);
            }
            ps3.setInt(6, 0);
            int orderId = 0;
            for (InitValue value : im.initValues) {
                ps3.setObject(4, value.getValue());
                int i = orderId;
                orderId++;
                ps3.setInt(5, i);
                ps3.setInt(7, value.getType());
                ps3.setString(8, value.getLabel());
                ps3.executeUpdate();
                this.initValues.add(value);
            }
            ps3.close();
            ps = con.prepareStatement("INSERT INTO plan_iresource (plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, plan_id);
            ps.setInt(2, type_id);
            if (this.f105id.length() == 0) {
                ps.setNull(3, 12);
            } else {
                ps.setString(3, this.f105id);
            }
            int orderId2 = 0;
            for (InitResource ir : im.initResources) {
                ps.setInt(4, ir.f107id);
                ps.setString(5, ir.initMod);
                int i2 = orderId2;
                orderId2++;
                ps.setInt(6, i2);
                ps.setInt(7, this.disabled ? 1 : 0);
                ps.executeUpdate();
                this.initResources.add(new InitResource(ir.f107id, ir.initMod, this.disabled ? 1 : 0));
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public InitModifier(ResourceType parent, String id, int disabled) {
        this.parent = parent;
        this.f105id = id;
        this.disabled = disabled == 1;
        this.initValues = new ArrayList();
        this.initResources = new ArrayList();
    }

    public void addInitResource(int typeId, String modId, int disabled) {
        try {
            addInitResource(typeId, modId, disabled, false);
        } catch (Exception ex) {
            Session.getLog().error("Unsuccessful try to add new InitResource", ex);
        }
    }

    public void addInitResource(int typeId, String modId, int disabled, boolean force) throws Exception {
        PreparedStatement ps;
        PreparedStatement ps2 = null;
        for (InitResource ir : this.initResources) {
            if (typeId == ir.getType() && modId.equals(ir.getMod()) && !ir.isDisabled()) {
                setIRDisabled(this.initResources.indexOf(ir), 0);
                return;
            }
        }
        if (force) {
            Connection con = Session.getDb();
            try {
                try {
                    if (this.f105id.length() == 0) {
                        ps = con.prepareStatement("SELECT MAX(order_id) as mid FROM plan_iresource WHERE plan_id = ? AND type_id =? AND mod_id IS NULL");
                    } else {
                        ps = con.prepareStatement("SELECT MAX(order_id) as mid FROM plan_iresource WHERE plan_id = ? AND type_id =? AND mod_id= ?");
                        ps.setString(3, this.f105id);
                    }
                    ps.setInt(1, this.parent.planId);
                    ps.setInt(2, this.parent.getId());
                    ResultSet rs = ps.executeQuery();
                    rs.next();
                    int orderId = rs.getInt("mid") + 1;
                    ps.close();
                    ps2 = con.prepareStatement("INSERT INTO plan_iresource (plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (?, ?, ?, ?, ?, ?, ?)");
                    ps2.setInt(1, this.parent.planId);
                    ps2.setInt(2, this.parent.getId());
                    if (this.f105id.length() == 0) {
                        ps2.setNull(3, 12);
                    } else {
                        ps2.setString(3, this.f105id);
                    }
                    ps2.setInt(4, typeId);
                    if (modId.length() == 0) {
                        ps2.setNull(5, 12);
                    } else {
                        ps2.setString(5, modId);
                    }
                    ps2.setInt(6, orderId);
                    ps2.setInt(7, disabled);
                    ps2.executeUpdate();
                    this.initResources.add(new InitResource(typeId, modId, disabled));
                    Session.closeStatement(ps2);
                    con.close();
                    return;
                } catch (Exception ex) {
                    Session.getLog().error("Unsuccessful try to add new initresource into database ", ex);
                    throw ex;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps2);
                con.close();
                throw th;
            }
        }
        this.initResources.add(new InitResource(typeId, modId, disabled));
    }

    public void deleteInitResource(String typeId, String modId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        StringBuffer sqlStatement = new StringBuffer("DELETE FROM plan_iresource WHERE plan_id=? AND type_id=? AND new_type_id=? AND ");
        if (this.f105id.length() == 0) {
            sqlStatement.append("mod_id IS NULL AND ");
        } else {
            sqlStatement.append("mod_id=? AND ");
        }
        if (modId.length() == 0) {
            sqlStatement.append("new_mod_id IS NULL");
        } else {
            sqlStatement.append("new_mod_id=?");
        }
        Session.getLog().debug(sqlStatement.toString());
        try {
            try {
                ps = con.prepareStatement(sqlStatement.toString());
                ps.setInt(1, this.parent.planId);
                ps.setInt(2, this.parent.getId());
                ps.setInt(3, Integer.parseInt(typeId));
                if (this.f105id.length() > 0) {
                    ps.setString(4, this.f105id);
                }
                if (modId.length() > 0) {
                    ps.setString(5, modId);
                }
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Unable to delete init resource for typeId=" + typeId + " with modId=" + modId, ex);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void addInitValue(InitValue value) {
        synchronized (this.initValues) {
            this.initValues.add(value);
        }
    }

    public InitResource getInitResource(String type) throws NoSuchTypeException {
        try {
            int typeId = Integer.parseInt(TypeRegistry.getTypeId(type));
            for (InitResource cir : this.initResources) {
                if (typeId == cir.getType()) {
                    return cir;
                }
            }
            return null;
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such Type " + type, nste);
            throw nste;
        }
    }

    public InitResource getInitResource(String type, String modId) throws NoSuchTypeException {
        if (modId == null) {
            Session.getLog().debug("Unable to get an init resource for the type [" + type + "] with the \"null\" modifier.");
            return null;
        }
        try {
            int typeId = Integer.parseInt(TypeRegistry.getTypeId(type));
            for (InitResource cir : this.initResources) {
                if (typeId == cir.getType() && modId.equals(cir.getMod())) {
                    return cir;
                }
            }
            return null;
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such Type " + type, nste);
            throw nste;
        }
    }

    public List getInitResources() {
        return this.initResources;
    }

    public List getInitValues() {
        return this.initValues;
    }

    public List getDefaultInitValues(InitToken token, int type) throws Exception {
        return getDefaultInitValues(token.getPeriodId(), null, type);
    }

    public List getDefaultInitValues(Resource r, int type) throws Exception {
        return getDefaultInitValues(r.getAccount().getPeriodId(), r, type);
    }

    public List getDefaultInitValues(int periodId, Resource r, int type) throws Exception {
        String tmp_val;
        Account tval;
        List l = new ArrayList();
        Plan currentPlan = Plan.getPlan(getParent().getPlanId());
        synchronized (this.initValues) {
            for (InitValue tmp : this.initValues) {
                if (tmp != null) {
                    if (tmp.getType() == 1) {
                        HttpServletRequest req = Session.getRequest();
                        if (req != null) {
                            tmp_val = req.getParameter(tmp.getValue());
                        } else {
                            tmp_val = null;
                        }
                        if (tmp_val == null) {
                            l.add("");
                        } else {
                            l.add(tmp_val);
                        }
                    } else if (tmp.getType() == 7) {
                        String val = currentPlan.getResourceValue(type, "_FREE_UNITS_" + periodId);
                        if (val == null) {
                            val = currentPlan.getResourceValue(type, "_FREE_UNITS_");
                        }
                        l.add(val);
                    } else if (tmp.getType() == 8) {
                        l.add(currentPlan.getResourceValue(type, tmp.getValue()));
                    } else if (tmp.getType() == 6) {
                        l.add(HostManager.getRandomHost(Integer.parseInt(currentPlan.getValue("_HOST_" + tmp.getValue()))));
                    } else if (tmp.getType() == 2 || tmp.getType() == 4 || tmp.getType() == 3 || tmp.getType() == 5) {
                        if (r == null) {
                            l.add("");
                        } else {
                            StringTokenizer st = new StringTokenizer(tmp.getValue(), ".");
                            if (tmp.getType() == 2 || tmp.getType() == 4) {
                                tval = r;
                            } else {
                                tval = Session.getAccount();
                            }
                            try {
                                if (tmp.getType() == 4 || tmp.getType() == 5) {
                                    while (st.hasMoreElements()) {
                                        tval = tval.recursiveGet(st.nextToken());
                                    }
                                } else {
                                    while (st.hasMoreElements()) {
                                        tval = tval.get(st.nextToken());
                                    }
                                }
                            } catch (Exception e) {
                                tval = null;
                            }
                            if (tval == null || !(tval instanceof TemplateScalarModel)) {
                                l.add("");
                            } else {
                                try {
                                    l.add(tval.getAsString());
                                } catch (TemplateModelException e2) {
                                    l.add("");
                                }
                            }
                        }
                    } else {
                        l.add(tmp.getValue());
                    }
                } else {
                    l.add("");
                }
            }
        }
        return l;
    }
}
