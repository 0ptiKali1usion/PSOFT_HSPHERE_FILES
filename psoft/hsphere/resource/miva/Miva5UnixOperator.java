package psoft.hsphere.resource.miva;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/Miva5UnixOperator.class */
public class Miva5UnixOperator extends Miva5Operator {
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMivaEmpresa() throws Exception {
        List list = new ArrayList();
        list.add(getMivaResource().recursiveGet("login").toString());
        list.add(getMivaResource().recursiveGet("group").toString());
        list.add(getMivaResource().recursiveGet("path").toString());
        Collection result = getHost().exec("miva-empresa5-install", list);
        if (result.size() > 0) {
            String res = (String) result.iterator().next();
            if (res.startsWith("FAILURE")) {
                throw new HSUserException("miva.installation_missed");
            }
        }
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMerchantBundle() throws Exception {
        List list = new ArrayList();
        list.add(getMivaResource().recursiveGet("login").toString());
        list.add(getMivaResource().recursiveGet("group").toString());
        list.add(getMivaResource().recursiveGet("path").toString());
        Collection result = getHost().exec("miva-merchant5-install", list);
        if (result.size() > 0) {
            String res = (String) result.iterator().next();
            if (res.startsWith("FAILURE")) {
                throw new HSUserException("miva.installation_missed");
            }
        }
    }

    public void createProvisioningData() throws Exception {
        Element setupConfig = getConfiguration();
        OutputFormat format = new OutputFormat(this.f198di, "utf-8", true);
        format.setOmitXMLDeclaration(true);
        StringWriter writer = new StringWriter();
        XMLSerializer s = new XMLSerializer(writer, format);
        s.serialize(setupConfig);
        writer.flush();
        writer.close();
        ArrayList args = new ArrayList();
        args.add(getMivaResource().recursiveGet("path").toString());
        Collection<String> result = getHost().exec("miva-merchant5-get-setup-xml", args);
        StringWriter stw = new StringWriter();
        for (String _s : result) {
            stw.write(_s + "\n");
            if ("<setup>".equals(_s.toLowerCase())) {
                stw.write(writer.toString());
            }
        }
        stw.flush();
        stw.close();
        ArrayList args2 = new ArrayList();
        args2.add(getMivaResource().recursiveGet("dir").toString() + "/mivadata/" + getMivaResource().recursiveGet("name") + "/setup.xml");
        Session.getLog().debug("GOT XML " + stw.toString());
        getHost().exec("io2file", args2, stw.toString());
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
        Session.getLog().debug("Miva Merchant physicalDelete");
        String login = getMivaResource().recursiveGet("login").toString();
        String dir = getMivaResource().recursiveGet("path").toString();
        Session.getLog().debug("MIVA: username=" + login + ",dir=" + dir);
        List list = new ArrayList();
        list.add(login);
        list.add(dir);
        getHost().exec("miva-merchant-uninstall", list);
    }
}
