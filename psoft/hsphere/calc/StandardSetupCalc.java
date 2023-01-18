package psoft.hsphere.calc;

import java.util.Date;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;

/* loaded from: hsphere.zip:psoft/hsphere/calc/StandardSetupCalc.class */
public class StandardSetupCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        double amount = Calc.getPrice(r, "SETUP");
        if (Double.isNaN(amount)) {
            amount = Calc.getDefaultPrice(r, "SETUP");
        }
        return amount * r.getSetupMultiplier();
    }

    public static double calc(InitToken token) throws Exception {
        double amount = Calc.getPrice(token, "SETUP");
        if (Double.isNaN(amount)) {
            amount = Calc.getDefaultPrice(token, "SETUP");
        }
        return amount * token.getSetupMultiplier();
    }
}
