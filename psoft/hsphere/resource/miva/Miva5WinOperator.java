package psoft.hsphere.resource.miva;

import java.io.StringWriter;
import java.util.Hashtable;
import org.apache.axis.AxisFault;
import org.apache.axis.encoding.Base64;
import org.apache.axis.message.SOAPEnvelope;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Element;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/Miva5WinOperator.class */
public class Miva5WinOperator extends Miva5Operator {
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
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void installMerchantBundle() throws Exception {
        Session.getLog().debug("Miva Merchant physicalCreate");
        String name = getMivaResource().recursiveGet("real_name").toString();
        try {
            ((WinHostEntry) getHost()).invokeMethod("create", new String[]{new String[]{"resourcename", "mivamerchant5"}, new String[]{"hostname", name}});
        } catch (AxisFault af) {
            Session.getLog().error("Error installing Miva Merchant5 for domain " + name, af);
            throw AxisFault.makeFault(af);
        }
    }

    @Override // psoft.hsphere.resource.miva.MivaOperator
    public Hashtable configureMerchant() throws Exception {
        createProvisioningData();
        return setupMivaMerchant();
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
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.miva.MivaOperator
    public void uninstallMivaMerchant() throws Exception {
        String name = getMivaResource().recursiveGet("real_name").toString();
        try {
            ((WinHostEntry) getHost()).invokeMethod("delete", new String[]{new String[]{"resourcename", "mivamerchant5"}, new String[]{"hostname", name}});
        } catch (AxisFault af) {
            Session.getLog().error("Error installing Miva Merchant5 for domain " + name, af);
            throw AxisFault.makeFault(af);
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v33, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v8, types: [java.lang.String[], java.lang.String[][]] */
    private void createProvisioningData() throws Exception {
        Session.getLog().debug("Creating provisioning data");
        String name = getMivaResource().recursiveGet("real_name").toString();
        Element setupConfig = getConfiguration();
        OutputFormat format = new OutputFormat(this.f198di, "utf-8", true);
        format.setOmitXMLDeclaration(true);
        StringWriter writer = new StringWriter();
        XMLSerializer s = new XMLSerializer(writer, format);
        s.serialize(setupConfig);
        writer.flush();
        writer.close();
        try {
            SOAPEnvelope envelope = ((WinHostEntry) getHost()).invokeMethod("get", new String[]{new String[]{"resourcename", "mivamerchant5"}, new String[]{"hostname", name}, new String[]{"subdir", "/mm5/5.00"}, new String[]{"configfile", "setup.xml"}});
            Session.getLog().debug("Got SOAP envelope");
            String config = new String(Base64.decode(WinService.getChildElement(envelope, "config").getValue()));
            Session.getLog().debug("Got config attribute:" + config);
            StringBuffer setupData = new StringBuffer();
            setupData.append(config.substring(0, config.indexOf("<Setup>") + 7));
            setupData.append('\n' + writer.toString());
            setupData.append(config.substring(config.indexOf("<Setup>") + 7, config.length() - 1));
            Session.getLog().debug("Got SETUP DATA\n" + setupData.toString());
            try {
                ((WinHostEntry) getHost()).invokeMethod("update", new String[]{new String[]{"resourcename", "mivamerchant5"}, new String[]{"hostname", name}, new String[]{"config", Base64.encode(setupData.toString().getBytes())}, new String[]{"configfile", "setup.xml"}});
            } catch (AxisFault af) {
                Session.getLog().error("Error configuring Miva Merchant5 for domain " + name, af);
                throw AxisFault.makeFault(af);
            }
        } catch (AxisFault af2) {
            Session.getLog().error("Error configuring Miva Merchant5 for domain " + name, af2);
            throw AxisFault.makeFault(af2);
        }
    }
}
