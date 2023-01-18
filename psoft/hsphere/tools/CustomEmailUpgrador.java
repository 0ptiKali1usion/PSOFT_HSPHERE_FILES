package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.resource.admin.CustomEmailMessage;

/* loaded from: hsphere.zip:psoft/hsphere/tools/CustomEmailUpgrador.class */
public class CustomEmailUpgrador extends C0004CP {
    public CustomEmailUpgrador() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    public static void main(String[] args) throws Exception {
        new PrioritySupportConfigurator();
        String locale = args.length < 1 ? CustomEmailMessage.DEFAULT_LOCALE : args[0];
        for (String tag : CustomEmailMessage.getTagSet()) {
            CustomEmailMessage.updateFromTemplate(0L, tag, locale, true, CustomEmailMessage.DefaultEmailTemplate.isNoCC(tag));
            System.out.println("Upgraded: " + tag);
        }
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT MAX(action) AS max_action FROM mailman");
            ResultSet max = ps.executeQuery();
            max.next();
            if (max.getInt(1) > 6) {
                System.out.println("Actions already was changed");
                Session.closeStatement(ps);
                con.close();
                return;
            }
            ResultSet resellers = con.prepareStatement("SELECT DISTINCT reseller_id FROM mailman").executeQuery();
            while (resellers.next()) {
                PreparedStatement ps2 = con.prepareStatement("SELECT * FROM mailman WHERE reseller_id = ?");
                ps2.setLong(1, resellers.getLong("reseller_id"));
                ResultSet mailman = ps2.executeQuery();
                ArrayList welcome = new ArrayList();
                ArrayList ticket = new ArrayList();
                while (mailman.next()) {
                    int action = mailman.getInt("action");
                    PreparedStatement ps3 = con.prepareStatement("UPDATE mailman SET action = ? WHERE action = ? AND reseller_id = ?");
                    ps3.setLong(2, action);
                    ps3.setLong(3, mailman.getLong("reseller_id"));
                    String email = mailman.getString("email");
                    switch (action) {
                        case 1:
                            if (!welcome.contains(email.intern())) {
                                ps3.setLong(1, -4L);
                                ps3.executeUpdate();
                                welcome.add(email.intern());
                                break;
                            } else {
                                deleteMail(con, action, mailman.getLong("reseller_id"));
                                break;
                            }
                        case 2:
                            if (!welcome.contains(email.intern())) {
                                ps3.setLong(1, -4L);
                                ps3.executeUpdate();
                                welcome.add(email.intern());
                                break;
                            } else {
                                deleteMail(con, action, mailman.getLong("reseller_id"));
                                break;
                            }
                        case 3:
                            if (!ticket.contains(email.intern())) {
                                ps3.setLong(1, 8L);
                                ps3.executeUpdate();
                                ticket.add(email.intern());
                                break;
                            } else {
                                deleteMail(con, action, mailman.getLong("reseller_id"));
                                break;
                            }
                        case 4:
                            ps3.setLong(1, 9L);
                            ps3.executeUpdate();
                            break;
                        case 5:
                            if (!welcome.contains(email.intern())) {
                                ps3.setLong(1, -4L);
                                ps3.executeUpdate();
                                welcome.add(email.intern());
                                break;
                            } else {
                                deleteMail(con, action, mailman.getLong("reseller_id"));
                                break;
                            }
                        case 6:
                            if (!ticket.contains(email.intern())) {
                                ps3.setLong(1, 8L);
                                ps3.executeUpdate();
                                ticket.add(email.intern());
                                break;
                            } else {
                                deleteMail(con, action, mailman.getLong("reseller_id"));
                                break;
                            }
                    }
                }
            }
            PreparedStatement ps4 = con.prepareStatement("UPDATE mailman SET action = 4 WHERE action = -4");
            ps4.executeUpdate();
            Session.closeStatement(ps4);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }

    protected static void deleteMail(Connection con, int action, long reseller_id) throws Exception {
        PreparedStatement ps = con.prepareStatement("DELETE FROM mailman WHERE action = ? AND reseller_id = ?");
        ps.setInt(1, action);
        ps.setLong(2, reseller_id);
        ps.executeUpdate();
    }
}
