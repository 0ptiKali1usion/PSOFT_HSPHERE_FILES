package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.hsphere.resource.apache.VirtualHostingResource;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MIVAEmpresaFix.class */
public class MIVAEmpresaFix extends C0004CP {
    private final String WINR = "psoft.hsphere.resource.IIS.MIVAEmpresaOnlyResource";
    private final String UNIXR = "psoft.hsphere.resource.apache.MIVAEmpresaOnlyResource";

    public MIVAEmpresaFix() throws Exception {
        super("psoft_config.hsphere");
        this.WINR = "psoft.hsphere.resource.IIS.MIVAEmpresaOnlyResource";
        this.UNIXR = "psoft.hsphere.resource.apache.MIVAEmpresaOnlyResource";
    }

    /* renamed from: go */
    public void m14go() throws Exception {
        try {
            fixPlans();
            addEmpresaResource();
        } catch (Exception ex) {
            Session.getLog().error("Failed to fix plans", ex);
        }
    }

    private void fixPlans() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps2 = null;
        try {
            try {
                PreparedStatement ps1 = con.prepareStatement("INSERT INTO plan_resource (plan_id, type_id, class_name, disabled, showable) VALUES (?, 6006, ?, 0, 1)");
                PreparedStatement ps3 = con.prepareStatement("INSERT INTO plan_iresource(plan_id,type_id,mod_id,new_type_id,new_mod_id,order_id,disabled) VALUES(?, 9, ?,6006,'',?,0)");
                PreparedStatement ps = con.prepareStatement("SELECT a.id, a.reseller_id, c.value  FROM plans a, plan_resource b, plan_value c WHERE a.id = b.plan_id AND a.id=c.plan_id AND c.name=? AND c.value in ('1', '2') AND b.type_id=? AND a.deleted <> ?  AND a.id NOT IN (SELECT DISTINCT plan_id FROM plan_resource WHERE type_id=6006)");
                ps.setString(1, "_CREATED_BY_");
                ps.setInt(2, 6004);
                ps.setInt(3, 1);
                ResultSet rs = ps.executeQuery();
                System.out.println("Working on plan_resource table");
                while (rs.next()) {
                    try {
                        System.out.print("Adding for plan " + rs.getInt(1));
                        ps1.setInt(1, rs.getInt(1));
                        ps1.setString(2, "1".equals(rs.getString(3)) ? "psoft.hsphere.resource.apache.MIVAEmpresaOnlyResource" : "psoft.hsphere.resource.IIS.MIVAEmpresaOnlyResource");
                        ps1.executeUpdate();
                        System.out.println(" [   OK   ]");
                    } catch (Exception ex) {
                        Session.getLog().error("Failed to update plan_resource", ex);
                        System.out.println(" [  FAIL  ]");
                    }
                }
                System.out.println("Working on plan_iresource table");
                System.out.print("Looking for plans having miva");
                ps.close();
                PreparedStatement ps4 = con.prepareStatement("SELECT plan_id, mod_id, order_id FROM plan_iresource WHERE type_id=? AND new_type_id=? ORDER BY plan_id, order_id");
                ps4.setLong(1, 9L);
                ps4.setLong(2, 6004L);
                ResultSet rs2 = ps4.executeQuery();
                System.out.println("   DONE");
                while (rs2.next()) {
                    try {
                        try {
                            System.out.println(" Working on plan " + rs2.getLong(1));
                            ps2 = con.prepareStatement("UPDATE plan_iresource SET order_id=order_id+1 WHERE plan_id=? AND type_id=9 AND order_id >= ? AND mod_id " + ("".equals(rs2.getString(2)) ? "IS NULL" : "= ?"));
                            ps2.setLong(1, rs2.getLong(1));
                            ps2.setLong(2, rs2.getLong(3));
                            if (!"".equals(rs2.getString(2))) {
                                ps2.setString(3, rs2.getString(2));
                            }
                            ps2.executeUpdate();
                            ps3.setLong(1, rs2.getLong(1));
                            ps3.setString(2, rs2.getString(2));
                            ps3.setLong(3, rs2.getLong(3));
                            ps3.executeUpdate();
                            ps2.close();
                        } catch (Exception ex2) {
                            Session.getLog().error("FAILED ", ex2);
                            ps2.close();
                        }
                    } catch (Throwable th) {
                        ps2.close();
                        throw th;
                    }
                }
                System.out.print("Reloading all plans ...");
                Plan.loadAllPlans();
                System.out.println("   DONE");
                Session.closeStatement(ps4);
                con.close();
            } catch (Exception ex3) {
                Session.getLog().error(ex3);
                Session.closeStatement(null);
                con.close();
            }
        } catch (Throwable th2) {
            Session.closeStatement(null);
            con.close();
            throw th2;
        }
    }

    private void addEmpresaResource() throws Exception {
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps1 = con.prepareStatement("DELETE FROM apache_mime WHERE id = ?");
            ps2 = con.prepareStatement("DELETE FROM iis_mime WHERE id = ?");
            System.out.print("Looking for domains which are lack MivaEmpresaEngine ...");
            ps = con.prepareStatement("SELECT b.user_id, a.account_id, a.parent_id, a.child_id  FROM parent_child a, user_account b WHERE a.child_type=? AND a.account_id=b.account_id AND a.account_id NOT IN (SELECT DISTINCT account_id FROM parent_child where child_type=?)");
            ps.setLong(1, 6004L);
            ps.setLong(2, 6006L);
            ResultSet rs = ps.executeQuery();
            System.out.println("Done");
            while (rs.next()) {
                User u = User.getUser(rs.getLong(1));
                System.out.print("Working on user " + u.getLogin() + " acc# " + rs.getString(2));
                Session.setUser(u);
                Account acc = u.getAccount(new ResourceId(rs.getString(2) + "_0"));
                Session.setAccount(acc);
                Resource hosting = new ResourceId(rs.getString(3) + "_9").get();
                double credit = acc.getBill().getCustomCredit();
                try {
                    if (hosting instanceof VirtualHostingResource) {
                        ps1.setLong(1, rs.getLong(4));
                        ps1.executeUpdate();
                    } else if (hosting instanceof psoft.hsphere.resource.IIS.VirtualHostingResource) {
                        ps2.setLong(1, rs.getLong(4));
                        ps2.executeUpdate();
                    }
                    acc.getBill().setCredit(100000.0d);
                    hosting.addChild("empresa", "", null);
                    System.out.println(" Done");
                    acc.getBill().setCredit(credit);
                } catch (Exception e) {
                    Session.getLog().error("Errors during empresa adding", e);
                    System.out.println(" Errors. See log for details");
                    e.printStackTrace();
                    acc.getBill().setCredit(credit);
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            con.close();
            throw th;
        }
    }

    public static void main(String[] argv) throws Exception {
        System.out.print("Initializing ...");
        MIVAEmpresaFix sa = new MIVAEmpresaFix();
        System.out.println(" Done");
        sa.m14go();
        System.exit(0);
    }
}
