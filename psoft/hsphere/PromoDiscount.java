package psoft.hsphere;

/* loaded from: hsphere.zip:psoft/hsphere/PromoDiscount.class */
public class PromoDiscount {
    private double discount;
    private String description;
    private long promoId;

    public PromoDiscount(double discount, String description, long promoId) {
        this.discount = discount;
        this.description = description;
        this.promoId = promoId;
    }

    public double getDiscount() {
        return this.discount;
    }

    public String getDescription() {
        return this.description + " : ";
    }

    public long getPromoId() {
        return this.promoId;
    }
}
