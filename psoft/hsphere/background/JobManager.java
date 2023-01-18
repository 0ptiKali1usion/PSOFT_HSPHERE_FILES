package psoft.hsphere.background;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.cron.CronManager;
import psoft.hsphere.exception.JobDataBrokenException;
import psoft.hsphere.global.Globals;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.util.XMLManager;
import psoft.util.TimeUtils;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/background/JobManager.class */
public class JobManager extends Thread implements TemplateHashModel {
    protected long lastLaunch;
    public static final String managerDbMark = "JOB_MANAGER";
    protected Hashtable jobRoster = new Hashtable();
    protected Hashtable jobGroups = new Hashtable();
    protected Hashtable aliveJobs = new Hashtable();
    protected static final long serviceablePeriod = 3600000;

    /* renamed from: cp */
    protected static C0004CP f73cp = null;
    protected static long sleepingTime = 10000;
    private static final Category log = Category.getInstance(JobManager.class.getName());

    /* loaded from: hsphere.zip:psoft/hsphere/background/JobManager$JobGroup.class */
    public class JobGroup {
        String name;
        ThreadGroup threadGroup;
        int defPriority;
        boolean disabled = false;
        int maxAliveJobCount = 0;
        Hashtable jobRecords = new Hashtable();
        ArrayList jobQueue = new ArrayList();

        JobGroup(String name, int maxPriority, int defPriority) {
            JobManager.this = r6;
            this.threadGroup = new ThreadGroup(name);
            this.threadGroup.setMaxPriority(maxPriority);
            this.name = name;
            this.defPriority = defPriority;
        }

        JobRecord getRecord(String jobName) throws Exception {
            Object jr = this.jobRecords.get(jobName);
            if (jr == null) {
                throw new Exception("Job " + jobName + " has not been found");
            }
            return (JobRecord) jr;
        }

        void addRecord(JobRecord jr) {
            if (this.jobRecords.get(jr.name) == null) {
                jr.group = this;
                this.jobRecords.put(jr.name, jr);
            }
        }

        int addToQueue(JobRecord jr) {
            this.jobQueue.add(jr);
            return this.jobQueue.size() - 1;
        }

        void removeFromQueue(int index) {
            this.jobQueue.remove(index);
        }

        int queueLength() {
            return this.jobQueue.size();
        }

        boolean hasQueued() {
            return this.jobQueue.size() != 0;
        }

