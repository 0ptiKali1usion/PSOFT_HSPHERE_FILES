package psoft.hsphere.monitoring;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/Monitorable.class */
public interface Monitorable {
    void check() throws Exception;

    void fix() throws Exception;

    void println(Object obj) throws Exception;
}
