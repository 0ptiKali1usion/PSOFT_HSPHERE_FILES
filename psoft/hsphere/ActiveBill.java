package psoft.hsphere;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/ActiveBill.class */
public class ActiveBill extends Bill implements TemplateHashModel {
    protected Bill bill;

    public ActiveBill() {
        this.bill = null;
    }

    public ActiveBill(Bill bill) {
        this.bill = bill;
    }

    public Bill getBill() {
        return this.bill;
    }

    @Override // psoft.hsphere.Bill
    public long getId() {
        return getBill().getId();
    }

    @Override // psoft.hsphere.Bill
    public boolean isEmpty() {
        return false;
    }

    @Override // psoft.hsphere.Bill
    public TemplateModel get(String key) throws TemplateModelException {
        return getBill().get(key);
    }

    @Override // psoft.hsphere.Bill
    public void charge(BillingInfoObject bi, boolean force) throws Exception {
        synchronized (this) {
            getBill().charge(bi, force);
        }
    }

    @Override // psoft.hsphere.Bill
    public void charge(BillingInfoObject bi) throws Exception {
        synchronized (this) {
            getBill().charge(bi);
        }
    }

    @Override // psoft.hsphere.Bill
    public HashMap auth(BillingInfoObject bi) throws Exception {
        HashMap auth;
        synchronized (this) {
            auth = getBill().auth(bi);
        }
        return auth;
    }

    @Override // psoft.hsphere.Bill
    public String getCCDescription(long accountId) {
        String cCDescription;
        synchronized (this) {
            cCDescription = getBill().getCCDescription(accountId);
        }
        return cCDescription;
    }

    @Override // psoft.hsphere.Bill
    public void capture(BillingInfoObject bi, HashMap data) throws Exception {
        synchronized (this) {
            getBill().capture(bi, data);
        }
    }

    @Override // psoft.hsphere.Bill
    public void void_auth(BillingInfoObject bi, HashMap data) throws Exception {
        synchronized (this) {
            getBill().void_auth(bi, data);
        }
    }

    public void setBill(Bill bill) throws Exception {
        synchronized (this) {
            this.bill = bill;
        }
    }

    @Override // psoft.hsphere.Bill
    public BillEntry getEntry(ResourceId rid, int entryType) {
        return getBill().getEntry(rid, entryType);
    }

    @Override // psoft.hsphere.Bill
    public Iterator getTypeEntries(int entryType) {
        return getBill().getTypeEntries(entryType);
    }

    @Override // psoft.hsphere.Bill
    public void cancel(ResourceId rid) throws Exception {
        synchronized (this) {
            getBill().cancel(rid);
        }
    }

    @Override // psoft.hsphere.Bill
    public BillEntry addEntry(int type, Date created, ResourceId rid, String description, Date started, Date ended, String transId, double amount) throws Exception {
        return addEntry(type, created, rid, description, started, ended, transId, amount, 0L);
    }

    @Override // psoft.hsphere.Bill
    public BillEntry addEntry(int type, Date created, ResourceId rid, String description, Date started, Date ended, String transId, double amount, long billing_info_id) throws Exception {
        return addEntry(type, created, rid.getId(), rid.getType(), description, started, ended, transId, amount, billing_info_id);
    }

    @Override // psoft.hsphere.Bill
    public BillEntry addEntry(int type, Date created, long rid, int rtype, String description, Date started, Date ended, String transId, double amount) throws Exception {
        return addEntry(type, created, rid, rtype, description, started, ended, transId, amount, 0L);
    }

    @Override // psoft.hsphere.Bill
    public BillEntry addEntry(int type, Date created, long rid, int rtype, String description, Date started, Date ended, String transId, double amount, long billing_info_id) throws Exception {
        BillEntry addEntry;
        synchronized (this) {
            addEntry = getBill().addEntry(type, created, rid, rtype, description, started, ended, transId, amount, billing_info_id);
        }
        return addEntry;
    }

    @Override // psoft.hsphere.Bill
    public Date getTime() {
        return getBill().getTime();
    }

