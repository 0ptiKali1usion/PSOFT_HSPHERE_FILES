package psoft.hsphere.cron;

import java.text.DateFormat;
import psoft.hsphere.C0004CP;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.admin.MailSRS;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/cron/MailSRSCron.class */
public class MailSRSCron extends BackgroundJob {

    /* renamed from: df */
    protected static DateFormat f84df = DateFormat.getDateInstance(3);

    public MailSRSCron(C0004CP cp) throws Exception {
        super(cp, "MAIL_SRS_CRON");
    }

    public MailSRSCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    @Override // psoft.hsphere.background.BackgroundJob
    protected void processJob() throws Exception {
        checkSuspended();
        getLog().info("Starting MAIL_SRS_CRON");
        Session.setUser(User.getUser(FMACLManager.ADMIN));
        Session.setAccount(Session.getUser().getAccount(new ResourceId(1L, 0)));
        MailSRS msrs = new MailSRS();
        msrs.regenerateSRSKey();
        getLog().info("MAIL_SRS_CRON FINISHED");
    }
}
