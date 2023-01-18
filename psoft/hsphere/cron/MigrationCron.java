package psoft.hsphere.cron;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import psoft.hsphere.C0004CP;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.migrator.wizards.MigrationJob;

/* loaded from: hsphere.zip:psoft/hsphere/cron/MigrationCron.class */
public class MigrationCron extends BackgroundJob {
    private static final LinkedList JOBS = new LinkedList();
    private static final Map allJobs = new HashMap();
    static MigrationJob job;

    public MigrationCron(C0004CP cp) throws Exception {
        super(cp);
    }

    public MigrationCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        while (JOBS.size() > 0) {
            job = (MigrationJob) JOBS.getFirst();
            JOBS.removeFirst();
            job.execute();
        }
    }

    public static MigrationJob getJob(String jobId) {
        return (MigrationJob) allJobs.get(jobId);
    }

    public static synchronized void addJob(MigrationJob mj) {
        allJobs.put(mj.getJobId(), mj);
        JOBS.add(mj);
    }

    public static int size() {
        return JOBS.size();
    }

    public static int getStep() {
        return job.getStep();
    }

    public static String getJobState() {
        return job.getState();
    }

    public static String getJobId() {
        return job.getJobId();
    }

    public static Map getJobs() {
        return allJobs;
    }
}
