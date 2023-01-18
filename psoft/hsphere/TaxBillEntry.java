package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Hashtable;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/TaxBillEntry.class */
public class TaxBillEntry implements TemplateHashModel {

    /* renamed from: id */
    protected long f54id;
    protected int type;
    protected double amount;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if (key.equals("id")) {
            return new TemplateString(new Long(this.f54id));
        }
        if (key.equals("amount")) {
            return new TemplateString(new Double(this.amount));
        }
        if (key.equals("type")) {
            return new TemplateString(this.type);
        }
        return null;
    }

    public TaxBillEntry(long id, int type, double amount) {
        this.f54id = id;
        this.type = type;
        this.amount = amount;
    }

    public long getId() {
        return this.f54id;
    }

    public double getAmount() {
        return this.amount;
    }

    public int getType() {
        return this.type;
    }

    public static TaxBillEntry create(long id, int type, double amount) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO tax_bill_entry(id, type, amount) VALUES (?, ?, ?)");
            ps.setLong(1, id);
            ps.setInt(2, type);
            ps.setDouble(3, amount);
            ps.executeUpdate();
            TaxBillEntry taxBillEntry = new TaxBillEntry(id, type, amount);
            Session.closeStatement(ps);
            con.close();
            return taxBillEntry;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static Hashtable calculateTaxes(double amount, String country, String state) throws Exception {
        return calculateTaxes(amount, country, state, null);
    }

    public static Hashtable calculateTaxes(double amount, String country, String state, BillingInfoObject bi) throws Exception {
        BillingInfoObject account_bi = null;
        try {
            account_bi = Session.getAccount().getBillingInfo();
        } catch (Exception e) {
        }
        if (bi == null) {
            bi = account_bi;
        }
        if (bi != null) {
            if (bi.taxExemptionApproved()) {
                return null;
            }
            if (bi != account_bi && bi.getExemptionCode() != null && !bi.taxExemptionRejected()) {
                return null;
            }
        }
        Hashtable resTaxes = Session.getReseller().getTaxes();
        if (resTaxes != null && resTaxes.size() > 0 && amount != 0.0d) {
            Hashtable taxes = new Hashtable();
            for (Tax tax : resTaxes.values()) {
                if (!tax.isDeleted() && tax.isApplicable(country, state)) {
                    tax.getPercent();
                    if (tax.getPercent() != 0.0d) {
                        double curTaxAmount = tax.getFactor() * amount;
                        taxes.put(String.valueOf(tax.getId()), new Double(curTaxAmount));
                        Session.getLog().debug("TAX ID:" + tax.getId() + " AMOUNT:" + curTaxAmount);
                    }
                }
            }
            return taxes;
        }
        return null;
    }
}
