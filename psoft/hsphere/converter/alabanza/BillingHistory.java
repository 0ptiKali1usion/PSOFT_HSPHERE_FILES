package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/BillingHistory.class */
public class BillingHistory {
    public static boolean parseBillingHistory(URL url, Document doc, Element user, List traffic) {
        boolean result;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            List billingInfo = MigrationProvider.getBillingHistory(in);
            if (!billingInfo.isEmpty()) {
                int length = billingInfo.size();
                for (int i = 0; i < length; i++) {
                    Hashtable oneEntry = (Hashtable) billingInfo.get(i);
                    Element entry = doc.createElement("billEntry");
                    if (oneEntry.containsKey("Domain")) {
                        entry.setAttribute("domain", (String) oneEntry.get("Domain"));
                    }
                    if (oneEntry.containsKey("Package")) {
                        entry.setAttribute("package", (String) oneEntry.get("Package"));
                    }
                    if (oneEntry.containsKey("Type")) {
                        entry.setAttribute("type", (String) oneEntry.get("Type"));
                    }
                    if (oneEntry.containsKey("Date")) {
                        entry.setAttribute("date", (String) oneEntry.get("Date"));
                    }
                    if (oneEntry.containsKey("Notes")) {
                        entry.setAttribute("notes", (String) oneEntry.get("Notes"));
                    }
                    if (oneEntry.containsKey("StartDate")) {
                        entry.setAttribute("startdate", (String) oneEntry.get("StartDate"));
                    }
                    if (oneEntry.containsKey("UnitPrice")) {
                        entry.setAttribute("unitprice", (String) oneEntry.get("UnitPrice"));
                    }
                    if (oneEntry.containsKey("Length")) {
                        entry.setAttribute("length", (String) oneEntry.get("Length"));
                    }
                    if (oneEntry.containsKey("Discount")) {
                        entry.setAttribute("discount", (String) oneEntry.get("Discount"));
                    }
                    if (oneEntry.containsKey("Credit")) {
                        entry.setAttribute("credit", (String) oneEntry.get("Credit"));
                    }
                    if (oneEntry.containsKey("Debit")) {
                        entry.setAttribute("debit", (String) oneEntry.get("Debit"));
                    }
                    if (oneEntry.containsKey("Balance")) {
                        entry.setAttribute("balance", (String) oneEntry.get("Balance"));
                    }
                    user.appendChild(entry);
                }
                result = true;
            } else {
                result = false;
            }
            in.close();
        } catch (Exception e) {
            System.out.println("Reading billing entries page failed. View log file for details.\n");
            AlabanzaConfig.getLog().error("Some errors occured in BillingHistory.parseBillingHistory()", e);
            result = false;
        }
        System.out.println("Finished reading billing entries page.");
        return result;
    }
}
