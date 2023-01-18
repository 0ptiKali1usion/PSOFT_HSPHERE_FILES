package psoft.hsphere.background;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.admin.CCEncryption;
import psoft.hsphere.admin.PrivateKeyNotLoadedException;
import psoft.hsphere.exception.JobDataBrokenException;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.Base64;
import psoft.util.TimeUtils;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/background/BackgroundJob.class */
public abstract class BackgroundJob extends Thread implements Backgroundable, Serializable, TemplateHashModel {

    /* renamed from: cp */
    protected transient C0004CP f71cp;
    protected String dbMark;
    protected double percentDone;
    protected double stepValue;
    protected boolean running;
    protected boolean suspended;
    protected boolean impliedResume;
    protected boolean persistent;
    protected int loopDelay;
    protected long sleepInterval;
    protected long lastStart;
    protected long nextStart;
    protected long lastUser;
    protected int maxThreadCount;
    protected int status;

    /* renamed from: id */
    private long f72id;
    private String groupId;
    private boolean starting;
    protected String statusMessage;
    private long jobProgressLastTime;
    private long jobProgressCheckPeriod;
    public static final int NOT_INITIALIZED_STATUS = -2;
    public static final int UNKNOWN_STATUS = -1;
    public static final int FINISHED_STATUS = 0;
    public static final int RUNNING_STATUS = 1;
    public static final int STUCK_STATUS = 2;
    public static final int SUSPENDED_STATUS = 3;
    public static final int RESUMED_STATUS = 4;
    public static final int INACTIVE_STATUS = 5;
    public static final int INTERRUPTED_STATUS = 6;
    protected static int STATUS_DATA_DBFIELD_LENGTH = 1024;
    private transient Category log;

    protected abstract void processJob() throws Exception;

    public Category getLog() {
        if (this.log == null) {
            synchronized (this) {
                if (this.log == null) {
                    String className = getClass().getName();
                    int pInd = className.lastIndexOf(46);
                    if (pInd >= 0) {
                        className = "cron." + className.substring(pInd + 1);
                    }
                    this.log = Category.getInstance(className);
                }
            }
        }
        return this.log;
    }

    public BackgroundJob(C0004CP cp) throws Exception {
        this(cp, null);
    }

    public BackgroundJob(C0004CP cp, String dbMark) throws Exception {
        this(cp, dbMark, 60000L);
    }

    public BackgroundJob(C0004CP cp, String dbMark, long jobProgressCheckPeriod) throws Exception {
        this.dbMark = null;
        this.percentDone = 0.0d;
        this.stepValue = 0.0d;
        this.running = false;
        this.suspended = false;
        this.impliedResume = false;
        this.persistent = false;
        this.loopDelay = 0;
        this.lastStart = -1L;
        this.lastUser = 0L;
        this.maxThreadCount = 1;
        this.status = -2;
        this.groupId = "UNKNOWN";
        this.starting = false;
        this.statusMessage = null;
        this.jobProgressLastTime = 0L;
        this.log = null;
        this.f71cp = cp;
        this.f72id = getNewId();
        this.dbMark = dbMark;
        this.jobProgressCheckPeriod = jobProgressCheckPeriod;
        this.status = 5;
        setDaemon(true);
    }

    @Override // psoft.hsphere.background.Backgroundable
    public synchronized void setParams(Hashtable params) throws Exception {
        if (!this.running) {
            if (params.containsKey("period")) {
                this.sleepInterval = ((Long) params.get("period")).longValue();
            }
            if (params.containsKey("threadcount")) {
                this.maxThreadCount = ((Integer) params.get("threadcount")).intValue();
                getLog().debug("JOB " + getFullMark() + " is multithread: " + this.maxThreadCount);
            }
            if (params.containsKey("persistent") && ((Boolean) params.get("persistent")).booleanValue()) {
                this.persistent = true;
            }
            if (params.containsKey("loopdelay")) {
                this.loopDelay = ((Integer) params.get("loopdelay")).intValue();
            }
        }
    }

