package psoft.hsphere.calc;

import java.util.Date;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.InitToken;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServer;
import psoft.hsphere.resource.p003ds.DedicatedServerResource;

/* loaded from: hsphere.zip:psoft/hsphere/calc/DedicatedServerCalc.class */
public class DedicatedServerCalc {
    public static double calc(Resource _r, Date begin, Date end) throws Exception {
        double priceValue;
        DedicatedServerResource r = (DedicatedServerResource) _r;
        DedicatedServer ds = r.getDSObject();
        if (!ds.isTemplatedServer()) {
            priceValue = ds.getRecurrent();
        } else {
            priceValue = Calc.getPrice(r, "DST_" + ds.getTemplate().getId() + "_REC");
            if (Double.isNaN(priceValue)) {
                priceValue = Calc.getDefaultPrice(r, "DST_" + ds.getTemplate().getId() + "_REC");
            }
        }
        return calc(1.0d * priceValue, r, begin, end);
    }

    public static double calc(InitToken token) throws Exception {
        double priceValue;
        Iterator i = token.getValues().iterator();
        String _dsoId = (String) i.next();
        long dsoId = Long.parseLong(_dsoId);
        DedicatedServer ds = DSHolder.getDedicatedServerObject(dsoId);
        Session.getLog().debug("DedicatedServerCalc::calc(InitToken) key is DST_" + dsoId + "_REC");
        if (ds.isTemplatedServer()) {
            priceValue = Calc.getPrice(token, "DST_" + dsoId + "_REC");
            if (Double.isNaN(priceValue)) {
                priceValue = Calc.getDefaultPrice(token, "DST_" + dsoId + "_REC");
            }
        } else {
            priceValue = ds.getRecurrent();
        }
        Session.getLog().debug("DedicatedServerCalc::calc(InitToken) priceValue=" + priceValue);
        double multiplier = Calc.getMultiplier(token);
        return calc(multiplier * priceValue, token);
    }

    protected static double calc(double price, Resource r, Date begin, Date end) {
        double price2;
        Account a = Session.getAccount();
        try {
            Session.getLog().info("TimeUtils dif: " + (end.getTime() - begin.getTime()) + "\nTime ration: " + ((end.getTime() - begin.getTime()) / a.getPeriodSize()) + "\nPeriod size" + a.getPeriodSize() + "\nPrice: " + price);
            price2 = price * ((end.getTime() - begin.getTime()) / a.getPeriodSize());
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
            price2 = 0.0d;
        }
        double price3 = price2 * r.getRecurrentMultiplier();
        Session.getLog().debug("DedicatedServerCalc::calc(double, Resource, Date, Date) result is " + price3);
        return Math.rint(price3 * 100.0d) / 100.0d;
    }

    public static double calc(double price, InitToken token) throws Exception {
        double price2;
        try {
            Session.getLog().info("Time dif: " + (token.getEndDate().getTime() - token.getStartDate().getTime()) + "\nTime ration: " + ((token.getEndDate().getTime() - token.getStartDate().getTime()) / token.getPeriodSize()) + "\nPeriod size" + token.getPeriodSize() + "\nPrice: " + price);
            price2 = price * ((token.getEndDate().getTime() - token.getStartDate().getTime()) / token.getPeriodSize());
        } catch (ArithmeticException ex) {
            Session.getLog().debug("Period Size =" + token.getPeriodSize(), ex);
            price2 = 0.0d;
        }
        double price3 = price2 * token.getRecurrentMultiplier();
        Session.getLog().debug("DedicatedServerCalc::calc(double, InitToken) result is " + price3);
        return Math.rint(price3 * 100.0d) / 100.0d;
    }
}
