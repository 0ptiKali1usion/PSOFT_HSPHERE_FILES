package psoft.license;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;
import psoft.encryption.MD5;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/license/RSLicenseEncryptor.class */
public class RSLicenseEncryptor {
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("Usage RSLicenseEncryptor: company_name name, license, accounts");
            System.exit(1);
        }
        Map map = new HashMap();
        map.put("ACCOUNTS", args[3]);
        encode(args[0], args[1], TimeUtils.currentTimeMillis(), 0L, 0L, "Rackspace Managed Hosting", "H-Sphere", "2.0", args[2], map, "POSITIVE_GOD_TODAY", "psoft_1234");
    }

    public static void encode(String companyName, String name, long date, long duration, long revocationDate, String issuer, String productName, String version, String licenseName, Map values, String secret, String internalSecret) {
        PrintStream out = System.out;
        StringBuffer buf = new StringBuffer();
        buf.append(companyName);
        out.println("COMPANY=" + companyName);
        if ((name != null) & (name.length() > 0)) {
            out.println("NAME=" + name);
            buf.append(name);
        }
        buf.append(Long.toString(date));
        out.println("ISSUED=" + date);
        if (duration > 0) {
            out.println("DURATION=" + duration);
            buf.append(Long.toString(duration));
        }
        if (revocationDate > 0) {
            out.println("REVOKATION=" + revocationDate);
            buf.append(Long.toString(revocationDate));
        }
        buf.append(issuer).append(productName).append(version);
        buf.append(licenseName);
        out.println("ISSUER=" + issuer);
        out.println("PRODUCT=" + productName);
        out.println("VERSION=" + version);
        out.println("LICENSE=" + licenseName);
        for (Object obj : values.keySet()) {
            String key = obj.toString();
            String value = values.get(key).toString();
            buf.append(value);
            out.println(key + "=" + value);
        }
        MD5 md5 = new MD5(buf.toString() + secret);
        out.println("LICENSE_KEY=" + md5.asHex());
        MD5 md52 = new MD5(buf.toString() + internalSecret);
        out.println("CERTIFICATE=" + md52.asHex());
        out.flush();
    }
}
