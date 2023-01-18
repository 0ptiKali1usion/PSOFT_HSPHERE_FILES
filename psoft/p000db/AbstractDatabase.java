package psoft.p000db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Random;
import psoft.hsphere.Session;
import psoft.hsphere.resource.email.AntiSpam;

/* renamed from: psoft.db.AbstractDatabase */
/* loaded from: hsphere.zip:psoft/db/AbstractDatabase.class */
public abstract class AbstractDatabase implements Database {
    protected static Random generator = new Random(5);
    protected Hashtable conns;
    protected boolean state;
    protected static Exception driver_error;
    protected String login;
    protected String password;
    protected String url;
    protected Pool pool;

    @Override // psoft.p000db.Database
    public abstract int getNewId() throws SQLException;

    @Override // psoft.p000db.Database
    public abstract int getNewId(String str) throws SQLException;

    /* JADX INFO: Access modifiers changed from: protected */
    public AbstractDatabase() {
        this.conns = new Hashtable();
        this.pool = new Pool(this, 5);
    }

    @Override // psoft.p000db.Database
    public Connection getTransConnection() throws SQLException {
        return this.pool.get();
    }

    @Override // psoft.p000db.Database
    public void commitTransConnection(Connection con) throws SQLException {
        this.pool.release(con);
    }

    @Override // psoft.p000db.Database
    public Connection getConnection() throws SQLException {
        return getConnection(AntiSpam.DEFAULT_LEVEL_VALUE);
    }

    @Override // psoft.p000db.Database
    public Connection getConnection(String type) throws SQLException {
        if (AntiSpam.DEFAULT_LEVEL_VALUE.equals(type)) {
            type = type + Integer.toString(generator.nextInt(5));
        }
        Connection con = (Connection) this.conns.get(type);
        if (con == null || con.isClosed()) {
            con = createConnection();
            this.conns.put(type, con);
        }
        return new DumbConnection(con);
    }

    @Override // psoft.p000db.Database
    public Statement getStatement() throws SQLException {
        Connection con = getConnection(AntiSpam.DEFAULT_LEVEL_VALUE);
        try {
            return con.createStatement();
        } catch (Exception e) {
            Connection con2 = getConnection(AntiSpam.DEFAULT_LEVEL_VALUE);
            System.err.println("Reconecting ...");
            return con2.createStatement();
        }
    }

    @Override // psoft.p000db.Database
    public long getNewIdAsLong() throws SQLException {
        return getNewId();
    }

    @Override // psoft.p000db.Database
    public long getNewIdAsLong(String name) throws SQLException {
        return getNewId(name);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public Connection createConnection() throws SQLException {
        if (driver_error == null) {
            return DriverManager.getConnection(this.url, this.login, this.password);
        }
        throw new SQLException("No driver: " + driver_error.getMessage());
    }

    public AbstractDatabase(String url, String login, String password) throws SQLException {
        this.conns = new Hashtable();
        init(url, login, password);
    }

    /* JADX INFO: Access modifiers changed from: protected */
    public void init(String url, String login, String password) throws SQLException {
        this.url = url;
        this.login = login;
        this.password = password;
        getConnection(AntiSpam.DEFAULT_LEVEL_VALUE);
    }

    public void close(String type) throws SQLException {
        Connection con = (Connection) this.conns.get(type);
        if (con != null && !con.isClosed()) {
            con.close();
        }
    }

    @Override // psoft.p000db.Database
    public void close() throws SQLException {
        close(AntiSpam.DEFAULT_LEVEL_VALUE);
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override // psoft.p000db.Database, psoft.util.Destroyable
    public void destroy() {
        try {
            close();
        } catch (Exception e) {
        }
    }

    @Override // psoft.p000db.Database
    public String quote(String val) {
        if (val == null) {
            return "null";
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append('\'');
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) == '\'') {
                buffer.append('\'');
            }
            buffer.append(val.charAt(i));
        }
        buffer.append('\'');
        return buffer.toString();
    }

    public String rmZero(String val) {
        if (val == null) {
            return "null";
        }
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < val.length(); i++) {
            if (val.charAt(i) != 0) {
                buffer.append(val.charAt(i));
            }
        }
        return buffer.toString();
    }

    @Override // psoft.p000db.Database
    public String quote(Object o) {
        return o == null ? "null" : quote(o.toString());
    }

    @Override // psoft.p000db.Database
    public String getClobValue(ResultSet rs, int index) throws SQLException {
        return rs.getString(index);
    }

    @Override // psoft.p000db.Database
    public String getClobValue(ResultSet rs, String name) throws SQLException {
        return rs.getString(name);
    }

    @Override // psoft.p000db.Database
    public void setClobValue(PreparedStatement ps, int index, String value) throws SQLException {
        ps.setString(index, rmZero(value));
    }

    @Override // psoft.p000db.Database
    public void setClobValue(PreparedStatement ps, int index, String value, String encoding) throws SQLException {
        try {
            byte[] bytes = (value == null ? "" : value).getBytes(encoding);
            Session.getLog().debug("Str before setCLOB: " + new String(bytes));
            ps.setString(index, new String(bytes));
        } catch (Exception e) {
            Session.getLog().error("CLOB Exception", e);
            throw new SQLException("Writing CLOB error " + e.getMessage());
        }
    }
}
