package psoft.hsphere.resource.apache;

import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.VHResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/PHP3Module.class */
public class PHP3Module extends VHResource {
    public PHP3Module(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public PHP3Module(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        StringBuffer buf = new StringBuffer();
        StringBuffer childrenResources = new StringBuffer("");
        Collection<ResourceId> ch = getChildHolder().getChildren();
        synchronized (ch) {
            for (ResourceId resourceId : ch) {
                VHResource r = (VHResource) Resource.get(resourceId);
                childrenResources.append(r.getServerConfig());
            }
        }
        buf.append("<IfModule mod_php4.c>\n");
        buf.append(childrenResources);
        buf.append("</IfModule>\n");
        buf.append("<IfModule mod_php5.c>\n");
        buf.append(childrenResources);
        buf.append("</IfModule>\n");
        return buf.toString();
    }

    @Override // psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.php3.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.php3.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.php3.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.php3.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("php3.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
