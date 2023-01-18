package psoft.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/* loaded from: hsphere.zip:psoft/db/NoSequenceDatabase.class */
public class NoSequenceDatabase extends AbstractDatabase {
    public NoSequenceDatabase(String drv, String url, String login, String pass) throws SQLException, ClassNotFoundException {
        Class.forName(drv);
        init(url, login, pass);
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public synchronized int getNewId(String name) throws SQLException {
        Statement ps = getStatement();
        try {
            ResultSet rs = ps.executeQuery("SELECT id FROM " + name);
            int id = 0;
            if (rs.next()) {
                id = rs.getInt(1);
            } else {
                ps.executeUpdate("INSERT INTO " + name + " VALUES(0)");
            }
            int id2 = id + 1;
            ps.executeUpdate("UPDATE " + name + " SET id = " + id2);
            return id2;
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
            }
        }
    }

    @Override // psoft.db.AbstractDatabase, psoft.db.Database
    public int getNewId() throws SQLException {
        return getNewId("newid");
    }
}
