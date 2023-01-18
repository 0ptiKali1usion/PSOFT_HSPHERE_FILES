package psoft.hsphere.tools;

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

/* loaded from: hsphere.zip:psoft/hsphere/tools/DatabaseConvertor.class */
public class DatabaseConvertor {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    public Database f221db = Toolbox.getDB(this.config);
    SQLTypesResourceBundle sqlBundle;
    public String dataMapFile;
    public Connection con;

    public DatabaseConvertor(String dataMapFile) throws Exception {
        setMapFile(dataMapFile);
        this.con = this.f221db.getConnection();
        System.out.print("Full driver name - " + this.con.getMetaData().getDriverName() + "\n");
    }

    public void setMapFile(String dataMapFile) throws Exception {
        this.sqlBundle = new SQLTypesResourceBundle(dataMapFile);
    }

    public void execFile(String fileName) throws Exception {
        try {
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
            } finally {
                this.con.close();
            }
        } catch (Exception exc) {
            exc.printStackTrace();
            this.con.close();
        }
    }

    public static void main(String[] argv) throws Exception {
        DatabaseConvertor dc = new DatabaseConvertor(argv[1]);
        dc.execFile(argv[0]);
        System.out.println("Finished");
        System.exit(0);
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
                    if (((last != ',') & (last != ')') & (last != ' ') & (last != '\n')) | (str.indexOf("UPDATE", s) + 2 == e) | (str.indexOf("update", s) + 2 == e) | (str.charAt(e - 1) != ' ')) {
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
        String query = parseSQL(oldQuery);
        executeQuery(query);
    }

    public void executeQuery(String query) throws Exception {
        PreparedStatement ps = null;
        try {
            try {
                ps = this.con.prepareStatement(query);
                ps.executeUpdate();
                System.out.println("OK: " + query);
                Session.closeStatement(ps);
            } catch (SQLException e) {
                System.out.println("ERROR: \"" + query + "\" failed, " + e.getMessage());
                Session.closeStatement(ps);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }
}
