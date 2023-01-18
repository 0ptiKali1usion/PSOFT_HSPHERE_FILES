package psoft.epayment;

/* loaded from: hsphere.zip:psoft/epayment/CreditCardExpiredException.class */
public class CreditCardExpiredException extends CreditCardException {
    public CreditCardExpiredException(String expDate) {
        super("Credit Card Expired, Exp Date: " + expDate);
    }
}
