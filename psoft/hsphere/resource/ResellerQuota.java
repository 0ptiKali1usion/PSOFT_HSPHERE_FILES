package psoft.hsphere.resource;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ResellerQuota.class */
public class ResellerQuota extends SummaryQuota {
    public ResellerQuota(ResourceId id) throws Exception {
        super(id);
    }

    public ResellerQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    @Override // psoft.hsphere.resource.SummaryQuota
    public double getUsage(Date dBegin, Date dEnd) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT SUM(used)/COUNT(DISTINCT(CDATE)) FROM usage_log u, accounts a WHERE   a.id = u.account_id and a.reseller_id = ? AND u.cdate BETWEEN ? AND ?");
            ps.setLong(1, Session.getUser().getId());
            ps.setDate(2, dBegin);
            ps.setDate(3, dEnd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double d = rs.getDouble(1);
                Session.closeStatement(ps);
                con.close();
                return d;
            }
            Session.closeStatement(ps);
            con.close();
            return 0.0d;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
