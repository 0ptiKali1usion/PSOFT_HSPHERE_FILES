package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Hashtable;
import psoft.util.NFUCache;

/* loaded from: hsphere.zip:psoft/hsphere/AccessTable.class */
public class AccessTable {
    protected Hashtable tMask;
    protected Hashtable rMask;
    protected Integer mask;
    protected static NFUCache cache = new NFUCache(100);

    public static AccessTable get(User user, Account a) throws Exception {
        Session.getLog().info("User: " + user + " Account: " + a);
        String key = user.getId() + "_" + a.getId().getId();
        AccessTable table = (AccessTable) cache.get(key);
        if (table == null) {
            table = new AccessTable(user, a);
        }
        cache.put(key, table);
        return table;
    }

    protected AccessTable(User user, Account a) throws Exception {
        boolean inside = false;
        long account_id = a.getId().getId();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT resource_id, resource_type, mask FROM access_resource WHERE user_id = ? AND account_id = ?");
            ps2.setLong(1, user.getId());
            ps2.setLong(2, account_id);
            ResultSet rs = ps2.executeQuery();
            this.rMask = new Hashtable();
            while (rs.next()) {
                this.rMask.put(new ResourceId(rs.getLong(1), rs.getInt(2)), new Integer(rs.getInt(3)));
                if (account_id == rs.getLong(1)) {
                    this.mask = new Integer(rs.getInt(3));
                }
                inside = true;
            }
            ps2.close();
            ps = con.prepareStatement("SELECT type_id, mask FROM access_type WHERE user_id = ? AND account_id = ?");
            ps.setLong(1, user.getId());
            ps.setLong(2, account_id);
            ResultSet rs2 = ps.executeQuery();
            this.tMask = new Hashtable();
            while (rs2.next()) {
                this.tMask.put(rs2.getString(1), new Integer(rs2.getInt(2)));
                inside = true;
            }
            if (this.mask == null) {
                if (!inside) {
                    this.tMask = null;
                    this.rMask = null;
                    this.mask = new Integer(16777215);
                } else {
                    this.rMask.put(a.getId(), new Integer(16777215));
                }
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    protected Integer getMask(int type) {
        return getMask(Integer.toString(type));
    }

    protected Integer getMask(String type) {
        return (Integer) this.tMask.get(type);
    }

    protected Integer getMask(ResourceId rid) {
        Integer i = (Integer) this.rMask.get(rid);
        return i != null ? i : getTypeMask(Session.getAccount().getChildManager().getParent(rid), rid.getType());
    }

    protected Integer getTypeMask(ResourceId rid, int type) {
        Integer m2 = getMask(type);
        Integer m1 = getMask(rid);
        return m2 == null ? m1 : new Integer(m1.intValue() & m2.intValue());
    }

    public boolean check(ResourceId rid, int type, int m) {
        int i = (this.mask == null ? getTypeMask(rid, type) : this.mask).intValue();
        return (i & m) == m;
    }

    public boolean check(ResourceId rid, int m) {
        int i = (this.mask == null ? getMask(rid) : this.mask).intValue();
        return (i & m) == m;
    }
}
