package psoft.hsphere.resource.admin;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.Set;
import java.util.StringTokenizer;
import psoft.hsphere.Account;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.SharedObject;
import psoft.hsphere.User;
import psoft.hsphere.global.Globals;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmResellerSSL.class */
public class AdmResellerSSL extends SharedObject implements TemplateHashModel {

    /* renamed from: id */
    private long f168id;

    /* renamed from: ip */
    private String f169ip;
    private String serverType;
    private long resellerId;
    private String resellerLogin;
    private String port;
    private String insec_port;
    private String aliasName;
    private String keyPath;
    private String crtPath;
    private int sslType;
    protected int rev;
    protected int cert;
    protected int chain;
    protected static final String CONFIG_PATH = "/hsphere/local/home/cpanel/apache/etc/sites/";
    static Boolean toGetPort = new Boolean(true);
    public static Collection sslBaseTypes = SSLType.getRegisteredTypes();
    public static String[] sslTypeArray = (String[]) sslBaseTypes.toArray(new String[0]);

    public static int sslBaseTypeId(String type) {
        return SSLType.getId(type);
    }

    public static boolean isSSLTypeAvailable(String type) {
        return SSLType.isAvailable(type);
    }

    public AdmResellerSSL(long id, String ip, long reseller_id, String alias, String port, String key_path, String crt_path) throws Exception {
        this(id, ip, reseller_id, alias, port, key_path, crt_path, SSLType.IP_BASED.getId());
    }

    public AdmResellerSSL(long id, String ip, long reseller_id, String alias, String port, String key_path, String crt_path, int rev, int chain, int cert) throws Exception {
        this(id, ip, reseller_id, alias, port, key_path, crt_path, SSLType.IP_BASED.getId(), rev, chain, cert);
    }

    public AdmResellerSSL(long id, String ip, long reseller_id, String alias, String port, String key_path, String crt_path, int sslType) throws Exception {
        this(id, ip, reseller_id, alias, port, key_path, crt_path, sslType, 0, 0, 0);
    }

    public AdmResellerSSL(long id, String ip, long reseller_id, String alias, String port, String key_path, String crt_path, int sslType, int rev, int chain, int cert) throws Exception {
        super(id);
        this.f168id = id;
        this.f169ip = ip;
        this.resellerId = reseller_id;
        if (reseller_id > 0) {
            this.resellerLogin = Reseller.getReseller(this.resellerId).getUser();
        }
        this.aliasName = alias;
        this.port = port;
        this.keyPath = key_path;
        this.crtPath = crt_path;
        this.sslType = sslType;
        this.rev = rev;
        this.chain = chain;
        this.cert = cert;
    }

    @Override // psoft.hsphere.SharedObject
    public long getId() {
        return this.f168id;
    }

    public static AdmResellerSSL get(long id) throws Exception {
        int sslType = 0;
        if (Globals.isObjectDisabled("cp_ssl_ip_based") == 0) {
            sslType = SSLType.IP_BASED.getId();
        } else if (Globals.isObjectDisabled("cp_ssl_port_based") == 0) {
            sslType = SSLType.PORT_BASED.getId();
        }
        return get(id, sslType);
    }

