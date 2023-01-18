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

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/ControlPanel.class */
public class ControlPanel {
    public static boolean parseControlPanel(URL url, Document doc, Element mysql, Element domain, String domainName, String address, String login, String password, List traffic) {
        boolean result = false;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            System.out.println("\nStart reading control panel for domain " + domainName);
            AlabanzaConfig.getLog().info("\nStart reading control panel for domain " + domainName);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            Hashtable managers = MigrationProvider.getManagers(in);
            if (!managers.isEmpty()) {
                String mailManagerLink = (String) managers.get("mailManager");
                Authenticator.setDefault(new HTTPAuth(login, password));
                URL urlMailManager = new URL(AlabanzaConfig.systemHttpPrefix + address + mailManagerLink);
                System.out.println("Mail manager link: " + AlabanzaConfig.systemHttpPrefix + address + mailManagerLink);
                AlabanzaConfig.getLog().info("Mail manager link: " + AlabanzaConfig.systemHttpPrefix + address + mailManagerLink);
                MailManager.parseMailManager(urlMailManager, doc, domain, domainName, address, login, password, traffic);
                String mysqlManagerLink = (String) managers.get("mysqlURL");
                if (mysqlManagerLink != null) {
                    Authenticator.setDefault(new HTTPAuth(login, password));
                    URL urlMySQLManager = new URL(AlabanzaConfig.systemHttpPrefix + address + mysqlManagerLink);
                    System.out.println("MySQL manager link: " + AlabanzaConfig.systemHttpPrefix + address + mysqlManagerLink);
                    AlabanzaConfig.getLog().info("MySQL manager link: " + AlabanzaConfig.systemHttpPrefix + address + mysqlManagerLink);
                    String mysqlcp = "http://" + address + "/cp/mysqladmin/";
                    MySQL.parseMySQLManager(urlMySQLManager, doc, mysql, mysqlcp, traffic);
                }
                result = true;
            }
            in.close();
            System.out.println("Finished reading control panel info for domain " + domainName + "\n");
            AlabanzaConfig.getLog().info("Finished reading control panel info for domain " + domainName + "\n");
        } catch (Exception e) {
            System.out.println("Reading control panel info failed.");
            AlabanzaConfig.getLog().error("Some errors occured in ControlPanel.parseControlPanel(). Domain name =" + domainName + " Address = " + address, e);
            result = false;
        }
        return result;
    }
}
