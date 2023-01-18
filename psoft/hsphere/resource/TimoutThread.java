package psoft.hsphere.resource;

import java.net.HttpURLConnection;
import java.util.Date;
import java.util.Hashtable;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/TimoutThread.class */
class TimoutThread extends Thread {
    protected long timeOut;
    protected long delay;
    boolean cont;
    protected Hashtable activeThread;

    TimoutThread(long delay, long timeOut) {
        super("TimeoutThread");
        this.timeOut = timeOut;
        this.delay = delay;
        this.cont = true;
        this.activeThread = new Hashtable();
        setDaemon(true);
    }

    public void die() {
        this.cont = false;
        interrupt();
    }

    public void addThread(HttpURLConnection tr) {
        synchronized (this.activeThread) {
            this.activeThread.put(tr, TimeUtils.getDate());
        }
    }

    public void removeThread(HttpURLConnection tr) {
        synchronized (this.activeThread) {
            this.activeThread.remove(tr);
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        while (true) {
            try {
                TimeUtils.sleep(this.timeOut);
                synchronized (this.activeThread) {
                    for (HttpURLConnection tr : this.activeThread.keySet()) {
                        Date startThread = (Date) this.activeThread.get(tr);
                        if (TimeUtils.currentTimeMillis() - startThread.getTime() > this.delay) {
                            removeThread(tr);
                            tr.disconnect();
                        }
                    }
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                if (!this.cont) {
                    return;
                }
            }
        }
    }
}
