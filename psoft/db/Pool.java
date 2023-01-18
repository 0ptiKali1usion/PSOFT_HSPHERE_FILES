package psoft.db;

import java.sql.Connection;
import java.sql.SQLException;

/* loaded from: hsphere.zip:psoft/db/Pool.class */
class Pool {
    Connection[] con;
    boolean[] usage;
    AbstractDatabase db;

    public Pool(AbstractDatabase db, int size) {
        this.con = new Connection[size];
        this.usage = new boolean[size];
        this.db = db;
    }

    Connection getConnection(int i) throws SQLException {
        if (this.con[i] == null || this.con[i].isClosed()) {
            this.con[i] = this.db.createConnection();
        }
        this.usage[i] = true;
        this.con[i].setAutoCommit(false);
        return this.con[i];
    }

    public Connection get() throws SQLException {
        while (true) {
            synchronized (this) {
                for (int i = 0; i < this.usage.length; i++) {
                    if (!this.usage[i]) {
                        return getConnection(i);
                    }
                }
            }
            synchronized (this) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public synchronized void release(Connection c) throws SQLException {
        if (c == null) {
            return;
        }
        int i = 0;
        while (true) {
            if (i >= this.usage.length) {
                break;
            } else if (this.con[i] != c) {
                i++;
            } else {
                this.usage[i] = false;
                notify();
                break;
            }
        }
        c.commit();
    }
}
