package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.C0004CP;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/DomainMover.class */
public class DomainMover extends C0004CP {
    int UNIXUSER_TYPE_ID;

    public DomainMover() throws Exception {
        super("psoft_config.hsphere");
        this.UNIXUSER_TYPE_ID = 7;
    }

    public void move(String domain, String username) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT account_id FROM users, user_account WHERE username = ? AND users.id = user_account.user_id");
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            List<Object> l = new ArrayList();
            while (rs.next()) {
                l.add(new Long(rs.getLong(1)));
            }
            if (l.size() == 1) {
                move(domain, ((Long) l.get(0)).longValue());
            } else if (l.size() == 0) {
                throw new Exception("Either user not found, or this user has no accounts");
            } else {
                StringBuffer buf = new StringBuffer("This user has " + l.size() + " accounts:");
                for (Object obj : l) {
                    buf.append(obj).append(" ");
                }
                throw new Exception(buf.toString());
            }
        } finally {
            ps.close();
            con.close();
        }
    }

    public void move(String domain, long accountId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT id FROM domains WHERE UPPER(name) = ?");
            ps.setString(1, domain.toUpperCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                move(rs.getLong(1), accountId);
                return;
            }
            throw new Exception("Domain not found: " + domain);
        } finally {
            ps.close();
            con.close();
        }
    }

    void move(long domainId, long accountId) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT child_id FROM parent_child WHERE account_id = ? AND child_type = ?");
            ps.setLong(1, accountId);
            ps.setInt(2, this.UNIXUSER_TYPE_ID);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Connection con2 = Session.getTransConnection();
                MoveResourceTree.move(domainId, rs.getLong(1), accountId, con2);
                Session.commitTransConnection(con2);
                return;
            }
            throw new Exception("Invlid account id, cannot find unix user resource:" + accountId);
        } finally {
            ps.close();
            con.close();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            System.err.println("Usage: java psoft.hsphere.tools.DomainMover domain_name account_id");
            System.err.println("or");
            System.err.println("java psoft.hsphere.tools.DomainMover domain_name username");
            System.err.println("For users with exactly one account");
            System.exit(-1);
        }
        DomainMover dm = new DomainMover();
        try {
            dm.move(args[0], Long.parseLong(args[1]));
        } catch (NumberFormatException e) {
            dm.move(args[0], args[1]);
        }
    }
}
