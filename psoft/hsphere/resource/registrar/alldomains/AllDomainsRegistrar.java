package psoft.hsphere.resource.registrar.alldomains;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.manager.Entity;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.util.HttpResponse;
import psoft.util.HttpUtils;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/alldomains/AllDomainsRegistrar.class */
public class AllDomainsRegistrar extends Entity implements Registrar {
    public static final String[] SUPPORTED_TLD = {"com", "net", "org"};
    protected String PROTOCOL;
    protected String SERVER;
    protected int PORT;
    protected String REGISTER_PATH;
    protected String LOOKUP_PATH;
    protected String MANAGE_PATH;
    protected String RENEW_PATH;
    protected String CPUSER;
    protected String CPPWD;
    protected String PREFIX;

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String[] getSupportedTLDs() {
        return SUPPORTED_TLD;
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String getSignature() {
        return "AllDomains.com";
    }

    public AllDomainsRegistrar(Integer id, String description) throws Exception {
        super(id, description);
        this.PROTOCOL = "http";
        this.SERVER = "regdev.alldomains.com";
        this.PORT = 80;
        this.REGISTER_PATH = "/cp/cp.ald";
        this.LOOKUP_PATH = "/cp/ml_whois.ald";
        this.MANAGE_PATH = "/cp/cp_manage.ald";
        this.RENEW_PATH = "/cp/cp_renewal.ald";
        this.CPUSER = "";
        this.CPPWD = "";
        this.PREFIX = "852_";
        this.PROTOCOL = get("protocol");
        this.PORT = Integer.parseInt(get("port"));
        this.SERVER = get("server");
        if (this.SERVER.equalsIgnoreCase("reg.alldomains.com")) {
            this.REGISTER_PATH = "/cp/cp_curl.ald";
        }
        this.CPUSER = get("cpuser");
        this.CPPWD = get("pwd");
        this.PREFIX = get("prefix");
        if (this.PREFIX == null || this.PREFIX.length() == 0) {
            this.PREFIX = "852_";
        }
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void transfer(String domain, String tld, String login, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns) throws Exception {
        throw new Exception("NOT IMPLEMENTED");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public Date isTransfered(String name, String tld) throws Exception {
        throw new Exception("NOT IMPLEMENTED");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public boolean lookup(String domain, String tld) throws Exception {
        Map map = new HashMap();
        map.put("encodetype1", "");
        map.put("dn1enc", "0");
        map.put("dn1", domain + "." + tld);
        HttpResponse res = HttpUtils.getForm(this.PROTOCOL, this.SERVER, this.PORT, this.LOOKUP_PATH, map);
        String body = res.getBody();
        return body.startsWith("AVAILABLE");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void register(String domain, String tld, String login, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns) throws Exception {
        register(domain, tld, login, password, period, registrant, tech, admin, billing, dns, null);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, Map extraInfo) throws Exception {
        new HashMap();
        Map map = new HashMap();
        map.put("cpuser", this.CPUSER);
        map.put("cppwd", this.CPPWD);
        map.put(FMACLManager.USER, this.PREFIX + username);
        map.put("pwd", password);
        map.put("dn1", domain + "." + tld);
        map.put("encodetype", "");
        map.put("dnterm1", Integer.toString(period));
        copyMap(map, "R_", tech);
        copyMap(map, "O_", registrant);
        copyMap(map, "A_", billing);
        copyMap(map, "B_", billing);
        if (extraInfo != null) {
            map.put("uspurpose", extraInfo.get("usppurpose"));
            map.put("uscateg", extraInfo.get("uscateg"));
        }
        HttpResponse res = HttpUtils.getForm(this.PROTOCOL, this.SERVER, this.PORT, this.REGISTER_PATH, map);
        String body = res.getBody();
        if (body == null || !body.startsWith("REG(")) {
            throwException(body);
        }
    }

    protected void throwException(String error) throws RegistrarException {
        if (error == null || error.length() == 0) {
            throw new RegistrarException(-1001, "No from registrar");
        }
        if (!error.startsWith("ERROR(")) {
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, error);
        }
        throw new RegistrarException(RegistrarException.OTHER_ERROR, error);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void changeContacts(String domain, String tld, String username, String password, Map registrant, Map tech, Map admin, Map billing) throws Exception {
        Map map = new HashMap();
        map.put("cpuser", this.CPUSER);
        map.put("cppwd", this.CPPWD);
        map.put(FMACLManager.USER, this.PREFIX + username);
        map.put("pwd", password);
        map.put("Domain", domain + "." + tld);
        copyMap(map, "O_", registrant);
        copyMap(map, "A_", billing);
        copyMap(map, "B_", billing);
        HttpResponse res = HttpUtils.getForm(this.PROTOCOL, this.SERVER, this.PORT, this.MANAGE_PATH, map);
        String body = res.getBody();
        if (body.indexOf("(ADMIN_UPDATE_SUCESS)") != 0 && body.indexOf("(BILL_UPDATE_SUCESS)") != 0 && body.indexOf("(ORG_UPDATE_SUCESS)") != 0) {
            return;
        }
        throw new RegistrarException(RegistrarException.OTHER_ERROR, body);
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void setPassword(String domain, String tld, String username, String password, String newPassword) throws Exception {
        throw new Exception("set password is not supported for AllDomains.com");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void renew(String domain, String tld, String currentExpYear, int period, Map registrant) throws Exception {
        Map map = new HashMap();
        map.put("cpuser", this.CPUSER);
        map.put("cppwd", this.CPPWD);
        map.put(FMACLManager.USER, this.PREFIX + "");
        map.put("pwd", "");
        map.put("dn1", domain + "." + tld);
        map.put("renewterm1", Integer.toString(1));
        HttpResponse res = HttpUtils.getForm(this.PROTOCOL, this.SERVER, this.PORT, this.RENEW_PATH, map);
        res.getBody();
        throw new Exception("renew is not supported for AllDomains.com");
    }

    protected void copyMap(Map map, String prefix, Map info) {
        map.put(prefix + "FirstName", info.get("first_name"));
        map.put(prefix + "LastName", info.get("last_name"));
        map.put(prefix + "Phone", info.get("phone"));
        map.put(prefix + "Email", info.get("email"));
        map.put(prefix + "Organization", info.get("org_name"));
        map.put(prefix + "Address1", info.get("address1"));
        map.put(prefix + "City", info.get("city"));
        map.put(prefix + "State", info.get("state"));
        map.put(prefix + "Zip", info.get("postal_code"));
        map.put(prefix + "Country", info.get("country"));
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void checkLogin() throws RegistrarException {
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public boolean isTransferable(String name, String tld) throws Exception {
        throw new Exception("Not Implemented");
    }
}
