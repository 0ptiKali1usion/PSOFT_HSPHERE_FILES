package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.log4j.Category;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;

/* loaded from: hsphere.zip:psoft/hsphere/tools/BillPeriodFixer.class */
public class BillPeriodFixer {
    private static final Category log = Category.getInstance(BillPeriodFixer.class.getName());

    public BillPeriodFixer() throws Exception {
        ExternalCP.initCP();
    }

    public static void main(String[] args) throws Exception {
        String plan_id = "";
        StringBuffer keys = new StringBuffer("");
        int i = 0;
        while (i < args.length) {
            if ("--planid".equals(args[i]) && args.length > i + 1) {
                plan_id = args[i + 1];
                i++;
            }
            keys.append(args[i]);
            i++;
        }
        if (keys.toString().equals("") || plan_id.equals("")) {
            keys.append("--help");
        }
        if (keys.toString().indexOf("--help") != -1) {
            System.out.println("NAME:\n\t psoft.hsphere.tools.BillPeriodFixer - H-Sphere Billing Period Fixing Tool");
            System.out.println("SYNOPSIS:\n\t psoft.hsphere.tools.BillPeriodFixer [options]");
            System.out.println("OPTIONS:");
            System.out.println("\t--help \t\t- Shows this screen");
            System.out.println("\t--planid id\t- Specifies the plan id to fix");
            System.exit(0);
        }
        System.out.println("Running Billing Period Fixer on Plan ID: " + plan_id);
        new BillPeriodFixer();
        process(plan_id);
        System.exit(0);
    }

    public static void process(String planid) throws Exception {
        Connection con = Session.getDb();
        try {
            PreparedStatement ps = con.prepareStatement("SELECT id FROM plans WHERE id = ? AND deleted != 1 AND billing = 1");
            ps.setString(1, planid);
            if (ps.executeQuery().next()) {
                System.out.println("Plan ID is valid");
                ps = con.prepareStatement("SELECT users.username, user_account.account_id FROM users LEFT OUTER JOIN user_account ON users.id = user_account.user_id LEFT OUTER JOIN accounts ON user_account.account_id = accounts.id WHERE accounts.plan_id = ? AND deleted IS NULL");
                ps.setString(1, planid);
                ResultSet rs = ps.executeQuery();
                System.out.println("Processing Accounts:");
                while (rs.next()) {
                    String username = rs.getString(1);
                    long aid = rs.getLong(2);
                    System.out.println("\t User Account: " + username + " #" + aid);
                    PreparedStatement ps2 = con.prepareStatement("SELECT opened, id FROM bill WHERE account_id = ? and closed IS NULL");
                    ps2.setLong(1, aid);
                    ResultSet rs2 = ps2.executeQuery();
                    Date billopened = null;
                    if (rs2.next()) {
                        billopened = rs2.getDate(1);
                        rs2.getLong(2);
                    }
                    PreparedStatement ps22 = con.prepareStatement("SELECT opened, id FROM bill WHERE account_id = ? AND closed IS NULL");
                    ps22.setLong(1, aid);
                    ResultSet rs22 = ps22.executeQuery();
                    int x = 0;
                    String extsql = "";
                    while (rs22.next()) {
                        if (x != 0) {
                            extsql = extsql + " or ";
                        }
                        extsql = extsql + "bill_id = " + rs22.getLong(2);
                        x++;
                    }
                    if (!extsql.equals("")) {
                        extsql = "and (" + extsql + ")";
                    }
                    if (!extsql.equals("")) {
                        System.out.println("\t\t Billing Period Started: " + billopened);
                        PreparedStatement ps23 = con.prepareStatement("SELECT parent_child.parent_id,  parent_child.parent_type,  parent_child.child_id, parent_child.child_type, parent_child.p_begin, type_name.description FROM parent_child LEFT OUTER JOIN type_name ON parent_child.child_type=type_name.id WHERE parent_child.account_id = ? AND parent_child.p_begin!= ?");
                        ps23.setLong(1, aid);
                        ps23.setDate(2, billopened);
                        ResultSet rs23 = ps23.executeQuery();
                        while (rs23.next()) {
                            int rtype = rs23.getInt(4);
                            Date ropened = rs23.getDate(5);
                            String rname = rs23.getString(6);
                            System.out.println("\t\t Checking Resource: " + rname);
                            PreparedStatement ps3 = con.prepareStatement("SELECT * FROM bill_entry WHERE rtype = " + rtype + " AND created LIKE '%" + ropened + "%' AND type = 2 " + extsql);
                            ResultSet rs3 = ps3.executeQuery();
                            if (rs3.next()) {
                                System.out.println("\t\t Resource " + rname + ": Skipped");
                            } else if (!TypeRegistry.isMonthly(rtype) && rtype != 14) {
                                PreparedStatement ps32 = con.prepareStatement("UPDATE parent_child SET p_begin='" + billopened + "' WHERE parent_id = " + rs23.getLong(1) + " AND parent_type = " + rs23.getLong(2) + " AND child_id = " + rs23.getLong(3) + " AND child_type= " + rs23.getLong(4) + " AND account_id= " + aid);
                                ps32.executeUpdate();
                                System.out.println("\t\t Resource " + rname + " was fixed");
                            } else {
                                System.out.println("\t\t Resource " + rname + ": Skipped");
                            }
                        }
                    }
                }
            } else {
                System.out.println("Billing Period Fixer Failed: no such plan, plan is deleted, or plan has no billing");
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(null);
            con.close();
            throw th;
        }
    }
}
