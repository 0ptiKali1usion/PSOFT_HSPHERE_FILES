package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.epayment.CreditCard;
import psoft.epayment.CreditCardProcessingException;
import psoft.epayment.PaymentInstrument;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.promotion.SubsidizedPromotion;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/Bill.class */
public class Bill implements TemplateHashModel, Comparable {
    private double balance;
    private double startBalance;
    private double endBalance;
    private double credit;
    private Date negativeDate;

    /* renamed from: id */
    protected long f25id;
    protected long accountId;
    protected Date opened;
    protected Date closed;
    protected List entries;
    protected String description;
    protected double amount;

    public long getId() {
        return this.f25id;
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean equals(Object obj) {
        return getId() == ((Bill) obj).getId();
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("id")) {
            return new TemplateString(this.f25id);
        }
        if (key.equals("opened")) {
            return new TemplateString(this.opened);
        }
        if (key.equals("closed")) {
            return new TemplateString(this.closed);
        }
        if (key.equals("entries")) {
            return new TemplateList(getEntries());
        }
        if (key.equals("description")) {
            return new TemplateString(getDescription());
        }
        if (key.equals("desc_plan")) {
            return new TemplateString(getPlanDescription());
        }
        if (key.equals("desc_short")) {
            return new TemplateString(getShortDescription());
        }
        if (key.equals("start_balance")) {
            if (Double.isNaN(this.startBalance)) {
                return null;
            }
            return new TemplateString(this.startBalance);
        } else if (key.equals("debits")) {
            return new TemplateString(getDebits());
        } else {
            if (key.equals("credits")) {
                return new TemplateString(getCredits());
            }
            if (key.equals("end_balance")) {
                if (Double.isNaN(this.endBalance)) {
                    return null;
                }
                return new TemplateString(this.endBalance);
            } else if (key.equals("change")) {
                return new TemplateString(getChange());
            } else {
                if (key.equals("amount")) {
                    return new TemplateString(getTotal() + getTaxesTotal());
                }
                if (key.equals("subamount")) {
                    return new TemplateString(getTotal());
                }
                if (key.equals("balance")) {
                    return new TemplateString(getBalance());
                }
                if (key.equals("credit")) {
                    return new TemplateString(getCredit());
                }
                if (key.equals("customCredit")) {
                    return new TemplateString(getCustomCredit());
                }
                if (key.equals("negativeDate")) {
                    return new TemplateString(this.negativeDate);
                }
                if (key.equals("format_opened")) {
                    if (this.opened != null) {
                        return new TemplateString(DateFormat.getDateInstance(2).format(this.opened));
                    }
                    return null;
                } else if (key.equals("format_closed")) {
                    if (this.closed != null) {
                        return new TemplateString(DateFormat.getDateInstance(2).format(this.closed));
                    }
                    return null;
                } else if (key.equals("to_pay")) {
                    return new TemplateString(getToPay());
                } else {
                    if (key.equals("taxes")) {
                        return new TemplateHash(getTaxes());
                    }
                    if (key.equals("total_taxes")) {
                        return new TemplateString(getTaxesTotal());
                    }
                    return AccessTemplateMethodWrapper.getMethod(this, key);
                }
            }
        }
    }

    public String getDescription() {
        return getDescription(this.description, this.f25id);
    }

    public String getShortDescription() {
        return getShortDescription(this.accountId, this.f25id);
    }

    public String getPlanDescription() {
        return getPlanDescription(this.accountId, this.description);
    }

    public static String getDescription(String baseDescription, long billId) {
        return baseDescription != null ? baseDescription + "-" + String.valueOf(billId) : "#" + billId;
    }

    public static String getShortDescription(long accountId, long billId) {
        return "#" + String.valueOf(accountId) + "-" + String.valueOf(billId);
    }

    public static String getPlanDescription(long accountId, String baseDescription) {
        int ind;
        return (baseDescription == null || (ind = baseDescription.lastIndexOf(new StringBuilder().append("#").append(String.valueOf(accountId)).toString())) < 0) ? "" : baseDescription.substring(0, ind).trim();
    }

    public void charge(BillingInfoObject bi) throws Exception {
        charge(bi, false);
    }

