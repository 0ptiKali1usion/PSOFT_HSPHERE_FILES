package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;
import psoft.hsphere.global.Globals;

/* loaded from: hsphere.zip:psoft/hsphere/tools/GLBValueCorrector.class */
public class GLBValueCorrector extends C0004CP {
    public GLBValueCorrector() throws Exception {
        super("psoft_config.hsphere");
        Session.setResellerId(1L);
    }

    public void correctSettings() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            try {
                ps1 = con.prepareStatement("SELECT name, value FROM settings WHERE name LIKE ?");
                ps2 = con.prepareStatement("UPDATE settings SET name = ?, value = ? WHERE name = ?");
                ps1.setString(1, "_GLB_DISABLED_%");
                ResultSet rs = ps1.executeQuery();
                while (rs.next()) {
                    String name = rs.getString(1);
                    String value = rs.getString(2);
                    if (name != null && name.startsWith("_GLB_DISABLED_")) {
                        String newName = name.substring("_GLB_DISABLED_".length());
                        String value2 = "1".equals(value) ? "0" : "1";
                        ps2.setString(1, Globals.GLB_VALUE_PREFIX + newName);
                        ps2.setString(2, value2);
                        ps2.setString(3, name);
                        ps2.executeUpdate();
                        ps2.clearParameters();
                    }
                }
                Session.closeStatement(ps2);
                Session.closeStatement(ps1);
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps1);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps2);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    public void correctPlanValueTable() throws Exception {
        List TO_LOWERCASE = Arrays.asList("CP_SSL_ip_based", "CP_SSL_port_based");
        Connection con = Session.getDb();
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        try {
            try {
                ps1 = con.prepareStatement("SELECT plan_id, name, value FROM plan_value WHERE name LIKE ?");
                ps2 = con.prepareStatement("UPDATE plan_value SET name = ?, value = ? WHERE plan_id = ? AND name = ?");
                ps1.setString(1, "_GLB_DISABLED_%");
                ResultSet rs = ps1.executeQuery();
                while (rs.next()) {
                    long plan_id = rs.getLong(1);
                    String name = rs.getString(2);
                    String value = rs.getString(3);
                    if (name != null) {
                        String newName = name.substring("_GLB_DISABLED_".length());
                        String value2 = "1".equals(value) ? "0" : "1";
                        ps2.setString(1, Globals.GLB_VALUE_PREFIX + newName);
                        ps2.setString(2, value2);
                        ps2.setLong(3, plan_id);
                        ps2.setString(4, name);
                        ps2.executeUpdate();
                        ps2.clearParameters();
                    }
                }
                ps1.clearParameters();
                ps1.setString(1, "_GLB_%");
                ResultSet rs2 = ps1.executeQuery();
                while (rs2.next()) {
                    long plan_id2 = rs2.getLong(1);
                    String name2 = rs2.getString(2);
                    String value3 = rs2.getString(3);
                    if (name2 != null) {
                        String newName2 = name2.substring(Globals.GLB_VALUE_PREFIX.length());
                        if (TO_LOWERCASE.contains(newName2)) {
                            ps2.setString(1, Globals.GLB_VALUE_PREFIX + newName2.toLowerCase());
                            ps2.setString(2, value3);
                            ps2.setLong(3, plan_id2);
                            ps2.setString(4, name2);
                            ps2.executeUpdate();
                            ps2.clearParameters();
                        }
                    }
                }
                Session.closeStatement(ps2);
                Session.closeStatement(ps1);
                con.close();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(1);
                Session.closeStatement(ps2);
                Session.closeStatement(ps1);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps2);
            Session.closeStatement(ps1);
            con.close();
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        GLBValueCorrector update = new GLBValueCorrector();
        update.correctSettings();
        update.correctPlanValueTable();
        System.out.println("Finished");
        System.exit(0);
    }
}
