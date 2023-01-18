package psoft.hsphere.resource.registrar.opensrs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import psoft.hsphere.Session;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.registrar.AsyncLoggableRegistrar;
import psoft.hsphere.resource.registrar.AsyncTLD;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.hsphere.resource.registrar.RegistrarTransactionData;
import psoft.util.TimeUtils;
import psoft.util.XMLEncoder;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/OpenSRS.class */
public class OpenSRS extends AsyncLoggableRegistrar {
    protected String username;
    protected String key;
    protected String host;
    protected int port;
    protected boolean debugMode;
    private static final String[] SUPPORTED_TLD = {"com", "net", "org", "name", "info", "biz", "ca", "tv", "us", "co.uk", "de"};
    private static final Map ASYNC_TLD = new HashMap();

    static {
        ASYNC_TLD.put("ca", new AsyncTLD(12, 180));
        ASYNC_TLD.put("co.uk", new AsyncTLD(1, 25));
        ASYNC_TLD.put("de", new AsyncTLD(1, 50));
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String getSignature() {
        return "OpenSRS";
    }

    @Override // psoft.hsphere.resource.registrar.AsyncRegistrar
    public boolean isAsyncTLD(String tld) {
        return ASYNC_TLD.get(tld.toLowerCase()) != null;
    }

    @Override // psoft.hsphere.resource.registrar.AsyncRegistrar
    public int getRegCheckInterval(String tld) {
        AsyncTLD a = (AsyncTLD) ASYNC_TLD.get(tld.toLowerCase());
        if (a == null) {
            return 0;
        }
        return a.getCheckInterval();
    }

    @Override // psoft.hsphere.resource.registrar.AsyncRegistrar
    public int getRegTimeout(String tld) {
        AsyncTLD a = (AsyncTLD) ASYNC_TLD.get(tld.toLowerCase());
        if (a == null) {
            return 0;
        }
        return a.getTimeout();
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void transfer(String domain, String tld, String username, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        getInstance().transfer(domain, tld, username, password, encodeInfo(registrant), encodeInfo(tech), encodeInfo(admin), encodeInfo(billing), dns, dat);
    }

    public OpenSRSInstance getInstance() throws Exception {
        return new OpenSRSInstance(this);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String[] getSupportedTLDs() {
        return SUPPORTED_TLD;
    }

    public OpenSRSConnection getConnection() throws RegistrarException {
        return new OpenSRSConnection(this.username, this.key, this.host, this.port);
    }

    public OpenSRS(Integer id, String description) throws Exception {
        super(id, description);
        this.debugMode = false;
        this.username = get("username");
        this.key = get(MerchantGatewayManager.MG_KEY_PREFIX);
        this.host = get("host");
        try {
            this.port = Integer.parseInt(get("port"));
        } catch (Exception e) {
            Session.getLog().error("Error parsing port " + get("port") + " assuming default OSRS port 55000");
            this.port = 55000;
        }
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public boolean lookup(String domain, String tld, RegistrarTransactionData dat) throws RegistrarException {
        try {
            checkLogin();
            return getInstance().lookup(domain, tld);
        } catch (Exception ex) {
            if (ex instanceof RegistrarException) {
                throw ((RegistrarException) ex);
            }
            RegistrarException re = new RegistrarException(RegistrarException.OTHER_ERROR, ex.getMessage());
            re.fillInStackTrace();
            throw re;
        }
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        register(domain, tld, username, password, period, registrant, tech, admin, billing, dns, null, dat);
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, Map extraInfo, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        getInstance().register(domain, tld, username, password, period, encodeInfo(registrant), encodeInfo(tech), encodeInfo(admin), encodeInfo(billing), dns, extraInfo, dat);
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void changeContacts(String domain, String tld, String username, String password, Map registrant, Map tech, Map admin, Map billing, RegistrarTransactionData dat) throws Exception {
        OpenSRSInstance instance = getInstance();
        instance.setCookie(domain, tld, username, password);
        instance.changeContactInfo("owner", encodeInfo(registrant), dat);
        instance.changeContactInfo("billing", encodeInfo(billing), dat);
        instance.changeContactInfo(FMACLManager.ADMIN, encodeInfo(admin), dat);
        instance.changeContactInfo("tech", encodeInfo(tech), dat);
    }

    @Override // psoft.hsphere.resource.registrar.AsyncLoggableRegistrar
    public boolean isRegComplete(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        OpenSRSInstance instance = getInstance();
        return instance.belongsToRSP(domain, tld, dat) != null;
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public boolean isTransferable(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        return getInstance().isTransferable(domain, tld, dat);
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public Date isTransfered(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        OpenSRSInstance instance = getInstance();
        return instance.belongsToRSP(domain, tld, dat);
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void setPassword(String domain, String tld, String username, String password, String newPassword, RegistrarTransactionData dat) throws Exception {
        OpenSRSInstance instance = getInstance();
        instance.setCookie(domain, tld, username, password);
        instance.changePassword(newPassword, dat);
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void renew(String domain, String tld, String currentExpYear, int period, Map registrant, RegistrarTransactionData dat) throws Exception {
        OpenSRSInstance instance = getInstance();
        instance.renew(domain, tld, currentExpYear, period, dat);
    }

    public void changeNameServer(String domain, String tld, String username, String password, List names, RegistrarTransactionData dat) throws Exception {
        OpenSRSInstance instance = getInstance();
        instance.setCookie(domain, tld, username, password);
        instance.changeNameServers(names, dat);
    }

    public void changeNameServers(String domain, String ext, String username, String password, List names, RegistrarTransactionData dat) throws Exception {
        throw new Exception("Not Supported");
    }

    public void setDebugMode(boolean mode) {
        this.debugMode = mode;
    }

    void log(String message) {
        if (this.debugMode) {
            System.err.println(message);
        }
    }

    public void fullAccessTest(String companyName, HashMap contactInfo, RegistrarTransactionData dat) throws Exception {
        Date start = TimeUtils.getDate();
        List nameservers = new ArrayList();
        nameservers.add("ns1.domaindirect.com");
        nameservers.add("ns2.domaindirect.com");
        OpenSRSInstance instance = getInstance();
        char c = 'a';
        while (true) {
            char ch = c;
            if (ch > 'z') {
                break;
            }
            log("Registering " + companyName + ch + ".com");
            instance.register(companyName + ch, "com", companyName + ch, "fulltest", 1, contactInfo, contactInfo, contactInfo, contactInfo, nameservers, dat);
            c = (char) (ch + 1);
        }
        for (int i = 1; i < 10; i++) {
            log("Registering " + companyName + i + ".org");
            instance.register(companyName + i, "org", companyName + i, "fulltest", 1, contactInfo, contactInfo, contactInfo, contactInfo, nameservers, dat);
        }
        List newNameServers = new ArrayList();
        newNameServers.add("cns1.idirect.com");
        newNameServers.add("cns2.idirect.com");
        log("Changing name servers for " + companyName + "1.org");
        instance.setCookie(companyName + 1, "org", companyName + 1, "fulltest");
        instance.changeNameServers(newNameServers, dat);
        Map newInfo = (Map) contactInfo.clone();
        newInfo.put("first_name", "John");
        newInfo.put("last_name", "Smith");
        newInfo.put("phone", "(123) 123-4567");
        newInfo.put("email", "xyz@changed.com");
        log("Changing contact info for " + companyName + "1.org");
        instance.changeContactInfo("billing", newInfo, dat);
        try {
            log("Registering " + companyName + "a.com");
            instance.register(companyName + 'a', "com", companyName + 'a', "fulltest", 1, contactInfo, contactInfo, contactInfo, contactInfo, nameservers, dat);
        } catch (RegistrarException e) {
            log("Unable to register domain [ok]");
        }
        log("Changing password for " + companyName + "z.com");
        instance.setCookie(companyName + 'z', "com", companyName + 'z', "fulltest");
        instance.changePassword("test-done", dat);
        Date finish = TimeUtils.getDate();
        System.out.println("started -->" + start);
        System.out.println("finished -->" + finish);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void checkLogin() throws RegistrarException {
        getConnection();
    }

    public Map encodeInfo(Map info) {
        Map result = new HashMap();
        for (String key : info.keySet()) {
            result.put(key, XMLEncoder.encode((String) info.get(key)));
        }
        return result;
    }
}
