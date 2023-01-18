package psoft.reminder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.hsphere.Mailer;
import psoft.hsphere.Session;
import psoft.util.TimeUtils;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/reminder/Reminder.class */
public class Reminder {
    protected static ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
    protected static Mailer mailer = new Mailer(config.getString("SMTP_HOST"), config.getString("FROM_ADDRESS"));

    public static void remove(String id) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM reminder WHERE id = ?");
            ps.setString(1, id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void notify(String id, String subject, String text, String recipient, int interval) throws Exception {
        remove(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Timestamp created = TimeUtils.getSQLTimestamp();
            Timestamp lastNotified = TimeUtils.getSQLTimestamp();
            Session.getMailer().sendMessage(recipient, subject, text, Session.getCurrentCharset());
            ps = con.prepareStatement("INSERT INTO reminder (id, subject, body, recipient, creation_date, last_notified, interv, count) VALUES (?, ?, ?, ?, ? ,?, ?, 1)");
            ps.setString(1, id);
            ps.setString(2, subject);
            ps.setString(3, text);
            ps.setString(4, recipient);
            ps.setTimestamp(5, created);
            ps.setTimestamp(6, lastNotified);
            ps.setInt(7, interval);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void main(String[] args) {
        try {
            config.getString("DB_NEWID");
            Toolbox.getDB(config);
            Connection con = Session.getDb();
            PreparedStatement ps1 = con.prepareStatement("UPDATE reminder SET count = ?, last_notified = ? WHERE id = ?");
            PreparedStatement ps = con.prepareStatement("SELECT id, subject, body, recipient, creation_date, last_notified, interv, count FROM reminder");
            ResultSet rs = ps.executeQuery();
            Calendar notify = TimeUtils.getCalendar();
            while (rs.next()) {
                Timestamp creationDate = rs.getTimestamp(5);
                Timestamp lastNotified = rs.getTimestamp(6);
                if (lastNotified == null) {
                    lastNotified = creationDate;
                }
                notify.setTime(lastNotified);
                int interval = rs.getInt(7);
                notify.roll(10, interval);
                Date now = TimeUtils.getDate();
                if (now.before(notify.getTime())) {
                    int count = rs.getInt(8);
                    String subject = rs.getString(2);
                    String text = rs.getString(3);
                    String recipient = rs.getString(4);
                    try {
                        mailer.sendMessage(recipient, subject + " #" + count + " (" + now + ")", text, Session.getCurrentCharset());
                        String id = rs.getString(1);
                        ps1.setString(3, id);
                        int i = count + 1;
                        ps1.setInt(1, count);
                        ps1.setTimestamp(2, new Timestamp(notify.getTime().getTime()));
                        ps1.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            con.close();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
    }
}
