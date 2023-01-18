package psoft.hsphere.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.StringTokenizer;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.AdmDNSZone;

/* loaded from: hsphere.zip:psoft/hsphere/tools/BatchDNSRecord.class */
public class BatchDNSRecord extends C0004CP {
    public BatchDNSRecord() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    public static void main(String[] argv) throws Exception {
        String pref;
        String data;
        new BatchDNSRecord();
        BufferedReader f1 = new BufferedReader(new InputStreamReader(new FileInputStream(argv[0])));
        AdmDNSZone zone = AdmDNSZone.getByName(argv[1]);
        if (HostManager.getHosts(2).size() <= 0) {
            System.exit(0);
        }
        while (true) {
            String line = f1.readLine();
            if (line != null) {
                StringTokenizer st = new StringTokenizer(line);
                String name = st.nextToken();
                if (name.equals(".")) {
                    name = "";
                }
                String ttl = st.nextToken();
                st.nextToken();
                String type = st.nextToken();
                if (st.countTokens() < 2) {
                    data = st.nextToken();
                    pref = "";
                } else {
                    pref = st.nextToken();
                    data = st.nextToken();
                }
                zone.addCustRecord(data, name, type, ttl, pref);
            } else {
                f1.close();
                System.out.println("Finished");
                System.exit(0);
                return;
            }
        }
    }
}
