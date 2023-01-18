package psoft.hsphere.calc;

import java.util.Date;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ResellerSetupCalc.class */
public class ResellerSetupCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        double amount = Session.getReseller().getPrices(r.getId().getType()).getSetupPrice();
        if (Double.isNaN(amount)) {
            return 0.0d;
        }
        return amount * r.getResellerSetupMultiplier();
    }
}
