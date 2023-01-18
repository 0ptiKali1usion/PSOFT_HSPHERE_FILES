package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Hashtable;
import java.util.Iterator;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.ResourceMoveException;
import psoft.hsphere.Session;
import psoft.hsphere.User;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MoveVHostResources.class */
public class MoveVHostResources extends MoveHostDependentResources {
    public MoveVHostResources(Hashtable h) throws Exception {
        super(h);
    }

    @Override // psoft.hsphere.tools.MoveHostDependentResources
    /* renamed from: go */
    public void mo11go(long accId, long newHostId) throws Exception {
        Connection con = Session.getDb();
        long userId = -1;
        try {
            PreparedStatement ps = con.prepareStatement("SELECT uu.hostid FROM unix_user uu, parent_child pc WHERE uu.id = pc.child_id AND pc.child_type=? AND pc.account_id = ?");
            ps.setLong(1, 7L);
            ps.setLong(2, accId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                long currHostId = rs.getLong(1);
                if (currHostId == newHostId) {
                    System.out.println("The specified account is already on the target server. Aborting operation");
                    return;
                }
                System.out.println("Moving VHost resources for account #" + accId + " to logical server #" + newHostId);
                try {
                    try {
                        ps = con.prepareStatement("SELECT DISTINCT user_id FROM user_account WHERE account_id=?");
                        ps.setLong(1, accId);
                        ResultSet rs2 = ps.executeQuery();
                        if (rs2.next()) {
                            userId = rs2.getLong(1);
                        }
                        Session.closeStatement(ps);
                        con.close();
                        if (userId == -1) {
                            System.out.println("Can not find owner of the account #" + accId + ". Skipping");
                            return;
                        }
                    } catch (SQLException ex) {
                        System.out.println("Error during query ");
                        ex.printStackTrace();
                        Session.closeStatement(ps);
                        con.close();
                        if (userId == -1) {
                            System.out.println("Can not find owner of the account #" + accId + ". Skipping");
                            return;
                        }
                    }
                    User oldUser = Session.getUser();
                    Account oldAcc = Session.getAccount();
                    try {
                        User u = User.getUser(userId);
                        Session.setUser(u);
                        Account a = null;
                        Iterator i = u.getAccountIds().iterator();
                        while (i.hasNext()) {
                            ResourceId rId = (ResourceId) i.next();
                            if (rId.getId() == accId) {
                                a = u.getAccount(rId);
                            }
                        }
                        if (a != null) {
                            System.out.println("Got account " + a.getId().toString() + " for processing");
                            Session.setAccount(a);
                            try {
                                if ("1".equals(a.getPlan().getValue("_CREATED_BY_"))) {
                                    a.moveVHostToHost(newHostId, getLogger());
                                } else if ("2".equals(a.getPlan().getValue("_CREATED_BY_"))) {
                                    a.moveWinVHostToHost(newHostId, getLogger());
                                } else {
                                    System.out.println("Only Unix or Windows plans based accounts can be moved.");
                                }
                            } catch (ResourceMoveException ex2) {
                                System.out.println("Unable to move " + a.getId().getId() + " account :" + ex2.getMessage());
                            }
                        } else {
                            System.out.println("Can not get account #" + accId + " for processing. Please check the account id");
                        }
                        return;
                    } finally {
                        Session.setUser(oldUser);
                        Session.setAccount(oldAcc);
                    }
                } catch (Throwable th) {
                    Session.closeStatement(ps);
                    con.close();
                    if (userId == -1) {
                        System.out.println("Can not find owner of the account #" + accId + ". Skipping");
                        return;
                    }
                    throw th;
                }
            }
            System.out.println("Unix user for the specified account was not found");
        } catch (Exception ex3) {
            Session.getLog().error("Check of moveability failed", ex3);
            System.out.println("An error occured during moveability test.");
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("Depricated ... ");
        System.exit(0);
        MoveVHostResources test = new MoveVHostResources(parseInitParams(args));
        test.setLogger(new ToolLogger(args));
        System.out.println("Done");
        test.m13go();
        System.exit(0);
    }
}
