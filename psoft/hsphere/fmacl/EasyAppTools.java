package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/EasyAppTools.class */
public class EasyAppTools implements TemplateHashModel {
    public static final TemplateString STATUS_OK = new TemplateString("OK");

    public TemplateModel get(String key) throws TemplateModelException {
        if ("status".equals(key)) {
            return STATUS_OK;
        }
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public int FM_getAppEnabled(int appType) throws Exception {
        int enabled = 0;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT enabled FROM easy_app_list WHERE id = ?");
            ps.setLong(1, appType);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                enabled = rs.getInt(1);
            }
            Session.closeStatement(ps);
            con.close();
            return enabled;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_disableAll() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE easy_app_list set enabled = 0");
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_enable(long aid) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE easy_app_list set enabled = 1 where id = ?");
            ps.setLong(1, aid);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
