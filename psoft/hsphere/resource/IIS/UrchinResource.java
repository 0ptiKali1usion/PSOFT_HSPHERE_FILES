package psoft.hsphere.resource.IIS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.hsphere.resource.admin.UrchinLicenseManager;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/UrchinResource.class */
public class UrchinResource extends Resource implements HostDependentResource {
    protected long lic_id;

    public UrchinResource(int type, Collection init) throws Exception {
        super(type, init);
    }

    public UrchinResource(ResourceId rid) throws Exception {
        super(rid);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT lic_id FROM urchin WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.lic_id = rs.getLong(1);
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
        recursiveGet("real_name").toString();
        recursiveGet("hostnum").toString();
        long host_id = Long.parseLong(recursiveGet("host_id").toString());
        this.lic_id = UrchinLicenseManager.getFreeLicense(host_id);
        if (this.lic_id == -1) {
            throw new HSUserException("urchin.nofreelicense");
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO urchin (id, lic_id) VALUES (?, ?)");
            ps.setLong(1, getId().getId());
            ps.setLong(2, this.lic_id);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            if (!UrchinLicenseManager.changeUsedLicense(this.lic_id, 1)) {
                throw new HSUserException("urchin.nofreelicense");
            }
            physicalCreate(getHostId());
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
        }
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM urchin WHERE id = ?");
            ps.setLong(1, getId().getId());
            ps.executeUpdate();
            if (this.lic_id != -1) {
                UrchinLicenseManager.changeUsedLicense(this.lic_id, -1);
            }
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    protected String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.urchin.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.urchin.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.urchin.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.urchin.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        he.exec("urchin-addreport.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        he.exec("urchin-delreport.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("urchin.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
