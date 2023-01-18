package psoft.hsphere.calc;

import java.util.Date;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.BillEntry;
import psoft.hsphere.Resource;
import psoft.hsphere.Session;
import psoft.hsphere.resource.BackupResource;

/* loaded from: hsphere.zip:psoft/hsphere/calc/BackupRefundCalc.class */
public class BackupRefundCalc extends StandardRefundCalc {
    public static double calc(Resource r, Date begin, Date end, List entries) throws Exception {
        double price;
        if (entries == null) {
            return 0.0d;
        }
        BackupResource br = (BackupResource) r;
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
            Date opened = entry.getOpened();
            Date ended = entry.getEnded();
            Date from = new Date(Math.max(opened.getTime(), begin.getTime()));
            try {
                Session.getLog().debug("Refund, price=" + price2 + " from:" + from + " to:" + ended + " opened:" + opened + " ened:" + ended + " percent:" + p + " SumRefund:" + sumPrice);
                price = (price2 * p) / 100.0d;
            } catch (ArithmeticException ex) {
                Session.getLog().debug("Period Size =" + a.getPeriodSize(), ex);
                price = 0.0d;
            }
            Session.getLog().debug("Refund proc =" + p + " refund sum" + price + " Sum refund:" + sumPrice);
            sumPrice += -price;
        }
        int completedBackupTasks = br.getCompletedScheduledTasksAmount();
        int scheduledBackupTasks = br.getScheduledBackupTasksAmount();
        if (scheduledBackupTasks == 0) {
            return 0.0d;
        }
        double sumPrice2 = sumPrice - ((sumPrice / scheduledBackupTasks) * completedBackupTasks);
        if (sumPrice2 < 0.0d) {
            return Math.rint(sumPrice2 * 100.0d) / 100.0d;
        }
        return 0.0d;
    }
}
