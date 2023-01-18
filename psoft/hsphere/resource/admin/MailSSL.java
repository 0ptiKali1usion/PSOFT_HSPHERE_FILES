package psoft.hsphere.resource.admin;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MailSSL.class */
public class MailSSL extends SharedObject implements TemplateHashModel {
    public static final String MAIL_SSL_PATH = "/hsphere/local/var/vpopmail/etc";
    public static final String MAIL_SSL_FILE_NAME = "mail.pem";
    public static final String MAIL_SSL_CHAIN_PATH = "/hsphere/local/var/vpopmail/etc";
    public static final String MAIL_SSL_CHAIN_FILE_NAME = "ca-mail.pem";
    public static final int CERT = 0;
    public static final int REV = 1;
    public static final int CHAIN = 2;
    public static final int CA_CERT = 3;
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ERROR = "ERROR";
    protected String status;
    protected String errorMsg;
    protected long zoneId;
    protected String key;
    protected String cert;
    protected String rev;
    protected String chain;
    protected String caCert;
    protected List mailSSLHosts;
    protected int certType;
    private static Category log = Category.getInstance(MailManager.class.getName());

    public MailSSL(long id, long zoneId, String key, String cert, String rev, String chain, String caCert, int certType) throws Exception {
        super(id);
        this.status = "OK";
        this.errorMsg = null;
        this.key = new String();
        this.cert = new String();
        this.rev = new String();
        this.chain = new String();
        this.caCert = new String();
        this.mailSSLHosts = new ArrayList();
        this.zoneId = zoneId;
        this.key = key;
        this.cert = cert;
        this.rev = rev;
        this.chain = chain;
        this.caCert = caCert;
        this.certType = certType;
        loadMailSSLHosts();
    }

    public MailSSL(long id, long zoneId) throws Exception {
        this(id, zoneId, null, null, null, null, null, 0);
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        return "msg".equals(key) ? new TemplateString(this.errorMsg) : "status".equals(key) ? new TemplateString(this.status) : "zone_id".equals(key) ? new TemplateString(this.zoneId) : "mail_hosts".equals(key) ? new TemplateList(this.mailSSLHosts) : super.get(key);
    }

    public boolean isEmpty() {
        return false;
    }

    public static MailSSL get(long id) throws Exception {
        synchronized (SharedObject.getLock(id)) {
            MailSSL tmp = (MailSSL) get(id, MailSSL.class);
            if (tmp != null) {
                return tmp;
            }
            return load(id);
        }
    }