    protected static long getNewId() throws Exception {
        return Session.getNewId("background_seq");
    }

    public final String getDBMark() {
        return this.dbMark;
    }

    public static long getLastStart(String dbMark) throws Exception {
        long date = -1;
        if (dbMark != null) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT value FROM last_start WHERE name = ?");
                ps.setString(1, dbMark);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    if (rs.getTimestamp(1) != null) {
                        date = rs.getTimestamp(1).getTime();
                    } else {
                        throw new Exception("Wrong time format.");
                    }
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        if (date != -1) {
            return date;
        }
        throw new Exception("Unable to obtain the date of the last start for a backgrond job called '" + dbMark + "' from the database.");
    }

    @Override // psoft.hsphere.background.Backgroundable
    public void startJob() throws Exception {
        this.starting = true;
        start();
    }

    @Override // psoft.hsphere.background.Backgroundable
    public boolean isAliveJob() {
        return this.starting || isAlive();
    }

    protected void setLastStartDb() throws Exception {
        if (this.dbMark != null) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("UPDATE last_start SET value = ?, last_user = ? WHERE name = ?");
                ps.setTimestamp(1, new Timestamp(this.lastStart));
                ps.setLong(2, 0L);
                ps.setString(3, this.dbMark);
                int count = ps.executeUpdate();
                if (count < 1) {
                    ps.close();
                    ps = con.prepareStatement("INSERT INTO last_start(name,value) VALUES(?, ?)");
                    ps.setString(1, this.dbMark);
                    ps.setTimestamp(2, new Timestamp(this.lastStart));
                    ps.executeUpdate();
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        this.starting = false;
        this.f71cp.setConfig();
        this.f71cp.initLog();
        Session.setLog(getLog());
        if (isInterrupted()) {
            getLog().debug("Breaking job '" + getName() + "' cycle due to interruption");
            this.running = false;
            this.status = 6;
            try {
                saveStatusData();
            } catch (Exception ex) {
                getLog().error("Unable to save data for job '" + getName() + "'", ex);
            }
        } else {
            long prevStart = this.lastStart;
            this.lastStart = TimeUtils.currentTimeMillis();
            try {
                try {
                    this.running = true;
                    this.status = 1;
                    try {
                        processJob();
                    } catch (Exception e) {
                        getLog().error("Error during processing job " + getFullMark(), e);
                    }
                    setLastStartDb();
                    this.running = false;
                    this.status = -1;
                } catch (Exception e2) {
                    this.lastStart = prevStart;
                    getLog().error("Exception processing job: ", e2);
                    this.running = false;
                    this.status = -1;
                }
            } catch (Throwable th) {
                this.running = false;
                this.status = -1;
                throw th;
            }
        }
        getLog().debug("Job '" + getName() + "' has been completed.");
        if (!isInterrupted()) {
            this.status = 0;
            try {
                deleteStatusData(getId());
            } catch (Exception e3) {
                getLog().error("Error deleteing status data for job #" + this.f72id, e3);
            }
        }
    }

    @Override // psoft.hsphere.background.Backgroundable
    public void interruptJob() throws Exception {
        this.status = 6;
        if (isPersistent()) {
            try {
                saveStatusData();
            } catch (Exception e) {
                getLog().error("Unable to save data for job '" + getName() + "'", e);
            }
        }
        interrupt();
        if (isInterrupted()) {
            getLog().info("Job <" + getFullMark() + "> has been interrupted !");
        }
    }

    @Override // psoft.hsphere.background.Backgroundable
    public int getStatus() throws Exception {
        return this.status;
    }

    public String getStringStatus() throws Exception {
        switch (this.status) {
            case 0:
                return "FINISHED";
            case 1:
                return "RUNNING";
            case 2:
                return "STUCK";
            case 3:
                return "SUSPENDED";
            case 4:
                return "RESUMED";
            case 5:
                return "INACTIVE";
            default:
                return "UNKNOWN";
        }
    }

    protected String getCurrentJobInternalData() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(os);
        oos.writeObject(this);
        oos.flush();
        byte[] bytes = os.toByteArray();
        oos.close();
        String encoded = Base64.encode(bytes);
        return encoded;
    }

