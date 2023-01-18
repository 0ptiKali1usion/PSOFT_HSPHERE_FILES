package psoft.hsphere.cron;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelRoot;
import java.util.HashMap;
import javax.mail.Address;
import psoft.hsphere.AccountPreferences;
import psoft.hsphere.C0004CP;
import psoft.hsphere.HSLingualScalar;
import psoft.hsphere.Session;
import psoft.hsphere.background.BackgroundJob;
import psoft.hsphere.resource.admin.CustomEmailMessage;
import psoft.hsphere.resource.admin.MailMan;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/cron/FailedSignupsCron.class */
public class FailedSignupsCron extends BackgroundJob {
    protected static final long TIME_DIFF = 600000;
    protected HashMap mails;

    public FailedSignupsCron(C0004CP cp) throws Exception {
        super(cp, "FAILED_SIGNUPS");
    }

    public FailedSignupsCron(C0004CP cp, String dbMark) throws Exception {
        super(cp, dbMark);
    }

    /* JADX WARN: Can't wrap try/catch for region: R(11:10|(2:12|(3:14|15|(1:17))(1:18))(1:35)|19|20|(1:22)|23|24|(1:26)(3:28|(1:30)|31)|27|15|(0)) */
    /* JADX WARN: Code restructure failed: missing block: B:61:0x0087, code lost:
        psoft.hsphere.Session.getLog().debug("CRON " + getDBMark() + " HAS BEEN INTERRUPTED");
     */
    /* JADX WARN: Code restructure failed: missing block: B:74:0x013f, code lost:
        r18 = false;
     */
    /* JADX WARN: Removed duplicated region for block: B:101:0x01d7 A[EDGE_INSN: B:101:0x01d7->B:85:0x01d7 ?: BREAK  , SYNTHETIC] */
    @Override // psoft.hsphere.background.BackgroundJob
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct code enable 'Show inconsistent code' option in preferences
    */
    protected void processJob() throws java.lang.Exception {
        /*
            Method dump skipped, instructions count: 644
            To view this dump change 'Code comments level' option to 'DEBUG'
        */
        throw new UnsupportedOperationException("Method not decompiled: psoft.hsphere.cron.FailedSignupsCron.processJob():void");
    }

    protected void incFailedSignups(long resellerId, String uname, String pname, String dName) {
        TemplateModel templateHash = new TemplateHash();
        templateHash.put("uname", uname);
        templateHash.put("pname", pname);
        templateHash.put("dname", dName);
        if (this.mails.containsKey(new Long(resellerId))) {
            TemplateList l = (TemplateList) this.mails.get(new Long(resellerId));
            l.add(templateHash);
            this.mails.put(new Long(resellerId), l);
            return;
        }
        TemplateList l2 = new TemplateList();
        l2.add(templateHash);
        this.mails.put(new Long(resellerId), l2);
    }

    protected void sendMail() {
        Session.getLog().debug("FSIGNUPS INSIDE MAIL SETTINGS mails.size()=" + this.mails.size());
        for (Long resId : this.mails.keySet()) {
            TemplateList l = (TemplateList) this.mails.get(resId);
            SimpleHash root = new SimpleHash();
            root.put("failed_signups_q", "" + l.size());
            root.put("accounts", l);
            root.put(AccountPreferences.LANGUAGE, new HSLingualScalar());
            try {
                Session.setResellerId(resId.longValue());
                Address[] emails = MailMan.getMailMan().getEmails(1);
                Session.getLog().debug("FSIGNUPS FOUND " + emails.length + " ADDRESSES TO SEND MAIL FOR RESELLER " + resId.longValue());
                for (int j = 0; j < emails.length; j++) {
                    Session.getLog().debug("FSIGNUPS SENDING MAIL TO " + emails[j].toString());
                    CustomEmailMessage.send("FAILED_SIGNUP", emails[j].toString(), (TemplateModelRoot) root);
                }
            } catch (Throwable se) {
                Session.getLog().warn("Error sending message", se);
            }
        }
    }
}
