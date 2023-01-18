package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.resource.HTTPAuth;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/DomainInfo.class */
public class DomainInfo {
    private static Hashtable domainsHash = new Hashtable();

    public static boolean parseDomainData(String url, Document doc, Element domain, String login, String password, List traffic) {
        Hashtable domainData = new Hashtable();
        url.toString();
        if (domainsHash.containsKey(url)) {
            System.out.println("Domain data link got from cash");
            domainData = (Hashtable) domainsHash.get(url);
        } else {
            try {
                Authenticator.setDefault(new HTTPAuth(login, password));
                URL urlDomainData = new URL(url);
                System.out.println("Domain data link: " + url);
                AlabanzaConfig.getLog().info("Domain data link: " + url);
                System.out.println("Waiting for reply...");
                HttpURLConnection con = (HttpURLConnection) urlDomainData.openConnection();
                con.setRequestMethod("GET");
                con.setDoInput(true);
                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
                traffic.add(new Integer(in.available()));
                domainData = MigrationProvider.getDomainData(in);
                in.close();
                domainsHash.put(url, domainData);
            } catch (Exception e) {
                System.out.println("Reading domain data failed. View log file for details.\n");
                AlabanzaConfig.getLog().error("Some errors occur in DomainInfo.parseDomainData", e);
            }
        }
        if (!domainData.isEmpty()) {
            domain.setAttribute("quota", (String) domainData.get("quota"));
            domain.setAttribute("transfer", (String) domainData.get("transfer"));
        }
        System.out.println("Finished reading domain data info.");
        return true;
    }
}
