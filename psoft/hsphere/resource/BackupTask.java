package psoft.hsphere.resource;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import psoft.hsphere.HsphereToolbox;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/BackupTask.class */
public class BackupTask extends SharedObject implements TemplateHashModel, Comparable {
    public static final int PLANED = 1;
    public static final int COMMENCED = 2;
    public static final int FINISHED = 3;
    public static final int CANCELED = 4;
    private int state;
    private Timestamp scheduledOn;
    private Timestamp processedOn;
    private int taskType;
    private long parentId;

    public BackupTask(long id, long parentId, int state, Timestamp planedOn, Timestamp processed, int taskType) {
        super(id);
        this.scheduledOn = null;
        this.processedOn = null;
        this.parentId = parentId;
        this.state = state;
        this.scheduledOn = planedOn;
        this.processedOn = processed;
        this.taskType = taskType;
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public static BackupTask get(long id) throws Exception {
        BackupTask bt = (BackupTask) get(id, BackupTask.class);
        if (bt != null) {
            return bt;
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id, parent_id, state, task_type, commenced, finished  FROM backup_log WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                BackupTask backupTask = new BackupTask(rs.getLong("id"), rs.getLong("parent_id"), rs.getInt("state"), rs.getTimestamp("commenced"), rs.getTimestamp("finished"), rs.getInt("task_type"));
                Session.closeStatement(ps);
                con.close();
                return backupTask;
            }
            throw new Exception("Backup task with id " + id + " not found");
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if ("scheduled_on".equals(key)) {
            return new TemplateString(getScheduledOn() == null ? "" : HsphereToolbox.getShortDateTimeStr(getScheduledOn()));
        } else if ("state".equals(key)) {
            return new TemplateString(getState());
        } else {
            if ("processed_on".equals(key)) {
                return new TemplateString(getProcessedOn() == null ? Localizer.translateMessage("backup.task.processed.not_processed") : HsphereToolbox.getShortDateTimeStr(getProcessedOn()));
            } else if ("task_type".equals(key)) {
                return new TemplateString(getTaskType());
            } else {
                if ("status_description".equals(key)) {
                    return new TemplateString(getStateDescription());
                }
                if ("type_description".equals(key)) {
                    return new TemplateString(getTaskTypeDescription());
                }
                if ("status".equals(key)) {
                    return STATUS_OK;
                }
                return null;
            }
        }
    }

    public Timestamp getProcessedOn() {
        return this.processedOn;
    }

    public Timestamp getScheduledOn() {
        return this.scheduledOn;
    }

    public int getState() {
        return this.state;
    }

    public int getTaskType() {
        return this.taskType;
    }

    public boolean isCompleted() {
        return getState() == 3;
    }

    public String getStateDescription() {
        switch (getState()) {
            case 1:
                return Localizer.translateMessage("backup.task.state_planned");
            case 2:
                return Localizer.translateMessage("backup.task.state_commenced");
            case 3:
                return Localizer.translateMessage("backup.task.state_finished");
            case 4:
                return Localizer.translateMessage("backup.task.state_canceled");
            default:
                return null;
        }
    }

    public String getTaskTypeDescription() {
        switch (getTaskType()) {
            case 1:
                return Localizer.translateMessage("backup.task.type_scheduled");
            case 2:
                return Localizer.translateMessage("backup.task.type_on_demand");
            default:
                return null;
        }
    }

    public void cancelTask() throws Exception {
        if (getState() == 1) {
            changeTaskState(4);
        }
    }

    public void completeTask() throws Exception {
        if (getState() == 1) {
            changeTaskState(3);
        }
    }

    private void changeTaskState(int newState) throws Exception {
        if (getState() == newState) {
            return;
        }
        Timestamp processed = TimeUtils.getSQLTimestamp();
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("UPDATE backup_log SET finished = ?, state = ? WHERE id = ?");
            ps.setTimestamp(1, processed);
            ps.setInt(2, newState);
            ps.setLong(3, getId());
            ps.executeUpdate();
            this.state = newState;
            this.processedOn = processed;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // java.lang.Comparable
    public int compareTo(Object o) {
        BackupTask _bt = (BackupTask) o;
        if (equals(o)) {
            return 0;
        }
        if (getScheduledOn().after(_bt.getScheduledOn())) {
            return 1;
        }
        return -1;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof BackupTask) {
            BackupTask backupTask = (BackupTask) o;
            if (this.scheduledOn != null) {
                if (!this.scheduledOn.equals(backupTask.scheduledOn)) {
                    return false;
                }
                return true;
            } else if (backupTask.scheduledOn != null) {
                return false;
            } else {
                return true;
            }
        }
        return false;
    }

    public long getParentId() {
        return this.parentId;
    }

    public void save() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO backup_log(id, parent_id, state, commenced, finished, task_type) VALUES(?,?,?,?,?,?)");
            ps.setLong(1, getId());
            ps.setLong(2, getParentId());
            ps.setInt(3, getState());
            ps.setTimestamp(4, getScheduledOn());
            if (getProcessedOn() == null) {
                ps.setNull(5, 93);
            } else {
                ps.setTimestamp(5, getProcessedOn());
            }
            ps.setInt(6, getTaskType());
            ps.executeUpdate();
            Session.getLog().debug("Backup task with id " + getId() + " has been saved");
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
