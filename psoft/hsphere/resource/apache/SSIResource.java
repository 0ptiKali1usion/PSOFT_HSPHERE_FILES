package psoft.hsphere.resource.apache;

import java.util.Collection;
import java.util.Date;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/SSIResource.class */
public class SSIResource extends MimeTypeResource {
    protected static final String DEFAULT_EXT = ".shtml";

    public SSIResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        if (initValues.size() == 0) {
            this.ext = DEFAULT_EXT;
        }
    }

    public SSIResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() {
        return "AddType text/html " + this.ext + "\nAddHandler server-parsed " + this.ext + "\n";
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getSetupChargeDescription(Date now) throws Exception {
        return Localizer.translateMessage("bill.ssi_entry.setup", new Object[]{this.ext, _getName()});
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getRecurrentChangeDescripton(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssi_entry.recurrent", new Object[]{getPeriodInWords(), this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getRecurrentRefundDescription(Date begin, Date end) throws Exception {
        return Localizer.translateMessage("bill.ssi_entry.refund", new Object[]{this.ext, _getName(), f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    protected String getRecurrentRefundAllDescription(Date begin, Date end) {
        return Localizer.translateMessage("bill.ssi_entry.refundall", new Object[]{_getName(), this.ext, f42df.format(begin), f42df.format(end)});
    }

    @Override // psoft.hsphere.resource.apache.MimeTypeResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("ssi.desc", new Object[]{recursiveGet("real_name").toString(), this.ext});
    }
}
