package psoft.hsphere.resource.ssl;

import com.comodo.api.ComodoAPI;
import com.comodo.api.ComodoOrder;
import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.log4j.Category;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.epayment.MerchantGatewayManager;
import psoft.hsphere.resource.system.MailServices;
import psoft.util.LocalExec;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ssl/SSLTools.class */
public class SSLTools {
    private static final Category log = Category.getInstance(SSLTools.class.getName());

    public static ComodoOrder orderLicense(String login, String password, String product, int years, int servers, int software, String streetAddress1, String streetAddress2, String streetAddress3, String postalCode, String dunsNumber, String companyNumber, String request, String email) throws Exception {
        Map clientInfo = new HashMap();
        addField(clientInfo, "streetAddress1", streetAddress1);
        addField(clientInfo, "streetAddress2", streetAddress2);
        addField(clientInfo, "streetAddress3", streetAddress3);
        addField(clientInfo, "postalCode", postalCode);
        addField(clientInfo, "dunsNumber", dunsNumber);
        addField(clientInfo, "companyNumber", companyNumber);
        addField(clientInfo, "emailAddress", email);
        addField(clientInfo, "contactEmailAddress", email);
        return ComodoAPI.applySSL(login, password, request, product, years, servers, software, clientInfo, false);
    }

    private static void addField(Map map, String label, String field) {
        if (field != null && field.length() > 0) {
            map.put(label, field);
        }
    }

    public static SelfSignedCertificate _generateSSL(String country, String state, String locality, String organization, String organ_unit, String site, String email) throws Exception {
        if (HostEntry.getEmulationMode()) {
            log.debug("EMULATION MODE: generating new SSL /hsphere/shared/scripts/generate-certificates " + MailServices.shellQuote(country) + " " + MailServices.shellQuote(state) + " " + MailServices.shellQuote(locality) + " " + MailServices.shellQuote(organization) + " " + MailServices.shellQuote(organ_unit) + " " + MailServices.shellQuote(site) + " " + MailServices.shellQuote(email));
            return SelfSignedCertificate.DEMO_CERT;
        }
        log.debug("generating new SSL /hsphere/shared/scripts/generate-certificates " + MailServices.shellQuote(country) + " " + MailServices.shellQuote(state) + " " + MailServices.shellQuote(locality) + " " + MailServices.shellQuote(organization) + " " + MailServices.shellQuote(organ_unit) + " " + MailServices.shellQuote(site) + " " + MailServices.shellQuote(email));
        Collection lines = LocalExec.exec(new String[]{"/hsphere/shared/scripts/generate-certificates", MailServices.shellQuote(country), MailServices.shellQuote(state), MailServices.shellQuote(locality), MailServices.shellQuote(organization), MailServices.shellQuote(organ_unit), MailServices.shellQuote(site), MailServices.shellQuote(email)}, null);
        Iterator i = lines.iterator();
        nextCert(i);
        return new SelfSignedCertificate(nextCert(i), nextCert(i), nextCert(i));
    }

    public static TemplateModel generateSSL(String country, String state, String locality, String organization, String organ_unit, String site, String email) throws Exception {
        TemplateHash ssl = new TemplateHash();
        SelfSignedCertificate c = _generateSSL(country, state, locality, organization, organ_unit, site, email);
        ssl.put(MerchantGatewayManager.MG_KEY_PREFIX, new TemplateString(c.getKey()));
        ssl.put("req", new TemplateString(c.getReq()));
        ssl.put("file", new TemplateString(c.getCrt()));
        return ssl;
    }

    protected static String nextCert(Iterator i) throws Exception {
        log.debug("SSLTools.nextCert ");
        StringBuffer buff = new StringBuffer();
        while (i.hasNext()) {
            String s = (String) i.next();
            log.debug("NEXT_CERT: '" + s + "'");
            if (s.indexOf("--SEPARATOR--psoft.hsphere.HsphereToolbox--") != -1) {
                log.debug("SSLTools.nextCert end of cert");
                return buff.toString();
            }
            buff.append(s).append("\n");
        }
        throw new Exception("unexpected end of collection");
    }
}
