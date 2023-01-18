package psoft.hsphere.promotion;

import java.util.Hashtable;
import psoft.hsphere.Account;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/SimplePromo.class */
public class SimplePromo extends AbstractPromoDataStorage implements PromoValidator {
    public SimplePromo(long promoId) throws Exception {
        super(promoId);
    }

    public SimplePromo(long promoId, Hashtable data) throws Exception {
        super(promoId, data);
    }

    @Override // psoft.hsphere.promotion.AbstractPromoDataStorage
    public int getDataType() {
        return 1;
    }

    @Override // psoft.hsphere.promotion.PromoValidator
    public boolean isPromoValid(Account a, int rt) {
        return true;
    }
}
