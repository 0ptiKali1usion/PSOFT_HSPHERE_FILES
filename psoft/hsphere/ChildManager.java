package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/* loaded from: hsphere.zip:psoft/hsphere/ChildManager.class */
public class ChildManager {
    Account account;
    HashMap childHolders = new HashMap();
    HashMap childParentMap = new HashMap();
    Collection allResources = new ArrayList();

    public ChildManager(Account account) throws SQLException {
        this.account = account;
        load();
    }

    public Collection getAllResources() {
        Collection tmpColl = new ArrayList();
        tmpColl.addAll(this.allResources);
        return tmpColl;
    }

    public ResourceId getParent(ResourceId child) {
        return (ResourceId) this.childParentMap.get(child);
    }

    private void load() throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT parent_id, parent_type, child_id, child_type FROM parent_child WHERE account_id = ? AND p_begin IS NOT NULL ORDER BY parent_id, child_id");
            ps.setLong(1, this.account.getId().getId());
            ResultSet rs = ps.executeQuery();
            ResourceId parent = null;
            long parentId = Long.MIN_VALUE;
            ChildHolder ch = null;
            while (rs.next()) {
                long _pId = rs.getLong(1);
                if (parentId != _pId) {
                    parentId = _pId;
                    parent = new ResourceId(parentId, rs.getInt(2));
                    ch = new ChildHolder(parent);
                    this.childHolders.put(parent, ch);
                }
                ResourceId child = new ResourceId(rs.getLong(3), rs.getInt(4));
                this.childParentMap.put(child, parent);
                ch.addChild(child);
                this.allResources.add(child);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public ChildHolder getChildHolder(ResourceId parent) {
        ChildHolder ch = (ChildHolder) this.childHolders.get(parent);
        if (ch == null) {
            HashMap hashMap = this.childHolders;
            ChildHolder childHolder = new ChildHolder(parent);
            ch = childHolder;
            hashMap.put(parent, childHolder);
        }
        return ch;
    }

    public synchronized void addChild(ResourceId id, ResourceId child) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO parent_child (account_id, child_id, child_type, parent_id, parent_type) VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, Session.getAccount().getId().getId());
            ps.setLong(2, child.getId());
            ps.setInt(3, child.getType());
            ps.setLong(4, id.getId());
            ps.setInt(5, id.getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            getChildHolder(id).addChild(child);
            this.allResources.add(child);
            this.childParentMap.put(child, id);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void deleteChild(ResourceId id) throws Exception {
        deleteChild(getParent(id), id);
    }

    private synchronized void deleteChild(ResourceId id, ResourceId child) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM parent_child WHERE child_id=? AND child_type=?");
            ps.setLong(1, child.getId());
            ps.setInt(2, child.getType());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            getChildHolder(id).deleteChild(child);
            this.allResources.remove(child);
            this.childHolders.remove(child);
            this.childParentMap.remove(child);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }
}
