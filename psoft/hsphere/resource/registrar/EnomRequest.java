package psoft.hsphere.resource.registrar;

import java.net.HttpURLConnection;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Localizer;
import psoft.hsphere.Session;
import psoft.util.HttpUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/EnomRequest.class */
public class EnomRequest {
    public static final String CHECK = "CHECK";
    public static final String CHECKLOGIN = "CHECKLOGIN";
    public static final String PURCHASE = "PURCHASE";
    public static final String CONTACTS = "CONTACTS";
    public static final String SETPASSWORD = "SETPASSWORD";
    public static final String EXTEND = "EXTEND";
    public static final String GETDOMAINSTATUS = "GETDOMAINSTATUS";
    public static final String TP_CREATEORDER = "TP_CREATEORDER";
    public static final String TP_GETDETAILSBYDOMAIN = "TP_GETDETAILSBYDOMAIN";
    public static final String TP_GETTLDINFO = "TP_GETTLDINFO";
    public static final String GETDOMAININFO = "GETDOMAININFO";
    protected HashMap request;
    protected Enom enom;

    public EnomRequest(Enom enom, HashMap settings, String command, String domain, String tld) {
        this.enom = enom;
        this.request = (HashMap) settings.clone();
        this.request.put("Command", command);
        this.request.put("Sld", domain);
        this.request.put("Tld", tld);
    }

    public void setDNS(Collection dns) {
        int count = 1;
        for (Object obj : dns) {
            this.request.put("NS" + count, obj);
            count++;
        }
    }

    public void setPassword(String password) {
        this.request.put("DomainPassword", password);
    }

    public void setNumYears(int years) {
        this.request.put("NumYears", Integer.toString(years));
    }

    public void setInfo(String pref, Map info) {
        Session.getLog().debug("Inside setInfo\n first_name=" + info.get("first_name") + "\nlast_name=" + info.get("last_name"));
        setValue(pref + "FirstName", info.get("first_name"));
        setValue(pref + "LastName", info.get("last_name"));
        setValue(pref + "Address1", info.get("address1"));
        setValue(pref + "Address2", info.get("address2"));
        setValue(pref + "City", info.get("city"));
        setValue(pref + "Country", info.get("country"));
        setValue(pref + "EmailAddress", info.get("email"));
        setValue(pref + "Fax", info.get("fax"));
        setValue(pref + "JobTitle", info.get("title"));
        setValue(pref + "OrganizationName", info.get("org_name"));
        setValue(pref + "Phone", info.get("phone"));
        setValue(pref + "PostalCode", info.get("postal_code"));
        setValue(pref + "StateProvince", info.get("state"));
        setValue(pref + "StateProvinceChoice", info.get("stateChoice"));
    }

    public void setExtraInfo(String pref, Map extra) throws Exception {
        if (extra != null) {
            if ("us".equals(extra.get("tld")) || "kids.us".equals(extra.get("tld"))) {
                String nexus = (String) extra.get("nexus_category");
                String validator = (String) extra.get("nexus_validator");
                if (("C31".equals(nexus) || "C32".equals(nexus)) && validator != null) {
                    nexus = nexus + "/" + validator;
                }
                setValue(pref + "Nexus", nexus);
                setValue(pref + "Purpose", (String) extra.get("app_purpose"));
            } else if ("ca".equals(extra.get("tld"))) {
                String legalType = (String) extra.get("legal_type");
                if (legalType != null) {
                    setValue("cira_legal_type", legalType);
                }
            } else if ("de".equals(extra.get("tld"))) {
                String confirmaddress = (String) extra.get("confirmaddress");
                if (confirmaddress != null && !"".equals(confirmaddress)) {
                    setValue("confirmaddress", confirmaddress);
                }
            } else if ("co.uk".equals(extra.get("tld")) || "org.uk".equals(extra.get("tld"))) {
                String ukLegalType = (String) extra.get("uk_legal_type");
                String ukRegCoNo = (String) extra.get("uk_reg_co_no");
                String registeredFor = (String) extra.get("registered_for");
                Session.getLog().debug("uk_legal_type= " + ukLegalType);
                Session.getLog().debug("uk_reg_co_no= " + ukRegCoNo);
                Session.getLog().debug("registered_for= " + registeredFor);
                if (ukLegalType != null && !"".equals(ukLegalType)) {
                    setValue("uk_legal_type", ukLegalType);
                }
                if (ukRegCoNo != null && !"".equals(ukRegCoNo)) {
                    setValue("uk_reg_co_no", ukRegCoNo);
                }
                if (registeredFor != null && !"".equals(registeredFor)) {
                    setValue("registered_for", registeredFor);
                }
            }
        }
    }

    public void setValue(String key, Object value) {
        if (value != null) {
            this.request.put(key, value);
        } else {
            Session.getLog().debug("Setting Enom Parameters: There is no value for the key '" + key + "'.");
        }
    }

    public EnomResponse execute() throws RegistrarException {
        return execute(new RegistrarTransactionData());
    }

    public EnomResponse execute(RegistrarTransactionData data) throws RegistrarException {
        data.setRequest(this.request.toString());
        if ("TRUE".equals(Session.getPropertyString("REGISTRAR_DEBUG_MODE"))) {
            StringBuffer requestText = new StringBuffer();
            requestText.append("\n\t---BEGIN OF ENOM REQUEST---");
            for (String key : this.request.keySet()) {
                requestText.append("\n" + key + " = " + this.request.get(key).toString());
            }
            requestText.append("\n\t---END OF ENOM REQUEST---");
            Session.getLog().debug(requestText.toString());
        }
        if ("TRUE".equals(Session.getPropertyString("\n\t---ENOM_SLIENT_MODE---"))) {
            return new EnomResponse(this.request);
        }
        try {
            HttpURLConnection con = HttpUtils.getRequest(this.enom.PROTOCOL, this.enom.SERVER, this.enom.PORT, this.enom.PATH, this.request);
            int rcode = con.getResponseCode();
            Session.getLog().debug("URL connection opened HTTP code " + rcode);
            EnomResponse resp = new EnomResponse(con);
            data.setResponse(resp.response.toString());
            return resp;
        } catch (UnknownHostException e) {
            throw new RegistrarException(RegistrarException.UNABLE_TO_CONNECT, Localizer.translateMessage("registrar.unknown_server", new String[]{this.enom.SERVER}));
        } catch (Exception e2) {
            throw new RegistrarException(RegistrarException.UNABLE_TO_CONNECT, Localizer.translateMessage("registrar.ioerror_contact", new String[]{this.enom.PROTOCOL + this.enom.SERVER + this.enom.PORT + this.enom.PATH}));
        }
    }
}
