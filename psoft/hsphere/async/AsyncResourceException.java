package psoft.hsphere.async;

/* loaded from: hsphere.zip:psoft/hsphere/async/AsyncResourceException.class */
public class AsyncResourceException extends Exception {
    int code;
    public static final int UNKNOWN = 100;
    public static final int NONE = 0;

    public AsyncResourceException(int code, String message) {
        super(message);
        this.code = code;
    }

    public AsyncResourceException(int code, Throwable t) {
        super(t.getMessage());
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }
}
