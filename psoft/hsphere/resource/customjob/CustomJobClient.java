package psoft.hsphere.resource.customjob;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/resource/customjob/CustomJobClient.class */
public class CustomJobClient implements TemplateHashModel {
    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public TemplateModel FM_getJob(int id) throws Exception {
        return CustomJob.get(id);
    }

    public TemplateModel FM_getJobs() throws Exception {
        long accountId = getAccountId();
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

    public TemplateModel FM_addNote(int id, String note, int visibility) throws Exception {
        CustomJob cj = CustomJob.get(id);
        cj.FM_addNote("@", note, visibility);
        return this;
    }

    protected long getAccountId() {
        return Session.getAccountId();
    }
}
