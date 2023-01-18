package psoft.p000db;

/* renamed from: psoft.db.DatabasePull */
/* loaded from: hsphere.zip:psoft/db/DatabasePull.class */
public class DatabasePull {
    protected Database[] dbs;
    protected int[] useCount;
    protected boolean[] exclusive;
    protected int maxUseCount;
    protected int counter = 0;
    protected boolean allBusy = false;

    public DatabasePull(Database[] db, int maxUseCount) {
        this.maxUseCount = maxUseCount;
        this.useCount = new int[db.length];
        this.exclusive = new boolean[db.length];
        for (int i = 0; i < db.length; i++) {
            this.useCount[i] = 0;
            this.exclusive[i] = false;
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void release(int count) {
        int[] iArr = this.useCount;
        iArr[count] = iArr[count] - 1;
        this.allBusy = false;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public synchronized void releaseExclusive(int count) {
        this.exclusive[count] = false;
        release(count);
    }

    public SharedDatabase getExclusive() {
        int startState = this.counter;
        int smallest = startState;
        synchronized (this) {
            do {
                if (this.exclusive[this.counter]) {
                    next();
                } else {
                    if (this.useCount[this.counter] < this.useCount[smallest]) {
                        smallest = this.counter;
                        if (this.useCount[smallest] == 0) {
                            break;
                        }
                    }
                    next();
                }
            } while (this.counter != startState);
            this.exclusive[smallest] = true;
        }
        while (this.useCount[smallest] > 0) {
            Thread.yield();
        }
        int[] iArr = this.useCount;
        int i = smallest;
        iArr[i] = iArr[i] + 1;
        return new SharedDatabase(this, this.dbs[smallest], smallest, true);
    }

    public SharedDatabase get() {
        int startState = this.counter;
        while (true) {
            if (this.allBusy) {
                Thread.yield();
            } else {
                synchronized (this) {
                    do {
                        if (this.exclusive[this.counter]) {
                            next();
                        } else if (this.useCount[this.counter] < this.maxUseCount) {
                            int[] iArr = this.useCount;
                            int i = this.counter;
                            iArr[i] = iArr[i] + 1;
                            SharedDatabase db = new SharedDatabase(this, this.dbs[this.counter], this.counter);
                            next();
                            return db;
                        } else {
                            next();
                        }
                    } while (this.counter != startState);
                    this.allBusy = true;
                }
            }
        }
    }

    protected void next() {
        this.counter++;
        if (this.counter == this.useCount.length) {
            this.counter = 0;
        }
    }
}
