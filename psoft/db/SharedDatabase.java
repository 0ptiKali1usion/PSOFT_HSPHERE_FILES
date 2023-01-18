package psoft.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/* loaded from: hsphere.zip:psoft/db/SharedDatabase.class */
public class SharedDatabase extends AbstractDatabase {
    protected Database db;
    protected int pos;
    protected boolean exclusive;
    protected DatabasePull pull;

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedDatabase(DatabasePull pull, Database db, int pos) {
        this(pull, db, pos, false);
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    public SharedDatabase(DatabasePull pull, Database db, int pos, boolean exclusive) {
        this.pull = pull;
        this.db = db;
        this.pos = pos;
        this.exclusive = exclusive;
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public Connection getConnection() throws SQLException {
        return this.db.getConnection();
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public Statement getStatement() throws SQLException {
        return this.db.getStatement();
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public int getNewId() throws SQLException {
        return this.db.getNewId();
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public int getNewId(String name) throws SQLException {
        return this.db.getNewId(name);
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public String quote(String val) {
        return this.db.quote(val);
    }

    @Override // psoft.db.AbstractDatabase
    protected void finalize() throws Throwable {
        if (!this.exclusive) {
            this.pull.release(this.pos);
        } else {
            this.pull.releaseExclusive(this.pos);
        }
    }
}