        JobRecord extractFirstQueued() {
            JobRecord res = null;
            if (hasQueued()) {
                res = (JobRecord) this.jobQueue.get(0);
                removeFromQueue(0);
            }
            return res;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/background/JobManager$JobRecord.class */
    public class JobRecord {
        String name;
        String dbMark;
        String requires;
        JobGroup group;
        int priority;
        Class jobClass = null;
        boolean disabled = false;
        boolean persistent = false;
        long period = 0;
        int threadCount = 0;
        String startAt = "";
        long startTime = 0;
        int maxInstanceCount = 1;
        int loopDelay = 0;
        long launchCount = 0;

        JobRecord(String name, String dbMark, JobGroup group, int priority) {
            JobManager.this = r5;
            this.name = name;
            this.dbMark = dbMark;
            this.group = group;
            this.priority = priority;
        }
    }

    public JobManager(C0004CP cp) throws Exception {
        this.lastLaunch = 0L;
        f73cp = cp;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT value FROM last_start WHERE name = ?");
                ps.setString(1, managerDbMark);
                ResultSet rs = ps.executeQuery();
                if (rs.next() && rs.getTimestamp(1) != null) {
                    this.lastLaunch = rs.getTimestamp(1).getTime();
                }
                parseConfig();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                log.error("Error getting JOB_LAST_START", e);
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void registerJob(String jobName, String jobClassName, JobRecord jobRec) throws Exception {
        if (this.jobRoster.get(jobName) == null) {
            Class jobClass = Class.forName(jobClassName);
            boolean found = false;
            JobRecord jr = null;
            Iterator i = this.jobRoster.values().iterator();
            while (true) {
                if (!i.hasNext()) {
                    break;
                }
                jr = (JobRecord) i.next();
                if (jr.jobClass.getName().equals(jobClassName)) {
                    found = true;
                    break;
                }
            }
            if (found) {
                jobRec.jobClass = jr.jobClass;
                this.jobRoster.put(jobName, jobRec);
                throw new Exception("Class " + jobClassName + " already registered");
            }
            Class tmpClass = jobClass;
            boolean impl = false;
            do {
                Class[] intf = tmpClass.getInterfaces();
                int i2 = 0;
                while (true) {
                    if (i2 >= intf.length) {
                        break;
                    } else if (intf[i2] != Backgroundable.class) {
                        i2++;
                    } else {
                        impl = true;
                        break;
                    }
                }
                tmpClass = tmpClass.getSuperclass();
            } while (tmpClass != Object.class);
            if (!impl) {
                throw new Exception("Class " + jobClassName + " does not implements Backgroundable");
            }
            jobRec.jobClass = jobClass;
            log.debug("NEW JOB CLASS REGISTERED: " + jobClass.getName() + "  <" + jobName + ">");
            this.jobRoster.put(jobName, jobRec);
            return;
        }
        throw new Exception("Job name " + jobName + " already registered");
    }

    private JobRecord getJobRecord(String jobName) throws Exception {
        JobRecord jr = (JobRecord) this.jobRoster.get(jobName);
        if (jr == null) {
            throw new Exception("Unregistered job");
        }
        return jr;
    }

    protected Backgroundable createJob(String jobName, boolean canQueueUp) throws Exception {
        return createJob(jobName, canQueueUp, null);
    }

    protected Backgroundable createJob(String jobName, Long storedJobId) throws Exception {
        return createJob(jobName, true, storedJobId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    private Backgroundable createJob(String jobName, boolean canQueueUp, Long storedJobId) throws Exception {
        Backgroundable res;
        JobRecord jr;
        Class[] argt;
        Object[] argv;
        log.debug("Trying to initialize the background job '" + jobName + "' (id #" + storedJobId + ")");
        try {
            jr = getJobRecord(jobName);
        } catch (Throwable te) {
            log.error("Job '" + jobName + "' (id #" + storedJobId + ") failed to start.", te);
            res = null;
        }
        if (jr.group.disabled || jr.disabled) {
            throw new Exception("Cannot start disabled job");
        }
        if (jr.requires != null && !areGlobalsAvailable(jr.requires)) {
            throw new Exception("The job won't be started due to limitations in the 'Globals' settings.");
        }
        if (canQueueUp && jr.group.maxAliveJobCount > 0) {
            int k = alivePerGroupCount(jr.group);
            if (k >= jr.maxInstanceCount) {
                return null;
            }
            if (k >= jr.group.maxAliveJobCount || k >= jr.maxInstanceCount) {
                jr.group.addToQueue(jr);
                return null;
            }
        }
        if (storedJobId != null) {
            Class jClass = jr.jobClass;
            Class[] argt2 = {storedJobId.getClass(), f73cp.getClass()};
            Method method = jClass.getMethod("getStoredInstance", argt2);
            Object[] argv2 = {storedJobId, f73cp};
            try {
                res = (Backgroundable) method.invoke(null, argv2);
            } catch (InvocationTargetException ite) {
                Throwable ex = ite.getTargetException();
                if (ex instanceof JobDataBrokenException) {
                    BackgroundJob.deleteStatusData(storedJobId.longValue());
                    log.info("Detected broken data of stored job #" + storedJobId + " have been deleted.");
                }
                throw ex;
            }
        } else {
            String jobDbMark = jr.dbMark;
            if (jobDbMark != null) {
                argt = new Class[]{f73cp.getClass(), jobDbMark.getClass()};
                argv = new Object[]{f73cp, jobDbMark};
            } else {
                argt = new Class[]{f73cp.getClass()};
                argv = new Object[]{f73cp};
            }
            Constructor c = jr.jobClass.getConstructor(argt);
            res = (Backgroundable) c.newInstance(argv);
        }
        if (jr.period > 0 || jr.threadCount > 0) {
            Hashtable params = new Hashtable();
            if (jr.period > 0) {
                params.put("period", new Long(jr.period));
            }
            if (jr.threadCount > 0) {
                params.put("threadcount", new Integer(jr.threadCount));
            }
            if (jr.persistent) {
                params.put("persistent", new Boolean(jr.persistent));
            }
            if (jr.loopDelay > 0) {
                params.put("loopdelay", new Integer(jr.loopDelay));
            }
            if (params.size() > 0) {
                res.setParams(params);
            }
        }
        res.setName(jobName);
        res.setGroupId(jr.group.name);
        res.setPriority(jr.priority);
        synchronized (this.aliveJobs) {
            this.aliveJobs.put(new Long(res.getId()), res);
        }
        log.debug("Job '" + jobName + "' initialized successfully.");
        res.startJob();
        jr.launchCount++;
        return res;
    }

    protected int aliveInstanceCount(JobRecord jr) {
        int res = 0;
        synchronized (this.aliveJobs) {
            for (Backgroundable job : this.aliveJobs.values()) {
                if (job != null && job.isAliveJob() && job.getName().equals(jr.name)) {
                    res++;
                }
            }
        }
        return res;
    }

    protected int alivePerGroupCount(JobGroup group) {
        int res = 0;
        synchronized (this.aliveJobs) {
            for (Backgroundable job : this.aliveJobs.values()) {
                if (job != null && job.isAliveJob() && job.getGroupId().equals(group.name)) {
                    res++;
                }
            }
        }
        return res;
    }

    protected void packAliveJobList() {
        synchronized (this.aliveJobs) {
            ArrayList deadJobs = new ArrayList();
            for (Long id : this.aliveJobs.keySet()) {
                Thread job = (Thread) this.aliveJobs.get(id);
                if (job == null || !job.isAlive()) {
                    deadJobs.add(id);
                }
            }
            for (int i = 0; i < deadJobs.size(); i++) {
                this.aliveJobs.remove(deadJobs.get(i));
            }
        }
    }

    protected Backgroundable getJobById(long jobId) throws Exception {
        Backgroundable backgroundable;
        synchronized (this.aliveJobs) {
            Object res = this.aliveJobs.get(new Long(jobId));
            if (res != null) {
                log.debug("JOB HAS BEEN FOUND (id = " + jobId + ")");
                backgroundable = (Backgroundable) res;
            } else {
                throw new Exception("Job not found by id: " + jobId);
            }
        }
        return backgroundable;
    }

    protected boolean runIncompletedStoredJob(JobRecord jr) throws Exception {
        boolean incomletedJobRun = false;
        if (jr != null && jr.name != null) {
            String name = jr.name;
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                try {
                    ps = con.prepareStatement("SELECT id FROM bg_job_data WHERE name = ? GROUP BY id ORDER BY id DESC");
                    ps.setString(1, name);
                    ResultSet rs = ps.executeQuery();
                    while (rs.next()) {
                        Long id = new Long(rs.getLong("id"));
                        if (this.aliveJobs.get(id) == null) {
                            if (createJob(name, id) != null) {
                                log.debug("Stored (incomple) job '" + name + "' with id #" + id + " has been successfully started.");
                                incomletedJobRun = true;
                            }
                        } else {
                            log.error("Unable to run the stored job '" + name + "' (id #" + id + ") because it has been already started.");
                        }
                    }
                    Session.closeStatement(ps);
                    con.close();
                } catch (Exception ex) {
                    log.error(ex);
                    Session.closeStatement(ps);
                    con.close();
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return incomletedJobRun;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        JobRecord jr;
        setName(managerDbMark);
        try {
            Session.setResellerId(1L);
        } catch (UnknownResellerException e) {
        }
        boolean firstPass = true;
        while (!isInterrupted()) {
            try {
                packAliveJobList();
                for (JobGroup group : this.jobGroups.values()) {
                    if (!group.disabled && group.hasQueued()) {
                        int m = group.queueLength();
                        if (group.maxAliveJobCount > 0) {
                            int k = alivePerGroupCount(group);
                            int k2 = k >= group.maxAliveJobCount ? 0 : group.maxAliveJobCount - k;
                            if (m > k2) {
                                m = k2;
                            }
                        }
                        for (int j = 0; j < m; j++) {
                            try {
                                createJob(group.extractFirstQueued().name, false);
                            } catch (Exception ex) {
                                log.error("Job '" + jr.name + "': ", ex);
                            }
                        }
                    }
                }
                for (JobRecord jr2 : this.jobRoster.values()) {
                    try {
                        if (!jr2.disabled && !jr2.group.disabled && jr2.startTime > 0 && (jr2.requires == null || areGlobalsAvailable(jr2.requires))) {
                            long curTime = TimeUtils.currentTimeMillis();
                            long startTime = jr2.startTime;
                            long period = jr2.period;
                            boolean started = false;
                            if (firstPass) {
                                started = runIncompletedStoredJob(jr2);
                                if (!started && jr2.dbMark != null && aliveInstanceCount(jr2) == 0) {
                                    long lastStart = BackgroundJob.getLastStart(jr2.dbMark);
                                    if (period > 0 && lastStart + period < curTime) {
                                        createJob(jr2.name, true);
                                        started = true;
                                    }
                                }
                            }
                            if (!started && startTime <= curTime) {
                                if (period > 0) {
                                    if (aliveInstanceCount(jr2) < jr2.maxInstanceCount) {
                                        jr2.startTime = curTime + period;
                                        createJob(jr2.name, true);
                                    }
                                } else if (curTime - jr2.startTime < serviceablePeriod) {
                                    createJob(jr2.name, true);
                                    jr2.startTime = 0L;
                                }
                            }
                        }
                    } catch (Exception ex2) {
                        log.debug("Job '" + jr2.name + "': ", ex2);
                    }
                }
                firstPass = false;
                TimeUtils.sleep(sleepingTime);
            } catch (Exception e2) {
                if (!(e2 instanceof InterruptedException)) {
                    log.error("JobManager error", e2);
                } else {
                    synchronized (this.aliveJobs) {
                        for (Backgroundable job : this.aliveJobs.keySet()) {
                            if (job.isAliveJob()) {
                                try {
                                    job.interruptJob();
                                } catch (Exception ex3) {
                                    log.warn(ex3);
                                }
                            }
                        }
                    }
                }
            }
        }
        log.debug("JobManager has been interrupted");
        log.debug("JobManager has been stopped");
    }

    protected Element getConfigXML() throws Exception {
        Element root;
        try {
            Document init_doc = XMLManager.getXML("CRON_CONFIG");
            root = init_doc.getDocumentElement();
        } catch (Exception e) {
            log.error("CANNOT READ JOB MANAGER CONFIG ! ALL JOBS ARE DISABLED !", e);
            root = null;
        }
        return root;
    }

    protected void parseConfig() {
        try {
            Element root = getConfigXML();
            NodeList groups = root.getElementsByTagName("group");
            for (int i = 0; i < groups.getLength(); i++) {
                String gname = ((Element) groups.item(i)).getAttribute("name");
                if (this.jobGroups.get(gname) != null) {
                    throw new Exception("Group '" + gname + "' has been already registered.");
                }
                Element groupEl = (Element) groups.item(i);
                String groupName = groupEl.getAttribute("name");
                int maxPriority = 10;
                int defPriority = Thread.currentThread().getPriority();
                if (groupEl.hasAttribute("maxpriority")) {
                    maxPriority = Integer.parseInt(groupEl.getAttribute("maxpriority"));
                }
                if (groupEl.hasAttribute("defpriority")) {
                    defPriority = Integer.parseInt(groupEl.getAttribute("defpriority"));
                }
                JobGroup jobGroup = new JobGroup(groupName, maxPriority, defPriority);
                if (groupEl.hasAttribute("disabled") && Integer.parseInt(groupEl.getAttribute("disabled")) == 1) {
                    jobGroup.disabled = true;
                }
                if (groupEl.hasAttribute("maxjobcount")) {
                    jobGroup.maxAliveJobCount = Integer.parseInt(groupEl.getAttribute("maxjobcount"));
                }
                this.jobGroups.put(groupName, jobGroup);
                NodeList jobs = groupEl.getElementsByTagName("job");
                for (int j = 0; j < jobs.getLength(); j++) {
                    Element jobEl = (Element) jobs.item(j);
                    long period = 0;
                    int jobPriority = jobGroup.defPriority;
                    String jobName = jobEl.getAttribute("name");
                    String jobClassName = jobEl.getAttribute("class");
                    String jobDbMark = jobEl.getAttribute("db_mark");
                    if (jobEl.hasAttribute("period")) {
                        period = Long.parseLong(jobEl.getAttribute("period"));
                    }
                    if (jobEl.hasAttribute("priority")) {
                        jobPriority = Integer.parseInt(jobEl.getAttribute("priority"));
                    }
                    JobRecord jr = new JobRecord(jobName, jobDbMark, jobGroup, jobPriority);
                    jr.period = period * 60 * 1000;
                    if (jobEl.hasAttribute("disabled") && Integer.parseInt(jobEl.getAttribute("disabled")) == 1) {
                        jr.disabled = true;
                    }
                    if (jobEl.hasAttribute("starttime")) {
                        jr.startAt = jobEl.getAttribute("starttime");
                        parseStartTime(jr);
                    }
                    if (jobEl.hasAttribute("threadcount")) {
                        jr.threadCount = Integer.parseInt(jobEl.getAttribute("threadcount"));
                    }
                    if (jobEl.hasAttribute("maxinstancecount")) {
                        jr.maxInstanceCount = Integer.parseInt(jobEl.getAttribute("maxinstancecount"));
                    }
                    if (jobEl.hasAttribute("persistent") && Integer.parseInt(jobEl.getAttribute("persistent")) == 1) {
                        jr.persistent = true;
                    }
                    if (jobEl.hasAttribute("loopdelay")) {
                        jr.loopDelay = Integer.parseInt(jobEl.getAttribute("loopdelay"));
                    }
                    String requires = jobEl.getAttribute("requires");
                    if ("".equals(requires)) {
                        jr.requires = null;
                    } else {
                        jr.requires = requires;
                    }
                    registerJob(jobName, jobClassName, jr);
                    jobGroup.addRecord(jr);
                }
            }
        } catch (Exception e) {
            log.error("Unable to read job config", e);
            this.jobGroups.clear();
            this.jobRoster.clear();
        }
    }

    protected void parseStartTime(JobRecord jr) throws Exception {
        String str;
        String subject = jr.startAt;
        if (subject.startsWith("now")) {
            jr.startTime = 0L;
            if (subject.startsWith("now+")) {
                String subject2 = subject.substring(4);
                int mode = 0;
                String hm = "";
                for (int i = 0; i < subject2.length(); i++) {
                    char c = subject2.charAt(i);
                    if (c >= '0' && c <= '9') {
                        if (hm.length() == 2 || mode > 1) {
                            throw new Exception("Invalid number format");
                        }
                        str = hm + c;
                    } else if ((mode == 0 && c != 'h' && c != 'm') || (mode == 1 && c != 'm')) {
                        throw new Exception("Invalid time format");
                    } else {
                        if (c == 'h') {
                            jr.startTime = 3600000 * Integer.parseInt(hm);
                        } else {
                            if (mode == 0) {
                                mode++;
                            }
                            jr.startTime += CronManager.MULTIPLIER * Integer.parseInt(hm);
                        }
                        mode++;
                        str = "";
                    }
                    hm = str;
                }
            }
            jr.startTime += TimeUtils.currentTimeMillis();
            return;
        }
        SimpleDateFormat df = new SimpleDateFormat("HH:mm");
        long parsedTime = df.parse(subject).getTime() - df.parse("00:00").getTime();
        long curTime = TimeUtils.currentTimeMillis();
        Calendar cn = TimeUtils.getCalendar();
        long parsedTime2 = (parsedTime + curTime) - (HostEntry.VPS_IP * (((cn.get(11) * 3600) + (cn.get(12) * 60)) + cn.get(13)));
        if (parsedTime2 < curTime) {
            parsedTime2 += 86400000;
        }
        jr.startTime = parsedTime2;
    }

    public void setJobStartStatus(String jobName, boolean enable) throws Exception {
        JobRecord jr = getJobRecord(jobName);
        jr.disabled = !enable;
    }

    public void restartJob(long jobId) throws Exception {
        Backgroundable job = getJobById(jobId);
        log.debug("******* RESTART ******** " + job.getId());
        String jobName = job.getName();
        if (job.isRunning()) {
            job.interruptJob();
        }
        long startTime = TimeUtils.currentTimeMillis();
        boolean ended = false;
        while (true) {
            if (job.isAliveJob()) {
                log.debug("**** RESTARTING: THREAD IS STILL ALIVE ****** " + job.getId());
                TimeUtils.sleep(100L);
                if (TimeUtils.currentTimeMillis() - startTime > 10000) {
                    log.debug("**** RESTARTING: CANNOT WAIT LONGER ****** " + job.getId());
                    break;
                }
            } else {
                ended = true;
                break;
            }
        }
        if (ended) {
            createJob(jobName, true);
        }
    }

    public TemplateModel get(String key) {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel FM_getJobById(long jobId) throws Exception {
        return getJobById(jobId);
    }

    public TemplateModel FM_getAliveJobList() {
        packAliveJobList();
        synchronized (this.aliveJobs) {
            if (this.aliveJobs.size() > 0) {
                return new ListAdapter(this.aliveJobs.values());
            }
            return null;
        }
    }

    public void FM_startJob(String jobName) throws Exception {
        log.debug("******* START JOB ******** " + jobName);
        createJob(jobName, true);
    }

    public void FM_disableJob(String jobName) throws Exception {
        log.debug("******* DISABLE JOB ******** " + jobName);
        setJobStartStatus(jobName, false);
    }

    public void FM_enableJob(String jobName) throws Exception {
        log.debug("******* ENABLE JOB ******** " + jobName);
        setJobStartStatus(jobName, true);
    }

    public void FM_suspendJob(long jobId) throws Exception {
        Backgroundable job = getJobById(jobId);
        if (job.isRunning() && !job.isSuspended()) {
            log.debug("******* SUSPEND ******** " + job.getId());
            job.suspendJob();
            return;
        }
        log.debug("**** CANNOT BE SUSPENDED ***** " + job.getId());
    }

    public void FM_resumeJob(long jobId) throws Exception {
        Backgroundable job = getJobById(jobId);
        if (job.isSuspended()) {
            log.debug("******* RESUME ******** " + job.getId());
            job.resumeJob();
            return;
        }
        log.debug("**** CANNOT BE RESUMED ***** " + job.getId());
    }

    public void FM_restartJob(long jobId) throws Exception {
        restartJob(jobId);
    }

    public void FM_interruptJob(long jobId) throws Exception {
        Backgroundable job = getJobById(jobId);
        log.debug("******* INTERRUPT ******** " + job.getId());
        if (job.isRunning() && !job.isSuspended()) {
            job.interruptJob();
        }
    }

    public TemplateModel FM_getGroupNames() {
        return new TemplateList(this.jobGroups.keySet());
    }

    public TemplateModel FM_getJobNamesOfGroup(String groupName) {
        JobGroup jg = (JobGroup) this.jobGroups.get(groupName);
        if (jg != null) {
            return new TemplateList(jg.jobRecords.keySet());
        }
        log.error("Job group not found: " + groupName);
        return null;
    }

    public TemplateString FM_getJobStartParam(String jobName, String paramName) throws Exception {
        JobRecord jr = getJobRecord(jobName);
        if (jr != null) {
            if (paramName.equals("priority")) {
                return new TemplateString(jr.priority);
            }
            if (paramName.equals("status")) {
                if (jr.disabled) {
                    return new TemplateString("disabled");
                }
                if (jr.requires != null && !areGlobalsAvailable(jr.requires)) {
                    return new TemplateString("disabled_globally");
                }
                return new TemplateString("enabled");
            } else if (paramName.equals("period_min")) {
                if (jr.period == 0) {
                    return null;
                }
                return new TemplateString(jr.period / 60000);
            } else if (paramName.equals("start_at")) {
                return new TemplateString(jr.startAt);
            } else {
                if (paramName.equals("group_id")) {
                    return new TemplateString(jr.group.name);
                }
                if (paramName.equals("beforestart_min")) {
                    return new TemplateString((jr.startTime - TimeUtils.currentTimeMillis()) / 60000);
                }
                if (paramName.equals("launch_count")) {
                    return new TemplateString(jr.launchCount);
                }
                log.error("Job parameter not found: " + paramName);
                return null;
            }
        }
        Session.getLog().error("Job record not found: " + jobName);
        return null;
    }

    public boolean isJobDisabled(String jobName) throws Exception {
        JobRecord jr = getJobRecord(jobName);
        return jr.disabled;
    }

    public int getAliveJobCount(String name) {
        int count = 0;
        packAliveJobList();
        synchronized (this.aliveJobs) {
            if (this.aliveJobs.size() > 0) {
                for (Backgroundable backgroundable : this.aliveJobs.values()) {
                    if ("vpsReconfig".equals(backgroundable.getName())) {
                        count++;
                    }
                }
            }
        }
        return count;
    }

    protected boolean areGlobalsAvailable(String pattern) throws Exception {
        boolean res = false;
        StringTokenizer orGroups = new StringTokenizer(pattern, ";");
        while (orGroups.hasMoreTokens() && !res) {
            StringTokenizer andElements = new StringTokenizer(orGroups.nextToken(), ",");
            while (andElements.hasMoreTokens()) {
                res = isGlobalAvailable(andElements.nextToken().trim());
                if (!res) {
                    break;
                }
            }
        }
        return res;
    }

    protected boolean isGlobalAvailable(String name) throws Exception {
        return Globals.getAccessor().isGlobalObject(name) && Globals.isObjectDisabled(name) == 0;
    }
}
