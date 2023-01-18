package psoft.hsphere.monitoring;

import psoft.hsphere.resource.HostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/MailServerMonitor.class */
public class MailServerMonitor extends MonitorableHost {
    public MailServerMonitor(HostEntry he) {
        super(he);
    }

    @Override // psoft.hsphere.monitoring.MonitorableHost, psoft.hsphere.monitoring.Monitorable
    public void check() throws Exception {
        super.check();
        if (this.host.getStatus() != 0) {
            Monitoring.checkMail(this.host);
        }
    }
}
