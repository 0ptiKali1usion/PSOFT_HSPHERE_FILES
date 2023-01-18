package psoft.hsphere.converter.alabanza;

import java.io.BufferedInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Hashtable;
import java.util.List;

/* loaded from: hsphere.zip:psoft/hsphere/converter/alabanza/BillPeriods.class */
public class BillPeriods {
    public static boolean parseBillPeriodsPage(URL url, List traffic) {
        boolean result = false;
        url.toString();
        try {
            System.out.println("Waiting for reply...");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setDoInput(true);
            BufferedInputStream in = new BufferedInputStream(con.getInputStream());
            traffic.add(new Integer(in.available()));
            List bills = MigrationProvider.getBillPeriods(in);
            Hashtable periods = new Hashtable();
            int size = bills.size();
            for (int i = 0; i < size; i++) {
                String key = (String) bills.get(i);
                periods.put(key, String.valueOf(i));
            }
            AlabanzaConfig.setBillPeriods(periods);
            in.close();
            System.out.println("Finished reading billing periods.");
        } catch (Exception e) {
            System.out.println("Reading billing periods failed.");
            AlabanzaConfig.getLog().error("Some errors occured in BillPeriods.parseBillPeriodsPage().", e);
            result = false;
        }
        return result;
    }
}
