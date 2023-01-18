package psoft.hsphere.cron;

import psoft.hsphere.C0004CP;
import psoft.hsphere.async.AsyncManager;
import psoft.hsphere.background.BackgroundJob;

/* loaded from: hsphere.zip:psoft/hsphere/cron/AsyncCron.class */
public class AsyncCron extends BackgroundJob {
    public AsyncCron(C0004CP cp) throws Exception {
        super(cp, "ASYNC_MAN");
    }

    public AsyncCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        AsyncManager.getManager().check();
    }
}
