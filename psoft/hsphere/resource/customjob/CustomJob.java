package psoft.hsphere.resource.customjob;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.Bill;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/customjob/CustomJob.class */
public class CustomJob extends SharedObject implements TemplateHashModel {
    public static final int NEW = 0;
    public static final int IN_PROGRESS = 1;
    public static final int DONE = 2;
    public static final int CANCELED = 3;
    public static final int ABORTED = 4;
    public static final int ON_HOLD = 5;

    /* renamed from: id */
    private long f188id;
    private long account_id;
    private String subject;
    private String description;
    private String entered_by;
    private Date start_date;
    private int status;
    private Date end_date;
    private long bill_id;
    private double total_time;
    private double total_amount;
    private TemplateList tasks;
    private TemplateList notes;

    public CustomJob(long id, long account_id, String subject, String description, String entered_by, Date start_date, int status, Date end_date, long bill_id, double total_time, double total_amount) throws Exception {
        super(id);
        this.f188id = id;
        this.account_id = account_id;
        this.subject = subject;
        this.description = description;
        this.entered_by = entered_by;
        this.start_date = start_date;
        this.status = status;
        this.end_date = end_date;
        this.bill_id = bill_id;
        this.total_time = total_time;
        this.total_amount = total_amount;
        this.tasks = getTasks(id);
        this.notes = getNotes(id);
    }

