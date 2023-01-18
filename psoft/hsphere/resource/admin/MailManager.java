package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MailManager.class */
public class MailManager implements TemplateHashModel {
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    protected String status = "OK";
    protected String errorMsg = null;
    protected long mailSSLId;
    private static Category log = Category.getInstance(MailManager.class.getName());

    public TemplateModel get(String key) {
        if ("status".equals(key)) {
            return new TemplateString(this.status);
        }
        if ("msg".equals(key)) {
            return new TemplateString(this.errorMsg);
        }
        if ("mail_ssl".equals(key)) {
            try {
                return getMailSSL();
            } catch (Exception e) {
                return null;
            }
        } else if ("mail_srs".equals(key)) {
            try {
                return new MailSRS();
            } catch (Exception e2) {
                return null;
            }
        } else {
            return AccessTemplateMethodWrapper.getMethod(this, key);
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public MailSSL getMailSSL() throws Exception {
        if (this.mailSSLId == 0) {
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT id FROM mail_ssl");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    this.mailSSLId = rs.getLong(1);
                }
                Session.closeStatement(ps);
                con.close();
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        return MailSSL.get(this.mailSSLId);
    }

    public void enableMailSSL(long zone_id, String key, String cert) throws Exception {
        MailSSL mssl = MailSSL.enableMailSSL(zone_id);
        mssl.installMailSSLOnServers(key, cert);
    }

    public TemplateModel FM_enableMailSSL(long zone_id, String key, String cert) throws Exception {
        try {
            enableMailSSL(zone_id, key, cert);
            this.status = "OK";
        } catch (Exception e) {
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
            log.error("Error enable mail SSL", e);
            disableMailSSL();
        }
        return this;
    }

    public void disableMailSSL() throws Exception {
        MailSSL mssl = getMailSSL();
        mssl.disableMailSSLOnServers();
        mssl.delete();
    }

    public TemplateModel FM_disableMailSSL() throws Exception {
        try {
            disableMailSSL();
            this.status = "OK";
        } catch (Exception e) {
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
            log.error("Error disable mail SSL", e);
        }
        return this;
    }

    public TemplateModel FM_getMailServers() throws Exception {
        return new TemplateList(MailSSL.getMailServers());
    }
}
