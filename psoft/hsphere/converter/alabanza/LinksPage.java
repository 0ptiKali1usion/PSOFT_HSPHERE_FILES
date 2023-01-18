package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.resource.HTTPAuth;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/LinksPage.class */
public class LinksPage {
    public static boolean parseLinksPage(URL url, Document doc, Element root, Document billDoc, Element billRoot, List traffic) {
        boolean result;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            List clients = MigrationProvider.getClientURLList(in);
            if (!clients.isEmpty()) {
                System.out.println("Accounts found...");
                Object[] arrayC = clients.toArray();
                Authenticator.setDefault(new HTTPAuth(AlabanzaConfig.systemLogin, AlabanzaConfig.systemPassword));
                for (Object obj : arrayC) {
                    String linkGeneral = (String) obj;
                    String str = AlabanzaConfig.systemPrefix + linkGeneral;
                    System.out.println("User general info link: " + AlabanzaConfig.systemPrefix + linkGeneral);
                    AlabanzaConfig.getLog().info("User general info link: " + AlabanzaConfig.systemPrefix + linkGeneral);
                    URL urlGeneral = new URL(AlabanzaConfig.systemPrefix + linkGeneral);
                    GeneralInfo.parseGeneralInfo(urlGeneral, doc, root, billDoc, billRoot, traffic);
                }
                result = true;
            } else {
                System.out.println("End of accounts list.");
                result = false;
            }
            in.close();
        } catch (Exception e) {
            result = false;
            AlabanzaConfig.getLog().error("Some errors in LinksPage.parseLinksPage", e);
            System.out.println("Errors occured in LinksPage.parseLinksPage(). View log file for details.\n");
        }
        return result;
    }
}
