package psoft.hsphere.cron;

import psoft.hsphere.C0004CP;
import psoft.hsphere.background.BackgroundJob;

/* loaded from: hsphere.zip:psoft/hsphere/cron/TTAutoCloseCron.class */
public class TTAutoCloseCron extends BackgroundJob {
    private static final int DEFAULT_AUTOCLOSE = 30;
    private static final int NO_AUTOCLOSE = -1;

    public TTAutoCloseCron(C0004CP cp) throws Exception {
        super(cp, "TTAutoCloseCron");
    }

    public TTAutoCloseCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    /* JADX WARN: Removed duplicated region for block: B:103:0x01a3 A[EDGE_INSN: B:103:0x01a3->B:87:0x01a3 ?: BREAK  , SYNTHETIC] */
    @Override // psoft.hsphere.background.BackgroundJob
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void processJob() throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 531
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.cron.TTAutoCloseCron.processJob():void");
    }
}
