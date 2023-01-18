package psoft.hsphere.resource.registrar;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/Enom.class */
public class Enom extends AsyncLoggableRegistrar {
    protected String PROTOCOL;
    protected String SERVER;
    protected int PORT;
    protected String PATH;
    protected String balance;
    protected HashMap settings;
    private static final String[] SUPPORTED_TLD = {"com", "net", "org", "us", "info", "biz", "tv", "ws", "nu", "bz", "cc", "ca", "cn", "com.cn", "net.cn", "org.cn", "br.com", "cn.com", "de.com", "eu.com", "hu.com", "no.com", "qc.com", "ru.com", "sa.com", "se.com", "uk.com", "us.com", "uy.com", "za.com", "de", "in", "jp", "uk.net", "se.net", "tw", "co.uk", "org.uk"};
    private static final Map ASYNC_TLD = new HashMap();

    /* renamed from: sf */
    private static final SimpleDateFormat f209sf;

    static {
        ASYNC_TLD.put("co.uk", new AsyncTLD(1, 25));
        ASYNC_TLD.put("org.uk", new AsyncTLD(1, 25));
        f209sf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String getSignature() {
        return "Enom";
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

    @Override // psoft.hsphere.resource.registrar.AsyncLoggableRegistrar
    public boolean isRegComplete(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.GETDOMAINSTATUS, domain, tld);
        checkLogin();
        EnomResponse response = request.execute(dat);
        try {
            response.isSuccess();
        } catch (RegistrarException e) {
        }
        String resultCode = response.getResponseValue("InAccount", true);
        try {
            int result = Integer.parseInt(resultCode);
            switch (result) {
                case 0:
                    return false;
                case 1:
                    return true;
                case 2:
                    throw new RegistrarException(response.getCode(), "Domains belongs to somebody else's account.");
                default:
                    throw new RegistrarException(response.getCode(), "Unexpected registrar response" + response.getText(), response.getFullResponse());
            }
        } catch (NumberFormatException e2) {
            throw new RegistrarException(RegistrarException.MAILFORMED_RESPONSE, Localizer.translateMessage("registrar.unexpected_param_value", new String[]{"ErrCount", resultCode}), "");
        }
    }

    public Enom(Integer id, String description) throws Exception {
        super(id, description);
        this.PROTOCOL = get("protocol");
        this.SERVER = get("server");
        try {
            this.PORT = Integer.parseInt(get("port"));
        } catch (Exception e) {
            Session.getLog().error("Error parsing port " + get("port") + " assuming default Enom port 80 ");
            this.PORT = 80;
        }
        this.PATH = get("path");
        this.settings = new HashMap();
        this.settings.put("Uid", get("username"));
        this.settings.put("Pw", get("password"));
        this.settings.put("responsetype", "text");
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public String[] getSupportedTLDs() {
        return SUPPORTED_TLD;
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public boolean lookup(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.CHECK, domain, tld);
        checkLogin();
        EnomResponse response = request.execute(dat);
        try {
            response.isSuccess();
        } catch (RegistrarException e) {
        }
        switch (response.getCode()) {
            case 210:
                return true;
            case 211:
                return false;
            default:
                throw new RegistrarException(response.getCode(), "Unexpected registrar response" + response.getText(), response.getFullResponse());
        }
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        register(domain, tld, username, password, period, registrant, tech, admin, billing, dns, null, dat);
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void register(String domain, String tld, String username, String password, int period, Map registrant, Map tech, Map admin, Map billing, Collection dns, Map extra, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.PURCHASE, domain, tld);
        request.setPassword(password);
        request.setNumYears(period);
        request.setInfo("Registrant", registrant);
        if (extra != null) {
            request.setExtraInfo("Registrant", extra);
        }
        request.setInfo("Tech", tech);
        request.setInfo("Admin", admin);
        request.setInfo("AuxBilling", billing);
        request.setDNS(dns);
        request.execute(dat).isSuccess();
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void changeContacts(String domain, String tld, String username, String password, Map registrant, Map tech, Map admin, Map billing, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.CONTACTS, domain, tld);
        request.setInfo("Registrant", registrant);
        request.setInfo("Tech", tech);
        request.setInfo("Admin", admin);
        request.setInfo("AuxBilling", billing);
        request.execute(dat).isSuccess();
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void setPassword(String domain, String tld, String username, String password, String newPassword, RegistrarTransactionData dat) throws Exception {
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.SETPASSWORD, domain, tld);
        request.setPassword(newPassword);
        request.execute(dat).isSuccess();
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void renew(String domain, String tld, String currentExpYear, int period, Map registrant, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.EXTEND, domain, tld);
        request.setNumYears(period);
        request.setInfo("Registrant", registrant);
        request.execute(dat).isSuccess();
    }

    @Override // psoft.hsphere.resource.registrar.Registrar
    public void checkLogin() throws RegistrarException {
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.CHECKLOGIN, "", "");
        try {
            request.execute().isSuccess();
        } catch (RegistrarException ex) {
            Session.getLog().error("Eniom:checkLogin failed " + ex.getfullRegistrarResponse());
            if (ex.getCode() == -2006) {
                throw new RegistrarException(RegistrarException.AUTH_ERROR, Localizer.translateMessage("registrar.auth_fail"));
            }
            throw ex;
        } catch (Exception e) {
        }
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public void transfer(String domain, String tld, String username, String password, Map registrant, Map tech, Map admin, Map billing, Collection dns, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.TP_CREATEORDER, domain, tld);
        request.setPassword(password);
        request.setInfo("Registrant", registrant);
        request.setInfo("Tech", tech);
        request.setInfo("Admin", admin);
        request.setInfo("AuxBilling", billing);
        request.setDNS(dns);
        request.setValue("DomainCount", "1");
        request.setValue("OrderType", "Autoverification");
        request.setValue("Sld1", domain);
        request.setValue("Tld1", tld);
        request.execute(dat).isSuccess();
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public boolean isTransferable(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.TP_GETTLDINFO, domain, tld);
        request.setValue("QueryFlag", "2");
        EnomResponse resp = request.execute(dat);
        resp.isSuccess();
        HashMap response = resp.response;
        int i = 1;
        boolean result = false;
        while (true) {
            String val = (String) response.get("TLD" + Integer.toString(i));
            if (val == null) {
                break;
            } else if (val.equals(tld)) {
                result = true;
                break;
            } else {
                i++;
            }
        }
        return result;
    }

    @Override // psoft.hsphere.resource.registrar.LoggableRegistrar
    public Date isTransfered(String domain, String tld, RegistrarTransactionData dat) throws Exception {
        checkLogin();
        EnomResponse response = new EnomRequest(this, this.settings, EnomRequest.TP_GETDETAILSBYDOMAIN, domain, tld).execute(dat);
        response.isSuccess();
        int count = Integer.parseInt(response.getResponseValue("ordercount", true));
        String status = "";
        int orderNum = 1;
        while (orderNum <= count) {
            status = response.getResponseValue("statusid" + orderNum, true);
            if ("5".equals(status)) {
                break;
            }
            orderNum++;
        }
        if (!"5".equals(status)) {
            return null;
        }
        String orderId = response.getResponseValue("orderid" + orderNum, true);
        EnomRequest request = new EnomRequest(this, this.settings, EnomRequest.GETDOMAINSTATUS, domain, tld);
        request.setValue("OrderID", orderId);
        request.setValue("OrderType", " Transfer");
        EnomResponse response2 = request.execute(dat);
        response2.isSuccess();
        String expDate = response2.getResponseValue("ExpDate", true);
        return f209sf.parse(expDate);
    }
}
