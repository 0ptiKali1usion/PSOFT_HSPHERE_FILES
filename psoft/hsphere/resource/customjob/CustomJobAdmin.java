package psoft.hsphere.resource.customjob;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.hsphere.resource.p004tt.TroubleTicketAdmin;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/customjob/CustomJobAdmin.class */
public class CustomJobAdmin implements TemplateHashModel {
    TroubleTicketAdmin ttadmin;

    public CustomJobAdmin(TroubleTicketAdmin ttadmin) {
        this.ttadmin = ttadmin;
    }

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateModel FM_createJob(long accountId, String subject, String description) throws Exception {
        return new TemplateString(CustomJob.createJob(accountId, subject, description, getEnteredBy()));
    }

    public TemplateModel FM_getJob(int id) throws Exception {
        return CustomJob.get(id);
    }

    public TemplateModel FM_getJobs(long accountId) throws Exception {
        TemplateList jobs = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM custom_jobs WHERE account_id = ?");
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long id = rs.getInt(1);
                jobs.add((TemplateModel) CustomJob.get(id));
            }
            Session.closeStatement(ps);
            con.close();
            return jobs;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_setStatus(int id, int status) throws Exception {
        CustomJob cj = CustomJob.get(id);
        if (status == 2) {
            cj.done();
        } else {
            cj.setStatus(status);
        }
        return cj;
    }

    public TemplateModel FM_getEnteredBy() throws Exception {
        return new TemplateString(getEnteredBy());
    }

    protected String getEnteredBy() {
        String enteredBy = this.ttadmin.getName();
        if (enteredBy == null || enteredBy.length() == 0) {
            enteredBy = Long.toString(this.ttadmin.getId().getId());
        }
        return enteredBy;
    }
}
