package psoft.hsphere.ipmanagement;

import psoft.hsphere.cache.AbstractCache;

/* loaded from: hsphere.zip:psoft/hsphere/ipmanagement/IPSubnetCache.class */
public class IPSubnetCache extends AbstractCache {
    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return IPSubnet.class;
    }
}
