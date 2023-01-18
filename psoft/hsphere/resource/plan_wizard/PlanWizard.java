package psoft.hsphere.resource.plan_wizard;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Vector;
import javax.servlet.ServletRequest;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Plan;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/resource/plan_wizard/PlanWizard.class */
public class PlanWizard implements PlanValueSetter {
    protected Connection con;

    /* renamed from: id */
    protected int f207id;
    protected PreparedStatement svps;
    protected PreparedStatement tps;
    protected PreparedStatement amps;
    protected PreparedStatement amsps;
    protected PreparedStatement aivps;
    protected PreparedStatement snps;
    protected PreparedStatement saps;
    protected PreparedStatement drps;
    protected Vector iresource;

    /* renamed from: rq */
    protected ServletRequest f208rq;
    private boolean wasTrans;

    public PlanWizard(ServletRequest rq) throws Exception {
        this.wasTrans = Session.isTransConnection();
        this.con = this.wasTrans ? Session.getDb() : Session.getTransConnection();
        this.iresource = new Vector();
        this.f208rq = rq;
    }

    public PlanWizard() throws Exception {
        this(null);
    }

    protected void setId() throws Exception {
        PreparedStatement ps = this.con.prepareStatement("SELECT max(id) FROM plans");
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            this.f207id = rs.getInt(1) + 1;
        } else {
            this.f207id = 1;
        }
        Session.closeStatement(ps);
    }

    public void startResources() throws Exception {
        this.tps = this.con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, ?, ?, ?, ?)");
        this.tps.setInt(1, this.f207id);
        this.tps.setInt(4, 0);
        this.tps.setInt(5, 1);
        Session.getLog().debug("Start adding resources");
    }

    public void addResource(String type, String className) throws Exception {
        try {
            this.tps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
            this.tps.setString(3, className);
            this.tps.executeUpdate();
            if (this.f208rq == null) {
                return;
            }
            setResourcePrice(type, this.f208rq.getParameter("s_" + type), this.f208rq.getParameter("m_" + type), this.f208rq.getParameter("u_" + type), this.f208rq.getParameter("f_" + type));
        } catch (NoSuchTypeException nstp) {
            Session.getLog().warn("Type: " + type + " is unknown", nstp);
        }
    }

    public void doneResoures() throws Exception {
        Session.getLog().debug("Going to doneResources");
    }

    public void setValue(String type, String name, String value) throws Exception {
        try {
            setValue(Integer.parseInt(TypeRegistry.getTypeId(type)), name, value);
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such type " + type, nste);
        }
    }

    protected void setValue(int type, String name, String value) throws Exception {
        Session.getLog().debug("Value in Plan Wizard with name " + name + " before setting = " + value);
        String newValue = value;
        try {
            newValue = USFormat.format(HsphereToolbox.parseLocalizedNumber(value));
            Session.getLog().debug("Value in Plan Wizard with name " + name + " after setting = " + newValue);
        } catch (ParseException e) {
        }
        if (this.svps == null) {
            this.svps = this.con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (?, ?, ?, ?)");
            this.svps.setInt(1, this.f207id);
            Session.getLog().debug("Prepeared statement svps");
        }
        this.svps.setInt(3, type);
        this.svps.setString(2, name);
        this.svps.setString(4, newValue);
        this.svps.executeUpdate();
    }

    @Override // psoft.hsphere.resource.plan_wizard.PlanValueSetter
    public void setValue(String name, String value) throws Exception {
        String newValue = value;
        Session.getLog().debug("Value in Plan Wizard with name " + name + " before setting = " + value);
        try {
            newValue = USFormat.format(HsphereToolbox.parseLocalizedNumber(value));
            Session.getLog().debug("Value in Plan Wizard with name " + name + " after setting = " + newValue);
        } catch (NullPointerException e) {
        } catch (ParseException e2) {
        }
        if (this.svps == null) {
            this.svps = this.con.prepareStatement("INSERT INTO plan_value (plan_id, name, type_id, value) VALUES (?, ?, ?, ?)");
            this.svps.setInt(1, this.f207id);
            Session.getLog().debug("Prepeared statement svps");
        }
        this.svps.setNull(3, 4);
        this.svps.setString(2, name);
        this.svps.setString(4, newValue);
        this.svps.executeUpdate();
    }

    public void setName(String name, String trial) throws Exception {
        int billingType = 1;
        if (trial != null && !"".equals(trial)) {
            billingType = Integer.parseInt(trial);
        }
        setName(name, billingType, 1, 0L);
    }

    public void setName(String name, int billingType, int cinfo, long resellerId) throws Exception {
        setId();
        if (this.snps == null) {
            this.snps = this.con.prepareStatement("INSERT INTO plans (id, description, disabled, billing, cinfo, reseller_id, deleted) VALUES (?, ?, ?, ?, ?, ?, ?)");
            this.snps.setInt(1, this.f207id);
        }
        this.snps.setString(2, name);
        this.snps.setInt(3, 1);
        this.snps.setInt(4, billingType);
        this.snps.setInt(5, cinfo);
        if (resellerId == 0) {
            this.snps.setLong(6, Session.getResellerId());
        } else {
            this.snps.setLong(6, resellerId);
        }
        this.snps.setInt(7, 0);
        this.snps.executeUpdate();
        Session.getLog().debug("Name=" + name + " has been set");
    }

    public void setAdminPlanName(String name, long resellerId) throws Exception {
        setId();
        if (this.saps == null) {
            this.saps = this.con.prepareStatement("INSERT INTO plans (id, description, disabled, billing, cinfo, reseller_id, deleted) VALUES (?, ?, ?, ?, ?, ? ,?)");
            this.saps.setInt(1, this.f207id);
        }
        this.saps.setString(2, name);
        this.saps.setInt(3, 0);
        this.saps.setInt(4, 0);
        this.saps.setInt(5, 0);
        if (resellerId == 0) {
            this.saps.setLong(6, Session.getResellerId());
        } else {
            this.saps.setLong(6, resellerId);
        }
        this.saps.setInt(7, 0);
        this.saps.executeUpdate();
        Session.getLog().debug("Name=" + name + " has been set");
    }

    protected void close() throws Exception {
        try {
            Session.closeStatement(this.tps);
            Session.closeStatement(this.svps);
            Session.closeStatement(this.amps);
            Session.closeStatement(this.amsps);
            Session.closeStatement(this.aivps);
            Session.closeStatement(this.snps);
            Session.closeStatement(this.saps);
            Session.closeStatement(this.drps);
            if (this.wasTrans) {
                this.con.close();
            } else {
                Session.commitTransConnection(this.con);
            }
        } catch (Throwable th) {
            if (this.wasTrans) {
                this.con.close();
            } else {
                Session.commitTransConnection(this.con);
            }
            throw th;
        }
    }

    public void abort() throws Exception {
        if (this.con != null) {
            try {
                Session.getLog().debug("Going to rollback in abort()");
                this.con.rollback();
                close();
            } catch (Throwable th) {
                close();
                throw th;
            }
        }
    }

    public Plan done() throws Exception {
        if (this.con != null) {
            try {
                Session.getLog().debug("Going to commit in done()");
                this.con.commit();
                Session.getLog().debug("Commited");
                close();
                Session.getLog().debug("Loading plan");
                return Plan.load(this.f207id);
            } catch (Throwable th) {
                close();
                throw th;
            }
        }
        throw new Exception("Error, connection was not initialized");
    }

    public void setResourcePrice(String type, String sprice, String rprice, String uprice, String freeUnits) throws Exception {
        int rtype = Integer.parseInt(TypeRegistry.getTypeId(type));
        if (!isEmpty(sprice)) {
            setValue(rtype, "_SETUP_CALC", "psoft.hsphere.calc.StandardSetupCalc");
            setValue(rtype, "_SETUP_PRICE_", sprice);
        }
        if (!isEmpty(rprice)) {
            setValue(rtype, "_RECURRENT_CALC", "psoft.hsphere.calc.StandardCalc");
            setValue(rtype, "_UNIT_PRICE_", rprice);
        }
        if (!isEmpty(uprice)) {
            setValue(rtype, "_USAGE_CALC", "psoft.hsphere.calc.StandardUsageCalc");
            setValue(rtype, "_USAGE_PRICE_", uprice);
        }
        if (!isEmpty(rprice)) {
            setValue(rtype, "_REFUND_CALC", "psoft.hsphere.calc.StandardRefundCalc");
        }
        if (!isEmpty(freeUnits)) {
            setValue(rtype, "_FREE_UNITS_", freeUnits);
        }
    }

    protected boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public void addMod(String type, String mod) throws Exception {
        try {
            if (this.amps == null) {
                this.amps = this.con.prepareStatement("INSERT INTO plan_imod(plan_id, type_id, mod_id,disabled) VALUES (?, ?, ?, 0)");
                this.amps.setInt(1, this.f207id);
            }
            this.amps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
            this.amps.setString(3, mod);
            this.amps.executeUpdate();
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such Type " + type, nste);
        }
    }

    public void addMods(String type, String[] mods) throws Exception {
        try {
            if (this.amsps == null) {
                this.amsps = this.con.prepareStatement("INSERT INTO plan_imod (plan_id, type_id, mod_id, disabled) VALUES (?, ?, ?, 0)");
                this.amsps.setInt(1, this.f207id);
                Session.getLog().debug("Prepared statement amsps");
            }
            for (String str : mods) {
                this.amsps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId(type)));
                this.amsps.setString(3, str);
                this.amsps.executeUpdate();
            }
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such Type " + type, nste);
        }
    }

    public void addIResource(String type, String mod, String newType, String newMod, int order) throws Exception {
        Object[] o = {type, mod, newType, newMod, new Integer(order)};
        this.iresource.add(o);
    }

    public void doneIResources() throws Exception {
        try {
            if (this.drps == null) {
                this.drps = this.con.prepareStatement("INSERT INTO plan_iresource (plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (?, ?, ?, ?, ?, ?, 0)");
                this.drps.setInt(1, this.f207id);
            }
            Iterator i = this.iresource.iterator();
            while (i.hasNext()) {
                Object[] entry = (Object[]) i.next();
                this.drps.setInt(2, Integer.parseInt(TypeRegistry.getTypeId((String) entry[0])));
                if (((String) entry[1]).length() == 0) {
                    this.drps.setNull(3, 12);
                } else {
                    this.drps.setString(3, (String) entry[1]);
                }
                this.drps.setInt(4, Integer.parseInt(TypeRegistry.getTypeId((String) entry[2])));
                if (((String) entry[3]).length() == 0) {
                    this.drps.setNull(5, 12);
                } else {
                    this.drps.setString(5, (String) entry[3]);
                }
                this.drps.setInt(6, ((Integer) entry[4]).intValue());
                this.drps.executeUpdate();
            }
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such Type ", nste);
        }
    }

    public void addIValue(String rtype, String mod, int type, String value, String label, int order) throws Exception {
        if (value.length() == 0) {
            return;
        }
        try {
            if (this.aivps == null) {
                this.aivps = this.con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value, label, order_id, disabled) VALUES (?, ?, ?, ?, ?, ?, ?, 0)");
                this.aivps.setInt(1, this.f207id);
            }
            int rType = Integer.parseInt(TypeRegistry.getTypeId(rtype));
            this.aivps.setInt(2, rType);
            this.aivps.setInt(3, type);
            if (mod.length() == 0) {
                this.aivps.setNull(4, 12);
            } else {
                this.aivps.setString(4, mod);
            }
            if (value.length() == 0) {
                this.aivps.setNull(5, 12);
            } else {
                String newValue = value;
                if (rType != 30 && rType != 32) {
                    try {
                        newValue = USFormat.format(HsphereToolbox.parseLocalizedNumber(value));
                    } catch (ParseException e) {
                    }
                }
                this.aivps.setString(5, newValue);
            }
            if (label.length() == 0) {
                this.aivps.setNull(6, 12);
            } else {
                this.aivps.setString(6, label);
            }
            this.aivps.setInt(7, order);
            this.aivps.executeUpdate();
        } catch (NoSuchTypeException nste) {
            Session.getLog().warn("No Such Type ", nste);
        }
    }
}
