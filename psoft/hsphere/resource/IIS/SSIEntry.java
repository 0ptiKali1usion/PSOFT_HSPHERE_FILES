package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/SSIEntry.class */
public class SSIEntry extends MimeTypeResource {
    public SSIEntry(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public SSIEntry(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "ext".equals(key) ? new TemplateString(this.ext) : super.get(key);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        String hostnum = recursiveGet("hostnum").toString();
        he.exec("ssi-addextension.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String hostnum;
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        String name = recursiveGet("real_name").toString();
        if (Session.getAccount().isBeingMoved() || targetHostId != getHostId()) {
            VirtualHostingResource vhr = recursiveGet("vhostingResource");
            hostnum = Integer.toString(vhr.getActualHostNum(targetHostId));
        } else {
            hostnum = recursiveGet("hostnum").toString();
        }
        he.exec("ssi-delextension.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"extension", this.ext}});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ssi_entry.setup", new Object[]{this.ext, _getName()});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssi_entry.recurrent", new Object[]{getPeriodInWords(), this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssi_entry.refund", new Object[]{this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ssi_entry.refundall", new Object[]{_getName(), this.ext, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.IIS.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("ssi_entry.desc", new Object[]{recursiveGet("real_name").toString(), this.ext});
    }
}
