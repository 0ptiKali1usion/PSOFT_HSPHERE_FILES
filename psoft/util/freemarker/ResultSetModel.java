package psoft.util.freemarker;

import freemarker.template.SimpleList;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/util/freemarker/ResultSetModel.class */
public class ResultSetModel extends SimpleList {
    int resultSetWidth;
    String[] colNames;

    /* renamed from: rs */
    ResultSet f273rs;

    public ResultSetModel(ResultSet rs) throws SQLException {
        this.f273rs = rs;
        ResultSetMetaData rsm = rs.getMetaData();
        this.resultSetWidth = rsm.getColumnCount();
        this.colNames = new String[this.resultSetWidth + 1];
        for (int i = 1; i <= this.resultSetWidth; i++) {
            this.colNames[i] = rsm.getColumnName(i);
        }
        System.err.println("ResultSetModel.add()");
        while (rs.next()) {
            add(new ResultRow(rs));
        }
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    /* loaded from: hsphere.zip:psoft/util/freemarker/ResultSetModel$ResultRow.class */
    public class ResultRow implements TemplateMethodModel {
        SimpleScalar[] values;

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) throws TemplateModelException {
            String key = HTMLEncoder.decode(list).get(0).toString();
            int colum = 0;
            try {
                colum = Integer.parseInt(key);
            } catch (NumberFormatException e) {
                try {
                    colum = ResultSetModel.this.f273rs.findColumn(key);
                } catch (SQLException e2) {
                }
            }
            if (0 == colum) {
                return null;
            }
            return this.values[colum];
        }

        public ResultRow(ResultSet rs) throws SQLException {
            ResultSetModel.this = r8;
            this.values = new SimpleScalar[r8.resultSetWidth + 1];
            for (int i = 1; i <= r8.resultSetWidth; i++) {
                this.values[i] = new SimpleScalar(rs.getString(i));
            }
        }
    }
}
