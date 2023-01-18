package psoft.hsphere.report;

import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import psoft.hsphere.Session;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/TemplateReport.class */
public class TemplateReport implements TemplateMethodModel {
    public static TemplateReport report = new TemplateReport();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel exec(List l) throws TemplateModelException {
        List l2 = HTMLEncoder.decode(l);
        String name = (String) l2.get(0);
        Report report2 = Report.getReport(name);
        if (report2 == null) {
            throw new TemplateModelException("No report named " + name);
        }
        Connection con = null;
        try {
            try {
                con = Session.getDb();
                TemplateModel templateModel = report2.get(con, l2.subList(1, l2.size()));
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException sqe) {
                        Session.getLog().warn("REPORT " + name, sqe);
                    }
                }
                return templateModel;
            } catch (Throwable th) {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException sqe2) {
                        Session.getLog().warn("REPORT " + name, sqe2);
                        throw th;
                    }
                }
                throw th;
            }
        } catch (Exception e) {
            Session.getLog().warn("REPORT " + name, e);
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException sqe3) {
                    Session.getLog().warn("REPORT " + name, sqe3);
                    return null;
                }
            }
            return null;
        }
    }
}
