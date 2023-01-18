package psoft.epayment;

import java.util.HashMap;
import java.util.Map;

/* loaded from: hsphere.zip:psoft/epayment/MerchantGateway.class */
public interface MerchantGateway {
    public static final int CHARGE = 0;
    public static final int AUTH = 1;
    public static final int REFUND = 2;
    public static final int VOID = 3;
    public static final int CAPTURE = 4;

    int getId();

    Map getValues();

    void init(int i, HashMap hashMap) throws Exception;

    HashMap charge(String str, double d, PaymentInstrument paymentInstrument) throws Exception;

    HashMap charge(long j, String str, double d, PaymentInstrument paymentInstrument) throws Exception;

    HashMap authorize(String str, double d, PaymentInstrument paymentInstrument) throws Exception;

    HashMap authorize(long j, String str, double d, PaymentInstrument paymentInstrument) throws Exception;

    HashMap capture(String str, HashMap hashMap, PaymentInstrument paymentInstrument) throws Exception;

    HashMap capture(long j, String str, HashMap hashMap, PaymentInstrument paymentInstrument) throws Exception;

    HashMap voidAuthorize(String str, HashMap hashMap, PaymentInstrument paymentInstrument) throws Exception;

    HashMap voidAuthorize(long j, String str, HashMap hashMap, PaymentInstrument paymentInstrument) throws Exception;

    void setLog(MerchantGatewayLog merchantGatewayLog);

    String getDescription();

    HashMap checkCVV(long j, PaymentInstrument paymentInstrument) throws Exception;
}
