package psoft.hsphere.tools;

import java.util.LinkedList;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/tools/RepostThread.class */
public class RepostThread extends Thread {
    protected long hostId;
    boolean cont;
    LinkedList actions;

    /* renamed from: cp */
    C0004CP f234cp;

    public long getHostId() {
        return this.hostId;
    }

    public RepostThread(C0004CP cp, long hostId) {
        super("RepostThread:" + hostId);
        this.cont = true;
        this.hostId = hostId;
        this.actions = new LinkedList();
        this.f234cp = cp;
    }

    public void die() {
        this.cont = false;
        interrupt();
    }

    public boolean isWorked() {
        boolean z;
        synchronized (this.actions) {
            z = this.cont && this.actions.size() > 0;
        }
        return z;
    }

    public synchronized void addAction(RepostAction ac) {
        synchronized (this.actions) {
            this.actions.addLast(ac);
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        RepostAction act;
        this.f234cp.setConfig();
        while (true) {
            try {
                TimeUtils.sleep(1000L);
                if (this.actions.size() > 0) {
                    synchronized (this.actions) {
                        try {
                            act = (RepostAction) this.actions.getFirst();
                            this.actions.remove(act);
                        } catch (IndexOutOfBoundsException ex) {
                            Session.getLog().error("IndexOutOfBoundsException", ex);
                        }
                    }
                    try {
                        act.repost();
                    } catch (Exception e) {
                    }
                }
                if (!this.cont) {
                    return;
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
                this.cont = false;
                return;
            }
        }
    }
}
