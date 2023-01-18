package psoft.hsp.tools;

import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/hsp/tools/PhysicalFileKey.class */
public class PhysicalFileKey {
    String srvGroupType = "ANY";
    String osFamily = "ANY";
    String osName = "ANY";

    public PhysicalFileKey(String composedKey) throws Exception {
        StringTokenizer st = new StringTokenizer(composedKey, "/");
        if (st.countTokens() != 3) {
            throw new Exception("Invalid composed key " + composedKey);
        }
        fillKey(st.nextToken(), st.nextToken(), st.nextToken());
    }

    public PhysicalFileKey(String srvGroupType, String osFamily, String osName) {
        fillKey(srvGroupType, osFamily, osName);
    }

    private void fillKey(String srvGroupType, String osFamily, String osName) {
        if (!isEmpty(srvGroupType) && PHServerTypes.getLServerGroupTypes().contains(srvGroupType)) {
            this.srvGroupType = srvGroupType;
        }
        if (!isEmpty(osFamily) && PHServerTypes.getSupportedOsFamilies().contains(osFamily)) {
            this.osFamily = osFamily;
        }
        if (!isEmpty(osName) && PHServerTypes.getSupportedOs().contains(osName)) {
            this.osName = osName;
        }
    }

    protected static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof PhysicalFileKey) {
            PhysicalFileKey physicalFileKey = (PhysicalFileKey) o;
            if (!this.osFamily.equals(physicalFileKey.osFamily) && !"ANY".equals(physicalFileKey.osFamily) && !"ANY".equals(this.osFamily)) {
                return false;
            }
            if (!this.osName.equals(physicalFileKey.osName) && !"ANY".equals(physicalFileKey.osName) && !"ANY".equals(this.osName)) {
                return false;
            }
            if (!this.srvGroupType.equals(physicalFileKey.srvGroupType) && !"ANY".equals(physicalFileKey.srvGroupType) && !"ANY".equals(this.srvGroupType)) {
                return false;
            }
            return true;
        }
        return false;
    }

    public String toString() {
        return this.srvGroupType + "/" + this.osFamily + "/" + this.osName;
    }

    public String getSrvGroupType() {
        return this.srvGroupType;
    }

    public String getOsFamily() {
        return this.osFamily;
    }

    public String getOsName() {
        return this.osName;
    }

    public void setOsFamily(String osFamily) {
        this.osFamily = osFamily;
    }
}
