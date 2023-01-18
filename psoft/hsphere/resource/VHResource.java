package psoft.hsphere.resource;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/VHResource.class */
public abstract class VHResource extends Resource {
    public abstract String getServerConfig() throws Exception;

    public VHResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public VHResource(ResourceId rId) throws Exception {
        super(rId);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        try {
            if (key.equals("config")) {
                return new TemplateString(getServerConfig());
            }
            return super.get(key);
        } catch (Exception e) {
            getLog().warn("geting config", e);
            return null;
        }
    }

    public String _getName() {
        try {
            return recursiveGet("name").toString();
        } catch (Exception e) {
            return "";
        }
    }
}
