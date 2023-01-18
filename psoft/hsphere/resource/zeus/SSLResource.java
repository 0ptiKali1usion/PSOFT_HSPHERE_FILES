package psoft.hsphere.resource.zeus;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.IPDeletedResource;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/SSLResource.class */
public class SSLResource extends Resource implements IPDeletedResource {
    protected String file;
    protected String key;
    protected String siteName;
    protected int rev;
    protected int cert;
    protected int chain;
    protected int vclient;
    protected int vdepth;

    public SSLResource(int type, Collection init) throws Exception {
        super(type, init);
        Iterator i = init.iterator();
        this.key = (String) i.next();
        this.file = (String) i.next();
        if (i.hasNext()) {
            this.siteName = (String) i.next();
        } else {
            this.siteName = "";
        }
    }

    public SSLResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT chain, cert, rev, vdepth, vclient, site_name FROM apache_ssl WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.chain = rs.getInt(1);
                this.cert = rs.getInt(2);
                this.rev = rs.getInt(3);
                this.vdepth = rs.getInt(4);
                this.vclient = rs.getInt(5);
                this.siteName = rs.getString(6);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        VirtualHostingResource vh = (VirtualHostingResource) getParent().get();
        if (!vh.hasDedicatedIp()) {
            throw new HSUserException("sslresource.ip");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO apache_ssl (id, chain, cert, rev, vdepth, vclient, site_name) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.chain);
            ps.setInt(3, this.cert);
            ps.setInt(4, this.rev);
            ps.setInt(5, this.vdepth);
            ps.setInt(6, this.vclient);
            ps.setString(7, this.siteName);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            installCertificate(this.file, this.key);
            restart();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected HostEntry getHost() throws Exception {
        return HostManager.getHost(recursiveGet("host_id"));
    }

    public TemplateModel FM_getConfigPath() throws Exception {
        return new TemplateString(recursiveGet("dir") + "/ssl.conf/" + recursiveGet("real_name"));
    }

    protected void install(String file, String filename) throws Exception {
        List l = new ArrayList();
        l.add(recursiveGet("login").toString());
        l.add(FM_getConfigPath().toString());
        l.add(filename);
        Iterator i = getHost().exec("ssl-install", l, file).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
    }

    protected void commitAction(String action) throws Exception {
        List l = new ArrayList();
        l.add(recursiveGet("login").toString());
        l.add(FM_getConfigPath().toString());
        l.add(action);
        Iterator i = getHost().exec("ssl-install", l).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate.different");
        }
    }

    public TemplateModel FM_installRevFile(String file) throws Exception {
        install(file, "ca-bundle.crl");
        this.rev = 1;
        update();
        return this;
    }

    public TemplateModel FM_installCertFile(String file) throws Exception {
        install(file, "ca-bundle.crt");
        this.cert = 1;
        update();
        return this;
    }

    public TemplateModel FM_installChainFile(String file) throws Exception {
        install(file, "ca.crt");
        this.chain = 1;
        update();
        return this;
    }

    public TemplateModel FM_removeChainFile() throws Exception {
        this.chain = 0;
        update();
        return this;
    }

    public TemplateModel FM_removeCertFile() throws Exception {
        this.cert = 0;
        update();
        return this;
    }

    public TemplateModel FM_removeRevFile() throws Exception {
        this.rev = 0;
        update();
        return this;
    }

    public TemplateModel FM_installCertificate(String file, String key) throws Exception {
        installCertificate(file, key);
        return this;
    }

    protected void installCertificate(String file, String key) throws Exception {
        install(file, "server.crt");
        install(key, "server.key");
        commitAction("commit_pair");
        restart();
    }

    public TemplateModel FM_installCertificateOnly(String file) throws Exception {
        install(file, "server.crt");
        commitAction("commit_cert");
        restart();
        return this;
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            VirtualHostingResource vh = (VirtualHostingResource) getParent().get();
            vh.deleteSSL();
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM apache_ssl WHERE id = ?");
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

    protected void restart() throws Exception {
        VirtualHostingResource vh = (VirtualHostingResource) getParent().get();
        vh.restartSSL(getId());
        Session.getLog().debug("ZEUS was restarted by SSL(cert change)");
    }

    protected void update() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE apache_ssl SET rev = ?, chain = ?, cert = ?, vdepth = ?, vclient = ? WHERE id = ?");
            ps.setInt(1, this.rev);
            ps.setInt(2, this.chain);
            ps.setInt(3, this.cert);
            ps.setInt(4, this.vdepth);
            ps.setInt(5, this.vclient);
            ps.setLong(6, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return key.equals("chain") ? new TemplateString(this.chain) : key.equals("rev") ? new TemplateString(this.rev) : key.equals(MerchantGatewayManager.MG_CERT_PREFIX) ? new TemplateString(this.cert) : key.equals("vdepth") ? new TemplateString(this.vdepth) : key.equals("vclient") ? new TemplateString(this.vclient) : key.equals("site_name") ? new TemplateString(this.siteName) : super.get(key);
    }

    @Override // psoft.hsphere.resource.IPDeletedResource
    public void ipDelete() throws Exception {
        Session.getLog().debug("SSL was deleted by IP");
        delete(false);
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ssl.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssl.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssl.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ssl.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
