package psoft.hsphere.promotion;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Date;
import psoft.hsphere.Bill;
import psoft.hsphere.Session;
import psoft.hsphere.SignupManager;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/SubsidizedPromotion.class */
public class SubsidizedPromotion {
    private static int PROMO = 2;
    private static int SPONSORED = 1;
    private static int BASIC = 0;

    protected static int isPromoPlan() throws Exception {
        String result = Session.getAccount().getPlan().getValue("_ENABLE_PROMO");
        if ("1".equals(result)) {
            return 1;
        }
        return "2".equals(result) ? 2 : 0;
    }

    protected static String getKey() throws Exception {
        return SignupManager.getValueByUserId(Session.getUser().getId(), "_ci_promo_code");
    }

    public static double process(double amount) {
        try {
            int type = isPromoPlan();
            if (type == 0) {
                return amount;
            }
            String key = getKey();
            return _process(key, amount, type);
        } catch (Exception ex) {
            Session.getLog().warn("Error processing promo", ex);
            return amount;
        }
    }

    private static double _process(String key, double amount, int type) throws Exception {
        String prefix;
        try {
            new String();
            if (type == SPONSORED) {
                prefix = "_SPONSOR_";
            } else {
                prefix = "_PROMO_";
            }
            String discount = Session.getAccount().getPlanValue(prefix + key);
            return (Double.parseDouble(discount) / 100.0d) * amount;
        } catch (Exception ex) {
            Session.getLog().debug("Unable to parse PROMOTIONAL percent", ex);
            return amount;
        }
    }

    public static void record(double amount, Bill bill) {
        String description;
        try {
            int type = isPromoPlan();
            if (type == 0) {
                return;
            }
            String key = getKey();
            double newAmount = amount - _process(key, amount, type);
            if (type == SPONSORED) {
                description = Session.getAccount().getPlan().getValue("_SPONSOR_DESC_" + key);
            } else {
                description = Session.getAccount().getPlan().getValue("_PROMO_DESC_" + key);
            }
            Date tmpDate = TimeUtils.getDate();
            bill.addEntry(6, tmpDate, -1L, -1, description, tmpDate, (Date) null, key, newAmount);
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("INSERT INTO subsidized_promo (account_id, pkey, amount, s_amount, created) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, Session.getAccount().getId().getId());
            ps.setString(2, key);
            ps.setDouble(3, -amount);
            ps.setDouble(4, -newAmount);
            ps.setTimestamp(5, TimeUtils.getSQLTimestamp());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Exception ex) {
            Session.getLog().warn("Error processing promo", ex);
        }
    }
}
