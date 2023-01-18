package psoft.hsphere.tools;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.Session;

/* loaded from: hsphere.zip:psoft/hsphere/tools/MoveResourceTree.class */
public class MoveResourceTree {
    public static void move(long resourceId, long newParentId, long newAccountId, Connection con) throws SQLException {
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT p_begin FROM parent_child WHERE child_id = ?");
                ps.setLong(1, newParentId);
                ResultSet rs = ps.executeQuery();
                rs.next();
                Timestamp d = rs.getTimestamp(1);
                Session.closeStatement(ps);
                try {
                    try {
                        PreparedStatement ps1 = con.prepareStatement("UPDATE parent_child SET parent_id = ? WHERE child_id = ?");
                        ps1.setLong(1, newParentId);
                        ps1.setLong(2, resourceId);
                        ps1.executeUpdate();
                        Session.closeStatement(ps);
                        move(resourceId, newAccountId, d, con);
                        Session.commitTransConnection(con);
                    } finally {
                        Session.closeStatement(ps);
                    }
                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                }
            } catch (Throwable th) {
                ps = ps;
                throw th;
            }
        } catch (SQLException ex2) {
            con.rollback();
            throw ex2;
        }
    }

    public static void move(long resourceId, long newAccountId, Timestamp d, Connection con) throws SQLException {
        List<Long> l = new ArrayList();
        PreparedStatement ps = null;
        PreparedStatement ps1 = null;
        try {
            try {
                ps = con.prepareStatement("SELECT child_id FROM parent_child WHERE parent_id = ?");
                ps.setLong(1, resourceId);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    l.add(new Long(rs.getLong(1)));
                }
                Session.closeStatement(ps);
                for (Long l2 : l) {
                    move(l2.longValue(), newAccountId, d, con);
                }
                try {
                    try {
                        ps1 = con.prepareStatement("UPDATE parent_child SET account_id = ?, p_begin = ? WHERE child_id = ?");
                        ps1.setLong(1, newAccountId);
                        ps1.setTimestamp(2, d);
                        ps1.setLong(3, resourceId);
                        ps1.executeUpdate();
                        Session.closeStatement(ps1);
                    } catch (Throwable th) {
                        Session.closeStatement(ps1);
                        throw th;
                    }
                } catch (SQLException ex) {
                    con.rollback();
                    throw ex;
                }
            } catch (SQLException ex2) {
                con.rollback();
                throw ex2;
            }
        } catch (Throwable th2) {
            Session.closeStatement(ps);
            throw th2;
        }
    }
}
