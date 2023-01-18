package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateScalarModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.promotion.Promo;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/BillEntry.class */
public class BillEntry implements TemplateScalarModel, TemplateHashModel, Comparable {
    private List promoDiscounts;
    private String discountDescription = null;
    double summaryDiscount;

    /* renamed from: id */
    protected long f26id;
    protected long billId;
    protected int type;
    protected Date opened;
    protected long rid;
    protected long rtype;
    protected String description;
    protected double amount;
    protected double taxAmount;
    protected List taxes;
    protected Date canceled;
    protected Date started;
    protected Date ended;
    protected long billing_info_id;
    protected double balance;
    protected String note;
    protected long refunded;
    protected long planId;
    protected long periodId;
    protected int orderId;

    public boolean isEmpty() {
        return false;
    }

    public String getAsString() {
        return this.description + " " + this.amount;
    }

    public boolean isCredit() {
        return this.amount < 0.0d;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof BillEntry) {
            BillEntry billEntry = (BillEntry) o;
            return getId() == billEntry.getId();
        }
        return false;
    }

    public boolean isCharge() {
        return this.type == 5 || this.type == 8 || this.type == 6;
    }

    public TemplateModel get(String key) {
        if (key.equals("id")) {
            return new TemplateString(this.f26id);
        }
        if (key.equals("bill_id")) {
            return new TemplateString(this.billId);
        }
        if (key.equals("description")) {
            return new TemplateString(getDescription());
        }
        if (key.equals("date")) {
            return new TemplateString(new Date(this.opened.getTime()));
        }
        if (key.equals("is_credit")) {
            return new TemplateString(isCredit());
        }
        if (key.equals("tax")) {
            return new TemplateString(this.taxAmount);
        }
        if (key.equals("amount")) {
            return new TemplateString(isCredit() ? -this.amount : this.amount);
        } else if (key.equals("balance")) {
            if (Double.isNaN(this.balance)) {
                return null;
            }
            return new TemplateString(this.balance);
        } else if (key.equals("note")) {
            return new TemplateString(this.note);
        } else {
            if (key.equals("type")) {
                return new TemplateString(this.type);
            }
            if (key.equals("canceled")) {
                return new TemplateString(this.canceled);
            }
            if (key.equals("billing_info_id")) {
                return new TemplateString(Long.toString(this.billing_info_id));
            }
            if (key.equals("format_date")) {
                return new TemplateString(DateFormat.getDateInstance(2).format(this.opened));
            }
            if (key.equals("order_id")) {
                return new TemplateString(this.orderId);
            }
            return null;
        }
    }

    public long getPlanId() {
        return this.planId;
    }

    public long getPeriodId() {
        return this.periodId;
    }

    public long getBillId() {
        return this.billId;
    }

    public ResourceId getResourceId() {
        return new ResourceId(this.rid, (int) this.rtype);
    }

    public void canceled(Date canceled) throws Exception {
        this.canceled = canceled;
        cancelTaxEntry();
    }

    public BillEntry(long id, long billId, int type, Date opened, long rid, int rtype, String description, double amount, Date canceled, long billing_info_id, Date started, Date ended, double taxAmount, double balance, String note, long refunded, long planId, long periodId, List promoDiscounts, double summaryDiscount, int orderId) {
        this.promoDiscounts = null;
        this.summaryDiscount = 0.0d;
        this.f26id = id;
        this.billId = billId;
        this.type = type;
        this.opened = opened;
        this.rid = rid;
        this.rtype = rtype;
        this.description = description;
        this.amount = amount;
        this.canceled = canceled;
        this.billing_info_id = billing_info_id;
        this.started = started;
        this.ended = ended;
        this.taxAmount = taxAmount;
        this.balance = balance;
        this.note = note;
        this.refunded = refunded;
        this.promoDiscounts = promoDiscounts;
        this.summaryDiscount = summaryDiscount;
        this.planId = planId;
        this.periodId = periodId;
        this.orderId = orderId;
    }

    public boolean belongs(ResourceId rid) {
        return this.rid == rid.getId() && this.rtype == ((long) rid.getType());
    }

    public double getAmount() {
        return this.amount;
    }

    public double getTaxAmount() throws Exception {
        return this.taxAmount;
    }

    public int getType() {
        return this.type;
    }

    public Date getOpened() {
        return this.opened;
    }

    public Date getStarted() {
        return this.started;
    }

    public Date getEnded() {
        return this.ended;
    }

    public Date getCanceled() {
        return this.canceled;
    }

    public static BillEntry create(long billId, int type, Date opened, long rid, int rtype, String description, Date started, Date ended, String transId, double amount, double balance) throws Exception {
        return create(billId, type, opened, rid, rtype, description, started, ended, transId, amount, 0L, balance);
    }

    public static BillEntry create(long billId, int type, Date opened, long rid, int rtype, String description, Date started, Date ended, String transId, double amount, long billing_info_id, double balance) throws Exception {
        double discountTotal = 0.0d;
        List promoDiscounts = new ArrayList();
        long id = Resource.getNewId();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        double balance2 = balance - amount;
        String note = Session.getBillingNote();
        try {
            String promoCode = Session.getAccount().getPreferences().getPromoCode();
            List<Promo> activePlanPromotions = Session.getAccount().getPlan().getAppliablePromotions(promoCode);
            Session.getLog().debug("INSIDE BillEntry::create; promoCode=" + promoCode + " active promotions found " + activePlanPromotions.size());
            if (amount > 0.0d && (type == 2 || type == 1 || type == 3)) {
                try {
                    for (Promo p : activePlanPromotions) {
                        if (p.isPromoValid(type, Session.getAccount(), rtype)) {
                            try {
                                PromoDiscount pd = p.createDiscountBillEntry(id, Session.getAccount(), amount);
                                promoDiscounts.add(pd);
                                discountTotal += pd.getDiscount();
                            } catch (Exception ex) {
                                Session.getLog().debug("Error applying promo with id " + p.getId(), ex);
                            }
                        }
                    }
                } catch (Exception ex2) {
                    Session.getLog().error("Error calculating promotions ", ex2);
                }
            }
            double discountTotal2 = Math.rint(discountTotal * 100.0d) / 100.0d;
            double amount2 = amount - discountTotal2;
            double balance3 = balance2 + discountTotal2;
            ps = con.prepareStatement("INSERT INTO bill_entry (id, bill_id, type, created, rid, rtype, description, amount, billing_info_id, plan_id, period_id, started, ended, trans_id, tax_amount, balance, note, discount, order_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, id);
            ps.setLong(2, billId);
            ps.setInt(3, type == 16 ? 11 : type);
            ps.setTimestamp(4, new Timestamp(opened.getTime()));
            ps.setLong(5, rid);
            ps.setInt(6, rtype);
            ps.setString(7, description);
            ps.setDouble(8, amount2);
            ps.setLong(9, billing_info_id);
            ps.setInt(10, Session.getAccount().getPlan().getId());
            ps.setInt(11, Session.getAccount().getPeriodId());
            if (started != null) {
                ps.setTimestamp(12, new Timestamp(started.getTime()));
            } else {
                ps.setNull(12, 93);
            }
            if (ended != null) {
                ps.setTimestamp(13, new Timestamp(ended.getTime()));
            } else {
                ps.setNull(13, 91);
            }
            if (transId != null) {
                ps.setString(14, transId);
            } else {
                ps.setNull(14, 12);
            }
            ps.setDouble(15, 0.0d);
            ps.setDouble(16, balance3);
            if (note == null) {
                ps.setNull(17, 12);
            } else {
                ps.setString(17, note);
            }
            ps.setDouble(18, discountTotal2);
            ps.setInt(19, 0);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            BillEntry entry = new BillEntry(id, billId, type, opened, rid, rtype, description, amount2, null, billing_info_id, started, ended, 0.0d, balance3, note, 0L, Session.getAccount().getPlan().getId(), Session.getAccount().getPeriodId(), promoDiscounts, discountTotal2, 0);
            entry.createTaxEntries();
            return entry;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public long getId() {
        return this.f26id;
    }

    public void createResellerEntry(double amount, String description) throws Exception {
        if (Session.getAccount().isDemoAccount()) {
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO bill_entry_res(id, reseller_id, type, created, amount, description) VALUES (?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId());
            ps.setLong(2, Session.getResellerId());
            ps.setInt(3, getType());
            ps.setTimestamp(4, new Timestamp(getOpened().getTime()));
            ps.setDouble(5, amount);
            ps.setString(6, description);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public BillEntryReseller getResellerEntry() throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT reseller_id, type, created, amount, canceled, description FROM bill_entry_res WHERE id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BillEntryReseller billEntryReseller = new BillEntryReseller(getId(), rs.getLong(1), rs.getInt(2), rs.getTimestamp(3), rs.getDouble(4), rs.getTimestamp(5), rs.getString(6));
                Session.closeStatement(ps);
                con.close();
                return billEntryReseller;
            }
            Session.getLog().warn("Reseller log record not found, bill_entry.id=" + getId());
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void cancelResellerEntry() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE bill_entry_res SET canceled = ? WHERE id = ?");
            ps.setTimestamp(1, new Timestamp(getCanceled().getTime()));
            ps.setLong(2, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void cancelTaxEntry() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM tax_bill_entry WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            this.taxes = new ArrayList();
            this.taxAmount = 0.0d;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void createTaxEntries() throws Exception {
        if (this.type == 5 || this.type == 8 || this.type == 6 || this.type == 11) {
            return;
        }
        if (this.type == 16) {
            this.type = 11;
        }
        if (this.type == 17) {
            this.type = 6;
        }
        String country = "";
        String state = "";
        BillingInfoObject bi = Session.getAccount().getBillingInfo();
        if (bi != null) {
            if (bi.getBillingType() == -1) {
                country = Session.getAccount().getContactInfo().getCountry();
                state = Session.getAccount().getContactInfo().getState();
                if ("NA".equals(state)) {
                    state = Session.getAccount().getContactInfo().getState2();
                }
            } else {
                country = Session.getAccount().getBillingInfo().getCountry();
                state = Session.getAccount().getBillingInfo().getState();
                if ("NA".equals(state)) {
                    state = Session.getAccount().getBillingInfo().getState2();
                }
            }
        }
        Hashtable taxCur = TaxBillEntry.calculateTaxes(getAmount(), country, state);
        if (taxCur == null) {
            return;
        }
        Enumeration e = taxCur.keys();
        while (e.hasMoreElements()) {
            String taxId = (String) e.nextElement();
            double curTaxAmount = ((Double) taxCur.get(taxId)).doubleValue();
            this.taxAmount += curTaxAmount;
            if (this.taxes == null) {
                this.taxes = new ArrayList();
            }
            this.taxes.add(TaxBillEntry.create(getId(), Integer.parseInt(taxId), curTaxAmount));
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE bill_entry SET tax_amount = ?, balance = ? WHERE id = ?");
            ps.setDouble(1, this.taxAmount);
            this.balance -= this.taxAmount;
            ps.setDouble(2, this.balance);
            ps.setLong(3, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List getTaxes() throws Exception {
        if (this.taxes != null) {
            return this.taxes;
        }
        this.taxes = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT type, amount FROM tax_bill_entry WHERE id = ?");
            ps.setLong(1, getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TaxBillEntry tx = new TaxBillEntry(getId(), rs.getInt(1), rs.getDouble(2));
                this.taxes.add(tx);
            }
            Session.closeStatement(ps);
            con.close();
            return this.taxes;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getDiscountDescription() throws ParseException {
        if (this.discountDescription != null) {
            return this.discountDescription;
        }
        DecimalFormat df = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormat df1 = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        DecimalFormatSymbols dfs = df.getDecimalFormatSymbols();
        dfs.setCurrencySymbol("");
        df.setDecimalFormatSymbols(dfs);
        df1.setDecimalFormatSymbols(dfs);
        df1.setMinimumFractionDigits(4);
        df1.setMaximumFractionDigits(4);
        StringBuffer res = new StringBuffer("");
        if (this.summaryDiscount != 0.0d) {
            res.append('\n');
            Object[] objArr = new Object[1];
            objArr[0] = df.format(isCredit() ? -this.amount : this.amount + this.summaryDiscount);
            res.append(Localizer.translateMessage("bill.full_amount", objArr));
            res.append('\n');
            if (this.promoDiscounts.size() == 1) {
                res.append(((PromoDiscount) this.promoDiscounts.get(0)).getDescription());
                res.append(df.format(this.summaryDiscount));
            } else {
                res.append(Localizer.translateMessage("bill.summary_entry_discount", new Object[]{df.format(this.summaryDiscount)}));
                res.append('\n');
                for (PromoDiscount p : this.promoDiscounts) {
                    res.append(p.getDescription());
                    res.append(df1.format(p.getDiscount()));
                    res.append('\n');
                }
            }
            this.discountDescription = res.toString();
        }
        return this.discountDescription;
    }

    public String getDescription() {
        String res;
        if (this.summaryDiscount > 0.0d) {
            try {
                res = this.description + getDiscountDescription();
            } catch (ParseException pe) {
                Session.getLog().error("Error parsing ", pe);
                res = this.description;
            }
        } else {
            res = this.description;
        }
        return res;
    }

    public double getDiscount() {
        return this.summaryDiscount;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        if (o == null) {
            throw new NullPointerException("Comapred object canno't be null:" + getId());
        }
        if (!(o instanceof BillEntry)) {
            throw new ClassCastException("Comapred object is not BillEntry, it is " + o.getClass().getName());
        }
        if (equals(o)) {
            return 0;
        }
        Long d1 = new Long(getId());
        Long d2 = new Long(((BillEntry) o).getId());
        return d1.compareTo(d2);
    }

    public void setOrderId(int orderId) throws Exception {
        synchronized (this) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE bill_entry SET order_id = ? WHERE id = ?");
            ps.setLong(1, orderId);
            ps.setLong(2, getId());
            ps.executeUpdate();
            this.orderId = orderId;
            Session.closeStatement(ps);
            con.close();
        }
    }

    public int getOrderId() {
        return this.orderId;
    }

    public String toString() {
        return "BillEntry#" + this.f26id + ":" + getDescription() + ":" + this.amount + ":" + this.rid + ":" + this.rtype;
    }
}