    public void charge(BillingInfoObject bi, boolean force) throws Exception {
        this.balance = fix(this.balance);
        saveBalance();
        Session.getLog().info("balance -->" + this.balance + "; type: " + bi.getBillingType());
        if (bi.getBillingType() == 1) {
            if (this.balance >= (-getCredit(bi)) && (!force || this.balance >= 0.0d)) {
                checkCC(this.accountId, bi);
                return;
            }
            Session.getLog().info("Balance -->" + this.balance);
            try {
                HashMap res = bi.charge(this.accountId, getCCDescription(this.accountId), SubsidizedPromotion.process(-this.balance));
                if (bi.getPaymentInstrument() instanceof GenericCreditCard) {
                    GenericCreditCard gcc = (GenericCreditCard) bi.getPaymentInstrument();
                    if (gcc.getFailedChargeAttemptsCounter() > 0) {
                        gcc.clearFatts();
                    }
                }
                SubsidizedPromotion.record(this.balance, this);
                Date tmpDate = TimeUtils.getDate();
                CreditCard pi = (CreditCard) bi.getPaymentInstrument();
                addEntry(5, tmpDate, -1L, -1, Localizer.translateMessage("bill.b_charge", new Object[]{pi.getType(), pi.getHiddenNumber(), pi.getExp()}), tmpDate, null, res != null ? (String) res.get("id") : null, this.balance, bi.getId());
                Session.billingLog(this.balance, Localizer.translateMessage("bill.b_charge", new Object[]{pi.getType(), pi.getHiddenNumber(), pi.getExp()}), 0.0d, "", "CHARGE");
                Session.getLog().info("Balance After -->" + this.balance);
            } catch (CreditCardProcessingException ex) {
                Session.getLog().error("Error processing credit card for bi.id=" + bi.getId());
                throw new HSUserException("billing.cc_processing_error", new String[]{ex.getMessage()});
            } catch (Exception e) {
                Session.getLog().error("Charge error: ", e);
                if ((bi.getPaymentInstrument() instanceof GenericCreditCard) && Settings.get().getValue("max_fatts") != null) {
                    ((GenericCreditCard) bi.getPaymentInstrument()).incFatts();
                    Session.getLog().error("Error processing credit card for bi.id=" + bi.getId());
                    throw new HSUserException("billing.cc_processing_error", new String[]{e.getMessage()});
                }
                throw new HSUserException(e.getMessage());
            }
        } else if (this.balance < (-getCredit(bi))) {
            throw new BillingException("bill.over", this.balance, this.balance, getCredit(bi));
        }
    }

    public HashMap auth(BillingInfoObject bi) throws Exception {
        if (bi.getBillingType() == 1) {
            if (this.balance >= (-getCredit(bi))) {
                HashMap retVal = new HashMap();
                retVal.put("amount", new Double(-this.balance));
                checkCC(this.accountId, bi);
                return retVal;
            }
            Session.getLog().info("Balance before auth -->" + this.balance);
            try {
                HashMap authData = bi.auth(this.accountId, getCCDescription(this.accountId), SubsidizedPromotion.process(-this.balance));
                if (bi.getPaymentInstrument() instanceof GenericCreditCard) {
                    GenericCreditCard gcc = (GenericCreditCard) bi.getPaymentInstrument();
                    if (gcc.getFailedChargeAttemptsCounter() > 0) {
                        gcc.clearFatts();
                    }
                }
                CreditCard pi = (CreditCard) bi.getPaymentInstrument();
                Date tmpDate = TimeUtils.getDate();
                addEntry(7, tmpDate, -1L, -1, Localizer.translateMessage("bill.b_auth", new Object[]{pi.getType(), pi.getHiddenNumber(), pi.getExp()}), tmpDate, null, authData != null ? (String) authData.get("id") : null, 0.0d, bi.getId());
                Session.getLog().info("Balance after auth -->" + this.balance);
                return authData;
            } catch (CreditCardProcessingException ex) {
                Session.getLog().error("Error processing credit card for bi.id=" + bi.getId());
                throw new HSUserException("billing.cc_processing_error", new String[]{ex.getMessage()});
            } catch (Exception e) {
                Session.getLog().error("Auth error: ", e);
                if (bi.getPaymentInstrument() instanceof GenericCreditCard) {
                    ((GenericCreditCard) bi.getPaymentInstrument()).incFatts();
                    Session.getLog().error("Error processing credit cardi for bi.id=" + bi.getId());
                    throw new HSUserException("billing.cc_processing_error", new String[]{e.getMessage()});
                }
                throw new HSUserException(e.getMessage());
            }
        } else if (this.balance < (-getCredit(bi))) {
            throw new BillingException("bill.over", this.balance, this.balance, getCredit(bi));
        } else {
            HashMap retVal2 = new HashMap();
            retVal2.put("amount", new Double(-this.balance));
            return retVal2;
        }
    }

