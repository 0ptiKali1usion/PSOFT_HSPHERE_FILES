package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/PostgresqlChecker.class */
public class PostgresqlChecker {
    public static void main(String[] args) throws Exception {
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        Database db = Toolbox.getDB(config);
        Connection con = db.getConnection();
        DatabaseMetaData md = con.getMetaData();
        System.out.println(md.getDatabaseProductVersion());
        con.close();
        db.close();
    }
}
