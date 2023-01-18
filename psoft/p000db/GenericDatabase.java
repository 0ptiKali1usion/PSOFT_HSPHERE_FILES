package psoft.p000db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.MessageFormat;

/* renamed from: psoft.db.GenericDatabase */
/* loaded from: hsphere.zip:psoft/db/GenericDatabase.class */
public class GenericDatabase extends AbstractDatabase {
    protected MessageFormat newIdQueryForm;

    public GenericDatabase(String driver, String url, String login, String password, String newIdQuery) throws SQLException, ClassNotFoundException {
        Class.forName(driver);
        init(url, login, password);
        this.newIdQueryForm = new MessageFormat(newIdQuery);
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public int getNewId(String name) throws SQLException {
        Object[] args = {name};
        ResultSet rs = getStatement().executeQuery(this.newIdQueryForm.format(args));
        if (rs.next()) {
            return rs.getInt(1);
        }
        throw new SQLException("Unable to get new id");
    }

    @Override // psoft.p000db.AbstractDatabase, psoft.p000db.Database
    public int getNewId() throws SQLException {
        return getNewId("newid");
    }
}
