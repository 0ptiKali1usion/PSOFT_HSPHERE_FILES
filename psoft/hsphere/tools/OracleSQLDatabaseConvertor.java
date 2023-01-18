package psoft.hsphere.tools;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.sql.SQLException;
import java.util.ResourceBundle;

/* loaded from: hsphere.zip:psoft/hsphere/tools/OracleSQLDatabaseConvertor.class */
public class OracleSQLDatabaseConvertor extends DatabaseConvertor {
    protected ResourceBundle config;
    public String dataMapFile;

    public OracleSQLDatabaseConvertor(String dataMapFile) throws Exception {
        super(dataMapFile);
    }

    public static void main(String[] argv) throws Exception {
    }

    @Override // psoft.hsphere.tools.DatabaseConvertor
    public String parseSQL(String sql) throws SQLException {
        String replaced = sql;
        try {
            RE regular = new RE("\\s*SELECT\\s+nextval\\s*\\('(\\S+)'\\);", 2);
            REMatch mt = regular.getMatch(replaced);
            if (mt != null) {
                replaced = mt.substituteInto("SELECT $1.nextval FROM dual;\n");
            }
            RE regular2 = new RE("\\s*CREATE\\s+SEQUENCE\\s+(\\S+)\\s+START\\s+(\\S+);", 2);
            REMatch mt2 = regular2.getMatch(replaced);
            if (mt2 != null) {
                replaced = mt2.substituteInto("CREATE SEQUENCE $1 START WITH $2;\n");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return super.parseSQL(replaced);
    }
}
