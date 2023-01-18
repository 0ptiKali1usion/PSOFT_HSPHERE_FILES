package psoft.hsphere.tools.batchsql;

import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/tools/batchsql/BatchSQL.class */
public class BatchSQL {
    boolean isMSSQL;
    boolean isOracle;
    boolean isPostgreSQL;
    public DatabaseConvertor conv = null;
    protected ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");

    /* renamed from: db */
    Database f240db = Toolbox.getDB(this.config);

    public BatchSQL() throws Exception {
        this.isMSSQL = false;
        this.isOracle = false;
        this.isPostgreSQL = false;
        String driverName = this.f240db.getConnection().getMetaData().getDriverName().toLowerCase();
        if (driverName.indexOf("sqlserver") > -1) {
            this.isMSSQL = true;
        } else if (driverName.indexOf("oracle") > -1) {
            this.isOracle = true;
        } else if (driverName.indexOf("postgresql") > -1) {
            this.isPostgreSQL = true;
        }
    }

    public static void main(String[] argv) throws Exception {
        BatchSQL batch = new BatchSQL();
        try {
            String str = argv[0];
            if (batch.isPostgreSQL) {
                System.out.print("Driver - Postgres\n");
                System.out.print("Data types file - postsql_types.hsphere\n");
                batch.conv = new PostSqlDatabaseConvertor("postsql_types.hsphere");
            } else if (batch.isMSSQL) {
                System.out.print("Driver - MSSQL\n");
                System.out.print("Data types file - mssql_types.hsphere\n");
                batch.conv = new MSSqlDatabaseConvertor("mssql_types.hsphere");
            } else if (batch.isOracle) {
                System.out.print("Driver - Oracle\n");
                System.out.print("Data types file - oraclesql_types.hsphere\n");
                batch.conv = new OracleSQLDatabaseConvertor("oraclesql_types.hsphere");
            }
            if ((!batch.isMSSQL) & (!batch.isOracle) & (!batch.isPostgreSQL)) {
                throw new Exception("Unknown database driver!");
            }
            DatabaseConvertor conv = batch.conv;
            conv.execFile(argv[0]);
            System.out.println("Finished");
            System.exit(0);
        } catch (Exception e) {
            System.out.print("SQL script file name is incorrect\n");
        }
    }
}
