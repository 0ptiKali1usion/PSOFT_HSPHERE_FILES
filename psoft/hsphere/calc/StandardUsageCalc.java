package psoft.hsphere.calc;

import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/calc/StandardUsageCalc.class */
public class StandardUsageCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        double price;
        double price2;
        Account a = Session.getAccount();
        if (end.getTime() - begin.getTime() <= 0 || a.getPeriodSize() <= 0) {
            Session.getLog().info("Price = 0, due to period size (<=0) ");
            return 0.0d;
        }
        double price3 = Calc.getPrice(r, "USAGE");
        if (Double.isNaN(price3)) {
            price = Calc.getDefaultPrice(r, "USAGE");
        } else {
            price = price3 / Calc.getMultiplier();
        }
        Session.getLog().debug("Usage calc price 1 =" + price + "\nUsage calc end =" + end.getTime() + " - begin = " + begin.getTime() + ", period = " + a.getPeriodSize());
        try {
            if (!r.getId().isMonthly()) {
                price *= (end.getTime() - begin.getTime()) / a.getPeriodSize();
            }
            price2 = price * r.getUsageMultiplier();
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
            price2 = 0.0d;
        }
        Session.getLog().debug("Usage calc price7 =" + price2);
        return Math.rint(price2 * 100.0d) / 100.0d;
    }
}
