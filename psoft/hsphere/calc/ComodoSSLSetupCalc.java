package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import org.apache.log4j.Category;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.fmacl.ComodoManager;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.ssl.ComodoSSLResource;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ComodoSSLSetupCalc.class */
public class ComodoSSLSetupCalc {
    private static final Category log = Category.getInstance(ComodoSSLSetupCalc.class.getName());

    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        ComodoSSLResource r = (ComodoSSLResource) _r;
        int period = r.getPeriod();
        String product = r.getProduct();
        return getAmountFinal(getAmount(r.getResourceType(), period, product), period, product);
    }

    public static double calc(InitToken token) throws Exception {
        Iterator i = token.getValues().iterator();
        String productPeriod = (String) i.next();
        Session.getLog().debug("PRODUCT PERIOD=" + productPeriod);
        int pos = productPeriod.indexOf(95);
        int period = Integer.parseInt(productPeriod.substring(pos + 1));
        String product = productPeriod.substring(0, pos);
        return getAmountFinal(getAmount(token.getResourceType(), period, product), period, product);
    }

    private static double getAmountFinal(double amount, int period, String product) throws Exception {
        if (Double.isNaN(amount)) {
            amount = ComodoManager.getPrice(period, product);
        }
        if (Double.isNaN(amount)) {
            throw new Exception("Price is not set for product: " + Localizer.translateLabel("comodossl.product_") + " and period of " + period + " years");
        }
        return amount;
    }

    protected static double getAmount(ResourceType rt, int period, String product) {
        if (period <= 0) {
            return 0.0d;
        }
        double result = Double.NaN;
        try {
        } catch (Exception e) {
            log.debug("Unable to get amount from plan settings!", e);
        }
        if (Plan.getPlan(rt.getPlanId()).isWitoutBilling()) {
            return 0.0d;
        }
        String price = rt.getValue("_SSL_" + period + "_" + product);
        if (price == null || price.length() == 0) {
            return Double.NaN;
        }
        result = Double.parseDouble(price);
        return result;
    }
}