    protected static MailSSL load(long id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT zone_id, pr_key, cert, rev, chain,ca_cert, cert_type FROM mail_ssl WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                MailSSL mssl = new MailSSL(id, rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getInt(7));
                Session.closeStatement(ps);
                con.close();
                return mssl;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static MailSSL enableMailSSL(long zoneId) throws Exception {
        long newId = Session.getNewIdAsLong();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("INSERT INTO mail_ssl (id, zone_id) values (?, ?)");
                ps.setLong(1, newId);
                ps.setLong(2, zoneId);
                ps.executeUpdate();
                MailSSL mailSSL = new MailSSL(newId, zoneId);
                Session.closeStatement(ps);
                con.close();
                return mailSSL;
            } catch (Exception e) {
                Session.getLog().error("Unsuccessful try to create Mail SSL", e);
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void delete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mail_ssl WHERE id = ?");
            ps.setLong(1, this.f51id);
            ps.executeUpdate();
            remove(this.f51id, MailSSL.class);
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateCertificatePair(String tmpKey, String tmpCert) throws Exception {
        log.info("in MailSSL.updateCertificatePair()!");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_ssl SET pr_key = ?, cert = ?, cert_type = ? WHERE id = ?");
            ps.setString(1, tmpKey);
            ps.setString(2, tmpCert);
            ps.setInt(3, 0);
            ps.setLong(4, this.f51id);
            ps.executeUpdate();
            this.key = tmpKey;
            this.cert = tmpCert;
            this.certType = 0;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateRev(String tmpRev) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_ssl SET rev = ?, cert_type = ? WHERE id = ?");
            ps.setString(1, tmpRev);
            ps.setInt(2, 1);
            ps.setLong(3, this.f51id);
            ps.executeUpdate();
            this.rev = tmpRev;
            this.certType = 1;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateChain(String tmpChain) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_ssl SET chain = ?, cert_type= ? WHERE id = ?");
            ps.setString(1, tmpChain);
            ps.setInt(2, 2);
            ps.setLong(3, this.f51id);
            ps.executeUpdate();
            this.chain = tmpChain;
            this.certType = 2;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void updateCACert(String tmpCACert) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE mail_ssl SET ca_cert = ?, cert_type = ? WHERE id = ?");
            ps.setString(1, tmpCACert);
            ps.setInt(2, 3);
            ps.setLong(3, this.f51id);
            ps.executeUpdate();
            this.caCert = tmpCACert;
            this.certType = 3;
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void checkCertPairOnCP(String tmpKey, String tmpCert) throws Exception {
        checkMail(tmpCert, "server.crt");
        checkMail(tmpKey, "server.key");
        commitMailAction("commit_pair");
    }

    protected void checkMail(String file, String filename) throws Exception {
        if (file == null || "".equals(file)) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
        HostEntry he = HostManager.getCPHost();
        List l = new ArrayList();
        l.add(filename);
        Iterator i = he.exec("ssl-mail-check", l, file).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
    }

    protected void commitMailAction(String action) throws Exception {
        HostEntry he = HostManager.getCPHost();
        List l = new ArrayList();
        l.add(action);
        Iterator i = he.exec("ssl-mail-check", l).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate.different");
        }
    }

    protected void physicalRemoveMailSSL(HostEntry he) throws Exception {
        List l = new ArrayList();
        l.add("/hsphere/local/var/vpopmail/etc/mail.pem");
        he.exec("ssl-mail-remove", l);
        he.exec("restart-mail-server", new ArrayList());
    }

    protected void installMailCert(HostEntry he, String file, String key) throws Exception {
        installMailCert(he, file, key, "/hsphere/local/var/vpopmail/etc", MAIL_SSL_FILE_NAME, true);
    }

    protected void installMailCert(HostEntry he, String file, String key, String path, String fileName, boolean putKey) throws Exception {
        List l = new ArrayList();
        l.add(path);
        l.add(fileName);
        StringBuffer resultFile = new StringBuffer();
        if (putKey) {
            resultFile.append(key);
            resultFile.append('\n');
        }
        resultFile.append(file);
        he.exec("ssl-mail-install", l, resultFile.toString());
        he.exec("restart-mail-server", new ArrayList());
    }

    public void installMailCertOnly(String file) throws Exception {
        installMailSSLOnServers(this.key, file);
    }

    public TemplateModel FM_installMailCertOnly(String file) throws Exception {
        try {
            installMailCertOnly(file);
            this.status = "OK";
        } catch (Exception e) {
            log.error("failed to install Mail SSL certificate", e);
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public void disableMailSSLOnServers() throws Exception {
        for (int i = 0; i < this.mailSSLHosts.size(); i++) {
            MailSSLHost mhost = (MailSSLHost) this.mailSSLHosts.get(i);
            long serverId = mhost.getLServerId();
            try {
                HostEntry he = mhost.getHostEntry();
                physicalRemoveMailSSL(he);
            } catch (Exception e) {
                log.warn("Cannot disable Mail SSL on mail server with id=" + serverId);
                Session.addMessage(Localizer.translateMessage("mailssl.disable_fail", new Object[]{String.valueOf(serverId)}));
            }
        }
        removeMailSSLHosts();
    }

    public void installMailRev(String rev) throws Exception {
        checkMail(rev, "ca-bundle.crl");
        updateRev(rev);
        repostCertificates();
    }

    public TemplateModel FM_installMailRev(String rev) throws Exception {
        try {
            installMailRev(rev);
            this.status = "OK";
        } catch (Exception e) {
            log.error("failed to install Mail SSL", e);
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public void installMailChain(String chain) throws Exception {
        checkMail(chain, "ca.crt");
        updateChain(chain);
        repostCertificates();
    }

    public TemplateModel FM_installMailChain(String chain) throws Exception {
        try {
            installMailChain(chain);
            this.status = "OK";
        } catch (Exception e) {
            log.error("failed to install Mail SSL", e);
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public void installMailCert(String cert) throws Exception {
        checkMail(cert, "ca-bundle.crt");
        updateCACert(cert);
        repostCertificates();
    }

    public TemplateModel FM_installMailCert(String cert) throws Exception {
        try {
            installMailCert(cert);
            this.status = "OK";
        } catch (Exception e) {
            log.error("failed to install Mail SSL", e);
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public void repostCertificates() throws Exception {
        List mailServers = getMailServers();
        if (mailServers != null && !mailServers.isEmpty()) {
            int counter = 0;
            for (int i = 0; i < mailServers.size(); i++) {
                HostEntry he = (HostEntry) mailServers.get(i);
                try {
                    switch (this.certType) {
                        case 0:
                            installMailCert(he, this.cert, this.key);
                            break;
                        case 1:
                            installMailCert(he, this.rev, this.key);
                            break;
                        case 2:
                            installMailCert(he, this.chain, this.key, "/hsphere/local/var/vpopmail/etc", MAIL_SSL_CHAIN_FILE_NAME, false);
                            break;
                        case 3:
                            installMailCert(he, this.caCert, this.key, "/hsphere/local/var/vpopmail/etc", MAIL_SSL_CHAIN_FILE_NAME, false);
                            break;
                    }
                    Session.addMessage(Localizer.translateMessage("mailssl.install_success", new Object[]{he.getName()}));
                    if (!isCertInstalledOnServer(he.getId())) {
                        addMailSSLHost(he.getId());
                    }
                    counter++;
                } catch (Exception e) {
                    log.warn("Cannot install Mail SSL on server " + he.getName(), e);
                    Session.addMessage(Localizer.translateMessage("mailssl.install_fail", new Object[]{he.getName()}));
                }
            }
            loadMailSSLHosts();
            if (counter > 0) {
                this.status = "OK";
                return;
            }
            this.errorMsg = Localizer.translateMessage("mailssl.install_failed");
            this.status = "ERROR";
            delete();
            return;
        }
        this.errorMsg = Localizer.translateMessage("mailssl.not_found");
        this.status = "ERROR";
        delete();
    }

    public TemplateModel FM_repostCertificates() throws Exception {
        try {
            repostCertificates();
            this.status = "OK";
        } catch (Exception e) {
            log.error("Failed to repost Mail SSL certs", e);
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    public static List getMailServers() throws Exception {
        try {
            return HostManager.getHostsByGroupType(3);
        } catch (Exception e) {
            log.warn("Cannot find any mail server", e);
            return null;
        }
    }

    public TemplateModel FM_getMailServers() throws Exception {
        return new TemplateList(getMailServers());
    }

    public void installMailSSLOnServers(String tmpKey, String tmpCert) throws Exception {
        checkCertPairOnCP(tmpKey, tmpCert);
        updateCertificatePair(tmpKey, tmpCert);
        repostCertificates();
    }

    public TemplateModel FM_installMailSSL(String tmpKey, String tmpCert) throws Exception {
        try {
            installMailSSLOnServers(tmpKey, tmpCert);
            this.status = "OK";
        } catch (Exception e) {
            log.error("Failed to install Mail SSL certs", e);
            this.status = "ERROR";
            this.errorMsg = e.getMessage();
        }
        return this;
    }

    protected void loadMailSSLHosts() throws Exception {
        synchronized (this) {
            this.mailSSLHosts.clear();
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT l_server_id, cert FROM mail_ssl_hosts WHERE mail_ssl_id = ?");
            ps.setLong(1, this.f51id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                long serverId = rs.getLong(1);
                int cert = rs.getInt(2);
                try {
                    HostEntry he = HostManager.getHost(serverId);
                    MailSSLHost mhost = new MailSSLHost(he, cert);
                    this.mailSSLHosts.add(mhost);
                } catch (Exception e) {
                    log.error("Cannot load Mail SSL Host with id = " + serverId, e);
                }
            }
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected void removeMailSSLHosts() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM mail_ssl_hosts WHERE mail_ssl_id = ?");
            ps.setLong(1, this.f51id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            loadMailSSLHosts();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void addMailSSLHost(long serverId) throws Exception {
        synchronized (SharedObject.getLock(this.f51id)) {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO mail_ssl_hosts (mail_ssl_id, l_server_id, cert) values (?, ?, ?)");
            ps.setLong(1, this.f51id);
            ps.setLong(2, serverId);
            ps.setInt(3, 1);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected boolean isCertInstalledOnServer(long serverId) throws Exception {
        for (int i = 0; i < this.mailSSLHosts.size(); i++) {
            MailSSLHost mhost = (MailSSLHost) this.mailSSLHosts.get(i);
            if (serverId == mhost.getLServerId()) {
                return mhost.isCertInstalled();
            }
        }
        return false;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/MailSSL$MailSSLHost.class */
    public class MailSSLHost implements TemplateHashModel {
        protected long lServerId;

        /* renamed from: he */
        protected HostEntry f172he;
        protected boolean isCertInstalled;

        public MailSSLHost(HostEntry he, int cert) throws Exception {
            MailSSL.this = r5;
            this.f172he = he;
            this.lServerId = he.getId();
            if (cert == 1) {
                this.isCertInstalled = true;
            } else {
                this.isCertInstalled = false;
            }
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel get(String key) {
            return "l_server_id".equals(key) ? new TemplateString(this.lServerId) : "server".equals(key) ? this.f172he : MerchantGatewayManager.MG_CERT_PREFIX.equals(key) ? new TemplateString(this.isCertInstalled) : AccessTemplateMethodWrapper.getMethod(this, key);
        }

        public long getLServerId() {
            return this.lServerId;
        }

        public HostEntry getHostEntry() {
            return this.f172he;
        }

        public boolean isCertInstalled() {
            return this.isCertInstalled;
        }
    }
}
