package psoft.hsphere.resource.zeus;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Collection;
import java.util.Iterator;
import psoft.hsphere.Localizer;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/zeus/DirectoryIndexResource.class */
public class DirectoryIndexResource extends psoft.hsphere.resource.apache.DirectoryIndexResource {
    public DirectoryIndexResource(int type, Collection values) throws Exception {
        super(type, values);
    }

    public DirectoryIndexResource(ResourceId rid) throws Exception {
        super(rid);
    }

    @Override // psoft.hsphere.resource.apache.DirectoryIndexResource, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
    }

    @Override // psoft.hsphere.resource.apache.DirectoryIndexResource, psoft.hsphere.resource.VHResource, psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return super.get(key);
    }

    @Override // psoft.hsphere.resource.apache.DirectoryIndexResource, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
    }

    @Override // psoft.hsphere.resource.apache.DirectoryIndexResource, psoft.hsphere.resource.VHResource
    public String getServerConfig() throws Exception {
        StringBuffer buf = new StringBuffer();
        Iterator i = this.indexes.iterator();
        if (i.hasNext()) {
            buf.append("modules!index!files ");
            if (i.hasNext()) {
                buf.append((String) i.next());
            }
            while (i.hasNext()) {
                buf.append(", " + ((String) i.next()));
            }
            buf.append("\n");
        }
        return buf.toString();
    }

    @Override // psoft.hsphere.resource.apache.DirectoryIndexResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("directory_ind.desc", new Object[]{recursiveGet("real_name").toString()});
    }
}
