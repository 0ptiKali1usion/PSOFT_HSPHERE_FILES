package psoft.hsphere.resource.miva;

import org.apache.xerces.dom.DocumentImpl;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/Miva5Operator.class */
public abstract class Miva5Operator extends AbstractMivaOperator {

    /* renamed from: di */
    protected Document f198di = new DocumentImpl();

    public Element getConfiguration() throws Exception {
        Element res = this.f198di.createElement("Configuration");
        res.appendChild(createNode("DomainName", getMivaResource().recursiveGet("name").toString()));
        res.appendChild(createNode("LicenseNumber", getMivaResource().get("lic").toString()));
        res.appendChild(createNode("UserName", getAdmLogin()));
        res.appendChild(createNode("UserPassword", getAdmPasswd()));
        res.appendChild(createNode("VerifyIP", "Yes"));
        res.appendChild(createNode("XMLResponse", "Yes"));
        res.appendChild(createNode("MivaMerchantURL", "http://" + getMerchantURL()));
        res.appendChild(createNode("MivaMerchantSecureURL", "https://" + getMerchantURL()));
        res.appendChild(createNode("MivaAdminURL", "http://" + getAdminURL()));
        res.appendChild(createNode("MivaAdminSecureURL", "https://" + getAdminURL()));
        res.appendChild(createNode("MivaImageRoot", "/mm5/"));
        res.appendChild(createNode("MivaBaseURL", "http://" + getBaseURL() + "/mm5/"));
        res.appendChild(createNode("MivaBaseSecureURL", "https://" + getBaseURL() + "/mm5/"));
        res.appendChild(createNode("MivaModuleRoot", "/mm5/"));
        res.appendChild(createNode("MivaModuleRootSecure", "/mm5/"));
        res.appendChild(createNode("MailHost", getMailServer()));
        res.appendChild(createNode("MailAddAngleBrackets", "Yes"));
        res.appendChild(createNode("DisplaySiteSteps", "No"));
        res.appendChild(createNode("DisplaySetupSteps", "No"));
        res.appendChild(createNode("DisplayDBStep", "No"));
        res.appendChild(createNode("Owner", getOwnerName()));
        res.appendChild(createNode("Email", getContactEmail()));
        res.appendChild(createNode("Company", getCompany()));
        res.appendChild(createNode("Address", getAddress()));
        res.appendChild(createNode("City", getCity()));
        res.appendChild(createNode("State", getState()));
        res.appendChild(createNode("Zip", getZip()));
        res.appendChild(createNode("Country", getCountry()));
        res.appendChild(createNode("Phone", getPhone()));
        res.appendChild(createNode("Database_Type", getDatabaseType()));
        res.appendChild(createNode("Database_DB", getDatabaseName()));
        return res;
    }

    private Node createNode(String nodeName, String nodeValue) {
        Element l = this.f198di.createElement(nodeName);
        l.appendChild(this.f198di.createTextNode(nodeValue));
        return l;
    }

    protected String getDatabaseName() {
        return "mivamerchant.dbf";
    }

    protected String getDatabaseType() {
        return "mivasql";
    }

    protected String getBaseURL() throws Exception {
        return getMivaResource().recursiveGet("name").toString();
    }

    protected String getAdminURL() throws Exception {
        String dName = getMivaResource().recursiveGet("name").toString();
        return dName + "/mm5/admin.mvc?";
    }

    protected String getMerchantURL() throws Exception {
        String dName = getMivaResource().recursiveGet("name").toString();
        return dName + "/mm5/merchant.mvc?";
    }

    protected Document getDoc() {
        return this.f198di;
    }

    protected void setDoc(Document d) {
        this.f198di = d;
    }

    @Override // psoft.hsphere.resource.miva.AbstractMivaOperator, psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantSetupURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/mm5/setup.mvc";
    }

    @Override // psoft.hsphere.resource.miva.AbstractMivaOperator, psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantAdminURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/mm5/admin.mvc";
    }

    @Override // psoft.hsphere.resource.miva.AbstractMivaOperator, psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/mm5/merchant.mvc";
    }

    @Override // psoft.hsphere.resource.miva.AbstractMivaOperator, psoft.hsphere.resource.miva.MivaOperator
    public String getMivaMerchantUninstallURL() throws Exception {
        return "http://" + getMivaResource().recursiveGet("name").toString() + "/mm5/remove.mvc";
    }
}
