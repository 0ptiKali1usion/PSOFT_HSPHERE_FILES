package psoft.license;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/license/LicenseManager.class */
public class LicenseManager {
    protected Map values;
    protected String token;

    public static void main(String[] args) throws Exception {
        FileReader fr = null;
        StringBuffer result = new StringBuffer();
        try {
            fr = new FileReader(args[0]);
            BufferedReader in = new BufferedReader(fr);
            while (true) {
                String tmp = in.readLine();
                if (tmp == null) {
                    break;
                }
                result.append(tmp).append("\n");
            }
            fr.close();
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e) {
                }
            }
            License l = new License(result.toString());
            System.out.println("Valid: " + l.isValid());
            System.out.println("Accounts: " + l.getValue("ACCOUNTS"));
        } catch (Throwable th) {
            if (fr != null) {
                try {
                    fr.close();
                } catch (IOException e2) {
                }
            }
            throw th;
        }
    }
}
