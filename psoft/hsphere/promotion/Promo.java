package psoft.hsphere.promotion;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import java.util.List;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.PromoDiscount;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateOKResult;
import psoft.hsphere.promotion.calc.PromoCalculator;
import psoft.hsphere.resource.NotFoundException;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/Promo.class */
public class Promo implements TemplateHashModel {

    /* renamed from: id */
    private long f115id;
    private String code;
    private PromoValidator validator;
    private PromoCalculator calc;
    private String name;
    private String billingDescr;
    private boolean affectSetup;
    private boolean affectRecurrent;
    private boolean affectUsage;
    private boolean active;
    private boolean codeless;
    private String validatorId;
    private String calcId;
    private static PromoConfig promoConfig = null;
    private static Hashtable cash = new Hashtable();

    public Promo(long id, String code, String name, String billingDescr, PromoValidator validator, PromoCalculator calc, boolean setup, boolean recurr, boolean usage, boolean isCodeLess, boolean isActive) {
        this.affectSetup = true;
        this.affectRecurrent = true;
        this.affectUsage = true;
        this.active = false;
        this.codeless = false;
        this.f115id = id;
        this.code = code;
        this.name = name;
        this.validator = validator;
        this.affectSetup = setup;
        this.affectRecurrent = recurr;
        this.affectUsage = usage;
        this.calc = calc;
        this.billingDescr = billingDescr;
        this.codeless = isCodeLess;
        this.active = isActive;
        this.validatorId = PromoConfig.getValidatorIdByClassName(validator.getClass().getName());
        this.calcId = PromoConfig.getCalcIdByClassName(calc.getClass().getName());
    }

    public double calcDiscount(Account a, double sum) {
        try {
            return this.calc.getPromoDiscount(a, sum);
        } catch (Throwable tr) {
            Session.getLog().error("An error occured while calculating promo discount:", tr);
            return 0.0d;
        }
    }

    public boolean isPromoValid(long billEntryType, Account a, int rt) {
        if ((billEntryType != 1 || !this.affectSetup) && ((billEntryType != 2 || !this.affectRecurrent) && (billEntryType != 3 || !this.affectUsage))) {
            return false;
        }
        try {
            if (this.active) {
                return this.validator.isPromoValid(a, rt);
            }
            return false;
        } catch (Exception e) {
            Session.getLog().error("Error validating promotion");
            return false;
        }
    }

    private static Hashtable getValidatorData(String validatorId) throws Exception {
        HttpServletRequest request = Session.getRequest();
        List<String> pvParamNames = PromoConfig.getPromoValidatorParams(validatorId);
        Session.getLog().debug("Inside createPromo pvParamNames=" + pvParamNames);
        Hashtable pvParams = new Hashtable();
        for (String pName : pvParamNames) {
            String param = request.getParameter("pv_" + pName);
            if (!isEmpty(param)) {
                pvParams.put(pName, param);
            } else {
                throw new Exception("Promo validator parameter " + pName + " is empty");
            }
        }
        return pvParams;
    }

    private static Hashtable getCalcData(String promoCalcId) throws Exception {
        HttpServletRequest request = Session.getRequest();
        PromoConfig promoConfig2 = promoConfig;
        List<String> pcParamNames = PromoConfig.getPromoCalcParams(promoCalcId);
        Session.getLog().debug("Inside createPromo pcParamNames=" + pcParamNames);
        Hashtable pcParams = new Hashtable();
        for (String pName : pcParamNames) {
            String param = request.getParameter("pc_" + pName);
            if (!isEmpty(param)) {
                pcParams.put(pName, param);
            } else {
                throw new Exception("Promo calculator parameter " + pName + " is empty");
            }
        }
        return pcParams;
    }

