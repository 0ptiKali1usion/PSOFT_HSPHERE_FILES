package psoft.hsphere.tools;

import gnu.regexp.RE;
import gnu.regexp.REMatch;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ResourceBundle;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MSSqlDatabaseConvertor.class */
public class MSSqlDatabaseConvertor extends DatabaseConvertor {
    protected ResourceBundle config;
    SQLTypesResourceBundle sqlBundle;
    public String dataMapFile;

    public MSSqlDatabaseConvertor(String dataMapFile) throws Exception {
        super(dataMapFile);
        if (!checkSequences()) {
            recreateTable();
        }
    }

    public static void main(String[] argv) throws Exception {
    }

    @Override // psoft.hsphere.tools.DatabaseConvertor
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
            RE regular4 = new RE("\\s*CREATE\\s+INDEX\\s+(\\S+)\\s+ON\\s+(\\S+)\\(\\S+\\);", 2);
            if (regular4.getMatch(replaced) != null) {
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        return super.parseSQL(replaced);
    }

    public boolean checkSequences() throws Exception {
        Connection con = this.f221db.getConnection();
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
