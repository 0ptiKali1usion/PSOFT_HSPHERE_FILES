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

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/ASPSecuredResource.class */
public class ASPSecuredResource extends Resource implements HostDependentResource {
    public ASPSecuredResource(int type, Collection init) throws Exception {
        super(type, init);
    }

    public ASPSecuredResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            physicalDelete(getHostId());
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
        return Localizer.translateMessage("bill.asp_secured.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.asp_secured.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.asp_secured.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.asp_secured.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        int lictype = 0;
        String license = "";
        String licusername = "";
        ResourceId rid = getParent().get().getParent().findChild("asp_secured_license");
        if (rid != null) {
            long lic_id = rid.getId();
            Connection con = Session.getDb();
            PreparedStatement ps = null;
            try {
                ps = con.prepareStatement("SELECT type, license, username FROM asp_secured_lic WHERE id = ?");
                ps.setLong(1, lic_id);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    lictype = rs.getInt(1);
                    license = rs.getString(2);
                    licusername = rs.getString(3);
                } else {
                    throw new HSUserException("asp_secured.licinfo_absent");
                }
            } finally {
                Session.closeStatement(ps);
                con.close();
            }
        }
        String login = recursiveGet("login").toString();
        String hostnum = recursiveGet("hostnum").toString();
        String name = recursiveGet("real_name").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        he.exec("set-aspsecured.asp", (String[][]) new String[]{new String[]{"user-name", login}, new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"lictype", String.valueOf(lictype)}, new String[]{"licensekey", license}, new String[]{"licusername", licusername}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        String login = recursiveGet("login").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        String name = recursiveGet("real_name").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        he.exec("del-aspsecured.asp", (String[][]) new String[]{new String[]{"user-name", login}, new String[]{"hostnum", hostnum}, new String[]{"hostname", name}});
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
        return Localizer.translateMessage("asp_secured.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
