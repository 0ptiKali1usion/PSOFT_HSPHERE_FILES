package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/ASPNETResource.class */
public class ASPNETResource extends Resource implements HostDependentResource {
    public ASPNETResource(int type, Collection init) throws Exception {
        super(type, init);
    }

    public ASPNETResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        physicalCreate(getHostId());
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
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
        return Localizer.translateMessage("bill.asp_net.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.asp_net.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.asp_net.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.asp_net.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        String hostname = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("create", new String[]{new String[]{"resourcename", "aspnet"}, new String[]{"hostname", hostname}});
        } else {
            he.exec("aspnet-install.asp", (String[][]) new String[]{new String[]{"hostname", hostname}, new String[]{"hostnum", hostnum}});
        }
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    /* JADX WARN: Type inference failed for: r2v3, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        String hostname = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        if (WinService.isSOAPSupport()) {
            he.invokeMethod("delete", new String[]{new String[]{"resourcename", "aspnet"}, new String[]{"hostname", hostname}});
        } else {
            he.exec("aspnet-remove.asp", (String[][]) new String[]{new String[]{"hostname", hostname}, new String[]{"hostnum", hostnum}});
        }
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
        return Localizer.translateMessage("asp_net.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
