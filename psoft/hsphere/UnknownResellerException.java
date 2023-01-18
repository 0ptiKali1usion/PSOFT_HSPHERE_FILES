package psoft.hsphere;

/* loaded from: hsphere.zip:psoft/hsphere/UnknownResellerException.class */
public class UnknownResellerException extends Exception {
    public UnknownResellerException(String message) {
        super(message);
    }

    public UnknownResellerException(Throwable tr) {
        super(tr.getMessage());
    }
}
