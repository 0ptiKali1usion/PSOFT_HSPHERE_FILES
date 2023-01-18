package psoft.hsphere.tools;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/BatchSQL.class */
public class BatchSQL {
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    Database f219db = Toolbox.getDB(this.config);

    public static void execFile(String fileName) throws Exception {
        BatchSQL bt = new BatchSQL();
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
                        buffer.append(line.substring(0, pos));
                        if (!skipNext) {
                            try {
                                bt.executeSQL(buffer.toString());
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
            bt.executeSQL(line2);
        }
    }

    public static void main(String[] argv) throws Exception {
        execFile(argv[0]);
        System.out.println("Finished");
        System.exit(0);
    }

    protected void executeSQL(String query) throws Exception {
        Connection con = this.f219db.getConnection();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement(query);
                ps.executeUpdate();
                System.out.println("OK: " + query);
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException e) {
                System.out.println("ERROR: \"" + query + "\" failed, " + e.getMessage());
                throw e;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
