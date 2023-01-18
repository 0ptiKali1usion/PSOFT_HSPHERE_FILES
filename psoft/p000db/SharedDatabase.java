package psoft.p000db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/* renamed from: psoft.db.SharedDatabase */
/* loaded from: hsphere.zip:psoft/db/SharedDatabase.class */
public class SharedDatabase extends AbstractDatabase {

    /* renamed from: db */
    protected Database f1db;
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
        this.f1db = db;
        this.pos = pos;
        this.exclusive = exclusive;
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public Connection getConnection() throws SQLException {
        return this.f1db.getConnection();
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public Statement getStatement() throws SQLException {
        return this.f1db.getStatement();
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public int getNewId() throws SQLException {
        return this.f1db.getNewId();
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public int getNewId(String name) throws SQLException {
        return this.f1db.getNewId(name);
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public String quote(String val) {
        return this.f1db.quote(val);
    }

    @Override // psoft.p000db.AbstractDatabase
    protected void finalize() throws Throwable {
        if (!this.exclusive) {
            this.pull.release(this.pos);
        } else {
            this.pull.releaseExclusive(this.pos);
        }
    }
}
