package psoft.hsphere.global;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import psoft.hsphere.Localizer;
import psoft.hsphere.design.DesignProvider;

/* loaded from: hsphere.zip:psoft/hsphere/global/Designs.class */
public class Designs extends AnyGlobalSet implements GlobalKeySet {
    static final String prefix = "DE_";

    public Designs(String name, String label, String section, Boolean isDisabledByDefault, Boolean isConfigured, Boolean managedGlobally) throws Exception {
        super(name, label, section, isDisabledByDefault, isConfigured, managedGlobally);
    }

    @Override // psoft.hsphere.global.GlobalKeySet
    public boolean isKeyAvailable(String designId) throws Exception {
        return DesignProvider.isValidDesignId(designId);
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public List getAvailableKeys() throws Exception {
        return new ArrayList(DesignProvider.getAllDesignIds());
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public String getPrefix() {
        return prefix;
    }

    @Override // psoft.hsphere.global.AnyGlobalSet, psoft.hsphere.global.GlobalKeySet
    public String getKeyDescription(String key) throws Exception {
        Hashtable design;
        String label;
        if (isKeyAvailable(key) && (design = DesignProvider.getDesignHash(key)) != null && (label = (String) design.get("label")) != null) {
            return Localizer.translateLabel(label);
        }
        return null;
    }
}
