package psoft.util;

/* loaded from: hsphere.zip:psoft/util/URLCheckResult.class */
public class URLCheckResult {

    /* renamed from: OK */
    public static final int f257OK = 0;
    public static final int UNABLE_FIND_RESOURCE_ERROR = 1;
    public static final int UNKNOWN_HOST_ERROR = 2;
    public static final int UNKNOWN_PROTOCOL_ERROR = 3;
    public static final int EMPTY_RESOURCE_URL_ERROR = 4;
    public static final int UNKNOWN_ERROR = 5;
    public static final int DESCRIBED_ERROR = 10;
    public static URLCheckResult OK_RESULT = new URLCheckResult(0);
    protected int result;
    protected String message;

    public URLCheckResult(int resultCode) {
        this.result = resultCode;
        this.message = null;
    }

    public URLCheckResult(int resultCode, String message) {
        this.result = resultCode;
        this.message = message;
    }

    public URLCheckResult(String message) {
        this.result = 10;
        this.message = message;
    }

    public boolean isResultOK() {
        return this.result == 0;
    }

    public int getResult() {
        return this.result;
    }

    public String getMessage() {
        return this.message;
    }
}
