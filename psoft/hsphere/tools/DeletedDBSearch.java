package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.PropertyResourceBundle;
import psoft.hsphere.C0004CP;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/DeletedDBSearch.class */
public class DeletedDBSearch extends C0004CP {

    /* renamed from: db */
    Database f222db;
    List mssqlDBsToDelete;
    List mssqlLoginsToDelete;
    List pgsqlDBsToDelete;
    List mysqlDBsToDelete;

    public DeletedDBSearch() throws Exception {
        super("psoft_config.hsphere");
        this.mssqlDBsToDelete = new ArrayList();
        this.mssqlLoginsToDelete = new ArrayList();
        this.pgsqlDBsToDelete = new ArrayList();
        this.mysqlDBsToDelete = new ArrayList();
        this.config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        this.f222db = Toolbox.getDB(this.config);
    }

    public static void main(String[] args) throws Exception {
        StringBuffer keys = new StringBuffer("");
        for (String str : args) {
            keys.append(str);
        }
        if (keys.toString().indexOf("--help") != -1) {
            System.out.println("NAME:\n\t psoft.hsphere.tools.DeletedDBSearch - H-Sphere deleted databases search utility");
            System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.DeletedDBSearch [options]");
            System.out.println("OPTIONS:");
            System.out.println("\t--help \t- shows this screen");
            System.out.println("\t--del \t- delete MS SQL databases and MS SQL logins that were found");
            System.out.println("\t--all \t- search all databases: MS SQL, PostgreSQL, MySQL");
            System.out.println("\t--mssql \t- search MS SQL databases and MS SQL logins");
            System.out.println("\t--mysql \t-  search MySQL databases");
            System.out.println("\t--pgsql \t-  search PostgreSQL databases");
            System.exit(0);
        }
        DeletedDBSearch ds = new DeletedDBSearch();
        System.out.println(keys);
        ds.process(keys.toString());
        System.exit(0);
    }

