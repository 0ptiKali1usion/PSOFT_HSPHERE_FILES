package psoft.hsphere.resource.miva;

import java.net.URL;
import java.util.Hashtable;
import psoft.hsphere.Session;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/Miva4WinOperator.class */
public class Miva4WinOperator extends AbstractMivaOperator {
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMivaEmpresa() throws Exception {
        Session.getLog().debug("Miva Empresa physicalCreate");
        String login = getMivaResource().recursiveGet("login").toString();
        String name = getMivaResource().recursiveGet("real_name").toString();
        String hostnum = getMivaResource().recursiveGet("hostnum").toString();
        Session.getLog().debug("MIVA: username=" + login + "name_domain=" + name);
        ((WinHostEntry) getHost()).exec("miva-empresainstall.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"user-name", login}, new String[]{"hostname", name}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMerchantBundle() throws Exception {
        String login = getMivaResource().recursiveGet("login").toString();
        String name = getMivaResource().recursiveGet("real_name").toString();
        Session.getLog().debug("MIVA:miva-merchantinstall.asp");
        ((WinHostEntry) getHost()).exec("miva-merchantinstall.asp", (String[][]) new String[]{new String[]{"hostnum", "0"}, new String[]{"user-name", login}, new String[]{"hostname", name}, new String[]{"data", getProvisioningData()}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void uninstallMivaEmpresa() throws Exception {
        Session.getLog().debug("Miva Empresa physicalDelete");
        String login = getMivaResource().recursiveGet("login").toString();
        String name = getMivaResource().recursiveGet("real_name").toString();
        String dir = getMivaResource().recursiveGet("local_dir").toString();
        Session.getLog().debug("MIVA: username=" + login + "name_domain=" + name);
        ((WinHostEntry) getHost()).exec("miva-empresaremove.asp", (String[][]) new String[]{new String[]{"hostnum", "0"}, new String[]{"user-name", login}, new String[]{"hostname", dir}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v6, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void uninstallMivaMerchant() throws Exception {
        Session.getLog().debug("Miva Merchant physicalDelete");
        String login = getMivaResource().recursiveGet("login").toString();
        String name = getMivaResource().recursiveGet("real_name").toString();
        String dir = getMivaResource().recursiveGet("local_dir").toString();
        Session.getLog().debug("MIVA: username=" + login + "name_domain=" + name);
        ((WinHostEntry) getHost()).exec("miva-merchantremove.asp", (String[][]) new String[]{new String[]{"hostnum", "0"}, new String[]{"user-name", login}, new String[]{"hostname", dir}});
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public Hashtable configureMerchant() throws Exception {
        return setupMivaMerchant();
    }

    private String getProvisioningData() throws Exception {
        String domainName = getMivaResource().recursiveGet("name").toString();
        String data = "MERCHANT|" + getMivaResource().get("lic").toString() + "|NO|http://" + domainName + "/Merchant2/merchant.mvc?|https://" + domainName + "/Merchant2/merchant.mvc?|https://" + domainName + "/Merchant2/admin.mvc?|/Merchant2/|http://" + domainName + "/Merchant2/|/Merchant2/|/Merchant2/|/Merchant2/|" + getMailServer() + "|YES|en-US|YES|YES|" + getOwnerName() + "|" + getContactEmail() + "|" + getCompany() + "|" + getAddress() + "|" + getCity() + "|" + getState() + "|" + getZip() + "|" + getCountry() + "|" + getPhone() + "||" + getAdmLogin() + "|" + getAdmPasswd() + "|YES|YES|YES|YES|L|CommerceLib|YES|YES|YES|YES";
        return data;
    }

    protected URL getSetupURL() throws Exception {
        return new URL("http://" + getMivaResource().recursiveGet("name").toString() + "/Merchant2/setup.mvc");
    }
}