    @Override // psoft.hsphere.Bill
    public Date opened() {
        return getBill().opened();
    }

    @Override // psoft.hsphere.Bill
    public Date closed() {
        return getBill().closed();
    }

    @Override // psoft.hsphere.Bill
    public double getCredits() {
        double credits;
        synchronized (this) {
            credits = getBill().getCredits();
        }
        return credits;
    }

    @Override // psoft.hsphere.Bill
    public double getDebits() {
        double debits;
        synchronized (this) {
            debits = getBill().getDebits();
        }
        return debits;
    }

    @Override // psoft.hsphere.Bill
    public double getChange() {
        double change;
        synchronized (this) {
            change = getBill().getChange();
        }
        return change;
    }

    @Override // psoft.hsphere.Bill
    public double getTotal() {
        double total;
        synchronized (this) {
            total = getBill().getTotal();
        }
        return total;
    }

    @Override // psoft.hsphere.Bill
    public double getTaxesTotal() {
        double taxesTotal;
        synchronized (this) {
            taxesTotal = getBill().getTaxesTotal();
        }
        return taxesTotal;
    }

    @Override // psoft.hsphere.Bill
    public Hashtable getTaxes() {
        Hashtable taxes;
        synchronized (this) {
            taxes = getBill().getTaxes();
        }
        return taxes;
    }

    @Override // psoft.hsphere.Bill
    public void close() throws Exception {
        synchronized (this) {
            getBill().close(TimeUtils.getDate());
        }
    }

    @Override // psoft.hsphere.Bill
    public void close(Date date) throws Exception {
        synchronized (this) {
            getBill().close(date);
        }
    }

    @Override // psoft.hsphere.Bill
    public List getEntries() {
        List entries;
        synchronized (this) {
            entries = getBill().getEntries();
        }
        return entries;
    }

    @Override // psoft.hsphere.Bill
    public boolean isInDebt() {
        boolean isInDebt;
        synchronized (this) {
            isInDebt = getBill().isInDebt();
        }
        return isInDebt;
    }

    @Override // psoft.hsphere.Bill
    public void setCredit(double val) throws Exception {
        synchronized (this) {
            getBill().setCredit(val);
        }
    }

    @Override // psoft.hsphere.Bill
    public double getCredit() {
        double credit;
        synchronized (this) {
            credit = getBill().getCredit();
        }
        return credit;
    }

    @Override // psoft.hsphere.Bill
    public double getCredit(BillingInfoObject bi) {
        double credit;
        synchronized (this) {
            credit = getBill().getCredit(bi);
        }
        return credit;
    }

    @Override // psoft.hsphere.Bill
    public double getCustomCredit() {
        double customCredit;
        synchronized (this) {
            customCredit = getBill().getCustomCredit();
        }
        return customCredit;
    }

    @Override // psoft.hsphere.Bill
    public BillEntry getBillEntry(long id) {
        return getBill().getBillEntry(id);
    }

    @Override // psoft.hsphere.Bill
    public double getBalance() {
        double balance;
        synchronized (this) {
            balance = getBill().getBalance();
        }
        return balance;
    }

    @Override // psoft.hsphere.Bill
    public double getToPay() {
        double toPay;
        synchronized (this) {
            toPay = getBill().getToPay();
        }
        return toPay;
    }

    @Override // psoft.hsphere.Bill
    public Date getNegativeDate() {
        Date negativeDate;
        synchronized (this) {
            negativeDate = getBill().getNegativeDate();
        }
        return negativeDate;
    }

    @Override // psoft.hsphere.Bill
    public void loadBalance(boolean isCreate) throws Exception {
        synchronized (this) {
            getBill().loadBalance(isCreate);
        }
    }

    @Override // psoft.hsphere.Bill
    public void loadEntries() throws Exception {
        synchronized (this) {
            getBill().loadEntries();
        }
    }

    @Override // psoft.hsphere.Bill
    public void saveBalance() throws Exception {
        synchronized (this) {
            getBill().saveBalance();
        }
    }
}
