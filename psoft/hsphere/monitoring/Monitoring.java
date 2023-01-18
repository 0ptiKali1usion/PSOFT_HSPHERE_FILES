package psoft.hsphere.monitoring;

import java.net.HttpURLConnection;
import java.net.URL;
import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/Monitoring.class */
public class Monitoring {
    public static void checkSSH(HostEntry host) throws Exception {
    }

    public static void checkHTTPd(HostEntry host) throws Exception {
        StringBuffer buf = new StringBuffer("http://");
        buf.append(host.getPFirstIP()).append("/server-status/");
        URL p = new URL(buf.toString());
        HttpURLConnection con = (HttpURLConnection) p.openConnection();
        int exit = con.getResponseCode();
        con.disconnect();
        if (exit != 200) {
            throw new Exception("Error checking http:" + exit);
        }
    }

    public static void checkFTPd(HostEntry host) throws Exception {
    }

    public static void checkMail(HostEntry host) throws Exception {
    }

    public static void checkDNS(HostEntry host) throws Exception {
    }
}
