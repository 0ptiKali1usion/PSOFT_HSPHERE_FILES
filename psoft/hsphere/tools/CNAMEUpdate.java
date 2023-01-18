package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.List;
import psoft.hsphere.Account;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/tools/CNAMEUpdate.class */
public class CNAMEUpdate extends C0004CP {
    public CNAMEUpdate() throws Exception {
        super("psoft_config.hsphere");
    }

    public static void main(String[] argv) {
        try {
            CNAMEUpdate test = new CNAMEUpdate();
            test.m19go();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("CNAME adding finished");
        System.exit(0);
    }

    /* renamed from: go */
    public void m19go() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            Session.getLog().info("Starting :");
            ps = con.prepareStatement("SELECT p1.child_id,p1.account_id,user_id FROM parent_child p1, user_account ua WHERE p1.child_type=1000 AND p1.parent_type IN (2,31,35,37) AND NOT EXISTS (SELECT child_id FROM parent_child p2 WHERE p2.parent_id = p1.child_id AND p2.child_type=3006) AND p1.account_id = ua.account_id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    System.out.println("Working on acc#" + rs.getString("account_id"));
                    User u = User.getUser(rs.getLong("user_id"));
                    Session.setUser(u);
                    Account acc = u.getAccount(new ResourceId(rs.getString("account_id") + "_0"));
                    Session.setAccount(acc);
                    Resource mailService = new ResourceId(rs.getString("child_id") + "_1000").get();
                    try {
                        System.out.print("Adding CNAME...");
                        List arg = Arrays.asList("mail", mailService.get("mail_server_name").toString());
                        mailService.addChild("cname_record", "mail", arg);
                        System.out.println("[    OK    ]");
                    } catch (Exception ex) {
                        System.out.println("[   FAIL   ]");
                        ex.printStackTrace();
                    }
                } catch (Exception ex1) {
                    System.out.println("Error occured....");
                    ex1.printStackTrace();
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
