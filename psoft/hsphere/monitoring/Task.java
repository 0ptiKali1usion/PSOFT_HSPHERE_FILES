package psoft.hsphere.monitoring;

import psoft.hsphere.Session;
import psoft.util.Config;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/monitoring/Task.class */
public class Task extends Thread {
    Monitorable mon;

    public Task(Monitorable m) {
        this.mon = m;
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        Session.setLog(MonitoringServer.LOG);
        Config.set("CLIENT", MonitoringServer.conf);
        super.run();
        while (true) {
            try {
                try {
                    this.mon.check();
                    this.mon.println("Check Done");
                } catch (Exception ee) {
                    this.mon.println("Something wrong:" + ee.getMessage() + ":");
                    ee.printStackTrace();
                    this.mon.fix();
                }
                System.out.println("--->0");
                TimeUtils.sleep(5000L);
            } catch (Exception ee2) {
                ee2.printStackTrace();
                return;
            }
        }
    }
}
