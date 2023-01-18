package psoft.hsphere.resource.registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/DomainTransferStatus.class */
public class DomainTransferStatus {
    public static final int PENDING_OWNER = 0;
    public static final int PENDING_ADMIN = 1;
    public static final int PENDING_REGISTRY = 2;
    public static final int COMPLETED = 3;
    public static final int CANCELED = 4;
    public static final int UNDEF = 5;
    public static final int UNKNOWN = 6;
    protected int status;
    protected String reason;

    public DomainTransferStatus(int status, String reason) {
        this.status = status;
        this.reason = reason;
    }

    public int getStatus() {
        return this.status;
    }

    public String getReason() {
        return this.reason;
    }
}