    public String getCCDescription(long accountId) {
        String description = Settings.get().getValue("CC_DESCRIPTION");
        if (description == null) {
            description = "";
        }
        try {
            if (!"1".equals(Settings.get().getValue("CC_DESCRIPTION_ONLY"))) {
                Account a = (Account) Account.get(new ResourceId(accountId, 0));
                description = description + " " + a.getPlan().getDescription() + "#" + accountId;
            }
        } catch (Throwable th) {
            Session.getLog().error("Unable to get description");
        }
        return description;
    }

    public void capture(BillingInfoObject bi, HashMap data) throws Exception {
        if (bi.getBillingType() == 1) {
            Session.getLog().info("Balance before capture -->" + this.balance);
            try {
                if (data.get("id") != null) {
                    HashMap res = bi.capture(this.accountId, getCCDescription(this.accountId), data);
                    if (bi.getPaymentInstrument() instanceof GenericCreditCard) {
                        GenericCreditCard gcc = (GenericCreditCard) bi.getPaymentInstrument();
                        if (gcc.getFailedChargeAttemptsCounter() > 0) {
                            gcc.clearFatts();
                        }
                    }
                    SubsidizedPromotion.record(this.balance, this);
                    CreditCard pi = (CreditCard) bi.getPaymentInstrument();
                    Date tmpDate = TimeUtils.getDate();
                    addEntry(8, tmpDate, -1L, -1, Localizer.translateMessage("bill.b_capture", new Object[]{pi.getType(), pi.getHiddenNumber(), pi.getExp()}), tmpDate, null, res != null ? (String) res.get("id") : null, -((Double) data.get("amount")).doubleValue(), bi.getId());
                    Session.getLog().info("Balance after capture -->" + this.balance);
                    return;
                }
                Session.getLog().debug("Empty capture(positive balance)");
            } catch (CreditCardProcessingException ex) {
                Session.getLog().error("Error processing credit card for bi.id=" + bi.getId());
                throw new HSUserException("billing.cc_processing_error", new String[]{ex.getMessage()});
            } catch (Exception e) {
                Session.getLog().error("Capture error: ", e);
                if (bi.getPaymentInstrument() instanceof GenericCreditCard) {
                    ((GenericCreditCard) bi.getPaymentInstrument()).incFatts();
                    Session.getLog().error("Error processing credit card for bi.id=" + bi.getId());
                    throw new HSUserException("billing.cc_processing_error", new String[]{e.getMessage()});
                }
                throw new HSUserException(e.getMessage());
            }
        }
    }

    public void void_auth(BillingInfoObject bi, HashMap data) throws Exception {
        if (bi.getBillingType() == 1) {
            Session.getLog().info("Balance before void_auth -->" + this.balance);
            HashMap res = null;
            if (data.get("id") != null) {
                res = bi.void_auth(this.accountId, getCCDescription(this.accountId), data);
            } else {
                Session.getLog().debug("Empty void_auth(positive balance)");
            }
            CreditCard pi = (CreditCard) bi.getPaymentInstrument();
            Date tmpDate = TimeUtils.getDate();
            addEntry(9, tmpDate, -1L, -1, Localizer.translateMessage("bill.b_void", new Object[]{pi.getType(), pi.getHiddenNumber(), pi.getExp()}), tmpDate, null, res != null ? (String) res.get("id") : null, 0.0d, bi.getId());
            Session.getLog().info("Balance after void_auth -->" + this.balance);
        }
    }

