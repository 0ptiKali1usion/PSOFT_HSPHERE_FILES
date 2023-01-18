package psoft.hsphere.resource.apache;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.HostDependentResource;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/TransferLogResource.class */
public class TransferLogResource extends LogResource implements HostDependentResource {
    private String LOG_FORMAT;

    public TransferLogResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.LOG_FORMAT = "LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-agent}i\\\"\"";
    }

    public TransferLogResource(ResourceId rid) throws Exception {
        super(rid);
        this.LOG_FORMAT = "LogFormat \"%h %l %u %t \\\"%r\\\" %>s %b \\\"%{Referer}i\\\" \\\"%{User-agent}i\\\"\"";
    }

    @Override // psoft.hsphere.resource.apache.LogResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if (key.equals("trans_file")) {
            try {
                return new TemplateString(recursiveGet("dir").toString() + getPlanValue("DIR") + "/" + recursiveGet("real_name") + "/" + this.file);
            } catch (Exception e) {
                getLog().warn("translogresource", e);
                return null;
            }
        } else if (key.equals("filename")) {
            return new TemplateString("access_log");
        } else {
            return super.get(key);
        }
    }

    @Override // psoft.hsphere.resource.apache.LogResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        return this.LOG_FORMAT + "\n" + this.logtype + " " + getLocalFile() + "\n";
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("transferlog.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
