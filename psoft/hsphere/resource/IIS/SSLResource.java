package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.HSUserException;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.AbstractSSLResource;
import psoft.hsphere.resource.SSLProperties;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/SSLResource.class */
public class SSLResource extends AbstractSSLResource {
    public SSLResource(int type, Collection values) throws Exception {
        super(type, values);
        Iterator i = values.iterator();
        getProperties().setForced(Integer.parseInt((String) i.next()));
        getProperties().setNeed128(Integer.parseInt((String) i.next()));
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
            ps = con.prepareStatement("SELECT forced, need128, site_name FROM iis_ssl WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                getProperties().setForced(rs.getInt(1));
                getProperties().setNeed128(rs.getInt(2));
                getProperties().setSiteName(rs.getString(3));
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
            ps = con.prepareStatement("INSERT INTO iis_ssl (id, forced, need128, site_name) VALUES (?, ?, ?, ?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, getProperties().getForced());
            ps.setInt(3, getProperties().getNeed128());
            ps.setString(4, getProperties().getSiteName());
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

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            deleteCertificatePair(getProperties());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM iis_ssl WHERE id = ?");
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

    public TemplateModel FM_updateSettings(int forced, int need128) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setForced(forced);
        tempSSLProp.setNeed128(need128);
        updateCertificateProperties(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_updateCert(String cert) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificate(cert);
        updateCertificate(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_updatePair(String key, String cert) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setKey(key);
        tempSSLProp.setCertificate(cert);
        installCertificatePair(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_installCertFile(String cert) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificate(cert);
        updateCertificate(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    public TemplateModel FM_installCACert(String certificateAuthority) throws Exception {
        SSLProperties tempSSLProp = new SSLProperties(getProperties());
        tempSSLProp.setCertificateAuthority(certificateAuthority);
        installCACert(tempSSLProp);
        setProperties(tempSSLProp);
        return this;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installCACert(SSLProperties sslp) throws Exception {
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        Iterator i = he.exec("ssl-authoritycreate.asp", (String[][]) new String[]{new String[]{"authority", sslp.getCertificateChain()}}).iterator();
        if (i.hasNext()) {
            String error = (String) i.next();
            Session.getLog().debug("ERROR updating IIS SSL" + error);
            if ("IC".equals(error)) {
                throw new HSUserException("sslresource.certificate.certificat");
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installCertificatePair(SSLProperties sslp) throws Exception {
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        Iterator i = he.exec("ssl-create.asp", (String[][]) new String[]{new String[]{"hostname", name}, new String[]{"hostnum", hostnum}, new String[]{MerchantGatewayManager.MG_CERT_PREFIX, sslp.getCertificate()}, new String[]{MerchantGatewayManager.MG_KEY_PREFIX, sslp.getKey()}, new String[]{"forced", Integer.toString(sslp.getForced())}, new String[]{"need128", Integer.toString(sslp.getNeed128())}}).iterator();
        if (i.hasNext()) {
            String error = (String) i.next();
            if ("IK".equals(error)) {
                throw new HSUserException("sslresource.certificate.key");
            }
            if ("IC".equals(error)) {
                throw new HSUserException("sslresource.certificate.certificat");
            }
            if ("DK".equals(error)) {
                throw new HSUserException("sslresource.certificate.different");
            }
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public void deleteCertificatePair(SSLProperties sslp) throws Exception {
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        he.exec("ssl-delete.asp", (String[][]) new String[]{new String[]{"hostname", name}, new String[]{"hostnum", hostnum}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void updateCertificateProperties(SSLProperties sslp) throws Exception {
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        he.exec("ssl-update.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"forced", Integer.toString(sslp.getForced())}, new String[]{"need128", Integer.toString(sslp.getNeed128())}});
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE iis_ssl SET forced = ? , need128 = ? WHERE id = ?");
            ps.setInt(1, sslp.getForced());
            ps.setInt(2, sslp.getNeed128());
            ps.setLong(3, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void updateCertificate(SSLProperties sslp) throws Exception {
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        Iterator i = he.exec("ssl-update.asp", (String[][]) new String[]{new String[]{"hostname", name}, new String[]{"hostnum", hostnum}, new String[]{MerchantGatewayManager.MG_CERT_PREFIX, sslp.getCertificate()}, new String[]{"forced", Integer.toString(sslp.getForced())}, new String[]{"need128", Integer.toString(sslp.getNeed128())}}).iterator();
        if (i.hasNext()) {
            String error = (String) i.next();
            Session.getLog().debug("ERROR updating IIS SSL" + error);
            if ("IK".equals(error)) {
                throw new HSUserException("sslresource.certificate.key");
            }
            if ("IC".equals(error)) {
                throw new HSUserException("sslresource.certificate.certificat");
            }
            if ("DK".equals(error)) {
                throw new HSUserException("sslresource.certificate.different");
            }
        }
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installCertificateChain(SSLProperties sslp) throws Exception {
        installCACert(sslp);
    }

    @Override // psoft.hsphere.resource.AbstractSSLResource, psoft.hsphere.SSL
    public void installRevocationCert(SSLProperties sslp) throws Exception {
        throw new Exception("The operation is not supported by IIS SSLResource");
    }

    public void removeCertificateChain(SSLProperties sslp) throws Exception {
        throw new Exception("The operation is not supported by IIS SSLResource");
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
