package psoft.epayment;

import psoft.hsphere.resource.epayment.BillingInfo;

/* loaded from: hsphere.zip:psoft/epayment/PaymentInstrument.class */
public interface PaymentInstrument {
    public static final int TRIAL = -1;
    public static final int NONE = 0;
    public static final int INSTANT = 1;
    public static final int AUTH = 2;
    public static final int EXTERNAL = 3;

    PaymentInstrument copy(BillingInfo billingInfo) throws Exception;

    int getBillingType();

    String getType();

    void checkValid() throws PaymentInstrumentException, Exception;
}
