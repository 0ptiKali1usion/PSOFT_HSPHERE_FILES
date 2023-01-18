package psoft.hsphere.resource.miva;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/Miva4UnixOperator.class */
public class Miva4UnixOperator extends AbstractMivaOperator {
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMivaEmpresa() throws Exception {
        List list = new ArrayList();
        list.add(getMivaResource().recursiveGet("login").toString());
        list.add(getMivaResource().recursiveGet("group").toString());
        list.add(getMivaResource().recursiveGet("path").toString());
        Collection result = getHost().exec("miva-empresa-install", list);
        if (result.size() > 0) {
            String res = (String) result.iterator().next();
            if (res.startsWith("FAILURE")) {
                throw new HSUserException("miva.installation_missed");
            }
        }
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMerchantBundle() throws Exception {
        String login = getMivaResource().recursiveGet("login").toString();
        String group = getMivaResource().recursiveGet("group").toString();
        String dir = getMivaResource().recursiveGet("path").toString();
        Session.getLog().debug("MIVA: username=" + login + ",dir=" + dir);
        List list = new ArrayList();
        list.add(login);
        list.add(group);
        list.add(dir);
        Collection result = getHost().exec("miva-merchant-install", list);
        if (result.size() > 0) {
            String res = (String) result.iterator().next();
            if (res.startsWith("FAILURE")) {
                throw new HSUserException("miva.installation_missed");
            }
        }
    }

    public void createProvisioningData() throws Exception {
        String dir = getMivaResource().recursiveGet("dir").toString() + "/mivadata/" + getMivaResource().recursiveGet("name").toString();
        String domain = this.mivaRes.recursiveGet("name").toString();
        List list = new ArrayList();
        list.add("--path=\"" + dir + '\"');
        list.add("--domain=\"" + domain + '\"');
        list.add("--license=\"" + getMivaResource().get("lic").toString() + '\"');
        list.add("--mail_server=\"" + getMailServer() + '\"');
        list.add("--name=\"" + getOwnerName() + '\"');
        list.add("--email=\"" + getContactEmail() + '\"');
        list.add("--company=\"" + getCompany() + '\"');
        list.add("--address=\"" + getAddress() + '\"');
        list.add("--city=\"" + getCity() + '\"');
        list.add("--state=\"" + getState() + '\"');
        list.add("--zip=\"" + getZip() + '\"');
        list.add("--country=\"" + getCountry() + '\"');
        list.add("--phone=\"" + getPhone() + '\"');
        list.add("--login=\"" + getAdmLogin() + '\"');
        list.add("--password=\"" + getAdmPasswd() + '\"');
        if (0 != 0) {
            list.add("--xml=NO");
        } else {
            list.add("--xml=YES");
        }
        this.f196he.exec("write_sitedat", list);
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public Hashtable configureMerchant() throws Exception {
        createProvisioningData();
        return setupMivaMerchant();
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void uninstallMivaEmpresa() throws Exception {
        Session.getLog().debug("Miva Empressa physicalDelete");
        String login = getMivaResource().recursiveGet("login").toString();
        String dir = getMivaResource().recursiveGet("path").toString();
        Session.getLog().debug("MIVA: username=" + login + ",dir=" + dir);
        List list = new ArrayList();
        list.add(login);
        list.add(dir);
        getHost().exec("miva-empresa-uninstall", list);
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void uninstallMivaMerchant() throws Exception {
        String login = getMivaResource().recursiveGet("login").toString();
        String dir = getMivaResource().recursiveGet("path").toString();
        Session.getLog().debug("MIVA: username=" + login + ",dir=" + dir);
        List list = new ArrayList();
        list.add(login);
        list.add(dir);
        getHost().exec("miva-merchant-uninstall", list);
    }

    protected String getBaseURL() throws Exception {
        return getMivaResource().recursiveGet("real_name").toString();
    }

    protected URL getSetupURL() throws Exception {
        return new URL("http://" + getMivaResource().recursiveGet("name").toString() + "/Merchant2/setup.mvc");
    }
}
