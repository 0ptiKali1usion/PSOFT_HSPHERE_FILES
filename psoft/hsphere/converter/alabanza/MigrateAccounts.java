package psoft.hsphere.converter.alabanza;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import psoft.hsphere.resource.HTTPAuth;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/MigrateAccounts.class */
public class MigrateAccounts {
    private Document mainDoc;
    private Document mainBillDoc;
    private Element mainRoot;
    private Element mainBillRoot;
    private List traffic = new LinkedList();

    public static void main(String[] argv) throws Exception {
        System.out.println("Preparing for migration...");
        MigrateAccounts test = new MigrateAccounts();
        if (AlabanzaConfig.setClassVariables() && AlabanzaConfig.setLog()) {
            if (test.m25go()) {
                System.out.println("Migration finished.\n\n");
                System.exit(0);
                return;
            }
            System.out.println("Migration failed.\n\n");
            System.exit(0);
            return;
        }
        System.out.println("Setting config failed.\n\n");
        System.exit(0);
    }

    /* renamed from: go */
    private boolean m25go() {
        boolean result;
        int counter = AlabanzaConfig.systemStartPage.intValue();
        new String();
        Authenticator.setDefault(new HTTPAuth(AlabanzaConfig.systemLogin, AlabanzaConfig.systemPassword));
        if (AlabanzaConfig.systemBillsUrl != null) {
            try {
                String strUrl = AlabanzaConfig.systemPrefix + AlabanzaConfig.systemBillsUrl;
                URL url = new URL(strUrl);
                System.out.println("Link to billing periods page: " + strUrl);
                BillPeriods.parseBillPeriodsPage(url, this.traffic);
            } catch (Exception e) {
                AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.go().", e);
                System.out.println("Some errors occured in MigrateAccounts.go(). View log file for details.\n");
                return false;
            }
        }
        if (AlabanzaConfig.systemGetCustomUsers.booleanValue()) {
            List users = CustomUsers.getCustomUsers();
            if (!users.isEmpty()) {
                setDocImpl();
                setBillDocImpl();
                int size = users.size();
                for (int i = 0; i < size; i++) {
                    Hashtable oneUser = (Hashtable) users.get(i);
                    String userId = (String) oneUser.get("id");
                    if (userId != null) {
                        try {
                            URL url2 = new URL(AlabanzaConfig.systemPrefix + AlabanzaConfig.systemOneClientUrl + userId);
                            System.out.println("Custom user link: " + AlabanzaConfig.systemPrefix + AlabanzaConfig.systemOneClientUrl + userId);
                            LinksPage.parseLinksPage(url2, this.mainDoc, this.mainRoot, this.mainBillDoc, this.mainBillRoot, this.traffic);
                        } catch (Exception e2) {
                            AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.go().", e2);
                            System.out.println("Some errors occured in MigrateAccounts.go(). View log file for details.\n");
                        }
                    }
                }
                this.mainDoc.appendChild(this.mainRoot);
                serializeDoc(counter);
                if (AlabanzaConfig.systemUseBill.booleanValue()) {
                    this.mainBillDoc.appendChild(this.mainBillRoot);
                    serializeBillDoc(counter);
                }
                result = true;
            } else {
                System.out.println("Custom users list is empty.");
                result = true;
            }
        } else {
            while (1 != 0) {
                try {
                    setDocImpl();
                    setBillDocImpl();
                    counter++;
                    URL url3 = new URL(AlabanzaConfig.systemPrefix + AlabanzaConfig.systemClientsListUrl + new Integer(counter).toString());
                    System.out.println("Users list link: " + AlabanzaConfig.systemPrefix + AlabanzaConfig.systemClientsListUrl + new Integer(counter).toString());
                    if (!LinksPage.parseLinksPage(url3, this.mainDoc, this.mainRoot, this.mainBillDoc, this.mainBillRoot, this.traffic)) {
                        break;
                    }
                    this.mainDoc.appendChild(this.mainRoot);
                    serializeDoc(counter);
                    if (AlabanzaConfig.systemUseBill.booleanValue()) {
                        this.mainBillDoc.appendChild(this.mainBillRoot);
                        serializeBillDoc(counter);
                    }
                    long tmp_traffic = 0;
                    int size2 = this.traffic.size();
                    for (int i2 = 0; i2 < size2; i2++) {
                        tmp_traffic += ((Integer) this.traffic.get(i2)).longValue();
                    }
                    AlabanzaConfig.getLog().info("http traffic on page " + String.valueOf(counter) + " (in MB) = " + String.valueOf((tmp_traffic / 1024) / 1024));
                    AlabanzaConfig.getLog().info("http traffic on page " + String.valueOf(counter) + " (in kb) = " + String.valueOf(tmp_traffic / 1024));
                    AlabanzaConfig.getLog().info("http traffic on page " + String.valueOf(counter) + " (in b) = " + String.valueOf(tmp_traffic));
                    AlabanzaConfig.getLog().info("Counter = " + String.valueOf(counter));
                } catch (Exception e3) {
                    AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.go().", e3);
                    System.out.println("Some errors occured in MigrateAccounts.go(). View log file for details.\n");
                    result = false;
                }
            }
            result = true;
        }
        long result_traffic = 0;
        int size3 = this.traffic.size();
        for (int i3 = 0; i3 < size3; i3++) {
            result_traffic += ((Integer) this.traffic.get(i3)).longValue();
        }
        AlabanzaConfig.getLog().info("Total http traffic (in MB) = " + String.valueOf((result_traffic / 1024) / 1024));
        AlabanzaConfig.getLog().info("Total http traffic (in kb) = " + String.valueOf(result_traffic / 1024));
        AlabanzaConfig.getLog().info("Total http traffic (in b) = " + String.valueOf(result_traffic));
        AlabanzaConfig.getLog().info("Finally counter = " + String.valueOf(counter));
        return result;
    }

