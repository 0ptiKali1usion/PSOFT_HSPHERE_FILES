package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.util.USFormat;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ResellerBackupResource.class */
public class ResellerBackupResource extends AbstractChangeableResource {
    public ResellerBackupResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        Iterator i = initValues.iterator();
        Double tmpSize = new Double(USFormat.parseDouble((String) i.next()));
        this.size = tmpSize.intValue();
    }

    public ResellerBackupResource(ResourceId id) throws Exception {
        super(id);
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT size FROM reseller_backup WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.size = rs.getInt(1);
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
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("INSERT INTO reseller_backup(id, size) VALUES (?,?)");
            ps.setLong(1, getId().getId());
            ps.setInt(2, this.size);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void changeResourcePhysical(double oldSize) throws Exception {
    }

    public int getInUse() throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            Date pBegin = new Date(getPeriodBegin().getTime());
            Date pEnd = new Date(nextMonthlyBillingDate().getTime());
            ps = con.prepareStatement("SELECT count(*) AS amount FROM backup_log bl, backup b, parent_child pc, accounts a WHERE bl.state = ? AND bl.finished >= ? AND bl.finished < ? AND bl.parent_id = b.id AND b.id = pc.child_id AND pc.account_id = a.id AND a.reseller_id = ?");
            ps.setInt(1, 3);
            ps.setDate(2, pBegin);
            ps.setDate(3, pEnd);
            ps.setLong(4, getAccount().getUser().getId());
            Session.getLog().debug("Inside ResellerBackupResource::InUse SQL=" + ps.toString());
            ResultSet rs = ps.executeQuery();
            rs.next();
            int i = rs.getInt(1);
            Session.closeStatement(ps);
            con.close();
            return i;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void saveSize(double newSize) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE reseller_backup SET size = ? WHERE id = ?");
            ps.setInt(1, (int) newSize);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public double getUsageMultiplier() {
        try {
            int completedBackups = getInUse();
            if (getAmount() >= completedBackups) {
                return 0.0d;
            }
            return completedBackups - getAmount();
        } catch (Exception e) {
            return 0.0d;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("size".equals(key)) {
            return new TemplateString(this.size);
        }
        if ("used".equals(key)) {
            try {
                return new TemplateString(getInUse());
            } catch (Exception e) {
                return new TemplateString("not available");
            }
        }
        return super.get(key);
    }
}
