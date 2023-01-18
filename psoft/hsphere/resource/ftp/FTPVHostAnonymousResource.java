package psoft.hsphere.resource.ftp;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.IPDependentResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ftp/FTPVHostAnonymousResource.class */
public class FTPVHostAnonymousResource extends Resource implements IPDependentResource {
    protected int incoming;

    public FTPVHostAnonymousResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.incoming = 0;
    }

    public FTPVHostAnonymousResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT incoming FROM ftp_vhost_anon WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.incoming = rs.getInt(1);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO ftp_vhost_anon (id, incoming) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.incoming);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            restartFTP();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            restartFTP();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM ftp_vhost_anon WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void restartFTP() throws Exception {
        ((FTPVHostResource) getParent().get()).FM_updateServer();
    }

    public TemplateModel FM_update(int incoming) throws Exception {
        int oldincoming = this.incoming;
        try {
            this.incoming = incoming;
            restartFTP();
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("UPDATE ftp_vhost_anon SET incoming = ? WHERE id = ?");
            ps.setInt(1, this.incoming);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            return this;
        } catch (Exception e) {
            this.incoming = oldincoming;
            throw e;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "allowIncoming".equals(key) ? new TemplateString(this.incoming) : super.get(key);
    }

    protected String _getName() {
        try {
            return ((FTPVHostResource) getParent().get()).getIp().toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ftp_vhost_anonymous.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("ftp_vhost_anonymous.desc", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.resource.IPDependentResource
    public void ipRestart() throws Exception {
        restartFTP();
    }
}
