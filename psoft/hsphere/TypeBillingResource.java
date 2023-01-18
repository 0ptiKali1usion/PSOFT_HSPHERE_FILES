package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/TypeBillingResource.class */
public class TypeBillingResource extends Resource {
    protected int resourceType;

    public TypeBillingResource(ResourceId rId) throws Exception {
        super(rId);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT rtype FROM type_billing WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.resourceType = rs.getInt(1);
                return;
            }
            throw new Exception("Unable to retrieve type for resource " + getId());
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        setPeriodBegin(TimeUtils.getDate());
    }

    public TypeBillingResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        if (i.hasNext()) {
            this.resourceType = Integer.parseInt(TypeRegistry.getTypeId((String) i.next()));
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("INSERT INTO type_billing (id, type, account_id, rtype) VALUES (?, ?, ?, ?)");
                ps.setLong(1, getId().getId());
                ps.setInt(2, getId().getType());
                ps.setLong(3, getAccount().getId().getId());
                ps.setInt(4, this.resourceType);
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
                getAccount().addTrigger(this.resourceType, getId());
                return;
            } catch (Throwable th) {
                Session.closeStatement(ps);
                con.close();
                throw th;
            }
        }
        throw new Exception("Resource type not specified for billing");
    }

    @Override // psoft.hsphere.Resource
    public double getRecurrentMultiplier() {
        Session.getLog().info("Multiplier for :" + this + " Is " + getAccount().getTypeCounter().getValue(this.resourceType) + "-->" + this.resourceType);
        return getAccount().getTypeCounter().getValue(this.resourceType);
    }
}
