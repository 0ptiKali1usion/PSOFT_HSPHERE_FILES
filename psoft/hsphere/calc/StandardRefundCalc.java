package psoft.hsphere.calc;

import java.util.Date;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/calc/StandardRefundCalc.class */
public class StandardRefundCalc {
    public static double getPercentage(Resource r) throws Exception {
        Account a = Session.getAccount();
        String per = r.getResourcePlanValue("_REFUND_PRICE_" + a.getPeriodId());
        Session.getLog().debug("Refund percentage =" + per + " _REFUND_PRICE_" + a.getPeriodId());
        if (per != null) {
            return USFormat.parseDouble(per);
        }
        String per2 = r.getResourcePlanValue("_REFUND_PRICE_");
        if (per2 == null) {
            return 100.0d;
        }
        return USFormat.parseDouble(per2);
    }

    public static double calc(Resource r, Date begin, Date end) throws Exception {
        return calc(r, begin, end, null);
    }

    public static double calc(Resource r, Date begin, Date end, List entries) throws Exception {
        double price;
        if (entries == null) {
            return 0.0d;
        }
        if (end.getTime() - begin.getTime() <= 0) {
            Session.getLog().info("Price = 0, due to period size (<=0) ");
            return 0.0d;
        }
        Account a = Session.getAccount();
        double p = getPercentage(r);
        Session.getLog().info("Percentage: " + p);
        double sumPrice = 0.0d;
        for (int i = 0; i < entries.size(); i++) {
            BillEntry entry = (BillEntry) entries.get(i);
            Session.getLog().info("Entry -->" + (entry == null ? 0L : entry.getId()));
            double price2 = entry.getAmount();
            Session.getLog().info("Price -->" + price2);
            Date opened = new Date(entry.getStarted().getTime());
            Date ended = new Date(entry.getEnded().getTime());
            Date from = new Date(Math.max(opened.getTime(), begin.getTime()));
            Date to = ended;
            if (r.getId().isMonthly()) {
                to = new Date(Math.min(ended.getTime(), end.getTime()));
            }
            if (to.getTime() - from.getTime() > 0) {
                try {
                    Session.getLog().debug("Refund, price=" + price2 + " from:" + from + " to:" + to + " opened:" + opened + " ened:" + ended + " percent:" + p + " SumRefund:" + sumPrice);
                    price = (((price2 * (to.getTime() - from.getTime())) / (ended.getTime() - opened.getTime())) * p) / 100.0d;
                } catch (ArithmeticException ex) {
                    Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
                    price = 0.0d;
                }
                Session.getLog().debug("Refund proc =" + p + " refund sum" + price + " Sum refund:" + sumPrice);
                sumPrice += -price;
            }
        }
        if (sumPrice < 0.0d) {
            return Math.rint(sumPrice * 100.0d) / 100.0d;
        }
        return 0.0d;
    }
}
