package psoft.hsphere.cron;

import org.apache.log4j.Category;
import psoft.hsphere.C0004CP;
import psoft.hsphere.background.BackgroundJob;

/* loaded from: hsphere.zip:psoft/hsphere/cron/ExternalChargeCron.class */
public class ExternalChargeCron extends BackgroundJob {
    private transient Category log;

    @Override // psoft.hsphere.background.BackgroundJob
    public Category getLog() {
        return this.log;
    }

    public ExternalChargeCron(C0004CP cp) throws Exception {
        super(cp, "EX_CHARGE_CRON");
        this.log = Category.getInstance(ExternalChargeCron.class.getName());
    }

    public ExternalChargeCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
        this.log = Category.getInstance(ExternalChargeCron.class.getName());
    }

    /* JADX WARN: Code restructure failed: missing block: B:41:0x0052, code lost:
        psoft.hsphere.Session.getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
     */
    @Override // psoft.hsphere.background.BackgroundJob
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void processJob() throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 352
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.cron.ExternalChargeCron.processJob():void");
    }
}
