package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.apache.log4j.Category;
import psoft.hsphere.Session;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.resource.HostEntry;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/tools/Plan2_3to2_4Updater.class */
public class Plan2_3to2_4Updater {
    private static Category log = Category.getInstance(Plan2_3to2_4Updater.class.getName());

    public static void main(String[] args) throws Exception {
        ExternalCP.initCP();
        PreparedStatement ps = null;
        Connection con = Session.getTransConnection();
        try {
            ps = con.prepareStatement("SELECT plan_id, value FROM plan_value WHERE name='_CREATED_BY_' AND value IN ('1','2','3','4','5','7','8','9', '12')");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    int planId = rs.getInt(1);
                    int value = Integer.parseInt(rs.getString(2));
                    try {
                        System.out.println("Processing plan: " + planId);
                        updatePlan(con, planId, value);
                    } catch (Exception ex) {
                        System.out.println("Error updating plan " + planId);
                        Session.getLog().error("Error updating plan ", ex);
                        con.rollback();
                    }
                } catch (NumberFormatException e) {
                    log.warn("Error", e);
                }
            }
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            System.exit(0);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.commitTransConnection(con);
            System.exit(0);
            throw th;
        }
    }

    private static void updatePlan(Connection con, int planId, int value) throws SQLException {
        String wizard;
        PreparedStatement psSetToNull = con.prepareStatement("UPDATE plan_ivalue SET mod_id = ? WHERE mod_id = '' AND plan_id = ?");
        psSetToNull.setNull(1, 12);
        psSetToNull.setInt(2, planId);
        psSetToNull.executeUpdate();
        psSetToNull.close();
        PreparedStatement psSetToNull2 = con.prepareStatement("UPDATE plan_iresource SET new_mod_id = ? WHERE new_mod_id = '' AND plan_id = ?");
        psSetToNull2.setNull(1, 12);
        psSetToNull2.setInt(2, planId);
        psSetToNull2.executeUpdate();
        psSetToNull2.close();
        PreparedStatement psSetToNull3 = con.prepareStatement("UPDATE plan_iresource SET mod_id = ? WHERE mod_id = '' AND plan_id = ?");
        psSetToNull3.setNull(1, 12);
        psSetToNull3.setInt(2, planId);
        psSetToNull3.executeUpdate();
        Session.closeStatement(psSetToNull3);
        switch (value) {
            case 1:
                wizard = "unix";
                updateMailDomain(con, planId);
                break;
            case 2:
                wizard = "windows";
                updateMailDomain(con, planId);
                break;
            case 3:
                wizard = "mysql";
                break;
            case 4:
                wizard = "unixreal";
                break;
            case 5:
                wizard = "windowsreal";
                break;
            case 6:
                return;
            case 7:
                wizard = FMACLManager.ADMIN;
                updateAdminTTAssignment(con, planId);
                break;
            case 8:
                wizard = "mailonly";
                break;
            case 9:
                wizard = "zeus";
                break;
            case 10:
            case 11:
            default:
                return;
            case 12:
                wizard = "vps";
                break;
        }
        if (value == 12) {
            changeIResourceMod(con, planId, 0, "", 7000, "", InitResource.PARENT_MOD);
            deleteMods(con, planId, 0, "");
        } else if (value == 8) {
            changeIResourceMod(con, planId, 0, "", 2, "signup", InitResource.PARENT_MOD);
            changeIResourceMod(con, planId, 2, "signup", HostEntry.VPS_IP, "signup", InitResource.PARENT_MOD);
            deleteMods(con, planId, 0, "signup");
            deleteMods(con, planId, 0, "empty");
        } else {
            updateAccountMod(con, planId);
        }
        PreparedStatement ps = con.prepareStatement("UPDATE plan_value SET value = ? WHERE name = '_CREATED_BY_' AND plan_id = ?");
        ps.setInt(2, planId);
        ps.setString(1, wizard);
        ps.executeUpdate();
        Session.closeStatement(ps);
        processServers(con, planId, wizard);
        updateQuotaAndTraffic(con, planId);
    }

    public static final void processServers(Connection con, int planId, String wizard) throws SQLException {
        String userHost;
        if (wizard.equals("windows")) {
            userHost = "windows_hosting";
        } else {
            userHost = "unix_hosting";
        }
        PreparedStatement ps = con.prepareStatement("SELECT type_id, value FROM plan_ivalue WHERE plan_id = ? AND type = 6");
        ps.setInt(1, planId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            int typeId = rs.getInt(1);
            String value = rs.getString(2);
            updateServer(con, planId, typeId, value, userHost);
        }
        Session.closeStatement(ps);
    }

    private static void updateServer(Connection con, int planId, int typeId, String value, String s) throws SQLException {
        String key;
        PreparedStatement ps = con.prepareStatement("INSERT INTO plan_value (plan_id, name, value) VALUES (?, ?, ?)");
        PreparedStatement ps1 = con.prepareStatement("UPDATE plan_ivalue SET value = ? WHERE plan_id = ? AND type_id = ? AND type = 6");
        ps.setInt(1, planId);
        ps.setString(3, value);
        ps1.setInt(2, planId);
        ps1.setInt(3, typeId);
        switch (typeId) {
            case 7:
                key = s;
                break;
            case HostEntry.VPS_IP /* 1000 */:
                key = "mail";
                break;
            case 6000:
                key = "mysql";
                break;
            case 6800:
                key = "mssql";
                break;
            case 6900:
                key = "pgsql";
                break;
            case 7000:
                key = "vps";
                break;
            default:
                key = "unknown" + typeId;
                log.warn("Unknown hostgroup for plan: " + planId + " type: " + typeId);
                break;
        }
        ps.setString(2, "_HOST_" + key);
        ps.executeUpdate();
        ps1.setString(1, key);
        ps1.executeUpdate();
        Session.closeStatement(ps);
        Session.closeStatement(ps1);
    }

    public static final void updateAccountMod(Connection con, int planId) throws SQLException {
        changeIResourceMod(con, planId, 0, "", 7, "signup", InitResource.PARENT_MOD);
        deleteMods(con, planId, 0, "");
    }

    public static final void updateAdminTTAssignment(Connection con, int planId) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE plan_value SET type_id = ? WHERE plan_id = ? AND name = '_ASSIGN_TT'");
        ps.setInt(1, 106);
        ps.setInt(2, planId);
        ps.executeUpdate();
    }

    public static final void updateMailDomain(Connection con, int planId) throws SQLException {
        deleteMod(con, planId, HostEntry.VPS_IP, "signup");
        changeIResourceMod(con, planId, HostEntry.VPS_IP, "", HostEntry.TAKEN_VPS_IP, "", InitResource.PARENT_MOD);
    }

    public static final void deleteMods(Connection con, int planId, int typeId, String ignoreMod) throws SQLException {
        PreparedStatement ps = con.prepareStatement("SELECT mod_id FROM plan_imod WHERE plan_id = ? AND type_id = ?");
        ps.setInt(1, planId);
        ps.setInt(2, typeId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            String mod = rs.getString(1);
            if (mod != null && !"".equals(mod) && !ignoreMod.equals(mod)) {
                deleteMod(con, planId, 0, mod);
            }
        }
        Session.closeStatement(ps);
    }

    public static final void changeIResourceMod(Connection con, int planId, int typeId, String mod, int new_type_id, String new_mod, String new_mod_to_use) throws SQLException {
        if (mod == null) {
            mod = "";
        }
        if (new_mod == null) {
            new_mod = "";
        }
        PreparedStatement ps = con.prepareStatement("UPDATE plan_iresource SET new_mod_id = ? WHERE plan_id = ? AND type_id = ? AND new_type_id = ?" + ("".equals(mod) ? " AND mod_id IS NULL" : " AND mod_id = ?") + ("".equals(new_mod) ? " AND new_mod_id IS NULL" : " AND new_mod_id = ?"));
        int counter = 1 + 1;
        ps.setString(1, new_mod_to_use);
        int counter2 = counter + 1;
        ps.setInt(counter, planId);
        int counter3 = counter2 + 1;
        ps.setInt(counter2, typeId);
        int counter4 = counter3 + 1;
        ps.setInt(counter3, new_type_id);
        if (!"".equals(mod)) {
            counter4++;
            ps.setString(counter4, mod);
        }
        if (!"".equals(new_mod)) {
            int i = counter4;
            int i2 = counter4 + 1;
            ps.setString(i, new_mod);
        }
        int count = ps.executeUpdate();
        Session.getLog().debug("UPDATE plan_iresource SET new_mod_id = " + new_mod + " WHERE plan_id = " + planId + " AND type_id = " + typeId + " AND new_type_id = " + new_type_id + ("".equals(mod) ? " AND mod_id IS NULL" : " AND mod_id = " + mod) + ("".equals(new_mod) ? " AND new_mod_id IS NULL" : " AND new_mod_id = " + new_mod) + " UPDATED:" + count);
        Session.closeStatement(ps);
    }

    public static final void deleteMod(Connection con, int planId, int typeId, String mod) throws SQLException {
        if (mod == null) {
            mod = "";
        }
        Session.getLog().debug("Deleting mod plan_id=" + planId + "typeId=" + typeId + " mod=" + mod);
        PreparedStatement ps = con.prepareStatement("DELETE FROM plan_imod WHERE plan_id = ? AND type_id = ? AND " + ("".equals(mod) ? "mod_id IS NULL" : "mod_id = ?"));
        ps.setInt(1, planId);
        ps.setInt(2, typeId);
        if (!"".equals(mod)) {
            ps.setString(3, mod);
        }
        ps.executeUpdate();
        Session.closeStatement(ps);
        PreparedStatement ps2 = con.prepareStatement("DELETE FROM plan_ivalue WHERE plan_id = ? AND type_id = ? AND " + ("".equals(mod) ? "mod_id IS NULL" : "mod_id = ?"));
        ps2.setInt(1, planId);
        ps2.setInt(2, typeId);
        if (!"".equals(mod)) {
            ps2.setString(3, mod);
        }
        ps2.executeUpdate();
        Session.closeStatement(ps2);
        PreparedStatement ps3 = con.prepareStatement("DELETE FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND " + ("".equals(mod) ? "mod_id IS NULL" : "mod_id = ?"));
        ps3.setInt(1, planId);
        ps3.setInt(2, typeId);
        if (!"".equals(mod)) {
            ps3.setString(3, mod);
        }
        ps3.executeUpdate();
        Session.closeStatement(ps3);
    }

    public static final void updateQuotaAndTraffic(Connection con, int planId) throws SQLException {
        setPlanFreeValues(con, planId, 1008);
        setPlanFreeValues(con, planId, 4001);
        setPlanFreeValues(con, planId, 4002);
        setPlanFreeValues(con, planId, 4003);
        setPlanFreeValues(con, planId, 6005);
        setPlanFreeValues(con, planId, 6804);
        setPlanFreeValues(con, planId, 6904);
        setPlanFreeValues(con, planId, 7002);
        setPlanFreeValues(con, planId, 7003);
        setPlanFreeValues(con, planId, 121);
    }

    public static final void setPlanFreeValues(Connection con, int planId, int typeId) throws SQLException {
        PreparedStatement ps = con.prepareStatement("UPDATE plan_ivalue SET type = 7 WHERE plan_id = ? AND type_id = ?");
        ps.setInt(1, planId);
        ps.setInt(2, typeId);
        PreparedStatement ps1 = con.prepareStatement("SELECT value FROM plan_value WHERE plan_id = ? AND name=? AND type_id = ?");
        ps1.setInt(1, planId);
        ps1.setString(2, "_FREE_UNITS_");
        ps1.setInt(3, typeId);
        PreparedStatement ps2 = con.prepareStatement("INSERT INTO plan_value(plan_id, type_id, name, value) SELECT ?, ?, ?, value FROM plan_ivalue WHERE order_id = ? AND type_id = ? AND plan_id = ?");
        ps2.setInt(1, planId);
        ps2.setInt(2, typeId);
        ps2.setString(3, "_FREE_UNITS_");
        ps2.setInt(4, 0);
        ps2.setInt(5, typeId);
        ps2.setInt(6, planId);
        ResultSet rs = ps1.executeQuery();
        if (!rs.next()) {
            ps2.executeUpdate();
        }
        ps.executeUpdate();
    }
}
