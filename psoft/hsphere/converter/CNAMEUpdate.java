package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Plan;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.UnknownResellerException;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/converter/CNAMEUpdate.class */
public class CNAMEUpdate extends C0004CP {
    public CNAMEUpdate() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            CNAMEUpdate test = new CNAMEUpdate();
            try {
                TypeRegistry.getTypeId("cname_record");
            } catch (NoSuchTypeException e) {
                test.fixType();
            }
            test.fixPlans();
            Plan.loadAllPlans();
            test.m35go();
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
            ps = con.prepareStatement("INSERT INTO type_name(id, name, description) VALUES(3006, 'cname_record', 'DNS CNAME Record')");
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
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps4 = null;
        PreparedStatement ps5 = null;
        PreparedStatement ps6 = null;
        try {
            Session.getLog().info("Starting :");
            ps = con.prepareStatement("SELECT DISTINCT(plan_id) FROM plan_iresource WHERE type_id = 1000 AND plan_id NOT IN (SELECT plan_id FROM plan_iresource WHERE new_type_id = 3006)");
            ResultSet rs = ps.executeQuery();
            ps1 = con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, 3006, 'psoft.hsphere.resource.dns.CNAMERecord',0,1)");
            ps2 = con.prepareStatement("INSERT INTO plan_imod( plan_id, type_id, mod_id, disabled) VALUES( ?, 3006 , 'mail', 0)");
            ps3 = con.prepareStatement("INSERT INTO plan_ivalue(plan_id, type_id, type, mod_id, value, label, order_id, disabled) VALUES( ?, 3006, 0,'mail', 'mail','',0,0)");
            ps4 = con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value,label, order_id, disabled) VALUES( ?, 3006, 2, 'mail', 'mail_server_name', '', 1, 0)");
            ps5 = con.prepareStatement("INSERT INTO plan_iresource (plan_id, type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES( ?, 1000, 'signup', 3006, 'mail', 2, 0)");
            ps6 = con.prepareStatement("INSERT INTO plan_iresource (plan_id,  type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES(?, 1000, '', 3006, 'mail', 2, 0)");
            while (rs.next()) {
                System.out.println("Plan id:" + rs.getString(1) + " fixed");
                ps1.setLong(1, rs.getLong(1));
                ps1.executeUpdate();
                ps2.setLong(1, rs.getLong(1));
                ps2.executeUpdate();
                ps3.setLong(1, rs.getLong(1));
                ps3.executeUpdate();
                ps4.setLong(1, rs.getLong(1));
                ps4.executeUpdate();
                ps5.setLong(1, rs.getLong(1));
                ps5.executeUpdate();
                ps6.setLong(1, rs.getLong(1));
                ps6.executeUpdate();
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps3);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            con.close();
            throw th;
        }
    }

    /* renamed from: go */
    public void m35go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Session.getLog().info("Starting :");
            ps = con.prepareStatement("SELECT username FROM users");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                User u = User.getUser(rs.getString("username"));
                Session.getLog().debug("SetAmount: The user is " + u.getLogin());
                System.out.println("Current user: " + u.getLogin());
                try {
                    Session.setUser(u);
                    Iterator i = u.getAccountIds().iterator();
                    while (i.hasNext()) {
                        try {
                            Session.setAccount(u.getAccount((ResourceId) i.next()));
                            Session.getLog().info("SetAmount: Using Account " + Session.getAccount().getId().getId());
                            proceedResources(Session.getAccount());
                        } catch (Exception e) {
                            System.err.println("Some problem during update:" + Session.getAccount().getId());
                            Session.getLog().info("Some problem during update:" + Session.getAccount().getId());
                        }
                    }
                } catch (UnknownResellerException e2) {
                    Session.getLog().warn("Live client of removed reseller", e2);
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

    protected void proceedResources(Account a) throws Exception {
        for (ResourceId rId : a.getId().findAllChildren("mail_service")) {
            try {
                Resource r = rId.get();
                ResourceId cnameId = r.FM_getChild("cname_record");
                if (cnameId != null) {
                    System.out.println("CNAME with empty pref deleted");
                    cnameId.get().delete(false);
                }
                System.out.println("cname not found: " + r.get("mail_server_name"));
                List arg = Arrays.asList("mail", r.get("mail_server_name").toString());
                r.addChild("cname_record", "mail", arg);
            } catch (Exception e) {
                Session.getLog().info("Some problem during get:" + a.getId(), e);
            }
        }
    }
}
