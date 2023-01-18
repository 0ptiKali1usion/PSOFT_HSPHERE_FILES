package psoft.hsphere.calc;

import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ResellerUsageCalc.class */
public class ResellerUsageCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        double price;
        Account a = Session.getAccount();
        if (end.getTime() - begin.getTime() <= 0 || a.getPeriodSize() <= 0) {
            Session.getLog().info("Price = 0, due to period size (<=0) ");
            return 0.0d;
        }
        double price2 = Session.getReseller().getPrices(r.getId().getType()).getUsagePrice();
        if (Double.isNaN(price2)) {
            return 0.0d;
        }
        double multiplier = Calc.getMultiplier();
        double price3 = price2 * multiplier;
        Session.getLog().debug("RESELLER:\nUsage calc price 1 =" + price3 + "\nUsage calc end =" + end.getTime() + " - begin = " + begin.getTime() + ", period = " + a.getPeriodSize());
        try {
            price = price3 * ((end.getTime() - begin.getTime()) / a.getPeriodSize()) * r.getResellerUsageMultiplier();
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
            price = 0.0d;
        }
        Session.getLog().debug("Usage calc price7 =" + price);
        return Math.rint(price * 100.0d) / 100.0d;
    }
}
