package psoft.hsphere.async;

/* loaded from: hsphere.zip:psoft/hsphere/async/AsyncDeclinedException.class */
public class AsyncDeclinedException extends AsyncResourceException {
    public AsyncDeclinedException(int code, String message) {
        super(code, message);
    }

    public AsyncDeclinedException(int code, Throwable t) {
        super(code, t);
    }
}
