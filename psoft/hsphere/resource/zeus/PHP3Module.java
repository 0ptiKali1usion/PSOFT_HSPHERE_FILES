package psoft.hsphere.resource.zeus;

import java.util.Collection;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.VHResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/PHP3Module.class */
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
        buf.append("modules!fastcgi!enabled           yes\n");
        buf.append("modules!fastcgi!allowanywhere     no\n");
        buf.append("modules!fastcgi!remote!/fcgi-bin/php     localhost:8002\n");
        buf.append("modules!map!alias!/fcgi-bin/php!filepath /dev/null\n");
        buf.append("modules!map!alias!/fcgi-bin/php!type     fastcgi\n");
        Collection<ResourceId> ch = getChildHolder().getChildren();
        synchronized (ch) {
            for (ResourceId resourceId : ch) {
                VHResource r = (VHResource) Resource.get(resourceId);
                buf.append(r.getServerConfig());
            }
        }
        return buf.toString();
    }

    @Override // psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("php3.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
