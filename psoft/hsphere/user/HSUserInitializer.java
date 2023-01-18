package psoft.hsphere.user;

import java.sql.SQLException;
import psoft.p000db.GenericDatabase;
import psoft.user.UserException;
import psoft.user.UserInitializer;
import psoft.user.WebUserProcessor;

/* loaded from: hsphere.zip:psoft/hsphere/user/HSUserInitializer.class */
public class HSUserInitializer extends UserInitializer {
    @Override // psoft.user.UserInitializer
    protected void initDB() throws UserException {
        try {
            this.f252db = new GenericDatabase(this.f253rb.getString("DB_DRIVER"), this.f253rb.getString("DB_URL"), this.f253rb.getString("DB_USER"), this.f253rb.getString("DB_PASSWORD"), this.f253rb.getString("DB_NEWID"));
        } catch (ClassNotFoundException cnfe) {
            throw new UserException("init error (db class not found):" + cnfe.getMessage());
        } catch (SQLException se) {
            throw new UserException("init error:" + se.getMessage());
        }
    }

    @Override // psoft.user.UserInitializer
    public void initUPM() throws UserException {
        super.initUPM();
        this.upm.register(HSUser.class, new HSUserManager(this.upm, this.f252db, this.f253rb.getString("USER_LOGIN_QUERY")));
    }

    @Override // psoft.user.UserInitializer
    protected void initWUP() throws UserException {
        this.wup = new WebUserProcessor(new HSUserAdapter(), this.tCache, this.f253rb, this.f254nm);
    }
}
