package psoft.hsphere.promotion.calc;

import java.util.Hashtable;
import psoft.hsphere.Account;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/calc/PromoCalculator.class */
public interface PromoCalculator {
    double getPromoDiscount(Account account, double d);

    void updateData(long j, Hashtable hashtable) throws Exception;

    void delete(long j) throws Exception;
}
