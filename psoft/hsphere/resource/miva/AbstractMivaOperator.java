package psoft.hsphere.resource.miva;

import freemarker.template.TemplateModelException;
import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.email.MailService;
import psoft.hsphere.resource.epayment.ContactInfoObject;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/AbstractMivaOperator.class */
public abstract class AbstractMivaOperator implements MivaOperator {

    /* renamed from: he */
    protected HostEntry f196he = null;

    /* renamed from: ci */
    private ContactInfoObject f197ci;
    protected Resource mivaRes;

    public void setResource(Resource _r) {
        this.mivaRes = _r;
        this.f197ci = _r.getAccount().getContactInfo();
    }

    protected String getDBPassword() {
        return "";
    }

    protected String getDBUsername() {
        return "";
    }

    public String getPhone() {
        return getCIAttribute("phone");
    }

    public String getZip() {
        return getCIAttribute("postal_code");
    }

    public String getState() {
        return getCIAttribute("state");
    }

    public String getCity() {
        return getCIAttribute("city");
    }

    public String getAddress() {
        return getCIAttribute("address1");
    }

    public String getCompany() {
        String res = getCIAttribute("company");
        if (res == null || res.length() == 0) {
            return getOwnerName();
        }
        return res;
    }

    public String getContactEmail() {
        return getCIAttribute("email");
    }

    public String getOwnerName() {
        return getCIAttribute("first_name") + " " + getCIAttribute("last_name");
    }

    public String getMailServer() throws Exception {
        ResourceId mailId = getMivaResource().recursiveGet("mail_service");
        if (mailId != null) {
            MailService ms = (MailService) mailId.get();
            return ms.get("mail_server_name").toString();
        }
        HostEntry mailServer = HostManager.getRandomHost(3);
        return mailServer.getPFirstIP();
    }

    public String getCountry() {
        return getCIAttribute("country");
    }

    public String getAdmLogin() throws Exception {
        return getMivaResource().getAccount().getUser().get("login").toString();
    }

    public String getAdmPasswd() throws Exception {
        return getMivaResource().getAccount().getUser().get("password").toString();
    }

    private String getCIAttribute(String att) {
        if (this.f197ci != null) {
            try {
                return this.f197ci.get(att).toString();
            } catch (TemplateModelException ex) {
                Session.getLog().error(ex);
                return "";
            }
        }
        return "";
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void setMivaResource(Resource r) throws Exception {
        this.mivaRes = r;
        this.f197ci = this.mivaRes.getAccount().getContactInfo();
    }

    public Resource getMivaResource() {
        return this.mivaRes;
    }

    public HostEntry getHost() {
        return this.f196he;
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void setHost(HostEntry _he) {
        this.f196he = _he;
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantSetupURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/Merchant2/setup.mvc";
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantAdminURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/Merchant2/admin.mvc";
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/Merchant2/merchant.mvc";
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantUninstallURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/Merchant2/remove.mvc";
    }

    public Hashtable setupMivaMerchant() throws Exception {
        Hashtable result = new Hashtable();
        Session.getLog().debug("Setting up Win Miva Merchant 4");
        try {
            URL setupURL = new URL(getMivaMerchantSetupURL());
            URLConnection connection = setupURL.openConnection();
            BufferedReader setupData = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            Session.getLog().debug("Got input stream");
            StringBuffer response = new StringBuffer();
            RE isCompleted = new RE("^\\s*<mmsetup\\s+completed\\s*=\\s*\"\\s*([^\"]*)\\s*");
            while (true) {
                String line = setupData.readLine();
                if (line != null) {
                    String line2 = line.trim();
                    response.append(line2).append('\n');
                    Session.getLog().debug("Got next line:" + line2);
                    if (line2.length() > 0) {
                        Session.getLog().debug("setting up miva merchant, matching line from response");
                        if (isCompleted.getMatch(line2.toLowerCase()) != null) {
                            String strRes = isCompleted.getMatch(line2.toLowerCase()).toString(1);
                            if ("yes".equalsIgnoreCase(strRes)) {
                                result.put("status", "OK");
                                return result;
                            } else if ("no".equalsIgnoreCase(strRes)) {
                                RE error = new RE("^\\s*<mmsetup\\s+.*error\\s*=\\s*\"\\s*([^\"]*)\\s*");
                                REMatch errorMatch = error.getMatch(line2.toLowerCase());
                                String errorMessage = errorMatch.toString(1);
                                result.put("status", "FAIL");
                                result.put("message", errorMessage);
                                return result;
                            }
                        } else {
                            continue;
                        }
                    }
                } else {
                    setupData.close();
                    result.put("status", "FAIL");
                    result.put("message", response.toString());
                    return result;
                }
            }
        } catch (Exception ex) {
            Session.getLog().error("Error configuring miva merchant:", ex);
            throw ex;
        }
    }
}
