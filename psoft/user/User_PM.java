package psoft.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import psoft.p000db.Database;
import psoft.persistance.PersistanceError;
import psoft.persistance.PersistanceManager;
import psoft.persistance.UniversalPersistanceManager;

/* loaded from: hsphere.zip:psoft/user/User_PM.class */
public abstract class User_PM extends PersistanceManager {

    /* renamed from: db */
    protected Database f255db;
    protected String table;

    public User_PM(UniversalPersistanceManager upm, Database db, String table) {
        super(upm);
        this.f255db = db;
        this.table = table;
    }

    public GenericUser findByEmail(String email) {
        Statement ps = null;
        try {
            try {
                ps = this.f255db.getStatement();
                ResultSet rs = ps.executeQuery("SELECT id, login, password FROM " + this.table + " WHERE UPPER(email) = " + this.f255db.quote(email.toUpperCase()));
                if (rs.next()) {
                    return new GenericUser(rs.getInt(1), rs.getString(2), rs.getString(3), email);
                }
                throw new PersistanceError("not in database");
            } catch (SQLException se) {
                se.printStackTrace();
                throw new PersistanceError("ooppps...");
            }
        } finally {
            try {
                ps.close();
            } catch (SQLException e) {
            }
        }
    }
}
