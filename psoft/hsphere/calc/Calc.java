package psoft.hsphere.calc;

import psoft.hsphere.InitToken;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.plan.ResourceType;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/calc/Calc.class */
public class Calc {
    public static double getDefaultPrice(Resource r, String type) throws Exception {
        return getDefaultPrice(r.getAccount().getPlan(), r.getResourceType(), Session.getAccount().getPeriodId(), type);
    }

    public static double getDefaultPrice(InitToken token, String type) throws Exception {
        return getDefaultPrice(token.getPlan(), token.getResourceType(), token.getPeriodId(), type);
    }

    public static double getDefaultPrice(Plan plan, ResourceType rt, int periodId, String type) throws Exception {
        String price = rt.getValue("_" + type + "_PRICE_");
        if (price == null || "0".equals(price)) {
            return 0.0d;
        }
        double priceValue = USFormat.parseDouble(price);
        String discount = plan.getValue(rt.getId(), "_PERIOD_" + type + "_DISC_" + periodId);
        if (discount == null || "0".equals(discount)) {
            return priceValue;
        }
        double discountValue = USFormat.parseDouble(discount);
        return priceValue - ((priceValue * discountValue) / 100.0d);
    }

    public static double getPrice(Resource r, String type) throws Exception {
        return getPrice(r.getAccount().getPlan(), r.getResourceType(), Session.getAccount().getPeriodId(), type);
    }

    public static double getPrice(InitToken token, String type) throws Exception {
        return getPrice(token.getPlan(), token.getResourceType(), token.getPeriodId(), type);
    }

    public static double getPrice(Plan p, ResourceType rt, int periodId, String type) throws Exception {
        String price = rt.getValue("_" + type + "_PRICE_" + periodId);
        if (price != null) {
            return USFormat.parseDouble(price);
        }
        return Double.NaN;
    }

    public static double getMultiplier() throws Exception {
        return getMultiplier(Session.getAccount().getPlan(), Session.getAccount().getPeriodId());
    }

    public static double getMultiplier(InitToken token) throws Exception {
        return getMultiplier(token.getPlan(), token.getPeriodId());
    }

    public static double getMultiplier(Plan plan, int periodId) throws Exception {
        String type = plan.getValue("_PERIOD_TYPE_" + periodId);
        String size = plan.getValue("_PERIOD_SIZE_" + periodId);
        if (size == null) {
            throw new Exception("Empty size for billing period");
        }
        double sizeVal = USFormat.parseDouble(size);
        if (type.equals("MONTH")) {
            return sizeVal;
        }
        if (type.equals("YEAR")) {
            return sizeVal * 12.0d;
        }
        if (type.equals("WEEK")) {
            return (sizeVal * 7.0d) / 30.416666666666668d;
        }
        if (type.equals("DAY")) {
            return (sizeVal * 12.0d) / 365.0d;
        }
        return Double.NaN;
    }
}
