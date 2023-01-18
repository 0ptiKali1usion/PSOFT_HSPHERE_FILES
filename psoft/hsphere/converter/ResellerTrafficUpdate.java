package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Plan;
import psoft.hsphere.Reseller;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.User;
import psoft.hsphere.plan.ResourceType;
import psoft.util.freemarker.acl.FMACLManager;

/* loaded from: hsphere.zip:psoft/hsphere/converter/ResellerTrafficUpdate.class */
public class ResellerTrafficUpdate extends C0004CP {
    long mt_id;

    public ResellerTrafficUpdate() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            ResellerTrafficUpdate test = new ResellerTrafficUpdate();
            try {
                TypeRegistry.getTypeId("reseller_traffic");
            } catch (NoSuchTypeException e) {
                test.fixType();
            }
            test.fixPlans();
            Plan.loadAllPlans();
            test.fixResellers();
        } catch (Exception e2) {
            e2.printStackTrace();
            System.exit(1);
        }
        System.out.println("Migration finished");
        System.exit(0);
    }

    protected void fixType() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO type_name(id,name,price,description,rprice, required, priority)VALUES(3011,'reseller_traffic','MRU','Reseller traffic','',1,0)");
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            TypeRegistry.reload();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void fixPlans() throws Exception {
        PreparedStatement ps = null;
        PreparedStatement psTraff = null;
        PreparedStatement psSetTraff = null;
        PreparedStatement psImod = null;
        PreparedStatement psSetImod = null;
        PreparedStatement psIRes = null;
        PreparedStatement psSetIRes = null;
        Session.setResellerId(1L);
        Connection con = Session.getTransConnection();
        try {
            ps = con.prepareStatement("SELECT plan_id FROM plan_resource WHERE type_id = 3010");
            psTraff = con.prepareStatement("SELECT type_id FROM plan_resource WHERE plan_id=? AND type_id=3011");
            psSetTraff = con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, 3011, 'psoft.hsphere.resource.Traffic',0,1)");
            psImod = con.prepareStatement("SELECT plan_id FROM plan_imod WHERE plan_id=? AND type_id= 3011");
            psSetImod = con.prepareStatement("INSERT INTO plan_imod( plan_id, type_id, mod_id, disabled) VALUES( ?, 3011 , NULL, 0)");
            psIRes = con.prepareStatement("SELECT plan_id FROM plan_iresource WHERE plan_id=? AND new_type_id= 3011");
            psSetIRes = con.prepareStatement("INSERT INTO plan_iresource (plan_id,  type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES (?, 0, '', 3011, '', 3, 0)");
            PreparedStatement psValue = con.prepareStatement("SELECT plan_id FROM plan_ivalue WHERE plan_id=? AND type_id=3011 AND order_id = ?");
            PreparedStatement psSetValue = con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value,label, order_id, disabled) VALUES( ?, 3011, 0, NULL, ?, '', ?, 0)");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    Session.getLog().info("Plan id:" + rs.getString(1));
                    System.out.println("Plan id:" + rs.getString(1));
                    Plan plan = Plan.getPlan(rs.getString(1));
                    psTraff.setInt(1, rs.getInt(1));
                    ResultSet rsTraff = psTraff.executeQuery();
                    if (!rsTraff.next()) {
                        psSetTraff.setInt(1, rs.getInt(1));
                        psSetTraff.executeUpdate();
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
                    }
                    psValue.setInt(1, rs.getInt(1));
                    psValue.setInt(2, 1);
                    ResultSet rsValue = psValue.executeQuery();
                    if (!rsValue.next()) {
                        psSetValue.setInt(1, rs.getInt(1));
                        psSetValue.setString(2, "-1");
                        psSetValue.setInt(3, 1);
                        psSetValue.executeUpdate();
                    }
                    psValue.setInt(1, rs.getInt(1));
                    psValue.setInt(2, 0);
                    ResultSet rsValue2 = psValue.executeQuery();
                    if (!rsValue2.next()) {
                        psSetValue.setInt(1, rs.getInt(1));
                        psSetValue.setInt(3, 0);
                        if (plan.getResourceType(3010).getValue("_MAX_traffic_") != null) {
                            psSetValue.setString(2, plan.getResourceType(3010).getValue("_MAX_traffic_"));
                        } else {
                            psSetValue.setString(2, "0");
                        }
                        psSetValue.executeUpdate();
                    }
                    ResourceType rt = new ResourceType(rs.getInt(1), 3011, "psoft.hsphere.resource.Traffic", 0, 1);
                    if (plan.getResourceType(3010).getValue("_REFUND_CALC_traffic_") != null) {
                        rt.FM_putValue("_REFUND_CALC_", "psoft.hsphere.calc.StandardRefundCalc");
                        plan.getResourceType(3010).FM_putValue("_REFUND_CALC_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_REFUND_PRICE_traffic_") != null) {
                        rt.FM_putValue("_REFUND_PRICE_", plan.getResourceType(3010).getValue("_REFUND_PRICE_traffic_"));
                        plan.getResourceType(3010).FM_putValue("_REFUND_PRICE_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_RECURRENT_CALC_traffic_") != null) {
                        rt.FM_putValue("_RECURRENT_CALC_", "psoft.hsphere.calc.StandardCalc");
                        plan.getResourceType(3010).FM_putValue("_RECURRENT_CALC_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_UNIT_PRICE_traffic_") != null) {
                        rt.FM_putValue("_UNIT_PRICE_", plan.getResourceType(3010).getValue("_UNIT_PRICE_traffic_"));
                        plan.getResourceType(3010).FM_putValue("_UNIT_PRICE_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_USAGE_CALC_traffic_") != null) {
                        rt.FM_putValue("_USAGE_CALC_", "psoft.hsphere.calc.StandardUsageCalc");
                        plan.getResourceType(3010).FM_putValue("_USAGE_PRICE_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_USAGE_PRICE_traffic_") != null) {
                        rt.FM_putValue("_USAGE_PRICE_", plan.getResourceType(3010).getValue("_USAGE_PRICE_traffic_"));
                        plan.getResourceType(3010).FM_putValue("_USAGE_PRICE_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_MAX_traffic_") != null) {
                        rt.FM_putValue("_MAX_", plan.getResourceType(3010).getValue("_MAX_traffic_"));
                        plan.getResourceType(3010).FM_putValue("_MAX_traffic_", "");
                    }
                    if (plan.getResourceType(3010).getValue("_FREE_UNITS_traffic_") != null) {
                        rt.FM_putValue("_FREE_UNITS_", plan.getResourceType(3010).getValue("_FREE_UNITS_traffic_"));
                        plan.getResourceType(3010).FM_putValue("_FREE_UNITS_traffic_", "");
                    }
                } catch (Exception ex) {
                    System.err.println("Skiped plan ID:" + rs.getString(1));
                    ex.printStackTrace();
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(psTraff);
            Session.closeStatement(psSetTraff);
            Session.closeStatement(psImod);
            Session.closeStatement(psSetImod);
            Session.closeStatement(psIRes);
            Session.closeStatement(psSetIRes);
            Session.commitTransConnection(con);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(psTraff);
            Session.closeStatement(psSetTraff);
            Session.closeStatement(psImod);
            Session.closeStatement(psSetImod);
            Session.closeStatement(psIRes);
            Session.closeStatement(psSetIRes);
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
                            if (a.FM_getChild("reseller_traffic") == null) {
                                double credit = a.getBill().getCustomCredit();
                                try {
                                    try {
                                        fixReseller(resel.getId(), a);
                                        Session.setUser(u);
                                        Session.setAccount(a);
                                        a.getBill().setCredit(100000.0d);
                                        a.addChild("reseller_traffic", "", null);
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
                                System.err.println("Reseller " + resel.getId() + " reseted");
                                fixReseller(resel.getId(), a);
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

    public void fixReseller(long resellerId, Account a) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id FROM accounts WHERE deleted IS NULL AND reseller_id = ?");
            ps.setLong(1, resellerId);
            ResultSet rs = ps.executeQuery();
            System.out.println("Done");
            Account acc = null;
            double credit = 0.0d;
            while (rs.next()) {
                try {
                    acc = (Account) Account.get(new ResourceId(rs.getString(1) + "_0"));
                    Session.setUser(acc.getUser());
                    Session.setAccount(acc);
                    System.out.println("Working on user " + Session.getUser().getLogin() + " acc# " + rs.getString(1));
                    credit = acc.getBill().getCustomCredit();
                    if (acc.FM_getChild("traffic") == null) {
                        acc.getBill().setCredit(credit);
                    } else {
                        Resource oldTraff = acc.FM_getChild("traffic").get();
                        acc.getBill().setCredit(100000.0d);
                        acc.addChild("traffic", "", null);
                        System.out.println("New traffic add ...  Done");
                        oldTraff.delete(false);
                        System.out.println("Deleting old ... Done");
                        acc.getBill().setCredit(credit);
                    }
                } catch (Exception e) {
                    Session.getLog().error("Errors changing Traffic", e);
                    System.out.println(" Errors. See log for details");
                    acc.getBill().setCredit(credit);
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
