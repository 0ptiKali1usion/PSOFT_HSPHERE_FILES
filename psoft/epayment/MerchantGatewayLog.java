package psoft.epayment;

import java.util.Date;

/* loaded from: hsphere.zip:psoft/epayment/MerchantGatewayLog.class */
public interface MerchantGatewayLog {
    long write(long j, long j2, double d, int i, String str, String str2, String str3, boolean z, int i2);

    void transaction(int i, double d, Date date, int i2);
}
