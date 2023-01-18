package psoft.hsphere.tools.batchsql;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.sql.SQLException;

/* loaded from: hsphere.zip:psoft/hsphere/tools/batchsql/PostSqlDatabaseConvertor.class */
public class PostSqlDatabaseConvertor extends DatabaseConvertor {
    public PostSqlDatabaseConvertor(String dataMapFile) throws Exception {
        super(dataMapFile);
    }

    public static void main(String[] argv) throws Exception {
    }

    @Override // psoft.hsphere.tools.batchsql.DatabaseConvertor
    public String parseSQL(String sql) throws SQLException {
        String replaced = super.parseSQL(sql);
        try {
            RE regular = new RE("\\s*ALTER\\s+TABLE\\s+(\\S+)\\s+ADD\\s+CONSTRAINT\\s+(\\S+)\\s+FOREIGN\\s+KEY\\s*\\((.*?)\\)\\s+REFERENCES\\s+(\\S+);", 2);
            REMatch mt = regular.getMatch(replaced);
            if (mt != null) {
                replaced = mt.substituteInto("CREATE INDEX $2 ON $1($3);\n");
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return replaced;
    }
}
