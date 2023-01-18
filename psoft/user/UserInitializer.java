package psoft.user;

import freemarker.template.TemplateCache;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.ResourceBundle;
import psoft.p000db.Database;
import psoft.p000db.PgDatabase;
import psoft.persistance.UniversalPersistanceManager;
import psoft.util.Config;
import psoft.util.freemarker.AutoRefreshFileTemplateCache;
import psoft.validators.NameModifier;

/* loaded from: hsphere.zip:psoft/user/UserInitializer.class */
public abstract class UserInitializer {

    /* renamed from: db */
    protected Database f252db;
    protected TemplateCache tCache;
    protected UniversalPersistanceManager upm;
    protected Hashtable config;
    protected WebUserProcessor wup;

    /* renamed from: rb */
    protected ResourceBundle f253rb;

    /* renamed from: nm */
    protected NameModifier f254nm;

    protected abstract void initWUP() throws UserException;

    protected void initDB() throws UserException {
        try {
            this.f252db = new PgDatabase(this.f253rb.getString("DB_URL"), this.f253rb.getString("DB_USER"), this.f253rb.getString("DB_PASSWORD"));
        } catch (SQLException se) {
            throw new UserException("init error:" + se.getMessage());
        }
    }

    public void initUPM() throws UserException {
        this.upm = new UniversalPersistanceManager();
    }

    protected void initTemplateCache() throws UserException {
        initTemplateCache(100);
    }

    protected void initTemplateCache(int size) throws UserException {
        this.tCache = new AutoRefreshFileTemplateCache(this.f253rb.getString("TEMPLATE_PATH"), size);
    }

    public Hashtable init(ResourceBundle rb, NameModifier nm) throws UserException {
        this.f253rb = rb;
        this.f254nm = nm;
        initDB();
        initTemplateCache();
        initUPM();
        this.config = new Hashtable();
        this.config.put("config", rb);
        this.config.put(TemplateCache.class, this.tCache);
        this.config.put(UniversalPersistanceManager.class, this.upm);
        if (this.f252db != null) {
            this.config.put(Database.class, this.f252db);
        }
        Config.set("psoft.user", this.config);
        initWUP();
        this.config.put(WebUserProcessor.class, this.wup);
        return this.config;
    }
}
