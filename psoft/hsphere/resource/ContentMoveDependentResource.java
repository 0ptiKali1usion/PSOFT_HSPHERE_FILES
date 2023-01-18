package psoft.hsphere.resource;

import java.util.Collection;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ContentMoveDependentResource.class */
public class ContentMoveDependentResource extends Resource {
    public ContentMoveDependentResource(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public ContentMoveDependentResource(ResourceId rId) throws Exception {
        super(rId);
    }

    public boolean isMoved() throws Exception {
        return getAccount().isMoved();
    }

    @Override // psoft.hsphere.Resource
    public ResourceId addChild(String type, String mod, Collection values) throws Exception {
        if (isMoved()) {
            throw new HSUserException("content.move_lock_resource");
        }
        return super.addChild(type, mod, values);
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        if (isMoved()) {
            throw new HSUserException("content.move_lock_resource");
        }
        super.initDone();
    }

    @Override // psoft.hsphere.Resource
    public synchronized void delete(boolean force, int billingAction) throws Exception {
        if (isMoved()) {
            throw new HSUserException("content.move_lock_resource");
        }
        super.delete(force, billingAction);
    }
}
