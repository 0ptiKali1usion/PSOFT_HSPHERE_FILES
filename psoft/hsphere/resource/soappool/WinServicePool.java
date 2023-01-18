package psoft.hsphere.resource.soappool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinResourceService;
import psoft.hsphere.axis.WinService;
import psoft.util.Config;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/soappool/WinServicePool.class */
public class WinServicePool {
    protected ConnectionThread conThread;
    protected static WinServicePool pool;
    protected int MAX_CONNECTION_PER_HOST = 5;
    protected List con = new ArrayList();

    static {
        pool = null;
        pool = new WinServicePool(1200000L);
        try {
            pool.MAX_CONNECTION_PER_HOST = Integer.parseInt(Config.getProperty("CLIENT", "MAX_CONNECTION_PER_HOST"));
            Session.getLog().info("Use custom MAX_CONNECTION_PER_HOST = " + pool.MAX_CONNECTION_PER_HOST);
        } catch (Exception e) {
        }
    }

    WinServicePool(long idle) {
        this.conThread = null;
        this.conThread = new ConnectionThread(idle);
        this.conThread.start();
    }

    public static synchronized WinResourceService get(String ip, int port, String login, String password) throws Exception {
        while (true) {
            int conNumber = 0;
            for (WinResourceService service : pool.con) {
                if (service.isMatch(ip)) {
                    conNumber++;
                    if (!service.isBusy()) {
                        service.setBusy(true);
                        return service;
                    }
                }
            }
            if (conNumber < pool.MAX_CONNECTION_PER_HOST) {
                WinResourceService service2 = new WinResourceService(ip, port, login, password);
                service2.setBusy(true);
                pool.con.add(service2);
                return service2;
            }
            try {
                WinServicePool.class.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public static synchronized void release(WinService service) {
        service.setBusy(false);
        WinServicePool.class.notify();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/soappool/WinServicePool$ConnectionThread.class */
    public class ConnectionThread extends Thread {
        protected long maxIdleTime;
        protected long delay;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ConnectionThread(long time) {
            super("WinServicePool");
            WinServicePool.this = r5;
            this.delay = 300000L;
            this.maxIdleTime = time;
            setPriority(1);
            setDaemon(true);
        }

        @Override // java.lang.Thread, java.lang.Runnable
        public void run() {
            while (true) {
                try {
                    TimeUtils.sleep(this.delay);
                    Iterator i = WinServicePool.this.con.iterator();
                    while (true) {
                        if (i.hasNext()) {
                            WinResourceService service = (WinResourceService) i.next();
                            Session.getLog().debug("SOAP connection to Windows box idle: " + (TimeUtils.currentTimeMillis() - service.getIdleTime()));
                            if (!service.isBusy() && TimeUtils.currentTimeMillis() - service.getIdleTime() >= this.maxIdleTime) {
                                try {
                                    service.free();
                                } catch (IOException e) {
                                }
                                WinServicePool.this.con.remove(service);
                                Session.getLog().debug("SOAP connection to Windows box has been closed");
                                break;
                            }
                        }
                    }
                } catch (InterruptedException e2) {
                    Session.getLog().debug("ConnectionThread is finished");
                    return;
                }
            }
        }
    }
}
