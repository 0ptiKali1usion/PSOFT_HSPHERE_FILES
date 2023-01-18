package psoft.hsphere.resource.mpf.hostedexchange;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.xml.rpc.holders.BooleanHolder;
import javax.xml.rpc.holders.StringHolder;
import org.apache.log4j.Category;
import psoft.hsphere.NoSuchTypeException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.mpf.MPFManager;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/DistributionList.class */
public class DistributionList extends Resource {
    private static final Category log = Category.getInstance(DistributionList.class.getName());
    String managedBy;
    String name;
    List subscribers;

    public DistributionList(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        this.managedBy = (String) i.next();
        this.name = (String) i.next();
    }

    public DistributionList(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT managedBy, name FROM he_distribution_list WHERE id = ?");
            ps.setLong(1, id.getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.managedBy = rs.getString(1);
                this.name = rs.getString(2);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO he_distribution_list (id, managedBy, name) VALUES (?, ?, ?)");
            ps.setLong(1, this.f41id.getId());
            ps.setString(2, this.managedBy);
            ps.setString(3, this.name);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            MPFManager manager = MPFManager.getManager();
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            String result = manager.getHES().createDistributionList(manager.getLDAP(bo.getName()), this.name, manager.getPdc(), this.managedBy, true);
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        int type = TypeRegistry.getIntTypeId("distribution_list");
        long dlId = 0;
        try {
            ps = con.prepareStatement("SELECT id FROM he_distribution_list_subscr WHERE subscriber_id=?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                try {
                    dlId = rs.getLong(1);
                    DistributionList dl = (DistributionList) new ResourceId(dlId, type).get();
                    dl.FM_deleteSubscriber(getId());
                } catch (Exception ex) {
                    Session.getLog().error("Unable to remove subscriber " + getId().toString() + " from the distribution list " + dlId + "_" + type, ex);
                }
            }
            Session.closeStatement(ps);
            con.close();
            if (this.initialized) {
                MPFManager manager = MPFManager.getManager();
                BusinessOrganization bo = (BusinessOrganization) getParent().get();
                String result = manager.getHES().deleteDistributionList(manager.getLDAP(bo.getName(), this.name), manager.getPdc(), true);
                MPFManager.Result res = manager.processHeResult(result);
                if (!res.isSuccess()) {
                    throw new Exception(res.getError());
                }
            }
            try {
                PreparedStatement ps2 = con.prepareStatement("DELETE FROM he_distribution_list WHERE id = ?");
                ps2.setLong(1, this.f41id.getId());
                ps2.executeUpdate();
                ps = con.prepareStatement("DELETE FROM he_distribution_list_subscr WHERE id = ?");
                ps.setLong(1, this.f41id.getId());
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } finally {
            }
        } finally {
        }
    }

    public static void deleteSubscriber(ResourceId subscriberId) throws SQLException {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_distribution_list_subscr WHERE subscriber_id = ?");
            ps.setLong(1, subscriberId.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_changeSettings(String managedBy) throws Exception {
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        String result = manager.getHES().modifyDistributionList(manager.getLDAP(bo.getName(), this.name), manager.getPdc(), managedBy, true);
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception(res.getError());
        }
        this.managedBy = managedBy;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_distribution_list SET managedBy = ? WHERE id = ?");
            ps.setString(1, managedBy);
            ps.setLong(2, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public boolean FM_contains(String s) throws SQLException, NoSuchTypeException {
        ResourceId sRid = new ResourceId(s);
        return getSubscribers().contains(sRid);
    }

    private synchronized List getSubscribers() throws SQLException, NoSuchTypeException {
        if (this.subscribers != null) {
            return this.subscribers;
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT dl.subscriber_id, p.child_type FROM he_distribution_list_subscr AS dl JOIN parent_child as p ON (p.child_id=dl.subscriber_id) WHERE id = ?");
            ps.setLong(1, this.f41id.getId());
            ResultSet rs = ps.executeQuery();
            this.subscribers = new ArrayList();
            while (rs.next()) {
                this.subscribers.add(new ResourceId(rs.getLong(1), rs.getInt(2)));
            }
            Session.closeStatement(ps);
            con.close();
            return this.subscribers;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_addSubscriber(ResourceId subscriberId) throws Exception {
        getSubscribers();
        if (this.subscribers.contains(subscriberId)) {
            return;
        }
        Resource res = subscriberId.get();
        String user = "";
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        if (res instanceof BusinessUser) {
            String user2 = ((BusinessUser) res).get("principalName").toString();
            user = user2 + "@" + ((BusinessUser) res).getDomain();
        } else if (res instanceof DistributionList) {
            user = ((DistributionList) res).get("name").toString();
        }
        MPFManager manager = MPFManager.getManager();
        BooleanHolder groupAddResult = new BooleanHolder();
        StringHolder errorReturn = new StringHolder();
        manager.getMADS().groupAdd(manager.getLDAP(bo.getName(), this.name), manager.getLDAP(bo.getName(), user), manager.getPdc(), true, groupAddResult, errorReturn);
        if (!groupAddResult.value) {
            throw new Exception(errorReturn.value);
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO he_distribution_list_subscr (id, subscriber_id) VALUES (?, ?)");
            ps.setLong(1, this.f41id.getId());
            ps.setLong(2, subscriberId.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            this.subscribers.add(subscriberId);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public void FM_deleteSubscriber(ResourceId subscriberId) throws Exception {
        getSubscribers();
        if (!this.subscribers.contains(subscriberId)) {
            return;
        }
        BooleanHolder groupRemoveResult = new BooleanHolder();
        StringHolder errorReturn = new StringHolder();
        Resource res = subscriberId.get();
        String user = "";
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        if (res instanceof BusinessUser) {
            String user2 = ((BusinessUser) res).get("principalName").toString();
            user = user2 + "@" + bo.getDomain().getName();
        } else if (res instanceof DistributionList) {
            user = ((DistributionList) res).get("name").toString();
        }
        MPFManager manager = MPFManager.getManager();
        manager.getMADS().groupRemove(manager.getLDAP(bo.getName(), this.name), manager.getLDAP(bo.getName(), user), manager.getPdc(), true, groupRemoveResult, errorReturn);
        if (!groupRemoveResult.value) {
            throw new Exception(errorReturn.value);
        }
        this.subscribers.remove(subscriberId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_distribution_list_subscr WHERE id = ? AND subscriber_id = ?");
            ps.setLong(1, this.f41id.getId());
            ps.setLong(2, subscriberId.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("name".equals(key)) {
            return new TemplateString(this.name);
        }
        if ("managedBy".equals(key)) {
            return new TemplateString(this.managedBy);
        }
        if ("subscribers".equals(key)) {
            try {
                return new TemplateList(getSubscribers());
            } catch (SQLException e) {
                log.warn(e);
            } catch (NoSuchTypeException e2) {
                log.warn(e2);
            }
        }
        return super.get(key);
    }

    public String getManagedBy() {
        return this.managedBy;
    }

    public String getName() {
        return this.name;
    }

    public void resetSubscriberList() {
        this.subscribers = null;
    }
}
