package psoft.hsphere.billing;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Calendar;
import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.BillEntry;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/billing/ServiceEntry.class */
public class ServiceEntry implements TemplateHashModel {

    /* renamed from: id */
    protected long f76id;
    protected long accountId;
    protected Date created;
    protected int billingType;
    protected int frequency;
    protected int duration;
    protected int durationCount;
    protected Date lastBill;
    protected Date nextBill;
    protected String description;
    protected String note;
    protected String adminNote;
    protected double amount;
    protected Date deleted;

    public ServiceEntry(long id, long accountId, Date created, int billingType, int frequency, int duration, int durationCount, Date lastBill, Date nextBill, String description, String note, String adminNote, double amount) {
        this(id, accountId, created, billingType, frequency, duration, durationCount, lastBill, nextBill, description, note, adminNote, amount, null);
    }

    public ServiceEntry(long id, long accountId, Date created, int billingType, int frequency, int duration, int durationCount, Date lastBill, Date nextBill, String description, String note, String adminNote, double amount, Date deleted) {
        this.f76id = id;
        this.accountId = accountId;
        this.created = created;
        this.billingType = billingType;
        this.frequency = frequency;
        this.duration = duration;
        this.durationCount = durationCount;
        this.lastBill = lastBill;
        this.nextBill = nextBill;
        this.description = description;
        this.note = note;
        this.adminNote = adminNote;
        this.amount = amount;
        this.deleted = deleted;
    }

    public long getId() {
        return this.f76id;
    }

