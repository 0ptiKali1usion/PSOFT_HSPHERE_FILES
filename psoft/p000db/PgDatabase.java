package psoft.p000db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/* renamed from: psoft.db.PgDatabase */
/* loaded from: hsphere.zip:psoft/db/PgDatabase.class */
public class PgDatabase extends AbstractDatabase {
    public PgDatabase(String db_string, String login, String password) throws SQLException {
        super("jdbc:postgresql:" + db_string, login, password);
    }

    static {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (Exception e) {
            e.printStackTrace();
            driver_error = e;
        }
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public int getNewId(String name) throws SQLException {
        Statement ps = getStatement();
        try {
            ResultSet rs = ps.executeQuery("SELECT nextval('" + name + "');");
            if (rs.next()) {
                return rs.getInt(1);
            }
            throw new SQLException("Can't obtain next sequence number");
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
            }
        }
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public int getNewId() throws SQLException {
        return getNewId("newid");
    }
}