    public Bill() {
        this.entries = null;
    }

    public Bill(long id) throws Exception {
        this.entries = null;
        this.f25id = id;
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT account_id, opened, closed, description, amount, start_balance, end_balance FROM bill WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.accountId = rs.getLong(1);
                this.opened = rs.getDate(2);
                this.closed = rs.getDate(3);
                this.description = rs.getString(4);
                this.amount = rs.getDouble(5);
                this.startBalance = rs.getDouble(6);
                if (rs.wasNull()) {
                    this.startBalance = Double.NaN;
                }
                this.endBalance = rs.getDouble(7);
                if (rs.wasNull()) {
                    this.endBalance = Double.NaN;
                }
                loadBalance(false);
                Session.closeStatement(ps);
                con.close();
                return;
            }
            throw new Exception("Bill not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public static BillEntry findEntry(ResourceId rid, int entryType) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, type, created, rid, rtype, description, amount, canceled, started, ended, tax_amount, balance, note, refunded, discount, plan_id, period_id, order_id, bill_id  FROM bill_entry WHERE rid = ? AND rtype = ? AND type = ?");
            ps.setLong(1, rid.getId());
            ps.setInt(2, rid.getType());
            ps.setInt(3, entryType);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double balance = rs.getDouble("balance");
                if (rs.wasNull()) {
                    balance = Double.NaN;
                }
                double summaryDiscount = rs.getDouble("discount");
                List discounts = null;
                if (summaryDiscount > 0.0d) {
                    discounts = loadBillEntryDiscounts(rs.getLong("id"));
                }
                int orderId = rs.getInt("order_id");
                BillEntry billEntry = new BillEntry(rs.getLong("id"), rs.getLong("bill_id"), rs.getInt("type"), rs.getTimestamp("created"), rs.getLong("rid"), rs.getInt("rtype"), rs.getString("description"), rs.getDouble("amount"), rs.getTimestamp("canceled"), 0L, rs.getTimestamp("started"), rs.getTimestamp("ended"), rs.getDouble("tax_amount"), balance, rs.getString("note"), rs.getLong("refunded"), rs.getLong("plan_id"), rs.getLong("period_id"), discounts, summaryDiscount, orderId);
                Session.closeStatement(ps);
                con.close();
                return billEntry;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public synchronized void loadEntries() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        ArrayList entries = new ArrayList();
        try {
            ps = con.prepareStatement("SELECT id, type, created, rid, rtype, description, amount, canceled, started, ended, tax_amount, balance, note, refunded, discount, plan_id, period_id, order_id  FROM bill_entry WHERE bill_id = ? ORDER BY id");
            ps.setLong(1, this.f25id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double balance = rs.getDouble(12);
                if (rs.wasNull()) {
                    balance = Double.NaN;
                }
                double summaryDiscount = rs.getDouble("discount");
                List discounts = null;
                if (summaryDiscount > 0.0d) {
                    discounts = loadBillEntryDiscounts(rs.getLong("id"));
                }
                int orderId = rs.getInt("order_id");
                entries.add(new BillEntry(rs.getLong("id"), this.f25id, rs.getInt("type"), rs.getTimestamp("created"), rs.getLong("rid"), rs.getInt("rtype"), rs.getString("description"), rs.getDouble("amount"), rs.getTimestamp("canceled"), 0L, rs.getTimestamp("started"), rs.getTimestamp("ended"), rs.getDouble("tax_amount"), balance, rs.getString("note"), rs.getLong("refunded"), rs.getLong("plan_id"), rs.getLong("period_id"), discounts, summaryDiscount, orderId));
            }
            Session.closeStatement(ps);
            con.close();
            this.entries = entries;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            this.entries = entries;
            throw th;
        }
    }

    public Bill(String description, long accountId, Date opened) throws Exception {
        this.entries = null;
        this.opened = opened;
        this.accountId = accountId;
        this.f25id = Resource.getNewId();
        this.description = description;
        loadBalance(true);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO bill (id, account_id, opened, description, start_balance) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, this.f25id);
            ps.setLong(2, accountId);
            ps.setDate(3, new java.sql.Date(opened.getTime()));
            ps.setString(4, description);
            double balance = getBalance();
            this.startBalance = balance;
            ps.setDouble(5, balance);
            ps.executeUpdate();
            this.startBalance = getBalance();
            this.endBalance = Double.NaN;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public BillEntry getEntry(ResourceId rid, int entryType) {
        BillEntry e = null;
        for (BillEntry te : getEntries()) {
            if (te.belongs(rid) && te.type == entryType) {
                e = te;
            }
        }
        return e;
    }

    public List getEntries(ResourceId rid, Integer[] entryTypes) {
        List entries = new ArrayList();
        List entryTypesList = Arrays.asList(entryTypes);
        for (BillEntry te : getEntries()) {
            if (te.belongs(rid) && entryTypesList.contains(new Integer(te.type))) {
                entries.add(te);
            }
        }
        if (entries.size() > 0) {
            return entries;
        }
        return null;
    }

    public Iterator getTypeEntries(int entryType) {
        List l = new ArrayList();
        for (BillEntry te : getEntries()) {
            if (te.type == entryType) {
                l.add(te);
            }
        }
        return l.iterator();
    }

    public void cancel(ResourceId rid) throws Exception {
        Timestamp now = new Timestamp(TimeUtils.currentTimeMillis());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE bill_entry SET canceled = ? WHERE bill_id = ? AND rid = ? AND rtype = ?");
            ps.setTimestamp(1, now);
            ps.setLong(2, this.f25id);
            ps.setLong(3, rid.getId());
            ps.setInt(4, rid.getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            for (BillEntry entry : getEntries()) {
                if (entry.belongs(rid)) {
                    this.balance += entry.getAmount() + entry.getTaxAmount();
                    entry.canceled(now);
                    if (Session.getResellerId() != 1) {
                        try {
                            entry.cancelResellerEntry();
                        } catch (Exception e) {
                            Session.getLog().error("Reseller log record cancelation failed", e);
                        }
                    }
                    saveBalance();
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void cancel(long id) throws Exception {
        Timestamp now = new Timestamp(TimeUtils.currentTimeMillis());
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE bill_entry SET canceled = ? WHERE id = ?");
            ps.setTimestamp(1, now);
            ps.setLong(2, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            for (BillEntry entry : getEntries()) {
                if (entry.getId() == id) {
                    this.balance += entry.getAmount() + entry.getTaxAmount();
                    entry.canceled(now);
                    if (Session.getResellerId() != 1) {
                        try {
                            entry.cancelResellerEntry();
                        } catch (Exception e) {
                            Session.getLog().error("Reseller log record cancelation failed", e);
                        }
                    }
                    saveBalance();
                }
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public BillEntry addEntry(int type, Date created, ResourceId rid, String description, Date started, Date ended, String transId, double amount) throws Exception {
        return addEntry(type, created, rid, description, started, ended, transId, amount, 0L);
    }

    public BillEntry addEntry(int type, Date created, ResourceId rid, String description, Date started, Date ended, String transId, double amount, long billing_info_id) throws Exception {
        return addEntry(type, created, rid.getId(), rid.getType(), description, started, ended, transId, amount, billing_info_id);
    }

    public BillEntry addEntry(int type, Date created, long rid, int rtype, String description, Date started, Date ended, String transId, double amount) throws Exception {
        return addEntry(type, created, rid, rtype, description, started, ended, transId, amount, 0L);
    }

    public BillEntry addEntry(int type, Date created, long rid, int rtype, String description, Date started, Date ended, String transId, double amount, long billing_info_id) throws Exception {
        BillEntry entry;
        if (closed() != null) {
            throw new Exception("Trying to write in closed bill:" + getId());
        }
        Session.getLog().info("Saved record -->" + description + "|" + amount);
        List entries = getEntries();
        synchronized (entries) {
            if (billing_info_id == 0) {
                entry = BillEntry.create(this.f25id, type, created, rid, rtype, description, started, ended, transId, amount, this.balance);
            } else {
                entry = BillEntry.create(this.f25id, type, created, rid, rtype, description, started, ended, transId, amount, billing_info_id, this.balance);
            }
            entries.add(entry);
        }
        if (amount != 0.0d) {
            this.balance -= entry.getAmount() + entry.getTaxAmount();
        }
        saveBalance();
        return entry;
    }

    public void loadBalance(boolean isCreate) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT balance, credit, negative_date FROM balance_credit WHERE id = ?");
            ps.setLong(1, this.accountId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.balance = rs.getDouble(1);
                this.credit = rs.getDouble(2);
                this.negativeDate = rs.getDate(3);
                Session.getLog().info("Balance found");
            } else if (!isCreate) {
                throw new Exception("Balance info not found");
            } else {
                ps.close();
                ps = con.prepareStatement("INSERT INTO balance_credit (id, balance, credit) VALUES (?, 0, 0)");
                ps.setLong(1, this.accountId);
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    public void saveBalance() throws Exception {
        this.credit = fix(this.credit);
        if (this.negativeDate != null) {
            if (this.balance >= (-getCredit())) {
                this.negativeDate = null;
            }
        } else if (this.balance < (-getCredit())) {
            this.negativeDate = TimeUtils.getDate();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE balance_credit SET balance = ?, credit = ?, negative_date = ? WHERE id = ?");
            ps.setDouble(1, this.balance);
            ps.setDouble(2, this.credit);
            if (this.negativeDate != null) {
                ps.setDate(3, new java.sql.Date(this.negativeDate.getTime()));
            } else {
                ps.setNull(3, 91);
            }
            ps.setLong(4, this.accountId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public Date getTime() {
        return this.opened;
    }

    public Date opened() {
        return this.opened;
    }

    public Date closed() {
        return this.closed;
    }

    protected double fix(double val) {
        return Math.rint(val * 100.0d) / 100.0d;
    }

    public double getCredits() {
        double total = 0.0d;
        for (BillEntry entry : getEntries()) {
            if (entry.canceled == null && entry.getAmount() < 0.0d) {
                total -= entry.getAmount();
            }
        }
        return fix(total);
    }

    public double getDebits() {
        double total = 0.0d;
        for (BillEntry entry : getEntries()) {
            if (entry.canceled == null && entry.getAmount() > 0.0d) {
                total += entry.getAmount();
            }
        }
        return fix(total);
    }

    public double getChange() {
        if (Double.isNaN(this.startBalance)) {
            if (Double.isNaN(this.endBalance)) {
                return getBalance();
            }
            return this.endBalance;
        } else if (Double.isNaN(this.endBalance)) {
            return getBalance() - this.startBalance;
        } else {
            return this.endBalance - this.startBalance;
        }
    }

    public double getTotal() {
        double total = 0.0d;
        for (BillEntry entry : getEntries()) {
            if (!entry.isCharge() && entry.canceled == null) {
                total += entry.getAmount();
            }
        }
        this.amount = fix(total);
        return this.amount;
    }

    public double getTaxesTotal() {
        double total = 0.0d;
        for (BillEntry entry : getEntries()) {
            try {
                if (entry.canceled == null) {
                    total += entry.getTaxAmount();
                }
            } catch (Exception e) {
                Session.getLog().error("Error calc taxes for bill ID:", e);
            }
        }
        return fix(total);
    }

    public Hashtable getTaxes() {
        if (getTaxesTotal() == 0.0d) {
        }
        Hashtable taxes = new Hashtable();
        for (BillEntry entry : getEntries()) {
            try {
                if (entry.getTaxes() != null) {
                    for (TaxBillEntry tx : entry.getTaxes()) {
                        Double curTax = (Double) taxes.get(String.valueOf(tx.getType()));
                        if (curTax == null) {
                            taxes.put(String.valueOf(tx.getType()), new Double(tx.getAmount()));
                        } else {
                            taxes.put(String.valueOf(tx.getType()), new Double(curTax.doubleValue() + tx.getAmount()));
                        }
                    }
                }
            } catch (Exception e) {
                Session.getLog().error("Error getting taxes bill: " + getId(), e);
            }
        }
        Enumeration e2 = taxes.keys();
        while (e2.hasMoreElements()) {
            String taxId = (String) e2.nextElement();
            double taxAmount = ((Double) taxes.get(taxId)).doubleValue();
            taxes.put(taxId, new Double(fix(taxAmount)));
        }
        return taxes;
    }

    public void close() throws Exception {
        close(TimeUtils.getDate());
    }

    public void close(Date date) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE bill SET closed = ?, amount = ?, end_balance = ? WHERE id = ?");
            ps.setDate(1, new java.sql.Date(date.getTime()));
            ps.setDouble(2, getTotal() + getTaxesTotal());
            ps.setDouble(3, getBalance());
            ps.setLong(4, this.f25id);
            ps.executeUpdate();
            this.closed = date;
            this.endBalance = getBalance();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public List getEntries() {
        if (this.entries == null) {
            try {
                loadEntries();
            } catch (Exception e) {
                Session.getLog().error("Error loading billing entries", e);
            }
        }
        return this.entries;
    }

    public boolean isInDebt() {
        return this.balance < (-getCredit());
    }

    public void setCredit(double val) throws Exception {
        this.credit = val;
        saveBalance();
    }

    public double getCredit() {
        Account a = Session.getAccount();
        return getCredit(a.getBillingInfo());
    }

    public double getCredit(BillingInfoObject bi) {
        double planCredit;
        Account a = Session.getAccount();
        if (bi.getBillingType() == -1) {
            planCredit = a.getPlan().getTrialCredit();
        } else {
            planCredit = a.getPlan().getCredit();
        }
        return planCredit + this.credit;
    }

    public double getCustomCredit() {
        return this.credit;
    }

    public BillEntry getBillEntry(long id) {
        for (BillEntry entry : getEntries()) {
            if (entry.getId() == id) {
                return entry;
            }
        }
        return null;
    }

    public double getBalance() {
        return this.balance;
    }

    public double getToPay() {
        return -this.balance;
    }

    public Date getNegativeDate() {
        return this.negativeDate;
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        Bill obj = (Bill) o;
        return -((int) (getId() - obj.getId()));
    }

    public static void checkCC(BillingInfoObject bi) throws Exception {
        checkCC(-1L, bi);
    }

    public static void checkCC(long acctid, BillingInfoObject bi) throws Exception {
        try {
            PaymentInstrument pi = bi.getPaymentInstrument();
            if (pi instanceof GenericCreditCard) {
                short cvvChecked = ((CreditCard) pi).isCVVChecked();
                if (cvvChecked != 1) {
                    return;
                }
                Session.getLog().info("CVV code checking");
                bi.checkCC(acctid);
            }
        } catch (CreditCardProcessingException ex) {
            Session.getLog().error("Error validating credit card for bi.id=" + bi.getId());
            throw new HSUserException("billing.cc_processing_error", new String[]{ex.getMessage()});
        } catch (Exception e) {
            Session.getLog().error("Error validating credit card: ", e);
            if ((bi.getPaymentInstrument() instanceof GenericCreditCard) && Settings.get().getValue("max_fatts") != null) {
                ((GenericCreditCard) bi.getPaymentInstrument()).incFatts();
                Session.getLog().error("Error processing credit card for bi.id=" + bi.getId());
                throw new HSUserException("billing.cc_processing_error", new String[]{e.getMessage()});
            }
            throw new HSUserException(e.getMessage());
        }
    }

    private static List loadBillEntryDiscounts(long beId) throws SQLException {
        PreparedStatement ps = null;
        List res = new ArrayList();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id, discount, description, promo_id FROM dsc_bill_entry WHERE be_id = ?");
            ps.setLong(1, beId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                PromoDiscount p = new PromoDiscount(rs.getDouble("discount"), rs.getString("description"), rs.getLong("promo_id"));
                res.add(p);
            }
            Session.closeStatement(ps);
            con.close();
            return res;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_paymentNeedsCharge(double paymentSum) throws Exception {
        return new TemplateString(paymentNeedsCharge(paymentSum) ? "1" : "");
    }

    public boolean paymentNeedsCharge(double paymentSum) throws Exception {
        return getBalance() - paymentSum < (-getCredit());
    }
}
