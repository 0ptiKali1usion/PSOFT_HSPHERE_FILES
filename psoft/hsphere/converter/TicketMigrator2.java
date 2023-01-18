package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Session;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/converter/TicketMigrator2.class */
public class TicketMigrator2 extends TicketMigrator {
    public static void main(String[] args) throws Exception {
        ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
        Database db = Toolbox.getDB(config);
        Connection con = db.getConnection();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        newIdQueryForm = new MessageFormat(config.getString("DB_NEWID"));
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT MAX(ttmessage.created), ticket.id FROM ticket, ttmessage WHERE ticket.id = ttmessage.id AND ttmessage.type = ? GROUP BY ticket.id");
            ps2.setInt(1, 1);
            PreparedStatement ps12 = con.prepareStatement("UPDATE ticket SET answered = ? WHERE id = ?");
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                ps12.setTimestamp(1, rs.getTimestamp(1));
                ps12.setLong(2, rs.getLong(2));
                ps12.executeUpdate();
            }
            ps12.close();
            ps1 = con.prepareStatement("UPDATE ticket SET modified = ? WHERE id = ?");
            ps2.setInt(1, 0);
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                ps1.setTimestamp(1, rs2.getTimestamp(1));
                ps1.setLong(2, rs2.getLong(2));
                ps1.executeUpdate();
            }
            ps2.close();
            ps = con.prepareStatement("UPDATE ticket SET email = (SELECT contact_info.email FROM accounts, contact_info WHERE ticket.account_id = accounts.id AND accounts.ci_id = contact_info.id)");
            ps.executeUpdate();
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
