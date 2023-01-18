package psoft.hsphere.resource.sshpool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Session;
import psoft.util.Config;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/sshpool/SSHPool.class */
public class SSHPool {
    protected List con = new ArrayList();
    protected static int MAX_CONNECTION_PER_HOST;
    protected long idleTime;
    protected ConnectionThread conThread;
    protected static SSHPool pool = new SSHPool(1200000);

    static {
        MAX_CONNECTION_PER_HOST = 5;
        try {
            MAX_CONNECTION_PER_HOST = Integer.parseInt(Config.getProperty("CLIENT", "MAX_CONNECTION_PER_HOST"));
            Session.getLog().info("Use custom MAX_CONNECTION_PER_HOST = " + MAX_CONNECTION_PER_HOST);
        } catch (Exception e) {
        }
    }

    SSHPool(long idle) {
        this.conThread = new ConnectionThread(idle);
        this.conThread.start();
    }

    public static synchronized SSHConnection get(String ip, String login) throws Exception {
        while (true) {
            int conNumber = 0;
            for (SSHConnection c : pool.con) {
                if (c.isMatch(ip, login)) {
                    conNumber++;
                    if (!c.isBusy()) {
                        c.setBusy(true);
                        return c;
                    }
                }
            }
            if (conNumber < MAX_CONNECTION_PER_HOST) {
                SSHConnection ncon = new SSHConnection(ip, login);
                pool.con.add(ncon);
                return ncon;
            }
            try {
                SSHPool.class.wait();
            } catch (InterruptedException e) {
            }
        }
    }

    public static synchronized void release(SSHConnection con) {
        con.setBusy(false);
        SSHPool.class.notify();
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/hsphere/resource/sshpool/SSHPool$ConnectionThread.class */
    public class ConnectionThread extends Thread {
        protected long maxIdleTime;
        protected long delay;

        /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
        ConnectionThread(long time) {
            super("SSHPool");
            SSHPool.this = r5;
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
                    Iterator i = SSHPool.this.con.iterator();
                    while (true) {
                        if (i.hasNext()) {
                            SSHConnection connection = (SSHConnection) i.next();
                            Session.getLog().debug("SSH connection idle: " + (TimeUtils.currentTimeMillis() - connection.getIdleTime()));
                            if (!connection.isBusy() && TimeUtils.currentTimeMillis() - connection.getIdleTime() >= this.maxIdleTime) {
                                try {
                                    connection.free();
                                } catch (IOException e) {
                                }
                                SSHPool.this.con.remove(connection);
                                Session.getLog().debug("SSH connection has been closed");
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
