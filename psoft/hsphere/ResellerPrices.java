package psoft.hsphere;

import java.text.ParseException;
import java.util.Date;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/ResellerPrices.class */
public class ResellerPrices {
    protected String name;
    protected Date created;
    protected double setupPrice;
    protected String setupCalc;
    protected double recurrentPrice;
    protected String recurrentCalc;
    protected double usagePrice;
    protected String usageCalc;
    protected double refundPercent;
    protected String refundCalc;
    protected double max;
    protected double freeUnits;

    public boolean equals(Object obj) {
        ResellerPrices pr = (ResellerPrices) obj;
        return this.name.equals(pr.getName()) && getEmptyIfNull(getSetupCalc()).equals(getEmptyIfNull(pr.getSetupCalc())) && (this.setupPrice == pr.getSetupPrice() || (Double.isNaN(this.setupPrice) && Double.isNaN(pr.getSetupPrice()))) && ((this.recurrentPrice == pr.getRecurrentPrice() || (Double.isNaN(this.recurrentPrice) && Double.isNaN(pr.getRecurrentPrice()))) && ((this.usagePrice == pr.getUsagePrice() || (Double.isNaN(this.usagePrice) && Double.isNaN(pr.getUsagePrice()))) && ((this.refundPercent == pr.getRefundPercent() || (Double.isNaN(this.refundPercent) && Double.isNaN(pr.getRefundPercent()))) && this.max == pr.getMax() && this.freeUnits == pr.getFreeUnits())));
    }

    public ResellerPrices(String name) {
        this.setupPrice = Double.NaN;
        this.setupCalc = null;
        this.recurrentPrice = Double.NaN;
        this.recurrentCalc = null;
        this.usagePrice = Double.NaN;
        this.usageCalc = null;
        this.refundPercent = Double.NaN;
        this.refundCalc = null;
        this.max = -1.0d;
        this.freeUnits = 0.0d;
        this.name = name;
        this.created = TimeUtils.getDate();
    }

    public ResellerPrices(String name, String setupPrice) {
        this(name, setupPrice, null, null, null, null, null, null, null, null, null);
    }

    public ResellerPrices(String name, String setupPrice, String setupCalc, String recurrentPrice, String recurrentCalc, String usagePrice, String usageCalc, String refundPercent, String refundCalc, String max, String freeUnits) {
        this(name);
        if (setupPrice != null) {
            try {
                this.setupPrice = USFormat.parseDouble(setupPrice);
            } catch (ParseException e) {
                this.setupPrice = Double.NaN;
                this.setupCalc = null;
            }
        }
        this.setupCalc = setupCalc;
        if (recurrentPrice != null) {
            try {
                this.recurrentPrice = USFormat.parseDouble(recurrentPrice);
            } catch (ParseException e2) {
                this.recurrentPrice = Double.NaN;
                this.recurrentCalc = null;
            }
        }
        this.recurrentCalc = recurrentCalc;
        if (usagePrice != null) {
            try {
                this.usagePrice = USFormat.parseDouble(usagePrice);
            } catch (ParseException e3) {
                this.usagePrice = Double.NaN;
                this.usageCalc = null;
            }
        }
        this.usageCalc = usageCalc;
        if (refundPercent != null) {
            try {
                this.refundPercent = USFormat.parseDouble(refundPercent);
            } catch (ParseException e4) {
                this.refundPercent = Double.NaN;
                this.refundCalc = null;
            }
        }
        this.refundCalc = refundCalc;
        if (max != null) {
            try {
                this.max = USFormat.parseDouble(max);
            } catch (ParseException e5) {
                this.max = -1.0d;
            }
        }
        if (freeUnits != null) {
            try {
                this.freeUnits = USFormat.parseDouble(freeUnits);
            } catch (ParseException e6) {
                this.freeUnits = 0.0d;
            }
        }
    }

    public ResellerPrices(String name, Plan plan) throws NoSuchTypeException {
        this(name, plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_SETUP_PRICE_" + name + "_"), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_SETUP_CALC_" + name), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_UNIT_PRICE_" + name + "_"), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_RECURRENT_CALC_" + name), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_USAGE_PRICE_" + name + "_"), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_USAGE_CALC_" + name), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_REFUND_PRICE_" + name + "_"), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_REFUND_CALC_" + name), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_MAX_" + name + "_"), plan.getValue(TypeRegistry.getTypeId(FMACLManager.RESELLER), "_FREE_UNITS_" + name + "_"));
    }

    public boolean isNull() {
        return Double.isNaN(this.setupPrice) && this.setupCalc == null && Double.isNaN(this.recurrentPrice) && this.recurrentCalc == null && Double.isNaN(this.usagePrice) && this.usageCalc == null && Double.isNaN(this.refundPercent) && this.refundCalc == null && this.max == -1.0d && this.freeUnits == 0.0d;
    }

    public String getName() {
        return this.name;
    }

    public double getSetupPrice() {
        return this.setupPrice;
    }

    public String getSetupCalc() {
        return this.setupCalc;
    }

    public double getRecurrentPrice() {
        return this.recurrentPrice;
    }

    public String getRecurrentCalc() {
        return this.recurrentCalc;
    }

    public double getUsagePrice() {
        return this.usagePrice;
    }

    public String getUsageCalc() {
        return this.usageCalc;
    }

    public double getRefundPercent() {
        return this.refundPercent;
    }

    public String getRefundCalc() {
        return this.refundCalc;
    }

    public double getMax() {
        return this.max;
    }

    public double getFreeUnits() {
        return this.freeUnits;
    }

    public Date getCreated() {
        return this.created;
    }

    public String toString() {
        return "[" + getName() + " SETUP_P:" + getSetupPrice() + " SETUP_C:" + getSetupCalc() + " RECURRENT_P:" + getRecurrentPrice() + " RECURRENT_C:" + getRecurrentCalc() + " USAGE_P:" + getUsagePrice() + " USAGE_C:" + getUsageCalc() + " REFUND_P:" + getRefundPercent() + " REFUND_C" + getRefundCalc() + " MAX:" + getMax() + " FREE_UNITS:" + getFreeUnits() + "]";
    }

    protected static String getEmptyIfNull(String arg) {
        if (arg == null) {
            return "";
        }
        return arg;
    }
}
