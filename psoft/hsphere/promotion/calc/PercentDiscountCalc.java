package psoft.hsphere.promotion.calc;

import java.util.Hashtable;
import psoft.hsphere.Account;
import psoft.hsphere.Session;
import psoft.hsphere.promotion.AbstractPromoDataStorage;
import psoft.util.USFormat;

/* loaded from: hsphere.zip:psoft/hsphere/promotion/calc/PercentDiscountCalc.class */
public class PercentDiscountCalc extends AbstractPromoDataStorage implements PromoCalculator {
    private double discountPercent;

    public PercentDiscountCalc(long promoId) throws Exception {
        super(promoId);
        this.discountPercent = USFormat.parseDouble((String) this.data.get("discount_percent"));
    }

    public PercentDiscountCalc(long promoId, Hashtable data) throws Exception {
        super(promoId, data);
        this.discountPercent = USFormat.parseDouble((String) data.get("discount_percent"));
    }

    @Override // psoft.hsphere.promotion.AbstractPromoDataStorage
    public int getDataType() {
        return 2;
    }

    @Override // psoft.hsphere.promotion.calc.PromoCalculator
    public double getPromoDiscount(Account a, double sum) {
        return (sum * this.discountPercent) / 100.0d;
    }

    @Override // psoft.hsphere.promotion.AbstractPromoDataStorage, psoft.hsphere.promotion.calc.PromoCalculator
    public void updateData(long promoId, Hashtable data) throws Exception {
        super.updateData(promoId, data);
        Session.getLog().debug("Updating discount percent");
        this.discountPercent = USFormat.parseDouble((String) data.get("discount_percent"));
    }
}
