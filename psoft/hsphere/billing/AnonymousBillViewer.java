package psoft.hsphere.billing;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.Bill;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.resource.p004tt.Ticket;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/billing/AnonymousBillViewer.class */
public class AnonymousBillViewer implements TemplateHashModel {
    public static final TemplateString STATUS_OK = new TemplateString("OK");
    private long clientAccountId;

    public boolean isEmpty() {
        return false;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        try {
            return "status".equals(key) ? STATUS_OK : "id".equals(key) ? new TemplateString(this.clientAccountId) : "plandescription".equals(key) ? new TemplateString(getPlanDescription()) : AccessTemplateMethodWrapper.getMethod(this, key);
        } catch (Exception e) {
            Ticket.create(e, new Long(this.clientAccountId));
            return new TemplateErrorResult("Internal Error, Tech Support Was Notified");
        }
    }

    public AnonymousBillViewer(long id) {
        this.clientAccountId = id;
    }

    public TemplateModel FM_view(long id) throws Exception {
        return new Bill(id);
    }

    public TemplateModel FM_view() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, opened, closed, description, amount FROM bill WHERE account_id = ? ORDER BY id DESC");
            ps.setLong(1, this.clientAccountId);
            try {
                ResultSet rs = ps.executeQuery();
                rs.next();
                int id = rs.getInt(1);
                Session.closeStatement(ps);
                con.close();
                return new Bill(id);
            } catch (Exception e) {
                Ticket.create(e, new Long(this.clientAccountId));
                TemplateErrorResult templateErrorResult = new TemplateErrorResult("Internal Error, Tech Support Was Notified");
                Session.closeStatement(ps);
                con.close();
                return templateErrorResult;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel list(PreparedStatement ps) throws SQLException {
        TemplateList list = new TemplateList();
        try {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("id", rs.getString(1));
                map.put("from", rs.getDate(2));
                map.put("to", rs.getDate(3));
                map.put("description", rs.getString(4));
                map.put("amount", rs.getString(5));
                list.add((TemplateModel) map);
            }
        } catch (SQLException se) {
            Session.getLog().warn("Unable to get old bills ", se);
        }
        return list;
    }

    public TemplateModel FM_list() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id, opened, closed, description, amount FROM bill WHERE account_id = ? ORDER BY id DESC");
            ps.setLong(1, this.clientAccountId);
            TemplateModel list = list(ps);
            Session.closeStatement(ps);
            con.close();
            return list;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private TemplateModel getPlanDescription() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT description FROM bill WHERE account_id = ? ORDER BY id DESC");
            ps.setLong(1, this.clientAccountId);
            try {
                ResultSet rs = ps.executeQuery();
                rs.next();
                String result = rs.getString(1);
                String result2 = result.substring(0, result.indexOf("#")).trim();
                Session.closeStatement(ps);
                con.close();
                return new TemplateString(result2);
            } catch (Exception e) {
                Ticket.create(e, new Long(this.clientAccountId));
                TemplateErrorResult templateErrorResult = new TemplateErrorResult("Internal Error, Tech Support Was Notified");
                Session.closeStatement(ps);
                con.close();
                return templateErrorResult;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
