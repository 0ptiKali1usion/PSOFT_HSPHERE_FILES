package psoft.hsphere.resource.IIS;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import org.apache.axis.message.SOAPEnvelope;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.axis.WinService;
import psoft.hsphere.resource.HostDependentResource;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.ListAdapter;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/CFModule.class */
public class CFModule extends Resource implements HostDependentResource {
    protected static final String[] cfDefaultTypes = {".cfm", ".cfml", ".cfc", ".cfr", ".cfswf"};

    public CFModule(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public CFModule(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public boolean canBeMovedTo(long newHostId) throws Exception {
        return true;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalCreate(long targetHostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            SOAPEnvelope envelope = he.invokeMethod("create", new String[]{new String[]{"resourcename", "coldfusion"}, new String[]{"hostname", recursiveGet("real_name").toString()}});
            if (envelope == null) {
                throw new HSUserException("The error invoked during SOAP calling");
            }
            return;
        }
        throw new HSUserException("SOAP is not supported");
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v2, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.HostDependentResource
    public void physicalDelete(long targetHostId) throws Exception {
        String name = recursiveGet("real_name").toString();
        WinHostEntry he = (WinHostEntry) HostManager.getHost(targetHostId);
        boolean isSOAP = WinService.isSOAPSupport();
        if (isSOAP) {
            SOAPEnvelope envelope = he.invokeMethod("delete", new String[]{new String[]{"resourcename", "coldfusion"}, new String[]{"hostname", name}});
            if (envelope == null) {
                throw new HSUserException("The error invoked during SOAP calling");
            }
            return;
        }
        throw new HSUserException("SOAP is not supported");
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public void setHostId(long newHostId) throws Exception {
    }

    @Override // psoft.hsphere.resource.HostDependentResource
    public long getHostId() throws Exception {
        return Long.parseLong(recursiveGet("host_id").toString());
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
        return Localizer.translateMessage("bill.cf.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cf.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.cf.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.cf.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("cf.desc");
    }

    public TemplateModel FM_getCFTypes() throws Exception {
        List<String> defTypes = Arrays.asList(cfDefaultTypes);
        List<ResourceId> entries = (List) getChildHolder().getChildrenByName("cfentry");
        List tmp = new ArrayList();
        List data = new ArrayList();
        for (ResourceId rid : entries) {
            HashMap hm = new HashMap();
            CFEntry cfentry = (CFEntry) rid.get();
            String curType = cfentry.ext;
            hm.put("name", curType);
            hm.put("value", "1");
            hm.put("id", rid.get("id").toString() + "_" + rid.get("type_id").toString());
            if (defTypes.contains(curType)) {
                tmp.add(curType);
            }
            data.add(hm);
        }
        for (String curType2 : defTypes) {
            if (!tmp.contains(curType2)) {
                HashMap hm2 = new HashMap();
                hm2.put("name", curType2);
                hm2.put("value", "0");
                data.add(hm2);
            }
        }
        return new ListAdapter(data);
    }
}
