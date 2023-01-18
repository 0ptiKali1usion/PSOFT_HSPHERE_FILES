package psoft.hsphere.resource.mpf.hostedexchange;

import freemarker.template.TemplateModel;
import java.util.Collection;
import java.util.HashMap;
import org.apache.log4j.Category;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.mpf.MPFManager;
import psoft.hsphere.resource.mpf.hostedexchange.BusinessOrganization;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/PublicFolder.class */
public class PublicFolder extends Resource {
    private static Category log = Category.getInstance(PublicFolder.class.getName());

    public PublicFolder(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public PublicFolder(ResourceId id) throws Exception {
        super(id);
    }

    private BusinessOrganization getBusinessOrganization() throws Exception {
        Resource parent = getParent().get();
        BusinessOrganization bo = (BusinessOrganization) parent.getParent().get();
        return bo;
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        MPFManager manager = MPFManager.getManager();
        BusinessOrganization bo = getBusinessOrganization();
        bo.fixPermissions();
        String result = manager.getHES().createPublicFolder(manager.getLDAP(bo.getName()), String.valueOf((int) bo.getPublicStoreQuota().getAmount()), manager.getPdc(), bo.getDomain().getName(), true);
        MPFManager.Result res = manager.processHeResult(result);
        if (!res.isSuccess()) {
            throw new Exception("Unable to create a public folder. " + res.getError());
        }
    }

    public TemplateModel FM_changeSize(int newSize) throws Exception {
        reallocate(newSize);
        return this;
    }

    public void reallocate(int newSize) throws Exception {
        if (this.initialized) {
            BusinessOrganization bo = getBusinessOrganization();
            try {
                HashMap publicFolders = bo.getOrgUsersInfo(true, BusinessOrganization.UsersInfoType.PUBLICFOLDERS);
                HashMap folderInfo = (HashMap) publicFolders.get("/" + bo.getName());
                int realSize = Integer.parseInt((String) folderInfo.get("megabytes"));
                MPFManager manager = MPFManager.getManager();
                int difference = newSize - realSize;
                if (difference != 0) {
                    String result = manager.getERMS().reallocatePublicFolder(manager.getLDAP(bo.getName()), "/" + bo.getName(), Integer.toString(newSize - realSize), manager.getPdc(), true);
                    MPFManager.Result res = manager.processHeResult(result);
                    if (!res.isSuccess()) {
                        throw new Exception(res.getError());
                    }
                    bo.getOrgUsersInfo(true, BusinessOrganization.UsersInfoType.PUBLICFOLDERS);
                }
            } catch (Exception ex) {
                log.error("Unable to get real public folder size from the server", ex);
                throw new Exception("Unable to get real public folder size from the server");
            }
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        if (this.initialized) {
            BusinessOrganization bo = getBusinessOrganization();
            MPFManager manager = MPFManager.getManager();
            manager.getHES().deletePublicFolder(manager.getLDAP(bo.getName()), bo.getName(), manager.getPdc(), true);
            bo.updateServerInfo();
        }
    }
}
