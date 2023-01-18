package psoft.hsp.tools;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

/* loaded from: hsphere.zip:psoft/hsp/tools/PHServerTypes.class */
public class PHServerTypes {
    private static Hashtable osToOsFamily = new Hashtable();
    private static Hashtable serverTypeToPlatform = new Hashtable();

    static {
        osToOsFamily.put("RH72", "LINUX");
        osToOsFamily.put("RH73", "LINUX");
        osToOsFamily.put("RHES2.1", "LINUX");
        osToOsFamily.put("RHAS2.1", "LINUX");
        osToOsFamily.put("RHAS3", "LINUX");
        osToOsFamily.put("RHES3", "LINUX");
        osToOsFamily.put("RHWS3", "LINUX");
        osToOsFamily.put("CENTOS3", "LINUX");
        osToOsFamily.put("TRUSTIXEL2", "LINUX");
        osToOsFamily.put("RHEL4", "LINUX");
        osToOsFamily.put("FBSD4", "FREEBSD");
        osToOsFamily.put("FBSD5", "FREEBSD");
        serverTypeToPlatform.put("UNIX_HOSTING", "UNIX");
        serverTypeToPlatform.put("MAIL", "UNIX");
        serverTypeToPlatform.put("MYSQL", "UNIX");
        serverTypeToPlatform.put("DNS", "UNIX");
        serverTypeToPlatform.put("CP", "UNIX");
        serverTypeToPlatform.put("PGSQL", "UNIX");
        serverTypeToPlatform.put("UNIX_REAL", "UNIX");
    }

    public static Set getSupportedOsFamilies() {
        return new HashSet(osToOsFamily.values());
    }

    public static void registerUnsupportedOsFamilies(String osFamily, String osType) {
        if (!"unknown".equals(osFamily.toLowerCase()) && !osToOsFamily.contains(osFamily)) {
            osToOsFamily.put(osFamily, osType);
        }
    }

    public static Set getSupportedOs() {
        return new HashSet(osToOsFamily.keySet());
    }

    public static String getOsFamilyByOsName(String os) {
        return (String) osToOsFamily.get(os);
    }

    public static Set getLServerGroupTypes() {
        return serverTypeToPlatform.keySet();
    }
}
