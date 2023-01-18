package psoft.hsphere.calc;

import java.util.Date;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/calc/StandardMoneyBackCalc.class */
public class StandardMoneyBackCalc {
    public static double calc(Resource r, Date begin, Date end) throws Exception {
        Account a = Session.getAccount();
        int day_back = -1;
        try {
            day_back = Integer.parseInt(a.getPlan().getValue("MONEY_BACK_DAYS"));
        } catch (NumberFormatException e) {
        }
        if ((TimeUtils.currentTimeMillis() - a.getCreated().getTime()) / 86400000 > day_back) {
            throw new HSUserException("standardmoneybackcalc.late");
        }
        double price = 0.0d;
        for (ResourceId resourceId : a.getChildManager().getAllResources()) {
            price += getRecurrentPrice(a, resourceId);
        }
        Session.getLog().debug("MoneyBack result=" + price);
        return Math.rint((-price) * 100.0d) / 100.0d;
    }

    public static double getRecurrentPrice(Account a, ResourceId rId) throws Exception {
        double d;
        List entries = a.getRefundedEntry(rId, new Date(), a.getPeriodEnd());
        double sumPrice = 0.0d;
        if (entries != null) {
            for (int i = 0; i < entries.size(); i++) {
                BillEntry entry = (BillEntry) entries.get(i);
                Session.getLog().info("Entry --> " + (entry == null ? 0L : entry.getId()));
                if (entry == null || entry.getAmount() == 0.0d) {
                    d = 0.0d;
                } else {
                    d = entry.getAmount();
                }
                double price = d;
                Session.getLog().info("Price --> " + price);
                sumPrice += price;
            }
        }
        return sumPrice;
    }
}
