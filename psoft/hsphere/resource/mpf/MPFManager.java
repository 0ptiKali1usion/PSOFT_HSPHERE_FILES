package psoft.hsphere.resource.mpf;

import com.microsoft.provisioning.webservices.ExchangeResourceManagerLocator;
import com.microsoft.provisioning.webservices.ExchangeResourceManagerSoap;
import com.microsoft.provisioning.webservices.ExchangeResourceManagerSoapEmulator;
import com.microsoft.provisioning.webservices.HostedExchangeLocator;
import com.microsoft.provisioning.webservices.HostedExchangeSoap;
import com.microsoft.provisioning.webservices.HostedExchangeSoapEmulator;
import com.microsoft.provisioning.webservices.MPSGeneralLocator;
import com.microsoft.provisioning.webservices.MPSGeneralSoap;
import com.microsoft.provisioning.webservices.MPSGeneralSoapEmulator;
import com.microsoft.provisioning.webservices.ManagedActiveDirectoryLocator;
import com.microsoft.provisioning.webservices.ManagedActiveDirectorySoap;
import com.microsoft.provisioning.webservices.ManagedActiveDirectorySoapEmulator;
import com.microsoft.provisioning.webservices.SubmitMpsRequestDataXmlBlob;
import com.microsoft.provisioning.webservices.holders.SubmitMpsRequestResponseSubmitMpsRequestResultHolder;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.rpc.holders.StringHolder;
import org.apache.axis.message.MessageElement;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.mpf.hostedexchange.HePlanSettings;
import psoft.hsphere.util.XMLManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/MPFManager.class */
public class MPFManager {
    private static final String PDC_KEY = "MPF_HE_PDC";
    private static final String LDAP_KEY = "MPF_HE_LDAP";
    private static final String URL_KEY = "MPF_HE_URL";
    private static final String HE_SERVICE = "/HostedExchange.asmx";
    private static final String MPS_SERVICE = "/MPSGeneral.asmx";
    private static final String ERM_SERVICE = "/ExchangeResourceManager.asmx";
    private static final String MAD_SERVICE = "/ManagedActiveDirectory.asmx";
    private static HashMap owaSegmentProp;
    String pdc;
    String ldapString;
    static DocumentBuilder dbuilder;
    HostedExchangeSoap hes;
    MPSGeneralSoap mps;
    ExchangeResourceManagerSoap erms;
    ManagedActiveDirectorySoap mads;
    HashMap availablePlans = new HashMap();
    boolean configured;
    private static final MPFManager manager = new MPFManager();
    static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    static {
        try {
            dbuilder = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            Session.getLog().error("Problem initializing DocumentBuilder", e);
            e.printStackTrace();
        }
    }

    private MPFManager() {
        init();
    }

    public void init() {
        this.pdc = Settings.get().getAdminGlobalValue(PDC_KEY);
        this.ldapString = Settings.get().getAdminGlobalValue(LDAP_KEY);
        try {
            this.hes = new HostedExchangeLocator(Settings.get().getAdminGlobalValue(URL_KEY) + HE_SERVICE).getHostedExchangeSoap();
            this.mps = new MPSGeneralLocator(Settings.get().getAdminGlobalValue(URL_KEY) + MPS_SERVICE).getMPSGeneralSoap();
            this.erms = new ExchangeResourceManagerLocator(Settings.get().getAdminGlobalValue(URL_KEY) + ERM_SERVICE).getExchangeResourceManagerSoap();
            this.mads = new ManagedActiveDirectoryLocator(Settings.get().getAdminGlobalValue(URL_KEY) + MAD_SERVICE).getManagedActiveDirectorySoap();
            Session.getLog().debug("Available plans : " + getAvailablePlans().toString());
            this.configured = true;
        } catch (Exception e) {
            Session.getLog().error("problem initializing MPFManager: " + e.getMessage());
            this.configured = false;
        }
    }

    public static MPFManager getManager() {
        return manager;
    }

    public String getPdc() {
        return this.pdc;
    }

    public HostedExchangeSoap getHES() {
        if (HostEntry.getEmulationMode()) {
            return HostedExchangeSoapEmulator.EMULATOR;
        }
        return this.hes;
    }

    public ExchangeResourceManagerSoap getERMS() {
        if (HostEntry.getEmulationMode()) {
            return ExchangeResourceManagerSoapEmulator.EMULATOR;
        }
        return this.erms;
    }

    public MPSGeneralSoap getMPS() {
        if (HostEntry.getEmulationMode()) {
            return MPSGeneralSoapEmulator.EMULATOR;
        }
        return this.mps;
    }

    public ManagedActiveDirectorySoap getMADS() {
        if (HostEntry.getEmulationMode()) {
            return ManagedActiveDirectorySoapEmulator.EMULATOR;
        }
        return this.mads;
    }

    public String getContainer() {
        return "LDAP://" + this.ldapString;
    }

    public String getLDAP(String orgName) {
        return "LDAP://OU=" + orgName + "," + this.ldapString;
    }

    public String getLDAP(String orgName, String principalName) {
        return "LDAP://CN=" + principalName + ",OU=" + orgName + "," + this.ldapString;
    }

    public void setPDC(String pdc) throws Exception {
        Settings.get().setValue("_GLB_MPF_HE_PDC", pdc);
    }

