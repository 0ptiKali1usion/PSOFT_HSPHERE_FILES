package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Category;
import psoft.hsphere.Session;
import psoft.hsphere.plan.InitResource;
import psoft.hsphere.resource.email.AntiSpam;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MailPlansRecovery.class */
public class MailPlansRecovery {
    private ToolLogger log;
    private boolean applyFix;
    private String planIds;
    private boolean force;
    private static Category hslog = Category.getInstance(MailPlansRecovery.class.getName());

    public MailPlansRecovery(ToolLogger log, String planIds, boolean applyFix, boolean force) {
        this.applyFix = false;
        this.planIds = null;
        this.force = false;
        this.log = log;
        this.planIds = planIds;
        this.applyFix = applyFix;
        this.force = force;
    }

    private void process() throws Exception {
        Connection con = Session.getDb();
        if (this.planIds == null) {
            StringBuffer sb = new StringBuffer();
            PreparedStatement ps = con.prepareStatement("SELECT DISTINCT plan_id FROM plan_value WHERE name=? AND value = ?");
            ps.setString(1, "_CREATED_BY_");
            ps.setString(2, "mailonly");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                sb.append(rs.getString(1)).append(',');
            }
            this.planIds = sb.toString();
            this.planIds = this.planIds.substring(0, this.planIds.length() - 1);
        }
        this.log.outMessage("Fixing acccount's mod\n");
        findDoubleIR(this.planIds, 0L, 2L, "", InitResource.PARENT_MOD);
        this.log.outMessage("Fixing domain's mod\n");
        findDoubleIR(this.planIds, 2L, 1000L, "signup", InitResource.PARENT_MOD);
    }

    private void findDoubleIR(String planIds, long typeId, long newTypeId, String modId, String correctModId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT plan_id, new_mod_id FROM plan_iresource  WHERE plan_id IN (" + planIds + ") AND type_id = ? AND mod_id " + ((modId == null || "".equals(modId)) ? "IS NULL" : " = ?") + " AND new_type_id = ? AND new_mod_id " + ((correctModId == null || "".equals(correctModId)) ? "IS NOT NULL" : " <>  ? AND new_mod_id IS NOT NULL") + " GROUP BY plan_id, new_mod_id");
            int i = 1 + 1;
            ps.setLong(1, typeId);
            if (!"".equals(modId) && modId != null) {
                i++;
                ps.setString(i, modId);
            }
            int i2 = i;
            int i3 = i + 1;
            ps.setLong(i2, newTypeId);
            if (!"".equals(correctModId) && correctModId != null) {
                int i4 = i3 + 1;
                ps.setString(i3, correctModId);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                this.log.outMessage("Fixing plan " + rs.getString("plan_id") + " type_id " + typeId + " new_type_id " + newTypeId + " mod_id " + (("".equals(modId) || modId == null) ? AntiSpam.DEFAULT_LEVEL_VALUE : modId) + " currectMod " + (("".equals(correctModId) || correctModId == null) ? AntiSpam.DEFAULT_LEVEL_VALUE : correctModId) + "\n");
                fixDoubleIR(rs.getLong("plan_id"), typeId, newTypeId, modId, correctModId);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    private void fixDoubleIR(long planId, long typeId, long newTypeId, String modId, String correctModId) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT count(*) FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND mod_id " + ((modId == null || "".equals(modId)) ? "IS NULL" : " = ?") + " AND new_type_id = ? AND new_mod_id " + ((correctModId == null || "".equals(correctModId)) ? "IS NULL" : " = ?"));
            int i = 1 + 1;
            ps.setLong(1, planId);
            int i2 = i + 1;
            ps.setLong(i, typeId);
            if (!"".equals(modId) && modId != null) {
                i2++;
                ps.setString(i2, modId);
            }
            int i3 = i2;
            int i4 = i2 + 1;
            ps.setLong(i3, newTypeId);
            if (!"".equals(correctModId) && correctModId != null) {
                int i5 = i4 + 1;
                ps.setString(i4, correctModId);
            }
            ResultSet rs = ps.executeQuery();
            boolean correctModFound = false;
            if (rs.next()) {
                correctModFound = rs.getLong(1) > 0;
            }
            this.log.outMessage("Deleting erroneus IR: plan_id " + planId + " type_id " + typeId + " mod_id " + (("".equals(modId) || modId == null) ? AntiSpam.DEFAULT_LEVEL_VALUE : modId) + " new_type_id " + newTypeId + "\n");
            PreparedStatement ps1 = con.prepareStatement("DELETE FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND mod_id " + ((modId == null || "".equals(modId)) ? "IS NULL" : " = ?") + " AND new_type_id = ? AND new_mod_id " + ((correctModId == null || "".equals(correctModId)) ? "IS NOT NULL" : " <> ? AND new_mod_id IS NOT NULL"));
            int i6 = 1 + 1;
            ps1.setLong(1, planId);
            int i7 = i6 + 1;
            ps1.setLong(i6, typeId);
            if (!"".equals(modId) && modId != null) {
                i7++;
                ps1.setString(i7, modId);
            }
            int i8 = i7;
            int i9 = i7 + 1;
            ps1.setLong(i8, newTypeId);
            if (!"".equals(correctModId) && correctModId != null) {
                int i10 = i9 + 1;
                ps1.setString(i9, correctModId);
            }
            ps1.executeUpdate();
            if (!correctModFound) {
                PreparedStatement ps2 = con.prepareStatement("SELECT max(order_id) FROM plan_iresource WHERE plan_id = ? AND type_id = ? AND mod_id " + ((modId == null || "".equals(modId)) ? "IS NULL" : " = ?"));
                ps2.setLong(1, planId);
                ps2.setLong(2, typeId);
                if (!"".equals(modId) && modId != null) {
                    ps2.setString(3, modId);
                }
                ResultSet rs1 = ps2.executeQuery();
                rs1.next();
                this.log.outMessage("Inserting correct mod for plan_id " + planId + " type_id " + typeId + " mod_id " + (("".equals(modId) || modId == null) ? AntiSpam.DEFAULT_LEVEL_VALUE : modId) + " new_type_id " + newTypeId + " order_id " + rs1.getLong(1) + "1\n");
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO plan_iresource(plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES(?,?,?,?,?,?,?)");
                ps3.setLong(1, planId);
                ps3.setLong(2, typeId);
                if (modId == null || "".equals(modId)) {
                    ps3.setNull(3, 12);
                } else {
                    ps3.setString(3, modId);
                }
                ps3.setLong(4, newTypeId);
                if (correctModId == null || "".equals(correctModId)) {
                    ps3.setNull(5, 12);
                } else {
                    ps3.setString(5, correctModId);
                }
                ps3.setLong(6, rs1.getLong(1) + 1);
                ps3.setLong(7, 0L);
                ps3.executeUpdate();
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        boolean applyFix = false;
        boolean configured = false;
        ToolLogger log = new ToolLogger(argv);
        for (String str : argv) {
            if ("--apply-fix".equals(str)) {
                applyFix = true;
                configured = true;
            }
        }
        if (configured) {
            log.outMessage("Initializing....");
            ExternalCP.initCP();
            log.outOK();
            MailPlansRecovery mpr = new MailPlansRecovery(log, null, applyFix, false);
            mpr.process();
            return;
        }
        printHelp();
    }

    public static void printHelp() {
        System.out.println("H-Sphere E-mail only plans fix.");
        System.out.println("Usage java psoft.hsphere.tools.MailPlansRecovery --apply-fix");
        System.out.println("   The utility fixes doubling of init resources in e-mail only plans.");
    }
}
