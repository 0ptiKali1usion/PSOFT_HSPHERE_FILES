package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import psoft.hsphere.HSUserException;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/BackupResource.class */
public class BackupResource extends Resource {
    public static final int DAY = 1;
    public static final int WEEK = 2;
    public static final int MONTH = 3;
    public static final int TASK_SCHEDULED = 1;
    public static final int TASK_ON_DEMAND = 2;
    private Hashtable backupTasks;
    private int regularBasisType;
    private Timestamp startDate;
    private long backupPerformerId;

    public BackupResource(ResourceId rId) throws Exception {
        super(rId);
        this.startDate = null;
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT type, start_date, performer_id FROM backup WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.regularBasisType = rs.getInt("type");
                this.startDate = rs.getTimestamp("start_date");
                this.backupPerformerId = rs.getLong("performer_id");
            } else {
                notFound();
            }
            loadActualBackupTasks();
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public BackupResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.startDate = null;
        Iterator i = initValues.iterator();
        int t = Integer.parseInt((String) i.next());
        if (t != 1 && t != 2 && t != 3) {
            throw new Exception("Illegal backup regular basis type " + t);
        }
        this.regularBasisType = t;
        this.backupPerformerId = Long.parseLong((String) i.next());
        this.startDate = formStartDate(TimeUtils.getSQLTimestamp(), initValues);
        Session.getLog().debug("Inside BackupResource(int, Collection) initValues=" + initValues + " formed startDate=" + this.startDate);
        createSchedule(getStartDate());
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO backup(id, type, start_date, performer_id) VALUES(?,?,?,?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, getRegularBasisType());
            Session.getLog().debug("Inside BackupResource::initDone() setting startDate=" + getStartDate());
            ps.setTimestamp(3, getStartDate());
            ps.setLong(4, getBackupPerformerId());
            saveSchedule();
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void saveSchedule() throws Exception {
        synchronized (this.backupTasks) {
            for (BackupTask bt : this.backupTasks.values()) {
                bt.save();
            }
        }
    }

    private void createSchedule(Timestamp sd) throws Exception {
        createSchedule(TimeUtils.getSQLTimestamp(), sd);
    }

    private void createSchedule(Timestamp startFrom, Timestamp sd) throws Exception {
        this.backupTasks = new Hashtable();
        Calendar c = TimeUtils.getCalendar();
        c.setTime(startFrom);
        c.add(2, 1);
        Timestamp periodEnd = new Timestamp(Math.min(getAccount().getPeriodEnd().getTime(), c.getTime().getTime()));
        c.setTime(sd);
        c.set(13, 0);
        c.set(14, 0);
        Session.getLog().debug("Inside createSchedule sd=" + sd + " regular basis = " + getRegularBasisType() + " from=" + sd + " to=" + periodEnd);
        while (c.getTime().getTime() < periodEnd.getTime()) {
            if (c.getTime().getTime() >= sd.getTime()) {
                addBackupTask(new Timestamp(c.getTime().getTime()), 1);
            }
            switch (getRegularBasisType()) {
                case 1:
                    c.add(5, 1);
                    break;
                case 2:
                    c.add(5, 7);
                    break;
                case 3:
                    c.add(2, 1);
                    break;
            }
        }
    }

    public int getRegularBasisType() {
        return this.regularBasisType;
    }

    private BackupTask addBackupTask(Timestamp time, int taskType) throws Exception {
        BackupTask bt;
        Session.getLog().debug("Adding new backup task scheduled on " + time);
        synchronized (this.backupTasks) {
            bt = new BackupTask(Session.getNewIdAsLong("backup_seq"), getId().getId(), 1, time, null, taskType);
            this.backupTasks.put(new Long(bt.getId()), bt);
        }
        return bt;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            ps1 = con.prepareStatement("DELETE FROM backup WHERE id = ?");
            ps2 = con.prepareStatement("DELETE FROM backup_log WHERE parent_id = ?");
            ps1.setLong(1, getId().getId());
            ps2.setLong(1, getId().getId());
            ps1.executeUpdate();
            ps2.executeUpdate();
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }

