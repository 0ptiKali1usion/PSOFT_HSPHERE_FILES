package psoft.hsphere.converter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import psoft.hsphere.C0004CP;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;

/* loaded from: hsphere.zip:psoft/hsphere/converter/MailtrafficUpdate.class */
public class MailtrafficUpdate extends C0004CP {
    long mt_id;

    public MailtrafficUpdate() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            MailtrafficUpdate test = new MailtrafficUpdate();
            try {
                TypeRegistry.getTypeId("mail_traffic");
            } catch (NoSuchTypeException e) {
                test.fixType();
            }
            test.fixPlans();
            test.fixDomains();
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
            ps = con.prepareStatement("INSERT INTO type_name(id, name, description) VALUES(122, 'mail_traffic', 'Mail traffic')");
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
        PreparedStatement ps01 = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps02 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps03 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps04 = null;
        PreparedStatement ps4 = null;
        PreparedStatement ps5 = null;
        PreparedStatement ps6 = null;
        try {
            Session.getLog().info("Starting :");
            ps = con.prepareStatement("SELECT DISTINCT(plan_id) FROM plan_iresource WHERE type_id IN (2,31,32,34,35,37)");
            ResultSet rs = ps.executeQuery();
            ps01 = con.prepareStatement("SELECT type_id FROM plan_resource WHERE plan_id=? AND type_id=122 AND class_name='psoft.hsphere.resource.Traffic'");
            ps1 = con.prepareStatement("INSERT INTO plan_resource(plan_id, type_id, class_name, disabled, showable) VALUES(?, 122, 'psoft.hsphere.resource.Traffic',0,1)");
            ps02 = con.prepareStatement("SELECT plan_id FROM plan_imod WHERE plan_id=? AND type_id=122");
            ps2 = con.prepareStatement("INSERT INTO plan_imod( plan_id, type_id, mod_id, disabled) VALUES( ?, 122 , NULL, 0)");
            ps03 = con.prepareStatement("SELECT plan_id FROM plan_ivalue WHERE plan_id=? AND type_id=122 AND value='1' AND order_id=0");
            ps3 = con.prepareStatement("INSERT INTO plan_ivalue(plan_id, type_id, type, mod_id, value, label, order_id, disabled) VALUES( ?, 122, 0, NULL, '1','',0,0)");
            ps04 = con.prepareStatement("SELECT plan_id FROM plan_ivalue WHERE plan_id=? AND type_id=122 AND value='5' AND order_id=1");
            ps4 = con.prepareStatement("INSERT INTO plan_ivalue (plan_id, type_id, type, mod_id, value,label, order_id, disabled) VALUES( ?, 122, 0, NULL, '5', '', 1, 0)");
            ps5 = con.prepareStatement("SELECT type_id,mod_id, max(order_id) FROM plan_iresource WHERE plan_id = ? AND type_id NOT IN (SELECT type_id FROM plan_iresource WHERE plan_id = ? AND new_type_id = 122) AND type_id in (2,31,32,34,35,37) GROUP BY type_id,mod_id ORDER BY type_id,mod_id");
            ps6 = con.prepareStatement("INSERT INTO plan_iresource (plan_id,  type_id, mod_id, new_type_id, new_mod_id, order_id, disabled) VALUES(?, ?, ?, 122, '', ?, 0)");
            while (rs.next()) {
                Session.getLog().info("Plan id:" + rs.getString(1));
                System.out.println("Plan id:" + rs.getString(1));
                ps01.setLong(1, rs.getLong(1));
                ResultSet rs01 = ps01.executeQuery();
                if (!rs01.next()) {
                    ps1.setLong(1, rs.getLong(1));
                    ps1.executeUpdate();
                    Session.getLog().info("plan_resource inserted");
                } else {
                    Session.getLog().info("plan_resource id=" + rs01.getLong(1) + " skiped");
                }
                ps02.setLong(1, rs.getLong(1));
                ResultSet rs02 = ps02.executeQuery();
                if (!rs02.next()) {
                    ps2.setLong(1, rs.getLong(1));
                    ps2.executeUpdate();
                    Session.getLog().info("plan_imod inserted");
                } else {
                    Session.getLog().info("plan_imod id=" + rs02.getLong(1) + " skiped");
                }
                ps03.setLong(1, rs.getLong(1));
                ResultSet rs03 = ps03.executeQuery();
                if (!rs03.next()) {
                    ps3.setLong(1, rs.getLong(1));
                    ps3.executeUpdate();
                    Session.getLog().info("plan_ivalue1 inserted");
                } else {
                    Session.getLog().info("plan_ivalue1 id=" + rs03.getLong(1) + " skiped");
                }
                ps04.setLong(1, rs.getLong(1));
                ResultSet rs04 = ps04.executeQuery();
                if (!rs04.next()) {
                    ps4.setLong(1, rs.getLong(1));
                    ps4.executeUpdate();
                    Session.getLog().info("plan_ivalue5 inserted");
                } else {
                    Session.getLog().info("plan_ivalue5 id=" + rs04.getLong(1) + " skiped");
                }
                ps5.setLong(1, rs.getLong(1));
                ps5.setLong(2, rs.getLong(1));
                ResultSet rs5 = ps5.executeQuery();
                while (rs5.next()) {
                    ps6.setLong(1, rs.getLong(1));
                    ps6.setLong(2, rs5.getLong(1));
                    ps6.setString(3, rs5.getString(2));
                    ps6.setInt(4, rs5.getInt(3) + 1);
                    ps6.executeUpdate();
                    Session.getLog().info("plan_iresource inserted");
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps01);
            Session.closeStatement(ps1);
            Session.closeStatement(ps02);
            Session.closeStatement(ps2);
            Session.closeStatement(ps03);
            Session.closeStatement(ps3);
            Session.closeStatement(ps04);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps01);
            Session.closeStatement(ps1);
            Session.closeStatement(ps02);
            Session.closeStatement(ps2);
            Session.closeStatement(ps03);
            Session.closeStatement(ps3);
            Session.closeStatement(ps04);
            Session.closeStatement(ps4);
            Session.closeStatement(ps5);
            Session.closeStatement(ps6);
            con.close();
            throw th;
        }
    }

    public void fixDomains() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        PreparedStatement ps2 = null;
        PreparedStatement ps03 = null;
        PreparedStatement ps3 = null;
        PreparedStatement ps04 = null;
        PreparedStatement ps4 = null;
        try {
            ps = con.prepareStatement("SELECT id,name FROM domains");
            ResultSet rs = ps.executeQuery();
            ps1 = con.prepareStatement("INSERT INTO traffics (id,tt_type,tt_size)VALUES (?,5,1)");
            ps03 = con.prepareStatement("SELECT child_id FROM parent_child WHERE parent_id=? AND child_type=122");
            ps2 = con.prepareStatement("SELECT child_type,account_id,p_begin,suspended FROM parent_child WHERE child_id = ?");
            ps3 = con.prepareStatement("INSERT INTO parent_child (parent_id,parent_type,child_id,child_type,account_id,p_begin,suspended) VALUES (?,?,?,122,?,?,?)");
            ps04 = con.prepareStatement("SELECT id FROM resource_amount WHERE id=?");
            ps4 = con.prepareStatement("INSERT INTO resource_amount (id,amount) VALUES (?,1)");
            while (rs.next()) {
                Session.getLog().info("Domain: " + rs.getString(2));
                System.out.println("Domain: " + rs.getString(2));
                ps03.setLong(1, rs.getLong(1));
                ResultSet rs03 = ps03.executeQuery();
                if (rs03.next()) {
                    this.mt_id = rs03.getLong(1);
                    Session.getLog().info("traffic id= " + rs03.getLong(1) + " parent_child&traffics skipped");
                } else {
                    this.mt_id = Session.getNewIdAsLong();
                    ps2.setLong(1, rs.getLong(1));
                    ResultSet rs2 = ps2.executeQuery();
                    if (rs2.next()) {
                        ps3.setLong(1, rs.getLong(1));
                        ps3.setInt(2, rs2.getInt(1));
                        ps3.setLong(3, this.mt_id);
                        ps3.setLong(4, rs2.getLong(2));
                        ps3.setDate(5, rs2.getDate(3));
                        ps3.setInt(6, rs2.getInt(4));
                        ps3.executeUpdate();
                        Session.getLog().info("parent_child inserted");
                    }
                    ps1.setLong(1, this.mt_id);
                    ps1.executeUpdate();
                    Session.getLog().info("traffics inserted");
                }
                ps04.setLong(1, this.mt_id);
                ResultSet rs04 = ps04.executeQuery();
                if (!rs04.next()) {
                    ps4.setLong(1, this.mt_id);
                    ps4.executeUpdate();
                    Session.getLog().info("resource_amount inserted");
                } else {
                    Session.getLog().info("resource_amount id= " + rs04.getLong(1) + " skipped");
                }
            }
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps03);
            Session.closeStatement(ps3);
            Session.closeStatement(ps04);
            Session.closeStatement(ps4);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            Session.closeStatement(ps1);
            Session.closeStatement(ps2);
            Session.closeStatement(ps03);
            Session.closeStatement(ps3);
            Session.closeStatement(ps04);
            Session.closeStatement(ps4);
            con.close();
            throw th;
        }
    }
}