    public static CustomJob get(long id) throws Exception {
        Date end_date;
        CustomJob tmp = (CustomJob) get(id, CustomJob.class);
        if (tmp != null) {
            return tmp;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT subject, description, entered_by, start_date, end_date, status, bill_id, total_time, total_amount, account_id FROM custom_jobs WHERE id=?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String subject = rs.getString(1);
                String description = rs.getString(2);
                String entered_by = rs.getString(3);
                Date start_date = new Date(rs.getTimestamp(4).getTime());
                try {
                    end_date = new Date(rs.getTimestamp(5).getTime());
                } catch (Exception e) {
                    end_date = null;
                }
                int status = rs.getInt(6);
                long bill_id = rs.getInt(7);
                double total_time = rs.getDouble(8);
                double total_amount = rs.getDouble(9);
                long account_id = rs.getLong(10);
                CustomJob customJob = new CustomJob(id, account_id, subject, description, entered_by, start_date, status, end_date, bill_id, total_time, total_amount);
                Session.closeStatement(ps);
                con.close();
                return customJob;
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

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("id") ? new TemplateString(this.f188id) : key.equals("account_id") ? new TemplateString(this.account_id) : key.equals("subject") ? new TemplateString(this.subject) : key.equals("description") ? new TemplateString(this.description) : key.equals("entered_by") ? new TemplateString(this.entered_by) : key.equals("start_date") ? new TemplateString(this.start_date) : key.equals("status") ? new TemplateString(this.status) : key.equals("end_date") ? new TemplateString(this.end_date) : key.equals("bill_id") ? new TemplateString(this.bill_id) : key.equals("total_time") ? new TemplateString(this.total_time) : key.equals("total_amount") ? new TemplateString(this.total_amount) : key.equals("tasks") ? this.tasks : key.equals("notes") ? this.notes : super.get(key);
    }

    public static void remove(long id) throws Exception {
        remove(id, CustomJob.class);
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel FM_addTask(String enteredBy, String description, String note, double rate, double time) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO custom_job_tasks (id, entered_by, entered_date, description, note, rate, total_time) VALUES (?, ?, ?, ?, ?, ?, ?)");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setLong(1, this.f188id);
            ps.setString(2, enteredBy);
            ps.setTimestamp(3, now);
            ps.setString(4, description);
            Session.setClobValue(ps, 5, note);
            ps.setDouble(6, rate);
            ps.setDouble(7, time);
            ps.executeUpdate();
            TemplateModel templateMap = new TemplateMap();
            templateMap.put("entered_by", enteredBy);
            templateMap.put("entered_date", now);
            templateMap.put("description", description);
            templateMap.put("note", note);
            templateMap.put("rate", new TemplateString(rate));
            templateMap.put("total_time", new TemplateString(time));
            templateMap.put("total", new TemplateString(rate * time));
            this.tasks.add(templateMap);
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected static TemplateModel getTasks(long id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT entered_by, entered_date, description, note, rate, total_time FROM custom_job_tasks WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("entered_by", rs.getString(1));
                map.put("entered_date", rs.getTimestamp(2));
                map.put("description", rs.getString(3));
                map.put("note", Session.getClobValue(rs, 4));
                double rate = rs.getDouble(5);
                double time = rs.getDouble(6);
                map.put("rate", new TemplateString(rate));
                map.put("total_time", new TemplateString(time));
                map.put("total", new TemplateString(rate * time));
                list.add((TemplateModel) map);
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_addNote(String enteredBy, String note, int visibility) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO custom_job_notes (id, entered_by, entered_date, note, visibility) VALUES (?, ?, ?, ?, ?)");
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setLong(1, this.f188id);
            ps.setString(2, enteredBy);
            ps.setTimestamp(3, now);
            Session.setClobValue(ps, 4, note);
            ps.setInt(5, visibility);
            ps.executeUpdate();
            TemplateModel templateMap = new TemplateMap();
            templateMap.put("entered_by", enteredBy);
            templateMap.put("entered_date", now);
            templateMap.put("note", note);
            templateMap.put("visibility", new Integer(visibility));
            this.notes.add(templateMap);
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected static TemplateModel getNotes(long id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT entered_by, entered_date, note, visibility FROM custom_job_notes WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            TemplateList list = new TemplateList();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("entered_by", rs.getString(1));
                map.put("entered_date", rs.getTimestamp(2));
                map.put("note", Session.getClobValue(rs, 3));
                map.put("visibility", rs.getString(4));
                list.add((TemplateModel) map);
            }
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static int createJob(long accountId, String subject, String description, String enteredBy) throws Exception {
        int id = Session.getNewId("custom_job_seq");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO custom_jobs (id, account_id, subject, description, entered_by, start_date, status) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setInt(1, id);
            ps.setLong(2, accountId);
            ps.setString(3, subject);
            Session.setClobValue(ps, 4, description);
            ps.setString(5, enteredBy);
            ps.setTimestamp(6, new Timestamp(System.currentTimeMillis()));
            ps.setInt(7, 0);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return id;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel done() throws Exception {
        Connection con = Session.getDb();
        Session.save();
        try {
            Account a = (Account) Account.get(new ResourceId(this.account_id, 0));
            Session.setUser(a.getUser());
            Session.setAccount(a);
            double totalAmount = 0.0d;
            double totalTime = 0.0d;
            PreparedStatement ps = con.prepareStatement("SELECT rate, total_time FROM custom_job_tasks WHERE id = ?");
            ps.setLong(1, this.f188id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                double time = rs.getDouble(2);
                totalTime += time;
                totalAmount += time * rs.getDouble(1);
            }
            Session.closeStatement(ps);
            Bill bill = a.getBill();
            Date now = new Date();
            bill.addEntry(2, now, ResourceId.getCustomJobId(this.f188id), "Custom Job: " + this.subject + " #" + this.f188id, this.start_date, now, null, totalAmount);
            PreparedStatement ps2 = con.prepareStatement("UPDATE custom_jobs SET end_date = ?, status = ?, bill_id = ?, total_time = ?, total_amount = ? WHERE account_id = ? AND id = ?");
            ps2.setTimestamp(1, new Timestamp(now.getTime()));
            ps2.setInt(2, 2);
            ps2.setLong(3, bill.getId());
            ps2.setDouble(4, totalTime);
            ps2.setDouble(5, totalAmount);
            ps2.setLong(6, this.account_id);
            ps2.setLong(7, this.f188id);
            ps2.executeUpdate();
            Session.closeStatement(ps2);
            try {
                a.charge();
            } catch (Exception ex) {
                Session.getLog().error("Unable to charge account " + a.getId().getId() + " for custom job " + this.f188id, ex);
            }
            this.status = 2;
            this.total_amount = totalAmount;
            this.total_time = totalTime;
            this.end_date = now;
            con.close();
            Session.restore();
            return this;
        } catch (Throwable th) {
            con.close();
            Session.restore();
            throw th;
        }
    }

    public TemplateModel setStatus(int status) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE custom_jobs SET status = ? WHERE account_id = ? AND id = ?");
            ps.setInt(1, status);
            ps.setLong(2, this.account_id);
            ps.setLong(3, this.f188id);
            ps.executeUpdate();
            this.status = status;
            ps.close();
            con.close();
            return this;
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }
}
