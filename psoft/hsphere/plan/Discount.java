package psoft.hsphere.plan;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/plan/Discount.class */
public class Discount {
    Map discounts;

    protected Discount(Map discounts) {
        this.discounts = new HashMap();
        this.discounts = discounts;
    }

    public String getValue(int id, String key) {
        return getValue(Integer.toString(id), key);
    }

    public String getValue(String id, String key) {
        Map values = (Map) this.discounts.get(id);
        if (values != null) {
            return (String) values.get(key);
        }
        return null;
    }

    public Map getDiscounts() {
        return this.discounts;
    }

    public static Discount getDiscount(long accountId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, type_id, value FROM discount_values WHERE account_id = ?");
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            Map d = new HashMap();
            boolean notEmpty = false;
            while (rs.next()) {
                notEmpty = true;
                String type = rs.getString(2);
                HashMap map = (Map) d.get(type);
                if (map == null) {
                    map = new HashMap();
                    d.put(type, map);
                }
                map.put(rs.getString(1), rs.getString(3));
            }
            if (notEmpty) {
                Discount discount = new Discount(d);
                Session.closeStatement(ps);
                con.close();
                return discount;
            }
            Session.closeStatement(ps);
            con.close();
            return null;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
