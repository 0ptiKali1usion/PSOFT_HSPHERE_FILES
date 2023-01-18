package psoft.hsphere.calc;

import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/calc/ResellerCalc.class */
public class ResellerCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        double multiplier = 1.0d;
        double price = Session.getReseller().getPrices(r.getId().getType()).getRecurrentPrice();
        if (Double.isNaN(price)) {
            return 0.0d;
        }
        if (!r.getId().isMonthly()) {
            multiplier = Calc.getMultiplier();
        }
        return calc(multiplier * price, r, begin, end);
    }

    protected static double calc(double price, Resource r, Date begin, Date end) {
        Account a = Session.getAccount();
        try {
            if (r.getId().isMonthly()) {
                Session.getLog().info("Resource is chanageable\n Time dif: \nPeriod size is Month\nPrice: " + price);
            } else {
                Session.getLog().info("RESELLER: \nTime dif: " + (end.getTime() - begin.getTime()) + "\nTime ration: " + ((end.getTime() - begin.getTime()) / a.getPeriodSize()) + "\nPeriod size" + a.getPeriodSize() + "\nPrice: " + price);
                price *= (end.getTime() - begin.getTime()) / a.getPeriodSize();
            }
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
            price = 0.0d;
        }
        return Math.rint((price * r.getResellerRecurrentMultiplier()) * 100.0d) / 100.0d;
    }

    public static double calc(InitToken token) throws Exception {
        double multiplier = 1.0d;
        double price = Session.getReseller().getPrices(token.getResourceType().getType()).getRecurrentPrice();
        if (Double.isNaN(price)) {
            return 0.0d;
        }
        if (!token.getResourceType().isMonthly()) {
            multiplier = Calc.getMultiplier();
        }
        return calc(multiplier * price, token);
    }

    public static double calc(double price, InitToken token) throws Exception {
        Date begin = token.getStartDate();
        Date end = token.getEndDate();
        Account a = Session.getAccount();
        try {
            if (token.getResourceType().isMonthly()) {
                Session.getLog().info("Resource is chanageable\n Time dif: \nPeriod size is Month\nPrice: " + price);
            } else {
                Session.getLog().info("RESELLER: \nTime dif: " + (end.getTime() - begin.getTime()) + "\nTime ration: " + ((end.getTime() - begin.getTime()) / token.getPeriodSize()) + "\nPeriod size" + token.getPeriodSize() + "\nPrice: " + price);
                price *= (end.getTime() - begin.getTime()) / a.getPeriodSize();
            }
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + token.getPeriodSize(), ex);
            price = 0.0d;
        }
        return Math.rint((price * token.getResellerRecurrentMultiplier()) * 100.0d) / 100.0d;
    }
}