    public void process(String keys) throws Exception {
        System.out.println("Initializing...");
        if (keys.indexOf("--mssql") != -1 || keys.indexOf("--all") != -1) {
            searchMSSQLDBs();
            searchMSSQLLogins();
        }
        System.out.println("\n");
        if (keys.indexOf("--mysql") != -1 || keys.indexOf("--all") != -1) {
            searchMySQLDBs();
        }
        System.out.println("\n");
        if (keys.indexOf("--pgsql") != -1 || keys.indexOf("--all") != -1) {
            searchPGSQLDBs();
        }
        System.out.println("\n");
        System.out.println("Search finished.");
        if (keys.indexOf("--del") != -1) {
            System.out.println("\n");
            deleteDBs();
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v19, types: [java.lang.String[], java.lang.String[][]] */
    private void searchMSSQLDBs() throws Exception {
        System.out.println("Search broken MS SQL databases...");
        Connection con = this.f222db.getConnection();
        PreparedStatement ps = null;
        try {
            try {
                ResultSet rs = con.prepareStatement("select mssql_db_name,mssql_login_id from mssql_dbs where id not in (select child_id from parent_child where child_type=6802)").executeQuery();
                List mssql_dbs = new ArrayList();
                while (rs.next()) {
                    String dbName = rs.getString(1);
                    int dbLogin = rs.getInt(2);
                    Hashtable oneDB = new Hashtable();
                    oneDB.put("db_name", dbName);
                    oneDB.put("db_login_id", new Integer(dbLogin));
                    mssql_dbs.add(oneDB);
                }
                rs.close();
                ps = con.prepareStatement("select id from l_server where group_id in (select id from l_server_groups where type_id=15)");
                ResultSet rs2 = ps.executeQuery();
                List serverDBs = new ArrayList();
                while (rs2.next()) {
                    int hostId = rs2.getInt(1);
                    WinHostEntry he = (WinHostEntry) HostManager.getHost(hostId);
                    String pServerIP = he.getPFirstIP();
                    Collection c = he.exec("mssql-getdatabaselist.asp", (String[][]) new String[]{new String[]{"test", "test"}});
                    Object[] res = c.toArray();
                    for (Object obj : res) {
                        String dbName2 = (String) obj;
                        Hashtable oneDB2 = new Hashtable();
                        oneDB2.put("host_id", new Integer(hostId));
                        oneDB2.put("host_ip", pServerIP);
                        oneDB2.put("db_name", dbName2);
                        serverDBs.add(oneDB2);
                    }
                }
                int total = 0;
                System.out.println("DB name \twinbox IP \tDB login");
                System.out.println("--------------------------------------------");
                for (int i = 0; i < mssql_dbs.size(); i++) {
                    Hashtable fromDB = (Hashtable) mssql_dbs.get(i);
                    String nameFromDB = (String) fromDB.get("db_name");
                    int dbLoginId = ((Integer) fromDB.get("db_login_id")).intValue();
                    ps = con.prepareStatement("select login from mssql_logins where id = ?");
                    ps.setInt(1, dbLoginId);
                    ResultSet rs3 = ps.executeQuery();
                    String dbLogin2 = "";
                    while (rs3.next()) {
                        dbLogin2 = rs3.getString(1);
                    }
                    rs3.close();
                    boolean onBox = false;
                    for (int j = 0; j < serverDBs.size(); j++) {
                        Hashtable fromBox = (Hashtable) serverDBs.get(j);
                        String nameFromBox = (String) fromBox.get("db_name");
                        String ip = (String) fromBox.get("host_ip");
                        if (nameFromDB.equals(nameFromBox)) {
                            System.out.println(nameFromBox + "\t " + ip + "\t " + dbLogin2);
                            Hashtable dbToDelete = new Hashtable();
                            dbToDelete.put("db_name", nameFromBox);
                            dbToDelete.put("host_id", (Integer) fromBox.get("host_id"));
                            dbToDelete.put("on_box", new Boolean(true));
                            this.mssqlDBsToDelete.add(dbToDelete);
                            total++;
                            onBox = true;
                        }
                    }
                    if (!onBox) {
                        Hashtable dbToDelete2 = new Hashtable();
                        dbToDelete2.put("db_name", nameFromDB);
                        dbToDelete2.put("on_box", new Boolean(false));
                        this.mssqlDBsToDelete.add(dbToDelete2);
                    }
                }
                System.out.println("------------------------------");
                System.out.println("MS SQL DBs Total: " + String.valueOf(total));
                ps.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                ps.close();
                con.close();
            }
            System.out.println("Search broken MS SQL databases finished.");
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v16, types: [java.lang.String[], java.lang.String[][]] */
    private void searchMSSQLLogins() throws Exception {
        System.out.println("Search broken MS SQL logins...");
        Connection con = this.f222db.getConnection();
        PreparedStatement ps = null;
        try {
            try {
                ResultSet rs = con.prepareStatement("select login from mssql_logins where id not in (select child_id from parent_child where child_type=6801)").executeQuery();
                List mssql_logins = new ArrayList();
                while (rs.next()) {
                    String loginName = rs.getString(1);
                    mssql_logins.add(loginName);
                }
                rs.close();
                ps = con.prepareStatement("select id from l_server where group_id in (select id from l_server_groups where type_id=15)");
                ResultSet rs2 = ps.executeQuery();
                List serverLogins = new ArrayList();
                while (rs2.next()) {
                    int hostId = rs2.getInt(1);
                    WinHostEntry he = (WinHostEntry) HostManager.getHost(hostId);
                    String pServerIP = he.getPFirstIP();
                    Collection c = he.exec("mssql-getloginlist.asp", (String[][]) new String[]{new String[]{"test", "test"}});
                    Object[] res = c.toArray();
                    for (Object obj : res) {
                        String loginName2 = (String) obj;
                        Hashtable oneLogin = new Hashtable();
                        oneLogin.put("host_id", new Integer(hostId));
                        oneLogin.put("host_ip", pServerIP);
                        oneLogin.put("login_name", loginName2);
                        serverLogins.add(oneLogin);
                    }
                }
                int total = 0;
                System.out.println("Login name\t winbox IP");
                System.out.println("----------------------------");
                for (int i = 0; i < mssql_logins.size(); i++) {
                    String nameFromDB = (String) mssql_logins.get(i);
                    boolean onBox = false;
                    for (int j = 0; j < serverLogins.size(); j++) {
                        Hashtable fromBox = (Hashtable) serverLogins.get(j);
                        String nameFromBox = (String) fromBox.get("login_name");
                        String ip = (String) fromBox.get("host_ip");
                        if (nameFromDB.equals(nameFromBox)) {
                            System.out.println(nameFromBox + "       " + ip);
                            Hashtable loginToDelete = new Hashtable();
                            loginToDelete.put("login_name", nameFromBox);
                            loginToDelete.put("host_id", (Integer) fromBox.get("host_id"));
                            loginToDelete.put("on_box", new Boolean(true));
                            this.mssqlLoginsToDelete.add(loginToDelete);
                            total++;
                            onBox = true;
                        }
                    }
                    if (!onBox) {
                        Hashtable loginToDelete2 = new Hashtable();
                        loginToDelete2.put("login_name", nameFromDB);
                        loginToDelete2.put("on_box", new Boolean(false));
                        this.mssqlLoginsToDelete.add(loginToDelete2);
                    }
                }
                System.out.println("-----------------------------");
                System.out.println("MS SQL Logins Total: " + String.valueOf(total));
                ps.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                ps.close();
                con.close();
            }
            System.out.println("Search broken MS SQL logins finished.");
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void searchMySQLDBs() throws Exception {
        System.out.println("Search broken MySQL databases...");
        Connection con = this.f222db.getConnection();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("select db_name,parent_id from mysqldb where id not in (select child_id from parent_child where child_type=6001)");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String dbName = rs.getString(1);
                    int parentId = rs.getInt(2);
                    Hashtable oneDB = new Hashtable();
                    oneDB.put("db_name", dbName);
                    oneDB.put("parent_id", new Integer(parentId));
                    ps = con.prepareStatement("select mysql_host_id from mysqlres where id = ?");
                    ps.setInt(1, parentId);
                    ResultSet rs1 = ps.executeQuery();
                    while (rs1.next()) {
                        int hostId = rs1.getInt(1);
                        HostEntry he = HostManager.getHost(hostId);
                        String pServerIP = he.getPFirstIP();
                        oneDB.put("host_id", new Integer(hostId));
                        oneDB.put("host_ip", pServerIP);
                    }
                    this.mysqlDBsToDelete.add(oneDB);
                }
                rs.close();
                ps.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                ps.close();
                con.close();
            }
            if (this.mysqlDBsToDelete.size() > 0) {
                System.out.println("DB name\t MySQL box IP");
                System.out.println("-------------------------------");
            }
            for (int i = 0; i < this.mysqlDBsToDelete.size(); i++) {
                Hashtable oneDB2 = (Hashtable) this.mysqlDBsToDelete.get(i);
                System.out.println(oneDB2.get("db_name") + "\t " + oneDB2.get("host_ip"));
            }
            System.out.println("-----------------------------");
            System.out.println("MySQL DBs Total: " + String.valueOf(this.mysqlDBsToDelete.size()));
            System.out.println("Search broken MySQL databases finished.");
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    private void searchPGSQLDBs() throws Exception {
        System.out.println("Search broken PostgreSQL databases...");
        Connection con = this.f222db.getConnection();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("select db_name,parent_id from pgsqldb where id not in (select child_id from parent_child where child_type=6901)");
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String dbName = rs.getString(1);
                    int parentId = rs.getInt(2);
                    Hashtable oneDB = new Hashtable();
                    oneDB.put("db_name", dbName);
                    oneDB.put("parent_id", new Integer(parentId));
                    System.out.println("PostgreSQL db name = " + dbName);
                    ps = con.prepareStatement("select pgsql_host_id from pgsqlres where id = ?");
                    ps.setInt(1, parentId);
                    ResultSet rs1 = ps.executeQuery();
                    while (rs1.next()) {
                        int hostId = rs1.getInt(1);
                        HostEntry he = HostManager.getHost(hostId);
                        String pServerIP = he.getPFirstIP();
                        oneDB.put("host_id", new Integer(hostId));
                        oneDB.put("host_ip", pServerIP);
                    }
                    this.pgsqlDBsToDelete.add(oneDB);
                }
                rs.close();
                ps.close();
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                ps.close();
                con.close();
            }
            if (this.pgsqlDBsToDelete.size() > 0) {
                System.out.println("DB name  PgSQL box IP");
                System.out.println("-------------------------------");
            }
            for (int i = 0; i < this.pgsqlDBsToDelete.size(); i++) {
                Hashtable oneDB2 = (Hashtable) this.pgsqlDBsToDelete.get(i);
                System.out.println(oneDB2.get("db_name") + "  " + oneDB2.get("host_ip"));
            }
            System.out.println("-----------------------------");
            System.out.println("PostgreSQL DBs Total: " + String.valueOf(this.pgsqlDBsToDelete.size()));
            System.out.println("Search broken PostgreSQL databases finished.");
        } catch (Throwable th) {
            ps.close();
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v11, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v5, types: [java.lang.String[], java.lang.String[][]] */
    private void deleteDBs() throws Exception {
        System.out.println("Databases deleting started...");
        System.out.println("MS SQL dbs deleting started...");
        for (int i = 0; i < this.mssqlDBsToDelete.size(); i++) {
            Hashtable oneDB = (Hashtable) this.mssqlDBsToDelete.get(i);
            String db_name = (String) oneDB.get("db_name");
            System.out.println("Delete\t " + db_name);
            boolean isOnBox = ((Boolean) oneDB.get("on_box")).booleanValue();
            if (isOnBox) {
                int host_id = ((Integer) oneDB.get("host_id")).intValue();
                WinHostEntry he = (WinHostEntry) HostManager.getHost(host_id);
                he.exec("mssql-dropdatabase.asp", (String[][]) new String[]{new String[]{"database", db_name}});
            }
            Connection con = this.f222db.getConnection();
            PreparedStatement ps = null;
            try {
                try {
                    ps = con.prepareStatement("delete from mssql_dbs where mssql_db_name = ?");
                    ps.setString(1, db_name);
                    ps.executeUpdate();
                    ps.close();
                    con.close();
                } catch (Exception e) {
                    System.out.println("Failed to delete MS SQL DB from H-Sphere database.");
                    e.printStackTrace();
                    ps.close();
                    con.close();
                }
            } catch (Throwable th) {
                ps.close();
                con.close();
                throw th;
            }
        }
        System.out.println("MS SQL dbs deleting finished.");
        System.out.println("\n");
        System.out.println("MS SQL logins deleting started...");
        for (int i2 = 0; i2 < this.mssqlLoginsToDelete.size(); i2++) {
            Hashtable oneLogin = (Hashtable) this.mssqlLoginsToDelete.get(i2);
            String login_name = (String) oneLogin.get("login_name");
            boolean isOnBox2 = ((Boolean) oneLogin.get("on_box")).booleanValue();
            System.out.println("Delete   " + login_name);
            if (isOnBox2) {
                int host_id2 = ((Integer) oneLogin.get("host_id")).intValue();
                WinHostEntry he2 = (WinHostEntry) HostManager.getHost(host_id2);
                he2.exec("mssql-removelogin.asp", (String[][]) new String[]{new String[]{"login", login_name}});
            }
            Connection con2 = this.f222db.getConnection();
            PreparedStatement ps2 = null;
            try {
                try {
                    ps2 = con2.prepareStatement("delete from mssql_logins where login = ?");
                    ps2.setString(1, login_name);
                    ps2.executeUpdate();
                    ps2.close();
                    con2.close();
                } catch (Exception e2) {
                    System.out.println("Failed to delete MS SQL Login from H-Sphere database.");
                    e2.printStackTrace();
                    ps2.close();
                    con2.close();
                }
            } catch (Throwable th2) {
                ps2.close();
                con2.close();
                throw th2;
            }
        }
        System.out.println("MS SQL logins deleting finished.");
    }
}
