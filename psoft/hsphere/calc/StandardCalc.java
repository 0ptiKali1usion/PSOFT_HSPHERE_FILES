package psoft.hsphere.calc;

import java.util.Date;
import psoft.hsphere.Account;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/calc/StandardCalc.class */
public class StandardCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        double multiplier = 1.0d;
        double priceValue = Calc.getPrice(r, "UNIT");
        if (Double.isNaN(priceValue)) {
            priceValue = Calc.getDefaultPrice(r, "UNIT");
            if (!r.getId().isMonthly()) {
                multiplier = Calc.getMultiplier();
            }
        } else if (r.getId().isMonthly()) {
            priceValue /= Calc.getMultiplier();
        }
        return calc(multiplier * priceValue, r, begin, end);
    }

    protected static double calc(double price, Resource r, Date begin, Date end) {
        Account a = Session.getAccount();
        try {
            if (r.getId().isMonthly()) {
                Session.getLog().info("Resource is monthly\n Time dif: \nPeriod size is Month\nPrice: " + price);
            } else {
                Session.getLog().info("TimeUtils dif: " + (end.getTime() - begin.getTime()) + "\nTime ration: " + ((end.getTime() - begin.getTime()) / a.getPeriodSize()) + "\nPeriod size" + a.getPeriodSize() + "\nPrice: " + price);
                price *= (end.getTime() - begin.getTime()) / a.getPeriodSize();
            }
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
            price = 0.0d;
        }
        return Math.rint((price * r.getRecurrentMultiplier()) * 100.0d) / 100.0d;
    }

    public static double calc(InitToken token) throws Exception {
        double multiplier = 1.0d;
        double priceValue = Calc.getPrice(token, "UNIT");
        if (Double.isNaN(priceValue)) {
            priceValue = Calc.getDefaultPrice(token, "UNIT");
            if (!token.getResourceType().isMonthly()) {
                multiplier = Calc.getMultiplier(token);
            }
        } else if (token.getResourceType().isMonthly()) {
            priceValue /= Calc.getMultiplier(token);
        }
        return calc(multiplier * priceValue, token);
    }

    public static double calc(double price, InitToken token) throws Exception {
        try {
            if (token.getResourceType().isMonthly()) {
                Session.getLog().info("Resource is monthly\n Time dif: \nPeriod size is Month\nPrice: " + price);
            } else {
                Session.getLog().info("Time dif: " + (token.getEndDate().getTime() - token.getStartDate().getTime()) + "\nTime ration: " + ((token.getEndDate().getTime() - token.getStartDate().getTime()) / token.getPeriodSize()) + "\nPeriod size" + token.getPeriodSize() + "\nPrice: " + price);
                price *= (token.getEndDate().getTime() - token.getStartDate().getTime()) / token.getPeriodSize();
            }
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + token.getPeriodSize(), ex);
            price = 0.0d;
        }
        return Math.rint((price * token.getRecurrentMultiplier()) * 100.0d) / 100.0d;
    }
}
