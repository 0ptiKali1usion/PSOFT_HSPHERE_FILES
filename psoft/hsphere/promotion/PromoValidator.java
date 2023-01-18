package psoft.hsphere.promotion;

import java.util.Hashtable;
import psoft.hsphere.Account;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/PromoValidator.class */
public interface PromoValidator {
    boolean isPromoValid(Account account, int i);

    void updateData(long j, Hashtable hashtable) throws Exception;

    void delete(long j) throws Exception;
}