    private void loadActualBackupTasks() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        this.backupTasks = new Hashtable();
        try {
            synchronized (this.backupTasks) {
                ps = con.prepareStatement("SELECT id FROM backup_log WHERE parent_id = ? AND commenced >= ? ORDER BY commenced");
                ps.setLong(1, getId().getId());
                ps.setTimestamp(2, getStartDate());
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    BackupTask bt = BackupTask.get(rs.getLong("id"));
                    this.backupTasks.put(new Long(bt.getId()), bt);
                    Session.getLog().debug("Added backup task with commenced=" + bt.getScheduledOn());
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private static Timestamp formStartDate(Timestamp startTime, Collection initValues) {
        Iterator i = initValues.iterator();
        int rb = Integer.parseInt((String) i.next());
        i.next();
        Session.getLog().debug("Inside BackupResource::formStartDate values=" + initValues + " regularBasisType=" + rb + "startTime=" + startTime);
        Calendar c = TimeUtils.getCalendar();
        Calendar now = TimeUtils.getCalendar();
        now.setTime(startTime);
        int hour = Integer.parseInt((String) i.next());
        int min = Integer.parseInt((String) i.next());
        c.setTime(startTime);
        c.set(11, hour);
        c.set(12, min);
        c.set(13, 0);
        c.set(14, 0);
        if (rb == 1) {
            new Timestamp(c.getTime().getTime());
            while (c.before(now)) {
                c.add(5, 1);
            }
        } else if (rb == 2) {
            int day = Integer.parseInt((String) i.next());
            c.set(7, day);
            new Timestamp(c.getTime().getTime());
            while (c.before(now)) {
                c.add(5, 7);
            }
        } else if (rb == 3) {
            int day2 = Integer.parseInt((String) i.next());
            c.set(5, day2);
            new Timestamp(c.getTime().getTime());
            while (c.before(now)) {
                c.add(2, 1);
            }
        }
        Timestamp result = new Timestamp(c.getTime().getTime());
        Session.getLog().debug("StartDate=" + result);
        return result;
    }

    public Timestamp getStartDate() {
        return this.startDate;
    }

    private static String getServiceDescription(int regularBasisType, Timestamp date) {
        Calendar c = TimeUtils.getCalendar();
        c.setTime(date);
        SimpleDateFormat df = new SimpleDateFormat("E");
        SimpleDateFormat tf = new SimpleDateFormat("kk:mm");
        switch (regularBasisType) {
            case 1:
                return Localizer.translateMessage("backup.basis_type.day", new String[]{tf.format(c.getTime())});
            case 2:
                return Localizer.translateMessage("backup.basis_type.week", new String[]{df.format(c.getTime()), tf.format(c.getTime())});
            case 3:
                return Localizer.translateMessage("backup.basis_type.month", new String[]{"" + c.get(5), tf.format(c.getTime()), ""});
            default:
                return null;
        }
    }

    public String getRegularBasisDescription() {
        return getServiceDescription(getRegularBasisType(), getStartDate());
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        Calendar c = TimeUtils.getCalendar();
        c.setTime(getStartDate());
        if ("regular_basis_description".equals(key)) {
            return new TemplateString(getRegularBasisDescription());
        }
        if ("backup_tasks".equals(key)) {
            return new TemplateList(new TreeSet(getBackupTasks().values()));
        }
        if ("regular_basis_id".equals(key)) {
            return new TemplateString(getRegularBasisType());
        }
        if ("hour".equals(key)) {
            return new TemplateString(c.get(11));
        }
        if ("min".equals(key)) {
            return new TemplateString(c.get(12));
        }
        if ("day_of_week".equals(key)) {
            return new TemplateString(c.get(7));
        }
        if ("day_of_month".equals(key)) {
            return new TemplateString(c.get(5));
        }
        if ("performer_id".equals(key)) {
            return new TemplateString(getBackupPerformerId());
        }
        return super.get(key);
    }

    public Hashtable getBackupTasks() {
        return this.backupTasks;
    }

    @Override // psoft.hsphere.Resource
    public double getAmount() {
        return getBackupTasks().size();
    }

    @Override // psoft.hsphere.Resource
    public double getRecurrentMultiplier() {
        double scheduledTasks = 0.0d;
        synchronized (this.backupTasks) {
            for (BackupTask bt : this.backupTasks.values()) {
                if (bt.getTaskType() == 1) {
                    scheduledTasks += 1.0d;
                }
            }
        }
        double mult = scheduledTasks - getFreeNumber();
        return Math.max(0.0d, mult);
    }

    @Override // psoft.hsphere.Resource
    public double getUsageMultiplier() {
        int completedOnDemandBackups = 0;
        synchronized (this.backupTasks) {
            for (BackupTask bt : this.backupTasks.values()) {
                if (bt.getTaskType() == 2 && bt.getState() == 3) {
                    completedOnDemandBackups++;
                }
            }
        }
        double freeBackups = getFreeNumber();
        if (freeBackups >= completedOnDemandBackups) {
            return 0.0d;
        }
        return completedOnDemandBackups;
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        String objectName = "";
        try {
            objectName = getParent().get().get("name").toString();
        } catch (Exception ex) {
            Session.getLog().error("Unable to get backup service object name in getDescription method:", ex);
        }
        switch (this.regularBasisType) {
            case 1:
                return Localizer.translateMessage("backup.basis_type.daily", new String[]{Localizer.translateMessage("backup.object.ds"), objectName});
            case 2:
                return Localizer.translateMessage("backup.basis_type.weekly", new String[]{Localizer.translateMessage("backup.object.ds"), objectName});
            case 3:
                return Localizer.translateMessage("backup.basis_type.monthly", new String[]{Localizer.translateMessage("backup.object.ds"), objectName});
            default:
                return null;
        }
    }

    public static double getAmount(InitToken t) {
        Session.getLog().debug("Inside BackupResource::getAmount(InitToken t) t=" + t + " values=" + t.getValues());
        double amount = 0.0d;
        Iterator i = t.getValues().iterator();
        int rb = Integer.parseInt((String) i.next());
        if (rb != 1 && rb != 2 && rb != 3) {
            return 0.0d;
        }
        i.next();
        Timestamp sd = formStartDate(TimeUtils.getSQLTimestamp(), t.getValues());
        Calendar c = TimeUtils.getCalendar();
        c.setTime(new Timestamp(TimeUtils.currentTimeMillis()));
        c.add(2, 1);
        Timestamp periodEnd = new Timestamp(Math.min(t.getEndDate().getTime(), c.getTime().getTime()));
        c.setTime(sd);
        while (c.getTime().getTime() < periodEnd.getTime()) {
            if (c.getTime().getTime() >= sd.getTime()) {
                amount += 1.0d;
            }
            switch (rb) {
                case 1:
                    c.add(5, 1);
                    break;
                case 2:
                    c.add(5, 7);
                    break;
                case 3:
                    c.add(2, 1);
                    break;
            }
        }
        return amount;
    }

    public static double getRecurrentMultiplier(InitToken t) throws Exception {
        double mult = getAmount(t) - t.getFreeUnits();
        if (mult > 0.0d) {
            return mult;
        }
        return 0.0d;
    }

    public static String getDescription(InitToken t) {
        int totalEstimatedBackups = 0;
        int paidEstimatedBackups = 0;
        Iterator i = t.getValues().iterator();
        int rb = Integer.parseInt((String) i.next());
        i.next();
        try {
            totalEstimatedBackups = (int) getAmount(t);
            paidEstimatedBackups = (int) getRecurrentMultiplier(t);
        } catch (Exception ex) {
            Session.getLog().error("Unable to estimate quantity of backups", ex);
        }
        switch (rb) {
            case 1:
                return Localizer.translateMessage("backup.basis_type.daily", new String[]{Localizer.translateMessage("backup.object.ds"), "", Integer.toString(totalEstimatedBackups), Integer.toString(totalEstimatedBackups - paidEstimatedBackups)});
            case 2:
                return Localizer.translateMessage("backup.basis_type.weekly", new String[]{Localizer.translateMessage("backup.object.ds"), "", Integer.toString(totalEstimatedBackups), Integer.toString(totalEstimatedBackups - paidEstimatedBackups)});
            case 3:
                return Localizer.translateMessage("backup.basis_type.monthly", new String[]{Localizer.translateMessage("backup.object.ds"), "", Integer.toString(totalEstimatedBackups), Integer.toString(totalEstimatedBackups - paidEstimatedBackups)});
            default:
                return null;
        }
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundDescription(Date begin, Date end, double delta) throws Exception {
        return Localizer.translateMessage("backup.refund_change_description", new String[]{Integer.toString((int) (-delta)), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        int unusedBackups = getScheduledBackupTasksAmount() - getCompletedScheduledTasksAmount();
        Resource bakupObject = getParent().get();
        return Localizer.translateMessage("backup.refund_recurrent_description", new Object[]{Integer.toString(unusedBackups), bakupObject, bakupObject.get("name").toString(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentChangeDescripton(InitToken token, double delta) throws Exception {
        BackupResource br = (BackupResource) token.getRes();
        int oldAmount = br.getScheduledBackupTasksAmount() - br.getCompletedScheduledTasksAmount();
        int newAmount = (int) getRecurrentMultiplier(token);
        return Localizer.translateMessage("backup.recurrent_change_description", new String[]{Integer.toString(oldAmount), Integer.toString(newAmount), f42df.format(token.getStartDate()), f42df.format(token.getEndDate())});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        String objectName = "";
        int totalEstimatedBackups = 0;
        int paidEstimatedBackups = 0;
        try {
            objectName = getParent().get().get("name").toString();
        } catch (Exception ex) {
            Session.getLog().error("Unable to get backup service object name in getDescription method:", ex);
        }
        try {
            totalEstimatedBackups = getScheduledBackupTasksAmount();
            paidEstimatedBackups = (int) getRecurrentMultiplier();
        } catch (Exception ex2) {
            Session.getLog().error("Unable to estimate quantity of backups", ex2);
        }
        switch (this.regularBasisType) {
            case 1:
                return Localizer.translateMessage("backup.basis_type.daily", new String[]{Localizer.translateMessage("backup.object.ds"), objectName, Integer.toString(totalEstimatedBackups), Integer.toString(totalEstimatedBackups - paidEstimatedBackups)}) + " (" + f42df.format(begin) + " - " + f42df.format(end) + ")";
            case 2:
                return Localizer.translateMessage("backup.basis_type.weekly", new String[]{Localizer.translateMessage("backup.object.ds"), objectName, Integer.toString(totalEstimatedBackups), Integer.toString(totalEstimatedBackups - paidEstimatedBackups)}) + " (" + f42df.format(begin) + " - " + f42df.format(end) + ")";
            case 3:
                return Localizer.translateMessage("backup.basis_type.monthly", new String[]{Localizer.translateMessage("backup.object.ds"), objectName, Integer.toString(totalEstimatedBackups), Integer.toString(totalEstimatedBackups - paidEstimatedBackups)}) + " (" + f42df.format(begin) + " - " + f42df.format(end) + ")";
            default:
                return null;
        }
    }

    public static String getResellerRecurrentChangeDescription(InitToken t, Date begin, Date end) {
        return "ToBeImplemented";
    }

    public static String getResellerRefundDescription(InitToken t, Date begin, Date end) {
        return "ToBeImplemented";
    }

    @Override // psoft.hsphere.Resource
    public double changeResource(Collection values) throws Exception {
        double oldAmount;
        Session.getLog().debug("Inside BackupResource::changeResource(Collection)");
        synchronized (this.backupTasks) {
            oldAmount = this.backupTasks.size();
            Iterator i = values.iterator();
            int rb = Integer.parseInt((String) i.next());
            Timestamp sd = formStartDate(TimeUtils.getSQLTimestamp(), values);
            clearSchedule(getId().getId());
            this.backupTasks.clear();
            Session.getLog().debug("Old backup tasks cleared");
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE backup SET type = ?, start_date = ? WHERE id = ?");
            ps.setInt(1, rb);
            ps.setTimestamp(2, sd);
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            this.startDate = sd;
            this.regularBasisType = rb;
            createSchedule(this.startDate);
            Session.getLog().debug("New schedule created size=" + getBackupTasks().size());
            saveSchedule();
            Session.getLog().debug("New schedule saved");
            Session.closeStatement(ps);
            con.close();
        }
        return oldAmount;
    }

    private static void clearSchedule(long backupId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("DELETE FROM backup_log WHERE parent_id = ?");
            ps.setLong(1, backupId);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public int getScheduledBackupTasksAmount() {
        int result = 0;
        if (getBackupTasks() == null) {
            return 0;
        }
        synchronized (this.backupTasks) {
            for (BackupTask bt : getBackupTasks().values()) {
                if (bt.getTaskType() == 1) {
                    result++;
                }
            }
        }
        return result;
    }

    public int getCompletedScheduledTasksAmount() {
        int result = 0;
        if (getBackupTasks() == null) {
            return 0;
        }
        synchronized (this.backupTasks) {
            for (BackupTask bt : getBackupTasks().values()) {
                if (bt.getTaskType() == 1 && bt.getState() == 3) {
                    result++;
                }
            }
        }
        return result;
    }

    public int getIncompletedScheduledTasksAmount() {
        int result = 0;
        if (getBackupTasks() == null) {
            return 0;
        }
        synchronized (this.backupTasks) {
            for (BackupTask bt : getBackupTasks().values()) {
                if (bt.getTaskType() == 1 && bt.getState() == 1) {
                    result++;
                }
            }
        }
        return result;
    }

    public long getBackupPerformerId() {
        return this.backupPerformerId;
    }

    @Override // psoft.hsphere.Resource
    public String getUsageChargeDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.b_usage", new Object[]{this, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public void closePeriodMonthlyAction(Date nextDate) throws Exception {
        Session.getLog().debug("Inside BackupResource::monthlyAction nextDate=" + nextDate);
        String descr = "";
        List entries = Session.getAccount().getRefundedEntry(getId(), getPeriodBegin(), nextMonthlyBillingDate());
        double refund = calc(4, getPeriodBegin(), nextMonthlyBillingDate(), entries);
        if (refund < 0.0d) {
            descr = getRecurrentRefundDescription(getPeriodBegin(), nextMonthlyBillingDate());
            getAccount().getBill().addEntry(4, TimeUtils.getDate(), getId(), descr, getPeriodBegin(), nextMonthlyBillingDate(), null, refund);
        }
        Session.billingLog(refund, descr, 0.0d, "", "REFUND");
    }

    @Override // psoft.hsphere.Resource
    public void openPeriodMonthlyAction(Date nextDate) throws Exception {
        Timestamp nextMonthlyBillingDate = new Timestamp(getPeriodBegin().getTime());
        Timestamp nextStartDate = formStartDate(nextMonthlyBillingDate, getCurrentInitValues());
        Session.getLog().debug("Inside BackupResource::monthlyAction p_begin=" + getPeriodBegin() + " nextStartDate=" + nextStartDate + " next monthly billing date = " + nextMonthlyBillingDate(getPeriodBegin()));
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE backup SET start_date = ? WHERE id = ?");
            ps.setTimestamp(1, nextStartDate);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            this.startDate = nextStartDate;
            createSchedule(nextMonthlyBillingDate, nextStartDate);
            Session.getLog().debug("New schedule built size = " + getBackupTasks().values().size());
            saveSchedule();
            Session.getLog().debug("New schedule has been saved");
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public Collection getCurrentInitValues() {
        Collection result = new ArrayList();
        Calendar c = TimeUtils.getCalendar();
        c.setTime(getStartDate());
        int hour = c.get(11);
        int min = c.get(12);
        result.add(Integer.toString(getRegularBasisType()));
        result.add(Long.toString(getBackupPerformerId()));
        result.add(Integer.toString(hour));
        result.add(Integer.toString(min));
        switch (getRegularBasisType()) {
            case 2:
                result.add(Integer.toString(c.get(7)));
                break;
            case 3:
                result.add(Integer.toString(c.get(5)));
                break;
        }
        return result;
    }

    public TemplateModel FM_addOnDemandBackupTask(String date, int hour, int minute) throws Exception {
        Date scheduleBegin;
        Session.getLog().debug("Inside BackupResource::FM_addOnDemandBackupTask");
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
        Timestamp ts = new Timestamp(sdf.parse(date).getTime());
        Calendar c = TimeUtils.getCalendar(ts);
        c.set(11, hour);
        c.set(12, minute);
        c.set(13, 0);
        c.set(14, 0);
        Session.getLog().debug("parsed timestamp=" + ts);
        Timestamp timeToPerform = new Timestamp(c.getTime().getTime());
        Date pBegin = getPeriodBegin();
        Date now = TimeUtils.getDate();
        if (now.after(pBegin)) {
            scheduleBegin = now;
        } else {
            scheduleBegin = pBegin;
        }
        Date pEnd = new Timestamp(Math.min(nextMonthlyBillingDate().getTime(), getAccount().getPeriodEnd().getTime()));
        if ((timeToPerform.after(scheduleBegin) || timeToPerform.equals(scheduleBegin)) && (pEnd.after(timeToPerform) || pEnd.equals(timeToPerform))) {
            return addOnDemandBackupTask(timeToPerform);
        }
        SimpleDateFormat df = new SimpleDateFormat("EEE MMM dd yyyy HH:mm");
        throw new HSUserException("backup.task.add_task.task_beyond_schedule", new String[]{df.format(scheduleBegin), df.format(pEnd)});
    }

    private BackupTask addOnDemandBackupTask(Timestamp ts) throws Exception {
        BackupTask bt;
        synchronized (this.backupTasks) {
            for (BackupTask _bt : getBackupTasks().values()) {
                if (_bt.getScheduledOn().equals(ts)) {
                    throw new HSUserException("backup.task.already_planned", new String[]{ts.toString()});
                }
            }
            bt = addBackupTask(ts, 2);
            bt.save();
        }
        return bt;
    }
}
