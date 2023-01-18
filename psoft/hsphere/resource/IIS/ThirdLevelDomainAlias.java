package psoft.hsphere.resource.IIS;

import java.util.Collection;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.AbstThirdLevelDomainAlias;
import psoft.hsphere.resource.WinHostEntry;

/* loaded from: hsphere.zip:psoft/hsphere/resource/IIS/ThirdLevelDomainAlias.class */
public class ThirdLevelDomainAlias extends AbstThirdLevelDomainAlias {
    public ThirdLevelDomainAlias(int type, Collection values) throws Exception {
        super(type, values);
    }

    public ThirdLevelDomainAlias(ResourceId rid) throws Exception {
        super(rid);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.DomainAlias, psoft.hsphere.Resource
    public void initDone() throws Exception {
        super.initDone();
        String name = recursiveGet("real_name").toString();
        String hostnum = getParent().get().FM_getChild("hosting").get("hostnum").toString();
        WinHostEntry he = (WinHostEntry) recursiveGet("host");
        he.exec("alias-create.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"alias", this.alias}});
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    @Override // psoft.hsphere.resource.DomainAlias, psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            String name = recursiveGet("real_name").toString();
            String hostnum = getParent().get().FM_getChild("hosting").get("hostnum").toString();
            WinHostEntry he = (WinHostEntry) recursiveGet("host");
            he.exec("alias-delete.asp", (String[][]) new String[]{new String[]{"hostnum", hostnum}, new String[]{"hostname", name}, new String[]{"alias", this.alias}});
        }
    }
}
