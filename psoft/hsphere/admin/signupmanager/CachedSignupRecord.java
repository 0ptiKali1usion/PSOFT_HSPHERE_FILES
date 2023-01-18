package psoft.hsphere.admin.signupmanager;

import psoft.hsphere.cache.AbstractCache;

/* loaded from: hsphere.zip:psoft/hsphere/admin/signupmanager/CachedSignupRecord.class */
public class CachedSignupRecord extends AbstractCache {
    @Override // psoft.hsphere.cache.Cache
    public Class getCacheInterface() {
        return SignupRecord.class;
    }
}
