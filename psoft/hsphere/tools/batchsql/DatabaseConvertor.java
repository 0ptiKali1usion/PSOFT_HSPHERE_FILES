package psoft.hsphere.tools.batchsql;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/batchsql/DatabaseConvertor.class */
public abstract class DatabaseConvertor {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    public Database f241db = Toolbox.getDB(this.config);
    SQLTypesResourceBundle sqlBundle;
    public Connection con;

    public DatabaseConvertor(String dataMapFile) throws Exception {
        try {
            setMapFile(dataMapFile);
            this.con = this.f241db.getConnection();
            System.out.print("Full driver name - " + this.con.getMetaData().getDriverName() + "\n");
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    public void setMapFile(String dataMapFile) throws Exception {
        String baseDir;
        try {
            baseDir = this.config.getString("HSPHERE_HOME");
        } catch (Exception e) {
            baseDir = "/home/cpanel/shiva";
        }
        this.sqlBundle = new SQLTypesResourceBundle(baseDir + "/psoft/hsphere/tools/batchsql/" + dataMapFile);
    }

    public void execFile(String fileName) throws Exception {
        try {
            if (this.sqlBundle == null) {
                return;
            }
            try {
                BufferedReader f1 = new BufferedReader(new InputStreamReader(new FileInputStream(fileName)));
                StringBuffer buffer = new StringBuffer();
                boolean criticalSection = false;
                boolean skipNext = false;
                while (true) {
                    String line = f1.readLine();
                    if (line == null) {
                        break;
                    } else if (!"".equals(line.trim())) {
                        if (line.trim().startsWith("#") || line.trim().startsWith("--")) {
                            if (line.trim().startsWith("#CRITICAL BEGIN")) {
                                criticalSection = true;
                                skipNext = false;
                                System.out.println("#CRITICAL BEGIN");
                            }
                            if (line.trim().startsWith("#CRITICAL END")) {
                                criticalSection = false;
                                skipNext = false;
                                System.out.println("#CRITICAL END");
                            }
                        } else {
                            int pos = line.indexOf(59);
                            if (pos >= 0) {
                                buffer.append(line.substring(0, pos + 1));
                                if (!skipNext) {
                                    try {
                                        executeSQL(buffer.toString());
                                    } catch (Exception e) {
                                        if (criticalSection) {
                                            skipNext = true;
                                        }
                                    }
                                }
                                buffer = new StringBuffer(line.substring(pos + 1));
                            } else {
                                buffer.append(line).append("\n");
                            }
                        }
                    }
                }
                f1.close();
                String line2 = buffer.toString().trim();
                if (!"".equals(line2)) {
                    executeSQL(line2);
                }
                this.con.close();
            } catch (Exception exc) {
                exc.printStackTrace();
                this.con.close();
            }
        } catch (Throwable th) {
            this.con.close();
            throw th;
        }
    }

    public String parseSQL(String sql) throws SQLException {
        String replaced = sql;
        Enumeration e = this.sqlBundle.getAllNames();
        while (e.hasMoreElements()) {
            String value = (String) e.nextElement();
            replaced = replace(replaced, value, this.sqlBundle.getString(value));
        }
        return replaced;
    }

    static String replace(String str, String pattern, String replace) {
        int s = 0;
        StringBuffer result = new StringBuffer();
        while (true) {
            int e = str.toLowerCase().indexOf(pattern.toLowerCase(), s);
            if (e >= 0) {
                char last = str.charAt(e + pattern.length());
                if (!((last == '(') & (str.charAt(e - 1) == ' '))) {
                    if (((last != ',') & (last != ')') & (last != ' ') & (last != '\n') & (last != ';')) | (str.indexOf("UPDATE", s) + 2 == e) | (str.indexOf("update", s) + 2 == e) | (str.charAt(e - 1) != ' ')) {
                        result.append(str.substring(s, e + pattern.length()));
                        s = e + pattern.length();
                    }
                }
                result.append(str.substring(s, e));
                result.append(replace);
                s = e + pattern.length();
            } else {
                result.append(str.substring(s));
                return result.toString();
            }
        }
    }

    protected void executeSQL(String oldQuery) throws Exception {
        String query = parseSQL(oldQuery.toLowerCase());
        executeQuery(query);
    }

    public void executeQuery(String query) throws Exception {
        PreparedStatement ps = null;
        try {
            try {
                ps = this.con.prepareStatement(query.toLowerCase());
                ps.executeUpdate();
                System.out.println("OK: " + query);
                Session.closeStatement(ps);
            } catch (SQLException e) {
                System.out.println("ERROR: \"" + query + "\" failed, " + e.getMessage());
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }
}
