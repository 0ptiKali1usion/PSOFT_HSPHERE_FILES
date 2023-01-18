package psoft.hsphere.resource.mysql;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mysql/UserPrivileges.class */
public class UserPrivileges {
    protected static Hashtable privileges = new Hashtable();
    protected Hashtable databasePriv = new Hashtable();
    protected String database;
    protected String user_name;
    private long hostId;

    static {
        privileges.put("Alter_priv", "alter");
        privileges.put("Delete_priv", "delete");
        privileges.put("Index_priv", "index");
        privileges.put("Insert_priv", "insert");
        privileges.put("Select_priv", "select");
        privileges.put("Update_priv", "update");
        privileges.put("Create_priv", "create");
        privileges.put("Drop_priv", "drop");
        privileges.put("Grant_priv", "grant");
        privileges.put("References_priv", "references");
        privileges.put("Create_tmp_table_priv", "create temporary tables");
        privileges.put("Execute_priv", "execute");
        privileges.put("File_priv", "file");
        privileges.put("Lock_tables_priv", "lock tables");
        privileges.put("Process_priv", "process");
        privileges.put("Reload_priv", "reload");
        privileges.put("Repl_client_priv", "replication client");
        privileges.put("Repl_slave_priv", "replication slave");
        privileges.put("Show_db_priv", "show databases");
        privileges.put("Shutdown_priv", "shutdown");
        privileges.put("Super_priv", "super");
    }

    public UserPrivileges(String uname, String database, long hostId) throws Exception {
        this.database = database;
        this.user_name = uname;
        this.hostId = hostId;
        Session.getLog().debug("UserPrivileges: hostId is " + hostId);
        HostEntry host = HostManager.getHost(hostId);
        if (HostEntry.getEmulationMode()) {
            return;
        }
        List args = new ArrayList();
        args.add(database);
        args.add(uname);
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        }
        Collection col = host.exec("mysql-get-privileges", args);
        if (col.size() < 2) {
            Session.getLog().debug("Priveleges for user " + uname + " not found");
            return;
        }
        Iterator i = col.iterator();
        this.databasePriv.clear();
        StringTokenizer key = new StringTokenizer((String) i.next());
        StringTokenizer value = new StringTokenizer((String) i.next());
        while (key.hasMoreElements()) {
            this.databasePriv.put(privileges.get(key.nextElement()), value.nextElement());
        }
    }

    public String getDatabasePriv(String priv) {
        return this.databasePriv.containsKey(priv) ? (String) this.databasePriv.get(priv) : "N";
    }

    public Hashtable getDatabasePrivileges() {
        return this.databasePriv;
    }

    public boolean setDatabasePrivileges(String privileges2) throws Exception {
        HostEntry host = HostManager.getHost(this.hostId);
        List args = new ArrayList();
        args.add(this.database);
        args.add(this.user_name);
        args.add('\"' + privileges2 + '\"');
        if ("1".equals(host.getOption("mysql_clustering"))) {
            args.add(host.getIP().toString());
        } else {
            args.add("");
        }
        host.exec("mysql-grant", args);
        return true;
    }

    public boolean revokeAllDatabasePrivileges() throws SQLException {
        Session.getLog().info("MySQLResource: revoking all database privileges on " + this.database + " for user " + this.user_name);
        try {
            HostEntry host = HostManager.getHost(this.hostId);
            List args = new ArrayList();
            args.add(this.database);
            args.add(this.user_name);
            if ("1".equals(host.getOption("mysql_clustering"))) {
                args.add(host.getIP().toString());
            }
            host.exec("mysql-revoke-all", args);
            Enumeration e = this.databasePriv.keys();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                Session.getLog().info("MySQLResource: revoking " + key + " privilege");
                this.databasePriv.put(key, "N");
            }
            return true;
        } catch (Exception e2) {
            Session.getLog().info("UserPrivileges: can't perform revoke", e2);
            return false;
        }
    }
}
