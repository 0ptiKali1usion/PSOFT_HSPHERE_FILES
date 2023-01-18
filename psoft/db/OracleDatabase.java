package psoft.db;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import oracle.jdbc.pool.OracleConnectionCacheImpl;

/* loaded from: hsphere.zip:psoft/db/OracleDatabase.class */
public class OracleDatabase implements Database {
    protected MessageFormat newIdQueryForm;
    private OracleConnectionCacheImpl ods = new OracleConnectionCacheImpl();

    @Override // psoft.db.Database
    public Statement getStatement() throws SQLException {
        throw new SQLException("Not supported in this driver");
    }

    @Override // psoft.db.Database
    public int getNewId() throws SQLException {
        return (int) getNewIdAsLong();
    }

    @Override // psoft.db.Database
    public int getNewId(String name) throws SQLException {
        return (int) getNewIdAsLong(name);
    }

    @Override // psoft.db.Database
    public void close() throws SQLException {
        this.ods.close();
    }

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    @Override // psoft.db.Database, psoft.util.Destroyable
    public void destroy() {
        try {
            close();
        } catch (Exception e) {
        }
    }

    public OracleDatabase(String url, String login, String password, String newIdQuery) throws SQLException, ClassNotFoundException {
        this.ods.setURL(url);
        this.ods.setUser(login);
        this.ods.setPassword(password);
        this.ods.setMaxLimit(100);
        this.ods.setCacheScheme(2);
        this.newIdQueryForm = new MessageFormat(newIdQuery);
    }

    @Override // psoft.db.Database
    public Connection getTransConnection() throws SQLException {
        Connection con = getConnection();
        con.setAutoCommit(false);
        return con;
    }

    @Override // psoft.db.Database
    public void commitTransConnection(Connection con) throws SQLException {
        con.setAutoCommit(true);
        con.commit();
        con.close();
    }

    @Override // psoft.db.Database
    public Connection getConnection(String type) throws SQLException {
        return getConnection();
    }

    @Override // psoft.db.Database
    public Connection getConnection() throws SQLException {
        return this.ods.getConnection();
    }

    @Override // psoft.db.Database
    public long getNewIdAsLong() throws SQLException {
        return getNewIdAsLong("newid");
    }

    @Override // psoft.db.Database
    public long getNewIdAsLong(String name) throws SQLException {
        Object[] args = {name};
        Connection con = getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(this.newIdQueryForm.format(args));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long j = rs.getLong(1);
                try {
                    ps.close();
                } catch (SQLException e) {
                }
                con.close();
                return j;
            }
            try {
                ps.close();
            } catch (SQLException e2) {
            }
            con.close();
            throw new SQLException("Unable to get new id");
        } catch (Throwable th) {
            try {
                ps.close();
            } catch (SQLException e3) {
            }
            con.close();
            throw th;
        }
    }

    @Override // psoft.db.Database
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

    @Override // psoft.db.Database
    public String quote(Object o) {
        return o == null ? "null" : quote(o.toString());
    }

    @Override // psoft.db.Database
    public String getClobValue(ResultSet rs, int index) throws SQLException {
        StringBuffer result = new StringBuffer();
        try {
            InputStream is = rs.getAsciiStream(index);
            if (is == null) {
                return "";
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String tmp = in.readLine();
                if (tmp == null) {
                    in.close();
                    return result.toString();
                }
                result.append(tmp).append("\n");
            }
        } catch (IOException e) {
            throw new SQLException("Reading CLOB error " + e.getMessage());
        }
    }

    @Override // psoft.db.Database
    public String getClobValue(ResultSet rs, String name) throws SQLException {
        StringBuffer result = new StringBuffer();
        try {
            InputStream is = rs.getAsciiStream(name);
            if (is == null) {
                return "";
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            while (true) {
                String tmp = in.readLine();
                if (tmp == null) {
                    in.close();
                    return result.toString();
                }
                result.append(tmp).append("\n");
            }
        } catch (IOException e) {
            throw new SQLException("Reading CLOB error " + e.getMessage());
        }
    }

    @Override // psoft.db.Database
    public void setClobValue(PreparedStatement ps, int index, String value) throws SQLException {
        byte[] bytes = (value == null ? "" : value).getBytes();
        ps.setAsciiStream(index, (InputStream) new ByteArrayInputStream(bytes), bytes.length);
    }

    @Override // psoft.db.Database
    public void setClobValue(PreparedStatement ps, int index, String value, String encoding) throws SQLException {
        try {
            byte[] bytes = (value == null ? "" : value).getBytes(encoding);
            ps.setAsciiStream(index, (InputStream) new ByteArrayInputStream(bytes), bytes.length);
        } catch (Exception e) {
            throw new SQLException("Writing CLOB error " + e.getMessage());
        }
    }
}
