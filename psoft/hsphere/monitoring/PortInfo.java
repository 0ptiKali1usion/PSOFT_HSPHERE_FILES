package psoft.hsphere.monitoring;

/* JADX INFO: Access modifiers changed from: package-private */
/* loaded from: hsphere.zip:psoft/hsphere/monitoring/PortInfo.class */
public class PortInfo {

    /* renamed from: ip */
    protected String f102ip;
    protected int blade;
    protected int port;

    public PortInfo(String ip, int blade, int port) {
        this.f102ip = ip;
        this.blade = blade;
        this.port = port;
    }

    public String getIP() {
        return this.f102ip;
    }

    public int getBlade() {
        return this.blade;
    }

    public int getPort() {
        return this.port;
    }
}