    private void testURL(URLConnection con, String fileName) {
        try {
            FileWriter writer = new FileWriter(fileName);
            BufferedReader inTest = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuffer message = new StringBuffer();
            while (true) {
                String inputline = inTest.readLine();
                if (inputline != null) {
                    message.append(inputline);
                    message.append("\n");
                } else {
                    inTest.close();
                    writer.write(message.toString());
                    writer.close();
                    return;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDocImpl() {
        try {
            this.mainDoc = new DocumentImpl();
            this.mainRoot = this.mainDoc.createElement("users");
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.setDocImpl().", e);
            System.out.println("Some errors occured in MigrateAccounts.setDocImpl(). View log file for details.\n");
        }
    }

    private void setBillDocImpl() {
        try {
            this.mainBillDoc = new DocumentImpl();
            this.mainBillRoot = this.mainBillDoc.createElement("users");
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.setBillDocImpl().", e);
            System.out.println("Some errors occured in MigrateAccounts.setBillDocImpl(). View log file for details.\n");
        }
    }

    private void serializeDoc(int i) {
        try {
            OutputFormat format = new OutputFormat(this.mainDoc);
            format.setIndenting(true);
            String filename = AlabanzaConfig.systemFileName + String.valueOf(i) + ".xml";
            XMLSerializer serializer = new XMLSerializer(new FileWriter(filename), format);
            serializer.serialize(this.mainDoc);
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.serializeDoc().", e);
            System.out.println("Some errors occured in MigrateAccounts.serializeDoc(). View log file for details.\n");
        }
    }

    private void serializeBillDoc(int i) {
        try {
            OutputFormat format = new OutputFormat(this.mainBillDoc);
            format.setIndenting(true);
            String filename = AlabanzaConfig.systemBillFileName + String.valueOf(i) + ".xml";
            XMLSerializer serializer = new XMLSerializer(new FileWriter(filename), format);
            serializer.serialize(this.mainBillDoc);
        } catch (Exception e) {
            AlabanzaConfig.getLog().error("Some errors occured in MigrateAccounts.serializeBillDoc().", e);
            System.out.println("Some errors occured in MigrateAccounts.serializeBillDoc(). View log file for details.\n");
        }
    }
}
