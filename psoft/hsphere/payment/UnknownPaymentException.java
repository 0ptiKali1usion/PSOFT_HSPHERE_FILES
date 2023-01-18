package psoft.hsphere.payment;

/* loaded from: hsphere.zip:psoft/hsphere/payment/UnknownPaymentException.class */
public class UnknownPaymentException extends Exception {
    public UnknownPaymentException(long extId) {
        super("Unknown payment #" + extId);
    }

    public UnknownPaymentException(long reqId, int type) {
        super("Unknown paymen #" + reqId + ":" + type);
    }
}