    /* JADX WARN: Finally extract failed */
    public static AdmResellerSSL get(long id, int sslType) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        long tmpId = 0;
        String tmpIp = "";
        long tmpResellerId = 0;
        String tmpAlias = "";
        String tmpPort = "";
        String tmpKeyPath = "";
        String tmpCrtPath = "";
        int tmpRev = 0;
        int tmpChain = 0;
        int tmpCert = 0;
        try {
            ps = con.prepareStatement("SELECT * FROM reseller_ssl WHERE id = ?");
            ps.setLong(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tmpId = rs.getInt(1);
                tmpIp = rs.getString(2);
                tmpResellerId = rs.getInt(3);
                tmpAlias = rs.getString(4);
                tmpPort = rs.getString(5);
                tmpKeyPath = rs.getString(6);
                tmpCrtPath = rs.getString(7);
                tmpRev = rs.getInt(8);
                tmpChain = rs.getInt(9);
                tmpCert = rs.getInt(10);
            }
            rs.close();
            Session.closeStatement(ps);
            con.close();
            return new AdmResellerSSL(tmpId, tmpIp, tmpResellerId, tmpAlias, tmpPort, tmpKeyPath, tmpCrtPath, sslType, tmpRev, tmpChain, tmpCert);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.SharedObject
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("ip")) {
            return new TemplateString(this.f169ip);
        }
        if (key.equals("server_alias")) {
            return new TemplateString(this.aliasName);
        }
        if (key.equals("port")) {
            return new TemplateString(this.port);
        }
        if (key.equals("insec_port")) {
            return new TemplateString(this.insec_port);
        }
        return key.equals("key_path") ? new TemplateString(this.keyPath) : key.equals("crt_path") ? new TemplateString(this.crtPath) : key.equals("chain") ? new TemplateString(this.chain) : key.equals("rev") ? new TemplateString(this.rev) : key.equals(MerchantGatewayManager.MG_CERT_PREFIX) ? new TemplateString(this.cert) : super.get(key);
    }

    protected void dbDelete() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM reseller_ssl WHERE id = ?");
            ps.setLong(1, this.f168id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected void deleteConfig() throws Exception {
        List l = new ArrayList();
        HostEntry he = HostManager.getCPHost();
        String confName = this.resellerLogin + ".conf";
        l.add(confName);
        he.exec("apache-cp-ssl-delconf", l);
    }

    public void delete() throws Exception {
        dbDelete();
        deleteConfig();
        deleteKeys();
        Session.getLog().debug("Try to create ticket");
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            User adminUser = User.getUser(FMACLManager.ADMIN);
            Account adminAccount = (Account) Account.get(new ResourceId(1L, 0));
            Session.setUser(adminUser);
            Session.setAccount(adminAccount);
            Ticket.create(Localizer.translateMessage("cpssl.tt_title"), 75, Localizer.translateMessage("cpssl.tt_dis_msg", new Object[]{this.resellerLogin}), null, 1, 1, 0, 0, 0, 0);
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public static AdmResellerSSL create() throws Exception {
        long id = Session.getNewIdAsLong();
        return new AdmResellerSSL(id, "", 0L, "", "", "", "");
    }

    public static AdmResellerSSL create(int sslType) throws Exception {
        long id = Session.getNewIdAsLong();
        return new AdmResellerSSL(id, "", 0L, "", "", "", "", sslType);
    }

    protected String getCPSSLIP() throws Exception {
        String ip = "";
        if (this.sslType == SSLType.PORT_BASED.getId()) {
            Session.getLog().debug("in AdmResellerSSL.getCPSSLIP() where SSLType = PORT_BASED");
            return HostManager.getCPHost().getIP().toString();
        }
        Session.getLog().debug("in AdmResellerSSL.getCPSSLIP() where SSLType = IP BASED");
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT ip FROM l_server_ips WHERE flag = ? AND r_id = ?");
            ps.setInt(1, 8);
            ps.setLong(2, this.f168id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                ip = rs.getString(1);
            }
            rs.close();
            Session.closeStatement(ps);
            con.close();
            return ip;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getServerType() throws Exception {
        HostEntry he = HostManager.getCPHost();
        List l = new ArrayList();
        Collection result = he.exec("detect-cp-server.sh", l);
        int size = result.size();
        if (size > 0) {
            Object[] res = result.toArray();
            return (String) res[0];
        }
        return "";
    }

    protected static String getSslPortRange() {
        try {
            return Session.getProperty("RESELLER_SSL_PORT_RANGE");
        } catch (MissingResourceException e) {
            return "";
        }
    }

    protected static String getSslSecuredPort() {
        try {
            return Session.getProperty("RESELLER_SSL_SEC_PORT");
        } catch (MissingResourceException e) {
            return "";
        }
    }

    protected static String getInsecuredPort() {
        try {
            return Session.getProperty("RESELLER_SSL_INSEC_PORT");
        } catch (MissingResourceException e) {
            return "";
        }
    }

    protected static boolean isIpBasedSslAvailable() {
        return ("".equals(getSslSecuredPort()) || "".equals(getInsecuredPort())) ? false : true;
    }

    protected static boolean isPortBasedSslAvailable() {
        return !"".equals(getSslPortRange());
    }

    /* JADX WARN: Finally extract failed */
    protected static synchronized String getFreeSSLPort() throws Exception {
        long beginToken;
        long endToken;
        List takenPorts = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT port FROM reseller_ssl WHERE ip = ? ORDER BY port");
            ps.setString(1, HostManager.getCPHost().getIP().toString());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                takenPorts.add(rs.getString(1));
            }
            rs.close();
            Session.closeStatement(ps);
            con.close();
            int size = takenPorts.size();
            long[] tPorts = new long[size];
            for (int i = 0; i < size; i++) {
                tPorts[i] = Long.parseLong((String) takenPorts.get(i));
            }
            Arrays.sort(tPorts);
            String resSSLPortRange = getSslPortRange();
            Session.getLog().debug("resSSLPortRange = " + resSSLPortRange);
            StringTokenizer tokenizer = new StringTokenizer(resSSLPortRange, ",");
            while (tokenizer.hasMoreTokens()) {
                String oneRange = tokenizer.nextToken().trim();
                Session.getLog().debug("oneRange = " + oneRange);
                StringTokenizer parseOneRange = new StringTokenizer(oneRange, "-");
                int countTokens = parseOneRange.countTokens();
                if (countTokens == 1) {
                    beginToken = Long.parseLong(parseOneRange.nextToken().trim());
                    endToken = beginToken;
                } else {
                    beginToken = Long.parseLong(parseOneRange.nextToken().trim());
                    endToken = Long.parseLong(parseOneRange.nextToken().trim());
                }
                if (size == 0) {
                    return String.valueOf(beginToken);
                }
                long j = beginToken;
                while (true) {
                    long i2 = j;
                    if (i2 <= endToken) {
                        if (Arrays.binarySearch(tPorts, i2) >= 0) {
                            j = i2 + 1;
                        } else {
                            return String.valueOf(i2);
                        }
                    }
                }
            }
            return "";
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public String getResellerPort() throws Exception {
        if (this.sslType == SSLType.PORT_BASED.getId()) {
            return getFreeSSLPort();
        }
        return getSslSecuredPort();
    }

    protected void install(String file, String filename) throws Exception {
        if (file == null || "".equals(file)) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
        List l = new ArrayList();
        this.serverType = getServerType();
        l.add(this.serverType);
        l.add("cpanel");
        l.add(CONFIG_PATH + this.resellerLogin);
        l.add(filename);
        HostEntry he = HostManager.getCPHost();
        Session.getLog().debug("this.serverType = " + this.serverType);
        Session.getLog().debug("CONFIG_PATH+resellerLogin = /hsphere/local/home/cpanel/apache/etc/sites/" + this.resellerLogin);
        Session.getLog().debug("filename = " + filename);
        Session.getLog().debug("HostEntry = " + he.getName());
        Iterator i = he.exec("cp-ssl-install", l, file).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate", new Object[]{filename});
        }
    }

    protected void commitAction(String action) throws Exception {
        List l = new ArrayList();
        l.add(this.serverType);
        l.add("cpanel");
        l.add(CONFIG_PATH + this.resellerLogin);
        l.add(action);
        HostEntry he = HostManager.getCPHost();
        Iterator i = he.exec("cp-ssl-install", l).iterator();
        if (i.hasNext() && "INVALID".equals(i.next().toString())) {
            throw new HSUserException("sslresource.certificate.different");
        }
    }

    protected void dbSave() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO reseller_ssl(id, ip, reseller_id, cp_alias, port, ssl_key_path, ssl_crt_path) VALUES(?, ?, ?, ?, ?, ?, ?)");
            ps.setLong(1, this.f168id);
            ps.setString(2, this.f169ip);
            ps.setLong(3, this.resellerId);
            ps.setString(4, this.aliasName);
            ps.setString(5, this.port);
            ps.setString(6, this.keyPath);
            ps.setString(7, this.crtPath);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void makeVirtualHost() throws Exception {
        Session.getLog().debug("IN makeVirtualHost()");
        SimpleHash root = new SimpleHash();
        this.insec_port = getInsecuredPort();
        root.put("res_ssl", this);
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        Session.getLog().debug("ssl type = " + this.sslType);
        if (this.sslType == SSLType.PORT_BASED.getId()) {
            Session.getTemplate("/admin/vhost_port_based.config").process(root, out);
            Session.getLog().debug("Port-based reseller SSL");
        } else {
            Session.getTemplate("/admin/vhost.config").process(root, out);
            Session.getLog().debug("IP-based reseller SSL");
        }
        out.close();
        String config_txt = sw.toString();
        List l = new ArrayList();
        l.add("/" + this.resellerLogin + ".conf");
        HostEntry he = HostManager.getCPHost();
        he.exec("apache-cp-ssl-saveconf", l, config_txt);
    }

    protected void reindexConfig() throws Exception {
        List l = new ArrayList();
        HostEntry he = HostManager.getCPHost();
        he.exec("apache-cp-ssl-indexconf", l);
    }

    public TemplateModel FM_getConfigPath() throws Exception {
        return new TemplateString(CONFIG_PATH + this.resellerLogin);
    }

    public TemplateModel FM_doImplement(String alias, String ssl_key, String ssl_crt) throws Exception {
        Session.getLog().debug("in AdmResellerSSL.FM_doImplement()");
        this.aliasName = alias;
        this.resellerId = Session.getResellerId();
        this.resellerLogin = Reseller.getReseller(this.resellerId).getUser();
        this.serverType = getServerType();
        synchronized (toGetPort) {
            this.f169ip = getCPSSLIP();
            if (this.f169ip == null || "".equals(this.f169ip)) {
                throw new HSUserException("cpssl.ips");
            }
            this.port = getResellerPort();
            if (this.port == null || "".equals(this.port)) {
                throw new HSUserException("cpssl.ports");
            }
            this.keyPath = CONFIG_PATH + this.resellerLogin + "/server.key";
            this.crtPath = CONFIG_PATH + this.resellerLogin + "/server.crt";
            Session.getLog().debug("Before exec script");
            install(ssl_crt, "server.crt");
            install(ssl_key, "server.key");
            commitAction("commit_pair");
            makeVirtualHost();
            reindexConfig();
            dbSave();
        }
        Session.getLog().debug("Try to create ticket");
        User oldUser = Session.getUser();
        Account oldAccount = Session.getAccount();
        try {
            User adminUser = User.getUser(FMACLManager.ADMIN);
            Account adminAccount = (Account) Account.get(new ResourceId(1L, 0));
            Session.setUser(adminUser);
            Session.setAccount(adminAccount);
            Ticket.create(Localizer.translateMessage("cpssl.tt_title"), 1, Localizer.translateMessage("cpssl.tt_msg", new Object[]{this.resellerLogin}), null, 1, 1, 0, 1, 2, 0);
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            return this;
        } catch (Throwable th) {
            Session.setUser(oldUser);
            Session.setAccount(oldAccount);
            throw th;
        }
    }

    public void deleteKeys() throws Exception {
        List l = new ArrayList();
        HostEntry he = HostManager.getCPHost();
        l.add(this.resellerLogin);
        he.exec("apache-cp-ssl-delkeys", l);
    }

    public TemplateModel FM_installCertificateOnly(String file) throws Exception {
        install(file, "server.crt");
        commitAction("commit_cert");
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
    }

    public boolean isEmpty() {
        return false;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/admin/AdmResellerSSL$SSLType.class */
    public static class SSLType {
        protected String type;
        protected int typeId;
        protected boolean availability;
        protected static Hashtable registeredTypes = new Hashtable();
        public static final SSLType UNKNOWN_TYPE = new SSLType("", -1, false);
        public static final SSLType DISABLED = new SSLType("ssl_disabled", 0, false);
        public static final SSLType IP_BASED = new SSLType("ip_based", 1, AdmResellerSSL.isIpBasedSslAvailable());
        public static final SSLType PORT_BASED = new SSLType("port_based", 2, AdmResellerSSL.isPortBasedSslAvailable());

        protected SSLType(String type, int typeId, boolean available) {
            this.type = type;
            this.typeId = typeId;
            this.availability = available;
            if (typeId > 0) {
                registeredTypes.put(type, this);
            }
        }

        public static boolean isAvailable(String type) {
            return getType(type).availability;
        }

        public boolean isAvailable() {
            return this.availability;
        }

        public static SSLType getType(String type) {
            SSLType res = (SSLType) registeredTypes.get(type);
            if (res == null) {
                return UNKNOWN_TYPE;
            }
            return res;
        }

        public String toString() {
            return this.type;
        }

        public int getId() {
            return this.typeId;
        }

        public static int getId(String type) {
            return getType(type).typeId;
        }

        public static Set getRegisteredTypes() {
            return registeredTypes.keySet();
        }
    }

    public void repostConfig(String newIp) throws Exception {
        this.f169ip = newIp;
        this.insec_port = getInsecuredPort();
        Session.getLog().debug("Insecured Port in AdmResellerSSL.repostConfig() = " + this.insec_port);
        makeVirtualHost();
        reindexConfig();
    }

    public void changeDBIP(String newIp) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE reseller_ssl SET ip=? WHERE id = ?");
            ps.setString(1, this.f169ip);
            ps.setLong(2, this.f168id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void changeDBId(long newId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE reseller_ssl SET id=? WHERE id = ?");
            ps.setLong(1, newId);
            ps.setLong(2, this.f168id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_installRevFile(String file) throws Exception {
        install(file, "ca-bundle.crl");
        this.rev = 1;
        update();
        makeVirtualHost();
        return this;
    }

    public TemplateModel FM_installCertFile(String file) throws Exception {
        install(file, "ca-bundle.crt");
        this.cert = 1;
        update();
        makeVirtualHost();
        return this;
    }

    public TemplateModel FM_installChainFile(String file) throws Exception {
        install(file, "ca.crt");
        this.chain = 1;
        update();
        makeVirtualHost();
        return this;
    }

    public TemplateModel FM_removeChainFile() throws Exception {
        this.chain = 0;
        update();
        makeVirtualHost();
        return this;
    }

    public TemplateModel FM_removeCertFile() throws Exception {
        this.cert = 0;
        update();
        makeVirtualHost();
        return this;
    }

    public TemplateModel FM_removeRevFile() throws Exception {
        this.rev = 0;
        update();
        makeVirtualHost();
        return this;
    }

    protected void update() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE reseller_ssl SET rev = ?, chain = ?, cert = ?  WHERE id = ?");
            ps.setInt(1, this.rev);
            ps.setInt(2, this.chain);
            ps.setInt(3, this.cert);
            ps.setLong(4, this.f168id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void setInsecPort(String port) throws Exception {
        this.insec_port = port;
    }

    public void setSSLType(int sslType) throws Exception {
        this.sslType = sslType;
    }
}
