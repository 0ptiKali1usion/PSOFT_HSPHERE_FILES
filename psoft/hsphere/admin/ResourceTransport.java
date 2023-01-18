package psoft.hsphere.admin;

import java.sql.Timestamp;

/* loaded from: hsphere.zip:psoft/hsphere/admin/ResourceTransport.class */
public interface ResourceTransport {
    public static final int PROCESS_STARTED = 1;
    public static final int PROCESS_RESTARTED = 2;
    public static final int PROCESS_FINISHED = 3;
    public static final int PROCESS_RUNNING = 4;
    public static final int PROCESS_FAILED = -1;
    public static final int PROCESS_MISSCONFIGURED = -2;
    public static final int LOW_LEVEL_TRANSPORT_FAILURE = -3;

    boolean configure() throws Exception;

    int executeTransfer() throws Exception;

    int executeTransfer(boolean z) throws Exception;

    long getId();

    Timestamp getStarted();

    Timestamp getFinished();

    boolean isFinished();

    void delete() throws Exception;
}
