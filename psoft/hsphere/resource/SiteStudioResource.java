package psoft.hsphere.resource;

import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/SiteStudioResource.class */
public class SiteStudioResource extends Resource {
    public SiteStudioResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public SiteStudioResource(ResourceId rId) throws Exception {
        super(rId);
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
        return Localizer.translateMessage("bill.sitestudio.setup", new Object[]{_getName()});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.sitestudio.recurrent", new Object[]{getPeriodInWords(), _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.sitestudio.refund", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.sitestudio.refundall", new Object[]{_getName(), f42df.format(begin), f42df.format(end)});
    }
}
