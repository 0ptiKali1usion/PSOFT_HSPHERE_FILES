package psoft.epayment;

import java.text.DateFormat;

/* loaded from: hsphere.zip:psoft/epayment/CreditCard.class */
public interface CreditCard extends PaymentInstrument {
    String getNumber() throws Exception;

    String getCVV() throws Exception;

    String getHiddenNumber();

    String getExp();

    String getExp(DateFormat dateFormat);

    boolean isExpired();

    String getFirstName();

    String getLastName();

    String getName();

    String getCompany();

    String getAddress();

    String getCity();

    String getState();

    String getZip();

    String getCountry();

    @Override // psoft.epayment.PaymentInstrument
    String getType();

    String getEmail();

    String getPhone();

    String getIssueNo();

    String getStartDate();

    String getStartDate(DateFormat dateFormat);

    void setCVVChecked(boolean z);

    short isCVVChecked();
}
