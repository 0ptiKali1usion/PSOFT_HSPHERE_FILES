package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.AbstractSSLResource;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.SSLProperties;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/SSLResource.class */
public class SSLResource extends AbstractSSLResource implements HostDependentResource {
    public SSLResource(int type, Collection init) throws Exception {
        super(type, init);
        Iterator i = init.iterator();
        getProperties().setKey((String) i.next());
        getProperties().setCertificate((String) i.next());
        if (i.hasNext()) {
            getProperties().setSiteName((String) i.next());
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
                getProperties().setChain(rs.getInt(1));
                getProperties().setCert(rs.getInt(2));
                getProperties().setRev(rs.getInt(3));
                getProperties().setVdepth(rs.getInt(4));
                getProperties().setVclient(rs.getInt(5));
                getProperties().setSiteName(rs.getString(6));
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
        checkInstallPossibility();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO apache_ssl (id, chain, cert, rev, vdepth, vclient, site_name) VALUES (?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, getProperties().getChain());
            ps.setInt(3, getProperties().getCert());
            ps.setInt(4, getProperties().getRev());
            ps.setInt(5, getProperties().getVdepth());
            ps.setInt(6, getProperties().getVclient());
            ps.setString(7, getProperties().getSiteName());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            installCertificatePair(getProperties());
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
        if (file == null || "".equals(file)) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
        List l = new ArrayList();
        l.add(recursiveGet("login").toString());
        l.add(FM_getConfigPath().toString());
        l.add(filename);
        Iterator i = getHost().exec("ssl-install", l, file).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
    }

    protected void commitAction(String action, HostEntry he) throws Exception {
        List l = new ArrayList();
        l.add(recursiveGet("login").toString());
        l.add(FM_getConfigPath().toString());
        l.add(action);
        Iterator i = he.exec("ssl-install", l).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate.different");
        }
    }

    protected void commitAction(String action) throws Exception {
        commitAction(action, getHost());
    }

    public TemplateModel FM_installRevFile(String file) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setRevocationCertificate(file);
        installRevocationCert(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_installCertFile(String _certificate) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificate(_certificate);
        updateCertificate(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_installChainFile(String _certificateChain) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificateChain(_certificateChain);
        installCertificateChain(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_removeChainFile() throws Exception {
        getProperties().setChain(0);
        update(getProperties());
        return this;
    }

    public TemplateModel FM_removeCertFile() throws Exception {
        getProperties().setRev(0);
        update(getProperties());
        return this;
    }

    public TemplateModel FM_removeRevFile() throws Exception {
        getProperties().setRev(0);
        update(getProperties());
        return this;
    }

    public TemplateModel FM_installCertificate(String certificate, String key) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificate(certificate);
        tempSSLProp.setKey(key);
        installCertificatePair(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installCertificatePair(SSLProperties sslp) throws Exception {
        install(sslp.getCertificate(), "server.crt");
        install(sslp.getKey(), "server.key");
        commitAction("commit_pair");
        restart();
    }

    public TemplateModel FM_installCertificateOnly(String file) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificate(file);
        updateCertificate(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void updateCertificate(SSLProperties sslp) throws Exception {
        install(sslp.getCertificate(), "server.crt");
        commitAction("commit_cert");
        restart();
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            restart();
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
        vh.restart(true);
        Session.getLog().debug("Apache was restarted by SSL(cert change)");
    }

    protected void update(SSLProperties sslp) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE apache_ssl SET rev = ?, chain = ?, cert = ?, vdepth = ?, vclient = ? WHERE id = ?");
            ps.setInt(1, sslp.getRev());
            ps.setInt(2, sslp.getChain());
            ps.setInt(3, sslp.getCert());
            ps.setInt(4, sslp.getVdepth());
            ps.setInt(5, sslp.getVclient());
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

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return getHost().getId();
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installCACert(SSLProperties sslp) throws Exception {
        install(sslp.getCertificateAuthority(), "ca-bundle.crt");
        sslp.setCert(1);
        update(sslp);
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installCertificateChain(SSLProperties sslp) throws Exception {
        install(sslp.getCertificateChain(), "ca.crt");
        sslp.setChain(1);
        update(sslp);
        restart();
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installRevocationCert(SSLProperties sslp) throws Exception {
        install(sslp.getRevocationCertificate(), "ca-bundle.crl");
        sslp.setRev(1);
        update(sslp);
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void updateCertificateProperties(SSLProperties sslp) throws Exception {
        throw new Exception("The operation is not supported by apache SSL Resource");
    }

    @Override // psoft.hsphere.SSL
    public void checkInstallPossibility() throws Exception {
        VirtualHostingResource vh = (VirtualHostingResource) getParent().get();
        if (!vh.hasDedicatedIp()) {
            throw new HSUserException("sslresource.ip");
        }
        if (vh.getId().findChild("sharedssl") != null) {
            throw new HSUserException("sslresource.enabled");
        }
    }
}
