package psoft.hsphere.resource.mpf.hostedexchange;

import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import org.apache.log4j.Category;
import psoft.hsphere.InitToken;
import psoft.hsphere.Localizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TypeRegistry;
import psoft.hsphere.resource.AbstractChangeableResource;
import psoft.hsphere.resource.mpf.MPFManager;
import psoft.hsphere.resource.mpf.Tag;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/mpf/hostedexchange/StoreQuota.class */
public class StoreQuota extends AbstractChangeableResource {
    private static Category log = Category.getInstance(StoreQuota.class.getName());

    public StoreQuota(int type, Collection initValues) throws Exception {
        super(type, initValues);
        this.size = Integer.parseInt((String) initValues.iterator().next());
    }

    public StoreQuota(ResourceId id) throws Exception {
        super(id);
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT size FROM he_storequota WHERE id = ?");
            ps.setLong(1, getId().getId());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                this.size = rs.getInt(1);
            } else {
                notFound();
            }
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void saveSize(double newSize) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("UPDATE he_storequota SET size = ? WHERE id = ?");
            ps.setInt(1, (int) newSize);
            ps.setLong(2, getId().getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource
    protected void changeResourcePhysical(double oldSize) throws Exception {
        Object serverValue;
        BusinessOrganization bo = (BusinessOrganization) getParent().get();
        MPFManager manager = MPFManager.getManager();
        int typeId = getId().getType();
        if (typeId == TypeRegistry.getIntTypeId("mailstore")) {
            serverValue = bo.getServerValue("mailstore_quota", true);
        } else {
            serverValue = bo.getServerValue("pubstore_quota", true);
        }
        if (serverValue != null) {
            int difference = this.size - ((Integer) serverValue).intValue();
            if (typeId == TypeRegistry.getIntTypeId("mailstore")) {
                Tag mailStore = new Tag("mailStore");
                mailStore.addChild(new Tag("megabytes", String.valueOf(difference)));
                mailStore.addChild(new Tag("shared", "1"));
                String result = manager.getERMS().reallocateOrganizationMailNoMove(manager.getLDAP(bo.getName()), mailStore.toXML(), manager.getPdc(), true);
                MPFManager.Result res = manager.processHeResult(result);
                if (!res.isSuccess()) {
                    throw new Exception(res.getError());
                }
            } else if (typeId == TypeRegistry.getIntTypeId("pubstore")) {
                PublicFolder pf = null;
                try {
                    pf = (PublicFolder) getId().FM_getChild("public_folder").get();
                } catch (Exception e) {
                    log.warn("Public folder resource is not initialized. ");
                }
                if (difference < 0) {
                    if (pf != null) {
                        pf.reallocate(this.size);
                    }
                    String result2 = manager.getERMS().reallocateOrganizationPublic(manager.getLDAP(bo.getName()), String.valueOf(difference), manager.getPdc(), true);
                    MPFManager.Result res2 = manager.processHeResult(result2);
                    if (!res2.isSuccess()) {
                        throw new Exception(res2.getError());
                    }
                } else {
                    if (difference > 0) {
                        String result3 = manager.getERMS().reallocateOrganizationPublic(manager.getLDAP(bo.getName()), String.valueOf(difference), manager.getPdc(), true);
                        MPFManager.Result res3 = manager.processHeResult(result3);
                        if (!res3.isSuccess()) {
                            throw new Exception(res3.getError());
                        }
                    }
                    if (pf != null) {
                        pf.reallocate(this.size);
                    }
                }
            }
            bo.updateServerInfo();
            return;
        }
        throw new Exception("Unable to get mail quota value from the server");
    }

    @Override // psoft.hsphere.Resource
    public void initDone() throws Exception {
        String result;
        super.initDone();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("INSERT INTO he_storequota (id, size) VALUES (?, ?)");
            ps.setLong(1, this.f41id.getId());
            ps.setInt(2, this.size);
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
            MPFManager manager = MPFManager.getManager();
            BusinessOrganization bo = (BusinessOrganization) getParent().get();
            if (getId().getType() == TypeRegistry.getIntTypeId("mailstore")) {
                Tag mailStore = new Tag("mailStore");
                mailStore.addChild(new Tag("megabytes", String.valueOf(this.size)));
                mailStore.addChild(new Tag("shared", "1"));
                result = manager.getERMS().reallocateOrganizationMailNoMove(manager.getLDAP(bo.getName()), mailStore.toXML(), manager.getPdc(), true);
            } else {
                result = manager.getERMS().reallocateOrganizationPublic(manager.getLDAP(bo.getName()), String.valueOf(this.size), manager.getPdc(), true);
            }
            MPFManager.Result res = manager.processHeResult(result);
            if (!res.isSuccess()) {
                throw new Exception(res.getError());
            }
            bo.updateServerInfo();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public void delete() throws Exception {
        super.delete();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM he_storequota WHERE id = ?");
            ps.setLong(1, this.f41id.getId());
            ps.executeUpdate();
            Session.closeStatement(ps);
            con.close();
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        return "size".equals(key) ? new TemplateString(this.size) : super.get(key);
    }

    public static String getDescription(InitToken token) throws Exception {
        return Resource.getDescription(token) + "MB " + getAmount(token);
    }

    @Override // psoft.hsphere.resource.AbstractChangeableResource, psoft.hsphere.Resource
    public String getDescription() throws Exception {
        return Localizer.translateMessage("quota.desc", new Object[]{String.valueOf(this.size)});
    }
}