    public void setLdapString(String ldapString) throws Exception {
        Settings.get().setValue("_GLB_MPF_HE_LDAP", ldapString);
    }

    public void setURL(String url) throws Exception {
        Settings.get().setValue("_GLB_MPF_HE_URL", url);
    }

    public Result processHeResult(String result) throws com.microsoft.provisioning.MPFException {
        boolean success = false;
        String error = "";
        String errorCode = "";
        Document doc = null;
        if (HostEntry.getEmulationMode()) {
            return new Result(true, "emulation mode", "");
        }
        try {
            doc = dbuilder.parse(new ByteArrayInputStream((com.psoft.mpf.Tag.xmlStart + result).getBytes(LanguageManager.STANDARD_CHARSET)));
            Node n = XPathAPI.selectSingleNode(doc, "response/errorContext");
            if (n == null) {
                success = true;
            } else {
                NamedNodeMap attr = n.getAttributes();
                String error2 = n.getAttributes().getNamedItem("description").getNodeValue();
                errorCode = attr.getNamedItem("code").getNodeValue();
                error = (error2 + " code: " + errorCode) + "executeSeqNo" + Integer.parseInt(attr.getNamedItem("executeSeqNo").getNodeValue());
            }
        } catch (Exception ex) {
            error = "Unable to parse HE response:" + ex.getMessage();
            Session.getLog().error("Unable to parse HE response:" + result, ex);
        }
        return new Result(success, doc, error, errorCode);
    }

    public String getLdapString() {
        return this.ldapString;
    }

    public String getURL() {
        return Settings.get().getAdminGlobalValue(URL_KEY);
    }

    public Result managedExchangeMailEnableGroup(String ldap, String cn, String proxy) throws Exception {
        MessageElement data = new MessageElement("", "data");
        MessageElement tmp = new MessageElement("", "path");
        tmp.addTextNode(ldap);
        data.addChild(tmp);
        MessageElement tmp2 = new MessageElement("", "cn");
        tmp2.addTextNode(cn);
        data.addChild(tmp2);
        MessageElement tmp3 = new MessageElement("", "proxyAddress");
        tmp3.addTextNode(proxy);
        data.addChild(tmp3);
        MessageElement tmp4 = new MessageElement("", "preferredDomainController");
        tmp4.addTextNode(getPdc());
        data.addChild(tmp4);
        return executeMPSRequest("Managed Exchange", "MailEnableGroup", data);
    }

    public Result executeMPSRequest(String namespace, String provider, MessageElement data) {
        String error;
        StringHolder errorHolder = new StringHolder();
        SubmitMpsRequestResponseSubmitMpsRequestResultHolder response = new SubmitMpsRequestResponseSubmitMpsRequestResultHolder();
        MessageElement[] responseXML = null;
        boolean success = false;
        try {
            SubmitMpsRequestDataXmlBlob dataXmlBlob = new SubmitMpsRequestDataXmlBlob();
            dataXmlBlob.set_any(new MessageElement[]{data});
            Session.getLog().debug("MPS Request: " + data);
            this.mps.submitMpsRequest(namespace, provider, dataXmlBlob, true, response, errorHolder);
            error = errorHolder.value;
            if (error == null || "".equals(error)) {
                responseXML = response.value.get_any();
                success = true;
            }
        } catch (Exception ex) {
            error = ex.getMessage();
            Session.getLog().error("Error running MPS request:", ex);
        }
        Session.getLog().debug("MPS result: " + responseXML + "\n Result: " + (success ? "success" : "error"));
        return new Result(success, responseXML, error);
    }

    public HashMap getAvailablePlans() {
        try {
            if (this.availablePlans.isEmpty()) {
                Document doc = XMLManager.getXML("HOSTED_EXCHANGE_PLANS");
                NodeList plans = XPathAPI.selectNodeList(doc, "/plans/plan");
                for (int i = 0; i < plans.getLength(); i++) {
                    new HashMap();
                    Node plan = plans.item(i);
                    String type = plan.getAttributes().getNamedItem("type").getNodeValue();
                    this.availablePlans.put(type, new HePlanSettings(plan));
                }
            }
        } catch (Exception ex) {
            Session.getLog().error("Problem getting available Hosted Exchange plans", ex);
        }
        return this.availablePlans;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/MPFManager$Result.class */
    public class Result {
        private boolean isSuccess;
        private Object response;
        private String errorMessage;
        private String errorCode;

        public Result(boolean isSuccess, Object response, String errorMessage) {
            MPFManager.this = r4;
            this.isSuccess = isSuccess;
            this.response = response;
            this.errorMessage = errorMessage;
            this.errorCode = "";
        }

        public Result(boolean isSuccess, Object response, String errorMessage, String errorCode) {
            MPFManager.this = r4;
            this.isSuccess = isSuccess;
            this.response = response;
            this.errorMessage = errorMessage;
            this.errorCode = errorCode;
        }

        public boolean isSuccess() {
            return this.isSuccess;
        }

        public Document getDocument() {
            return (Document) this.response;
        }

        public MessageElement[] getMessageElements() {
            return (MessageElement[]) this.response;
        }

        public String getError() {
            return this.errorMessage;
        }

        public String getErrorCode() {
            return this.errorCode;
        }
    }
}
