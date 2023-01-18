package psoft.hsphere.cron;

import psoft.hsphere.C0004CP;
import psoft.hsphere.background.BackgroundJob;

/* loaded from: hsphere.zip:psoft/hsphere/cron/VPSCron.class */
public class VPSCron extends BackgroundJob {
    public VPSCron(C0004CP cp) throws Exception {
        super(cp, "VPS_CRON");
    }

    public VPSCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    /* JADX WARN: Code restructure failed: missing block: B:54:0x006d, code lost:
        getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
     */
    @Override // psoft.hsphere.background.BackgroundJob
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void processJob() throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 615
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.cron.VPSCron.processJob():void");
    }
}
