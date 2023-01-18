package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/converter/TicketMigrator.class */
public class TicketMigrator {
    static MessageFormat newIdQueryForm;

    protected static long getNewId(Connection con) throws Exception {
        Object[] args = {"ticket_seq"};
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement(newIdQueryForm.format(args));
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long j = rs.getLong(1);
                Session.closeStatement(ps);
                return j;
            }
            Session.closeStatement(ps);
            throw new SQLException("Unable to get new id");
        } catch (Throwable th) {
            Session.closeStatement(ps);
            throw th;
        }
    }

    public static void main(String[] args) throws Exception {
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        Database db = Toolbox.getDB(config);
        Connection con = db.getConnection();
        PreparedStatement ps = null;
        Statement st = null;
        newIdQueryForm = new MessageFormat(config.getString("DB_NEWID"));
        try {
            ps = con.prepareStatement("INSERT INTO ticket (id, created, priority, user_id, account_id, resource_id, resource_type, closed, assigned, title, type) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0)");
            st = con.createStatement();
            ResultSet rs = st.executeQuery("SELECT title, status, created, lastmod, closed, assigned, priority, severity, user_id, account_id, resource_id, resource_type, email, description, modby, id FROM troubleticket");
            while (rs.next()) {
                long newTicketId = getNewId(con);
                ps.setLong(1, newTicketId);
                ps.setTimestamp(2, rs.getTimestamp(3));
                ps.setInt(3, rs.getInt(7));
                ps.setLong(4, rs.getLong(9));
                ps.setLong(5, rs.getLong(10));
                ps.setLong(6, rs.getLong(11));
                ps.setInt(7, rs.getInt(12));
                if (rs.getInt(5) == 1) {
                    ps.setTimestamp(8, rs.getTimestamp(4));
                } else {
                    ps.setNull(8, 93);
                }
                ps.setLong(9, rs.getLong(6));
                ps.setString(10, rs.getString(1));
                ps.executeUpdate();
                getMessages(db, rs.getLong(16), newTicketId);
            }
            st.executeUpdate("DROP TABLE tt_types");
            st.executeUpdate("DROP TABLE ttanswer");
            st.executeUpdate("DROP TABLE ttquestion");
            st.executeUpdate("DROP TABLE ttaction");
            st.executeUpdate("DROP TABLE troubleticket");
            st.executeUpdate("DELETE FROM parent_child WHERE child_id = 105");
            Session.closeStatement(ps);
            Session.closeStatement(st);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(st);
            con.close();
            throw th;
        }
    }

    protected static void getMessages(Database db, long oldId, long newId) throws Exception {
        Connection con = db.getConnection();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT created, message FROM ttquestion WHERE tt_id = ?");
            ps2.setLong(1, oldId);
            PreparedStatement ps12 = con.prepareStatement("INSERT INTO ttmessage (id, message, created, type) VALUES (?, ?, ?, 0)");
            ps12.setLong(1, newId);
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                String value = db.getClobValue(rs, 2);
                if (value != null) {
                    db.setClobValue(ps12, 2, value);
                } else {
                    ps12.setNull(2, 12);
                }
                ps12.setTimestamp(3, rs.getTimestamp(1));
                ps12.executeUpdate();
            }
            ps = con.prepareStatement("SELECT created, answer, note, answered FROM ttanswer WHERE tt_id = ?");
            ps.setLong(1, oldId);
            ps1 = con.prepareStatement("INSERT INTO ttmessage (id, message, note, created, aid, type) VALUES (?, ?, ?, ?, ?, 1)");
            ps1.setLong(1, newId);
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                String value2 = db.getClobValue(rs2, 2);
                if (value2 != null) {
                    db.setClobValue(ps1, 2, value2);
                } else {
                    ps1.setNull(2, 12);
                }
                String value3 = db.getClobValue(rs2, 3);
                if (value3 != null) {
                    db.setClobValue(ps1, 3, value3);
                } else {
                    ps1.setNull(3, 12);
                }
                ps1.setTimestamp(4, rs2.getTimestamp(1));
                ps1.setLong(5, rs2.getLong(4));
                ps1.executeUpdate();
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }
}
