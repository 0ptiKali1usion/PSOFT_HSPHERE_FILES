package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/converter/ForwardFix.class */
public class ForwardFix extends C0004CP {
    public ForwardFix() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            ForwardFix test = new ForwardFix();
            test.m30go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m30go() throws Exception {
        PreparedStatement ps;
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            PreparedStatement ps3 = con.prepareStatement("SELECT email_foreign FROM mail_forwards");
            ResultSetMetaData rsmd = ps3.executeQuery().getMetaData();
            if (rsmd.getColumnType(1) == 2005 || rsmd.getColumnType(1) == 2004 || rsmd.getColumnType(1) == -1) {
                System.out.println("Already migrated");
                Session.closeStatement(ps3);
                Session.closeStatement(null);
                con.close();
                return;
            }
            if ("ORACLE".equals(Session.getPropertyString("DB_VENDOR"))) {
                ps3.close();
                PreparedStatement ps4 = con.prepareStatement("ALTER TABLE mail_forwards ADD foreign2 varchar(128)");
                ps4.executeUpdate();
                ps4.close();
                PreparedStatement ps5 = con.prepareStatement("UPDATE mail_forwards SET foreign2 = email_foreign");
                ps5.executeUpdate();
                ps5.close();
                PreparedStatement ps6 = con.prepareStatement("ALTER TABLE mail_forwards DROP COLUMN email_foreign");
                ps6.executeUpdate();
                ps6.close();
                PreparedStatement ps7 = con.prepareStatement("ALTER TABLE mail_forwards ADD email_foreign LONG");
                ps7.executeUpdate();
                ps7.close();
                ps2 = con.prepareStatement("UPDATE mail_forwards SET email_foreign = ? WHERE id = ?");
                PreparedStatement ps8 = con.prepareStatement("SELECT id, foreign2 FROM mail_forwards");
                ResultSet rs = ps8.executeQuery();
                while (rs.next()) {
                    Session.setClobValue(ps2, 1, rs.getString(2));
                    ps2.setLong(2, rs.getLong(1));
                    ps2.executeUpdate();
                }
                ps8.close();
                PreparedStatement ps9 = con.prepareStatement("ALTER TABLE mail_forwards DROP COLUMN foreign2");
                ps9.executeUpdate();
                ps9.close();
                ps = con.prepareStatement("ALTER TABLE mail_forwards MODIFY email_foreign LONG NOT NULL");
                ps.executeUpdate();
            } else {
                ps3.close();
                PreparedStatement ps10 = con.prepareStatement("CREATE TABLE mail_forwards_new ( id int8 NOT NULL PRIMARY KEY, email_local varchar(64) NOT NULL, email_foreign TEXT NOT NULL)");
                ps10.executeUpdate();
                ps10.close();
                PreparedStatement ps11 = con.prepareStatement("INSERT INTO mail_forwards_new(id, email_local, email_foreign) SELECT id, email_local, email_foreign FROM mail_forwards");
                ps11.executeUpdate();
                ps11.close();
                PreparedStatement ps12 = con.prepareStatement("DROP TABLE mail_forwards");
                ps12.executeUpdate();
                ps12.close();
                ps = con.prepareStatement("ALTER TABLE mail_forwards_new RENAME TO mail_forwards");
                ps.executeUpdate();
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }
}
