package psoft.hsphere;

import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import psoft.p000db.Database;
import psoft.util.Toolbox;

/* loaded from: hsphere.zip:psoft/hsphere/ConfigExport.class */
public class ConfigExport {

    /* renamed from: db */
    static Database f32db;
    static Hashtable type;

    public static void main(String[] argv) {
        System.out.println("<?xml version=\"1.0\"?>");
        System.out.println("<!DOCTYPE config SYSTEM \"config.dtd\">");
        System.out.println("\n<config>");
        try {
            type = new Hashtable();
            ResourceBundle config = PropertyResourceBundle.getBundle("psoft_config.hsphere");
            f32db = Toolbox.getDB(config);
            Connection con = f32db.getConnection();
            PreparedStatement ps = con.prepareStatement("SELECT id, name, description, price, rprice, required, priority FROM type_name ORDER BY id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("<type id=\"" + rs.getString(1) + "\" price=\"" + (rs.getString(4) == null ? "" : rs.getString(4)) + "\" rprice=\"" + (rs.getString(5) == null ? "" : rs.getString(5)) + "\" name=\"" + rs.getString(2) + "\"" + (rs.getInt(6) == 0 ? "" : " required=\"" + rs.getInt(6) + "\"") + (rs.getInt(7) == 0 ? "" : " priority=\"" + rs.getInt(7) + "\"") + ">" + rs.getString(3) + "</type>");
                type.put(rs.getString(1), rs.getString(2));
            }
            System.out.println("");
            ps.close();
            PreparedStatement ps2 = con.prepareStatement("SELECT id, description, disabled, billing, cinfo, reseller_id,deleted FROM plans ORDER by id");
            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                String disabled = rs2.getString(3);
                int billable = rs2.getInt(4);
                int cinfo = rs2.getInt(5);
                long resellerId = rs2.getLong(6);
                int deleted = rs2.getInt(7);
                System.out.println("<plan id=\"" + rs2.getString(1) + "\" description=\"" + rs2.getString(2) + "\"" + ((disabled == null || !disabled.equals("1")) ? "" : " disabled=\"1\"") + (billable != 1 ? " billable=\"" + billable + "\"" : "") + (cinfo != 1 ? " cinfo=\"" + cinfo + "\"" : "") + (resellerId != 1 ? " reseller_id=\"" + resellerId + "\"" : "") + (deleted == 1 ? " deleted=\"1\"" : "") + ">");
                printPlan(rs2.getString(1));
                System.out.println("</plan>\n\n");
            }
            Session.closeStatement(ps2);
            con.close();
        } catch (Exception se) {
            se.printStackTrace();
        }
        System.out.println("\n</config>");
    }

    public static void printPlan(String planId) throws SQLException {
        Connection con = f32db.getConnection();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT a_id FROM plan_access WHERE id = " + planId + " ORDER BY a_id");
            ResultSet rs = ps2.executeQuery();
            while (rs.next()) {
                System.out.println("  <access>" + rs.getString(1) + "</access>");
            }
            ps2.close();
            ps = con.prepareStatement("SELECT name, value FROM plan_value WHERE plan_id = " + planId + " AND type_id is NULL ORDER BY name");
            ResultSet rs2 = ps.executeQuery();
            while (rs2.next()) {
                System.out.println("  <value name=\"" + rs2.getString(1) + "\">" + rs2.getString(2) + "</value>");
            }
            Session.closeStatement(ps);
            con.close();
            printInitType(planId);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    static void printInitType(String planId) throws SQLException {
        Connection con = f32db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT type_id, class_name, disabled FROM plan_resource WHERE plan_id = " + planId + " ORDER BY type_id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("  <resource name=\"" + type.get(rs.getString(1)) + "\" class=\"" + rs.getString(2) + "\"" + (rs.getInt(3) == 1 ? " disabled=\"1\"" : "") + ">");
                printValues(planId, rs.getString(1));
                printMod(planId, rs.getString(1));
                System.out.println("  </resource>\n");
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    static void printValues(String planId, String typeId) throws SQLException {
        Connection con = f32db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT name, value FROM plan_value WHERE plan_id = " + planId + " AND type_id = " + typeId + " ORDER BY name");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                System.out.println("    <value name=\"" + rs.getString(1) + "\">" + rs.getString(2) + "</value>");
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    static void printMod(String planId, String typeId) throws SQLException {
        String str;
        new HashSet();
        Connection con = f32db.getConnection();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT mod_id, disabled  FROM plan_imod WHERE plan_id = " + planId + " AND type_id = " + typeId + " ORDER BY mod_id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String mod = rs.getString(1);
                String disabled = rs.getString(2);
                if (mod == null) {
                    mod = "";
                }
                PrintStream printStream = System.out;
                StringBuilder append = new StringBuilder().append("    <mod");
                if (mod.length() == 0) {
                    str = "";
                } else {
                    str = " name=\"" + mod + "\"" + ((disabled == null || !disabled.equals("1")) ? "" : " disabled=\"1\"");
                }
                printStream.println(append.append(str).append(">").toString());
                printMod(planId, typeId, mod);
                System.out.println("    </mod>");
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* JADX WARN: Finally extract failed */
    static void printMod(String planId, String typeId, String modId) throws SQLException {
        String type2;
        Connection con = f32db.getConnection();
        PreparedStatement ps = null;
        try {
            if (modId.length() != 0) {
                ps = con.prepareStatement("SELECT value, disabled, type, label FROM plan_ivalue WHERE plan_id = ? AND type_id = ? AND mod_id = ? ORDER BY order_id");
                ps.setString(3, modId);
            } else {
                ps = con.prepareStatement("SELECT value, disabled, type, label FROM plan_ivalue WHERE plan_id = ? AND type_id = ? AND (mod_id = '' OR mod_id IS NULL) ORDER BY order_id");
            }
            ps.setLong(1, Long.parseLong(planId));
            ps.setLong(2, Integer.parseInt(typeId));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                switch (rs.getInt(3)) {
                    case 1:
                        type2 = " type=\"field\" label=\"" + rs.getString(4) + "\"";
                        break;
                    case 2:
                        type2 = " type=\"relative\"";
                        break;
                    case 3:
                        type2 = " type=\"absolute\"";
                        break;
                    case 4:
                        type2 = " type=\"relative_rec\"";
                        break;
                    case 5:
                        type2 = " type=\"absolute_rec\"";
                        break;
                    case 6:
                        type2 = " type=\"hostgroup\"";
                        break;
                    default:
                        type2 = "";
                        break;
                }
                System.out.println("      <initvalue" + type2 + (rs.getInt(2) == 1 ? " disabled=\"1\"" : "") + ">" + rs.getString(1) + "</initvalue>");
            }
        } catch (SQLException e) {
        }
        try {
            try {
                if (modId.length() != 0) {
                    ps = con.prepareStatement("SELECT new_type_id, new_mod_id, disabled FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND mod_id = ? ORDER BY order_id");
                    ps.setString(3, modId);
                } else {
                    ps = con.prepareStatement("SELECT new_type_id, new_mod_id, disabled FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND (mod_id = '' OR mod_id IS NULL) ORDER BY order_id");
                }
                ps.setLong(1, Long.parseLong(planId));
                ps.setInt(2, Integer.parseInt(typeId));
                ResultSet rs2 = ps.executeQuery();
                while (rs2.next()) {
                    String newModId = rs2.getString(2);
                    if (newModId == null) {
                        newModId = "";
                    }
                    System.out.println("      <initresource name=\"" + type.get(rs2.getString(1)) + "\"" + (rs2.getInt(3) == 1 ? " disabled=\"1\"" : "") + (newModId.length() != 0 ? " mod=\"" + newModId + "\"/>" : "/>"));
                }
                Session.closeStatement(ps);
                con.close();
            } catch (SQLException se) {
                se.printStackTrace();
                Session.closeStatement(ps);
                con.close();
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