    public static Promo createPromo(String promoCode, String promoName, String billDescr, String promoValidatorId, String promoCalcId, boolean setup, boolean recurr, boolean usage, boolean codeLess) throws Exception {
        Hashtable pvParams = getValidatorData(promoValidatorId);
        Hashtable pcParams = getCalcData(promoCalcId);
        PromoConfig promoConfig2 = promoConfig;
        Class promoValidatorClass = PromoConfig.getPromoValidatorClass(promoValidatorId);
        PromoConfig promoConfig3 = promoConfig;
        Promo result = createPromo(promoCode, promoName, billDescr, promoValidatorClass, PromoConfig.getPromoCalculatorClass(promoCalcId), pvParams, pcParams, setup, recurr, usage, codeLess);
        getPromoHash().put(new Long(result.getId()), result);
        return result;
    }

    private static Promo createPromo(String promoCode, String promoName, String billDescr, Class promoValidator, Class promoCalc, Hashtable validatorData, Hashtable calcData, boolean setup, boolean recurr, boolean usage, boolean codeLess) throws Exception {
        PreparedStatement ps = null;
        Class[] argT = {Long.TYPE, Hashtable.class};
        long promoId = Session.getNewIdAsLong("promo_seq");
        Object[] validatorArgV = {new Long(promoId), validatorData};
        Object[] calcArgV = {new Long(promoId), calcData};
        Session.getLog().debug("INSIDE createPromo: validator - " + promoValidator.getName() + " calculator - " + promoCalc.getName() + " validatorData=" + validatorData + " calcData=" + calcData + " code=" + promoCode + " codeless=" + codeLess);
        boolean wasTrans = Session.isTransConnection();
        Connection con = wasTrans ? Session.getDb() : Session.getTransConnection();
        try {
            try {
                ps = con.prepareStatement("INSERT INTO promo(id, code, descr, bill_descr, validator_class, calc_class, setup, recurrent, usage,  is_active, reseller_id, codeless) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                ps.setLong(1, promoId);
                if (!codeLess) {
                    ps.setString(2, promoCode);
                } else {
                    ps.setNull(2, 12);
                }
                ps.setString(3, promoName);
                ps.setString(4, billDescr);
                ps.setString(5, promoValidator.getName());
                ps.setString(6, promoCalc.getName());
                ps.setInt(7, setup ? 1 : 0);
                ps.setInt(8, recurr ? 1 : 0);
                ps.setInt(9, usage ? 1 : 0);
                ps.setInt(10, 0);
                ps.setLong(11, Session.getResellerId());
                ps.setLong(12, codeLess ? 1L : 0L);
                PromoValidator pv = (PromoValidator) promoValidator.getConstructor(argT).newInstance(validatorArgV);
                PromoCalculator pc = (PromoCalculator) promoCalc.getConstructor(argT).newInstance(calcArgV);
                ps.executeUpdate();
                Promo promo = new Promo(promoId, promoCode, promoName, billDescr, pv, pc, setup, recurr, usage, codeLess, false);
                Session.closeStatement(ps);
                if (!wasTrans) {
                    Session.commitTransConnection(con);
                } else {
                    con.close();
                }
                return promo;
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            if (!wasTrans) {
                Session.commitTransConnection(con);
            } else {
                con.close();
            }
            throw th;
        }
    }

    private static Hashtable loadAllPromoForReseller() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        Hashtable result = new Hashtable();
        Class[] argT = {Long.TYPE};
        try {
            ps = con.prepareStatement("SELECT id, code, descr, bill_descr, validator_class, calc_class,  setup, recurrent, usage, codeless, is_active  FROM promo WHERE reseller_id = ? ORDER BY id");
            ps.setLong(1, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Object[] argV = {new Long(rs.getLong("id"))};
                    PromoValidator pv = (PromoValidator) Class.forName(rs.getString("validator_class")).getConstructor(argT).newInstance(argV);
                    PromoCalculator pc = (PromoCalculator) Class.forName(rs.getString("calc_class")).getConstructor(argT).newInstance(argV);
                    Promo p = new Promo(rs.getLong("id"), rs.getString("code"), rs.getString("descr"), rs.getString("bill_descr"), pv, pc, rs.getInt("setup") == 1, rs.getInt("recurrent") == 1, rs.getInt("usage") == 1, rs.getInt("codeless") == 1, rs.getInt("is_active") == 1);
                    result.put(new Long(p.getId()), p);
                } catch (Exception ex) {
                    Session.getLog().error("Unable to load promo with id " + rs.getLong("id"), ex);
                }
            }
            Session.closeStatement(ps);
            con.close();
            return result;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(this.f115id);
        }
        if ("name".equals(key)) {
            return new TemplateString(this.name);
        }
        if ("code".equals(key)) {
            return new TemplateString(getCode());
        }
        if ("is_codeless".equals(key)) {
            return new TemplateString(isCodeLess());
        }
        if ("billing_description".equals(key)) {
            return new TemplateString(this.billingDescr);
        }
        if ("STATUS".equals(key)) {
            return new TemplateString("OK");
        }
        if ("setup".equals(key)) {
            return new TemplateString(this.affectSetup);
        }
        if ("recurrent".equals(key)) {
            return new TemplateString(this.affectRecurrent);
        }
        if ("usage".equals(key)) {
            return new TemplateString(this.affectUsage);
        }
        if ("isActive".equals(key)) {
            return new TemplateString(this.active);
        }
        if ("validator_node".equals(key)) {
            return PromoConfig.getPromoValidatorNode(this.validatorId);
        }
        if ("calc_node".equals(key)) {
            return PromoConfig.getPromoCalcNode(this.calcId);
        }
        if ("validator_data".equals(key)) {
            return new TemplateHash(((AbstractPromoDataStorage) this.validator).getData());
        }
        if ("calc_data".equals(key)) {
            return new TemplateHash(((AbstractPromoDataStorage) this.calc).getData());
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    protected static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public long getId() {
        return this.f115id;
    }

    public static synchronized Hashtable getPromoHash() throws Exception {
        Hashtable resellerPromoHash = (Hashtable) cash.get(new Long(Session.getResellerId()));
        if (resellerPromoHash == null) {
            resellerPromoHash = loadAllPromoForReseller();
            cash.put(new Long(Session.getResellerId()), resellerPromoHash);
        }
        return resellerPromoHash;
    }

    public static Promo getPromo(long promoId) throws Exception {
        Promo result = (Promo) getPromoHash().get(new Long(promoId));
        if (result != null) {
            return result;
        }
        throw new NotFoundException("Promo with id " + promoId + " not found");
    }

    public void updatePromo(String newCode, String newName, String newBillDescr, boolean setup, boolean recurrent, boolean usage, boolean codeless) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE promo SET code = ?, descr = ?, bill_descr = ?, setup = ?, recurrent = ?, usage = ?, codeless= ?  WHERE id = ?");
            if (!codeless) {
                ps.setString(1, newCode);
            } else {
                ps.setNull(1, 12);
            }
            ps.setString(2, newName);
            ps.setString(3, newBillDescr);
            ps.setInt(4, setup ? 1 : 0);
            ps.setInt(5, recurrent ? 1 : 0);
            ps.setInt(6, usage ? 1 : 0);
            ps.setInt(7, codeless ? 1 : 0);
            ps.setLong(8, getId());
            ps.executeUpdate();
            this.code = codeless ? null : newCode;
            this.name = newName;
            this.billingDescr = newBillDescr;
            this.affectSetup = setup;
            this.affectRecurrent = recurrent;
            this.affectUsage = usage;
            this.codeless = codeless;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_updatePromo(String newCode, String newName, String newBillDescr, String setup, String recurrent, String usage, String codeLess) throws Exception {
        Session.getLog().debug("Inside FM_updatePromo: newName=" + newName + " newBillDescr=  " + newBillDescr + " setup=" + setup + " recurrent=" + recurrent + " usage=" + usage);
        Hashtable pvParams = getValidatorData(this.validatorId);
        Hashtable pcParams = getCalcData(this.calcId);
        boolean wasTrans = Session.isTransConnection();
        Connection con = wasTrans ? Session.getDb() : Session.getTransConnection();
        try {
            try {
                boolean _codeless = "ON".equals(codeLess.toUpperCase());
                updatePromo(newCode, newName, newBillDescr, "ON".equals(setup.toUpperCase()), "ON".equals(recurrent.toUpperCase()), "ON".equals(usage.toUpperCase()), _codeless);
                this.validator.updateData(getId(), pvParams);
                this.calc.updateData(getId(), pcParams);
                if (!wasTrans) {
                    Session.commitTransConnection(con);
                } else {
                    con.close();
                }
                return this;
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            }
        } catch (Throwable th) {
            if (!wasTrans) {
                Session.commitTransConnection(con);
            } else {
                con.close();
            }
            throw th;
        }
    }

    public void setState(boolean st) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE promo SET is_active = ? WHERE id = ? AND reseller_id = ?");
            ps.setInt(1, st ? 1 : 0);
            ps.setLong(2, getId());
            ps.setLong(3, Session.getResellerId());
            ps.executeUpdate();
            this.active = st;
            Session.getLog().debug("Promo id=" + getId() + " activity set to " + st);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setState(String state) throws Exception {
        setState("TRUE".equals(state.toUpperCase()));
        return this;
    }

    public void delete() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        StringBuffer usedBy = new StringBuffer();
        try {
            ps = con.prepareStatement("SELECT p.id, p.description, pv.value FROM plans p, plan_value pv WHERE pv.name= ? AND pv.plan_id = p.id");
            ps.setString(1, "_PROMOTIONS");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                StringTokenizer st = new StringTokenizer(rs.getString("value"), "|");
                while (st.hasMoreTokens()) {
                    String t = st.nextToken();
                    if (t.equals(Long.toString(getId()))) {
                        usedBy.append(rs.getString("id") + " " + rs.getString("description") + '\n');
                    }
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (usedBy.length() > 0) {
                throw new HSUserException("admin.promo.unable_to_delete", new Object[]{'\n' + usedBy.toString()});
            }
            deleteReal();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deleteReal() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM promo WHERE id = ? AND reseller_id = ?");
            ps.setLong(1, getId());
            ps.setLong(2, Session.getResellerId());
            ps.executeUpdate();
            this.validator.delete(getId());
            this.calc.delete(getId());
            getPromoHash().remove(new Long(getId()));
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_delete() throws Exception {
        delete();
        return new TemplateOKResult();
    }

    public boolean isActive() {
        return this.active;
    }

    public String getBillingDescr() {
        return this.billingDescr + " ";
    }

    public PromoDiscount createDiscountBillEntry(long beId, Account a, double amount) throws Exception {
        double discount = calcDiscount(a, amount);
        PreparedStatement ps = null;
        if (discount > 0.0d) {
            Connection con = Session.getDb();
            try {
                String description = getBillingDescr();
                ps = con.prepareStatement("INSERT INTO dsc_bill_entry (id, be_id, promo_id, description, discount) VALUES(?, ?, ?, ?, ?)");
                ps.setLong(1, Session.getNewIdAsLong("d_bill_entry_seq"));
                ps.setLong(2, beId);
                ps.setLong(3, getId());
                ps.setString(4, getBillingDescr());
                ps.setDouble(5, discount);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                return new PromoDiscount(discount, description, getId());
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return null;
    }

    public String getCode() {
        if (isCodeLess()) {
            return null;
        }
        return this.code;
    }

    public boolean isCodeLess() {
        return this.codeless;
    }

    public boolean equals(Object o) {
        return o != null && (o instanceof Promo) && ((Promo) o).getId() == this.f115id;
    }
}
