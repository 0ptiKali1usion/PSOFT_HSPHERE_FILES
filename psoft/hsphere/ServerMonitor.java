package psoft.hsphere;

import java.util.Hashtable;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.p000db.Database;
import psoft.p000db.GenericDatabase;
import psoft.util.Config;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/ServerMonitor.class */
public class ServerMonitor {
    static ResourceBundle config;

    public static void main(String[] argv) throws Exception {
        initialize();
        Database db = new GenericDatabase("postgresql.Driver", "jdbc:postgresql://localhost/hsphere", "postgres", "", "");
        ServerManager sm = new ServerManager(db);
        sm.incarnate(1L);
    }

    protected static void initialize() throws Exception {
        config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        Hashtable conf = new Hashtable();
        conf.put(Database.class, Toolbox.getDB(config));
        conf.put("config", config);
        Config.set("CLIENT", conf);
    }
}
