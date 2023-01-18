package psoft.hsphere.calc;

import java.util.Date;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/calc/StandardRefundCalc_100.class */
public class StandardRefundCalc_100 {
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
        double sumPrice = 0.0d;
        for (int i = 0; i < entries.size(); i++) {
            BillEntry entry = (BillEntry) entries.get(i);
            Session.getLog().info("Entry -->" + (entry == null ? 0L : entry.getId()));
            double price2 = entry.getAmount();
            Session.getLog().info("Price -->" + price2);
            Date opened = entry.getOpened();
            Date ended = entry.getEnded();
            Date from = new Date(Math.max(opened.getTime(), begin.getTime()));
            Date to = ended;
            if (r.getId().isMonthly()) {
                to = new Date(Math.min(ended.getTime(), end.getTime()));
            }
            if (to.getTime() - from.getTime() > 0) {
                try {
                    Session.getLog().debug("Refund, price=" + price2 + " from:" + from + " to:" + to + " opened:" + opened + " ened:" + ended);
                    price = (price2 * (to.getTime() - from.getTime())) / (ended.getTime() - opened.getTime());
                } catch (ArithmeticException ex) {
                    Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
                    price = 0.0d;
                }
                Session.getLog().debug("Refund proc = 100% refund sum" + price);
                sumPrice += -price;
            }
        }
        if (sumPrice < 0.0d) {
            return Math.rint(sumPrice * 100.0d) / 100.0d;
        }
        return 0.0d;
    }
}
