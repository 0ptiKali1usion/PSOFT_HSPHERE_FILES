package psoft.hsphere.monitoring;

import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/WebServerMonitor.class */
public class WebServerMonitor extends MonitorableHost {
    public WebServerMonitor(HostEntry he) {
        super(he);
    }

    @Override // psoft.hsphere.monitoring.MonitorableHost, psoft.hsphere.monitoring.Monitorable
    public void check() throws Exception {
        super.check();
        if (this.host.getStatus() != 0) {
            Monitoring.checkHTTPd(this.host);
            Monitoring.checkFTPd(this.host);
        }
    }
}
