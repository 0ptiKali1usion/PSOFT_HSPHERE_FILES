package psoft.hsphere.report;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import psoft.hsphere.Session;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.web.utils.HTMLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/report/ReportToolbox.class */
public class ReportToolbox implements TemplateHashModel {
    public static final ReportToolbox toolbox = new ReportToolbox();
    private static final BillingInfoAccessor biAccessor = new BillingInfoAccessor();

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) {
        if ("bi".equals(key)) {
            return biAccessor;
        }
        return null;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/report/ReportToolbox$BillingInfoAccessor.class */
    static class BillingInfoAccessor implements TemplateMethodModel {
        BillingInfoAccessor() {
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            String id = (String) HTMLEncoder.decode(l).get(0);
            Connection con = null;
            PreparedStatement ps = null;
            try {
                try {
                    con = Session.getDb();
                    ps = con.prepareStatement("SELECT bi_id FROM accounts WHERE id = ?");
                    ps.setLong(1, Long.parseLong(id));
                    ResultSet rs = ps.executeQuery();
                    if (!rs.next()) {
                        Session.closeStatement(ps);
                        if (con != null) {
                            try {
                                con.close();
                            } catch (SQLException e) {
                            }
                        }
                        return null;
                    }
                    BillingInfoObject billingInfoObject = new BillingInfoObject(rs.getLong(1));
                    Session.closeStatement(ps);
                    if (con != null) {
                        try {
                            con.close();
                        } catch (SQLException e2) {
                        }
                    }
                    return billingInfoObject;
                } catch (Exception se) {
                    Session.getLog().info("Unable to retrieve billing info #" + id, se);
                    Session.closeStatement(ps);
                    if (con != null) {
                        try {
                            con.close();
                        } catch (SQLException e3) {
                            return null;
                        }
                    }
                    return null;
                }
            } catch (Throwable th) {
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException e4) {
                        throw th;
                    }
                }
                throw th;
            }
        }
    }
}
