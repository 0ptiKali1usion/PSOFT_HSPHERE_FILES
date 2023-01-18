package psoft.hsphere.report;

import java.sql.ResultSet;
import java.sql.SQLException;
import psoft.hsphere.Session;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/report/Result.class */
public class Result {
    protected String name;
    protected String type;

    public Result(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() {
        return this.name;
    }

    public TemplateString getEntry(ResultSet rs, int pos) throws SQLException {
        try {
            if ("CLOB".equals(this.type)) {
                return new TemplateString(Session.getClobValue(rs, pos));
            }
            return new TemplateString(rs.getObject(pos));
        } catch (SQLException e) {
            return new TemplateString(rs.getString(pos));
        } catch (Exception e2) {
            Session.getLog().error("Result: error during reading CLOB field", e2);
            return new TemplateString("");
        }
    }
}
