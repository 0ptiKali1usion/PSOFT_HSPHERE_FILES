package psoft.hsphere.ipmanagement;

import psoft.hsphere.cache.AbstractCache;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPRangeCache.class */
public class IPRangeCache extends AbstractCache {
    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return IPRange.class;
    }
}
