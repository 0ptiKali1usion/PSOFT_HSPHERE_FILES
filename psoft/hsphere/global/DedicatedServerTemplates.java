package psoft.hsphere.global;

import freemarker.template.TemplateModel;
import java.util.ArrayList;
import java.util.List;
import psoft.hsphere.Reseller;
import psoft.hsphere.Session;
import psoft.hsphere.composite.Holder;
import psoft.hsphere.composite.Item;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.util.freemarker.TemplateList;

/* loaded from: hsphere.zip:psoft/hsphere/global/DedicatedServerTemplates.class */
public class DedicatedServerTemplates extends AnyGlobalSet implements GlobalKeySet {
    static final String prefix = "_DST_";

    public DedicatedServerTemplates(String name, String label, String section, Boolean isDisabledByDefault, Boolean isConfigured, Boolean managedGlobally) throws Exception {
        super(name, label, section, isDisabledByDefault, isConfigured, managedGlobally);
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public boolean isKeyAvailable(String dstId) throws Exception {
        try {
            long id = Long.parseLong(dstId);
            Item item = Holder.findChild(id);
            if (item != null && (item instanceof DedicatedServerTemplate)) {
                if (DSHolder.getResellerHolder().contains(item)) {
                    return true;
                }
                if (Session.getResellerId() == 1 || !DSHolder.getAdminHolder().contains(item)) {
                    return false;
                }
                if (GlobalValueProvider.keyStatus(getPrefix() + dstId, Session.getReseller(), this.isDisabledByDefault, true) == 0) {
                    return true;
                }
                return false;
            }
            return false;
        } catch (NumberFormatException nfe) {
            Session.getLog().debug("Cannot conver id '" + dstId + "' into a number", nfe);
            return false;
        }
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public List getAvailableKeys() throws Exception {
        List result = getOwnTemplateIds();
        List uptList = getUpstreamProviderTemplateIds();
        if (!uptList.isEmpty()) {
            result.addAll(uptList);
        }
        return result;
    }

    public List getOwnTemplateIds() throws Exception {
        List result = new ArrayList();
        Holder h = DSHolder.getResellerHolder();
        for (Object item : h.getAllChildren()) {
            if (item instanceof DedicatedServerTemplate) {
                result.add(String.valueOf(((DedicatedServerTemplate) item).getId()));
            }
        }
        return result;
    }

    public List getUpstreamProviderTemplateIds() throws Exception {
        List result = new ArrayList();
        if (Session.getResellerId() != 1) {
            Reseller r = Session.getReseller();
            Holder h = DSHolder.getAdminHolder();
            for (Object item : h.getAllChildren()) {
                if (item instanceof DedicatedServerTemplate) {
                    String key = String.valueOf(((DedicatedServerTemplate) item).getId());
                    if (GlobalValueProvider.keyStatus(getPrefix() + key, r, this.isDisabledByDefault, true) == 0) {
                        result.add(key);
                    }
                }
            }
        }
        return result;
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public String getPrefix() {
        return prefix;
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public String getKeyDescription(String key) throws Exception {
        long dsoId = Long.parseLong(key);
        return ((DedicatedServerTemplate) Holder.findChild(dsoId)).getName();
    }

    public TemplateModel FM_ownTemplateIds() throws Exception {
        return new TemplateList(getOwnTemplateIds());
    }

    public TemplateModel FM_upstreamTemplateIds() throws Exception {
        return new TemplateList(getUpstreamProviderTemplateIds());
    }
}
