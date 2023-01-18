package psoft.hsphere.resource.registrar;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/AsyncTLD.class */
public class AsyncTLD {
    int checkInterval;
    int timeout;

    public AsyncTLD(int checkInterval, int timeout) {
        this.checkInterval = checkInterval;
        this.timeout = timeout;
    }

    public int getCheckInterval() {
        return this.checkInterval;
    }

    public int getTimeout() {
        return this.timeout;
    }
}
