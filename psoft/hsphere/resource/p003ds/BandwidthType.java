package psoft.hsphere.resource.p003ds;

import java.util.Hashtable;
import psoft.hsphere.Session;

/* renamed from: psoft.hsphere.resource.ds.BandwidthType */
/* loaded from: hsphere.zip:psoft/hsphere/resource/ds/BandwidthType.class */
class BandwidthType {
    static final int PERC95_INOUT_MBPS_ITYPE = 1;
    static final int AVERAGE_INOUT_MBPS_ITYPE = 2;
    static final int PERC95_INOUT_GB_ITYPE = 3;
    static final int AVERAGE_INOUT_GB_ITYPE = 4;
    static final int PERC95_OUT_MBPS_ITYPE = 5;
    static final int AVERAGE_OUT_MBPS_ITYPE = 6;
    static final int PERC95_OUT_GB_ITYPE = 7;
    static final int AVERAGE_OUT_GB_ITYPE = 8;

    /* renamed from: gt */
    private static Hashtable f189gt = new Hashtable();
    public static final BandwidthType UNKNOWN = new BandwidthType(-1, "UNKNOWN");
    public static final BandwidthType PERC95_INOUT_MBPS = new BandwidthType(1, "PERC95_INOUT_MBPS");
    public static final BandwidthType AVERAGE_INOUT_MBPS = new BandwidthType(2, "AVERAGE_INOUT_MBPS");
    public static final BandwidthType PERC95_INOUT_GB = new BandwidthType(3, "PERC95_INOUT_GB");
    public static final BandwidthType AVERAGE_INOUT_GB = new BandwidthType(4, "AVERAGE_INOUT_GB");
    public static final BandwidthType PERC95_OUT_MBPS = new BandwidthType(5, "PERC95_OUT_MBPS");
    public static final BandwidthType AVERAGE_OUT_MBPS = new BandwidthType(6, "AVERAGE_OUT_MBPS");
    public static final BandwidthType PERC95_OUT_GB = new BandwidthType(7, "PERC95_OUT_GB");
    public static final BandwidthType AVERAGE_OUT_GB = new BandwidthType(8, "AVERAGE_OUT_GB");
    private int intValue;
    private String strValue;
    private boolean isTrafficlike;

    private BandwidthType(int intValue, String strValue) {
        try {
            this.intValue = intValue;
            this.strValue = strValue;
            f189gt.put(new Integer(intValue), this);
            f189gt.put(strValue, this);
            this.isTrafficlike = strValue != null && strValue.endsWith("_GB");
        } catch (Exception ex) {
            Session.getLog().error("Unable to initialize a bandwidth type object with values: '" + strValue + "', " + intValue, ex);
        }
    }

    public static BandwidthType getType(int intType) {
        Object type = f189gt.get(new Integer(intType));
        if (type != null) {
            return (BandwidthType) type;
        }
        return UNKNOWN;
    }

    public static BandwidthType getType(String strType) {
        Object type = f189gt.get(strType);
        if (type != null) {
            return (BandwidthType) type;
        }
        return UNKNOWN;
    }

    public int toInt() {
        return this.intValue;
    }

    public String toString() {
        return this.strValue;
    }

    public boolean isTrafficlike() {
        return this.isTrafficlike;
    }
}
