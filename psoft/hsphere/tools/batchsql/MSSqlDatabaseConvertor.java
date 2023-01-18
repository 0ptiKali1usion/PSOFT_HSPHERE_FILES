package psoft.hsphere.tools.batchsql;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/batchsql/MSSqlDatabaseConvertor.class */
public class MSSqlDatabaseConvertor extends DatabaseConvertor {
    public MSSqlDatabaseConvertor(String dataMapFile) throws Exception {
        super(dataMapFile);
        if (!checkSequences()) {
            recreateTable();
        }
    }

    @Override // psoft.hsphere.tools.batchsql.DatabaseConvertor
    public String parseSQL(String sql) throws SQLException {
        String replaced = sql;
        try {
            RE regular = new RE("\\s*CREATE\\s+SEQUENCE\\s+(\\S+);", 2);
            REMatch mt = regular.getMatch(replaced);
            if (mt != null) {
                replaced = mt.substituteInto("INSERT INTO sequences VALUES ('$1',0);\n");
            }
            RE regular2 = new RE("\\s*DROP\\s+SEQUENCE\\s+(\\S+);", 2);
            REMatch mt2 = regular2.getMatch(replaced);
            if (mt2 != null) {
                replaced = mt2.substituteInto("DELETE FROM sequences WHERE seq_name='$1';\n");
            }
            RE regular3 = new RE("\\s*CREATE\\s+SEQUENCE\\s+(\\S+)\\s+START\\s+(\\d+)", 2);
            REMatch mt3 = regular3.getMatch(replaced);
            if (mt3 != null) {
                replaced = mt3.substituteInto("INSERT INTO sequences VALUES ('$1',$2);\n");
            }
            RE regular4 = new RE("\\s*ALTER\\s+TABLE\\s+(\\S+)\\s+RENAME\\s+TO\\s+(\\S+);", 2);
            REMatch mt4 = regular4.getMatch(replaced);
            if (mt4 != null) {
                replaced = mt4.substituteInto("EXEC sp_rename '$1','$2';\n");
            }
            StringBuffer buff = new StringBuffer(replaced);
            while (replaced.indexOf("with time zone") >= 0) {
                int start = replaced.indexOf("with time zone");
                buff.replace(start, start + "with time zone".length(), "");
                replaced = buff.toString();
                buff = new StringBuffer(replaced);
            }
            StringBuffer buff2 = new StringBuffer(replaced);
            if (replaced.indexOf("add column") >= 0) {
                int start2 = replaced.indexOf("add column");
                buff2.replace(start2, start2 + "add column".length(), "add");
            }
            replaced = buff2.toString();
            RE regular5 = new RE("\\s*CREATE\\s+INDEX\\s+(\\S+)\\s+ON\\s+(\\S+)\\(\\S+\\);", 2);
            if (regular5.getMatch(replaced) != null) {
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return super.parseSQL(replaced);
    }

    public boolean checkSequences() throws Exception {
        Connection con = this.f241db.getConnection();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT * FROM sequences;");
                ps.execute();
                System.out.print("Sequences is avaliable\n\n");
                Session.closeStatement(ps);
                con.close();
                return true;
            } catch (Exception e) {
                System.out.print("Create sequences\n\n");
                Session.closeStatement(ps);
                con.close();
                return false;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void recreateTable() throws Exception {
        executeQuery("CREATE TABLE sequences(\nseq_name char(20) NOT NULL PRIMARY KEY,\n id int default 0);\n");
        executeQuery("CREATE PROCEDURE nextval\n@seq_name char(20)\nas begin\ndeclare @ID int\nSET TRANSACTION ISOLATION LEVEL SERIALIZABLE\nBEGIN TRANSACTION\nUPDATE sequences SET  @ID =id = id + 1 WHERE seq_name = @seq_name\nCOMMIT TRANSACTION\nreturn @id\nend");
    }
}
