package psoft.hsphere.resource.apache;

import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.VHResource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/apache/RLimitResource.class */
public class RLimitResource extends VHResource {
    public RLimitResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public RLimitResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
    }

    @Override // psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        String max = getPlanValue("RMAX");
        String min = getPlanValue("RMIN");
        String type = getPlanValue("RTYPE");
        if (("CPU".equals(type) || "MEM".equals(type) || "NPROC".equals(type)) && min != null && !"".equals(min)) {
            try {
                new Integer(min);
                try {
                    new Integer(max);
                } catch (Exception e) {
                    if (!"max".equals(max)) {
                        max = "";
                    }
                }
                return "RLimit" + type + " " + min + " " + max + "\n";
            } catch (Exception e2) {
                return "";
            }
        }
        return "";
    }
}
