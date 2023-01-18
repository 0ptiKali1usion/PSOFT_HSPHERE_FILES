package psoft.hsphere.export;

import java.util.Date;
import psoft.epayment.PaymentInstrument;
import psoft.hsphere.resource.epayment.BillingInfo;
import psoft.hsphere.resource.epayment.ContactInfo;

/* loaded from: hsphere.zip:psoft/hsphere/export/Export.class */
public interface Export {
    void onAddUser(long j, String str, String str2, long j2);

    void onChangePassword(long j, String str);

    void onDeleteUser(long j);

    void OnAddAccount(long j, long j2, long j3, int i) throws ForbiddenActionException;

    void OnChangePlan(long j, long j2) throws ForbiddenActionException;

    void OnBillingPeriodChange(long j, int i) throws ForbiddenActionException;

    void onSuspendAccount(long j);

    void onDeleteAccount(long j);

    void OnAddResource(long j, long j2, int i) throws ForbiddenActionException;

    void onDeleteResource(long j);

    void onNewBillingEntry(long j, long j2, int i, Date date, long j3, int i2, String str, double d, Date date2, Date date3, Date date4, double d2);

    void onNewBill(long j, long j2);

    void onSetContactInfo(long j, ContactInfo contactInfo);

    void onSetBillingInfo(long j, BillingInfo billingInfo);

    void OnSetPaymentInfo(long j, PaymentInstrument paymentInstrument);

    double getBalance(long j) throws ImportException;

    boolean isGetBalance();
}
