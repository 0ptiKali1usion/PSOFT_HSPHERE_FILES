package psoft.hsphere.ipmanagement;

import psoft.hsphere.cache.AbstractCache;
import psoft.hsphere.resource.C0015IP;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPCache.class */
public class IPCache extends AbstractCache {
    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return C0015IP.class;
    }
}
