package psoft.hsphere.payment;

/* loaded from: hsphere.zip:psoft/hsphere/payment/DuplicatePaymentException.class */
public class DuplicatePaymentException extends Exception {
    public DuplicatePaymentException(long extId) {
        super("Duplicate payment #" + extId);
    }
}