    private ArrayList getCurrentJobSplittedData() throws Exception {
        int idx;
        ArrayList res = new ArrayList();
        String data = getCurrentJobInternalData();
        if (data != null && !"".equals(data)) {
            int length = data.length();
            int i = 0;
            while (true) {
                idx = i;
                if (length - idx <= STATUS_DATA_DBFIELD_LENGTH) {
                    break;
                }
                int idx2 = idx + STATUS_DATA_DBFIELD_LENGTH;
                res.add(data.substring(idx, idx2));
                i = idx2;
            }
            res.add(data.substring(idx));
        }
        return res;
    }

    private static Object restoreJobObject(ArrayList splittedData) throws Exception {
        StringBuffer data = new StringBuffer();
        for (int i = 0; i < splittedData.size(); i++) {
            data.append((String) splittedData.get(i));
        }
        ByteArrayInputStream is = new ByteArrayInputStream(Base64.decode(data.toString()));
        ObjectInputStream ois = new ObjectInputStream(is);
        Object obj = ois.readObject();
        ois.close();
        return obj;
    }

    public synchronized void saveStatusData() throws Exception {
        ArrayList data = getCurrentJobSplittedData();
        if (data == null || "".equals(data)) {
            getLog().warn("Job <" + getFullMark() + "> has no data to save");
            return;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM bg_job_data WHERE id = ?");
                ps2.setLong(1, getId());
                ps2.executeUpdate();
                ps2.close();
                ps = con.prepareStatement("INSERT INTO bg_job_data (id, part, last_part, name, status, timemark, data) VALUES (?, ?, ?, ?, ?, ?, ?)");
                int lastPart = data.size() - 1;
                for (int part = 0; part <= lastPart; part++) {
                    ps.setLong(1, getId());
                    ps.setInt(2, part);
                    ps.setInt(3, lastPart);
                    ps.setString(4, getName());
                    ps.setInt(5, getStatus());
                    ps.setTimestamp(6, TimeUtils.getSQLTimestamp());
                    ps.setString(7, (String) data.get(part));
                    ps.executeUpdate();
                    ps.close();
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                getLog().error("Error saving status data for job #" + this.f72id, e);
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void saveLastUser(long lastUser) throws Exception {
        this.lastUser = lastUser;
        saveStatusData();
    }

    public static synchronized void deleteStatusData(long id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM bg_job_data WHERE id = ?");
            ps.setLong(1, id);
            ps.executeUpdate();
            ps.close();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    public static Object getStoredInstance(Long id, C0004CP cp) throws Exception {
        Connection con = Session.getDb();
        ArrayList allData = new ArrayList();
        PreparedStatement ps = null;
        int lastPart = -1;
        try {
            ps = con.prepareStatement("SELECT part, last_part, name, status, timemark, data FROM bg_job_data WHERE id = ? ORDER BY id, part");
            ps.setLong(1, id.longValue());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int part = rs.getInt("part");
                if (lastPart < 0) {
                    lastPart = rs.getInt("last_part");
                } else if (rs.getInt("last_part") != lastPart) {
                    throw new JobDataBrokenException("Corrupted data in the database for job #" + id);
                }
                if (part >= 0 && part <= lastPart) {
                    String data = rs.getString("data");
                    allData.add(part, data);
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (lastPart < 0 || allData.size() != lastPart + 1) {
                throw new JobDataBrokenException("Stored data for job #" + id + " were incomplete or missing.");
            }
            try {
                Object obj = restoreJobObject(allData);
                ((BackgroundJob) obj).f71cp = cp;
                return obj;
            } catch (InvalidClassException ce) {
                throw new JobDataBrokenException(ce.getMessage());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.background.Backgroundable
    public boolean isSuspended() {
        return this.suspended;
    }

    @Override // psoft.hsphere.background.Backgroundable
    public boolean isRunning() {
        return this.running;
    }

    public boolean isPersistent() {
        return this.persistent;
    }

    protected boolean isToBeResumed() {
        return this.impliedResume;
    }

    @Override // psoft.hsphere.background.Backgroundable
    public void suspendJob() throws Exception {
        if (isSuspended()) {
            throw new Exception("Job " + getFullMark() + " is already suspended");
        }
        this.suspended = true;
        this.status = 3;
        saveStatusData();
        getLog().debug("Job " + getFullMark() + " has been suspended");
    }

    @Override // psoft.hsphere.background.Backgroundable
    public synchronized void resumeJob() throws Exception {
        try {
            if (!isSuspended()) {
                throw new Exception("Job cannot be resumed");
            }
            this.suspended = false;
            this.status = 4;
            notify();
            getLog().debug("Job " + getFullMark() + " has been resumed");
        } catch (Exception e) {
            getLog().error("Cannot resume job " + getFullMark(), e);
        }
    }

    public void checkSuspended() {
        if (isSuspended()) {
            synchronized (this) {
                while (isSuspended()) {
                    try {
                        getLog().debug("Job " + getFullMark() + " is waiting");
                        wait();
                    } catch (Exception e) {
                        getLog().error("Cannot make wait job " + getFullMark(), e);
                    }
                }
            }
        } else if (this.loopDelay > 0) {
            try {
                TimeUtils.sleep(this.loopDelay);
            } catch (Exception e2) {
                getLog().error("Cannot delay job " + getFullMark(), e2);
            }
        }
    }

    @Override // psoft.hsphere.background.Backgroundable
    public int getProgress() {
        checkProgressStatus();
        return (int) this.percentDone;
    }

    @Override // psoft.hsphere.background.Backgroundable
    public synchronized int setProgress(int maxValue, int increment, int currentValue) throws Exception {
        if (maxValue <= 0 || increment <= 0 || currentValue < 0 || maxValue < currentValue) {
            throw new Exception("Wrong progress parameters (job " + getDBMark() + "): " + maxValue + " ;" + increment + " ;" + currentValue);
        }
        this.stepValue = (increment * 100.0d) / maxValue;
        this.percentDone = currentValue > 0 ? (currentValue / maxValue) * 100.0d : 0.0d;
        setProgressStatus(true);
        return getProgress();
    }

    public boolean isProgressInitialized() {
        return this.stepValue > 0.0d;
    }

    @Override // psoft.hsphere.background.Backgroundable
    public synchronized int addProgress(int stepCount) throws Exception {
        return addProgress(stepCount, null);
    }

    public synchronized int addProgress(int stepCount, String message) throws Exception {
        if (this.percentDone != 100.0d) {
            if (this.stepValue != 0.0d) {
                this.percentDone += stepCount * this.stepValue;
                if (this.percentDone > 100.0d) {
                    this.percentDone = 100.0d;
                }
            } else {
                throw new Exception(getFullMark() + ". Progress counter was not initialized");
            }
        }
        freshStatusMessage(message);
        setProgressStatus(true);
        return getProgress();
    }

    public void freshStatusMessage(String message) {
        if (message != null) {
            this.statusMessage = message;
            getLog().debug(message + " PERCENT:" + getProgress());
        }
    }

    protected boolean checkProgressStatus() {
        if (isAlive()) {
            if (this.status != 2 && System.currentTimeMillis() - this.jobProgressLastTime > this.jobProgressCheckPeriod) {
                setProgressStatus(false);
                return false;
            }
            return false;
        }
        getLog().error("Job '" + getFullMark() + "' is not alive.");
        if (this.status != 2) {
            setProgressStatus(false);
            return false;
        }
        return false;
    }

    private synchronized void setProgressStatus(boolean progress) {
        if (progress) {
            this.jobProgressLastTime = System.currentTimeMillis();
            if (this.status == 2) {
                this.status = 1;
            }
        } else if (this.status != 2) {
            this.status = 2;
        }
    }

    protected String getNoProgressTime() {
        long secs = (System.currentTimeMillis() - this.jobProgressLastTime) / 1000;
        long hh = secs / 3600;
        long secs2 = secs - (hh * 3600);
        int min = ((int) secs2) / 60;
        int sec = ((int) secs2) - (min * 60);
        return new String(String.valueOf(hh) + ":" + (min < 10 ? "0" : "") + String.valueOf(min) + ":" + (sec < 10 ? "0" : "") + String.valueOf(sec));
    }

    protected String getLastProgressDate() {
        return DateFormat.getDateTimeInstance(3, 2).format((Date) new java.sql.Date(this.jobProgressLastTime));
    }

    @Override // java.lang.Thread, psoft.hsphere.background.Backgroundable
    public long getId() {
        return this.f72id;
    }

    @Override // psoft.hsphere.background.Backgroundable
    public String getGroupId() {
        return this.groupId;
    }

    @Override // psoft.hsphere.background.Backgroundable
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getFullMark() {
        return getDBMark() + " [id=" + getId() + "]";
    }

    public TemplateModel get(String key) {
        try {
            if (key.equals("id")) {
                return new TemplateString(getId());
            }
            if (key.equals("name")) {
                return new TemplateString(getName() != null ? getName() : getDBMark());
            } else if (key.equals("group_id")) {
                return new TemplateString(getGroupId());
            } else {
                if (key.equals("priority")) {
                    return new TemplateString(getPriority());
                }
                if (key.equals("status")) {
                    return new TemplateString(getStringStatus());
                }
                if (key.equals("progress")) {
                    return new TemplateString(getProgress());
                }
                if (key.equals("status_message")) {
                    return new TemplateString(this.statusMessage != null ? this.statusMessage : "");
                } else if (key.equals("last_progress_date")) {
                    return new TemplateString(getLastProgressDate());
                } else {
                    if (key.equals("no_progress_time")) {
                        return new TemplateString(getNoProgressTime());
                    }
                    return AccessTemplateMethodWrapper.getMethod(this, key);
                }
            }
        } catch (Exception e) {
            getLog().error("Job " + getFullMark() + ".get(\"" + key + "\") error", e);
            return null;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    protected void serialize() throws Exception {
        serialize();
    }

    public static void sendBillingException(Exception ex) {
        try {
            Session.getLog().debug("Accounting error is HSUserException:" + (ex instanceof HSUserException), ex);
            if (!(ex instanceof HSUserException)) {
                Ticket.create(ex, Session.getAccount(), "Accounting Error");
                return;
            }
            String errorMessage = ex.getMessage();
            if (ex instanceof PrivateKeyNotLoadedException) {
                if (CCEncryption.get().isKeyLoadEmailSent()) {
                    return;
                }
                errorMessage = Localizer.translateMessage("admin.CCEncryption.privateKeyNotLoadedAdminError");
            }
            String limitMessage = Localizer.translateMessage("bill.over");
            boolean limitExceeded = (limitMessage == null || errorMessage == null || !errorMessage.equals(limitMessage)) ? false : true;
            StringWriter st = new StringWriter();
            PrintWriter out2 = new PrintWriter(st);
            ex.printStackTrace(out2);
            out2.flush();
            SimpleHash root = CustomEmailMessage.getDefaultRoot(Session.getAccount());
            root.put("error", limitExceeded ? Localizer.translateMessage("bill.over.admin", new Object[]{Long.toString(Session.getAccount().getId().getId()), Session.getUser().getLogin()}) : errorMessage);
            root.put("date", TimeUtils.getDate().toString());
            if (!limitExceeded) {
                root.put("stack", st.toString());
            }
            root.put("error_subject", limitExceeded ? Localizer.translateMessage("bill.over.admin_title") : errorMessage);
            CustomEmailMessage.send("ACCOUNTING_ERROR", root);
            if (ex instanceof PrivateKeyNotLoadedException) {
                CCEncryption.get().justSentKeyLoadEmail();
            }
        } catch (Throwable se) {
            Session.getLog().warn("Error sending message", se);
        }
    }
}
