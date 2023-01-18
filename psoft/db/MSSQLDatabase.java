package psoft.db;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/* loaded from: hsphere.zip:psoft/db/MSSQLDatabase.class */
public class MSSQLDatabase extends AbstractDatabase {
    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public int getNewId() throws SQLException {
        return (int) getNewIdAsLong();
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public int getNewId(String name) throws SQLException {
        return (int) getNewIdAsLong(name);
    }

    static {
        try {
            Class.forName("com.thinweb.tds.Driver");
        } catch (Exception e) {
            e.printStackTrace();
            driver_error = e;
        }
    }

    public MSSQLDatabase(String url, String login, String password) throws SQLException, ClassNotFoundException {
        this.url = url;
        this.login = login;
        this.password = password;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // psoft.db.AbstractDatabase
    public Connection createConnection() throws SQLException {
        if (driver_error == null) {
            return DriverManager.getConnection(this.url, this.login, this.password);
        }
        throw new SQLException("No driver:" + driver_error.getMessage());
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public Connection getTransConnection() throws SQLException {
        return getConnection();
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public long getNewIdAsLong(String name) throws SQLException {
        Statement stmt = getStatement();
        stmt.execute("declare @i int;  exec @i = nextval " + name + "; select @i");
        ResultSet rs = stmt.getResultSet();
        rs.next();
        int rez = rs.getInt(1);
        stmt.close();
        return rez;
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public long getNewIdAsLong() throws SQLException {
        return getNewIdAsLong("newid");
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
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

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public String quote(Object o) {
        return o == null ? "null" : quote(o.toString());
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
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

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
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

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public void setClobValue(PreparedStatement ps, int index, String value) throws SQLException {
        byte[] bytes = (value == null ? "" : value).getBytes();
        ps.setAsciiStream(index, (InputStream) new ByteArrayInputStream(bytes), bytes.length);
    }
}
