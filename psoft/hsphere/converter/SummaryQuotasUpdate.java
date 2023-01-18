package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/SummaryQuotasUpdate.class */
public class SummaryQuotasUpdate extends C0004CP {
    long mt_id;

    public SummaryQuotasUpdate() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        if (argv.length != 1) {
            printHelpMessage();
        }
        try {
            SummaryQuotasUpdate test = new SummaryQuotasUpdate();
            if ("-h".equals(argv[0]) || "--help".equals(argv[0])) {
                printHelpMessage();
            } else if ("--enable".equals(argv[0])) {
                test.fixPlans();
                test.fixResellerPlans();
                Plan.loadAllPlans();
                test.fixAccounts();
            } else if ("--disable".equals(argv[0])) {
                test.removeDiskUsage();
            } else {
                printHelpMessage();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    public void fixPlans() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement psQuota = null;
        PreparedStatement psSetQuota = null;
        PreparedStatement psImod = null;
        PreparedStatement psSetImod = null;
        PreparedStatement psIRes = null;
        PreparedStatement psSetIRes = null;
        PreparedStatement psUpdateQuota = null;
        PreparedStatement psUpdateIRes = null;
        Session.setResellerId(1L);
        Connection con = Session.getTransConnection();
        try {
            ps = con.prepareStatement("SELECT plan_id, reseller_id FROM plans p, plan_value v WHERE p.id = v.plan_id AND name = '_CREATED_BY_' AND value NOT IN ('6','7')");
            psQuota = con.prepareStatement("SELECT type_id FROM plan_resource WHERE plan_id=? AND type_id=4003");
            psSetQuota = con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, 4003, 'psoft.hsphere.resource.SummaryQuota',0,1)");
            psUpdateQuota = con.prepareStatement("UPDATE plan_resource SET disabled = 0 WHERE plan_id=? AND type_id = 4003");
            psImod = con.prepareStatement("SELECT plan_id FROM plan_imod WHERE plan_id=? AND type_id= 4003");
            psSetImod = con.prepareStatement("INSERT INTO plan_imod( plan_id, type_id, mod_id, disabled) VALUES( ?, 4003 , NULL, 0)");
            psIRes = con.prepareStatement("SELECT plan_id FROM plan_iresource WHERE plan_id=? AND new_type_id= 4003");
            psSetIRes = con.prepareStatement("INSERT INTO plan_iresource (plan_id,  type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (?, 0, '', 4003, '', 4, 0)");
            psUpdateIRes = con.prepareStatement("UPDATE plan_iresource SET disabled = 0 WHERE plan_id=? AND new_type_id = 4003");
            PreparedStatement psValue = con.prepareStatement("SELECT plan_id FROM plan_ivalue WHERE plan_id=? AND type_id=4003 AND order_id = ?");
            PreparedStatement psSetValue = con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value,label, order_id, disabled) VALUES( ?, 4003, 0, NULL, ?, '', ?, 0)");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Session.setResellerId(rs.getLong(2));
                } catch (Exception e) {
                    System.err.println("Unable to set reseller ID:" + rs.getString(2));
                }
                try {
                    Session.getLog().info("Plan id:" + rs.getString(1));
                    System.out.println("Plan id:" + rs.getString(1));
                    Plan plan = Plan.getPlan(rs.getString(1));
                    psQuota.setInt(1, rs.getInt(1));
                    ResultSet rsQuota = psQuota.executeQuery();
                    if (!rsQuota.next()) {
                        psSetQuota.setInt(1, rs.getInt(1));
                        psSetQuota.executeUpdate();
                    } else {
                        psUpdateQuota.setInt(1, rs.getInt(1));
                        psUpdateQuota.executeUpdate();
                    }
                    psImod.setInt(1, rs.getInt(1));
                    ResultSet rsImod = psImod.executeQuery();
                    if (!rsImod.next()) {
                        psSetImod.setInt(1, rs.getInt(1));
                        psSetImod.executeUpdate();
                    }
                    psIRes.setInt(1, rs.getInt(1));
                    ResultSet rsIRes = psIRes.executeQuery();
                    if (!rsIRes.next()) {
                        psSetIRes.setInt(1, rs.getInt(1));
                        psSetIRes.executeUpdate();
                    } else {
                        psUpdateIRes.setLong(1, rsIRes.getLong("plan_id"));
                        psUpdateIRes.executeUpdate();
                    }
                    psValue.setInt(1, rs.getInt(1));
                    psValue.setInt(2, 0);
                    ResultSet rsValue = psValue.executeQuery();
                    if (!rsValue.next()) {
                        psSetValue.setInt(1, rs.getInt(1));
                        psSetValue.setInt(3, 0);
                        if (plan.getResourceType(4001).getValue("_FREE_UNITS_") != null) {
                            psSetValue.setString(2, plan.getResourceType(4001).getValue("_FREE_UNITS_"));
                        } else {
                            psSetValue.setString(2, "0");
                        }
                        psSetValue.executeUpdate();
                    }
                    Session.setResellerId(1L);
                } catch (Exception ex) {
                    System.err.println("Skiped plan ID:" + rs.getString(1));
                    ex.printStackTrace();
                    Session.setResellerId(1L);
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(psQuota);
            Session.closeStatement(psSetQuota);
            Session.closeStatement(psImod);
            Session.closeStatement(psSetImod);
            Session.closeStatement(psIRes);
            Session.closeStatement(psSetIRes);
            Session.closeStatement(psUpdateIRes);
            Session.closeStatement(psUpdateQuota);
            Session.commitTransConnection(con);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(psQuota);
            Session.closeStatement(psSetQuota);
            Session.closeStatement(psImod);
            Session.closeStatement(psSetImod);
            Session.closeStatement(psIRes);
            Session.closeStatement(psSetIRes);
            Session.closeStatement(psUpdateIRes);
            Session.closeStatement(psUpdateQuota);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public void fixResellerPlans() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement psQuota = null;
        PreparedStatement psSetQuota = null;
        PreparedStatement psImod = null;
        PreparedStatement psSetImod = null;
        PreparedStatement psIRes = null;
        PreparedStatement psSetIRes = null;
        PreparedStatement psUpdateQuota = null;
        PreparedStatement psUpdateIRes = null;
        Session.setResellerId(1L);
        Connection con = Session.getTransConnection();
        try {
            ps = con.prepareStatement("SELECT plan_id FROM plan_value WHERE name = '_CREATED_BY_' AND value = '6'");
            psQuota = con.prepareStatement("SELECT type_id, disabled FROM plan_resource WHERE plan_id=? AND type_id=4003");
            psSetQuota = con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, 4003, 'psoft.hsphere.resource.ResellerQuota',0,1)");
            psUpdateQuota = con.prepareStatement("UPDATE plan_resource SET disabled = 0 WHERE plan_id=? AND type_id = 4003");
            psImod = con.prepareStatement("SELECT plan_id FROM plan_imod WHERE plan_id=? AND type_id= 4003");
            psSetImod = con.prepareStatement("INSERT INTO plan_imod( plan_id, type_id, mod_id, disabled) VALUES( ?, 4003 , NULL, 0)");
            psIRes = con.prepareStatement("SELECT plan_id FROM plan_iresource WHERE plan_id=? AND new_type_id= 4003");
            psSetIRes = con.prepareStatement("INSERT INTO plan_iresource (plan_id,  type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (?, 0, '', 4003, '', 4, 0)");
            psUpdateIRes = con.prepareStatement("UPDATE plan_iresource SET disabled = 0 WHERE plan_id=? AND new_type_id = 4003");
            PreparedStatement psValue = con.prepareStatement("SELECT plan_id FROM plan_ivalue WHERE plan_id=? AND type_id=4003 AND order_id = ?");
            PreparedStatement psSetValue = con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value,label, order_id, disabled) VALUES( ?, 4003, 0, NULL, ?, '', ?, 0)");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Session.getLog().info("Plan id:" + rs.getString(1));
                    System.out.println("Plan id:" + rs.getString(1));
                    Plan plan = Plan.getPlan(rs.getString(1));
                    psQuota.setInt(1, rs.getInt(1));
                    ResultSet rsQuota = psQuota.executeQuery();
                    if (!rsQuota.next()) {
                        psSetQuota.setInt(1, rs.getInt(1));
                        psSetQuota.executeUpdate();
                    } else {
                        psUpdateQuota.setInt(1, rs.getInt(1));
                        psUpdateQuota.executeUpdate();
                    }
                    psImod.setInt(1, rs.getInt(1));
                    ResultSet rsImod = psImod.executeQuery();
                    if (!rsImod.next()) {
                        psSetImod.setInt(1, rs.getInt(1));
                        psSetImod.executeUpdate();
                    }
                    psIRes.setInt(1, rs.getInt(1));
                    ResultSet rsIRes = psIRes.executeQuery();
                    if (!rsIRes.next()) {
                        psSetIRes.setInt(1, rs.getInt(1));
                        psSetIRes.executeUpdate();
                    } else {
                        psUpdateIRes.setLong(1, rsIRes.getLong("plan_id"));
                        psUpdateIRes.executeUpdate();
                    }
                    psValue.setInt(1, rs.getInt(1));
                    psValue.setInt(2, 0);
                    ResultSet rsValue = psValue.executeQuery();
                    if (!rsValue.next()) {
                        psSetValue.setInt(1, rs.getInt(1));
                        psSetValue.setInt(3, 0);
                        if (plan.getResourceType(3010).getValue("_FREE_UNITS_quota_") != null) {
                            psSetValue.setString(2, plan.getResourceType(3010).getValue("_FREE_UNITS_quota_"));
                        } else {
                            psSetValue.setString(2, "0");
                        }
                        psSetValue.executeUpdate();
                    }
                } catch (Exception ex) {
                    System.err.println("Skiped plan ID:" + rs.getString(1));
                    ex.printStackTrace();
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(psQuota);
            Session.closeStatement(psSetQuota);
            Session.closeStatement(psImod);
            Session.closeStatement(psSetImod);
            Session.closeStatement(psIRes);
            Session.closeStatement(psSetIRes);
            Session.closeStatement(psUpdateIRes);
            Session.closeStatement(psUpdateQuota);
            Session.commitTransConnection(con);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(psQuota);
            Session.closeStatement(psSetQuota);
            Session.closeStatement(psImod);
            Session.closeStatement(psSetImod);
            Session.closeStatement(psIRes);
            Session.closeStatement(psSetIRes);
            Session.closeStatement(psUpdateIRes);
            Session.closeStatement(psUpdateQuota);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public void fixResellers() throws Exception {
        for (Reseller resel : Reseller.getResellerList()) {
            try {
                try {
                    User u = User.getUser(resel.getId());
                    Session.setUser(u);
                    Iterator i = u.getAccountIds().iterator();
                    while (i.hasNext()) {
                        Account a = u.getAccount((ResourceId) i.next());
                        ResourceId resRsourceId = a.FM_getChild(FMACLManager.RESELLER);
                        if (resRsourceId != null) {
                            if (a.FM_getChild("summary_quota") == null) {
                                double credit = a.getBill().getCustomCredit();
                                try {
                                    try {
                                        Session.setUser(u);
                                        Session.setAccount(a);
                                        a.getBill().setCredit(100000.0d);
                                        a.addChild("summary_quota", "", null);
                                        a.getBill().setCredit(credit);
                                    } catch (Exception ex) {
                                        Session.getLog().error("Errors processing Reseller ID :" + resel.getId(), ex);
                                        System.err.println("Reseller " + resel.getId() + " process error");
                                        a.getBill().setCredit(credit);
                                    }
                                } catch (Throwable th) {
                                    a.getBill().setCredit(credit);
                                    throw th;
                                    break;
                                }
                            } else {
                                System.err.println("Reseller " + resel.getId() + " skiped");
                            }
                        }
                    }
                    Session.setUser(null);
                    Session.setAccount(null);
                } catch (Exception e) {
                    Session.getLog().error("Error calc Reseller#:" + resel.getId());
                    Session.setUser(null);
                    Session.setAccount(null);
                }
            } catch (Throwable th2) {
                Session.setUser(null);
                Session.setAccount(null);
                throw th2;
            }
        }
    }

    public void fixAccounts() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM accounts WHERE deleted IS NULL");
            ResultSet rs = ps.executeQuery();
            System.out.println("Done");
            while (rs.next()) {
                try {
                    Account acc = (Account) Account.get(new ResourceId(rs.getString(1) + "_0"));
                    Session.setUser(acc.getUser());
                    Session.setAccount(acc);
                    if (acc.getPlan().areResourcesAvailable("summary_quota") && acc.FM_getChild("summary_quota") == null) {
                        double credit = acc.getBill().getCustomCredit();
                        try {
                            try {
                                System.out.print("Adding New Summary quota for account:" + rs.getString(1) + " ... ");
                                acc.getBill().setCredit(100000.0d);
                                acc.addChild("summary_quota", "", null);
                                System.out.println("Done");
                                acc.getBill().setCredit(credit);
                            } catch (Throwable th) {
                                acc.getBill().setCredit(credit);
                                throw th;
                                break;
                            }
                        } catch (Exception e) {
                            System.out.println("Failed");
                            Session.getLog().error("Errors adding Summary Quota", e);
                            acc.getBill().setCredit(credit);
                        }
                    }
                } catch (Exception ex) {
                    System.err.println("Error adding summary quota:");
                    ex.printStackTrace();
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            con.close();
            throw th2;
        }
    }

    public void removeDiskUsage() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        Connection con = Session.getTransConnection();
        try {
            try {
                ps = con.prepareStatement("UPDATE plan_iresource SET disabled = ? WHERE new_type_id = ?");
                ps.setInt(1, 1);
                ps.setLong(2, 4003L);
                ps1 = con.prepareStatement("DELETE FROM parent_child WHERE child_type = ?");
                ps1.setLong(1, 4003L);
                ps2 = con.prepareStatement("UPDATE plan_resource SET disabled =? WHERE type_id=?");
                ps2.setInt(1, 1);
                ps2.setLong(2, 4003L);
                ps.executeUpdate();
                ps1.executeUpdate();
                ps2.executeUpdate();
                con.commit();
                System.out.println("\nDatabase has been updated\nDisk usage mechanism has been disabled\nWARNING:\n\tPlease, do not forget to restart control panel\n\tin order the changes take effect");
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.commitTransConnection(con);
            } catch (Exception ex) {
                con.rollback();
                System.out.println("An error has occured. Disk usage mechanism has NOT been disabled.");
                ex.printStackTrace();
                Session.closeStatement(ps);
                Session.closeStatement(ps1);
                Session.closeStatement(ps2);
                Session.commitTransConnection(con);
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.commitTransConnection(con);
            throw th;
        }
    }

    public static void printHelpMessage() {
        System.out.println("NAME:\n\tpsoft.hsphere.converter.SummaryQuotasUpdate -\n\tH-Sphere disk usage billing mechanism switcher.");
        System.out.println("SYNOPSIS:\n\tpsoft.hsphere.converter.SummaryQuotasUpdate --enable|--disable");
        System.out.println("OPTIONS:");
        System.out.println("\t-h|--help \t\t- shows this screen");
        System.out.println("\t--enable\t- enables disk usage billing mechanism");
        System.out.println("\t--disable \t- disables usage mechanism. Allocation mechanism will be used instead");
        System.exit(0);
    }
}
