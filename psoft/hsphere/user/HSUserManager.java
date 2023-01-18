package psoft.hsphere.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.persistance.PersistanceError;
import psoft.persistance.UniversalPersistanceManager;
import psoft.user.GenericUser;
import psoft.user.UserCache;
import psoft.user.UserSignature;
import psoft.user.User_PM;
import psoft.util.TimeUtils;
import psoft.validators.Accessor;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/hsphere/user/HSUserManager.class */
public class HSUserManager extends User_PM {
    protected String query;

    public HSUserManager(UniversalPersistanceManager upm, Database db, String query) {
        super(upm, db, "");
        this.query = query;
    }

    protected String getQuery(String login) {
        String[] args = {login};
        System.err.println("Query is " + MessageFormat.format(this.query, args));
        return MessageFormat.format(this.query, args);
    }

    @Override // psoft.persistance.PersistanceManager
    public Object get(Accessor data, NameModifier nm) {
        try {
            try {
                String param = data.get("1");
                if ("email".equals(param)) {
                    GenericUser findByEmail = findByEmail(data.get("2"));
                    Session.closeStatement(null);
                    return findByEmail;
                }
                String login = data.get(nm.getName("login"));
                if (login == null) {
                    throw new PersistanceError("No user login specified. ");
                }
                HSUser hsu = (HSUser) UserCache.get(new UserSignature(login, HSUser.class));
                if (hsu != null) {
                    if (hsu.getTimeStamp() != null) {
                        return hsu;
                    }
                    System.err.println("USER IN CACHE, BUT RELOADING FROM DATABASE, BY REQUEST OF ADAPTER...");
                }
                Statement ps = this.f255db.getStatement();
                ResultSet rs = ps.executeQuery(getQuery(login));
                if (rs.next()) {
                    HSUser hsu2 = new HSUser(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getString(6), rs.getString(7));
                    hsu2.setPersistanceManager(this.f246pm);
                    hsu2.setTimeStamp(TimeUtils.getDate());
                    UserCache.put(new UserSignature(login, HSUser.class), hsu2);
                    Session.closeStatement(ps);
                    return hsu2;
                }
                throw new PersistanceError("No such user");
            } catch (SQLException se) {
                se.printStackTrace();
                throw new PersistanceError("Internal problem, try again");
            }
        } finally {
            Session.closeStatement(null);
        }
    }

    @Override // psoft.persistance.PersistanceManager
    public void insert(Object obj) {
        throw new PersistanceError("User signup not supported");
    }

    @Override // psoft.persistance.PersistanceManager
    public void update(Object obj) {
        throw new PersistanceError("User updates not implemented");
    }

    @Override // psoft.persistance.PersistanceManager
    public void delete(Object obj) {
        throw new PersistanceError("User deletion not implemented");
    }

    @Override // psoft.persistance.PersistanceManager
    public int getNewId() {
        throw new PersistanceError("User signup not supported");
    }

    @Override // psoft.persistance.PersistanceManager
    public long getNewIdAsLong() {
        return getNewId();
    }
}
