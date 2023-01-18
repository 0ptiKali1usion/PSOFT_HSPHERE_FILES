package psoft.hsphere.resource.registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/RegistrarException.class */
public class RegistrarException extends Exception {
    int code;
    String fullRegistrarResponse;
    public static final int NOT_IMPLEMENTED = -1000;
    public static final int EMPTY_RESPONSE = -1001;
    public static final int UNKNOWN_HOST = -2002;
    public static final int UNABLE_TO_CONNECT = -2003;
    public static final int IOERROR = -1004;
    public static final int MAILFORMED_RESPONSE = -2005;
    public static final int OPERATION_FAILED = -2006;
    public static final int AUTH_ERROR = -2007;
    public static final int OTHER_ERROR = -2008;

    public RegistrarException(int code, String text, String fullRegistrarResponse) {
        super(text);
        this.fullRegistrarResponse = fullRegistrarResponse;
        this.code = code;
    }

    public RegistrarException(int code, String text) {
        super(code < 2000 ? text + ":" + code : text);
        this.code = code;
    }

    public int getCode() {
        return this.code;
    }

    public String getfullRegistrarResponse() {
        return this.fullRegistrarResponse;
    }
}