    public static ServiceEntry create(long accountId, int billingType, int frequency, int duration, double amount, String description, String note, String adminNote) throws Exception {
        double currentAmount;
        ServiceEntry serviceEntry;
        long id = Resource.getNewId();
        Calendar cal = TimeUtils.getCalendar();
        Date now = cal.getTime();
        Date deleted = null;
        if (billingType == 1 || billingType == 2) {
            cal.set(5, 1);
        }
        Date begin = cal.getTime();
        cal.add(2, frequency);
        Date end = TimeUtils.dropMinutes(cal.getTime());
        if (billingType == 1) {
            currentAmount = (amount * (end.getTime() - now.getTime())) / (end.getTime() - begin.getTime());
        } else {
            currentAmount = amount;
        }
        Account oldAccount = Session.getAccount();
        Account a = (Account) Account.get(new ResourceId(accountId, 0));
        BillEntry be = null;
        synchronized (a) {
            Connection con = Session.getDb();
            Session.setBillingNote(note);
            try {
                be = a.getBill().addEntry(2, now, ResourceId.getServiceId(id), description, begin, end, null, currentAmount);
                PreparedStatement ps = con.prepareStatement("INSERT INTO service(id, account_id, created, billing_type, frequency, duration, duration_count, next_bill,description, note, adm_note, last_bill, amount, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                java.sql.Date dNow = new java.sql.Date(now.getTime());
                ps.setLong(1, id);
                ps.setLong(2, accountId);
                ps.setDate(3, dNow);
                ps.setInt(4, billingType);
                ps.setInt(5, frequency);
                ps.setInt(6, duration);
                ps.setInt(7, 1);
                ps.setDate(8, new java.sql.Date(end.getTime()));
                ps.setString(9, description);
                ps.setString(10, note);
                ps.setString(11, adminNote);
                ps.setDate(12, dNow);
                ps.setDouble(13, amount);
                if (1 >= duration && duration > 0) {
                    deleted = TimeUtils.getDate();
                    ps.setDate(14, new java.sql.Date(deleted.getTime()));
                } else {
                    ps.setNull(14, 93);
                }
                ps.executeUpdate();
                serviceEntry = new ServiceEntry(id, accountId, now, billingType, frequency, duration, 1, now, end, description, note, adminNote, amount, deleted);
                Session.setAccount(oldAccount);
                Session.resetBillingNote();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                if (be != null) {
                    a.getBill().cancel(ResourceId.getServiceId(id));
                }
                throw ex;
            }
        }
        return serviceEntry;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if ("id".equals(key)) {
            return new TemplateString(getId());
        }
        if ("account_id".equals(key)) {
            return new TemplateString(this.accountId);
        }
        if ("billing_type".equals(key)) {
            return new TemplateString(this.billingType);
        }
        if ("frequency".equals(key)) {
            return new TemplateString(this.frequency);
        }
        if ("created".equals(key)) {
            return new TemplateString(this.created);
        }
        if ("duration".equals(key)) {
            return new TemplateString(this.duration);
        }
        if ("duration_count".equals(key)) {
            return new TemplateString(this.durationCount);
        }
        if ("lastBill".equals(key)) {
            return new TemplateString(this.lastBill);
        }
        if ("nextBill".equals(key)) {
            return new TemplateString(this.nextBill);
        }
        if ("description".equals(key)) {
            return new TemplateString(this.description);
        }
        if ("note".equals(key)) {
            return new TemplateString(this.note);
        }
        if ("admin_note".equals(key)) {
            return new TemplateString(this.adminNote);
        }
        if ("amount".equals(key)) {
            return new TemplateString(this.amount);
        }
        if ("deleted".equals(key)) {
            return new TemplateString(this.deleted);
        }
        return null;
    }

    public boolean isEmpty() {
        return false;
    }

    public void remove() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM service WHERE id = ?");
            ps.setLong(1, getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE service SET deleted = ? WHERE id = ?");
            this.deleted = TimeUtils.getSQLDate();
            ps.setDate(1, (java.sql.Date) this.deleted);
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

    public void bill() throws Exception {
        if (this.nextBill.after(TimeUtils.getDate())) {
            return;
        }
        BillEntry be = null;
        Date begin = this.nextBill;
        Calendar cal = TimeUtils.getCalendar(begin);
        cal.add(2, this.frequency);
        Date end = TimeUtils.dropMinutes(cal.getTime());
        PreparedStatement ps = null;
        Account account = null;
        Connection con = Session.getDb();
        Session.save();
        try {
            try {
                account = (Account) Account.get(new ResourceId(this.accountId, 0));
                Session.setAccount(account);
                User user = account.getUser();
                if (user != null) {
                    Session.setUser(user);
                }
                Bill bill = account.getBill();
                if (this.note != null && !this.note.equals("")) {
                    Session.setBillingNote(this.note);
                }
                be = bill.addEntry(2, TimeUtils.getDate(), ResourceId.getServiceId(getId()), this.description, begin, end, null, this.amount);
                if (this.duration > 0) {
                    this.durationCount++;
                }
                ps = con.prepareStatement("UPDATE service SET duration_count = ? , next_bill = ?, last_bill = ? " + ((this.durationCount < this.duration || this.duration <= 0) ? "" : ", deleted = ? ") + " WHERE id = ?");
                int i = 1 + 1;
                ps.setInt(1, this.durationCount);
                this.nextBill = end;
                int i2 = i + 1;
                ps.setDate(i, new java.sql.Date(this.nextBill.getTime()));
                this.lastBill = TimeUtils.getDate();
                int i3 = i2 + 1;
                ps.setDate(i2, new java.sql.Date(this.lastBill.getTime()));
                if (this.durationCount >= this.duration && this.duration > 0) {
                    this.deleted = TimeUtils.getDate();
                    i3++;
                    ps.setDate(i3, new java.sql.Date(this.deleted.getTime()));
                }
                int i4 = i3;
                int i5 = i3 + 1;
                ps.setLong(i4, getId());
                ps.executeUpdate();
                Session.restore();
                Session.resetBillingNote();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                if (be != null) {
                    account.getBill().cancel(ResourceId.getServiceId(this.f76id));
                }
                throw ex;
            }
        } catch (Throwable th) {
            Session.restore();
            Session.resetBillingNote();
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
